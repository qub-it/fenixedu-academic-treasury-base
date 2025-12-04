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
package org.fenixedu.academictreasury.domain.event;

import static java.lang.String.format;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituationType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.treasury.AcademicTreasuryEventPayment;
import org.fenixedu.academic.domain.treasury.IAcademicServiceRequestAndAcademicTaxTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEventPayment;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryTarget;
import org.fenixedu.academic.domain.treasury.IImprovementTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IPaymentReferenceCode;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.domain.emoluments.AcademicTax;
import org.fenixedu.academictreasury.domain.emoluments.ServiceRequestMapEntry;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.reservationtax.ReservationTaxEventTarget;
import org.fenixedu.academictreasury.domain.serviceRequests.ITreasuryServiceRequest;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.domain.tariff.AcademicTariff;
import org.fenixedu.academictreasury.domain.tuition.TuitionInstallmentTariff;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.academictreasury.services.TuitionServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.domain.tariff.Tariff;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.core.AbstractDomainObject;

public class AcademicTreasuryEvent extends AcademicTreasuryEvent_Base
        implements IAcademicTreasuryEvent, IImprovementTreasuryEvent, IAcademicServiceRequestAndAcademicTaxTreasuryEvent {

    private static Logger logger = LoggerFactory.getLogger(AcademicTreasuryEvent.class);

    public AcademicTreasuryEvent() {
        super();
        setCustomAcademicDebt(false);
    }

    @Deprecated
    protected AcademicTreasuryEvent(final ITreasuryServiceRequest iTreasuryServiceRequest) {
        this();
        initForTreasuryServiceRequest(iTreasuryServiceRequest.getPerson(), iTreasuryServiceRequest,
                ServiceRequestMapEntry.findProduct(iTreasuryServiceRequest));
    }

    protected AcademicTreasuryEvent(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup, final Product product,
            final Registration registration, final ExecutionYear executionYear) {
        this();
        initForTuition(tuitionPaymentPlanGroup, product, registration, executionYear);

        checkRules();
    }

    protected AcademicTreasuryEvent(final AcademicTax academicTax, final Registration registration,
            final ExecutionYear executionYear) {
        this();
        initForAcademicTax(academicTax, registration, executionYear);

        checkRules();
    }

    // ANIL 2025-02-06 (#qubIT-Fenix-6602)
    protected AcademicTreasuryEvent(FinantialEntity finantialEntity, Product product, IAcademicTreasuryTarget target) {
        this();
        initAcademicTreasuryEventTarget(finantialEntity, product, target);

        checkRules();
    }

    protected AcademicTreasuryEvent(Product product, Registration registration, ExecutionYear executionYear,
            int customAcademicDebtNumberOfUnits, int customAcademicDebtNumberOfPages, boolean customAcademicDebtUrgent,
            LocalDate customAcademicDebtEventDate, String academicProcessNumber) {
        initForCustomAcademicDebt(product, registration, executionYear, customAcademicDebtNumberOfUnits,
                customAcademicDebtNumberOfPages, customAcademicDebtUrgent, customAcademicDebtEventDate, academicProcessNumber);

        checkRules();
    }

    @Override
    protected void init(FinantialEntity finantialEntity, Product product, LocalizedString name) {
        throw new RuntimeException("wrong call");
    }

    @Deprecated
    private void initForTreasuryServiceRequest(final Person person, final ITreasuryServiceRequest iTreasuryServiceRequest,
            final Product product) {
        LocalDate eventDate =
                iTreasuryServiceRequest.getRequestDate() != null ? iTreasuryServiceRequest.getRequestDate().toLocalDate() : null;

        if (eventDate == null) {
            eventDate = new LocalDate();
        }

        Degree degree = iTreasuryServiceRequest.getRegistration().getDegree();
        FinantialEntity finantialEntity = AcademicTreasuryConstants.getFinantialEntityOfDegree(degree, eventDate);

        if (finantialEntity == null && FinantialEntity.findAll().count() == 1) {
            finantialEntity = FinantialEntity.findAll().iterator().next();
        }

        super.init(finantialEntity, product, nameForAcademicServiceRequest(product, iTreasuryServiceRequest));

        setPerson(person);
        setITreasuryServiceRequest(iTreasuryServiceRequest);
        setPropertiesJsonMap(org.fenixedu.treasury.util.TreasuryConstants.propertiesMapToJson(fillPropertiesMap()));
        setDescription(descriptionForAcademicServiceRequest());

        checkRules();
    }

    public static LocalizedString nameForAcademicServiceRequest(final Product product,
            final ITreasuryServiceRequest iTreasuryServiceRequest) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();
        LocalizedString result = new LocalizedString();

        final ServiceRequestMapEntry serviceRequestMapEntry = ServiceRequestMapEntry.findMatch(iTreasuryServiceRequest);

        for (final Locale locale : treasuryServices.availableLocales()) {
            final ExecutionYear executionYear = iTreasuryServiceRequest.getExecutionYear();
            if (executionYear != null) {
                String text = format("%s [%s - %s] (%s)", product.getName().getContent(locale),
                        iTreasuryServiceRequest.getRegistration().getDegree().getPresentationName(executionYear, locale),
                        executionYear.getQualifiedName(), iTreasuryServiceRequest.getServiceRequestNumberYear());

                if (!StringUtils.isEmpty(serviceRequestMapEntry.getDebitEntryDescriptionExtensionFormat())) {
                    final StrSubstitutor str = new StrSubstitutor(iTreasuryServiceRequest.getPropertyValuesMap());

                    final String extString = str.replace(serviceRequestMapEntry.getDebitEntryDescriptionExtensionFormat());
                    text += " " + extString;
                }

                result = result.with(locale, text);
            } else {
                String text = format("%s [%s] (%s)", product.getName().getContent(locale),
                        iTreasuryServiceRequest.getRegistration().getDegree().getPresentationName(null, locale),
                        iTreasuryServiceRequest.getServiceRequestNumberYear());

                if (!StringUtils.isEmpty(serviceRequestMapEntry.getDebitEntryDescriptionExtensionFormat())) {
                    final StrSubstitutor str = new StrSubstitutor(iTreasuryServiceRequest.getPropertyValuesMap());

                    final String extString = str.replace(serviceRequestMapEntry.getDebitEntryDescriptionExtensionFormat());
                    text += " " + extString;
                }

                result = result.with(locale, text);
            }
        }

        return result;
    }

    private void initForTuition(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup, final Product product,
            final Registration registration, final ExecutionYear executionYear) {
        final RegistrationDataByExecutionYear data = findRegistrationDataByExecutionYear(registration, executionYear);

        LocalDate eventDate = null;
        if (data != null && data.getEnrolmentDate() != null) {
            eventDate = data.getEnrolmentDate();
        } else {
            eventDate = executionYear.getBeginLocalDate();
        }

        if (eventDate == null) {
            eventDate = new LocalDate();
        }

        Degree degree = registration.getDegree();
        FinantialEntity finantialEntity = AcademicTreasuryConstants.getFinantialEntityOfDegree(degree, eventDate);

        if (finantialEntity == null && FinantialEntity.findAll().count() == 1) {
            finantialEntity = FinantialEntity.findAll().iterator().next();
        }

        super.init(finantialEntity, product, nameForTuition(product, registration, executionYear));

        setPerson(registration.getPerson());
        setTuitionPaymentPlanGroup(tuitionPaymentPlanGroup);
        setRegistration(registration);
        setExecutionYear(executionYear);
        setPropertiesJsonMap(org.fenixedu.treasury.util.TreasuryConstants.propertiesMapToJson(fillPropertiesMap()));

        checkRules();
    }

    private RegistrationDataByExecutionYear findRegistrationDataByExecutionYear(Registration registration,
            ExecutionYear executionYear) {
        return registration.getRegistrationDataByExecutionYearSet().stream().filter(rd -> rd.getExecutionYear() == executionYear)
                .findAny().orElse(null);
    }

    private LocalizedString nameForTuition(final Product product, final Registration registration,
            final ExecutionYear executionYear) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();
        LocalizedString result = new LocalizedString();

        for (final Locale locale : treasuryServices.availableLocales()) {
            final String name = String.format("%s [%s - %s]", product.getName().getContent(locale),
                    registration.getDegree().getPresentationName(executionYear, locale), executionYear.getQualifiedName());

            result = result.with(locale, name);
        }

        return result;
    }

    private void initForAcademicTax(final AcademicTax academicTax, final Registration registration,
            final ExecutionYear executionYear) {
        final RegistrationDataByExecutionYear data = findRegistrationDataByExecutionYear(registration, executionYear);

        LocalDate eventDate = null;
        if (data != null && data.getEnrolmentDate() != null) {
            eventDate = data.getEnrolmentDate();
        } else {
            eventDate = executionYear.getBeginLocalDate();
        }

        if (eventDate == null) {
            eventDate = new LocalDate();
        }

        Degree degree = registration.getDegree();
        FinantialEntity finantialEntity = AcademicTreasuryConstants.getFinantialEntityOfDegree(degree, eventDate);

        if (finantialEntity == null && FinantialEntity.findAll().count() == 1) {
            finantialEntity = FinantialEntity.findAll().iterator().next();
        }

        super.init(finantialEntity, academicTax.getProduct(), nameForAcademicTax(academicTax, registration, executionYear));

        setAcademicTax(academicTax);
        setPerson(registration.getPerson());
        setRegistration(registration);
        setExecutionYear(executionYear);
        setPropertiesJsonMap(org.fenixedu.treasury.util.TreasuryConstants.propertiesMapToJson(fillPropertiesMap()));

        checkRules();
    }

    public static LocalizedString nameForAcademicTax(final AcademicTax academicTax, final Registration registration,
            final ExecutionYear executionYear) {
        // ANIL 2025-05-28 (#qubIT-Fenix-6941)
        if (AcademicTreasurySettings.getInstance().getUseCustomAcademicDebtFormat()) {
            return buildCustomNameForAcademicTax(academicTax, registration, executionYear);
        } else {
            return defaultNameForAcademicTax(academicTax, registration, executionYear);
        }
    }

    private static LocalizedString buildCustomNameForAcademicTax(AcademicTax academicTax, Registration registration,
            ExecutionYear executionYear) {
        if (!academicTax.isAppliedOnRegistration()) {
            throw new RuntimeException(
                    "error.AcademicTreasuryEvent.buildCustomNameForAcademicTax.isAppliedOnRegistration.not.supported");
        }

        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        LocalizedString result = new LocalizedString();
        for (final Locale locale : treasuryServices.availableLocales()) {
            Map<String, String> valueMap = new HashMap<String, String>();

            String degreePresentationName = registration.getDegree().getPresentationName(executionYear, locale);
            String degreeName = registration.getDegree().getNameI18N(executionYear).getContent(locale);

            Product product = academicTax.getProduct();
            LocalizedString productName = product.getName();

            valueMap.put("productName", StringUtils.isNotEmpty(productName.getContent(locale)) ? productName.getContent(
                    locale) : productName.getContent());
            valueMap.put("degreeCode", registration.getDegree().getCode());

            valueMap.put("degreePresentationName", degreePresentationName);
            valueMap.put("degreeName", degreeName);

            valueMap.put("executionYearName", executionYear.getQualifiedName());

            LocalizedString formatToUse = AcademicTreasurySettings.getInstance().getCustomAcademicDebtFormat();
            String name = StrSubstitutor.replace(formatToUse.getContent(locale), valueMap);
            result = result.with(locale, name);
        }

        return result;
    }

    private static LocalizedString defaultNameForAcademicTax(AcademicTax academicTax, Registration registration,
            ExecutionYear executionYear) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        LocalizedString result = new LocalizedString();
        for (final Locale locale : treasuryServices.availableLocales()) {
            String name = null;
            if (academicTax.isAppliedOnRegistration()) {
                name = String.format("%s [%s - %s]", academicTax.getProduct().getName().getContent(locale),
                        registration.getDegree().getPresentationName(executionYear, locale), executionYear.getQualifiedName());
            } else {
                name = String.format("%s [%s]", academicTax.getProduct().getName().getContent(locale),
                        executionYear.getQualifiedName());
            }

            result = result.with(locale, name);
        }

        return result;
    }

    // ANIL 2025-02-06 (#qubIT-Fenix-6602)
    //
    // The financial entity must be received as argument
    private void initAcademicTreasuryEventTarget(FinantialEntity finantialEntity, Product product,
            IAcademicTreasuryTarget target) {
        if (target == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.target.required");
        }

        if (finantialEntity == null) {
            if (target.getAcademicTreasuryTargetDegree() != null) {
                LocalDate eventDate = target.getAcademicTreasuryTargetEventDate();

                if (eventDate == null) {
                    eventDate = new LocalDate();
                }

                finantialEntity =
                        AcademicTreasuryConstants.getFinantialEntityOfDegree(target.getAcademicTreasuryTargetDegree(), eventDate);
            }
        }

        if (finantialEntity == null && FinantialEntity.findAll().count() == 1) {
            finantialEntity = FinantialEntity.findAll().iterator().next();
        }

        super.init(finantialEntity, product, target.getAcademicTreasuryTargetDescription());

        setPerson(target.getAcademicTreasuryTargetPerson());
        setTreasuryEventTarget((AbstractDomainObject) target);
    }

    private void initForCustomAcademicDebt(Product product, Registration registration, ExecutionYear executionYear,
            int customAcademicDebtNumberOfUnits, int customAcademicDebtNumberOfPages, boolean customAcademicDebtUrgent,
            LocalDate customAcademicDebtEventDate, String academicProcessNumber) {
        LocalizedString nameForCustomAcademicDebt = nameForCustomAcademicDebt(product, registration, executionYear);

        if (StringUtils.isNotEmpty(academicProcessNumber)) {
            nameForCustomAcademicDebt = nameForCustomAcademicDebt.append(" [" + academicProcessNumber + "]");
        }

        LocalDate eventDate = customAcademicDebtEventDate;
        if (eventDate == null) {
            eventDate = executionYear.getBeginLocalDate();
        }

        if (eventDate == null) {
            eventDate = new LocalDate();
        }

        FinantialEntity finantialEntity =
                AcademicTreasuryConstants.getFinantialEntityOfDegree(registration.getDegree(), eventDate);

        if (finantialEntity == null && FinantialEntity.findAll().count() == 1) {
            finantialEntity = FinantialEntity.findAll().iterator().next();
        }

        super.init(finantialEntity, product, nameForCustomAcademicDebt);

        setPerson(registration.getPerson());
        setCustomAcademicDebt(true);
        setRegistration(registration);
        setExecutionYear(executionYear);

        setCustomAcademicDebtNumberOfUnits(customAcademicDebtNumberOfUnits);
        setCustomAcademicDebtNumberOfPages(customAcademicDebtNumberOfPages);
        setCustomAcademicDebtUrgent(customAcademicDebtUrgent);
        setCustomAcademicDebtEventDate(customAcademicDebtEventDate);

        super.setAcademicProcessNumber(academicProcessNumber);
    }

    public static LocalizedString nameForCustomAcademicDebt(final Product product, final Registration registration,
            final ExecutionYear executionYear) {
        // ANIL 2025-05-28 (#qubIT-Fenix-6941)
        if (AcademicTreasurySettings.getInstance().getUseCustomAcademicDebtFormat()) {
            return buildCustomNameForCustomAcademicDebt(product, registration, executionYear);
        } else {
            return defaultNameForCustomAcademicDebt(product, registration, executionYear);
        }
    }

    private static LocalizedString buildCustomNameForCustomAcademicDebt(Product product, Registration registration,
            ExecutionYear executionYear) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        LocalizedString result = new LocalizedString();
        for (final Locale locale : treasuryServices.availableLocales()) {
            Map<String, String> valueMap = new HashMap<String, String>();

            String degreePresentationName = registration.getDegree().getPresentationName(executionYear, locale);
            String degreeName = registration.getDegree().getNameI18N(executionYear).getContent(locale);

            LocalizedString productName = product.getName();

            valueMap.put("productName", StringUtils.isNotEmpty(productName.getContent(locale)) ? productName.getContent(
                    locale) : productName.getContent());
            valueMap.put("degreeCode", registration.getDegree().getCode());

            valueMap.put("degreePresentationName", degreePresentationName);
            valueMap.put("degreeName", degreeName);

            valueMap.put("executionYearName", executionYear.getQualifiedName());

            LocalizedString formatToUse = AcademicTreasurySettings.getInstance().getCustomAcademicDebtFormat();
            String name = StrSubstitutor.replace(formatToUse.getContent(locale), valueMap);

            result = result.with(locale, name);
        }

        return result;
    }

    private static LocalizedString defaultNameForCustomAcademicDebt(Product product, Registration registration,
            ExecutionYear executionYear) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        LocalizedString result = new LocalizedString();
        for (final Locale locale : treasuryServices.availableLocales()) {
            final String name = String.format("%s [%s - %s]", product.getName().getContent(),
                    registration.getDegree().getPresentationName(executionYear, locale), executionYear.getQualifiedName());

            result = result.with(locale, name);
        }

        return result;
    }

    @Override
    protected void checkRules() {
        super.checkRules();

        if (getPerson() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.person.required");
        }

        if (!isForAcademicServiceRequest() && !isTuitionEvent() && !isForAcademicTax() && !isForImprovementTax() && !isForTreasuryEventTarget() && !isCustomAcademicDebt()) {
            throw new AcademicTreasuryDomainException(
                    "error.AcademicTreasuryEvent.not.for.service.request.nor.tuition.nor.academic.tax");
        }

        if (!(isForAcademicServiceRequest() ^ isForRegistrationTuition() ^ isForStandaloneTuition() ^ isForExtracurricularTuition() ^ isForImprovementTax() ^ isForAcademicTax() ^ isForTreasuryEventTarget() ^ isCustomAcademicDebt())) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.only.for.one.type");
        }

        if ((isTuitionEvent() || isForImprovementTax()) && getRegistration() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.registration.required");
        }

        if ((isTuitionEvent() || isForImprovementTax()) && getExecutionYear() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.executionYear.required");
        }

        if (isForAcademicServiceRequest() && find(getITreasuryServiceRequest()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.event.for.academicServiceRequest.duplicate");
        }

        if (isForAcademicServiceRequest()) {
            //Ensuring that the Academic Service Request implements the ITreasuryServiceRequest.
            getITreasuryServiceRequest();
        }

        if (isForRegistrationTuition() && findForRegistrationTuition(getRegistration(), getExecutionYear()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.for.registration.tuition.duplicate");
        }

        if (isForStandaloneTuition() && findForStandaloneTuition(getRegistration(), getExecutionYear()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.for.standalone.tuition.duplicate");
        }

        if (isForExtracurricularTuition() && findForExtracurricularTuition(getRegistration(), getExecutionYear()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.for.extracurricular.tuition.duplicate");
        }

        if (isForImprovementTax() && findForImprovementTuition(getRegistration(), getExecutionYear()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.for.improvement.tuition.duplicate");
        }

        if (isForAcademicTax() && getExecutionYear() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.for.academic.tax.execution.year.required");
        }

        if (isForAcademicTax() && getRegistration() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.for.academic.tax.registration.required");
        }

        if (isForAcademicTax() && findForAcademicTax(getRegistration(), getExecutionYear(), getAcademicTax()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.for.academic.tax.duplicate");
        }

        if (isForCustomAcademicDebt() && getRegistration() == null) {
            throw new AcademicTreasuryDomainException(
                    "error.AcademicTreasuryEvent.for.custom.academic.debt.registration.required");
        }

        if (isForCustomAcademicDebt() && getExecutionYear() == null) {
            throw new AcademicTreasuryDomainException(
                    "error.AcademicTreasuryEvent.for.custom.academic.debt.execution.year.required");
        }

        if (isForCustomAcademicDebt() && getCustomAcademicDebtEventDate() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.for.custom.academic.debt.event.date.required");
        }

    }

    public boolean isCustomAcademicDebt() {
        return getCustomAcademicDebt();
    }

    public boolean isForCustomAcademicDebt() {
        return isCustomAcademicDebt();
    }

    public boolean isForTreasuryEventTarget() {
        return getTreasuryEventTarget() != null;
    }

    public boolean isForAcademicServiceRequest() {
        return getITreasuryServiceRequest() != null;
    }

    public boolean isForRegistrationTuition() {
        return getTuitionPaymentPlanGroup() != null && getTuitionPaymentPlanGroup().isForRegistration();
    }

    public boolean isForStandaloneTuition() {
        return getTuitionPaymentPlanGroup() != null && getTuitionPaymentPlanGroup().isForStandalone();
    }

    public boolean isLegacy() {
        return false;
    }

    public boolean isForLegacy() {
        return isLegacy();
    }

    public boolean isCustomAcademicDebtUrgent() {
        return getCustomAcademicDebtUrgent();
    }

    public boolean isForExtracurricularTuition() {
        return getTuitionPaymentPlanGroup() != null && getTuitionPaymentPlanGroup().isForExtracurricular();
    }

    public boolean isForImprovementTax() {
        return getAcademicTax() != null && getAcademicTax() == AcademicTreasurySettings.getInstance().getImprovementAcademicTax();
    }

    public boolean isForAcademicTax() {
        return getAcademicTax() != null && !isImprovementTax();
    }

    public static int numberOfUnits(final ITreasuryServiceRequest iTreasuryServiceRequest) {
        return AcademicTreasuryConstants.getNumberOfUnits(iTreasuryServiceRequest);
    }

    public int getNumberOfUnits() {
        if (isForAcademicServiceRequest()) {
            return numberOfUnits(getITreasuryServiceRequest());
        } else if (isForAcademicTax()) {
            return 0;
        } else if (isForImprovementTax()) {
            return 0;
        } else if (isTuitionEvent()) {
            return 0;
        } else if (isForCustomAcademicDebt()) {
            return getCustomAcademicDebtNumberOfUnits();
        }

        throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.numberOfUnits.not.applied");
    }

    public static int numberOfPages(final ITreasuryServiceRequest iTreasuryServiceRequest) {
        return iTreasuryServiceRequest.hasNumberOfPages() ? iTreasuryServiceRequest.getNumberOfPages() : 0;
    }

    public int getNumberOfPages() {
        if (isForAcademicServiceRequest()) {
            return numberOfPages(getITreasuryServiceRequest());
        } else if (isForAcademicTax()) {
            return 0;
        } else if (isForImprovementTax()) {
            return 0;
        } else if (isTuitionEvent()) {
            return 0;
        } else if (isForCustomAcademicDebt()) {
            return getCustomAcademicDebtNumberOfPages();
        }

        throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.numberOfPages.not.applied");
    }

    public static boolean urgentRequest(final ITreasuryServiceRequest iTreasuryServiceRequest) {
        return iTreasuryServiceRequest.isUrgent();
    }

    public boolean isUrgentRequest() {
        if (isForAcademicServiceRequest()) {
            return urgentRequest(getITreasuryServiceRequest());
        } else if (isForAcademicTax()) {
            return false;
        } else if (isForImprovementTax()) {
            return false;
        } else if (isTuitionEvent()) {
            return false;
        } else if (isForCustomAcademicDebt()) {
            return isCustomAcademicDebtUrgent();
        }

        throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.urgentRequest.not.applied");
    }

    public LocalDate getRequestDate() {
        if (isForAcademicServiceRequest()) {
            return getITreasuryServiceRequest().getRequestDate().toLocalDate();
        } else if (isForAcademicTax() && !isForImprovementTax()) {
            final RegistrationDataByExecutionYear registrationDataByExecutionYear =
                    findRegistrationDataByExecutionYear(getRegistration(), getExecutionYear());

            return registrationDataByExecutionYear != null ? registrationDataByExecutionYear.getEnrolmentDate() : new LocalDate();
        } else if (isForImprovementTax()) {
            final RegistrationDataByExecutionYear registrationDataByExecutionYear =
                    findRegistrationDataByExecutionYear(getRegistration(), getExecutionYear());

            return registrationDataByExecutionYear != null ? registrationDataByExecutionYear.getEnrolmentDate() : new LocalDate();
        }

        throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.requestDate.not.applied");
    }

    public static Locale language(final ITreasuryServiceRequest iTreasuryServiceRequest) {
        return iTreasuryServiceRequest.getLanguage();
    }

    public Locale getLanguage() {
        if (isForAcademicServiceRequest()) {
            return language(getITreasuryServiceRequest());
        } else if (isForAcademicTax()) {
            return null;
        } else if (isForImprovementTax()) {
            return null;
        } else if (isTuitionEvent()) {
            return null;
        } else if (isForCustomAcademicDebt()) {
            return null;
        }

        throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.language.not.applied");
    }

    public boolean isChargedWithDebitEntry(final TuitionInstallmentTariff tariff) {
        return DebitEntry.findActive(this).filter(d -> d.getProduct().equals(tariff.getProduct())).count() > 0;
    }

    @Override
    public boolean isCharged() {
        return TreasuryEventDefaultMethods.isCharged(this);
    }

    public boolean isChargedWithDebitEntry(final Enrolment enrolment) {
        if (!isForStandaloneTuition() && !isForExtracurricularTuition()) {
            throw new RuntimeException("wrong call");
        }

        return findActiveEnrolmentDebitEntry(enrolment).isPresent();
    }

    public boolean isChargedWithDebitEntry(final EnrolmentEvaluation enrolmentEvaluation) {
        if (!isForImprovementTax()) {
            throw new RuntimeException("wrong call");
        }

        return findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).isPresent();
    }

    @Override
    public LocalDate getTreasuryEventDate() {
        if (isForAcademicServiceRequest()) {
            return getITreasuryServiceRequest().getRequestDate().toLocalDate();
        } else if (isForImprovementTax() || isForAcademicTax() || isForRegistrationTuition() || isForExtracurricularTuition() || isForStandaloneTuition()) {

            final RegistrationDataByExecutionYear data =
                    findRegistrationDataByExecutionYear(getRegistration(), getExecutionYear());

            if (data != null && data.getEnrolmentDate() != null) {
                return data.getEnrolmentDate();
            }

            return getExecutionYear().getBeginLocalDate();
        } else if (isForTreasuryEventTarget()) {
            return ((IAcademicTreasuryTarget) getTreasuryEventTarget()).getAcademicTreasuryTargetEventDate();
        } else if (isForCustomAcademicDebt()) {
            return getCustomAcademicDebtEventDate();
        }

        throw new RuntimeException("dont know how to handle this!");
    }

    public Optional<? extends DebitEntry> findActiveAcademicServiceRequestDebitEntry() {
        return DebitEntry.findActive(this).findFirst();
    }

    public Optional<? extends DebitEntry> findActiveEnrolmentDebitEntry(final Enrolment enrolment) {
        return DebitEntry.findActive(this)
                .filter(d -> d.getCurricularCourse() == enrolment.getCurricularCourse() && d.getExecutionSemester() == enrolment.getExecutionPeriod())
                .findFirst();
    }

    public Optional<? extends DebitEntry> findActiveEnrolmentEvaluationDebitEntry(final EnrolmentEvaluation enrolmentEvaluation) {
        return DebitEntry.findActive(this).filter(d -> d.getCurricularCourse() == enrolmentEvaluation.getEnrolment()
                        .getCurricularCourse() && d.getExecutionSemester() == enrolmentEvaluation.getExecutionPeriod() && d.getEvaluationSeason() == enrolmentEvaluation.getEvaluationSeason())
                .findFirst();
    }

    public void associateEnrolment(final DebitEntry debitEntry, final Enrolment enrolment) {
        if (!isForStandaloneTuition() && !isForExtracurricularTuition()) {
            throw new RuntimeException("wrong call");
        }

        if (enrolment == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.enrolment.cannot.be.null");
        }

        if (enrolment.isOptional()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.optional.enrolments.not.supported");
        }

        debitEntry.setCurricularCourse(enrolment.getCurricularCourse());
        debitEntry.setExecutionSemester(enrolment.getExecutionInterval());
    }

    public void associateEnrolmentEvaluation(final DebitEntry debitEntry, final EnrolmentEvaluation enrolmentEvaluation) {
        if (!isForImprovementTax()) {
            throw new RuntimeException("wrong call");
        }

        if (enrolmentEvaluation == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.enrolmentEvaluation.cannot.be.null");
        }

        debitEntry.setCurricularCourse(enrolmentEvaluation.getEnrolment().getCurricularCourse());
        debitEntry.setExecutionSemester(enrolmentEvaluation.getEnrolment().getExecutionInterval());

        if (enrolmentEvaluation.getExecutionInterval() != null) {
            debitEntry.setExecutionSemester(enrolmentEvaluation.getExecutionInterval());
        }

        debitEntry.setEvaluationSeason(enrolmentEvaluation.getEvaluationSeason());
    }

    @Override
    public Set<Product> getPossibleProductsToExempt() {
        if (isForRegistrationTuition()) {
            return TuitionPaymentPlan.find(getTuitionPaymentPlanGroup(),
                            getRegistration().getStudentCurricularPlan(getExecutionYear()).getDegreeCurricularPlan(), getExecutionYear())
                    .map(t -> t.getTuitionInstallmentTariffsSet()).reduce((a, b) -> Sets.union(a, b)).orElse(Sets.newHashSet())
                    .stream().map(i -> i.getProduct()).collect(Collectors.toSet());
        }

        return Sets.newHashSet(getProduct());
    }

    private LocalizedString descriptionForAcademicServiceRequest() {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        final ServiceRequestMapEntry serviceRequestMapEntry = ServiceRequestMapEntry.findMatch(getITreasuryServiceRequest());

        LocalizedString result = new LocalizedString();

        for (final Locale locale : treasuryServices.availableLocales()) {
            String text =
                    getProduct().getName().getContent(locale) + ": " + getITreasuryServiceRequest().getServiceRequestNumberYear();

            if (!StringUtils.isEmpty(serviceRequestMapEntry.getDebitEntryDescriptionExtensionFormat())) {
                final StrSubstitutor str = new StrSubstitutor(getITreasuryServiceRequest().getPropertyValuesMap());

                final String extString = str.replace(serviceRequestMapEntry.getDebitEntryDescriptionExtensionFormat());
                text += " " + extString;
            }

            result = result.with(locale, text);
        }

        return result;
    }

    @Override
    public String getDegreeCode() {
        if (getDegree() == null) {
            return null;
        }

        return getDegree().getCode();
    }

    @Override
    public String getDegreeName() {
        if (getDegree() == null) {
            return null;
        }

        if (getExecutionYear() != null) {
            return getDegree().getPresentationName(getExecutionYear());
        }

        return getDegree().getPresentationName();
    }

    @Override
    public Degree getDegree() {
        Degree degree = degree();

        if (degree != null) {
            return degree;
        }

        return super.getDegree();
    }

    @Override
    public String getExecutionYearName() {
        if (super.getExecutionYear() != null) {
            return super.getExecutionYear().getQualifiedName();
        } else if (isForTreasuryEventTarget() && ((IAcademicTreasuryTarget) getTreasuryEventTarget()).getAcademicTreasuryTargetExecutionYear() != null) {
            return ((IAcademicTreasuryTarget) getTreasuryEventTarget()).getAcademicTreasuryTargetExecutionYear()
                    .getQualifiedName();
        }

        return null;
    }

    private Degree degree() {
        Degree degree = null;

        if (isForRegistrationTuition() && getRegistration() != null) {
            degree = getRegistration().getDegree();
        } else if (isForStandaloneTuition() || isForExtracurricularTuition()) {
            if (getRegistration() != null) {
                degree = getRegistration().getDegree();
            }
        } else if (isForImprovementTax()) {
            if (getRegistration() != null) {
                degree = getRegistration().getDegree();
            }
        } else if (isForAcademicTax() && getRegistration() != null) {
            degree = getRegistration().getDegree();
        } else if (isForAcademicServiceRequest() && getRegistration() != null) {
            degree = getRegistration().getDegree();
        } else if (isForCustomAcademicDebt() && getRegistration() != null) {
            degree = getRegistration().getDegree();
        } else if (isForTreasuryEventTarget() && ((IAcademicTreasuryTarget) getTreasuryEventTarget()).getAcademicTreasuryTargetDegree() != null) {
            return ((IAcademicTreasuryTarget) getTreasuryEventTarget()).getAcademicTreasuryTargetDegree();
        } else if (isForAcademicServiceRequest() && getITreasuryServiceRequest() != null && getITreasuryServiceRequest().getRegistration() != null) {
            degree = getITreasuryServiceRequest().getRegistration().getDegree();
        }

        return degree;
    }

    // Category code for treasuryEvent propose
    public AcademicTreasuryEventType getTreasuryEventTypeCode() {
        if (isForAcademicServiceRequest()) {
            return AcademicTreasuryEventType.ACADEMIC_SERVICE_REQUEST;
        } else if (isForAcademicTax()) {
            return AcademicTreasuryEventType.ACADEMIC_TAX;
        } else if (isForExtracurricularTuition()) {
            return AcademicTreasuryEventType.EXTRACURRICULAR_TUITION;
        } else if (isForImprovementTax()) {
            return AcademicTreasuryEventType.IMPROVEMENT_TAX;
        } else if (isForRegistrationTuition()) {
            return AcademicTreasuryEventType.REGISTRATION_TUITION;
        } else if (isForStandaloneTuition()) {
            return AcademicTreasuryEventType.STANDALONE_TUITION;
        } else if (isForTreasuryEventTarget()) {
            return AcademicTreasuryEventType.TREASURY_EVENT_TARGET;
        } else if (isLegacy()) {
            return AcademicTreasuryEventType.LEGACY;
        } else if (isCustomAcademicDebt()) {
            return AcademicTreasuryEventType.CUSTOM_ACADEMIC_DEBT;
        }

        throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.unkwnown.type");
    }

    public void mergeToTargetPerson(Person targetPerson) {
        super.setPerson(targetPerson);

        if (isForTreasuryEventTarget() && getTreasuryEventTarget() != null && getTreasuryEventTarget() instanceof ReservationTaxEventTarget) {
            ((ReservationTaxEventTarget) getTreasuryEventTarget()).mergeToTargetPerson(targetPerson);
        }
    }

    public void mergeDebitEntriesAndExemptions(final AcademicTreasuryEvent event) {
        if (this.getTreasuryEventTypeCode() != event.getTreasuryEventTypeCode()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.cannot.merge.for.different.types");
        }

        if (isForAcademicServiceRequest()) {
            // Does not make sense
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.cannot.merge.not.supported");
        } else if (isForAcademicTax()) {
            if (getExecutionYear() != event.getExecutionYear()) {
                throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.cannot.merge.different.execution.year");
            }

            if (getAcademicTax() != event.getAcademicTax()) {
                throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.cannot.merge.different.academic.tax");
            }
        } else if (isForExtracurricularTuition()) {
            if (getExecutionYear() != event.getExecutionYear()) {
                throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.cannot.merge.different.execution.year");
            }

        } else if (isForImprovementTax()) {
            if (getExecutionYear() != event.getExecutionYear()) {
                throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.cannot.merge.different.execution.year");
            }
        } else if (isForRegistrationTuition()) {
            if (getExecutionYear() != event.getExecutionYear()) {
                throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.cannot.merge.different.execution.year");
            }
        } else if (isForStandaloneTuition()) {
            if (getExecutionYear() != event.getExecutionYear()) {
                throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.cannot.merge.different.execution.year");
            }
        } else if (isForTreasuryEventTarget()) {
            // Does not make sense
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.cannot.merge.not.supported");
        } else if (isForLegacy()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.cannot.merge.not.supported");
        } else if (isCustomAcademicDebt()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.cannot.merge.not.supported");
        } else {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.unkwnown.type");
        }

        for (final DebitEntry debitEntry : Sets.newHashSet(event.getDebitEntriesSet())) {
            debitEntry.setTreasuryEvent(this);
        }

    }

    @Override
    public void copyDebitEntryInformation(final DebitEntry sourceDebitEntry, final DebitEntry copyDebitEntry) {
        copyDebitEntry.setCurricularCourse(sourceDebitEntry.getCurricularCourse());
        copyDebitEntry.setEvaluationSeason(sourceDebitEntry.getEvaluationSeason());
        copyDebitEntry.setExecutionSemester(sourceDebitEntry.getExecutionSemester());
    }

    @Override
    public boolean isDeletable() {
        return getDebitEntriesSet().isEmpty() && getTreasuryExemptionsSet().isEmpty();
    }

    @Atomic
    @Override
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.AcademicTreasuryEvent.cannot.delete");
        }

        setPerson(null);
        setITreasuryServiceRequest(null);
        setTuitionPaymentPlanGroup(null);
        setRegistration(null);
        setAcademicTax(null);
        setRegistration(null);
        setExecutionYear(null);
        setAcademicTax(null);
        setTreasuryEventTarget(null);

        super.delete();
    }

    @Override
    public Optional<Tariff> findMatchTariff(FinantialEntity finantialEntity, Product product, LocalDate when) {

        if (getDegree() != null) {
            return Optional.ofNullable(
                    AcademicTariff.findMatch(finantialEntity, product, getDegree(), when.toDateTimeAtStartOfDay()));
        }

        return Optional.ofNullable(AcademicTariff.findMatch(finantialEntity, product, when.toDateTimeAtStartOfDay()));
    }

    @Override
    public boolean isEventAccountedAsTuition() {
        if (isForTreasuryEventTarget()) {
            return ((IAcademicTreasuryTarget) getTreasuryEventTarget()).isEventAccountedAsTuition();
        }

        return isForRegistrationTuition() || isForStandaloneTuition() || isForExtracurricularTuition();
    }

    @Override
    public boolean isEventDiscountInTuitionFee() {
        if (isForTreasuryEventTarget()) {
            return ((IAcademicTreasuryTarget) getTreasuryEventTarget()).isEventDiscountInTuitionFee();
        }

        return false;
    }

    @Override
    public boolean isEventDiscountInTuitionFeeWithTreasuryExemption() {
        return true;
    }

    @Override
    public TreasuryExemptionType getTreasuryExemptionToApplyInEventDiscountInTuitionFee() {
        if (isForTreasuryEventTarget() && (getTreasuryEventTarget() instanceof ReservationTaxEventTarget)) {
            return ((ReservationTaxEventTarget) getTreasuryEventTarget()).getReservationTax().getTreasuryExemptionType();
        }

        return null;
    }

    // @formatter: off

    /************
     * SERVICES *
     ************/
    // @formatter: on
    public static Stream<? extends AcademicTreasuryEvent> findAll() {
        return TreasuryEvent.findAll().filter(e -> e instanceof AcademicTreasuryEvent).map(AcademicTreasuryEvent.class::cast);
    }

    public static Stream<? extends AcademicTreasuryEvent> find(Person person) {
        return person.getAcademicTreasuryEventSet().stream();
    }

    public static Stream<? extends AcademicTreasuryEvent> find(ExecutionYear executionYear) {
        return findAll().filter(e -> e.getExecutionYear() == executionYear);
    }

    public static Stream<? extends AcademicTreasuryEvent> find(Registration registration, ExecutionYear executionYear) {
        return find(registration.getPerson()).filter(e -> e.getRegistration() == registration)
                .filter(l -> l.getExecutionYear() == executionYear);
    }

    /* --- Academic Service Requests --- */

    public static Stream<? extends AcademicTreasuryEvent> find(ITreasuryServiceRequest iTreasuryServiceRequest) {
        if (iTreasuryServiceRequest == null) {
            throw new RuntimeException("wrong call");
        }

        return findAll().filter(e -> e.getITreasuryServiceRequest() != null && e.getITreasuryServiceRequest().getExternalId()
                .equals(iTreasuryServiceRequest.getExternalId()));
    }

    public static Optional<? extends AcademicTreasuryEvent> findUnique(ITreasuryServiceRequest iTreasuryServiceRequest) {
        return find(iTreasuryServiceRequest).findFirst();
    }

    @Deprecated
    public static AcademicTreasuryEvent createForAcademicServiceRequest(ITreasuryServiceRequest iTreasuryServiceRequest) {
        return new AcademicTreasuryEvent(iTreasuryServiceRequest);
    }

    // @formatter: off
    /* *******
     * TUITION
     * *******
     */
    // @formatter: on

    /* For Registration */

    protected static Stream<? extends AcademicTreasuryEvent> findForRegistrationTuition(final Registration registration,
            final ExecutionYear executionYear) {
        return find(registration, executionYear).filter(e -> e.isForRegistrationTuition());
    }

    public static Optional<? extends AcademicTreasuryEvent> findUniqueForRegistrationTuition(final Registration registration,
            final ExecutionYear executionYear) {
        return findForRegistrationTuition(registration, executionYear).findFirst();
    }

    public static AcademicTreasuryEvent createForRegistrationTuition(final Product product, final Registration registration,
            final ExecutionYear executionYear) {
        return new AcademicTreasuryEvent(TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get(), product,
                registration, executionYear);
    }

    /* For Standalone */

    protected static Stream<? extends AcademicTreasuryEvent> findForStandaloneTuition(final Registration registration,
            final ExecutionYear executionYear) {
        return find(registration, executionYear).filter(e -> e.isForStandaloneTuition());
    }

    public static Optional<? extends AcademicTreasuryEvent> findUniqueForStandaloneTuition(final Registration registration,
            final ExecutionYear executionYear) {
        return findForStandaloneTuition(registration, executionYear).findFirst();
    }

    public static AcademicTreasuryEvent createForStandaloneTuition(final Product product, final Registration registration,
            final ExecutionYear executionYear) {
        return new AcademicTreasuryEvent(TuitionPaymentPlanGroup.findUniqueDefaultGroupForStandalone().get(), product,
                registration, executionYear);
    }

    /* For Extracurricular */

    protected static Stream<? extends AcademicTreasuryEvent> findForExtracurricularTuition(final Registration registration,
            final ExecutionYear executionYear) {
        return find(registration, executionYear).filter(e -> e.isForExtracurricularTuition());
    }

    public static Optional<? extends AcademicTreasuryEvent> findUniqueForExtracurricularTuition(final Registration registration,
            final ExecutionYear executionYear) {
        return findForExtracurricularTuition(registration, executionYear).findFirst();
    }

    public static AcademicTreasuryEvent createForExtracurricularTuition(final Product product, final Registration registration,
            final ExecutionYear executionYear) {
        return new AcademicTreasuryEvent(TuitionPaymentPlanGroup.findUniqueDefaultGroupForExtracurricular().get(), product,
                registration, executionYear);
    }

    /* For Improvement */

    protected static Stream<? extends AcademicTreasuryEvent> findForImprovementTuition(final Registration registration,
            final ExecutionYear executionYear) {
        return find(registration, executionYear).filter(e -> e.isForImprovementTax());
    }

    public static Optional<? extends AcademicTreasuryEvent> findUniqueForImprovementTuition(final Registration registration,
            final ExecutionYear executionYear) {
        return findForImprovementTuition(registration, executionYear).findFirst();
    }

    public static AcademicTreasuryEvent createForImprovementTuition(final Registration registration,
            final ExecutionYear executionYear) {
        return new AcademicTreasuryEvent(AcademicTreasurySettings.getInstance().getImprovementAcademicTax(), registration,
                executionYear);
    }

    // @formatter: off
    /* ************
     * ACADEMIC TAX
     * ************
     */
    // @formatter: on

    public static Stream<? extends AcademicTreasuryEvent> findAllForAcademicTax(final Registration registration,
            final ExecutionYear executionYear) {
        final Set<AcademicTreasuryEvent> result = Sets.newHashSet();

        for (final AcademicTax academicTax : AcademicTax.findAll().collect(Collectors.toSet())) {
            result.addAll(findForAcademicTax(registration, executionYear, academicTax).collect(
                    Collectors.<AcademicTreasuryEvent> toSet()));
        }

        return result.stream();
    }

    public static Stream<? extends AcademicTreasuryEvent> findForAcademicTax(final Registration registration,
            final ExecutionYear executionYear, final AcademicTax academicTax) {
        return find(registration.getPerson()).filter(
                e -> e.isForAcademicTax() && e.getAcademicTax() == academicTax && e.getExecutionYear() == executionYear && (!e.getAcademicTax()
                        .isAppliedOnRegistration() && e.getPerson() == registration.getPerson() || e.getRegistration() == registration));
    }

    public static Optional<? extends AcademicTreasuryEvent> findUniqueForAcademicTax(final Registration registration,
            final ExecutionYear executionYear, final AcademicTax academicTax) {
        return findForAcademicTax(registration, executionYear, academicTax).findFirst();
    }

    public static Optional<? extends AcademicTreasuryEvent> findUniqueForTarget(final Person person,
            final IAcademicTreasuryTarget target) {
        return find(person).filter(a -> a.getTreasuryEventTarget() == target).findFirst();
    }

    public static Stream<? extends AcademicTreasuryEvent> findByDescription(final Person person, final String description,
            final boolean trimmed) {
        return find(person).filter(
                t -> (!trimmed && t.getDescription().getContent().equals(description)) || (trimmed && t.getDescription()
                        .getContent().trim().equals(description)));
    }

    public static AcademicTreasuryEvent createForAcademicTax(final AcademicTax academicTax, final Registration registration,
            final ExecutionYear executionYear) {
        return new AcademicTreasuryEvent(academicTax, registration, executionYear);
    }

    // ANIL 2025-02-06 (#qubIT-Fenix-6602)
    //
    // The finantial entity must be received as argument
    public static AcademicTreasuryEvent createForAcademicTreasuryEventTarget(FinantialEntity finantialEntity, Product product,
            IAcademicTreasuryTarget target) {
        return new AcademicTreasuryEvent(finantialEntity, product, target);
    }

    // @formatter: off
    /* ********************
     * CUSTOM ACADEMIC DEBT
     * ********************
     */
    // @formatter: on

    public static AcademicTreasuryEvent createForCustomAcademicDebt(Product product, Registration registration,
            ExecutionYear executionYear, int customAcademicDebtNumberOfUnits, int customAcademicDebtNumberOfPages,
            boolean customAcademicDebtUrgent, LocalDate customAcademicDebtEventDate, String academicProcessNumber) {
        return new AcademicTreasuryEvent(product, registration, executionYear, customAcademicDebtNumberOfUnits,
                customAcademicDebtNumberOfPages, customAcademicDebtUrgent, customAcademicDebtEventDate, academicProcessNumber);
    }

    public static Stream<? extends AcademicTreasuryEvent> findForCustomAcademicDebt(final Product product,
            final Registration registration, final ExecutionYear executionYear) {

        // ANIL 2025-06-26 (#qubIT-Fenix-7151) - The registration must be filtered
        return find(registration.getPerson()) //
                .filter(e -> e.getExecutionYear() == executionYear) //
                .filter(e -> e.isCustomAcademicDebt()) //
                .filter(e -> e.getRegistration() == registration) //
                .filter(e -> e.getProduct() == product);
    }

    // @formatter:off
    /* -----
     * UTILS
     * -----
     */
    // @formatter:on

    // @formatter:off
    public static enum AcademicTreasuryEventKeys {

        ACADEMIC_SERVICE_REQUEST_NAME("1"),

        ACADEMIC_SERVICE_REQUEST_NUMBER_YEAR("2"),

        EXECUTION_YEAR("3"),

        EXECUTION_SEMESTER("4"),

        EVALUATION_SEASON("5"),

        DETAILED("6"),

        URGENT("7"),

        LANGUAGE("8"),

        BASE_AMOUNT("9"),

        UNITS_FOR_BASE("10"),

        UNIT_AMOUNT("11"),

        ADDITIONAL_UNITS("12"),

        CALCULATED_UNITS_AMOUNT("13"),

        PAGE_AMOUNT("14"),

        NUMBER_OF_PAGES("15"),

        CALCULATED_PAGES_AMOUNT("16"),

        MAXIMUM_AMOUNT("17"),

        AMOUNT_WITHOUT_RATES("18"),

        FOREIGN_LANGUAGE_RATE("19"),

        CALCULATED_FOREIGN_LANGUAGE_RATE("20"),

        URGENT_PERCENTAGE("21"),

        CALCULATED_URGENT_AMOUNT("22"),

        FINAL_AMOUNT("23"),

        TUITION_PAYMENT_PLAN("24"),

        TUITION_PAYMENT_PLAN_CONDITIONS("25"),

        TUITION_CALCULATION_TYPE("26"),

        FIXED_AMOUNT("27"),

        ECTS_CREDITS("28"),

        AMOUNT_PER_ECTS("29"),

        ENROLLED_COURSES("30"),

        AMOUNT_PER_COURSE("31"),

        DUE_DATE("32"),

        DEGREE("33"),

        DEGREE_CODE("34"),

        DEGREE_CURRICULAR_PLAN("35"),

        ENROLMENT("36"),

        FACTOR("37"),

        TOTAL_ECTS_OR_UNITS("38"),

        COURSE_FUNCTION_COST("39"),

        DEFAULT_TUITION_TOTAL_AMOUNT("40"),

        USED_DATE("41"),

        TUITION_PAYOR_DEBT_ACCOUNT("42"),

        EFFECTIVE_TUITION_PAYMENT_PLAN("43"),

        CUSTOM_CALCULATOR("44"),

        CALCULATED_AMOUNT_TYPE("45");

        private String code;

        private AcademicTreasuryEventKeys(final String code) {
            this.code = code;
        }

        public LocalizedString getDescriptionI18N() {
            return AcademicTreasuryConstants.academicTreasuryBundleI18N(
                    "label." + AcademicTreasuryEventKeys.class.getSimpleName() + "." + name());
        }

        public static String valueFor(final DebitEntry debitEntry, final AcademicTreasuryEventKeys key) {
            if (debitEntry.getPropertiesMap() == null) {
                return null;
            }

            // HACK Should retrieve with code and not with the description
            final LocalizedString descriptionI18N = key.getDescriptionI18N();
            if (debitEntry.getPropertiesMap()
                    .containsKey(descriptionI18N.getContent(AcademicTreasuryConstants.DEFAULT_LANGUAGE))) {
                return debitEntry.getPropertiesMap().get(descriptionI18N.getContent(AcademicTreasuryConstants.DEFAULT_LANGUAGE));
            }

            return null;
        }
    }

    // @formatter:on

    private Map<String, String> fillPropertiesMap() {
        final Map<String, String> propertiesMap = Maps.newHashMap();

        //THIS IS WRONG!!! - Ricardo Pedro 5-9-2015
        // The properties MAP SHOULD BE FILLED WITH KEY:VALUE
        // Then in the JSP should be used the "getDescriptionI18N().getContent()" to show to the description

        if (isForAcademicServiceRequest()) {
            propertiesMap.put(AcademicTreasuryEventKeys.ACADEMIC_SERVICE_REQUEST_NAME.getDescriptionI18N().getContent(),
                    getITreasuryServiceRequest().getServiceRequestType().getName().getContent());

            propertiesMap.put(AcademicTreasuryEventKeys.ACADEMIC_SERVICE_REQUEST_NUMBER_YEAR.getDescriptionI18N().getContent(),
                    getITreasuryServiceRequest().getServiceRequestNumberYear());

            propertiesMap.put(AcademicTreasuryEventKeys.DEGREE.getDescriptionI18N().getContent(),
                    getITreasuryServiceRequest().getRegistration().getDegree()
                            .getPresentationName(getITreasuryServiceRequest().getExecutionYear()));

            propertiesMap.put(AcademicTreasuryEventKeys.DEGREE_CODE.getDescriptionI18N().getContent(),
                    getITreasuryServiceRequest().getRegistration().getDegree().getCode());

            if (getITreasuryServiceRequest().hasExecutionYear()) {
                propertiesMap.put(AcademicTreasuryEventKeys.EXECUTION_YEAR.getDescriptionI18N().getContent(),
                        getITreasuryServiceRequest().getExecutionYear().getQualifiedName());
            }

            propertiesMap.put(AcademicTreasuryEventKeys.DETAILED.getDescriptionI18N().getContent(),
                    booleanLabel(getITreasuryServiceRequest().isDetailed()).getContent());
            propertiesMap.put(AcademicTreasuryEventKeys.URGENT.getDescriptionI18N().getContent(),
                    booleanLabel(getITreasuryServiceRequest().isUrgent()).getContent());
            if (getITreasuryServiceRequest().hasLanguage()) {
                propertiesMap.put(AcademicTreasuryEventKeys.LANGUAGE.getDescriptionI18N().getContent(),
                        getITreasuryServiceRequest().getLanguage().getLanguage());
            }
        } else if (isForRegistrationTuition() || isForStandaloneTuition() || isForExtracurricularTuition()) {
            propertiesMap.put(AcademicTreasuryEventKeys.EXECUTION_YEAR.getDescriptionI18N().getContent(),
                    getExecutionYear().getQualifiedName());
            propertiesMap.put(AcademicTreasuryEventKeys.DEGREE.getDescriptionI18N().getContent(),
                    getRegistration().getDegree().getPresentationName(getExecutionYear()));
            propertiesMap.put(AcademicTreasuryEventKeys.DEGREE_CURRICULAR_PLAN.getDescriptionI18N().getContent(),
                    getRegistration().getDegreeCurricularPlanName());
            propertiesMap.put(AcademicTreasuryEventKeys.DEGREE_CODE.getDescriptionI18N().getContent(),
                    getRegistration().getDegree().getCode());
        }

        return propertiesMap;
    }

    private LocalizedString booleanLabel(final boolean detailed) {
        return AcademicTreasuryConstants.academicTreasuryBundleI18N(detailed ? "label.true" : "label.false");
    }

    public static BigDecimal getEnrolledEctsUnits(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            final Registration registration, final ExecutionYear executionYear) {

        if (tuitionPaymentPlanGroup.isForRegistration()) {

            return TuitionServices.normalEnrolmentsWithoutAnnuled(registration, executionYear).stream()
                    .map(e -> new BigDecimal(e.getEctsCredits())).reduce((a, b) -> a.add(b)).orElse(BigDecimal.ZERO);

        } else if (tuitionPaymentPlanGroup.isForStandalone()) {

            return TuitionServices.standaloneEnrolmentsWithoutAnnuled(registration, executionYear).stream()
                    .map(e -> new BigDecimal(e.getEctsCredits())).reduce((a, c) -> a.add(c)).orElse(BigDecimal.ZERO);

        } else if (tuitionPaymentPlanGroup.isForExtracurricular()) {

            return TuitionServices.extracurricularEnrolmentsWithoutAnnuled(registration, executionYear).stream()
                    .map(e -> new BigDecimal(e.getEctsCredits())).reduce((a, c) -> a.add(c)).orElse(BigDecimal.ZERO);
        }

        throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.unknown.tuition.group");
    }

    public BigDecimal getEnrolledEctsUnits() {
        return getEnrolledEctsUnits(getTuitionPaymentPlanGroup(), getRegistration(), getExecutionYear());
    }

    public static BigDecimal getEnrolledCoursesCount(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            final Registration registration, final ExecutionYear executionYear) {
        if (tuitionPaymentPlanGroup.isForRegistration()) {
            return new BigDecimal(TuitionServices.normalEnrolmentsWithoutAnnuled(registration, executionYear).size());
        } else if (tuitionPaymentPlanGroup.isForStandalone()) {
            return new BigDecimal(TuitionServices.standaloneEnrolmentsWithoutAnnuled(registration, executionYear).size());
        } else if (tuitionPaymentPlanGroup.isForExtracurricular()) {
            return new BigDecimal(TuitionServices.extracurricularEnrolmentsWithoutAnnuled(registration, executionYear).size());
        }

        throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.unknown.tuition.group");
    }

    public BigDecimal getEnrolledCoursesCount() {
        return getEnrolledCoursesCount(getTuitionPaymentPlanGroup(), getRegistration(), getExecutionYear());
    }

    public void updatePricingFields(final BigDecimal baseAmount, final BigDecimal amountForAdditionalUnits,
            final BigDecimal amountForPages, final BigDecimal maximumAmount, final BigDecimal amountForLanguageTranslationRate,
            final BigDecimal amountForUrgencyRate) {

        super.setBaseAmount(baseAmount);
        super.setAmountForAdditionalUnits(amountForAdditionalUnits);
        super.setAmountForPages(amountForPages);
        super.setMaximumAmount(maximumAmount);
        super.setAmountForLanguageTranslationRate(amountForLanguageTranslationRate);
        super.setAmountForUrgencyRate(amountForUrgencyRate);
    }

    /* ----------------------
     * IAcademicTreasuryEvent
     * ----------------------
     */

    @Override
    public String getDebtAccountURL() {
        return TreasuryEventDefaultMethods.getDebtAccountURL(this);
    }

    /* -------------------------
     * KIND OF EVENT INFORMATION
     * -------------------------
     */

    @Override
    public boolean isTuitionEvent() {
        return isForRegistrationTuition() || isForStandaloneTuition() || isForExtracurricularTuition();
    }

    @Override
    public boolean isAcademicServiceRequestEvent() {
        return isForAcademicServiceRequest();
    }

    @Override
    public boolean isAcademicTax() {
        return isForAcademicTax();
    }

    @Override
    public boolean isImprovementTax() {
        return isForImprovementTax();
    }

    /* ---------------------
     * FINANTIAL INFORMATION
     * ---------------------
     */

    @Override
    public boolean isExempted() {
        return TreasuryEventDefaultMethods.isExempted(this);
    }

    @Override
    public boolean isDueDateExpired(final LocalDate when) {
        return TreasuryEventDefaultMethods.isDueDateExpired(this, when);
    }

    @Override
    public boolean isBlockingAcademicalActs(final LocalDate when) {
        return TreasuryEventDefaultMethods.isBlockingAcademicalActs(this, when);
    }

    @Override
    public LocalDate getDueDate() {
        return DebitEntry.findActive(this).sorted(DebitEntry.COMPARE_BY_DUE_DATE).map(l -> l.getDueDate()).findFirst()
                .orElse(null);
    }

    public LocalDate getDueDate(Product product) {
        return DebitEntry.findActive(this, product).sorted(DebitEntry.COMPARE_BY_DUE_DATE).map(l -> l.getDueDate()).findFirst()
                .orElse(null);
    }

    @Override
    public String getExemptionReason() {
        return TreasuryEventDefaultMethods.getExemptionReason(this);
    }

    @Override
    public String getExemptionTypeName() {
        return TreasuryEventDefaultMethods.getExemptionTypeName(this, AcademicTreasuryConstants.DEFAULT_LANGUAGE);
    }

    @Override
    public String getExemptionTypeName(final Locale locale) {
        return TreasuryEventDefaultMethods.getExemptionTypeName(this, locale);
    }

    @Override
    public List<IAcademicTreasuryEventPayment> getPaymentsList() {
        return TreasuryEventDefaultMethods.getPaymentsList(this);
    }

    @Override
    public void invokeSettlementCallbacks(SettlementNote settlementNote) {
        if (isForTreasuryEventTarget()) {
            ((IAcademicTreasuryTarget) getTreasuryEventTarget()).handleSettlement(this);
        }
    }

    @Override
    public void invokeSettlementCallbacks(TreasuryExemption treasuryExemption) {
        if (isForTreasuryEventTarget()) {
            ((IAcademicTreasuryTarget) getTreasuryEventTarget()).handleSettlement(this);
        }
    }

    /* ---------------------------------------------
     * ACADEMIC SERVICE REQUEST EVENT & ACADEMIC TAX
     * ---------------------------------------------
     */

    @Override
    public BigDecimal getBaseAmount() {
        if (!isChargedWithDebitEntry()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.baseAmount.unavailable");
        }

        return super.getBaseAmount();
    }

    @Override
    public BigDecimal getAdditionalUnitsAmount() {
        if (!isChargedWithDebitEntry()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.additionalUnitsAmount.unavailable");
        }

        return super.getAmountForAdditionalUnits();
    }

    @Override
    public BigDecimal getMaximumAmount() {
        if (!isChargedWithDebitEntry()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.maximumAmount.unavailable");
        }

        return super.getMaximumAmount();
    }

    @Override
    public BigDecimal getPagesAmount() {
        if (!isChargedWithDebitEntry()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.pagesAmount.unavailable");
        }

        return super.getAmountForPages();
    }

    @Override
    public BigDecimal getAmountForLanguageTranslationRate() {
        if (!isChargedWithDebitEntry()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.amountForLanguageTranslationRate.unavailable");
        }

        return super.getAmountForLanguageTranslationRate();
    }

    @Override
    public BigDecimal getAmountForUrgencyRate() {
        if (!isChargedWithDebitEntry()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.amountForUrgencyRate.unavailable");
        }

        return super.getAmountForUrgencyRate();
    }

    /*
     * -----------
     * IMPROVEMENT
     * -----------
     */

    @Override
    public boolean isCharged(final EnrolmentEvaluation enrolmentEvaluation) {
        return isChargedWithDebitEntry(enrolmentEvaluation);
    }

    @Override
    public boolean isExempted(final EnrolmentEvaluation enrolmentEvaluation) {
        if (!findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).isPresent()) {
            return false;
        }

        final DebitEntry debitEntry = findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).get();

        return TreasuryExemption.findUnique(this, debitEntry.getProduct()).isPresent();
    }

    @Override
    public boolean isDueDateExpired(final EnrolmentEvaluation enrolmentEvaluation, final LocalDate when) {
        if (!findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).isPresent()) {
            return false;
        }

        final DebitEntry debitEntry = findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).get();
        return debitEntry.isDueDateExpired(when);
    }

    @Override
    public boolean isBlockingAcademicalActs(final EnrolmentEvaluation enrolmentEvaluation, final LocalDate when) {
        if (!findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).isPresent()) {
            return false;
        }

        final DebitEntry debitEntry = findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).get();
        return debitEntry.isInDebt() && debitEntry.isDueDateExpired(when);
    }

    @Override
    public BigDecimal getAmountWithVatToPay(final EnrolmentEvaluation enrolmentEvaluation) {
        if (!findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).isPresent()) {
            return BigDecimal.ZERO;
        }

        final DebitEntry debitEntry = findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).get();
        return debitEntry.getAmount();
    }

    @Override
    public BigDecimal getRemainingAmountToPay(final EnrolmentEvaluation enrolmentEvaluation) {
        if (!findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).isPresent()) {
            return BigDecimal.ZERO;
        }

        final DebitEntry debitEntry = findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).get();
        return debitEntry.getOpenAmount();
    }

    @Override
    public BigDecimal getNetExemptedAmount(final EnrolmentEvaluation enrolmentEvaluation) {
        if (!isExempted(enrolmentEvaluation)) {
            return BigDecimal.ZERO;
        }

        final DebitEntry debitEntry = findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).get();
        return debitEntry.getOpenAmount();
    }

    @Override
    public LocalDate getDueDate(final EnrolmentEvaluation enrolmentEvaluation) {
        if (!isChargedWithDebitEntry(enrolmentEvaluation)) {
            return null;
        }

        final DebitEntry debitEntry = findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).get();
        return debitEntry.getDueDate();
    }

    @Override
    public String getExemptionReason(final EnrolmentEvaluation enrolmentEvaluation) {
        return getExemptionReason();
    }

    @Override
    public List<IAcademicTreasuryEventPayment> getPaymentsList(final EnrolmentEvaluation enrolmentEvaluation) {
        if (!isChargedWithDebitEntry(enrolmentEvaluation)) {
            return Collections.emptyList();
        }

        return findActiveEnrolmentEvaluationDebitEntry(enrolmentEvaluation).get().getSettlementEntriesSet().stream()
                .filter(l -> l.getFinantialDocument().isClosed()).map(l -> new AcademicTreasuryEventPayment(l))
                .collect(Collectors.toList());
    }

    @Override
    public String formatMoney(final BigDecimal moneyValue) {
        return TreasuryEventDefaultMethods.formatMoney(this, moneyValue);
    }

    @Override
    public boolean isOnlinePaymentsActive() {
        return TreasuryEventDefaultMethods.isOnlinePaymentsActive(this);
    }

    @Override
    public void annulDebts(final String reason) {
        TreasuryEventDefaultMethods.annulDebts(this, reason);
    }

    @Override
    public List<IPaymentReferenceCode> getPaymentReferenceCodesList() {
        return TreasuryEventDefaultMethods.getPaymentReferenceCodesList(this);
    }

    /*
     * This is used only for methods above
     */
    protected List<DebitEntry> orderedTuitionDebitEntriesList() {
        if (!isForRegistrationTuition()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.only.tuition.for.registration.supported");
        }

        return DebitEntry.findActive(this).sorted((a, b) -> a.getExternalId().compareTo(b.getExternalId()))
                .collect(Collectors.<DebitEntry> toList());
    }

    @Override
    public String getERPIntegrationMetadata() {
        String degreeCode = "";
        String executionYear = "";
//HACK: This should be done using GJSON
        if (this.getDegree() != null) {
            degreeCode = this.getDegree().getCode();
        } else {
            if (this.getPropertiesMap().containsKey(AcademicTreasuryEventKeys.DEGREE_CODE)) {
                degreeCode = this.getPropertiesMap().get(AcademicTreasuryEventKeys.DEGREE_CODE);
            }
        }
        if (this.getExecutionYear() != null) {
            executionYear = this.getExecutionYear().getQualifiedName();
        } else {
            if (this.getPropertiesMap().containsKey(AcademicTreasuryEventKeys.EXECUTION_YEAR)) {
                executionYear = this.getPropertiesMap().get(AcademicTreasuryEventKeys.EXECUTION_YEAR);
            }
        }
        return "{\"" + AcademicTreasuryEventKeys.DEGREE_CODE + "\":\"" + degreeCode + "\",\"" + AcademicTreasuryEventKeys.EXECUTION_YEAR + "\":\"" + executionYear + "\"}";
    }

    @Override
    public LocalizedString getEventTargetCurrentState() {
        ITreasuryPlatformDependentServices platformServices = TreasuryPlataformDependentServicesFactory.implementation();
        // Catch the exception and log only, to prevent an error
        // that will deny access to the debt account and
        // execution of treasury operations

        try {

            if (isForAcademicServiceRequest()) {
                return getAcademicRequestStateDescription(platformServices);
            } else if (isForAcademicTax()) {
                return registrationStateDescription(platformServices);
            } else if (isForExtracurricularTuition()) {
                return registrationStateDescription(platformServices);
            } else if (isForImprovementTax()) {
                return registrationStateDescription(platformServices);
            } else if (isForRegistrationTuition()) {
                return registrationStateDescription(platformServices);
            } else if (isForStandaloneTuition()) {
                return registrationStateDescription(platformServices);
            } else if (isForTreasuryEventTarget()) {
                IAcademicTreasuryTarget target = (IAcademicTreasuryTarget) getTreasuryEventTarget();

                return target.getEventTargetCurrentState();
            } else if (isLegacy()) {
                return new LocalizedString();
            } else if (isCustomAcademicDebt()) {
                return registrationStateDescription(platformServices);
            }
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }

        return new LocalizedString();
    }

    private LocalizedString getAcademicRequestStateDescription(ITreasuryPlatformDependentServices platformServices) {
        ITreasuryServiceRequest request = getITreasuryServiceRequest();
        AcademicServiceRequest serviceRequest = (AcademicServiceRequest) request;

        AcademicServiceRequestSituationType currentSituationType = serviceRequest.getAcademicServiceRequestSituationType();

        if (currentSituationType != null) {
            LocalizedString ls = new LocalizedString();
            for (Locale locale : platformServices.availableLocales()) {
                ls = ls.with(locale, currentSituationType.getLocalizedName(locale));
            }

            return ls;
        }

        return new LocalizedString();
    }

    private LocalizedString registrationStateDescription(ITreasuryPlatformDependentServices platformServices) {
        Registration registration = getRegistration();

        RegistrationStateType activeStateType = registration.getActiveStateType();

        if (activeStateType != null) {
            return activeStateType.getName();
        }

        return new LocalizedString();
    }

    /**
     * You should use the abstraction. API setITreasuryServiceRequest
     */
    @Deprecated
    @Override
    public void setAcademicServiceRequest(final AcademicServiceRequest academicServiceRequest) {
        super.setAcademicServiceRequest(academicServiceRequest);
    }

    public void setITreasuryServiceRequest(final ITreasuryServiceRequest iTreasuryServiceRequest) {
        super.setAcademicServiceRequest((AcademicServiceRequest) iTreasuryServiceRequest);
    }

    /**
     * You should use the abstraction. API getITreasuryServiceRequest
     */
    @Deprecated
    @Override
    public AcademicServiceRequest getAcademicServiceRequest() {
        return super.getAcademicServiceRequest();
    }

    public ITreasuryServiceRequest getITreasuryServiceRequest() {
        return (ITreasuryServiceRequest) super.getAcademicServiceRequest();
    }

    /* --------------------
     * CUSTOMER INFORMATION
     * --------------------
     */

    @Override
    public String getPersonName() {
        if (getPerson() != null) {
            return getPerson().getName();
        }

        return null;
    }

    // @formatter: off
    /* ******************************** *
     * FINANTIAL ENTITY RELATED METHODS *
     * ******************************** */
    // @formatter: on

    @Override
    public FinantialEntity getAssociatedFinantialEntity() {
        if (getDegree() != null) {
            Degree degree = getDegree();
            FinantialEntity finantialEntityOfDegree =
                    AcademicTreasuryConstants.getFinantialEntityOfDegree(degree, getTreasuryEventDate());

            return finantialEntityOfDegree;
        }

        return super.getAssociatedFinantialEntity();
    }

    // @formatter: off
    /* ************************************* *
     * REGISTRATION TUITION SPECIFIC METHODS *
     * ************************************* */
    // @formatter: on

    public BigDecimal getRegistrationTuitionAmountToPayIncludingOtherTuitionRelatedEmolumentsAndExcludingInterests() {
        if (!isForRegistrationTuition()) {
            throw new RuntimeException(
                    "error.AcademicTreasuryEvent.getRegistrationTuitionAmountToPayIncludingOtherTuitionRelatedEmolumentsAndExcludingInterests.invalid.event.type");
        }

        BigDecimal registrationTuitionAmount = getAmountWithVatToPay().subtract(getInterestsAmountToPay());

        Predicate<TreasuryEvent> considerTreasuryEvents = t -> {
            if (!(t instanceof AcademicTreasuryEvent)) {
                return true;
            }

            AcademicTreasuryEvent ate = (AcademicTreasuryEvent) t;
            if (ate.isForRegistrationTuition()) {
                return false;
            }

            // ANIL (2024-11-18) (#qubIT-Fenix-6109)
            //
            // Exclude standalone and extracurricular tuitions
            if (ate.isForStandaloneTuition()) {
                return false;
            }

            if (ate.isForExtracurricularTuition()) {
                return false;
            }

            return true;
        };

        BigDecimal otherTuitionAmount = AcademicTreasuryEvent.getAllAcademicTreasuryEventsOfPerson(getPerson()) //
                .stream() //
                .map(TreasuryEvent.class::cast) //
                .filter(t -> t != this) //
                .filter(considerTreasuryEvents) //
                .filter(t -> t.isEventAccountedAsTuition()) //
                .filter(t -> getExecutionYear().getQualifiedName().equals(t.getExecutionYearName())) //
                .filter(t -> getDegreeCode().equals(t.getDegreeCode())) //
                .map(t -> t.getAmountWithVatToPay().subtract(t.getInterestsAmountToPay())) //
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return registrationTuitionAmount.add(otherTuitionAmount);
    }

    private static Set<Function<Person, Set<IAcademicTreasuryEvent>>> ACADEMIC_TREASURY_READERS = new HashSet<>();

    public static void registerAcademicTreasuryEvent(Function<Person, Set<IAcademicTreasuryEvent>> reader) {
        ACADEMIC_TREASURY_READERS.add(reader);
    }

    public static Set<IAcademicTreasuryEvent> getAllAcademicTreasuryEventsOfPerson(Person person) {
        Set<IAcademicTreasuryEvent> result = new HashSet<>();

        AcademicTreasuryEvent.find(person).collect(Collectors.toCollection(() -> result));

        ACADEMIC_TREASURY_READERS.stream().flatMap(reader -> reader.apply(person).stream())
                .collect(Collectors.toCollection(() -> result));

        return result;
    }
}
