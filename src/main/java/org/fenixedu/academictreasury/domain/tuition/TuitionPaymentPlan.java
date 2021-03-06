package org.fenixedu.academictreasury.domain.tuition;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.dto.tariff.AcademicTariffBean;
import org.fenixedu.academictreasury.dto.tariff.TuitionPaymentPlanBean;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.academictreasury.util.LocalizedStringUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class TuitionPaymentPlan extends TuitionPaymentPlan_Base {

    private static final String CONDITIONS_DESCRIPTION_SEPARATOR = ", ";

    public TuitionPaymentPlan() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected TuitionPaymentPlan(final TuitionPaymentPlan tuitionPaymentPlanToCopy, final ExecutionYear toExecutionYear) {
        this();

        setFinantialEntity(tuitionPaymentPlanToCopy.getFinantialEntity());
        setTuitionPaymentPlanGroup(tuitionPaymentPlanToCopy.getTuitionPaymentPlanGroup());
        setProduct(tuitionPaymentPlanToCopy.getTuitionPaymentPlanGroup().getCurrentProduct());
        setExecutionYear(toExecutionYear);

        setDefaultPaymentPlan(tuitionPaymentPlanToCopy.isDefaultPaymentPlan());
        setPayorDebtAccount(tuitionPaymentPlanToCopy.getPayorDebtAccount());
        setCustomized(tuitionPaymentPlanToCopy.isCustomized());

        setCustomizedName(tuitionPaymentPlanToCopy.getCustomizedName());

        createInstallments(tuitionPaymentPlanToCopy);

        setCopyFromTuitionPaymentPlan(tuitionPaymentPlanToCopy);

        tuitionPaymentPlanToCopy.getTuitionConditionRulesSet().forEach(cond -> cond.copyToPlan(this));

        tuitionPaymentPlanToCopy.getTuitionPaymentPlanOrdersSet().forEach(order -> order.copyToPlan(this));

        checkRules();
    }

    public TuitionPaymentPlan(TuitionPaymentPlanBean tuitionPaymentPlanBean) {
        this();

        setFinantialEntity(tuitionPaymentPlanBean.getFinantialEntity());
        setTuitionPaymentPlanGroup(tuitionPaymentPlanBean.getTuitionPaymentPlanGroup());
        setProduct(tuitionPaymentPlanBean.getTuitionPaymentPlanGroup().getCurrentProduct());
        setExecutionYear(tuitionPaymentPlanBean.getExecutionYear());
        setDefaultPaymentPlan(tuitionPaymentPlanBean.isDefaultPaymentPlan());
        setPayorDebtAccount(tuitionPaymentPlanBean.getPayorDebtAccount());
        setCustomized(tuitionPaymentPlanBean.isCustomized());

        LocalizedString mls = new LocalizedString();
        for (final Locale locale : TreasuryPlataformDependentServicesFactory.implementation().availableLocales()) {
            mls = mls.with(locale, tuitionPaymentPlanBean.getName());
        }

        setCustomizedName(mls);

        for (TuitionConditionRule condition : tuitionPaymentPlanBean.getConditionRules()) {
            addTuitionConditionRules(condition);
        }

        createPaymentPlanOrder(tuitionPaymentPlanBean.getDegreeCurricularPlans());

        createInstallments(tuitionPaymentPlanBean);

        checkRules();

    }

    public void checkRules() {
        if (getTuitionPaymentPlanGroup() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.tuitionPaymentPlanGroup.required");
        }

        if (getFinantialEntity() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.finantialEntity.required");
        }

        if (getExecutionYear() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.executionYear.required");
        }

        if (getTuitionPaymentPlanOrdersSet().isEmpty()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.tuitionPaymentPlanOrders.required");
        }

        if (isCustomized() && LocalizedStringUtil.isTrimmedEmpty(getCustomizedName())) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.customized.required.name");
        }

        if (isCustomized() && hasStudentSpecificConditionSelected()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.customized.plan.cannot.have.other.options");
        }

        if (isDefaultPaymentPlan()
                && getTuitionPaymentPlanGroup() != TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.default.payment.plan.must.be.for.registration");
        }

        if (isDefaultPaymentPlan()) {
            for (final TuitionInstallmentTariff tuitionInstallmentTariff : getTuitionInstallmentTariffsSet()) {
                if (!tuitionInstallmentTariff.getTuitionCalculationType().isFixedAmount()) {
                    throw new AcademicTreasuryDomainException(
                            "error.TuitionPaymentPlan.default.payment.plan.tariffs.calculation.type.not.fixed.amount");
                }
            }
        }

        if (getTuitionInstallmentTariffsSet().isEmpty()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.installments.must.not.be.empty");
        }
        if (existsAtLeastOneTariffCalculatedAmountWithoutRemaining()) {
            throw new AcademicTreasuryDomainException(
                    "error.TuitionPaymentPlan.installments.customCalculators.must.have.remaining");
        }

        if ((getTuitionPaymentPlanGroup().isForStandalone() || getTuitionPaymentPlanGroup().isForExtracurricular())
                && getTuitionInstallmentTariffsSet().size() > 1) {
            throw new AcademicTreasuryDomainException(
                    "error.TuitionPaymentPlan.standalone.and.extracurricular.supports.only.one.installment");
        }

        if (getTuitionPaymentPlanGroup().isForRegistration() && !hasAtLeastOneConditionSpecified()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.specify.at.least.one.condition");
        }

    }

    private boolean existsAtLeastOneTariffCalculatedAmountWithoutRemaining() {
        return !getTuitionInstallmentTariffsSet().stream()
                .filter(tariff -> tariff.getTuitionCalculationType().isCalculatedAmount())
                .map(tariff -> tariff.getTuitionTariffCustomCalculator())
                .allMatch(calculator -> getTuitionInstallmentTariffsSet().stream()
                        .anyMatch(tariff -> (tariff.getTuitionCalculationType().isCalculatedAmount()
                                && tariff.getTuitionTariffCalculatedAmountType().isRemaining()
                                && tariff.getTuitionTariffCustomCalculator() == calculator)));
    }

    private boolean hasStudentSpecificConditionSelected() {
        return !getTuitionConditionRulesSet().isEmpty();
    }

    private boolean hasAtLeastOneConditionSpecified() {

        boolean hasAtLeastOneCondition = false;

        hasAtLeastOneCondition |= isDefaultPaymentPlan();
        hasAtLeastOneCondition |= !getTuitionConditionRulesSet().isEmpty();
        hasAtLeastOneCondition |= isCustomized();

        return hasAtLeastOneCondition;
    }

    private void createPaymentPlanOrder(Set<DegreeCurricularPlan> degreeCurricularPlans) {
        for (DegreeCurricularPlan plan : degreeCurricularPlans) {
            TuitionPaymentPlanOrder.create(this, plan);
        }
    }

    private TuitionPaymentPlanOrder getTuitionPaymentPlanOrder(DegreeCurricularPlan degreeCurricularPlan) {
        return getTuitionPaymentPlanOrdersSet().stream().filter(order -> order.getDegreeCurricularPlan() == degreeCurricularPlan)
                .findFirst().orElse(null);
    }

    private void createInstallments(final TuitionPaymentPlanBean tuitionPaymentPlanBean) {
        for (final AcademicTariffBean academicTariffBean : tuitionPaymentPlanBean.getTuitionInstallmentBeans()) {
            TuitionInstallmentTariff.create(tuitionPaymentPlanBean.getFinantialEntity(), this, academicTariffBean);
        }
    }

    private void createInstallments(final TuitionPaymentPlan tuitionPaymentPlanToCopy) {
        tuitionPaymentPlanToCopy.getTuitionInstallmentTariffsSet().stream()
                .sorted(TuitionInstallmentTariff.COMPARATOR_BY_INSTALLMENT_NUMBER)
                .forEach(t -> TuitionInstallmentTariff.copy(t, this));
    }

    public String getConditionsDescription() {
        if (isCustomized()) {
            return AcademicTreasuryConstants.academicTreasuryBundle("label.TuitionPaymentPlan.customized") + " ["
                    + getCustomizedName().getContent() + "] ";
        }

        if (isDefaultPaymentPlan()) {
            return AcademicTreasuryConstants.academicTreasuryBundle("label.TuitionPaymentPlan.defaultPaymentPlan");
        }

        if (getTuitionPaymentPlanGroup().isForStandalone()) {
            return AcademicTreasuryConstants.academicTreasuryBundle("label.TuitionPaymentPlan.standalone") + ", "
                    + getTuitionConditionRulesSet().stream().sorted(TuitionConditionRule.COMPARE_BY_CONDITION_RULE_NAME)
                            .map(c -> TuitionConditionRule.getPresentationName(c.getClass()) + " [" + c.getDescription() + "]")
                            .collect(Collectors.joining(", "));

        }
        if (getTuitionPaymentPlanGroup().isForExtracurricular()) {
            return AcademicTreasuryConstants.academicTreasuryBundle("label.TuitionPaymentPlan.extracurricular") + ", "
                    + getTuitionConditionRulesSet().stream().sorted(TuitionConditionRule.COMPARE_BY_CONDITION_RULE_NAME)
                            .map(c -> TuitionConditionRule.getPresentationName(c.getClass()) + " [" + c.getDescription() + "]")
                            .collect(Collectors.joining(", "));
        }

        return getTuitionConditionRulesSet().stream().sorted(TuitionConditionRule.COMPARE_BY_CONDITION_RULE_NAME)
                .map(c -> TuitionConditionRule.getPresentationName(c.getClass()) + " [" + c.getDescription() + "]")
                .collect(Collectors.joining(", "));
    }

    public List<TuitionInstallmentTariff> getOrderedTuitionInstallmentTariffs() {
        return super.getTuitionInstallmentTariffsSet().stream().sorted(TuitionInstallmentTariff.COMPARATOR_BY_INSTALLMENT_NUMBER)
                .collect(Collectors.toList());
    }

    public TuitionInstallmentTariff getStandaloneTuitionInstallmentTariff() {
        if (!getTuitionPaymentPlanGroup().isForStandalone()) {
            throw new RuntimeException("wrong call");
        }

        return getOrderedTuitionInstallmentTariffs().get(0);
    }

    public TuitionInstallmentTariff getExtracurricularTuitionInstallmentTariff() {
        if (!getTuitionPaymentPlanGroup().isForExtracurricular()) {
            throw new RuntimeException("wrong call");
        }

        return getOrderedTuitionInstallmentTariffs().get(0);
    }

    public TuitionConditionRule getTuitionConditionRule(Class<? extends TuitionConditionRule> clazz) {
        return getTuitionConditionRulesSet().stream().filter(c -> c.getClass().equals(clazz)).findFirst().orElse(null);
    }

    public LocalizedString installmentName(Registration registration, final TuitionInstallmentTariff installmentTariff) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();
        String label = "label.TuitionInstallmentTariff.debitEntry.name.";

        if (getTuitionPaymentPlanGroup().isForRegistration()) {
            label += "registration";
        } else if (getTuitionPaymentPlanGroup().isForStandalone()) {
            label += "standalone";
        } else if (getTuitionPaymentPlanGroup().isForExtracurricular()) {
            label += "extracurricular";
        }
        DegreeCurricularPlan degreeCurricularPlan =
                registration.getStudentCurricularPlan(getExecutionYear()).getDegreeCurricularPlan();

        LocalizedString result = new LocalizedString();
        for (final Locale locale : treasuryServices.availableLocales()) {
            final String installmentName = AcademicTreasuryConstants.academicTreasuryBundle(locale, label,
                    String.valueOf(installmentTariff.getInstallmentOrder()),
                    degreeCurricularPlan.getDegree().getPresentationName(getExecutionYear(), locale),
                    getExecutionYear().getQualifiedName());

            result = result.with(locale, installmentName);
        }

        return result;
    }

    public boolean isCustomized() {
        return getCustomized();
    }

    public boolean isDefaultPaymentPlan() {
        return getDefaultPaymentPlan();
    }

    public boolean createDebitEntriesForRegistration(final DebtAccount debtAccount,
            final AcademicTreasuryEvent academicTreasuryEvent, final LocalDate when) {

        if (!getTuitionPaymentPlanGroup().isForRegistration()) {
            throw new RuntimeException("wrong call");
        }

        if (academicTreasuryEvent.isCharged()) {
            return false;
        }

        boolean createdDebitEntries = false;
        for (final TuitionInstallmentTariff tariff : getTuitionInstallmentTariffsSet()) {
            if (!academicTreasuryEvent.isChargedWithDebitEntry(tariff)) {
                tariff.createDebitEntryForRegistration(debtAccount, academicTreasuryEvent, when);
                createdDebitEntries = true;
            }
        }

        return createdDebitEntries;
    }

    public boolean createDebitEntriesForStandalone(final DebtAccount debtAccount,
            final AcademicTreasuryEvent academicTreasuryEvent, final Enrolment standaloneEnrolment, final LocalDate when) {

        if (!getTuitionPaymentPlanGroup().isForStandalone()) {
            throw new RuntimeException("wrong call");
        }

        if (!standaloneEnrolment.isStandalone()) {
            throw new RuntimeException("error.TuitionPaymentPlan.enrolment.not.standalone");
        }

        boolean createdDebitEntries = false;
        final Set<DebitEntry> createdDebitEntriesSet = Sets.newHashSet();
        for (final TuitionInstallmentTariff tariff : getTuitionInstallmentTariffsSet()) {
            if (!academicTreasuryEvent.isChargedWithDebitEntry(standaloneEnrolment)) {
                createdDebitEntriesSet
                        .add(tariff.createDebitEntryForStandalone(debtAccount, academicTreasuryEvent, standaloneEnrolment, when));
                createdDebitEntries = true;
            }
        }

        if (createdDebitEntries) {
            final DebitNote debitNote = DebitNote.create(debtAccount, DocumentNumberSeries
                    .findUniqueDefault(FinantialDocumentType.findForDebitNote(), debtAccount.getFinantialInstitution()).get(),
                    new DateTime());

            debitNote.addDebitNoteEntries(Lists.newArrayList(createdDebitEntriesSet));

            if (AcademicTreasurySettings.getInstance().isCloseServiceRequestEmolumentsWithDebitNote()) {
                debitNote.closeDocument();
            }
        }

        return createdDebitEntries;
    }

    public boolean createDebitEntriesForExtracurricular(final DebtAccount debtAccount,
            final AcademicTreasuryEvent academicTreasuryEvent, final Enrolment extracurricularEnrolment, final LocalDate when) {

        if (!getTuitionPaymentPlanGroup().isForExtracurricular()) {
            throw new RuntimeException("wrong call");
        }

        if (!extracurricularEnrolment.isExtraCurricular()) {
            throw new RuntimeException("error.TuitionPaymentPlan.enrolment.not.standalone");
        }

        boolean createdDebitEntries = false;
        final Set<DebitEntry> createdDebitEntriesSet = Sets.newHashSet();
        for (final TuitionInstallmentTariff tariff : getTuitionInstallmentTariffsSet()) {
            if (!academicTreasuryEvent.isChargedWithDebitEntry(extracurricularEnrolment)) {
                createdDebitEntriesSet.add(tariff.createDebitEntryForExtracurricular(debtAccount, academicTreasuryEvent,
                        extracurricularEnrolment, when));
                createdDebitEntries = true;
            }
        }

        if (createdDebitEntries) {
            final DebitNote debitNote = DebitNote.create(debtAccount, DocumentNumberSeries
                    .findUniqueDefault(FinantialDocumentType.findForDebitNote(), debtAccount.getFinantialInstitution()).get(),
                    new DateTime());

            debitNote.addDebitNoteEntries(Lists.newArrayList(createdDebitEntriesSet));

            if (AcademicTreasurySettings.getInstance().isCloseServiceRequestEmolumentsWithDebitNote()) {
                debitNote.closeDocument();
            }
        }

        return createdDebitEntries;

    }

    public boolean isDeletable() {

        if (getTuitionPaymentPlanGroup().isForRegistration() && isDefaultPaymentPlan()) {

            Set<DegreeCurricularPlan> collectionOfDcp = getTuitionPaymentPlanOrdersSet().stream()
                    .map(order -> order.getDegreeCurricularPlan()).collect(Collectors.toSet());

            final Set<TuitionPaymentPlan> allPlans = Sets.newHashSet();
            for (DegreeCurricularPlan degreeCurricularPlan : collectionOfDcp) {
                allPlans.addAll(TuitionPaymentPlan.find(TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get(),
                        degreeCurricularPlan, getExecutionYear()).collect(Collectors.toSet()));
                allPlans.addAll(TuitionPaymentPlan.find(TuitionPaymentPlanGroup.findUniqueDefaultGroupForStandalone().get(),
                        degreeCurricularPlan, getExecutionYear()).collect(Collectors.toSet()));
                allPlans.addAll(TuitionPaymentPlan.find(TuitionPaymentPlanGroup.findUniqueDefaultGroupForExtracurricular().get(),
                        degreeCurricularPlan, getExecutionYear()).collect(Collectors.toSet()));
            }
            for (final TuitionPaymentPlan tuitionPaymentPlan : allPlans) {

                if (tuitionPaymentPlan == this) {
                    continue;
                }

                for (final TuitionInstallmentTariff tuitionInstallmentTariff : tuitionPaymentPlan
                        .getTuitionInstallmentTariffsSet()) {
                    if (tuitionInstallmentTariff.isDefaultPaymentPlanDependent()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public BigDecimal tuitionTotalAmount() {
        return getTuitionInstallmentTariffsSet().stream().map(t -> t.getFixedAmount()).reduce((a, c) -> a.add(c))
                .orElse(BigDecimal.ZERO);
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.delete.impossible");
        }

        setDomainRoot(null);

        while (!getTuitionInstallmentTariffsSet().isEmpty()) {
            getTuitionInstallmentTariffsSet().iterator().next().delete();
        }
        super.getTuitionConditionRulesSet().forEach(rule -> rule.delete());
        super.setTuitionPaymentPlanGroup(null);
        super.setExecutionYear(null);
        super.setProduct(null);
        this.setFinantialEntity(null);
        this.setPayorDebtAccount(null);

        setCopyFromTuitionPaymentPlan(null);
        while (!getTuitionPaymentPlanCopiesSet().isEmpty()) {
            getTuitionPaymentPlanCopiesSet().iterator().next().setCopyFromTuitionPaymentPlan(null);
        }
        while (!getTuitionPaymentPlanOrdersSet().isEmpty()) {
            getTuitionPaymentPlanOrdersSet().iterator().next().delete(false);
        }

        // From old model but migrated tuition payment plans might have associated dcp or other conditions
        super.setDegreeCurricularPlan(null);
        super.setCurricularYear(null);
        super.setRegistrationProtocol(null);
        super.setStatuteType(null);
        super.setIngression(null);
        
        super.deleteDomainObject();
    }

    // @formatter:off
    /* -------------
     * OTHER METHODS
     * -------------
     */
    // @formatter:on

    protected FinantialEntity finantialEntity() {
        // TODO ANIL
        return FinantialEntity.findAll().findFirst().get();
    }

    // To be extended
    public boolean isStudentMustBeEnrolled() {
        return true;
    }

    public boolean isPayorDebtAccountDefined() {
        return getPayorDebtAccount() != null;
    }

    public boolean isCopyFromOtherExistingTuitionPaymentPlan() {
        return getCopyFromTuitionPaymentPlan() != null;
    }

    public boolean hasCopiesInExecutionInterval(ExecutionInterval executionInterval) {
        return getTuitionPaymentPlanCopiesSet().stream().anyMatch(p -> p.getExecutionYear() == executionInterval);
    }

    // @formatter:off
    /* --------
     * SERVICES
     * --------
     */
    // @formatter:on

    public static Stream<TuitionPaymentPlan> findAll() {
        return FenixFramework.getDomainRoot().getTuitionPaymentPlansSet().stream();
    }

    public static Stream<TuitionPaymentPlan> find(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup) {
        return tuitionPaymentPlanGroup.getTuitionPaymentPlansSet().stream();
    }

    public static Stream<TuitionPaymentPlan> find(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            final FinantialEntity finantialEntity, final ExecutionYear executionYear) {
        return find(tuitionPaymentPlanGroup).filter(t -> t.finantialEntity() == finantialEntity)
                .filter(t -> t.getExecutionYear() == executionYear);
    }

    @Deprecated
    public static Stream<TuitionPaymentPlan> find(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            final DegreeCurricularPlan degreeCurricularPlan, final ExecutionYear executionYear) {

        return find(tuitionPaymentPlanGroup)
                .filter(t -> t.getExecutionYear() == executionYear && t.getTuitionPaymentPlanOrdersSet().stream()
                        .anyMatch(order -> order.getDegreeCurricularPlan() == degreeCurricularPlan));
    }

    @Deprecated
    public static Stream<TuitionPaymentPlan> findSortedByPaymentPlanOrder(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            final DegreeCurricularPlan degreeCurricularPlan, final ExecutionYear executionYear) {
        return TuitionPaymentPlanOrder.findSortedByPaymentPlanOrder(tuitionPaymentPlanGroup, degreeCurricularPlan, executionYear)
                .map(order -> order.getTuitionPaymentPlan());
    }

    private static Stream<TuitionPaymentPlan> findDefaultPaymentPlans(final DegreeCurricularPlan degreeCurricularPlan,
            final ExecutionYear executionYear) {
        return find(TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get(), degreeCurricularPlan, executionYear)
                .filter(t -> t.isDefaultPaymentPlan());
    }

    public static Optional<TuitionPaymentPlan> findUniqueDefaultPaymentPlan(final DegreeCurricularPlan degreeCurricularPlan,
            final ExecutionYear executionYear) {
        return findDefaultPaymentPlans(degreeCurricularPlan, executionYear).findFirst();
    }

    public static boolean isDefaultPaymentPlanDefined(final DegreeCurricularPlan degreeCurricularPlan,
            final ExecutionYear executionYear) {
        return findUniqueDefaultPaymentPlan(degreeCurricularPlan, executionYear).isPresent();
    }

    public static TuitionPaymentPlan inferTuitionPaymentPlanForRegistration(final DegreeCurricularPlan degreeCurricularPlan,
            final ExecutionYear executionYear, Predicate<? super TuitionPaymentPlan> predicate) {

        final List<TuitionPaymentPlan> plans = TuitionPaymentPlan
                .findSortedByPaymentPlanOrder(TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get(),
                        degreeCurricularPlan, executionYear)
                .collect(Collectors.toList());

        return plans.stream().filter(predicate).findFirst().orElse(null);
    }

    public static TuitionPaymentPlan inferTuitionPaymentPlanForRegistration(final Registration registration,
            final ExecutionYear executionYear, Predicate<? super TuitionPaymentPlan> predicate) {
        final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);

        if (studentCurricularPlan == null) {
            return null;
        }

        final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();

        return inferTuitionPaymentPlanForRegistration(degreeCurricularPlan, executionYear, predicate);
    }

    public static TuitionPaymentPlan inferTuitionPaymentPlanForRegistration(final Registration registration,
            final ExecutionYear executionYear) {

        final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);

        if (studentCurricularPlan == null) {
            return null;
        }

        Predicate<? super TuitionPaymentPlan> predicate =
                plan -> !plan.isCustomized() && plan.isValidTo(registration, executionYear, null, Collections.emptySet());

        return inferTuitionPaymentPlanForRegistration(registration, executionYear, predicate);
    }

    public static TuitionPaymentPlan inferTuitionPaymentPlanForStandaloneEnrolment(final Registration registration,
            final ExecutionYear executionYear, final Enrolment enrolment) {

        if (!enrolment.isStandalone()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.enrolment.is.not.standalone");
        }

        final DegreeCurricularPlan degreeCurricularPlan = enrolment.getCurricularCourse().getDegreeCurricularPlan();

        final List<TuitionPaymentPlan> filtered = TuitionPaymentPlan
                .findSortedByPaymentPlanOrder(TuitionPaymentPlanGroup.findUniqueDefaultGroupForStandalone().get(),
                        degreeCurricularPlan, executionYear)
                .collect(Collectors.toList());

        return filtered.stream().filter(
                plan -> !plan.isCustomized() && plan.isValidTo(registration, executionYear, enrolment, Collections.emptySet()))
                .findFirst().orElse(null);
    }

    public static TuitionPaymentPlan inferTuitionPaymentPlanForExtracurricularEnrolment(final Registration registration,
            final ExecutionYear executionYear, final Enrolment enrolment) {

        if (!enrolment.isExtraCurricular()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.enrolment.is.not.extracurricular");
        }

        final DegreeCurricularPlan degreeCurricularPlan = enrolment.getCurricularCourse().getDegreeCurricularPlan();
        final List<TuitionPaymentPlan> filtered = TuitionPaymentPlan
                .findSortedByPaymentPlanOrder(TuitionPaymentPlanGroup.findUniqueDefaultGroupForStandalone().get(),
                        degreeCurricularPlan, executionYear)
                .collect(Collectors.toList());

        return filtered.stream().filter(
                plan -> !plan.isCustomized() && plan.isValidTo(registration, executionYear, enrolment, Collections.emptySet()))
                .findFirst().orElse(null);
    }

    public boolean isValidTo(Registration registration, ExecutionYear executionYear, Enrolment enrolment,
            Set<Class<? extends TuitionConditionRule>> exclude) {
        return isCustomized() || isDefaultPaymentPlan() || getTuitionConditionRulesSet().stream()
                .allMatch(c -> !exclude.contains(c.getClass()) && c.isValidTo(registration, executionYear, enrolment));
    }

    public static boolean firstTimeStudent(final Registration registration, final ExecutionYear executionYear) {
        return registration.isFirstTime(executionYear);
    }

    public static Integer curricularYear(final Registration registration, final ExecutionYear executionYear) {
        return registration.getCurricularYear(executionYear);
    }

    public static Set<Integer> semestersWithEnrolments(final Registration registration, final ExecutionYear executionYear) {
        return registration.getEnrolments(executionYear).stream().map(e -> e.getExecutionInterval().getChildOrder())
                .collect(Collectors.toSet());
    }

    @Atomic
    public static TuitionPaymentPlan create(final TuitionPaymentPlanBean tuitionPaymentPlanBean) {
        return new TuitionPaymentPlan(tuitionPaymentPlanBean);
    }

    public static TuitionPaymentPlan copy(final TuitionPaymentPlan tuitionPaymentPlanToCopy,
            final ExecutionYear toExecutionYear) {
        return new TuitionPaymentPlan(tuitionPaymentPlanToCopy, toExecutionYear);
    }

    public boolean equalsTuitionPlanConditions(TuitionPaymentPlan plan) {
        return this.containsTuitionPlanConditions(plan) && plan.containsTuitionPlanConditions(this);
    }

    public boolean containsTuitionPlanConditions(TuitionPaymentPlan plan) {

        for (TuitionConditionRule otherCondition : plan.getTuitionConditionRulesSet()) {
            TuitionConditionRule condition = getTuitionConditionRule(otherCondition.getClass());
            if (condition == null || !condition.containsRule(otherCondition)) {
                return false;
            }
        }
        return true;
    }

}
