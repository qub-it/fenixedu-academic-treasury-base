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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DomainObjectUtil;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.domain.tuition.TuitionInstallmentTariff;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.academictreasury.domain.tuition.TuitionTariffCustomCalculator;
import org.fenixedu.academictreasury.dto.tuition.TuitionDebitEntryBean;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class TuitionServices {

    public static Comparator<Enrolment> ENROLMENT_COMPARATOR_BY_NAME_AND_ID = (o1, o2) -> {
        IAcademicTreasuryPlatformDependentServices implementation =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        int c = implementation.localizedNameOfEnrolment(o1).compareTo(implementation.localizedNameOfEnrolment(o2));
        return c != 0 ? c : DomainObjectUtil.COMPARATOR_BY_ID.compare(o1, o2);
    };

    private static final List<ITuitionServiceExtension> TUITION_SERVICE_EXTENSIONS = Lists.newArrayList();

    public static synchronized void registerTuitionServiceExtension(final ITuitionServiceExtension extension) {
        TUITION_SERVICE_EXTENSIONS.add(extension);
    }

    public static boolean isToPayRegistrationTuition(final Registration registration, final ExecutionYear executionYear) {
        final IAcademicTreasuryPlatformDependentServices academicServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
	return Boolean.TRUE.equals(academicServices.registrationProtocol(registration).getPayGratuity());
    }

    public static AcademicTreasuryEvent findAcademicTreasuryEventTuitionForRegistration(final Registration registration,
            final ExecutionYear executionYear) {
        return AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).orElse(null);
    }

    public static boolean isTuitionForRegistrationCharged(final Registration registration, final ExecutionYear executionYear) {
        if (!AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).isPresent()) {
            return false;
        }

        final AcademicTreasuryEvent academicTreasuryEvent =
                AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).get();

        return academicTreasuryEvent.isCharged();
    }

    @Atomic
    public static boolean createInferedTuitionForRegistration(final Registration registration, final ExecutionYear executionYear,
            final LocalDate when, final boolean forceCreationIfNotEnrolled) {
        return createTuitionForRegistration(registration, executionYear, when, forceCreationIfNotEnrolled, null, true);
    }

    @Atomic
    public static boolean createTuitionForRegistration(final Registration registration, final ExecutionYear executionYear,
            final LocalDate debtDate, final boolean forceCreationIfNotEnrolled, TuitionPaymentPlan tuitionPaymentPlan,
            final boolean applyTuitionServiceExtensions) {

        if (!isToPayRegistrationTuition(registration, executionYear) && !forceCreationIfNotEnrolled) {
            return false;
        }

        if (applyTuitionServiceExtensions) {
            for (final ITuitionServiceExtension iTuitionServiceExtension : TUITION_SERVICE_EXTENSIONS) {
                if (iTuitionServiceExtension.applyExtension(registration, executionYear)) {
                    return iTuitionServiceExtension.createTuitionForRegistration(registration, executionYear, debtDate,
                            forceCreationIfNotEnrolled, tuitionPaymentPlan);
                }
            }
        }

        final Person person = registration.getPerson();
        final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);
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

        if (tuitionPaymentPlan == null) {
            tuitionPaymentPlan = TuitionPaymentPlan.inferTuitionPaymentPlanForRegistration(registration, executionYear);
        }

        if (tuitionPaymentPlan == null) {
            return false;
        }

        if (!forceCreationIfNotEnrolled && tuitionPaymentPlan.isStudentMustBeEnrolled()
                && normalEnrolmentsIncludingAnnuled(registration, executionYear).isEmpty()) {
            return false;
        }

        if (!DebtAccount.findUnique(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer)
                .isPresent()) {
            DebtAccount.create(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer);
        }

        if (!AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).isPresent()) {
            AcademicTreasuryEvent.createForRegistrationTuition(tuitionPaymentPlan.getProduct(), registration, executionYear);
        }

        final DebtAccount debtAccount =
                DebtAccount.findUnique(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer).get();

        final AcademicTreasuryEvent academicTreasuryEvent =
                AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).get();

        return tuitionPaymentPlan.createDebitEntriesForRegistration(debtAccount, academicTreasuryEvent, debtDate);
    }

    public static TuitionPaymentPlan usedPaymentPlan(final Registration registration, final ExecutionYear executionYear,
            final LocalDate debtDate) {
        return usedPaymentPlan(registration, executionYear, debtDate, null);
    }

    public static TuitionPaymentPlan usedPaymentPlan(final Registration registration, final ExecutionYear executionYear,
            final LocalDate debtDate, final TuitionPaymentPlan tuitionPaymentPlan) {
        if (tuitionPaymentPlan != null) {
            return tuitionPaymentPlan;
        }

        return TuitionPaymentPlan.inferTuitionPaymentPlanForRegistration(registration, executionYear);
    }

    @Atomic
    public static List<TuitionDebitEntryBean> calculateInstallmentDebitEntryBeans(final Registration registration,
            final ExecutionYear executionYear, final LocalDate debtDate) {
        return calculateInstallmentDebitEntryBeans(registration, executionYear, debtDate, null, true);
    }

    public static List<TuitionDebitEntryBean> calculateInstallmentDebitEntryBeans(final Registration registration,
            final ExecutionYear executionYear, final LocalDate debtDate, TuitionPaymentPlan tuitionPaymentPlan,
            final boolean applyTuitionServiceExtensions) {

        if (applyTuitionServiceExtensions) {
            for (final ITuitionServiceExtension iTuitionServiceExtension : TUITION_SERVICE_EXTENSIONS) {
                if (iTuitionServiceExtension.applyExtension(registration, executionYear)) {
                    return iTuitionServiceExtension.calculateInstallmentDebitEntryBeans(registration, executionYear, debtDate,
                            tuitionPaymentPlan);
                }
            }
        }

        if (tuitionPaymentPlan == null) {
            tuitionPaymentPlan = TuitionPaymentPlan.inferTuitionPaymentPlanForRegistration(registration, executionYear);
        }

        if (tuitionPaymentPlan == null) {
            return Lists.newArrayList();
        }

        final BigDecimal enrolledEctsUnits = AcademicTreasuryEvent
                .getEnrolledEctsUnits(tuitionPaymentPlan.getTuitionPaymentPlanGroup(), registration, executionYear);
        final BigDecimal enrolledCoursesCount = AcademicTreasuryEvent
                .getEnrolledCoursesCount(tuitionPaymentPlan.getTuitionPaymentPlanGroup(), registration, executionYear);

        return buildInstallmentDebitEntryBeans(registration, tuitionPaymentPlan, debtDate, enrolledEctsUnits,
                enrolledCoursesCount);
    }

    public static List<TuitionDebitEntryBean> buildInstallmentDebitEntryBeans(Registration registration,
            final TuitionPaymentPlan tuitionPaymentPlan, final LocalDate debtDate, final BigDecimal enrolledEctsUnits,
            final BigDecimal enrolledCoursesCount) {

        Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap = new HashMap<>();

        tuitionPaymentPlan.getTuitionInstallmentTariffsSet().stream().map(tariff -> tariff.getTuitionTariffCustomCalculator())
                .collect(Collectors.toSet()).forEach(clazz -> {
                    if (clazz != null) {
                        TuitionTariffCustomCalculator newInstanceFor =
                                TuitionTariffCustomCalculator.getNewInstanceFor(clazz, registration, tuitionPaymentPlan);
                        calculatorsMap.put(clazz, newInstanceFor);
                    }
                });

        final List<TuitionDebitEntryBean> entries = Lists.newArrayList();
        for (final TuitionInstallmentTariff tuitionInstallmentTariff : tuitionPaymentPlan.getTuitionInstallmentTariffsSet()) {
            final int installmentOrder = tuitionInstallmentTariff.getInstallmentOrder();
            final LocalizedString installmentName = tuitionPaymentPlan.installmentName(registration, tuitionInstallmentTariff);
            final LocalDate dueDate = tuitionInstallmentTariff.dueDate(debtDate);
            final Vat vat = tuitionInstallmentTariff.vat(debtDate);
            final BigDecimal amount =
                    tuitionInstallmentTariff.amountToPay(registration, enrolledEctsUnits, enrolledCoursesCount, calculatorsMap);
            final Currency currency = tuitionInstallmentTariff.getFinantialEntity().getFinantialInstitution().getCurrency();

            entries.add(
                    new TuitionDebitEntryBean(installmentOrder, installmentName, dueDate, vat.getTaxRate(), amount, currency));
        }

        final Comparator<? super TuitionDebitEntryBean> comparator =
                (o1, o2) -> o1.getInstallmentOrder() - o2.getInstallmentOrder();

        return entries.stream().sorted(comparator).collect(Collectors.toList());
    }

    /* **********
     * Standalone
     * **********
     */

    public static AcademicTreasuryEvent findAcademicTreasuryEventTuitionForStandalone(final Registration registration,
            final ExecutionYear executionYear) {
        return AcademicTreasuryEvent.findUniqueForStandaloneTuition(registration, executionYear).orElse(null);
    }

    public static boolean isTuitionForStandaloneCharged(final Registration registration, final ExecutionYear executionYear,
            final Enrolment enrolment) {
        if (findAcademicTreasuryEventTuitionForStandalone(registration, executionYear) == null) {
            return false;
        }

        final AcademicTreasuryEvent academicTreasuryEvent =
                findAcademicTreasuryEventTuitionForStandalone(registration, executionYear);

        return academicTreasuryEvent.isChargedWithDebitEntry(enrolment);
    }

    @Atomic
    public static boolean createInferedTuitionForStandalone(final Enrolment standaloneEnrolment, final LocalDate when,
            final boolean forceCreation) {
        return createInferedTuitionForStandalone(Sets.newHashSet(standaloneEnrolment), when, forceCreation);
    }

    @Atomic
    public static boolean createInferedTuitionForStandalone(final Set<Enrolment> standaloneEnrolments, final LocalDate when,
            final boolean forceCreation) {

        if (AcademicTreasurySettings.getInstance().getTuitionProductGroup() == null) {
            return false;
        }

        if (!TuitionPaymentPlanGroup.findUniqueDefaultGroupForStandalone().isPresent()) {
            return false;
        }

        boolean created = false;

        // Validate all enrolments are standalone

        for (final Enrolment standaloneEnrolment : standaloneEnrolments) {
            if (!standaloneEnrolment.isStandalone()) {
                throw new AcademicTreasuryDomainException("error.TuitionServices.enrolment.is.not.standalone");
            }
        }

        for (final Enrolment standaloneEnrolment : standaloneEnrolments) {
            final Registration registration = standaloneEnrolment.getRegistration();

            final Person person = registration.getPerson();
            final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(person);
            final String fiscalNumber = PersonCustomer.fiscalNumber(person);
            if (Strings.isNullOrEmpty(addressFiscalCountryCode) || Strings.isNullOrEmpty(fiscalNumber)) {
                throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalInformation.required");
            }

            // Read person customer
            if (!PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).isPresent()) {
                PersonCustomer.create(person, addressFiscalCountryCode, fiscalNumber);
            }

            final PersonCustomer personCustomer = PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).get();
            if (!personCustomer.isActive()) {
                throw new AcademicTreasuryDomainException("error.PersonCustomer.not.active", addressFiscalCountryCode,
                        fiscalNumber);
            }

            final ExecutionYear executionYear = standaloneEnrolment.getExecutionYear();

            if (TuitionPaymentPlan.inferTuitionPaymentPlanForStandaloneEnrolment(registration, executionYear,
                    standaloneEnrolment) == null) {
                continue;
            }

            final TuitionPaymentPlan tuitionPaymentPlan = TuitionPaymentPlan
                    .inferTuitionPaymentPlanForStandaloneEnrolment(registration, executionYear, standaloneEnrolment);

            created |= createTuitionForStandalone(standaloneEnrolment, tuitionPaymentPlan, when, forceCreation);
        }

        return created;
    }

    @Atomic
    public static boolean createTuitionForStandalone(final Enrolment standaloneEnrolment,
            final TuitionPaymentPlan tuitionPaymentPlan, final LocalDate when, final boolean forceCreation) {

        if (AcademicTreasurySettings.getInstance().getTuitionProductGroup() == null) {
            return false;
        }

        if (!TuitionPaymentPlanGroup.findUniqueDefaultGroupForStandalone().isPresent()) {
            return false;
        }

        final Registration registration = standaloneEnrolment.getRegistration();
        final ExecutionYear executionYear = standaloneEnrolment.getExecutionYear();

        final Person person = registration.getPerson();
        final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);
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

        if (!isToPayRegistrationTuition(registration, executionYear) && !forceCreation) {
            return false;
        }

        if (!DebtAccount.findUnique(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer)
                .isPresent()) {
            DebtAccount.create(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer);
        }

        final DebtAccount debtAccount =
                DebtAccount.findUnique(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer).get();

        if (!AcademicTreasuryEvent.findUniqueForStandaloneTuition(registration, executionYear).isPresent()) {
            AcademicTreasuryEvent.createForStandaloneTuition(tuitionPaymentPlan.getProduct(), registration, executionYear);
        }

        final AcademicTreasuryEvent academicTreasuryEvent =
                AcademicTreasuryEvent.findUniqueForStandaloneTuition(registration, executionYear).get();

        if (debtAccount.getFinantialInstitution() != tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution()) {
            throw new AcademicTreasuryDomainException(
                    "error.TuitionServices.standalone.tuition.for.different.finantial.institutions.not.supported");
        }

        return tuitionPaymentPlan.createDebitEntriesForStandalone(debtAccount, academicTreasuryEvent, standaloneEnrolment, when);
    }

    public static TuitionPaymentPlan usedPaymentPlanForStandalone(final Registration registration,
            final ExecutionYear executionYear, final Enrolment enrolment, final LocalDate debtDate) {
        return usedPaymentPlanForStandalone(registration, executionYear, enrolment, debtDate, null);
    }

    public static TuitionPaymentPlan usedPaymentPlanForStandalone(final Registration registration,
            final ExecutionYear executionYear, final Enrolment enrolment, final LocalDate debtDate,
            final TuitionPaymentPlan tuitionPaymentPlan) {
        if (tuitionPaymentPlan != null) {
            return tuitionPaymentPlan;
        }

        return TuitionPaymentPlan.inferTuitionPaymentPlanForStandaloneEnrolment(registration, executionYear, enrolment);
    }

    @Atomic
    public static List<TuitionDebitEntryBean> calculateInstallmentDebitEntryBeansForStandalone(final Registration registration,
            final ExecutionYear executionYear, final LocalDate debtDate, final Set<Enrolment> enrolments) {
        return calculateInstallmentDebitEntryBeansForStandalone(registration, executionYear, debtDate, null, enrolments);
    }

    @Atomic
    public static List<TuitionDebitEntryBean> calculateInstallmentDebitEntryBeansForStandalone(final Registration registration,
            final ExecutionYear executionYear, final LocalDate debtDate, TuitionPaymentPlan tuitionPaymentPlan,
            final Set<Enrolment> enrolments) {

        final Person person = registration.getPerson();
        final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);
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

        final List<TuitionDebitEntryBean> entries = Lists.newArrayList();
        for (final Enrolment enrolment : enrolments) {
            if (tuitionPaymentPlan == null) {
                tuitionPaymentPlan =
                        TuitionPaymentPlan.inferTuitionPaymentPlanForStandaloneEnrolment(registration, executionYear, enrolment);
            }

            if (tuitionPaymentPlan == null) {
                continue;
            }

            if (!DebtAccount.findUnique(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer)
                    .isPresent()) {
                DebtAccount.create(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer);
            }

            final DebtAccount debtAccount = DebtAccount
                    .findUnique(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer).get();

            if (!AcademicTreasuryEvent.findUniqueForStandaloneTuition(registration, executionYear).isPresent()) {
                AcademicTreasuryEvent.createForStandaloneTuition(tuitionPaymentPlan.getProduct(), registration, executionYear);
            }

            final AcademicTreasuryEvent academicTreasuryEvent =
                    AcademicTreasuryEvent.findUniqueForStandaloneTuition(registration, executionYear).get();

            Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap = new HashMap<>();

            TuitionPaymentPlan tuitionPaymentPlanToCalculator = tuitionPaymentPlan;
            tuitionPaymentPlan.getTuitionInstallmentTariffsSet().stream().map(tariff -> tariff.getTuitionTariffCustomCalculator())
                    .collect(Collectors.toSet()).forEach(clazz -> {
                        if (clazz != null) {
                            TuitionTariffCustomCalculator newInstanceFor = TuitionTariffCustomCalculator.getNewInstanceFor(clazz,
                                    registration, tuitionPaymentPlanToCalculator, enrolment);
                            calculatorsMap.put(clazz, newInstanceFor);
                        }
                    });

            final TuitionInstallmentTariff tuitionInstallmentTariff = tuitionPaymentPlan.getStandaloneTuitionInstallmentTariff();
            final int installmentOrder = tuitionInstallmentTariff.getInstallmentOrder();
            final LocalizedString installmentName = tuitionInstallmentTariff.standaloneDebitEntryName(enrolment);
            final LocalDate dueDate = tuitionInstallmentTariff.dueDate(debtDate);
            final Vat vat = tuitionInstallmentTariff.vat(debtDate);
            final BigDecimal amount = tuitionInstallmentTariff.amountToPay(academicTreasuryEvent, enrolment, calculatorsMap);
            final Currency currency = tuitionInstallmentTariff.getFinantialEntity().getFinantialInstitution().getCurrency();

            entries.add(
                    new TuitionDebitEntryBean(installmentOrder, installmentName, dueDate, vat.getTaxRate(), amount, currency));
        }

        return entries.stream().sorted((o1, o2) -> o1.getInstallmentOrder() - o2.getInstallmentOrder())
                .collect(Collectors.toList());
    }

    public static boolean removeDebitEntryForStandaloneEnrolment(final Enrolment standaloneEnrolment) {
        final Registration registration = standaloneEnrolment.getRegistration();
        final ExecutionYear executionYear = standaloneEnrolment.getExecutionYear();

        if (!AcademicTreasuryEvent.findUniqueForStandaloneTuition(registration, executionYear).isPresent()) {
            return false;
        }

        final AcademicTreasuryEvent academicTreasuryEvent =
                AcademicTreasuryEvent.findUniqueForStandaloneTuition(registration, executionYear).get();

        if (!academicTreasuryEvent.isChargedWithDebitEntry(standaloneEnrolment)) {
            return false;
        }

        final DebitEntry debitEntry = academicTreasuryEvent.findActiveEnrolmentDebitEntry(standaloneEnrolment).get();

        DebitNote debitNote = (DebitNote) debitEntry.getFinantialDocument();
        if (!debitEntry.isProcessedInDebitNote()) {
            debitEntry.annulDebitEntry(academicTreasuryBundle(TreasuryConstants.DEFAULT_LANGUAGE,
                    "label.TuitionServices.removeDebitEntryForStandaloneEnrolment.reason"));

        } else {
            debitNote.anullDebitNoteWithCreditNote(academicTreasuryBundle(TreasuryConstants.DEFAULT_LANGUAGE,
                    "label.TuitionServices.removeDebitEntryForStandaloneEnrolment.reason"), false);

        }

        return true;
    }

    /* ***************
     * EXTRACURRICULAR
     * ***************
     */

    public static AcademicTreasuryEvent findAcademicTreasuryEventTuitionForExtracurricular(final Registration registration,
            final ExecutionYear executionYear) {
        return AcademicTreasuryEvent.findUniqueForExtracurricularTuition(registration, executionYear).orElse(null);
    }

    public static boolean isTuitionForExtracurricularCharged(final Registration registration, final ExecutionYear executionYear,
            final Enrolment enrolment) {
        if (findAcademicTreasuryEventTuitionForExtracurricular(registration, executionYear) == null) {
            return false;
        }

        final AcademicTreasuryEvent academicTreasuryEvent =
                findAcademicTreasuryEventTuitionForExtracurricular(registration, executionYear);

        return academicTreasuryEvent.isChargedWithDebitEntry(enrolment);
    }

    @Atomic
    public static boolean createInferedTuitionForExtracurricular(final Enrolment extracurricularEnrolment, final LocalDate when,
            final boolean forceCreation) {
        return createInferedTuitionForExtracurricular(Sets.newHashSet(extracurricularEnrolment), when, forceCreation);
    }

    @Atomic
    public static boolean createInferedTuitionForExtracurricular(final Set<Enrolment> extracurricularEnrolments,
            final LocalDate when, final boolean forceCreation) {

        if (AcademicTreasurySettings.getInstance().getTuitionProductGroup() == null) {
            return false;
        }

        if (!TuitionPaymentPlanGroup.findUniqueDefaultGroupForExtracurricular().isPresent()) {
            return false;
        }

        boolean created = false;

        // Validate all enrolments are extracurricular

        for (final Enrolment extracurricularEnrolment : extracurricularEnrolments) {
            if (!extracurricularEnrolment.isExtraCurricular()) {
                throw new AcademicTreasuryDomainException("error.TuitionServices.enrolment.is.not.extracurricular");
            }
        }

        for (final Enrolment extracurricularEnrolment : extracurricularEnrolments) {
            final Registration registration = extracurricularEnrolment.getRegistration();

            final Person person = registration.getPerson();
            final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(person);
            final String fiscalNumber = PersonCustomer.fiscalNumber(person);
            if (Strings.isNullOrEmpty(addressFiscalCountryCode) || Strings.isNullOrEmpty(fiscalNumber)) {
                throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalInformation.required");
            }

            // Read person customer
            if (!PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).isPresent()) {
                PersonCustomer.create(person, addressFiscalCountryCode, fiscalNumber);
            }

            final PersonCustomer personCustomer = PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).get();
            if (!personCustomer.isActive()) {
                throw new AcademicTreasuryDomainException("error.PersonCustomer.not.active", addressFiscalCountryCode,
                        fiscalNumber);
            }

            final ExecutionYear executionYear = extracurricularEnrolment.getExecutionYear();

            if (TuitionPaymentPlan.inferTuitionPaymentPlanForExtracurricularEnrolment(registration, executionYear,
                    extracurricularEnrolment) == null) {
                continue;
            }

            final TuitionPaymentPlan tuitionPaymentPlan = TuitionPaymentPlan
                    .inferTuitionPaymentPlanForExtracurricularEnrolment(registration, executionYear, extracurricularEnrolment);

            created |= createTuitionForExtracurricular(extracurricularEnrolment, tuitionPaymentPlan, when, forceCreation);
        }

        return created;
    }

    @Atomic
    public static boolean createTuitionForExtracurricular(final Enrolment extracurricularEnrolment,
            final TuitionPaymentPlan tuitionPaymentPlan, final LocalDate when, final boolean forceCreation) {

        if (AcademicTreasurySettings.getInstance().getTuitionProductGroup() == null) {
            return false;
        }

        if (!TuitionPaymentPlanGroup.findUniqueDefaultGroupForExtracurricular().isPresent()) {
            return false;
        }

        final Registration registration = extracurricularEnrolment.getRegistration();
        final ExecutionYear executionYear = extracurricularEnrolment.getExecutionYear();

        final Person person = registration.getPerson();
        final String fiscalCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);
        if (Strings.isNullOrEmpty(fiscalCountryCode) || Strings.isNullOrEmpty(fiscalNumber)) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalInformation.required");
        }

        // Read person customer
        if (!PersonCustomer.findUnique(person, fiscalCountryCode, fiscalNumber).isPresent()) {
            PersonCustomer.create(person, fiscalCountryCode, fiscalNumber);
        }

        final PersonCustomer personCustomer = PersonCustomer.findUnique(person, fiscalCountryCode, fiscalNumber).get();
        if (!personCustomer.isActive()) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.not.active", fiscalCountryCode, fiscalNumber);
        }

        if (!isToPayRegistrationTuition(registration, executionYear) && !forceCreation) {
            return false;
        }

        if (!DebtAccount.findUnique(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer)
                .isPresent()) {
            DebtAccount.create(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer);
        }

        final DebtAccount debtAccount =
                DebtAccount.findUnique(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer).get();

        if (!AcademicTreasuryEvent.findUniqueForExtracurricularTuition(registration, executionYear).isPresent()) {
            AcademicTreasuryEvent.createForExtracurricularTuition(tuitionPaymentPlan.getProduct(), registration, executionYear);
        }

        final AcademicTreasuryEvent academicTreasuryEvent =
                AcademicTreasuryEvent.findUniqueForExtracurricularTuition(registration, executionYear).get();

        if (debtAccount.getFinantialInstitution() != tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution()) {
            throw new AcademicTreasuryDomainException(
                    "error.TuitionServices.standalone.tuition.for.different.finantial.institutions.not.supported");
        }

        return tuitionPaymentPlan.createDebitEntriesForExtracurricular(debtAccount, academicTreasuryEvent,
                extracurricularEnrolment, when);
    }

    public static TuitionPaymentPlan usedPaymentPlanForExtracurricular(final Registration registration,
            final ExecutionYear executionYear, final Enrolment enrolment, final LocalDate debtDate) {
        return usedPaymentPlanForExtracurricular(registration, executionYear, enrolment, debtDate, null);
    }

    public static TuitionPaymentPlan usedPaymentPlanForExtracurricular(final Registration registration,
            final ExecutionYear executionYear, final Enrolment enrolment, final LocalDate debtDate,
            final TuitionPaymentPlan tuitionPaymentPlan) {
        if (tuitionPaymentPlan != null) {
            return tuitionPaymentPlan;
        }

        return TuitionPaymentPlan.inferTuitionPaymentPlanForExtracurricularEnrolment(registration, executionYear, enrolment);
    }

    @Atomic
    public static List<TuitionDebitEntryBean> calculateInstallmentDebitEntryBeansForExtracurricular(
            final Registration registration, final ExecutionYear executionYear, final LocalDate debtDate,
            final Set<Enrolment> enrolments) {
        return calculateInstallmentDebitEntryBeansForExtracurricular(registration, executionYear, debtDate, null, enrolments);
    }

    @Atomic
    public static List<TuitionDebitEntryBean> calculateInstallmentDebitEntryBeansForExtracurricular(
            final Registration registration, final ExecutionYear executionYear, final LocalDate debtDate,
            TuitionPaymentPlan tuitionPaymentPlan, final Set<Enrolment> enrolments) {

        final Person person = registration.getPerson();
        final String fiscalCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);
        if (Strings.isNullOrEmpty(fiscalCountryCode) || Strings.isNullOrEmpty(fiscalNumber)) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalInformation.required");
        }

        // Read person customer
        if (!PersonCustomer.findUnique(person, fiscalCountryCode, fiscalNumber).isPresent()) {
            PersonCustomer.create(person, fiscalCountryCode, fiscalNumber);
        }

        final PersonCustomer personCustomer = PersonCustomer.findUnique(person, fiscalCountryCode, fiscalNumber).get();
        if (!personCustomer.isActive()) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.not.active", fiscalCountryCode, fiscalNumber);
        }

        final List<TuitionDebitEntryBean> entries = Lists.newArrayList();
        for (final Enrolment enrolment : enrolments) {
            if (tuitionPaymentPlan == null) {
                tuitionPaymentPlan = TuitionPaymentPlan.inferTuitionPaymentPlanForExtracurricularEnrolment(registration,
                        executionYear, enrolment);
            }

            if (tuitionPaymentPlan == null) {
                continue;
            }

            if (!DebtAccount.findUnique(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer)
                    .isPresent()) {
                DebtAccount.create(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer);
            }

            if (!AcademicTreasuryEvent.findUniqueForExtracurricularTuition(registration, executionYear).isPresent()) {
                AcademicTreasuryEvent.createForExtracurricularTuition(tuitionPaymentPlan.getProduct(), registration,
                        executionYear);
            }

            final AcademicTreasuryEvent academicTreasuryEvent =
                    AcademicTreasuryEvent.findUniqueForExtracurricularTuition(registration, executionYear).get();

            Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap = new HashMap<>();

            TuitionPaymentPlan tuitionPaymentPlanToCalculator = tuitionPaymentPlan;
            tuitionPaymentPlan.getTuitionInstallmentTariffsSet().stream().map(tariff -> tariff.getTuitionTariffCustomCalculator())
                    .collect(Collectors.toSet()).forEach(clazz -> {
                        if (clazz != null) {
                            TuitionTariffCustomCalculator newInstanceFor = TuitionTariffCustomCalculator.getNewInstanceFor(clazz,
                                    registration, tuitionPaymentPlanToCalculator, enrolment);
                            calculatorsMap.put(clazz, newInstanceFor);
                        }
                    });

            final TuitionInstallmentTariff tuitionInstallmentTariff =
                    tuitionPaymentPlan.getExtracurricularTuitionInstallmentTariff();
            final int installmentOrder = tuitionInstallmentTariff.getInstallmentOrder();
            final LocalizedString installmentName = tuitionInstallmentTariff.extracurricularDebitEntryName(enrolment);
            final LocalDate dueDate = tuitionInstallmentTariff.dueDate(debtDate);
            final Vat vat = tuitionInstallmentTariff.vat(debtDate);
            final BigDecimal amount = tuitionInstallmentTariff.amountToPay(academicTreasuryEvent, enrolment, calculatorsMap);
            final Currency currency = tuitionInstallmentTariff.getFinantialEntity().getFinantialInstitution().getCurrency();

            entries.add(
                    new TuitionDebitEntryBean(installmentOrder, installmentName, dueDate, vat.getTaxRate(), amount, currency));
        }

        return entries.stream().sorted((o1, o2) -> o1.getInstallmentOrder() - o2.getInstallmentOrder())
                .collect(Collectors.toList());
    }

    public static boolean removeDebitEntryForExtracurricularEnrolment(final Enrolment extracurricularEnrolment) {
        final Registration registration = extracurricularEnrolment.getRegistration();
        final ExecutionYear executionYear = extracurricularEnrolment.getExecutionYear();

        if (!AcademicTreasuryEvent.findUniqueForExtracurricularTuition(registration, executionYear).isPresent()) {
            return false;
        }

        final AcademicTreasuryEvent academicTreasuryEvent =
                AcademicTreasuryEvent.findUniqueForExtracurricularTuition(registration, executionYear).get();

        if (!academicTreasuryEvent.isChargedWithDebitEntry(extracurricularEnrolment)) {
            return false;
        }

        final DebitEntry debitEntry = academicTreasuryEvent.findActiveEnrolmentDebitEntry(extracurricularEnrolment).get();

        final DebitNote debitNote = (DebitNote) debitEntry.getFinantialDocument();
        if (!debitEntry.isProcessedInDebitNote()) {
            debitEntry.annulDebitEntry(
                    academicTreasuryBundle("label.TuitionServices.removeDebitEntryForExtracurricularEnrolment.reason"));

        } else {
            debitNote.anullDebitNoteWithCreditNote(
                    academicTreasuryBundle("label.TuitionServices.removeDebitEntryForExtracurricularEnrolment.reason"), false);
        }

        return true;
    }

    /* ----------
     * ENROLMENTS
     * ----------
     */

    public static LocalDate enrolmentDate(final Registration registration, final ExecutionYear executionYear,
            final boolean isToForceCreation) {
        for (final RegistrationDataByExecutionYear registrationDataByExecutionYear : registration
                .getRegistrationDataByExecutionYearSet()) {
            if (registrationDataByExecutionYear.getExecutionYear() == executionYear
                    && registrationDataByExecutionYear.getEnrolmentDate() != null) {
                return registrationDataByExecutionYear.getEnrolmentDate();
            }
        }

        if (isToForceCreation) {
            // Search the enrolment dates for most recent years in which the student was enrolled

            int i = 0;
            for (ExecutionYear it = executionYear.getPreviousExecutionYear(); it != null; it =
                    it.getPreviousExecutionYear(), i++) {
                if (registrationDataByExecutionYear(registration, it) != null
                        && registrationDataByExecutionYear(registration, it).getEnrolmentDate() != null) {
                    return registrationDataByExecutionYear(registration, it).getEnrolmentDate().plusYears(i);
                }
            }
        }

        return new LocalDate();
    }

    public static LocalDate lastRegisteredDate(final Registration registration, final ExecutionYear executionYear) {

        final RegistrationState lastRegistrationState = registration.getLastRegistrationState(executionYear);

        if (lastRegistrationState == null) {
            return null;
        }

        if (!lastRegistrationState.isActive()) {
            return null;
        }

        final LocalDate stateDate = lastRegistrationState.getStateDate().toLocalDate();
        final LocalDate dateOnBeginExecutionYearCivilDate = new LocalDate(
                executionYear.getAcademicInterval().getStart().getYear(), stateDate.getMonthOfYear(), stateDate.getDayOfMonth());
        final LocalDate dateOnEndExecutionYearCivilDate = new LocalDate(executionYear.getAcademicInterval().getEnd().getYear(),
                stateDate.getMonthOfYear(), stateDate.getDayOfMonth());

        if (executionYear.containsDate(dateOnBeginExecutionYearCivilDate.toDateTimeAtStartOfDay())) {
            return dateOnBeginExecutionYearCivilDate;
        } else if (executionYear.containsDate(dateOnEndExecutionYearCivilDate.toDateTimeAtStartOfDay())) {
            return dateOnEndExecutionYearCivilDate;
        }

        return null;
    }

    public static List<ExecutionYear> orderedEnrolledExecutionYears(final Registration registration) {
        return registration.getEnrolmentsExecutionYears().stream().sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR)
                .collect(Collectors.toList());
    }

    public static List<ExecutionYear> orderedEnrolledAndImprovementExecutionYears(final Registration registration) {
        Set<ExecutionYear> result = Sets.newHashSet();

        result.addAll(registration.getEnrolmentsExecutionYears().stream().collect(Collectors.toSet()));

        result.addAll(registration.getStudentCurricularPlansSet().stream().map(l -> l.getEnrolmentsSet())
                .reduce((a, b) -> Sets.union(a, b)).orElse(Sets.newHashSet()).stream().map(l -> l.getEvaluationsSet())
                .reduce((a, b) -> Sets.union(a, b)).orElse(Sets.newHashSet()).stream()
                .filter(l -> l.getEvaluationSeason().isImprovement() && l.getExecutionPeriod() != null)
                .map(l -> l.getExecutionPeriod().getExecutionYear()).collect(Collectors.toSet()));

        return result.stream().sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR).collect(Collectors.toList());
    }

    // The result includes annuled enrolments
    public static Set<Enrolment> normalEnrolmentsIncludingAnnuled(final Registration registration,
            final ExecutionYear executionYear) {
        final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);

        if (studentCurricularPlan == null) {
            return Sets.newHashSet();
        }

        final Set<Enrolment> result = Sets.newHashSet(registration.getEnrolments(executionYear));

        result.removeAll(standaloneEnrolmentsIncludingAnnuled(registration, executionYear));

        result.removeAll(extracurricularEnrolmentsIncludingAnnuled(registration, executionYear));

        result.removeAll(studentCurricularPlan.getPropaedeuticCurriculumLines().stream()
                .filter(enrolmentInExecutionYearTest(executionYear)).collect(Collectors.toList()));

        return result;
    }

    public static Set<Enrolment> normalEnrolmentsWithoutAnnuled(final Registration registration,
            final ExecutionYear executionYear) {
        return normalEnrolmentsIncludingAnnuled(registration, executionYear).stream().filter(e -> !e.isAnnulled())
                .collect(Collectors.toSet());
    }

    public static Set<Enrolment> standaloneEnrolmentsIncludingAnnuled(final Registration registration,
            final ExecutionYear executionYear) {
        final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);

        if (studentCurricularPlan == null) {
            return Sets.newHashSet();
        }

	// TODO Check code Refactor/20210624-MergeWithISCTE
	// This code should be in academic treasury dependent platform services
//        return studentCurricularPlan.getStandaloneCurriculumLines().stream()
//                .filter(l -> l.getExecutionYear() == executionYear && l.isEnrolment()).map(l -> (Enrolment) l)
        return studentCurricularPlan.getExtraCurricularEnrolments().stream().filter(e -> e.isStandalone())
                .filter(l -> l.getExecutionYear() == executionYear && l.isEnrolment()).map(l -> l)
                .collect(Collectors.<Enrolment> toSet());
    }

    public static Set<Enrolment> standaloneEnrolmentsWithoutAnnuled(final Registration registration,
            final ExecutionYear executionYear) {
        return standaloneEnrolmentsIncludingAnnuled(registration, executionYear).stream().filter(e -> !e.isAnnulled())
                .collect(Collectors.toSet());
    }

    public static Set<Enrolment> extracurricularEnrolmentsIncludingAnnuled(final Registration registration,
            final ExecutionYear executionYear) {
        final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);

        if (studentCurricularPlan == null) {
            return Sets.newHashSet();
        }

        return studentCurricularPlan.getExtraCurricularCurriculumLines().stream()
                .filter(l -> l.getExecutionYear() == executionYear && l.isEnrolment()).map(l -> (Enrolment) l)
                .collect(Collectors.<Enrolment> toSet());
    }

    public static Set<Enrolment> extracurricularEnrolmentsWithoutAnnuled(final Registration registration,
            final ExecutionYear executionYear) {
        return extracurricularEnrolmentsIncludingAnnuled(registration, executionYear).stream().filter(e -> !e.isAnnulled())
                .collect(Collectors.toSet());
    }

    private static Predicate<? super CurriculumLine> enrolmentInExecutionYearTest(final ExecutionYear executionYear) {
        return l -> l.getExecutionYear() == executionYear && l.isEnrolment();
    }

    public static Set<EnrolmentEvaluation> improvementEnrolments(final Registration registration,
            final ExecutionYear executionYear) {

        final Set<EnrolmentEvaluation> result = Sets.newHashSet();

        final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);

        if (studentCurricularPlan == null) {
            return result;
        }

        for (final ExecutionInterval executionSemester : executionYear.getExecutionPeriodsSet()) {
            // TODO: check out to grab improved enrolment evaluations by execution semester
//            result.addAll(studentCurricularPlan.getEnroledImprovements().stream().filter(e -> e.getExecutionPeriod() == executionSemester).collect(Collectors.toList()));
        }

        return result;
    }

    private static RegistrationDataByExecutionYear registrationDataByExecutionYear(final Registration registration,
            final ExecutionYear executionYear) {
        return registration.getRegistrationDataByExecutionYearSet().stream().filter(l -> l.getExecutionYear() == executionYear)
                .findFirst().orElse(null);
    }

}
