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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
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
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.services.TuitionServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class CloseDebtsStrategy implements IAcademicDebtGenerationRuleStrategy {

    private static Logger logger = LoggerFactory.getLogger(CloseDebtsStrategy.class);

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
        return true;
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

                if (!rule.isRuleToApply(registration)) {
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

        if (!rule.isRuleToApply(registration)) {
            return Lists.newArrayList();
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
            logger.debug(e.getMessage());
        } catch (final Exception e) {
            result.markException(e);
            e.printStackTrace();
        }

        return Lists.newArrayList(result);
    }

    @Atomic(mode = TxMode.WRITE)
    private void processDebtsForRegistration(AcademicDebtGenerationRule rule, Registration registration) {
        final Set<DebitEntry> debitEntriesSetForAlignment = Sets.newHashSet();

        for (final AcademicDebtGenerationRuleEntry entry : rule.getAcademicDebtGenerationRuleEntriesSet()) {
            final Product product = entry.getProduct();

            Set<DebitEntry> grabbedDebitEntries = null;
            if (AcademicTreasurySettings.getInstance().getTuitionProductGroup() == product.getProductGroup()) {
                grabbedDebitEntries = grabDebitEntryForTuitions(rule, registration, entry);
            } else if (AcademicTax.findUnique(product).isPresent()) {
                // Check if the product is an academic tax
                grabbedDebitEntries = grabDebitEntryForAcademicTax(rule, registration, entry);
            } else if (product == TreasurySettings.getInstance().getInterestProduct()) {
                grabbedDebitEntries = grabInterestDebitEntries(rule, registration);
            }

            if (grabbedDebitEntries != null) {
                debitEntriesSetForAlignment.addAll(grabbedDebitEntries);
            }
        }

        if (isToAlignAcademicTaxesDueDate() && rule.getAcademicTaxDueDateAlignmentType() != null) {
            rule.getAcademicTaxDueDateAlignmentType().applyDueDate(rule, debitEntriesSetForAlignment);
        }

        for (final DebitEntry grabbedDebitEntry : debitEntriesSetForAlignment) {
            if (grabbedDebitEntry.getFinantialDocument() == null || !grabbedDebitEntry.getFinantialDocument().isPreparing()) {
                continue;
            }

            final LocalDate dueDate = grabbedDebitEntry.getDueDate();
            if (dueDate.minusDays(rule.getDays()).isAfter(new LocalDate())) {
                continue;
            }

            final DebitNote debitNote = (DebitNote) grabbedDebitEntry.getFinantialDocument();

            if (!AcademicTreasuryConstants.isPositive(debitNote.getTotalAmount())) {
                continue;
            }

            debitNote.closeDocument();
        }
    }

    private Set<DebitEntry> grabInterestDebitEntries(AcademicDebtGenerationRule rule, Registration registration) {
        final Set<DebitEntry> result = new HashSet<>();

        Person person = registration.getStudent().getPerson();

        PersonCustomer.find(person).forEach(pc -> {
            pc.getDebtAccountsSet().forEach(da -> {
                DebitEntry.find(da).filter(de -> de.getProduct() == TreasurySettings.getInstance().getInterestProduct())
                        .filter(de -> de.getFinantialDocument() == null || de.getFinantialDocument().isPreparing())
                        .filter(de -> de.isInDebt()).filter(de -> de.getDebitEntry() != null)
                        .filter(de -> de.getDebitEntry().getTreasuryEvent() != null)
                        .filter(de -> de.getDebitEntry().getTreasuryEvent() instanceof AcademicTreasuryEvent)
                        .filter(de -> ((AcademicTreasuryEvent) de.getDebitEntry().getTreasuryEvent()).getExecutionYear() == rule
                                .getExecutionYear())
                        .collect(Collectors.toCollection(() -> result));
            });
        });

        for (DebitEntry debitEntry : result) {
            if (debitEntry.getFinantialDocument() == null) {
                DebtAccount debtAccount = debitEntry.getDebtAccount();
                DebitNote debitNote = DebitNote.create(debtAccount, DocumentNumberSeries
                        .findUniqueDefault(FinantialDocumentType.findForDebitNote(), debtAccount.getFinantialInstitution()).get(),
                        new DateTime());
                debitEntry.setFinantialDocument(debitNote);
            }
        }

        return result;
    }

    private Set<DebitEntry> grabDebitEntryForTuitions(final AcademicDebtGenerationRule rule, final Registration registration,
            final AcademicDebtGenerationRuleEntry entry) {
        IAcademicTreasuryPlatformDependentServices implementation =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        final PersonCustomer customer = implementation.personCustomer(registration.getPerson());
        if (customer == null) {
            return Sets.newHashSet();
        }

        final Product product = entry.getProduct();
        final ExecutionYear executionYear = rule.getExecutionYear();

        final AcademicTreasuryEvent t =
                TuitionServices.findAcademicTreasuryEventTuitionForRegistration(registration, executionYear);

        if (t == null || !t.isChargedWithDebitEntry(product)) {
            return Sets.newHashSet();
        }

        return findActiveDebitEntries(customer, t, product).collect(Collectors.<DebitEntry> toSet());
    }

    private Set<DebitEntry> grabDebitEntryForAcademicTax(final AcademicDebtGenerationRule rule, final Registration registration,
            final AcademicDebtGenerationRuleEntry entry) {
        IAcademicTreasuryPlatformDependentServices implementation =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        final PersonCustomer customer = implementation.personCustomer(registration.getPerson());
        if (customer == null) {
            return Sets.newHashSet();
        }

        final Product product = entry.getProduct();
        final ExecutionYear executionYear = rule.getExecutionYear();
        final AcademicTax academicTax = AcademicTax.findUnique(product).get();

        final AcademicTreasuryEvent t = AcademicTaxServices.findAcademicTreasuryEvent(registration, executionYear, academicTax);

        if (t != null && t.isChargedWithDebitEntry()) {
            return findActiveDebitEntries(customer, t).collect(Collectors.<DebitEntry> toSet());
        }

        return Sets.newHashSet();
    }

    private LocalDate maxDebitEntryDueDate(final DebitNote debitNote) {
        final LocalDate maxDate = debitNote.getDebitEntries().max(DebitEntry.COMPARE_BY_DUE_DATE).map(DebitEntry::getDueDate)
                .orElse(new LocalDate());
        return maxDate.isAfter(new LocalDate()) ? maxDate : new LocalDate();
    }

}
