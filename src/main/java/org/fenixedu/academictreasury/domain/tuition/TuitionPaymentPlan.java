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
package org.fenixedu.academictreasury.domain.tuition;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.dto.tariff.AcademicTariffBean;
import org.fenixedu.academictreasury.dto.tariff.TuitionPaymentPlanBean;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
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

    public static final Comparator<TuitionPaymentPlan> COMPARE_BY_PAYMENT_PLAN_ORDER = (o1, o2) -> {
        int c = Integer.compare(o1.getPaymentPlanOrder(), o2.getPaymentPlanOrder());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static final Comparator<TuitionPaymentPlan> COMPARE_BY_DCP_AND_PAYMENT_PLAN_ORDER = (o1, o2) -> {
        int c = DegreeCurricularPlan.COMPARATOR_BY_PRESENTATION_NAME.compare(o1.getDegreeCurricularPlan(),
                o2.getDegreeCurricularPlan());

        if (c != 0) {
            return c;
        }

        c = Integer.compare(o1.getPaymentPlanOrder(), o2.getPaymentPlanOrder());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    private static final int MAX_PAYMENT_PLANS_LIMIT = 50;

    public TuitionPaymentPlan() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected TuitionPaymentPlan(final DegreeCurricularPlan degreeCurricularPlan,
            final TuitionPaymentPlanBean tuitionPaymentPlanBean) {
        this();

        setFinantialEntity(tuitionPaymentPlanBean.getFinantialEntity());
        setTuitionPaymentPlanGroup(tuitionPaymentPlanBean.getTuitionPaymentPlanGroup());
        setProduct(tuitionPaymentPlanBean.getTuitionPaymentPlanGroup().getCurrentProduct());
        setExecutionYear(tuitionPaymentPlanBean.getExecutionYear());
        setDegreeCurricularPlan(degreeCurricularPlan);

        setDefaultPaymentPlan(tuitionPaymentPlanBean.isDefaultPaymentPlan());
        setRegistrationRegimeType(tuitionPaymentPlanBean.getRegistrationRegimeType());
        setRegistrationProtocol(tuitionPaymentPlanBean.getRegistrationProtocol());
        setIngression(tuitionPaymentPlanBean.getIngression());
        setCurricularYear(tuitionPaymentPlanBean.getCurricularYear());
        setStatuteType(tuitionPaymentPlanBean.getStatuteType());
        setPayorDebtAccount(tuitionPaymentPlanBean.getPayorDebtAccount());

        // TODO check how to reference semester
//        setSemester(tuitionPaymentPlanBean.getExecutionSemester() != null ? tuitionPaymentPlanBean.getExecutionSemester()
//                .getChildOrder() : null);

        setFirstTimeStudent(tuitionPaymentPlanBean.isFirstTimeStudent());
        setCustomized(tuitionPaymentPlanBean.isCustomized());

        LocalizedString mls = new LocalizedString();
        for (final Locale locale : TreasuryPlataformDependentServicesFactory.implementation().availableLocales()) {
            mls = mls.with(locale, tuitionPaymentPlanBean.getName());
        }

        setCustomizedName(mls);

        setWithLaboratorialClasses(tuitionPaymentPlanBean.isWithLaboratorialClasses());
        setPaymentPlanOrder(find(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear())
                .max(COMPARE_BY_PAYMENT_PLAN_ORDER).map(TuitionPaymentPlan::getPaymentPlanOrder).orElse(0) + 1);

        createInstallments(tuitionPaymentPlanBean);

        checkRules();

        setOnReachablePosition();
    }

    protected TuitionPaymentPlan(final TuitionPaymentPlan tuitionPaymentPlanToCopy, final ExecutionYear toExecutionYear) {
        this();

        setFinantialEntity(tuitionPaymentPlanToCopy.getFinantialEntity());
        setTuitionPaymentPlanGroup(tuitionPaymentPlanToCopy.getTuitionPaymentPlanGroup());
        setProduct(tuitionPaymentPlanToCopy.getTuitionPaymentPlanGroup().getCurrentProduct());
        setExecutionYear(toExecutionYear);
        setDegreeCurricularPlan(tuitionPaymentPlanToCopy.getDegreeCurricularPlan());

        setDefaultPaymentPlan(tuitionPaymentPlanToCopy.isDefaultPaymentPlan());
        setRegistrationRegimeType(tuitionPaymentPlanToCopy.getRegistrationRegimeType());
        setRegistrationProtocol(tuitionPaymentPlanToCopy.getRegistrationProtocol());
        setIngression(tuitionPaymentPlanToCopy.getIngression());
        setCurricularYear(tuitionPaymentPlanToCopy.getCurricularYear());
        setStatuteType(tuitionPaymentPlanToCopy.getStatuteType());
        setPayorDebtAccount(tuitionPaymentPlanToCopy.getPayorDebtAccount());
        setSemester(tuitionPaymentPlanToCopy.getSemester());
        setFirstTimeStudent(tuitionPaymentPlanToCopy.isFirstTimeStudent());
        setCustomized(tuitionPaymentPlanToCopy.isCustomized());

        setCustomizedName(tuitionPaymentPlanToCopy.getCustomizedName());

        setWithLaboratorialClasses(tuitionPaymentPlanToCopy.isWithLaboratorialClasses());
        setPaymentPlanOrder(find(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear())
                .max(COMPARE_BY_PAYMENT_PLAN_ORDER).map(TuitionPaymentPlan::getPaymentPlanOrder).orElse(0) + 1);

        createInstallments(tuitionPaymentPlanToCopy);

        setCopyFromTuitionPaymentPlan(tuitionPaymentPlanToCopy);

        checkRules();
    }

    private void checkRules() {
        if (getTuitionPaymentPlanGroup() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.tuitionPaymentPlanGroup.required");
        }

        if (getFinantialEntity() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.finantialEntity.required");
        }

        if (getExecutionYear() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.executionYear.required");
        }

        if (getDegreeCurricularPlan() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.degreeCurricularPlan.required");
        }

        if (isCustomized() && LocalizedStringUtil.isTrimmedEmpty(getName())) {
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

        if (findDefaultPaymentPlans(getDegreeCurricularPlan(), getExecutionYear()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.defaultPaymentPlan.not.unique",
                    getDegreeCurricularPlan().getPresentationName());
        }

        if (find(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear(), getPaymentPlanOrder())
                .count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.paymentPlan.with.order.already.exists");
        }

        if (getTuitionInstallmentTariffsSet().isEmpty()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.installments.must.not.be.empty");
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

    private boolean hasStudentSpecificConditionSelected() {
        return getRegistrationRegimeType() != null || isDefaultPaymentPlan() || getRegistrationProtocol() != null
                || getIngression() != null || getCurricularYear() != null || getSemester() != null || isFirstTimeStudent()
                || getStatuteType() != null;
    }

    private boolean hasAtLeastOneConditionSpecified() {
        boolean hasAtLeastOneCondition = false;

        hasAtLeastOneCondition |= getDefaultPaymentPlan();
        hasAtLeastOneCondition |= getRegistrationRegimeType() != null;
        hasAtLeastOneCondition |= getRegistrationProtocol() != null;
        hasAtLeastOneCondition |= getIngression() != null;
        hasAtLeastOneCondition |= getCurricularYear() != null;
        hasAtLeastOneCondition |= getStatuteType() != null;
        hasAtLeastOneCondition |= getSemester() != null;
        hasAtLeastOneCondition |= isFirstTimeStudent();
        hasAtLeastOneCondition |= isCustomized();
        hasAtLeastOneCondition |= isWithLaboratorialClasses();

        return hasAtLeastOneCondition;
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

    public LocalizedString getName() {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        LocalizedString result = new LocalizedString();

        for (final Locale locale : treasuryServices.availableLocales()) {
            final String paymentPlanLabel =
                    isCustomized() ? "label.TuitionPaymentPlan.paymentPlanName.customized" : "label.TuitionPaymentPlan.paymentPlanName";

            result = result.with(locale,
                    AcademicTreasuryConstants.academicTreasuryBundle(locale, paymentPlanLabel,
                            "[" + getDegreeCurricularPlan().getDegree().getCode() + "] "
                                    + getDegreeCurricularPlan().getDegree().getPresentationName() + " - "
                                    + getDegreeCurricularPlan().getName(),
                            getExecutionYear().getQualifiedName(),
                            isCustomized() ? getCustomizedName().getContent(locale) : null));
        }

        return result;
    }

    public LocalizedString getConditionsDescription() {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        LocalizedString result = new LocalizedString();

        for (final Locale locale : treasuryServices.availableLocales()) {
            StringBuilder description = new StringBuilder();

            if (isDefaultPaymentPlan()) {
                description.append(academicTreasuryBundle("label.TuitionPaymentPlan.defaultPaymentPlan"))
                        .append(CONDITIONS_DESCRIPTION_SEPARATOR);
            }

            if (getTuitionPaymentPlanGroup().isForStandalone()) {
                description.append(academicTreasuryBundle(locale, "label.TuitionPaymentPlan.standalone"))
                        .append(CONDITIONS_DESCRIPTION_SEPARATOR);

            } else if (getTuitionPaymentPlanGroup().isForExtracurricular()) {
                description.append(academicTreasuryBundle(locale, "label.TuitionPaymentPlan.extracurricular"))
                        .append(CONDITIONS_DESCRIPTION_SEPARATOR);
            }

            if (getRegistrationRegimeType() != null) {
                description.append(getRegistrationRegimeType().getLocalizedName()).append(CONDITIONS_DESCRIPTION_SEPARATOR);
            }

            if (getRegistrationProtocol() != null) {
                description.append(getRegistrationProtocol().getDescription().getContent(locale))
                        .append(CONDITIONS_DESCRIPTION_SEPARATOR);
            }

            if (getIngression() != null) {
                description.append(getIngression().getLocalizedName()).append(CONDITIONS_DESCRIPTION_SEPARATOR);
            }

            if (getCurricularYear() != null) {
                description.append(academicTreasuryBundle(locale, "label.TuitionPaymentPlan.curricularYear.description",
                        String.valueOf(getCurricularYear().getYear()))).append(CONDITIONS_DESCRIPTION_SEPARATOR);
            }

            if (getSemester() != null) {
                description.append(academicTreasuryBundle(locale, "label.TuitionPaymentPlan.curricularSemester.description",
                        String.valueOf(getSemester()))).append(CONDITIONS_DESCRIPTION_SEPARATOR);
            }

            if (getStatuteType() != null) {
                description.append(academicTreasuryServices.localizedNameOfStatuteType(getStatuteType()))
                        .append(CONDITIONS_DESCRIPTION_SEPARATOR);
            }

            if (isFirstTimeStudent()) {
                description.append(academicTreasuryBundle(locale, "label.TuitionPaymentPlan.firstTimeStudent"))
                        .append(CONDITIONS_DESCRIPTION_SEPARATOR);
            }

            if (isCustomized()) {
                description.append(academicTreasuryBundle(locale, "label.TuitionPaymentPlan.customized")).append(" [")
                        .append(getCustomizedName().getContent()).append("]").append(CONDITIONS_DESCRIPTION_SEPARATOR);
            }

            if (isWithLaboratorialClasses()) {
                description.append(academicTreasuryBundle(locale, "label.TuitionPaymentPlan.withLaboratorialClasses"))
                        .append(CONDITIONS_DESCRIPTION_SEPARATOR);
            }

            if (description.toString().contains(CONDITIONS_DESCRIPTION_SEPARATOR)) {
                description.delete(description.length() - CONDITIONS_DESCRIPTION_SEPARATOR.length(), description.length());
            }

            result = result.with(locale, description.toString());
        }

        return result;
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

    public LocalizedString installmentName(final TuitionInstallmentTariff installmentTariff) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();
        String label = "label.TuitionInstallmentTariff.debitEntry.name.";

        if (getTuitionPaymentPlanGroup().isForRegistration()) {
            if(getTuitionInstallmentTariffsSet().size() == 1) {
                label += "registration.one.installment";
            } else {
                label += "registration";
            }
        } else if (getTuitionPaymentPlanGroup().isForStandalone()) {
            label += "standalone";
        } else if (getTuitionPaymentPlanGroup().isForExtracurricular()) {
            label += "extracurricular";
        }

        LocalizedString result = new LocalizedString();
        for (final Locale locale : treasuryServices.availableLocales()) {
            final String installmentName =
                    academicTreasuryBundle(locale, label, String.valueOf(installmentTariff.getInstallmentOrder()),
                            getDegreeCurricularPlan().getDegree().getPresentationName(getExecutionYear(), locale),
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

    public boolean isFirstTimeStudent() {
        return getFirstTimeStudent();
    }

    public boolean isFirst() {
        return findSortedByPaymentPlanOrder(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear())
                .collect(Collectors.toList()).get(0) == this;
    }

    public boolean isLast() {
        final List<TuitionPaymentPlan> list =
                findSortedByPaymentPlanOrder(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear())
                        .collect(Collectors.toList());

        return list.get(list.size() - 1) == this;
    }

    public boolean isWithLaboratorialClasses() {
        return super.getWithLaboratorialClasses();
    }

    @Atomic
    public void orderUp() {
        if (isFirst()) {
            return;
        }

        final TuitionPaymentPlan previous = getPreviousTuitionPaymentPlan();

        int order = previous.getPaymentPlanOrder();
        previous.setPaymentPlanOrder(getPaymentPlanOrder());
        setPaymentPlanOrder(order);
    }

    @Atomic
    public void orderDown() {
        if (isLast()) {
            return;
        }

        final TuitionPaymentPlan next = getNextTuitionPaymentPlan();

        int order = getPaymentPlanOrder() + 1;
        next.setPaymentPlanOrder(getPaymentPlanOrder());
        setPaymentPlanOrder(order);
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
            final Set<TuitionPaymentPlan> allPlans = Sets.newHashSet();
            allPlans.addAll(TuitionPaymentPlan.find(TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get(),
                    getDegreeCurricularPlan(), getExecutionYear()).collect(Collectors.toSet()));
            allPlans.addAll(TuitionPaymentPlan.find(TuitionPaymentPlanGroup.findUniqueDefaultGroupForStandalone().get(),
                    getDegreeCurricularPlan(), getExecutionYear()).collect(Collectors.toSet()));
            allPlans.addAll(TuitionPaymentPlan.find(TuitionPaymentPlanGroup.findUniqueDefaultGroupForExtracurricular().get(),
                    getDegreeCurricularPlan(), getExecutionYear()).collect(Collectors.toSet()));

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

        super.setTuitionPaymentPlanGroup(null);
        super.setExecutionYear(null);
        super.setDegreeCurricularPlan(null);
        super.setRegistrationProtocol(null);
        super.setProduct(null);
        this.setCurricularYear(null);
        this.setFinantialEntity(null);
        this.setIngression(null);
        this.setStatuteType(null);
        this.setPayorDebtAccount(null);

        setCopyFromTuitionPaymentPlan(null);
        while (!getTuitionPaymentPlanCopiesSet().isEmpty()) {
            getTuitionPaymentPlanCopiesSet().iterator().next().setCopyFromTuitionPaymentPlan(null);
        }

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

    public static Stream<TuitionPaymentPlan> find(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            final DegreeCurricularPlan degreeCurricularPlan, final ExecutionYear executionYear) {

        return find(tuitionPaymentPlanGroup)
                .filter(t -> t.getExecutionYear() == executionYear && t.getDegreeCurricularPlan() == degreeCurricularPlan);
    }

    public static Stream<TuitionPaymentPlan> findSortedByPaymentPlanOrder(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            final DegreeCurricularPlan degreeCurricularPlan, final ExecutionYear executionYear) {
        return find(tuitionPaymentPlanGroup, degreeCurricularPlan, executionYear).sorted(COMPARE_BY_PAYMENT_PLAN_ORDER)
                .collect(Collectors.toList()).stream();
    }

    protected static Stream<TuitionPaymentPlan> find(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            final DegreeCurricularPlan degreeCurricularPlan, final ExecutionYear executionYear, final int paymentPlanOrder) {
        return find(tuitionPaymentPlanGroup, degreeCurricularPlan, executionYear)
                .filter(t -> t.getPaymentPlanOrder() == paymentPlanOrder);
    }

    protected static Optional<TuitionPaymentPlan> findUnique(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            final DegreeCurricularPlan degreeCurricularPlan, final ExecutionYear executionYear, final int paymentPlanOrder) {
        return find(tuitionPaymentPlanGroup, degreeCurricularPlan, executionYear, paymentPlanOrder).findFirst();
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
            final ExecutionYear executionYear, final InferTuitionStudentConditionsBean conditionsBean) {

        final List<TuitionPaymentPlan> plans = TuitionPaymentPlan
                .findSortedByPaymentPlanOrder(TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get(),
                        degreeCurricularPlan, executionYear)
                .collect(Collectors.toList());

        final List<TuitionPaymentPlan> filtered = Lists.newArrayList();
        for (final TuitionPaymentPlan t : plans) {

            if (t.getRegistrationRegimeType() != null && t.getRegistrationRegimeType() != conditionsBean.getRegimeType()) {
                continue;
            }

            if (t.getRegistrationProtocol() != null && t.getRegistrationProtocol() != conditionsBean.getRegistrationProtocol()) {
                continue;
            }

            if (t.getIngression() != null && t.getIngression() != conditionsBean.getIngression()) {
                continue;
            }

            if (t.getSemester() != null) {
                if (conditionsBean.getSemestersWithEnrolments().size() != 1) {
                    continue;
                }

                final Integer semester = conditionsBean.getSemestersWithEnrolments().iterator().next();

                if (!t.getSemester().equals(semester)) {
                    continue;
                }
            }

            if (t.getCurricularYear() != null && t.getCurricularYear() != conditionsBean.getCurricularYear()) {
                continue;
            }

            if (t.getStatuteType() != null && !conditionsBean.getStatutes().contains(t.getStatuteType())) {
                continue;
            }

            if (t.getFirstTimeStudent() && !conditionsBean.isFirstTimeStudent()) {
                continue;
            }

            if (t.isCustomized()) {
                continue;
            }

            filtered.add(t);
        }

        return !filtered.isEmpty() ? filtered.get(0) : null;
    }

    public static TuitionPaymentPlan inferTuitionPaymentPlanForRegistration(final Registration registration,
            final ExecutionYear executionYear, final InferTuitionStudentConditionsBean conditionsBean) {
        final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);

        if (studentCurricularPlan == null) {
            return null;
        }

        final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();

        return inferTuitionPaymentPlanForRegistration(degreeCurricularPlan, executionYear, conditionsBean);
    }

    public static TuitionPaymentPlan inferTuitionPaymentPlanForRegistration(final Registration registration,
            final ExecutionYear executionYear) {

        final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);

        if (studentCurricularPlan == null) {
            return null;
        }

        final InferTuitionStudentConditionsBean conditionsBean =
                InferTuitionStudentConditionsBean.build(registration, executionYear);

        return inferTuitionPaymentPlanForRegistration(registration, executionYear, conditionsBean);
    }

    public static TuitionPaymentPlan inferTuitionPaymentPlanForStandaloneEnrolment(final Registration registration,
            final ExecutionYear executionYear, final Enrolment enrolment) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        if (!enrolment.isStandalone()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.enrolment.is.not.standalone");
        }

        final DegreeCurricularPlan degreeCurricularPlan = enrolment.getCurricularCourse().getDegreeCurricularPlan();
        final RegistrationProtocol registrationProtocol = academicTreasuryServices.registrationProtocol(registration);
        final IngressionType ingression = academicTreasuryServices.ingression(registration);
        final boolean laboratorial = laboratorial(enrolment);
        final Set<StatuteType> statutes =
                academicTreasuryServices.statutesTypesValidOnAnyExecutionSemesterFor(registration, executionYear);

        final Stream<TuitionPaymentPlan> stream = TuitionPaymentPlan.findSortedByPaymentPlanOrder(
                TuitionPaymentPlanGroup.findUniqueDefaultGroupForStandalone().get(), degreeCurricularPlan, executionYear);

        final List<TuitionPaymentPlan> l = stream.collect(Collectors.toList());

        List<TuitionPaymentPlan> plans = Lists.newArrayList(l).stream()
                .filter(t -> (t.getRegistrationProtocol() == null || t.getRegistrationProtocol() == registrationProtocol)
                        && (t.getIngression() == null || t.getIngression() == ingression)
                        && (!t.isWithLaboratorialClasses() || t.isWithLaboratorialClasses() == laboratorial) && !t.isCustomized())
                .collect(Collectors.toList());

        final List<TuitionPaymentPlan> filtered = Lists.newArrayList();
        for (final TuitionPaymentPlan t : plans) {
            if (t.getStatuteType() != null && !statutes.contains(t.getStatuteType())) {
                continue;
            }

            filtered.add(t);
        }

        return filtered.stream().findFirst().orElse(null);
    }

    public static TuitionPaymentPlan inferTuitionPaymentPlanForExtracurricularEnrolment(final Registration registration,
            final ExecutionYear executionYear, final Enrolment enrolment) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        if (!enrolment.isExtraCurricular()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.enrolment.is.not.extracurricular");
        }

        final DegreeCurricularPlan degreeCurricularPlan = enrolment.getCurricularCourse().getDegreeCurricularPlan();
        final RegistrationProtocol registrationProtocol = academicTreasuryServices.registrationProtocol(registration);
        final IngressionType ingression = academicTreasuryServices.ingression(registration);
        final boolean laboratorial = laboratorial(enrolment);
        final Set<StatuteType> statutes =
                academicTreasuryServices.statutesTypesValidOnAnyExecutionSemesterFor(registration, executionYear);

        final Stream<TuitionPaymentPlan> stream = TuitionPaymentPlan.findSortedByPaymentPlanOrder(
                TuitionPaymentPlanGroup.findUniqueDefaultGroupForExtracurricular().get(), degreeCurricularPlan, executionYear);

        final List<TuitionPaymentPlan> l = stream.collect(Collectors.toList());

        List<TuitionPaymentPlan> plans = Lists.newArrayList(l).stream()
                .filter(t -> (t.getRegistrationProtocol() == null || t.getRegistrationProtocol() == registrationProtocol)
                        && (t.getIngression() == null || t.getIngression() == ingression)
                        && (!t.isWithLaboratorialClasses() || t.isWithLaboratorialClasses() == laboratorial) && !t.isCustomized())
                .collect(Collectors.toList());

        final List<TuitionPaymentPlan> filtered = Lists.newArrayList();
        for (final TuitionPaymentPlan t : plans) {
            if (t.getStatuteType() != null && !statutes.contains(t.getStatuteType())) {
                continue;
            }

            filtered.add(t);
        }

        return filtered.stream().findFirst().orElse(null);
    }

    private static boolean laboratorial(final Enrolment enrolment) {
        return AcademicTreasuryConstants.isPositive(new BigDecimal(
                enrolment.getCurricularCourse().getCompetenceCourse().getLaboratorialHours(enrolment.getExecutionPeriod())));
    }

    public static boolean firstTimeStudent(final Registration registration, final ExecutionYear executionYear) {
        return registration.isFirstTime(executionYear);
    }

    public static Integer curricularYear(final Registration registration, final ExecutionYear executionYear) {
        return registration.getCurricularYear(executionYear);
    }

    public static Set<Integer> semestersWithEnrolments(final Registration registration, final ExecutionYear executionYear) {
        // TODO: Check how in ISCTE childOrder is obtained
        return Collections.emptySet();
//        return registration.getEnrolments(executionYear).stream().map(e -> (Integer) e.getExecutionInterval().getChildOrder())
//                .collect(Collectors.toSet());
    }

    private TuitionPaymentPlan getPreviousTuitionPaymentPlan() {
        for (int i = getPaymentPlanOrder() - 1; i >= 0; i--) {
            if (findUnique(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear(), i).isPresent()) {
                return findUnique(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear(), i).get();
            }
        }

        return null;
    }

    private TuitionPaymentPlan getNextTuitionPaymentPlan() {
        for (int i = getPaymentPlanOrder() + 1; i <= MAX_PAYMENT_PLANS_LIMIT; i++) {
            if (findUnique(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear(), i).isPresent()) {
                return findUnique(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear(), i).get();
            }
        }

        return null;
    }

    @Atomic
    public static Set<TuitionPaymentPlan> create(final TuitionPaymentPlanBean tuitionPaymentPlanBean) {
        final Set<TuitionPaymentPlan> result = Sets.newHashSet();

        for (final DegreeCurricularPlan degreeCurricularPlan : tuitionPaymentPlanBean.getDegreeCurricularPlans()) {
            result.add(new TuitionPaymentPlan(degreeCurricularPlan, tuitionPaymentPlanBean));
        }

        return result;
    }

    public static TuitionPaymentPlan copy(final TuitionPaymentPlan tuitionPaymentPlanToCopy,
            final ExecutionYear toExecutionYear) {
        return new TuitionPaymentPlan(tuitionPaymentPlanToCopy, toExecutionYear);
    }

    public boolean isReacheble() {
        SortedSet<TuitionPaymentPlan> tuitionPaymentPlans = new TreeSet<>(TuitionPaymentPlan.COMPARE_BY_PAYMENT_PLAN_ORDER);
        tuitionPaymentPlans.addAll(TuitionPaymentPlan
                .findSortedByPaymentPlanOrder(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear())
                .collect(Collectors.toSet()));
        for (TuitionPaymentPlan tuitionPlan : tuitionPaymentPlans) {
            if (this == tuitionPlan) {
                return true;
            }
            if (this.compareOrderWithTuitionPlan(tuitionPlan) < 0) {
                return false;
            }
        }
        return true;
    }

    private int compareOrderWithTuitionPlan(TuitionPaymentPlan plan) {
        if (isCustomized() || plan.isCustomized()) {
            return 0;
        }
        if (isDefaultPaymentPlan()) {
            return 1;
        }
        if (plan.isDefaultPaymentPlan()) {
            return -1;
        }
        if (equalsTuitionPlanConditions(plan)) {
            return -1;
        }
        if (containsTuitionPlanConditions(plan)) {
            return -1;
        }
        if (plan.containsTuitionPlanConditions(this)) {
            return 1;
        }
        return 0;
    }

    private boolean equalsTuitionPlanConditions(TuitionPaymentPlan plan) {
        return Objects.equals(getRegistrationRegimeType(), plan.getRegistrationRegimeType())
                && Objects.equals(getIngression(), plan.getIngression())
                && Objects.equals(getRegistrationProtocol(), plan.getRegistrationProtocol())
                && Objects.equals(getCurricularYear(), plan.getCurricularYear())
                && Objects.equals(getSemester(), plan.getSemester()) && Objects.equals(getStatuteType(), plan.getStatuteType())
                && (getFirstTimeStudent() == plan.getFirstTimeStudent());
    }

    private boolean containsTuitionPlanConditions(TuitionPaymentPlan plan) {
        if (plan.getRegistrationRegimeType() != null && !plan.getRegistrationRegimeType().equals(getRegistrationRegimeType())) {
            return false;
        }
        if (plan.getIngression() != null && !plan.getIngression().equals(getIngression())) {
            return false;
        }
        if (plan.getRegistrationProtocol() != null && !plan.getRegistrationProtocol().equals(getRegistrationProtocol())) {
            return false;
        }
        if (plan.getCurricularYear() != null && !plan.getCurricularYear().equals(getCurricularYear())) {
            return false;
        }
        if (plan.getSemester() != null && !plan.getSemester().equals(getSemester())) {
            return false;
        }
        if (plan.getStatuteType() != null && !plan.getStatuteType().equals(getStatuteType())) {
            return false;
        }
        if (plan.getFirstTimeStudent() && getFirstTimeStudent() != plan.getFirstTimeStudent()) {
            return false;
        }
        return true;
    }

    private void setOnReachablePosition() {
        while (!isReacheble()) {
            orderUp();
        }
    }

}
