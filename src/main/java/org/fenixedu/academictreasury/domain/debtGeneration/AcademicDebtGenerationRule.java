package org.fenixedu.academictreasury.domain.debtGeneration;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.dto.debtGeneration.AcademicDebtGenerationRuleBean;
import org.fenixedu.academictreasury.dto.debtGeneration.AcademicDebtGenerationRuleBean.ProductEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class AcademicDebtGenerationRule extends AcademicDebtGenerationRule_Base {

    public static final Comparator<AcademicDebtGenerationRule> COMPARE_BY_ORDER_NUMBER =
            new Comparator<AcademicDebtGenerationRule>() {

                @Override
                public int compare(final AcademicDebtGenerationRule o1, final AcademicDebtGenerationRule o2) {
                    int v = Integer.compare(o1.getAcademicDebtGenerationRuleType().getOrderNumber(),
                            o2.getAcademicDebtGenerationRuleType().getOrderNumber());

                    if (v != 0) {
                        return v;
                    }

                    int c = Integer.compare(o1.getOrderNumber(), o2.getOrderNumber());
                    return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
                }
            };

    public static Comparator<AcademicDebtGenerationRule> COMPARATOR_BY_EXECUTION_YEAR =
            new Comparator<AcademicDebtGenerationRule>() {

                @Override
                public int compare(AcademicDebtGenerationRule o1, AcademicDebtGenerationRule o2) {
                    int c = o1.getExecutionYear().compareTo(o2.getExecutionYear());

                    return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
                }
            };

    protected AcademicDebtGenerationRule() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected AcademicDebtGenerationRule(final AcademicDebtGenerationRuleBean bean) {
        this();

        setActive(true);
        setBackgroundExecution(true);

        setAcademicDebtGenerationRuleType(bean.getType());
        setExecutionYear(bean.getExecutionYear());
        setAggregateOnDebitNote(bean.isAggregateOnDebitNote());
        setAggregateAllOrNothing(bean.isAggregateAllOrNothing());
        setEventDebitEntriesMustEqualRuleProducts(bean.isEventDebitEntriesMustEqualRuleProducts());
        setCloseDebitNote(false);
        setAlignAllAcademicTaxesDebitToMaxDueDate(false);
        setCreatePaymentReferenceCode(bean.isToCreatePaymentReferenceCodes());
        setPaymentCodePool(bean.getPaymentCodePool());

        if (bean.isToAlignAcademicTaxesDueDate()) {
            setAcademicTaxDueDateAlignmentType(bean.getAcademicTaxDueDateAlignmentType());
        }

        for (final ProductEntry productEntry : bean.getEntries()) {
            AcademicDebtGenerationRuleEntry.create(this, 
                    productEntry.getProduct(), 
                    productEntry.isCreateDebt(),
                    productEntry.isToCreateAfterLastRegistrationStateDate(), 
                    productEntry.isForceCreation(),
                    productEntry.isLimitToRegisteredOnExecutionYear());
        }

        getDegreeCurricularPlansSet().addAll((bean.getDegreeCurricularPlans()));

        setOrderNumber(-1);
        final Optional<AcademicDebtGenerationRule> max =
                find(getAcademicDebtGenerationRuleType(), getExecutionYear()).max(COMPARE_BY_ORDER_NUMBER);

        setOrderNumber(max.isPresent() ? max.get().getOrderNumber() + 1 : 1);

        setDays(bean.getNumberOfDaysToDueDate());

        setDebtGenerationRuleRestriction(bean.getDebtGenerationRuleRestriction());

        if(bean.isAppliedMinimumAmountForPaymentCode()) {
            setMinimumAmountForPaymentCode(bean.getMinimumAmountForPaymentCode());
        } else {
            setMinimumAmountForPaymentCode(null);
        }
        
        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.bennu.required");
        }

        if (getExecutionYear() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.executionYear.required");
        }

        if (isCloseDebitNote() && !isAggregateOnDebitNote()) {
            throw new AcademicTreasuryDomainException(
                    "error.AcademicDebtGenerationRule.closeDebitNote.requires.aggregateOnDebitNote");
        }

        if (isAggregateAllOrNothing() && !isAggregateOnDebitNote()) {
            throw new AcademicTreasuryDomainException(
                    "error.AcademicDebtGenerationRule.aggregateAllOrNothing.requires.aggregateOnDebitNote");
        }

        if (getAcademicDebtGenerationRuleType().strategyImplementation().isEntriesRequired()
                && getAcademicDebtGenerationRuleEntriesSet().isEmpty()) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.entries.required");
        }

        if (isCreatePaymentReferenceCode() && getPaymentCodePool() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.paymentCodePool.required");
        }

        if (getDegreeCurricularPlansSet().isEmpty()) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.degreeCurricularPlans.required");
        }

        for (final DegreeCurricularPlan degreeCurricularPlan : getDegreeCurricularPlansSet()) {
            if (ExecutionDegree.getByDegreeCurricularPlanAndExecutionYear(degreeCurricularPlan, getExecutionYear()) == null) {
                throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.degreeCurricularPlan.not.active",
                        degreeCurricularPlan.getName());
            }
        }
        
        if(getMinimumAmountForPaymentCode() != null && !TreasuryConstants.isPositive(getMinimumAmountForPaymentCode())) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.minimumAmountForPaymentCode.invalid",
                    getMinimumAmountForPaymentCode().toString());
        }
        
        if(getMinimumAmountForPaymentCode() != null && getMinimumAmountForPaymentCode().scale() > 2) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.minimumAmountForPaymentCode.invalid",
                    getMinimumAmountForPaymentCode().toString());
        }
    }

    public List<AcademicDebtGenerationRuleEntry> getOrderedAcademicDebtGenerationRuleEntries() {
        return getAcademicDebtGenerationRuleEntriesSet().stream().sorted((e1, e2) -> Integer
                .compare(e1.getProduct().getTuitionInstallmentOrder(), e2.getProduct().getTuitionInstallmentOrder()))
                .collect(Collectors.toList());
    }

    public boolean isActive() {
        return getActive();
    }

    public boolean isBackgroundExecution() {
        return getBackgroundExecution();
    }

    public boolean isAggregateOnDebitNote() {
        return super.getAggregateOnDebitNote();
    }

    public boolean isAggregateAllOrNothing() {
        return super.getAggregateAllOrNothing();
    }

    public boolean isEventDebitEntriesMustEqualRuleProducts() {
        return super.getEventDebitEntriesMustEqualRuleProducts();
    }

    public boolean isCloseDebitNote() {
        return super.getCloseDebitNote();
    }

    public boolean isAlignAllAcademicTaxesDebitToMaxDueDate() {
        return super.getAlignAllAcademicTaxesDebitToMaxDueDate();
    }

    public boolean isCreatePaymentReferenceCode() {
        return super.getCreatePaymentReferenceCode();
    }

    public boolean isAppliedMinimumAmountForPaymentCode() {
        return getMinimumAmountForPaymentCode() != null;
    }
    
    public boolean isWithAtLeastOneForceCreationEntry() {
        return getAcademicDebtGenerationRuleEntriesSet().stream().anyMatch(e -> e.isForceCreation());
    }
    
    private boolean isDeletable() {
        return true;
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    @Atomic
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        if (!isDeletable()) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.delete.impossible");
        }

        setDomainRoot(null);
        setAcademicDebtGenerationRuleType(null);
        getDegreeCurricularPlansSet().clear();
        setExecutionYear(null);
        setPaymentCodePool(null);
        while (getAcademicDebtGenerationRuleEntriesSet().size() > 0) {
            getAcademicDebtGenerationRuleEntriesSet().iterator().next().delete();
        }

        setDebtGenerationRuleRestriction(null);
        super.deleteDomainObject();
    }

    @Atomic
    public void activate() {
        setActive(true);

        checkRules();
    }

    @Atomic
    public void inactivate() {
        setActive(false);

        checkRules();
    }

    @Atomic
    public void toggleBackgroundExecution() {
        setBackgroundExecution(!isBackgroundExecution());
    }

    private AcademicDebtGenerationRule findPrevious() {
        List<AcademicDebtGenerationRule> list = find(getAcademicDebtGenerationRuleType(), getExecutionYear())
                .sorted(COMPARE_BY_ORDER_NUMBER).collect(Collectors.toList());

        AcademicDebtGenerationRule result = null;
        for (final AcademicDebtGenerationRule r : list) {
            if (r.getOrderNumber() >= getOrderNumber()) {
                continue;
            }

            if (result == null) {
                result = r;
                continue;
            }

            if (r.getOrderNumber() > result.getOrderNumber()) {
                result = r;
                continue;
            }
        }

        return result;
    }

    private AcademicDebtGenerationRule findNext() {
        List<AcademicDebtGenerationRule> list = find(getAcademicDebtGenerationRuleType(), getExecutionYear())
                .sorted(COMPARE_BY_ORDER_NUMBER).collect(Collectors.toList());

        AcademicDebtGenerationRule result = null;
        for (final AcademicDebtGenerationRule r : list) {
            if (r.getOrderNumber() <= getOrderNumber()) {
                continue;
            }

            if (result == null) {
                result = r;
                continue;
            }

            if (r.getOrderNumber() < result.getOrderNumber()) {
                result = r;
                continue;
            }
        }

        return result;
    }

    public boolean isFirst() {
        return find(this.getAcademicDebtGenerationRuleType(), this.getExecutionYear()).min(COMPARE_BY_ORDER_NUMBER).get() == this;
    }

    public boolean isLast() {
        return find(this.getAcademicDebtGenerationRuleType(), this.getExecutionYear()).max(COMPARE_BY_ORDER_NUMBER).get() == this;
    }

    @Atomic
    public void orderUp() {
        final AcademicDebtGenerationRule previous = findPrevious();

        if (previous == null) {
            return;
        }

        int t = previous.getOrderNumber();

        previous.setOrderNumber(getOrderNumber());
        this.setOrderNumber(t);
    }

    @Atomic
    public void orderDown() {
        final AcademicDebtGenerationRule next = findNext();

        if (next == null) {
            return;
        }

        int t = next.getOrderNumber();

        next.setOrderNumber(getOrderNumber());
        this.setOrderNumber(t);
    }

    @Atomic
    public void editDegreeCurricularPlans(final Set<DegreeCurricularPlan> degreeCurricularPlans) {
        getDegreeCurricularPlansSet().clear();
        getDegreeCurricularPlansSet().addAll(degreeCurricularPlans);
    }
    
    @Atomic
    public void edit(final AcademicDebtGenerationRuleBean bean) {
        getDegreeCurricularPlansSet().clear();
        
        while(!getAcademicDebtGenerationRuleEntriesSet().isEmpty()) {
            getAcademicDebtGenerationRuleEntriesSet().iterator().next().delete();
        }
        
        List<DegreeCurricularPlan> degreeCurricularPlans = bean.getDegreeCurricularPlans();
        getDegreeCurricularPlansSet().addAll(degreeCurricularPlans);

        setAggregateOnDebitNote(bean.isAggregateOnDebitNote());
        setAggregateAllOrNothing(bean.isAggregateAllOrNothing());
        setEventDebitEntriesMustEqualRuleProducts(bean.isEventDebitEntriesMustEqualRuleProducts());
        setCloseDebitNote(false);
        setAlignAllAcademicTaxesDebitToMaxDueDate(false);
        setCreatePaymentReferenceCode(bean.isToCreatePaymentReferenceCodes());
        setPaymentCodePool(bean.getPaymentCodePool());

        if (bean.isToAlignAcademicTaxesDueDate()) {
            setAcademicTaxDueDateAlignmentType(bean.getAcademicTaxDueDateAlignmentType());
        }

        for (final ProductEntry productEntry : bean.getEntries()) {
            AcademicDebtGenerationRuleEntry.create(this, productEntry.getProduct(), productEntry.isCreateDebt(),
                    productEntry.isToCreateAfterLastRegistrationStateDate(), productEntry.isForceCreation(),
                    productEntry.isLimitToRegisteredOnExecutionYear());
        }

        setDays(bean.getNumberOfDaysToDueDate());

        setDebtGenerationRuleRestriction(bean.getDebtGenerationRuleRestriction());

        if(bean.isAppliedMinimumAmountForPaymentCode()) {
            setMinimumAmountForPaymentCode(bean.getMinimumAmountForPaymentCode());
        } else {
            setMinimumAmountForPaymentCode(null);
        }
        
        checkRules();
    }
    
    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<AcademicDebtGenerationRule> findAll() {
        return FenixFramework.getDomainRoot().getAcademicDebtGenerationRuleSet().stream();
    }

    public static Stream<AcademicDebtGenerationRule> find(final AcademicDebtGenerationRuleType type,
            final ExecutionYear executionYear) {
        return executionYear.getAcademicDebtGenerationRulesSet().stream()
                .filter(r -> r.getAcademicDebtGenerationRuleType() == type);
    }

    public static Stream<AcademicDebtGenerationRule> findActive() {
        return findAll().filter(AcademicDebtGenerationRule::isActive);
    }

    public static Stream<AcademicDebtGenerationRule> findActiveByType(final AcademicDebtGenerationRuleType type) {
        return type.getAcademicDebtGenerationRulesSet().stream().filter(AcademicDebtGenerationRule::isActive);
    }

    @Atomic
    public static AcademicDebtGenerationRule create(final AcademicDebtGenerationRuleBean bean) {
        if(bean.isAppliedMinimumAmountForPaymentCode()) {
            if(bean.getMinimumAmountForPaymentCode() == null || !TreasuryConstants.isPositive(bean.getMinimumAmountForPaymentCode())) {
                throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.create.appliedMinimumAmountForPaymentCode.but.minimumAmountForPaymentCode.not.valid");
            }
        }
        
        return new AcademicDebtGenerationRule(bean);
    }

    public static List<AcademicDebtGenerationProcessingResult> runAllActive(final boolean runOnlyWithBackgroundExecution) {
        final List<Future<List<AcademicDebtGenerationProcessingResult>>> futureList = Lists.newArrayList();
        
        final ExecutorService exService = Executors.newSingleThreadExecutor();
        for (final AcademicDebtGenerationRuleType type : AcademicDebtGenerationRuleType.findAll()
                .sorted(AcademicDebtGenerationRuleType.COMPARE_BY_ORDER_NUMBER).collect(Collectors.toList())) {
            for (final AcademicDebtGenerationRule academicDebtGenerationRule : AcademicDebtGenerationRule.findActiveByType(type)
                    .sorted(COMPARE_BY_ORDER_NUMBER).collect(Collectors.toList())) {

                if (runOnlyWithBackgroundExecution && !academicDebtGenerationRule.isBackgroundExecution()) {
                    continue;
                }

                final RuleCallable exec = new RuleCallable(academicDebtGenerationRule);
                futureList.add(exService.submit(exec));
            }
        }
        
        exService.shutdown();
        
        final List<AcademicDebtGenerationProcessingResult> returnResult = Lists.newArrayList();
        for (Future<List<AcademicDebtGenerationProcessingResult>> future : futureList) {
            try {
                returnResult.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
            }
        }
        
        return returnResult;
    }

    public static List<AcademicDebtGenerationProcessingResult> runAllActiveForRegistration(final Registration registration,
            final boolean runOnlyWithBackgroundExecution) {
        final List<Future<List<AcademicDebtGenerationProcessingResult>>> futureList = Lists.newArrayList();
        
        final ExecutorService exService = Executors.newSingleThreadExecutor();
        for (final AcademicDebtGenerationRuleType type : AcademicDebtGenerationRuleType.findAll()
                .sorted(AcademicDebtGenerationRuleType.COMPARE_BY_ORDER_NUMBER).collect(Collectors.toList())) {
            for (final AcademicDebtGenerationRule academicDebtGenerationRule : AcademicDebtGenerationRule.findActiveByType(type)
                    .sorted(COMPARE_BY_ORDER_NUMBER).collect(Collectors.toList())) {

                if (runOnlyWithBackgroundExecution && !academicDebtGenerationRule.isBackgroundExecution()) {
                    continue;
                }

                final RuleCallable exec = new RuleCallable(academicDebtGenerationRule, registration);
                futureList.add(exService.submit(exec));
            }
        }
        
        exService.shutdown();
        
        final List<AcademicDebtGenerationProcessingResult> returnResult = Lists.newArrayList();
        for (Future<List<AcademicDebtGenerationProcessingResult>> future : futureList) {
            try {
                returnResult.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
            }
        }
        
        return returnResult;
    }

    public static List<AcademicDebtGenerationProcessingResult> runAllActiveForRegistrationAndExecutionYear(final Registration registration,
            final ExecutionYear executionYear, final boolean runOnlyWithBackgroundExecution) {
        final List<Future<List<AcademicDebtGenerationProcessingResult>>> futureList = Lists.newArrayList();
        
        final ExecutorService exService = Executors.newSingleThreadExecutor();
        for (final AcademicDebtGenerationRuleType type : AcademicDebtGenerationRuleType.findAll()
                .sorted(AcademicDebtGenerationRuleType.COMPARE_BY_ORDER_NUMBER).collect(Collectors.toList())) {
            for (final AcademicDebtGenerationRule academicDebtGenerationRule : AcademicDebtGenerationRule.findActiveByType(type)
                    .sorted(COMPARE_BY_ORDER_NUMBER).collect(Collectors.toList())) {

                if (runOnlyWithBackgroundExecution && !academicDebtGenerationRule.isBackgroundExecution()) {
                    continue;
                }

                if (academicDebtGenerationRule.getExecutionYear() != executionYear) {
                    continue;
                }

                final RuleCallable exec = new RuleCallable(academicDebtGenerationRule, registration);
                futureList.add(exService.submit(exec));
            }
        }
        
        exService.shutdown();
        
        final List<AcademicDebtGenerationProcessingResult> returnResult = Lists.newArrayList();
        for (Future<List<AcademicDebtGenerationProcessingResult>> future : futureList) {
            try {
                returnResult.addAll(future.get());
            } catch (InterruptedException | ExecutionException e) {
            }
        }
        
        return returnResult;
    }

    public static List<AcademicDebtGenerationProcessingResult> runAcademicDebtGenerationRule(final AcademicDebtGenerationRule rule) {

        if (!rule.isActive()) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.not.active");
        }

        final ExecutorService exService = Executors.newSingleThreadExecutor();

        final RuleCallable exec = new RuleCallable(rule);
        try {
            return exService.submit(exec).get();
        } catch (InterruptedException | ExecutionException e) {
        }
        
        return Lists.newArrayList();
    }

    // @formatter: off
    /**********
     * EXECUTOR
     **********
     */
    // @formatter: on

    private static Logger logger = LoggerFactory.getLogger(AcademicDebtGenerationRule.class);

    private static final List<String> MESSAGES_TO_IGNORE =
            Lists.newArrayList("error.AcademicDebtGenerationRule.debit.note.without.debit.entries",
                    "error.AcademicDebtGenerationRule.debitEntry.with.none.or.annuled.finantial.document");

    public static final class RuleCallable implements Callable<List<AcademicDebtGenerationProcessingResult>> {

        private String academicDebtGenerationRuleId;
        private String registrationId;

        public RuleCallable(final AcademicDebtGenerationRule rule) {
            this.academicDebtGenerationRuleId = rule.getExternalId();
        }

        public RuleCallable(final AcademicDebtGenerationRule rule, final Registration registration) {
            this.academicDebtGenerationRuleId = rule.getExternalId();
            this.registrationId = registration.getExternalId();
        }

        @Override
        public List<AcademicDebtGenerationProcessingResult> call() {
            return executeRule();
        }

        @Atomic(mode = TxMode.READ)
        private List<AcademicDebtGenerationProcessingResult> executeRule() {
            final List<AcademicDebtGenerationProcessingResult> result = Lists.newArrayList();
            
            final AcademicDebtGenerationRule rule = FenixFramework.getDomainObject(academicDebtGenerationRuleId);
            try {
                if (!Strings.isNullOrEmpty(registrationId)) {
                    final Registration registration = FenixFramework.getDomainObject(registrationId);
                    result.addAll(rule.getAcademicDebtGenerationRuleType().strategyImplementation().process(rule, registration));
                } else {
                    result.addAll(rule.getAcademicDebtGenerationRuleType().strategyImplementation().process(rule));
                }
            } catch (final AcademicTreasuryDomainException e) {
                if (!MESSAGES_TO_IGNORE.contains(e.getMessage())) {
                    logger.info(e.getMessage());
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
            
            return result;
        }
    }

}
