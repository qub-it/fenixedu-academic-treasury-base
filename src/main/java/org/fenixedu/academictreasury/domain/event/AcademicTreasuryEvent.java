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
package org.fenixedu.academictreasury.domain.event;

import static java.lang.String.format;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.treasury.AcademicTreasuryEventPayment;
import org.fenixedu.academic.domain.treasury.IAcademicServiceRequestAndAcademicTaxTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEventPayment;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryTarget;
import org.fenixedu.academic.domain.treasury.IImprovementTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IPaymentReferenceCode;
import org.fenixedu.academic.domain.treasury.ITuitionTreasuryEvent;
import org.fenixedu.academictreasury.domain.emoluments.AcademicTax;
import org.fenixedu.academictreasury.domain.emoluments.ServiceRequestMapEntry;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.serviceRequests.ITreasuryServiceRequest;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.domain.tariff.AcademicTariff;
import org.fenixedu.academictreasury.domain.tuition.TuitionInstallmentTariff;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.services.TuitionServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.tariff.Tariff;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.LocalDate;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.core.AbstractDomainObject;

public class AcademicTreasuryEvent extends AcademicTreasuryEvent_Base implements IAcademicTreasuryEvent, ITuitionTreasuryEvent,
        IImprovementTreasuryEvent, IAcademicServiceRequestAndAcademicTaxTreasuryEvent {

    public AcademicTreasuryEvent() {
        super();
        setCustomAcademicDebt(false);
    }

    protected AcademicTreasuryEvent(final ITreasuryServiceRequest iTreasuryServiceRequest) {
        this();
        init(iTreasuryServiceRequest.getPerson(), iTreasuryServiceRequest,
                ServiceRequestMapEntry.findProduct(iTreasuryServiceRequest));
    }

    protected AcademicTreasuryEvent(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup, final Product product,
            final Registration registration, final ExecutionYear executionYear) {
        this();
        init(tuitionPaymentPlanGroup, product, registration, executionYear);

        checkRules();
    }

    protected AcademicTreasuryEvent(final AcademicTax academicTax, final Registration registration,
            final ExecutionYear executionYear) {
        this();
        init(academicTax, registration, executionYear);

        checkRules();
    }

    protected AcademicTreasuryEvent(final Product product, final IAcademicTreasuryTarget target) {
        this();
        init(product, target);

        checkRules();
    }

    protected AcademicTreasuryEvent(final Product product, final Registration registration, final ExecutionYear executionYear,
            final int customAcademicDebtNumberOfUnits, final int customAcademicDebtNumberOfPages,
            final boolean customAcademicDebtUrgent, final LocalDate customAcademicDebtEventDate) {
        init(product, registration, executionYear, customAcademicDebtNumberOfUnits, customAcademicDebtNumberOfPages,
                customAcademicDebtUrgent, customAcademicDebtEventDate);

        checkRules();
    }

    @Override
    protected void init(final Product product, final LocalizedString name) {
        throw new RuntimeException("wrong call");
    }

    protected void init(final Person person, final ITreasuryServiceRequest iTreasuryServiceRequest, final Product product) {
        super.init(product, nameForAcademicServiceRequest(product, iTreasuryServiceRequest));

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

    protected void init(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup, final Product product,
            final Registration registration, final ExecutionYear executionYear) {
        super.init(product, nameForTuition(product, registration, executionYear));

        setPerson(registration.getPerson());
        setTuitionPaymentPlanGroup(tuitionPaymentPlanGroup);
        setRegistration(registration);
        setExecutionYear(executionYear);
        setPropertiesJsonMap(org.fenixedu.treasury.util.TreasuryConstants.propertiesMapToJson(fillPropertiesMap()));

        checkRules();
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

    protected void init(final AcademicTax academicTax, final Registration registration, final ExecutionYear executionYear) {
        super.init(academicTax.getProduct(), nameForAcademicTax(academicTax, registration, executionYear));

        setAcademicTax(academicTax);
        setPerson(registration.getPerson());
        setRegistration(registration);
        setExecutionYear(executionYear);
        setPropertiesJsonMap(org.fenixedu.treasury.util.TreasuryConstants.propertiesMapToJson(fillPropertiesMap()));

        checkRules();
    }

    public static LocalizedString nameForAcademicTax(final AcademicTax academicTax, final Registration registration,
            final ExecutionYear executionYear) {
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

    protected void init(final Product product, final IAcademicTreasuryTarget target) {
        if (target == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.target.required");
        }

        super.init(product, target.getAcademicTreasuryTargetDescription());

        setPerson(target.getAcademicTreasuryTargetPerson());
        setTreasuryEventTarget((AbstractDomainObject) target);
    }

    protected void init(final Product product, final Registration registration, final ExecutionYear executionYear,
            final int customAcademicDebtNumberOfUnits, final int customAcademicDebtNumberOfPages,
            final boolean customAcademicDebtUrgent, final LocalDate customAcademicDebtEventDate) {
        super.init(product, nameForCustomAcademicDebt(product, registration, executionYear));

        setPerson(registration.getPerson());
        setCustomAcademicDebt(true);
        setRegistration(registration);
        setExecutionYear(executionYear);

        setCustomAcademicDebtNumberOfUnits(customAcademicDebtNumberOfUnits);
        setCustomAcademicDebtNumberOfPages(customAcademicDebtNumberOfPages);
        setCustomAcademicDebtUrgent(customAcademicDebtUrgent);
        setCustomAcademicDebtEventDate(customAcademicDebtEventDate);
    }

    public static LocalizedString nameForCustomAcademicDebt(final Product product, final Registration registration,
            final ExecutionYear executionYear) {
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

        if (!isForAcademicServiceRequest() && !isTuitionEvent() && !isForAcademicTax() && !isForImprovementTax()
                && !isForTreasuryEventTarget() && !isCustomAcademicDebt()) {
            throw new AcademicTreasuryDomainException(
                    "error.AcademicTreasuryEvent.not.for.service.request.nor.tuition.nor.academic.tax");
        }

        if (!(isForAcademicServiceRequest() ^ isForRegistrationTuition() ^ isForStandaloneTuition()
                ^ isForExtracurricularTuition() ^ isForImprovementTax() ^ isForAcademicTax() ^ isForTreasuryEventTarget()
                ^ isCustomAcademicDebt())) {
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
        } else if (isForCustomAcademicDebt()) {
            return isCustomAcademicDebtUrgent();
        }

        throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.urgentRequest.not.applied");
    }

    public LocalDate getRequestDate() {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        if (isForAcademicServiceRequest()) {
            return getITreasuryServiceRequest().getRequestDate().toLocalDate();
        } else if (isForAcademicTax() && !isForImprovementTax()) {
            final RegistrationDataByExecutionYear registrationDataByExecutionYear =
                    academicTreasuryServices.findRegistrationDataByExecutionYear(getRegistration(), getExecutionYear());

            return registrationDataByExecutionYear != null ? registrationDataByExecutionYear.getEnrolmentDate() : new LocalDate();
        } else if (isForImprovementTax()) {
            final RegistrationDataByExecutionYear registrationDataByExecutionYear =
                    academicTreasuryServices.findRegistrationDataByExecutionYear(getRegistration(), getExecutionYear());

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
    @Atomic
    public LocalDate getTreasuryEventDate() {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        if (isForAcademicServiceRequest()) {
            return getITreasuryServiceRequest().getRequestDate().toLocalDate();
        } else if (isForImprovementTax() || isForAcademicTax() || isForRegistrationTuition() || isForExtracurricularTuition()
                || isForStandaloneTuition()) {

            final RegistrationDataByExecutionYear data =
                    academicTreasuryServices.findRegistrationDataByExecutionYear(getRegistration(), getExecutionYear());

            if (data != null && data.getEnrolmentDate() != null) {
                return data.getEnrolmentDate();
            }

            return getExecutionYear().getBeginLocalDate();
        } else if (isForTreasuryEventTarget()) {
            return ((IAcademicTreasuryTarget) getTreasuryEventTarget()).getAcademicTreasuryTargetEventDate();
        }

        throw new RuntimeException("dont know how to handle this!");
    }

    public Optional<? extends DebitEntry> findActiveAcademicServiceRequestDebitEntry() {
        return DebitEntry.findActive(this).findFirst();
    }

    public Optional<? extends DebitEntry> findActiveEnrolmentDebitEntry(final Enrolment enrolment) {
        return DebitEntry.findActive(this).filter(d -> d.getCurricularCourse() == enrolment.getCurricularCourse()
                && d.getExecutionSemester() == enrolment.getExecutionPeriod()).findFirst();
    }

    public Optional<? extends DebitEntry> findActiveEnrolmentEvaluationDebitEntry(final EnrolmentEvaluation enrolmentEvaluation) {
        return DebitEntry.findActive(this)
                .filter(d -> d.getCurricularCourse() == enrolmentEvaluation.getEnrolment().getCurricularCourse()
                        && d.getExecutionSemester() == enrolmentEvaluation.getExecutionPeriod()
                        && d.getEvaluationSeason() == enrolmentEvaluation.getEvaluationSeason())
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
        debitEntry.setExecutionSemester(enrolment.getExecutionPeriod());
    }

    public void associateEnrolmentEvaluation(final DebitEntry debitEntry, final EnrolmentEvaluation enrolmentEvaluation) {
        if (!isForImprovementTax()) {
            throw new RuntimeException("wrong call");
        }

        if (enrolmentEvaluation == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.enrolmentEvaluation.cannot.be.null");
        }

        debitEntry.setCurricularCourse(enrolmentEvaluation.getEnrolment().getCurricularCourse());
        debitEntry.setExecutionSemester(enrolmentEvaluation.getEnrolment().getExecutionPeriod());

        if (enrolmentEvaluation.getExecutionPeriod() != null) {
            debitEntry.setExecutionSemester(enrolmentEvaluation.getExecutionPeriod());
        }

        debitEntry.setEvaluationSeason(enrolmentEvaluation.getEvaluationSeason());
    }

    @Override
    public Set<Product> getPossibleProductsToExempt() {
        if (isForRegistrationTuition()) {
            return TuitionPaymentPlan
                    .find(getTuitionPaymentPlanGroup(),
                            getRegistration().getStudentCurricularPlan(getExecutionYear()).getDegreeCurricularPlan(),
                            getExecutionYear())
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
        if (degree() == null) {
            return null;
        }

        return degree().getCode();
    }

    @Override
    public String getDegreeName() {
        if (degree() == null) {
            return null;
        }

        if (getExecutionYear() != null) {
            return degree().getPresentationName(getExecutionYear());
        }

        return degree().getPresentationName();
    }

    @Override
    public String getExecutionYearName() {
        if (getExecutionYear() != null) {
            return getExecutionYear().getQualifiedName();
        }

        return null;
    }

    private Degree degree() {
        Degree degree = null;

        if (isForRegistrationTuition() && getRegistration() != null) {
            degree = getRegistration().getDegree();
        } else if (isForStandaloneTuition() || isForExtracurricularTuition()) {
        } else if (isForImprovementTax()) {
        } else if (isForAcademicTax() && getRegistration() != null) {
            degree = getRegistration().getDegree();
        } else if (isForAcademicServiceRequest() && getRegistration() != null) {
            degree = getRegistration().getDegree();
        }

        return degree;
    }

    // Category code for treasuryEvent propose
    private int treasuryEventTypeCode() {
        if (isForAcademicServiceRequest()) {
            return 0;
        } else if (isForAcademicTax()) {
            return 1;
        } else if (isForExtracurricularTuition()) {
            return 2;
        } else if (isForImprovementTax()) {
            return 3;
        } else if (isForRegistrationTuition()) {
            return 4;
        } else if (isForStandaloneTuition()) {
            return 5;
        } else if (isForTreasuryEventTarget()) {
            return 6;
        } else if (isLegacy()) {
            return 7;
        } else if (isCustomAcademicDebt()) {
            return 8;
        }

        throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.unkwnown.type");
    }

    public void mergeDebitEntriesAndExemptions(final AcademicTreasuryEvent event) {
        if (this.treasuryEventTypeCode() != event.treasuryEventTypeCode()) {
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

        for (final TreasuryExemption exemption : Sets.newHashSet(event.getTreasuryExemptionsSet())) {
            exemption.setTreasuryEvent(this);
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
        
        if(degree() != null) {
            return Optional.ofNullable(AcademicTariff.findMatch(finantialEntity, product, degree(), when.toDateTimeAtStartOfDay()));
        } else if(getDegree() != null) {
            return Optional.ofNullable(AcademicTariff.findMatch(finantialEntity, product, getDegree(), when.toDateTimeAtStartOfDay()));
        }
        
        return Optional.ofNullable(AcademicTariff.findMatch(finantialEntity, product, finantialEntity.getAdministrativeOffice(), when.toDateTimeAtStartOfDay()));
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
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        return findAll().filter(e -> e.getPerson() == person);
    }
    
    public static Stream<? extends AcademicTreasuryEvent> find(ExecutionYear executionYear) {
        return findAll().filter(e -> e.getExecutionYear() == executionYear);
    }

    public static Stream<? extends AcademicTreasuryEvent> find(Registration registration,
            ExecutionYear executionYear) {
        return findAll().filter(e -> e.getRegistration() == registration).filter(l -> l.getExecutionYear() == executionYear);
    }

    /* --- Academic Service Requests --- */

    public static Stream<? extends AcademicTreasuryEvent> find(final ITreasuryServiceRequest iTreasuryServiceRequest) {
        if (iTreasuryServiceRequest == null) {
            throw new RuntimeException("wrong call");
        }

        return findAll().filter(e -> e.getITreasuryServiceRequest() != null
                && e.getITreasuryServiceRequest().getExternalId().equals(iTreasuryServiceRequest.getExternalId()));
    }

    public static Optional<? extends AcademicTreasuryEvent> findUnique(final ITreasuryServiceRequest iTreasuryServiceRequest) {
        return find(iTreasuryServiceRequest).findFirst();
    }

    public static AcademicTreasuryEvent createForAcademicServiceRequest(final ITreasuryServiceRequest iTreasuryServiceRequest) {
        return new AcademicTreasuryEvent(iTreasuryServiceRequest);
    }

    /* *******
     * TUITION
     * *******
     */

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

    /* ************
     * ACADEMIC TAX
     * ************
     */

    public static Stream<? extends AcademicTreasuryEvent> findAllForAcademicTax(final Registration registration,
            final ExecutionYear executionYear) {
        final Set<AcademicTreasuryEvent> result = Sets.newHashSet();

        for (final AcademicTax academicTax : AcademicTax.findAll().collect(Collectors.toSet())) {
            result.addAll(findForAcademicTax(registration, executionYear, academicTax)
                    .collect(Collectors.<AcademicTreasuryEvent> toSet()));
        }

        return result.stream();
    }

    public static Stream<? extends AcademicTreasuryEvent> findForAcademicTax(final Registration registration,
            final ExecutionYear executionYear, final AcademicTax academicTax) {
        return find(registration.getPerson())
                .filter(e -> e.isForAcademicTax() && e.getAcademicTax() == academicTax && e.getExecutionYear() == executionYear
                        && (!e.getAcademicTax().isAppliedOnRegistration() && e.getPerson() == registration.getPerson()
                                || e.getRegistration() == registration));
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
        return find(person).filter(t -> (!trimmed && t.getDescription().getContent().equals(description))
                || (trimmed && t.getDescription().getContent().trim().equals(description)));
    }

    public static AcademicTreasuryEvent createForAcademicTax(final AcademicTax academicTax, final Registration registration,
            final ExecutionYear executionYear) {
        return new AcademicTreasuryEvent(academicTax, registration, executionYear);
    }

    public static AcademicTreasuryEvent createForAcademicTreasuryEventTarget(final Product product,
            final IAcademicTreasuryTarget target) {
        return new AcademicTreasuryEvent(product, target);
    }

    public static AcademicTreasuryEvent createForCustomAcademicDebt(final Product product, final Registration registration,
            final ExecutionYear executionYear, final int customAcademicDebtNumberOfUnits,
            final int customAcademicDebtNumberOfPages, final boolean customAcademicDebtUrgent,
            final LocalDate customAcademicDebtEventDate) {
        return new AcademicTreasuryEvent(product, registration, executionYear, customAcademicDebtNumberOfUnits,
                customAcademicDebtNumberOfPages, customAcademicDebtUrgent, customAcademicDebtEventDate);
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
            return AcademicTreasuryConstants
                    .academicTreasuryBundleI18N("label." + AcademicTreasuryEventKeys.class.getSimpleName() + "." + name());
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

            propertiesMap.put(AcademicTreasuryEventKeys.DEGREE.getDescriptionI18N().getContent(), getITreasuryServiceRequest()
                    .getRegistration().getDegree().getPresentationName(getITreasuryServiceRequest().getExecutionYear()));

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
    public void invokeSettlementCallbacks() {
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

    /* -------------------
     * TUITION INFORMATION
     * -------------------
     */

    @Override
    public int getTuitionInstallmentSize() {
        if (!isForRegistrationTuition()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.only.tuition.for.registration.supported");
        }

        return orderedTuitionDebitEntriesList().size();
    }

    @Override
    public BigDecimal getTuitionInstallmentAmountToPay(final int installmentOrder) {
        if (!isForRegistrationTuition()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.only.tuition.for.registration.supported");
        }

        return orderedTuitionDebitEntriesList().get(installmentOrder).getOpenAmount();
    }

    @Override
    public BigDecimal getTuitionInstallmentRemainingAmountToPay(final int installmentOrder) {
        if (!isForRegistrationTuition()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.only.tuition.for.registration.supported");
        }

        return orderedTuitionDebitEntriesList().get(installmentOrder).getOpenAmount();
    }

    @Override
    public BigDecimal getTuitionInstallmentExemptedAmount(final int installmentOrder) {
        if (!isForRegistrationTuition()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.only.tuition.for.registration.supported");
        }

        final DebitEntry debitEntry = orderedTuitionDebitEntriesList().get(installmentOrder);

        BigDecimal result = debitEntry.getExemptedAmount();
        result = result.add(debitEntry.getCreditEntriesSet().stream().filter(l -> l.isFromExemption())
                .map(l -> l.getAmountWithVat()).reduce((a, b) -> a.add(b)).orElse(BigDecimal.ZERO));

        return result;
    }

    @Override
    public LocalDate getTuitionInstallmentDueDate(final int installmentOrder) {
        if (!isForRegistrationTuition()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.only.tuition.for.registration.supported");
        }

        final DebitEntry debitEntry = orderedTuitionDebitEntriesList().get(installmentOrder);
        return debitEntry.getDueDate();
    }

    @Override
    public String getTuitionInstallmentDescription(final int installmentOrder) {
        if (!isForRegistrationTuition()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.only.tuition.for.registration.supported");
        }

        final DebitEntry debitEntry = orderedTuitionDebitEntriesList().get(installmentOrder);
        return debitEntry.getDescription();
    }

    @Override
    public boolean isTuitionInstallmentExempted(final int installmentOrder) {
        if (!isForRegistrationTuition()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.only.tuition.for.registration.supported");
        }

        final DebitEntry debitEntry = orderedTuitionDebitEntriesList().get(installmentOrder);
        return TreasuryExemption.findUnique(this, debitEntry.getProduct()).isPresent();
    }

    @Override
    public String getTuitionInstallmentExemptionReason(final int installmentOrder) {
        if (!isForRegistrationTuition()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.only.tuition.for.registration.supported");
        }

        final DebitEntry debitEntry = orderedTuitionDebitEntriesList().get(installmentOrder);
        if (!TreasuryExemption.findUnique(this, debitEntry.getProduct()).isPresent()) {
            return null;
        }

        return TreasuryExemption.findUnique(this, debitEntry.getProduct()).get().getReason();
    }

    @Override
    public List<IAcademicTreasuryEventPayment> getTuitionInstallmentPaymentsList(final int installmentOrder) {
        if (!isForRegistrationTuition()) {
            throw new AcademicTreasuryDomainException("error.AcademicTreasuryEvent.only.tuition.for.registration.supported");
        }

        final DebitEntry debitEntry = orderedTuitionDebitEntriesList().get(installmentOrder);

        return debitEntry.getSettlementEntriesSet().stream().map(l -> new AcademicTreasuryEventPayment(l))
                .collect(Collectors.toList());
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
    public BigDecimal getAmountToPay(final EnrolmentEvaluation enrolmentEvaluation) {
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
    public BigDecimal getExemptedAmount(final EnrolmentEvaluation enrolmentEvaluation) {
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
        return "{\"" + AcademicTreasuryEventKeys.DEGREE_CODE + "\":\"" + degreeCode + "\",\""
                + AcademicTreasuryEventKeys.EXECUTION_YEAR + "\":\"" + executionYear + "\"}";
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

}
