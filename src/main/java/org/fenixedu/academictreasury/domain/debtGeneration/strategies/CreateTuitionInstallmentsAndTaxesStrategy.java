package org.fenixedu.academictreasury.domain.debtGeneration.strategies;

import static org.fenixedu.academictreasury.domain.debtGeneration.IAcademicDebtGenerationRuleStrategy.findActiveDebitEntries;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtEntriesAggregationInDebitNoteType;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtGenerationProcessingResult;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtGenerationRule;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtGenerationRuleEntry;
import org.fenixedu.academictreasury.domain.debtGeneration.IAcademicDebtGenerationRuleStrategy;
import org.fenixedu.academictreasury.domain.emoluments.AcademicTax;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.services.AcademicTaxServices;
import org.fenixedu.academictreasury.services.TuitionServices;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class CreateTuitionInstallmentsAndTaxesStrategy implements IAcademicDebtGenerationRuleStrategy {

    private static Logger logger = LoggerFactory.getLogger(CreateTuitionInstallmentsAndTaxesStrategy.class);

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
        return true;
    }

    @Override
    public boolean isToCreateDebitEntries() {
        return true;
    }

    @Override
    public boolean isToApplyEventDebitEntriesMustEqualRuleProducts() {
        return false;
    }

    @Override
    public boolean isToAggregateDebitEntries() {
        return true;
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
        return true;
    }

    @Override
    public boolean isToApplyOptionForEntriesAggregationInDebitNote() {
        return true;
    }

    @Override
    public boolean isAggregateAllOrNothingDefaultValue() {
        return false;
    }

    @Override
    @Atomic(mode = TxMode.READ)
    public List<AcademicDebtGenerationProcessingResult> process(AcademicDebtGenerationRule rule) {

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
                    processDebtsForRegistration(rule, registration);
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

    @Override
    public List<AcademicDebtGenerationProcessingResult> process(AcademicDebtGenerationRule rule, Registration registration) {
        if (!rule.isActive()) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.not.active.to.process");
        }

        final AcademicDebtGenerationProcessingResult result = new AcademicDebtGenerationProcessingResult(rule, registration);
        try {

            if (isToDiscard(rule, registration)) {
                return Lists.newArrayList();
            }

            processDebtsForRegistration(rule, registration);
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

    @Atomic(mode = TxMode.WRITE)
    private void processDebtsForRegistration(AcademicDebtGenerationRule rule, Registration registration) {

        // For each product try to grab or create if requested
        final Set<DebitEntry> debitEntries = Sets.newHashSet();

        DebitEntry grabbedDebitEntry = null;
        for (final AcademicDebtGenerationRuleEntry entry : rule.getOrderedAcademicDebtGenerationRuleEntries() /* rule.getAcademicDebtGenerationRuleEntriesSet() */) {
            final Product product = entry.getProduct();

            if (AcademicTreasurySettings.getInstance().getTuitionProductGroup() == product.getProductGroup()) {
                grabbedDebitEntry = grabOrCreateDebitEntryForTuition(rule, registration, entry);
            } else if (AcademicTax.findUnique(product).isPresent()) {
                // Check if the product is an academic tax
                grabbedDebitEntry = grabOrCreateDebitEntryForAcademicTax(rule, registration, entry);
            }

            if (grabbedDebitEntry != null) {
                debitEntries.add(grabbedDebitEntry);
            }
        }

        if (debitEntries.isEmpty()) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.empty.debitEntries.to.process");
        }

        if (!rule.isAggregateOnDebitNote()) {
            return;
        }

        if (rule.getEntriesAggregationInDebitNoteType() == AcademicDebtEntriesAggregationInDebitNoteType.AGGREGATE_IN_UNIQUE_DEBIT_NOTE) {
            Map<DebtAccount, DebitNote> debitNotesMap = grabPreparingOrCreateDebitNotes(rule, debitEntries);

            for (final DebitEntry debitEntry : debitEntries) {

                if (debitEntry.getFinantialDocument() == null) {
                    DebitNote debitNote = debitNotesMap.get(
                            debitEntry.getPayorDebtAccount() != null ? debitEntry.getPayorDebtAccount() : debitEntry.getDebtAccount());
                    debitEntry.addToFinantialDocument(debitNote);
                }

                if (debitEntry.getDebtAccount() != debitEntry.getFinantialDocument().getDebtAccount()) {
                    throw new AcademicTreasuryDomainException(
                            "error.AcademicDebtGenerationRule.debitEntry.debtAccount.not.equal.to.debitNote.debtAccount");
                }
            }

            for (DebitNote debitNote : debitNotesMap.values()) {
                if (debitNote.getFinantialDocumentEntriesSet().isEmpty()) {
                    throw new AcademicTreasuryDomainException(
                            "error.AcademicDebtGenerationRule.debit.note.without.debit.entries");
                }
            }
        } else if (rule.getEntriesAggregationInDebitNoteType() == AcademicDebtEntriesAggregationInDebitNoteType.AGGREGATE_IN_INDIVIDUAL_DEBIT_NOTE) {
            for (final DebitEntry debitEntry : debitEntries) {

                if (debitEntry.getFinantialDocument() == null) {
                    DebtAccount debtAccountOwnerOfDebitNote =
                            debitEntry.getPayorDebtAccount() != null ? debitEntry.getPayorDebtAccount() : debitEntry.getDebtAccount();

                    DocumentNumberSeries documentNumberSeries =
                            DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(),
                                    rule.getFinantialEntity());

                    DebitNote debitNote = DebitNote.create(debitEntry.getFinantialEntity(), debitEntry.getDebtAccount(), null,
                            documentNumberSeries, new DateTime(), new LocalDate(), null, Collections.emptyMap(), null, null);

                    if (debtAccountOwnerOfDebitNote != debitEntry.getDebtAccount()) {
                        debitNote.updatePayorDebtAccount(debtAccountOwnerOfDebitNote);
                    }

                    debitEntry.addToFinantialDocument(debitNote);
                }
            }
        }

        if (rule.isAggregateAllOrNothing()) {
            // Check if all configured produts are in debitEntries
            for (Product product : rule.getAcademicDebtGenerationRuleEntriesSet().stream()
                    .map(AcademicDebtGenerationRuleEntry::getProduct).collect(Collectors.toSet())) {
                if (debitEntries.stream().noneMatch(de -> de.getProduct() == product)) {
                    throw new AcademicTreasuryDomainException(
                            "error.AcademicDebtGenerationRule.debit.entries.not.aggregated.on.same.debit.note");
                }
            }
        }

    }

    private DebitEntry grabOrCreateDebitEntryForAcademicTax(AcademicDebtGenerationRule rule, Registration registration,
            AcademicDebtGenerationRuleEntry entry) {
        Product product = entry.getProduct();
        ExecutionYear executionYear = rule.getExecutionYear();
        AcademicTax academicTax = AcademicTax.findUnique(product).get();

        {
            AcademicTreasuryEvent t = AcademicTaxServices.findAcademicTreasuryEvent(registration, executionYear, academicTax);
            if (t == null || !t.isChargedWithDebitEntry()) {
                if (!entry.isCreateDebt()) {
                    return null;
                }

                boolean forceCreation = entry.isCreateDebt() && entry.isForceCreation() && registration.getLastRegistrationState(
                        executionYear) != null && registration.getLastRegistrationState(executionYear)
                        .isActive() && (!entry.isLimitToRegisteredOnExecutionYear() || registration.isFirstTime(
                        rule.getExecutionYear()));

                AcademicTaxServices.createAcademicTaxForEnrolmentDateAndDefaultFinantialEntity(registration, executionYear,
                        academicTax, forceCreation);
            }
        }

        PersonCustomer customer = registration.getPerson().getPersonCustomer();

        if (customer == null) {
            return null;
        }

        AcademicTreasuryEvent t = AcademicTaxServices.findAcademicTreasuryEvent(registration, executionYear, academicTax);

        if (t != null && t.isChargedWithDebitEntry()) {
            return findActiveDebitEntries(customer, t).findFirst().orElse(null);
        }

        return null;
    }

    private DebitEntry grabOrCreateDebitEntryForTuition(AcademicDebtGenerationRule rule, Registration registration,
            AcademicDebtGenerationRuleEntry entry) {
        Product product = entry.getProduct();
        ExecutionYear executionYear = rule.getExecutionYear();

        // Is of tuition kind try to catch the tuition event
        {
            AcademicTreasuryEvent t =
                    TuitionServices.findAcademicTreasuryEventTuitionForRegistration(registration, executionYear);

            if (t == null || !t.isChargedWithDebitEntry(product)) {

                if (!entry.isCreateDebt()) {
                    return null;
                }

                boolean forceCreation = entry.isCreateDebt() && entry.isForceCreation() && registration.getLastRegistrationState(
                        executionYear) != null && registration.getLastRegistrationState(executionYear)
                        .isActive() && (!entry.isLimitToRegisteredOnExecutionYear() || registration.isFirstTime(
                        rule.getExecutionYear()));

                if (entry.isToCreateAfterLastRegistrationStateDate()) {
                    final LocalDate lastRegisteredStateDate = TuitionServices.lastRegisteredDate(registration, executionYear);
                    if (lastRegisteredStateDate == null) {
                        return null;
                    } else if (lastRegisteredStateDate.isAfter(new LocalDate())) {
                        return null;
                    } else {
                        TuitionServices.createInferedTuitionForRegistration(registration, executionYear, lastRegisteredStateDate,
                                forceCreation, true, Collections.singleton(entry.getProduct()), true);
                    }
                } else {
                    final LocalDate enrolmentDate = TuitionServices.enrolmentDate(registration, executionYear, forceCreation);
                    TuitionServices.createInferedTuitionForRegistration(registration, executionYear, enrolmentDate, forceCreation,
                            true, Collections.singleton(entry.getProduct()), true);
                }
            }
        }

        if (TuitionServices.findAcademicTreasuryEventTuitionForRegistration(registration, executionYear) == null) {
            // Did not create exit with nothing
            return null;
        }

        final AcademicTreasuryEvent t =
                TuitionServices.findAcademicTreasuryEventTuitionForRegistration(registration, executionYear);

        if (!t.isChargedWithDebitEntry(product)) {
            return null;
        }

        final PersonCustomer customer = registration.getPerson().getPersonCustomer();
        if (customer == null) {
            return null;
        }

        return findActiveDebitEntries(customer, t, product).findFirst().orElse(null);
    }

    private Set<Registration> getRegistrations(DegreeCurricularPlan dcp) {
        final Set<Registration> registrations = new HashSet<>();

        for (StudentCurricularPlan studentCurricularPlan : dcp.getActiveStudentCurricularPlans()) {
            registrations.add(studentCurricularPlan.getRegistration());
        }

        return registrations;
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

        // Discard registrations not active and with no enrolments
        if (!registration.hasAnyActiveState(year)) {
            return true;
        }

        if (registration.getRegistrationDataByExecutionYearSet().stream().noneMatch(i -> i.getExecutionYear() == year)) {

            // only return is this rule has not entry that forces creation
            if (!isRuleWithOneEntryForcingCreation(rule)) {
                return true;
            }
        }

        return false;
    }

    private boolean isRuleWithOneEntryForcingCreation(final AcademicDebtGenerationRule rule) {
        return rule.isWithAtLeastOneForceCreationEntry();
    }

    private Map<DebtAccount, DebitNote> grabPreparingOrCreateDebitNotes(AcademicDebtGenerationRule rule,
            final Set<DebitEntry> debitEntries) {
        Map<DebtAccount, DebitNote> result = new HashMap<>();

        for (final DebitEntry debitEntry : debitEntries) {
            if (debitEntry.getFinantialDocument() != null && debitEntry.getFinantialDocument().isPreparing()) {
                DebitNote debitNote = (DebitNote) debitEntry.getFinantialDocument();
                DebtAccount ownerDebtAccount =
                        debitNote.getPayorDebtAccount() != null ? debitNote.getPayorDebtAccount() : debitNote.getDebtAccount();
                result.put(ownerDebtAccount, debitNote);
            } else if (debitEntry.getFinantialDocument() == null) {
                DebtAccount debtAccountOwnerOfDebitNote =
                        debitEntry.getPayorDebtAccount() != null ? debitEntry.getPayorDebtAccount() : debitEntry.getDebtAccount();

                if (result.get(debtAccountOwnerOfDebitNote) == null) {
                    DocumentNumberSeries documentNumberSeries =
                            DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(),
                                    rule.getFinantialEntity());

                    DebitNote debitNote = DebitNote.create(debitEntry.getFinantialEntity(), debitEntry.getDebtAccount(), null,
                            documentNumberSeries, new DateTime(), new LocalDate(), null, Collections.emptyMap(), null, null);

                    if (debtAccountOwnerOfDebitNote != debitEntry.getDebtAccount()) {
                        debitNote.updatePayorDebtAccount(debtAccountOwnerOfDebitNote);
                    }

                    result.put(debtAccountOwnerOfDebitNote, debitNote);
                }
            }
        }

        // Ensure all debit notes are in preparing state
        if (!result.values().stream().allMatch(d -> d.isPreparing())) {
            throw new RuntimeException(
                    "error.CreateDebtsStrategy.grabPreparingOrCreateDebitNotes.debitNote.preparing.state.failed");
        }

        return result;
    }

}
