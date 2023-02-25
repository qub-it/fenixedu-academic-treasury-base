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
package org.fenixedu.academictreasury.services;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.emoluments.AcademicTax;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.domain.tariff.AcademicTariff;
import org.fenixedu.academictreasury.dto.academictax.AcademicDebitEntryBean;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;

public class AcademicTaxServices {

    public static List<IAcademicTreasuryEvent> findAllTreasuryEventsForAcademicTaxes(final Registration registration,
            final ExecutionInterval executionYear) {
        return AcademicTreasuryEvent.findAllForAcademicTax(registration, executionYear)
                .collect(Collectors.<IAcademicTreasuryEvent> toList());
    }

    public static AcademicTreasuryEvent findAcademicTreasuryEvent(final Registration registration,
            final ExecutionInterval executionYear, final AcademicTax academicTax) {
        return AcademicTreasuryEvent.findUniqueForAcademicTax(registration, executionYear, academicTax).orElse(null);
    }

    public static AcademicTariff findAcademicTariff(final AcademicTax academicTax, final Registration registration,
            final LocalDate debtDate) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        final FinantialEntity finantialEntity =
                academicTreasuryServices.finantialEntityOfDegree(registration.getDegree(), debtDate);

        return AcademicTariff.findMatch(finantialEntity, academicTax.getProduct(), registration.getDegree(),
                debtDate.toDateTimeAtStartOfDay());
    }

    public static AcademicDebitEntryBean calculateAcademicTaxForDefaultFinantialEntity(
            final Registration registration, final ExecutionInterval executionYear, final AcademicTax academicTax,
            final LocalDate debtDate, final boolean forceCreation) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        
        final FinantialEntity finantialEntity = academicTreasuryServices.finantialEntityOfDegree(registration.getDegree(), debtDate);
        
        return calculateAcademicTax(finantialEntity, registration, executionYear, academicTax, debtDate, forceCreation);
    }
    
    public static AcademicDebitEntryBean calculateAcademicTax(final FinantialEntity finantialEntity,
            final Registration registration, final ExecutionInterval executionYear, final AcademicTax academicTax,
            final LocalDate debtDate, final boolean forceCreation) {

        if (!forceCreation && TuitionServices.normalEnrolmentsIncludingAnnuled(registration, executionYear).isEmpty()) {
            throw new AcademicTreasuryDomainException("error.AcademicTaxServices.calculateAcademicTax.not.enrolled");
        }

        if (!isAppliableOnRegistration(academicTax, registration, executionYear)) {
            throw new AcademicTreasuryDomainException(
                    "error.AcademicTaxServices.calculateAcademicTax.not.appliable.for.registration.and.execution.year");
        }

        final AcademicTariff academicTariff = AcademicTariff.findMatch(finantialEntity, academicTax.getProduct(),
                registration.getDegree(), debtDate.toDateTimeAtStartOfDay());

        if (academicTariff == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTaxServices.calculateAcademicTax.tariff.not.found",
                    debtDate.toString(TreasuryConstants.DATE_FORMAT));
        }
        
        final LocalizedString debitEntryName = AcademicTreasuryEvent.nameForAcademicTax(academicTax, registration, executionYear);
        final LocalDate dueDate = academicTariff.dueDate(debtDate);
        final Vat vat = academicTariff.vat(debtDate);
        final BigDecimal amount = academicTariff.amountToPay(0, 0, AcademicTreasuryConstants.DEFAULT_LANGUAGE, false);

        return new AcademicDebitEntryBean(debitEntryName, dueDate, vat.getTaxRate(), amount);
    }

    public static boolean isAcademicTaxCharged(final Registration registration, final ExecutionYear executionYear,
            final AcademicTax academicTax) {

        if (findAcademicTreasuryEvent(registration, executionYear, academicTax) == null) {
            return false;
        }

        final AcademicTreasuryEvent academicTreasuryEvent = findAcademicTreasuryEvent(registration, executionYear, academicTax);
        return academicTreasuryEvent.isCharged();
    }

    @Atomic
    public static boolean createAcademicTaxForEnrolmentDateAndDefaultFinantialEntity(final Registration registration,
            final ExecutionInterval executionYear, final AcademicTax academicTax, final boolean forceCreation) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        final LocalDate enrolmentDate = possibleEnrolmentDate(registration, executionYear);

        final FinantialEntity finantialEntity = academicTreasuryServices.finantialEntityOfDegree(registration.getDegree(), enrolmentDate);
        return createAcademicTax(finantialEntity, registration, executionYear, academicTax, enrolmentDate, forceCreation);
    }

    private static LocalDate possibleEnrolmentDate(Registration registration, ExecutionInterval executionYear) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        final RegistrationDataByExecutionYear data = academicTreasuryServices.findRegistrationDataByExecutionYear(registration, executionYear);

        if (data != null && data.getEnrolmentDate() != null) {
            return data.getEnrolmentDate();
        }

        // TODO ANIL 2023-02-16: This is applied with a temporary fix flag
        //
        // Remove as soon as everyone accepts the fix
        if(AcademicTreasurySettings.getInstance().getFixAcademicTaxEmolumentEnrolmentDate()) {
            return new LocalDate();
        } else {
            return executionYear.getBeginLocalDate();
        }
    }

    @Atomic
    public static boolean createAcademicTaxForDefaultFinantialEntity(final Registration registration,
            final ExecutionInterval executionYear, final AcademicTax academicTax, final LocalDate when, final boolean forceCreation) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        final FinantialEntity finantialEntity = academicTreasuryServices.finantialEntityOfDegree(registration.getDegree(), when);
        return createAcademicTax(finantialEntity, registration, executionYear, academicTax, when, forceCreation);
    }

    @Atomic
    public static boolean createAcademicTax(final FinantialEntity finantialEntity, final Registration registration,
            final ExecutionInterval executionYear, final AcademicTax academicTax, final LocalDate when, final boolean forceCreation) {
        if (!forceCreation && TuitionServices.normalEnrolmentsIncludingAnnuled(registration, executionYear).isEmpty()) {
            return false;
        }

        if (!isAppliableOnRegistration(academicTax, registration, executionYear)) {
            return false;
        }

        final AcademicTariff academicTariff = AcademicTariff.findMatch(finantialEntity, academicTax.getProduct(),
                registration.getDegree(), when.toDateTimeAtStartOfDay());

        if (academicTariff == null) {
            return false;
        }

        final Person person = registration.getPerson();
        final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);
        if (findAcademicTreasuryEvent(registration, executionYear, academicTax) == null) {

            if (Strings.isNullOrEmpty(addressFiscalCountryCode) || Strings.isNullOrEmpty(fiscalNumber)) {
                throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalInformation.required");
            }

            // Read person customer
            if (!PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).isPresent()) {
                PersonCustomer.create(person, addressFiscalCountryCode, fiscalNumber);
            }

            final PersonCustomer personCustomer = PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).get();
            if (!personCustomer.isActive()) {
                throw new AcademicTreasuryDomainException("error.PersonCustomer.not.active", addressFiscalCountryCode, fiscalNumber);
            }

            if (!DebtAccount.findUnique(finantialEntity.getFinantialInstitution(), personCustomer).isPresent()) {

                DebtAccount.create(finantialEntity.getFinantialInstitution(), personCustomer);
            }

            AcademicTreasuryEvent.createForAcademicTax(academicTax, registration, executionYear);
        }

        final AcademicTreasuryEvent academicTreasuryEvent = findAcademicTreasuryEvent(registration, executionYear, academicTax);

        if (academicTreasuryEvent.isChargedWithDebitEntry()) {
            return false;
        }

        final PersonCustomer personCustomer = PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).get();
        final DebtAccount debtAccount = DebtAccount.findUnique(finantialEntity.getFinantialInstitution(), personCustomer).get();

        academicTariff.createDebitEntryForAcademicTax(debtAccount, academicTreasuryEvent, when);

        return true;
    }

    public static boolean isRegistrationFirstYear(final Registration registration, final ExecutionInterval executionYear) {
        return registration.getRegistrationYear() == executionYear;
    }

    public static boolean isRegistrationSubsequentYear(final Registration registration, final ExecutionInterval executionYear) {
        return registration.getRegistrationYear().isBefore(executionYear);
    }

    public static boolean isAppliableOnRegistration(final AcademicTax academicTax, final Registration registration,
            final ExecutionInterval executionYear) {
        return (isRegistrationFirstYear(registration, executionYear) && academicTax.isAppliedOnRegistrationFirstYear())
                || (isRegistrationSubsequentYear(registration, executionYear)
                        && academicTax.isAppliedOnRegistrationSubsequentYears());
    }

    /* ***********
     * IMPROVEMENT
     * ***********
     */

    public static AcademicTreasuryEvent findAcademicTreasuryEventForImprovementTax(final Registration registration,
            final ExecutionYear executionYear) {
        return AcademicTreasuryEvent.findUniqueForImprovementTuition(registration, executionYear).orElse(null);
    }

    public static AcademicTariff findAcademicTariffForDefaultFinantialEntity(final EnrolmentEvaluation enrolmentEvaluation,
            final LocalDate debtDate) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        final FinantialEntity finantialEntity = academicTreasuryServices
                .finantialEntityOfDegree(enrolmentEvaluation.getEnrolment().getRegistration().getDegree(), debtDate);

        return findAcademicTariff(finantialEntity, enrolmentEvaluation, debtDate);
    }

    public static AcademicTariff findAcademicTariff(final FinantialEntity finantialEntity,
            final EnrolmentEvaluation enrolmentEvaluation, final LocalDate debtDate) {
        final Registration registration = enrolmentEvaluation.getRegistration();

        final Product improvementAcademicTaxProduct =
                AcademicTreasurySettings.getInstance().getImprovementAcademicTax().getProduct();
        return AcademicTariff.findMatch(finantialEntity, improvementAcademicTaxProduct, registration.getDegree(),
                debtDate.toDateTimeAtStartOfDay());
    }

    public static boolean isImprovementAcademicTaxCharged(final Registration registration, final ExecutionYear executionYear,
            final EnrolmentEvaluation enrolmentEvaluation) {
        if (findAcademicTreasuryEventForImprovementTax(registration, executionYear) == null) {
            return false;
        }

        final AcademicTreasuryEvent academicTreasuryEvent =
                findAcademicTreasuryEventForImprovementTax(registration, executionYear);

        return academicTreasuryEvent.isChargedWithDebitEntry(enrolmentEvaluation);
    }

    public static AcademicDebitEntryBean calculateImprovementTaxForDefaultEntity(final EnrolmentEvaluation enrolmentEvaluation,
            final LocalDate debtDate) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        
        final FinantialEntity finantialEntity = academicTreasuryServices.finantialEntityOfDegree(enrolmentEvaluation.getEnrolment().getRegistration().getDegree(), debtDate);
        
        return calculateImprovementTax(finantialEntity, enrolmentEvaluation, debtDate);
        
    }

    public static AcademicDebitEntryBean calculateImprovementTax(final FinantialEntity finantialEntity,
            final EnrolmentEvaluation enrolmentEvaluation, final LocalDate debtDate) {
        if (!enrolmentEvaluation.getEvaluationSeason().isImprovement()) {
            throw new AcademicTreasuryDomainException("error.AcademicTaxServices.enrolmentEvaluation.is.not.improvement");
        }

        final AcademicTax improvementAcademicTax = AcademicTreasurySettings.getInstance().getImprovementAcademicTax();

        if (improvementAcademicTax == null) {
            return null;
        }

        final Registration registration = enrolmentEvaluation.getRegistration();
        final ExecutionYear executionYear = enrolmentEvaluation.getExecutionPeriod().getExecutionYear();

        final AcademicTreasuryEvent academicTreasuryEvent =
                findAcademicTreasuryEventForImprovementTax(registration, executionYear);

        if (academicTreasuryEvent.isChargedWithDebitEntry(enrolmentEvaluation)) {
            return null;
        }

        final AcademicTariff academicTariff = AcademicTariff.findMatch(finantialEntity, improvementAcademicTax.getProduct(),
                registration.getDegree(), debtDate.toDateTimeAtStartOfDay());

        if (academicTariff == null) {
            return null;
        }

        final LocalizedString debitEntryName =
                AcademicTariff.improvementDebitEntryName(improvementAcademicTax, enrolmentEvaluation);
        final LocalDate dueDate = academicTariff.dueDate(debtDate);
        final Vat vat = academicTariff.vat(debtDate);
        final BigDecimal amount = academicTariff.amountToPay(academicTreasuryEvent, enrolmentEvaluation);

        return new AcademicDebitEntryBean(debitEntryName, dueDate, vat.getTaxRate(), amount);
    }

    @Atomic
    public static boolean createImprovementTaxForDefaultFinantialEntity(final EnrolmentEvaluation enrolmentEvaluation, final LocalDate when) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        final FinantialEntity finantialEntity = academicTreasuryServices.finantialEntityOfDegree(enrolmentEvaluation.getRegistration().getDegree(), when);
        
        return createImprovementTax(finantialEntity, enrolmentEvaluation, when); 
    }
    
    @Atomic
    public static boolean createImprovementTax(final FinantialEntity finantialEntity,
            final EnrolmentEvaluation enrolmentEvaluation, final LocalDate when) {

        if (!enrolmentEvaluation.getEvaluationSeason().isImprovement()) {
            throw new AcademicTreasuryDomainException("error.AcademicTaxServices.enrolmentEvaluation.is.not.improvement");
        }

        final Registration registration = enrolmentEvaluation.getRegistration();
        final ExecutionYear executionYear = enrolmentEvaluation.getExecutionPeriod().getExecutionYear();

        AcademicTreasurySettings instance = AcademicTreasurySettings.getInstance();

        if (instance == null) {
            return false;
        }

        final AcademicTax improvementAcademicTax = instance.getImprovementAcademicTax();

        if (improvementAcademicTax == null) {
            return false;
        }

        final AcademicTariff academicTariff = AcademicTariff.findMatch(finantialEntity, improvementAcademicTax.getProduct(),
                registration.getDegree(), when.toDateTimeAtStartOfDay());

        if (academicTariff == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTaxDebtCreation.tariff.not.found");
        }

        final Person person = registration.getPerson();
        final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);

        if (findAcademicTreasuryEventForImprovementTax(registration, executionYear) == null) {
            if (Strings.isNullOrEmpty(addressFiscalCountryCode) || Strings.isNullOrEmpty(fiscalNumber)) {
                throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalInformation.required");
            }

            // Read person customer
            if (!PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).isPresent()) {
                PersonCustomer.create(person, addressFiscalCountryCode, fiscalNumber);
            }

            final PersonCustomer personCustomer = PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).get();
            if (!personCustomer.isActive()) {
                throw new AcademicTreasuryDomainException("error.PersonCustomer.not.active", addressFiscalCountryCode, fiscalNumber);
            }

            AcademicTreasuryEvent.createForImprovementTuition(registration, executionYear);
        }

        final AcademicTreasuryEvent academicTreasuryEvent =
                findAcademicTreasuryEventForImprovementTax(registration, executionYear);

        if (academicTreasuryEvent.isChargedWithDebitEntry(enrolmentEvaluation)) {
            return false;
        }

        final PersonCustomer personCustomer = PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).get();
        final DebtAccount debtAccount = DebtAccount.findUnique(finantialEntity.getFinantialInstitution(), personCustomer).get();

        final DebitEntry debitEntry =
                academicTariff.createDebitEntryForImprovement(debtAccount, academicTreasuryEvent, enrolmentEvaluation);

        return debitEntry != null;
    }

    public static boolean removeDebitEntryForImprovement(final EnrolmentEvaluation improvementEnrolmentEvaluation) {
        final Registration registration = improvementEnrolmentEvaluation.getRegistration();
        final ExecutionYear executionYear = improvementEnrolmentEvaluation.getExecutionPeriod().getExecutionYear();

        if (findAcademicTreasuryEventForImprovementTax(registration, executionYear) == null) {
            return false;
        }

        final AcademicTreasuryEvent academicTreasuryEvent =
                findAcademicTreasuryEventForImprovementTax(registration, executionYear);

        if (!academicTreasuryEvent.isChargedWithDebitEntry(improvementEnrolmentEvaluation)) {
            return false;
        }

        final DebitEntry debitEntry =
                academicTreasuryEvent.findActiveEnrolmentEvaluationDebitEntry(improvementEnrolmentEvaluation).get();

        final DebitNote debitNote = (DebitNote) debitEntry.getFinantialDocument();

        if (!debitEntry.isProcessedInDebitNote()) {
            debitEntry.annulDebitEntry(academicTreasuryBundle("label.AcademicTaxServices.removeDebitEntryForImprovement.reason"));

//            debitEntry.setCurricularCourse(null);
//            debitEntry.setExecutionSemester(null);
//            debitEntry.setEvaluationSeason(null);

            return true;
        } else if (debitEntry.getCreditEntriesSet().isEmpty()) {
            debitNote.anullDebitNoteWithCreditNote(
                    academicTreasuryBundle("label.AcademicTaxServices.removeDebitEntryForImprovement.reason"), false);
            return true;
        }

        return false;
    }

    /* ----------
     * ENROLMENTS
     * ----------
     */
}
