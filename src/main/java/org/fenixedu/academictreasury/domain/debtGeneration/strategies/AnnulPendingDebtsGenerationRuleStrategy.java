package org.fenixedu.academictreasury.domain.debtGeneration.strategies;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtGenerationProcessingResult;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtGenerationRule;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtGenerationRuleEntry;
import org.fenixedu.academictreasury.domain.debtGeneration.IAcademicDebtGenerationRuleStrategy;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class AnnulPendingDebtsGenerationRuleStrategy implements IAcademicDebtGenerationRuleStrategy {

    private static Logger logger = LoggerFactory.getLogger(CreateDebtsStrategy.class);

    @Override
    public boolean isAppliedOnTuitionDebitEntries() {
        return true;
    }

    @Override
    public boolean isAppliedOnAcademicTaxDebitEntries() {
        return true;
    }

    @Override
    public boolean isAppliedOnOtherDebitEntries() {
        return false;
    }

    @Override
    public boolean isToCreateDebitEntries() {
        return false;
    }

    @Override
    public boolean isToAggregateDebitEntries() {
        return false;
    }

    @Override
    public boolean isToCloseDebitNote() {
        return false;
    }

    @Override
    public boolean isToCreatePaymentReferenceCodes() {
        return false;
    }

    @Override
    public boolean isEntriesRequired() {
        return true;
    }

    @Override
    public boolean isToAlignAcademicTaxesDueDate() {
        return false;
    }

    @Override
    @Atomic(mode = TxMode.READ)
    public List<AcademicDebtGenerationProcessingResult> process(final AcademicDebtGenerationRule rule) {

        if (!rule.isActive()) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.not.active.to.process");
        }

        final List<AcademicDebtGenerationProcessingResult> resultList = Lists.newArrayList();
        for (final DegreeCurricularPlan degreeCurricularPlan : rule.getDegreeCurricularPlansSet()) {
            for (final Registration registration : getRegistrations(degreeCurricularPlan)) {

                if (isToDiscard(rule, registration)) {
                    continue;
                }

                final AcademicDebtGenerationProcessingResult result =
                        new AcademicDebtGenerationProcessingResult(rule, registration);
                resultList.add(result);
                try {
                    processAnnulmentOfDebtsForRegistration(rule, registration, result);
                    result.markProcessingEndDateTime();
                } catch (final AcademicTreasuryDomainException e) {
                    result.markException(e);
                    logger.debug(e.getMessage());
                } catch (final Exception e) {
                    result.markException(e);
                    e.printStackTrace();
                }
            }
        }

        return resultList;
    }

    private Set<Registration> getRegistrations(DegreeCurricularPlan degreeCurricularPlan) {
        return degreeCurricularPlan.getStudentCurricularPlansSet().stream().map(s -> s.getRegistration())
                .collect(Collectors.toSet());
    }

    @Override
    @Atomic(mode = TxMode.READ)
    public List<AcademicDebtGenerationProcessingResult> process(final AcademicDebtGenerationRule rule,
            final Registration registration) {
        if (!rule.isActive()) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.not.active.to.process");
        }

        final AcademicDebtGenerationProcessingResult result = new AcademicDebtGenerationProcessingResult(rule, registration);
        try {

            if (isToDiscard(rule, registration)) {
                return Lists.newArrayList();
            }

            processAnnulmentOfDebtsForRegistration(rule, registration, result);
            result.markProcessingEndDateTime();
        } catch (final AcademicTreasuryDomainException e) {
            result.markException(e);
            logger.debug(e.getMessage());
        } catch (final Exception e) {
            result.markException(e);
            e.printStackTrace();
        }

        return Lists.newArrayList(result);

    }

    private boolean isToDiscard(final AcademicDebtGenerationRule rule, final Registration registration) {

        if (!rule.isRuleToApply(registration)) {
            return true;
        }

        final ExecutionYear year = rule.getExecutionYear();
        final StudentCurricularPlan scp = registration.getStudentCurricularPlan(year);
        if (scp == null) {
            return true;
        }

        if (!rule.getDegreeCurricularPlansSet().contains(scp.getDegreeCurricularPlan())) {
            return true;
        }

        return false;
    }

    @Atomic(mode = TxMode.WRITE)
    private void processAnnulmentOfDebtsForRegistration(final AcademicDebtGenerationRule rule, final Registration registration,
            final AcademicDebtGenerationProcessingResult processingResult) {
        LocalDate now = new LocalDate();

        // For each product try to grab the pending debit entries
        final Set<DebitEntry> debitEntries = Sets.newHashSet();

        for (final AcademicDebtGenerationRuleEntry entry : rule.getOrderedAcademicDebtGenerationRuleEntries()) {
            registration.getAcademicTreasuryEventSet() //
                    .stream() //
                    .filter(te -> te.getExecutionYear() == rule.getExecutionYear()) //
                    .flatMap(te -> DebitEntry.findActive(te, entry.getProduct())) //
                    .filter(de -> de.isInDebt()) //
                    .filter(de -> de.isDueDateExpired(now)) //
                    .filter(de -> de.getFinantialDocument() == null || de.getFinantialDocument().isPreparing()) //
                    .filter(de -> allSplittedDebitEntriesArePreparing(de)) //
                    .collect(Collectors.toCollection(() -> debitEntries));
        }

        // Ensure the system does not annul beyond what is expected

        Set<Product> productsSet =
                rule.getAcademicDebtGenerationRuleEntriesSet().stream().map(e -> e.getProduct()).collect(Collectors.toSet());

        if (debitEntries.stream().anyMatch(de -> !productsSet.contains(de.getProduct()))) {
            throw new RuntimeException("error in collecting debit entries to annul, error in product");
        }

        if (debitEntries.stream().anyMatch(de -> !rule.getExecutionYear().getQualifiedName().equals(de.getExecutionYearName()))) {
            throw new RuntimeException("error in collecting debit entries to annul, error in execution year");
        }

        debitEntries.forEach(d -> {
            d.annulOnlyThisDebitEntryAndInterestsInBusinessContext(AcademicTreasuryConstants
                    .academicTreasuryBundle("label.AnnulPendingDebtsGenerationRuleStrategy.annuled.automatically"));

            processingResult.markRuleMadeUpdates();
            processingResult.appendRemarks(String.format("\t%s;%s;%s;%s;%s;\"%s\"", //
                    d.getExternalId(), //
                    d.getDebtAccount().getCustomer().getUiFiscalNumber(), //
                    d.getProduct().getCode(), //
                    d.getDueDate().toString("yyyy-MM-dd"), //
                    d.getTotalAmount().toString(), //
                    d.getDescription()));
        });

    }

    private boolean allSplittedDebitEntriesArePreparing(DebitEntry debitEntry) {
        return debitEntry.getAllSplittedDebitEntriesSet().stream() //
                .filter(d -> !d.isAnnulled()) //
                .allMatch(d -> d.isInDebt() && (d.getFinantialDocument() == null || d.getFinantialDocument().isPreparing()));
    }

}
