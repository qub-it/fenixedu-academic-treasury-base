/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/> All rights reserved.
 *
 * Redistribution and use in source and binary forms, without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution. * Neither the name of Quorum Born IT nor
 * the names of its contributors may be used to endorse or promote products derived from this software without specific prior
 * written permission. * Universidade de Lisboa and its respective subsidiary Serviços Centrais da Universidade de Lisboa
 * (Departamento de Informática), hereby referred to as the Beneficiary, is the sole demonstrated end-user and ultimately the only
 * beneficiary of the redistributed binary form and/or source code. * The Beneficiary is entrusted with either the binary form,
 * the source code, or both, and by accepting it, accepts the terms of this License. * Redistribution of any binary form and/or
 * source code is only allowed in the scope of the Universidade de Lisboa FenixEdu(™)’s implementation projects. * This license
 * and conditions of redistribution of source code/binary can only be reviewed by the Steering Comittee of FenixEdu(™)
 * <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL “Quorum Born IT” BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.academictreasury.domain.debtGeneration.strategies;

import static org.fenixedu.academictreasury.domain.debtGeneration.IAcademicDebtGenerationRuleStrategy.findActiveDebitEntries;

import java.util.*;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
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
import org.fenixedu.treasury.domain.FinantialEntity;
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

public class AggregateDebtsStrategy implements IAcademicDebtGenerationRuleStrategy {

    private static Logger logger = LoggerFactory.getLogger(AggregateDebtsStrategy.class);

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
        return false;
    }

    private static final List<String> MESSAGES_TO_IGNORE =
            Lists.newArrayList("error.AcademicDebtGenerationRule.debit.note.without.debit.entries");

    @Override
    @Atomic(mode = TxMode.READ)
    public List<AcademicDebtGenerationProcessingResult> process(final AcademicDebtGenerationRule rule) {

        if (!rule.isActive()) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.not.active.to.process");
        }

        final List<AcademicDebtGenerationProcessingResult> resultList = Lists.newArrayList();
        for (final DegreeCurricularPlan degreeCurricularPlan : rule.getDegreeCurricularPlansSet()) {
            for (final Registration registration : degreeCurricularPlan.getRegistrations()) {

                if (!rule.isRuleToApply(registration)) {
                    continue;
                }

                if (registration.getStudentCurricularPlan(rule.getExecutionYear()) == null) {
                    continue;
                }

                if (!rule.getDegreeCurricularPlansSet()
                        .contains(registration.getStudentCurricularPlan(rule.getExecutionYear()).getDegreeCurricularPlan())) {
                    continue;
                }

                final AcademicDebtGenerationProcessingResult result =
                        new AcademicDebtGenerationProcessingResult(rule, registration);
                resultList.add(result);

                try {
                    processDebtsForRegistration(rule, registration);
                } catch (final AcademicTreasuryDomainException e) {
                    result.markException(e);
                    if (!MESSAGES_TO_IGNORE.contains(e.getMessage())) {
                        logger.debug(e.getMessage());
                    }
                } catch (final Exception e) {
                    result.markException(e);
                    e.printStackTrace();
                }
            }
        }

        return resultList;
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

            if (!rule.isRuleToApply(registration)) {
                return Lists.newArrayList();
            }

            if (registration.getStudentCurricularPlan(rule.getExecutionYear()) == null) {
                return Lists.newArrayList();
            }

            if (!rule.getDegreeCurricularPlansSet()
                    .contains(registration.getStudentCurricularPlan(rule.getExecutionYear()).getDegreeCurricularPlan())) {
                return Lists.newArrayList();
            }

            processDebtsForRegistration(rule, registration);
            result.markProcessingEndDateTime();
        } catch (final AcademicTreasuryDomainException e) {
            result.markException(e);
            if (!MESSAGES_TO_IGNORE.contains(e.getMessage())) {
                logger.info(e.getMessage());
            }
        } catch (final Exception e) {
            result.markException(e);
            e.printStackTrace();
        }

        return Lists.newArrayList(result);
    }

    @Atomic(mode = TxMode.WRITE)
    private void processDebtsForRegistration(final AcademicDebtGenerationRule rule, final Registration registration) {

        // For each product try to grab or create if requested
        final Set<DebitEntry> debitEntries = Sets.newHashSet();

        DebitEntry grabbedDebitEntry = null;
        for (final AcademicDebtGenerationRuleEntry entry : rule.getAcademicDebtGenerationRuleEntriesSet()) {
            final Product product = entry.getProduct();

            if (AcademicTreasurySettings.getInstance().getTuitionProductGroup() == product.getProductGroup()) {
                grabbedDebitEntry = grabDebitEntryForTuition(rule, registration, entry);
            } else if (AcademicTax.findUnique(product).isPresent()) {
                // Check if the product is an academic tax
                grabbedDebitEntry = grabDebitEntryForAcademicTax(rule, registration, entry);
            }

            if (grabbedDebitEntry != null) {
                debitEntries.add(grabbedDebitEntry);
            }
        }

        if (debitEntries.isEmpty()) {
            return;
        }

        if (!rule.isAggregateOnDebitNote()) {
            return;
        }

        Map<DebtAccount, DebitNote> debitNotesMap = grabPreparingOrCreateDebitNote(rule, debitEntries);

        for (final DebitEntry debitEntry : debitEntries) {
            if (debitEntry.getFinantialDocument() == null) {
                DebtAccount debtAccountOwnerOfDebitNote =
                        debitEntry.getPayorDebtAccount() != null ? debitEntry.getPayorDebtAccount() : debitEntry.getDebtAccount();
                DebitNote debitNote = debitNotesMap.get(debtAccountOwnerOfDebitNote);
                debitEntry.addToFinantialDocument(debitNote);
            }

            if (debitEntry.getDebtAccount() != debitEntry.getDebitNote().getDebtAccount()) {
                throw new AcademicTreasuryDomainException(
                        "error.AcademicDebtGenerationRule.debit.entry.debtAccount.not.equal.debit.note.debtAccount");
            }
        }

        debitNotesMap.values().stream().filter(debitNote -> debitNote.getFinantialDocumentEntriesSet().isEmpty()).findAny()
                .ifPresent(d -> {
                    throw new AcademicTreasuryDomainException(
                            "error.AcademicDebtGenerationRule.debit.note.without.debit.entries");
                });

        if (rule.isAggregateAllOrNothing()) {
            for (final DebitEntry debitEntry : debitEntries) {
                if (!debitNotesMap.containsValue(debitEntry.getFinantialDocument())) {
                    throw new AcademicTreasuryDomainException(
                            "error.AcademicDebtGenerationRule.debit.entries.not.aggregated.on.same.debit.note");
                }
            }

            // Check if all configured produts are in debitNote
            for (final Product product : rule.getAcademicDebtGenerationRuleEntriesSet().stream()
                    .map(AcademicDebtGenerationRuleEntry::getProduct).collect(Collectors.toSet())) {

                if (debitNotesMap.values().stream().flatMap(d -> d.getDebitEntriesSet().stream())
                        .noneMatch(de -> de.getProduct() == product)) {
                    throw new AcademicTreasuryDomainException(
                            "error.AcademicDebtGenerationRule.debit.entries.not.aggregated.on.same.debit.note");
                }
            }
        }
    }

    private DebitEntry grabDebitEntryForAcademicTax(final AcademicDebtGenerationRule rule, final Registration registration,
            final AcademicDebtGenerationRuleEntry entry) {
        PersonCustomer personCustomer = registration.getPerson().getPersonCustomer();
        if (personCustomer == null) {
            return null;
        }

        final Product product = entry.getProduct();
        final ExecutionYear executionYear = rule.getExecutionYear();
        final AcademicTax academicTax = AcademicTax.findUnique(product).get();

        AcademicTreasuryEvent t = AcademicTaxServices.findAcademicTreasuryEvent(registration, executionYear, academicTax);
        if (t == null || !t.isChargedWithDebitEntry()) {
            return null;
        }

        if (t != null && t.isChargedWithDebitEntry()) {
            // ANIL 2025-01-27 (#qubIT-Fenix-6562)
            //
            // Instead of grabbing debit entries that are in debt,
            // grab if they are preparing or without debit note.
            // This will avoid fetching those already closed in
            // debit note, and will fetch those that are totally
            // exempted
            return findActiveDebitEntries(personCustomer, t).filter(
                    d -> d.getFinantialDocument() == null || d.getFinantialDocument().isPreparing()).findFirst().orElse(null);
        }

        return null;
    }

    private DebitEntry grabDebitEntryForTuition(final AcademicDebtGenerationRule rule, final Registration registration,
            final AcademicDebtGenerationRuleEntry entry) {
        PersonCustomer personCustomer = registration.getPerson().getPersonCustomer();
        if (personCustomer == null) {
            return null;
        }

        final Product product = entry.getProduct();
        final ExecutionYear executionYear = rule.getExecutionYear();

        // Is of tuition kind try to catch the tuition event
        final AcademicTreasuryEvent t =
                TuitionServices.findAcademicTreasuryEventTuitionForRegistration(registration, executionYear);

        if (t == null || !t.isChargedWithDebitEntry(product)) {
            return null;
        }

        if (!t.isChargedWithDebitEntry(product)) {
            return null;
        }

        // ANIL 2025-01-27 (#qubIT-Fenix-6562)
        //
        // Instead of grabbing debit entries that are in debt,
        // grab if they are preparing or without debit note.
        // This will avoid fetching those already closed in
        // debit note, and will fetch those that are totally
        // exempted
        return findActiveDebitEntries(personCustomer, t, product).filter(
                d -> d.getFinantialDocument() == null || d.getFinantialDocument().isPreparing()).findFirst().orElse(null);
    }

    // ANIL 2025-01-27 (#qubIT-Fenix-6562)
    //
    // Take into account the debit entries allocated for payor entities
    private Map<DebtAccount, DebitNote> grabPreparingOrCreateDebitNote(final AcademicDebtGenerationRule rule,
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

                FinantialEntity finantialEntity = debitEntry.getFinantialEntity();
                DocumentNumberSeries defaultDocumentNumberSeries =
                        DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);

                final DebitNote debitNote = DebitNote.create(finantialEntity, debitEntry.getDebtAccount(),
                        debtAccountOwnerOfDebitNote != debitEntry.getDebtAccount() ? debtAccountOwnerOfDebitNote : null,
                        defaultDocumentNumberSeries, new DateTime(), new LocalDate(), null, Collections.emptyMap(), null, null);

                result.put(debtAccountOwnerOfDebitNote, debitNote);
            }
        }

        return result;
    }

}
