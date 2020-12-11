/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * * Neither the name of Quorum Born IT nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 * * Universidade de Lisboa and its respective subsidiary Serviços Centrais da Universidade
 * de Lisboa (Departamento de Informática), hereby referred to as the Beneficiary, is the
 * sole demonstrated end-user and ultimately the only beneficiary of the redistributed binary
 * form and/or source code.
 * * The Beneficiary is entrusted with either the binary form, the source code, or both, and
 * by accepting it, accepts the terms of this License.
 * * Redistribution of any binary form and/or source code is only allowed in the scope of the
 * Universidade de Lisboa FenixEdu(™)’s implementation projects.
 * * This license and conditions of redistribution of source code/binary can only be reviewed
 * by the Steering Comittee of FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT” BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.academictreasury.domain.debtGeneration.strategies;

import static org.fenixedu.academictreasury.domain.debtGeneration.IAcademicDebtGenerationRuleStrategy.findActiveDebitEntries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
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
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class CreateDebtsStrategy implements IAcademicDebtGenerationRuleStrategy {

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
        return true;
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

    @Override
    @Atomic(mode = TxMode.READ)
    public List<AcademicDebtGenerationProcessingResult> process(final AcademicDebtGenerationRule rule) {

        if (!rule.isActive()) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.not.active.to.process");
        }

        final List<AcademicDebtGenerationProcessingResult> resultList = Lists.newArrayList();
        for (final DegreeCurricularPlan degreeCurricularPlan : rule.getDegreeCurricularPlansSet()) {
            for (final Registration registration : degreeCurricularPlan.getRegistrations()) {

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

    static private boolean isToDiscard(final AcademicDebtGenerationRule rule, final Registration registration) {

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

    static private boolean isRuleWithOneEntryForcingCreation(final AcademicDebtGenerationRule rule) {
        return rule.isWithAtLeastOneForceCreationEntry();
    }

    @Atomic(mode = TxMode.WRITE)
    private void processDebtsForRegistration(final AcademicDebtGenerationRule rule, final Registration registration) {

        // For each product try to grab or create if requested
        final Set<DebitEntry> debitEntries = Sets.newHashSet();

        DebitEntry grabbedDebitEntry = null;
        for (final AcademicDebtGenerationRuleEntry entry : rule
                .getOrderedAcademicDebtGenerationRuleEntries() /* rule.getAcademicDebtGenerationRuleEntriesSet() */) {
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

        DebitNote debitNote = grabPreparingOrCreateDebitNote(debitEntries);

        for (final DebitEntry debitEntry : debitEntries) {
            if (debitEntry.getFinantialDocument() == null) {
                debitEntry.setFinantialDocument(debitNote);
            }

            if (debitNote.getPayorDebtAccount() == null && debitEntry.getPayorDebtAccount() != null) {
                debitNote.updatePayorDebtAccount(debitEntry.getPayorDebtAccount());
            }

            if (debitEntry.getDebtAccount() != debitNote.getDebtAccount()) {
                throw new AcademicTreasuryDomainException(
                        "error.AcademicDebtGenerationRule.debitEntry.debtAccount.not.equal.to.debitNote.debtAccount");
            }
        }

        if (debitNote.getFinantialDocumentEntriesSet().isEmpty()) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.debit.note.without.debit.entries");
        }

        if (rule.isAggregateAllOrNothing()) {
            for (final DebitEntry debitEntry : debitEntries) {
                if (debitEntry.getFinantialDocument() != debitNote) {
                    throw new AcademicTreasuryDomainException(
                            "error.AcademicDebtGenerationRule.debit.entries.not.aggregated.on.same.debit.note");
                }
            }

            // Check if all configured produts are in debitNote
            for (final Product product : rule.getAcademicDebtGenerationRuleEntriesSet().stream()
                    .map(AcademicDebtGenerationRuleEntry::getProduct).collect(Collectors.toSet())) {
                if (debitNote.getDebitEntries().filter(l -> l.getProduct() == product).count() == 0) {
                    throw new AcademicTreasuryDomainException(
                            "error.AcademicDebtGenerationRule.debit.entries.not.aggregated.on.same.debit.note");
                }
            }
        }

        if (rule.isEventDebitEntriesMustEqualRuleProducts()) {
            final TreasuryEvent treasuryEvent = debitEntries.iterator().next().getTreasuryEvent();

            // First ensure all academic debitEntries all from same academic event
            for (final DebitEntry db : debitEntries) {
                if (db.getTreasuryEvent() != treasuryEvent) {
                    throw new AcademicTreasuryDomainException(
                            "error.AcademicDebtGenerationRule.debitEntry.not.from.same.academic.event");
                }

                final Product interestProduct = TreasurySettings.getInstance().getInterestProduct();
                final Set<DebitEntry> treasuryEventDebitEntries = DebitEntry.findActive(treasuryEvent)
                        .filter(d -> d.getProduct() != interestProduct).collect(Collectors.<DebitEntry> toSet());

                if (!treasuryEventDebitEntries.equals(debitEntries)) {
                    throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRule.not.all.debitEntries.aggregated");
                }
            }
        }
    }

    private DebitEntry grabOrCreateDebitEntryForAcademicTax(final AcademicDebtGenerationRule rule,
            final Registration registration, final AcademicDebtGenerationRuleEntry entry) {
        final Product product = entry.getProduct();
        final ExecutionYear executionYear = rule.getExecutionYear();
        final AcademicTax academicTax = AcademicTax.findUnique(product).get();

        {
            AcademicTreasuryEvent t = AcademicTaxServices.findAcademicTreasuryEvent(registration, executionYear, academicTax);
            if (t == null || !t.isChargedWithDebitEntry()) {
                if (!entry.isCreateDebt()) {
                    return null;
                }

                boolean forceCreation =
                        entry.isCreateDebt() 
                        && entry.isForceCreation() 
                        && registration.getLastRegistrationState(executionYear) != null
                        && registration.getLastRegistrationState(executionYear).isActive()
                        && (!entry.isLimitToRegisteredOnExecutionYear() || registration.isFirstTime(rule.getExecutionYear()));

                AcademicTaxServices.createAcademicTaxForEnrolmentDateAndDefaultFinantialEntity(registration, executionYear, academicTax, forceCreation);
            }
        }

        final PersonCustomer customer = registration.getPerson().getPersonCustomer();

        if (customer == null) {
            return null;
        }

        final AcademicTreasuryEvent t = AcademicTaxServices.findAcademicTreasuryEvent(registration, executionYear, academicTax);

        if (t != null && t.isChargedWithDebitEntry()) {
            return findActiveDebitEntries(customer, t).filter(d -> d.isInDebt()).findFirst().orElse(null);
        }

        return null;
    }

    private DebitEntry grabOrCreateDebitEntryForTuition(final AcademicDebtGenerationRule rule, final Registration registration,
            final AcademicDebtGenerationRuleEntry entry) {
        final Product product = entry.getProduct();
        final ExecutionYear executionYear = rule.getExecutionYear();

        // Is of tuition kind try to catch the tuition event
        {
            AcademicTreasuryEvent t =
                    TuitionServices.findAcademicTreasuryEventTuitionForRegistration(registration, executionYear);

            if (t == null || !t.isChargedWithDebitEntry(product)) {

                if (!entry.isCreateDebt()) {
                    return null;
                }

                boolean forceCreation =
                        entry.isCreateDebt() 
                        && entry.isForceCreation() 
                        && registration.getLastRegistrationState(executionYear) != null
                        && registration.getLastRegistrationState(executionYear).isActive()
                        && (!entry.isLimitToRegisteredOnExecutionYear() || registration.isFirstTime(rule.getExecutionYear()));
                
                if (entry.isToCreateAfterLastRegistrationStateDate()) {
                    final LocalDate lastRegisteredStateDate = TuitionServices.lastRegisteredDate(registration, executionYear);
                    if (lastRegisteredStateDate == null) {
                        return null;
                    } else if (lastRegisteredStateDate.isAfter(new LocalDate())) {
                        return null;
                    } else {
                        TuitionServices.createInferedTuitionForRegistration(registration, executionYear, lastRegisteredStateDate,
                                forceCreation);
                    }
                } else {
                    final LocalDate enrolmentDate = TuitionServices.enrolmentDate(registration, executionYear, forceCreation);
                    TuitionServices.createInferedTuitionForRegistration(registration, executionYear, enrolmentDate, forceCreation);
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

        return findActiveDebitEntries(customer, t, product).filter(d -> d.isInDebt()).findFirst().orElse(null);
    }

    private DebitNote grabPreparingOrCreateDebitNote(final Set<DebitEntry> debitEntries) {

        for (final DebitEntry debitEntry : debitEntries) {
            if (debitEntry.getFinantialDocument() != null && debitEntry.getFinantialDocument().isPreparing()) {
                return (DebitNote) debitEntry.getFinantialDocument();
            }
        }

        final DebitNote debitNote =
                DebitNote
                        .create(debitEntries.iterator().next().getDebtAccount(),
                                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(),
                                        debitEntries.iterator().next().getDebtAccount().getFinantialInstitution()).get(),
                                new DateTime());

        return debitNote;
    }

}
