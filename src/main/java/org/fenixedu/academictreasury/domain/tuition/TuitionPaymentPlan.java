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
package org.fenixedu.academictreasury.domain.tuition;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.domain.tuition.calculators.TuitionPaymentPlanCalculator;
import org.fenixedu.academictreasury.dto.tariff.AcademicTariffBean;
import org.fenixedu.academictreasury.dto.tariff.TuitionPaymentPlanBean;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.academictreasury.util.LocalizedStringUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class TuitionPaymentPlan extends TuitionPaymentPlan_Base {

    private static final String CONDITIONS_DESCRIPTION_SEPARATOR = ", ";

    public TuitionPaymentPlan() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected TuitionPaymentPlan(final TuitionPaymentPlan tuitionPaymentPlanToCopy, final ExecutionYear toExecutionYear) {
        this();

        setFinantialEntity(tuitionPaymentPlanToCopy.getFinantialEntity());
        setTuitionPaymentPlanGroup(tuitionPaymentPlanToCopy.getTuitionPaymentPlanGroup());
        setProduct(tuitionPaymentPlanToCopy.getTuitionPaymentPlanGroup().getCurrentProduct());
        setExecutionYear(toExecutionYear);

        setDefaultPaymentPlan(tuitionPaymentPlanToCopy.isDefaultPaymentPlan());
        setCustomized(tuitionPaymentPlanToCopy.isCustomized());

        setCustomizedName(tuitionPaymentPlanToCopy.getCustomizedName());

        Map<TuitionPaymentPlanCalculator, TuitionPaymentPlanCalculator> calculatorsCopyMap = new HashMap<>();
        getTuitionPaymentPlanCalculatorSet().forEach(c -> calculatorsCopyMap.put(c, c.copyTo(this)));

        createInstallments(tuitionPaymentPlanToCopy, calculatorsCopyMap);

        setCopyFromTuitionPaymentPlan(tuitionPaymentPlanToCopy);

        tuitionPaymentPlanToCopy.getTuitionConditionRulesSet().forEach(cond -> cond.copyToPlan(this));

        tuitionPaymentPlanToCopy.getTuitionPaymentPlanOrdersSet().forEach(order -> order.copyToPlan(this));

        getTuitionPaymentPlanRecalculationsSet().forEach(
                r -> TuitionPaymentPlanRecalculation.create(r.getProduct(), r.getRecalculationDueDate())
                        .setTuitionPaymentPlan(this));

        checkRules();
    }

    public TuitionPaymentPlan(TuitionPaymentPlanBean tuitionPaymentPlanBean) {
        this();

        setFinantialEntity(tuitionPaymentPlanBean.getFinantialEntity());
        setTuitionPaymentPlanGroup(tuitionPaymentPlanBean.getTuitionPaymentPlanGroup());
        setProduct(tuitionPaymentPlanBean.getTuitionPaymentPlanGroup().getCurrentProduct());
        setExecutionYear(tuitionPaymentPlanBean.getExecutionYear());
        setDefaultPaymentPlan(tuitionPaymentPlanBean.isDefaultPaymentPlan());
        setCustomized(tuitionPaymentPlanBean.isCustomized());

        LocalizedString mls = new LocalizedString();
        for (final Locale locale : TreasuryConstants.getAvailableLocales()) {
            mls = mls.with(locale, tuitionPaymentPlanBean.getName());
        }

        setCustomizedName(mls);

        for (TuitionConditionRule condition : tuitionPaymentPlanBean.getConditionRules()) {
            addTuitionConditionRules(condition);
        }

        createPaymentPlanOrder(tuitionPaymentPlanBean.getDegreeCurricularPlans());

        tuitionPaymentPlanBean.getTuitionPaymentPlanCalculatorList().forEach(c -> c.setTuitionPaymentPlan(this));

        createInstallments(tuitionPaymentPlanBean);

        tuitionPaymentPlanBean.getTuitionPaymentPlanRecalculationList().forEach(r -> {
            TuitionPaymentPlanRecalculation.create(r.getProduct(), r.getRecalculationDueDate()).setTuitionPaymentPlan(this);
        });

        checkRules();
    }

    public void checkRules() {
        if (getTuitionPaymentPlanGroup() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.tuitionPaymentPlanGroup.required");
        }

        if (getFinantialEntity() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.finantialEntity.required");
        }

        if (getExecutionYear() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.executionYear.required");
        }

        if (getTuitionPaymentPlanOrdersSet().isEmpty()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.tuitionPaymentPlanOrders.required");
        }

        if (isCustomized() && LocalizedStringUtil.isTrimmedEmpty(getCustomizedName())) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.customized.required.name");
        }

        if (isCustomized() && hasStudentSpecificConditionSelected()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.customized.plan.cannot.have.other.options");
        }

        if (isDefaultPaymentPlan() && getTuitionPaymentPlanGroup() != TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration()
                .get()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.default.payment.plan.must.be.for.registration");
        }

        if (getTuitionInstallmentTariffsSet().isEmpty()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.installments.must.not.be.empty");
        }

        if (existsAtLeastOneCalculatorWithMoreThanOneRemaining()) {
            throw new AcademicTreasuryDomainException(
                    "error.TuitionPaymentPlan.installments.customCalculators.must.have.remaining");
        }

//        if (existsAtLeastOneTariffCalculatedAmountWithoutOnlyOneRemaining()) {
//            throw new AcademicTreasuryDomainException(
//                    "error.TuitionPaymentPlan.installments.customCalculators.must.have.remaining");
//        }

        if ((getTuitionPaymentPlanGroup().isForStandalone() || getTuitionPaymentPlanGroup().isForExtracurricular()) && getTuitionInstallmentTariffsSet().size() > 1) {
            throw new AcademicTreasuryDomainException(
                    "error.TuitionPaymentPlan.standalone.and.extracurricular.supports.only.one.installment");
        }

        if (getTuitionPaymentPlanGroup().isForRegistration() && !hasAtLeastOneConditionSpecified()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.specify.at.least.one.condition");
        }
    }

    // TODO
    private boolean existsAtLeastOneCalculatorWithMoreThanOneRemaining() {
        return false;
    }

    private boolean existsAtLeastOneTariffCalculatedAmountWithoutOnlyOneRemaining() {
        if (Boolean.TRUE.equals(getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
            return !getTuitionInstallmentTariffsSet().stream()
                    .filter(tariff -> tariff.getTuitionCalculationType().isCalculatedAmount())
                    .map(tariff -> tariff.getTuitionPaymentPlanCalculator()).allMatch(
                            calculator -> getTuitionInstallmentTariffsSet().stream()
                                    .filter(tariff -> (tariff.getTuitionCalculationType()
                                            .isCalculatedAmount() && tariff.getTuitionTariffCalculatedAmountType() != null && tariff.getTuitionTariffCalculatedAmountType()
                                            .isRemaining() && tariff.getTuitionPaymentPlanCalculator() == calculator))
                                    .count() == 1);
        } else {
            return !getTuitionInstallmentTariffsSet().stream()
                    .filter(tariff -> tariff.getTuitionCalculationType().isCalculatedAmount())
                    .map(tariff -> tariff.getTuitionTariffCustomCalculator()).allMatch(
                            calculator -> getTuitionInstallmentTariffsSet().stream().anyMatch(
                                    tariff -> (tariff.getTuitionCalculationType()
                                            .isCalculatedAmount() && tariff.getTuitionTariffCalculatedAmountType() != null && tariff.getTuitionTariffCalculatedAmountType()
                                            .isRemaining() && tariff.getTuitionTariffCustomCalculator() == calculator)));
        }
    }

    private boolean hasStudentSpecificConditionSelected() {
        return !getTuitionConditionRulesSet().isEmpty();
    }

    private boolean hasAtLeastOneConditionSpecified() {

        boolean hasAtLeastOneCondition = false;

        hasAtLeastOneCondition |= isDefaultPaymentPlan();
        hasAtLeastOneCondition |= !getTuitionConditionRulesSet().isEmpty();
        hasAtLeastOneCondition |= isCustomized();

        return hasAtLeastOneCondition;
    }

    private void createPaymentPlanOrder(Set<DegreeCurricularPlan> degreeCurricularPlans) {
        for (DegreeCurricularPlan plan : degreeCurricularPlans) {
            TuitionPaymentPlanOrder.create(this, plan);
        }
    }

    private TuitionPaymentPlanOrder getTuitionPaymentPlanOrder(DegreeCurricularPlan degreeCurricularPlan) {
        return getTuitionPaymentPlanOrdersSet().stream().filter(order -> order.getDegreeCurricularPlan() == degreeCurricularPlan)
                .findFirst().orElse(null);
    }

    private void createInstallments(final TuitionPaymentPlanBean tuitionPaymentPlanBean) {
        for (final AcademicTariffBean academicTariffBean : tuitionPaymentPlanBean.getTuitionInstallmentBeans()) {
            TuitionInstallmentTariff.create(tuitionPaymentPlanBean.getFinantialEntity(), this, academicTariffBean);
        }
    }

    private void createInstallments(final TuitionPaymentPlan tuitionPaymentPlanToCopy,
            Map<TuitionPaymentPlanCalculator, TuitionPaymentPlanCalculator> calculatorsCopyMap) {
        tuitionPaymentPlanToCopy.getTuitionInstallmentTariffsSet().stream()
                .sorted(TuitionInstallmentTariff.COMPARATOR_BY_INSTALLMENT_NUMBER)
                .forEach(t -> TuitionInstallmentTariff.copy(t, this, calculatorsCopyMap));
    }

    public String getConditionsDescription() {
        if (isCustomized()) {
            return AcademicTreasuryConstants.academicTreasuryBundle(
                    "label.TuitionPaymentPlan.customized") + " [" + getCustomizedName().getContent() + "]";
        }

        if (isDefaultPaymentPlan()) {
            return AcademicTreasuryConstants.academicTreasuryBundle("label.TuitionPaymentPlan.defaultPaymentPlan");
        }

        if (getTuitionPaymentPlanGroup().isForStandalone()) {
            return AcademicTreasuryConstants.academicTreasuryBundle(
                    "label.TuitionPaymentPlan.standalone") + ", " + getTuitionConditionRulesSet().stream()
                    .sorted(TuitionConditionRule.COMPARE_BY_CONDITION_RULE_NAME)
                    .map(c -> TuitionConditionRule.getPresentationName(c.getClass()) + " [" + c.getDescription() + "]")
                    .collect(Collectors.joining(", "));

        }
        if (getTuitionPaymentPlanGroup().isForExtracurricular()) {
            return AcademicTreasuryConstants.academicTreasuryBundle(
                    "label.TuitionPaymentPlan.extracurricular") + ", " + getTuitionConditionRulesSet().stream()
                    .sorted(TuitionConditionRule.COMPARE_BY_CONDITION_RULE_NAME)
                    .map(c -> TuitionConditionRule.getPresentationName(c.getClass()) + " [" + c.getDescription() + "]")
                    .collect(Collectors.joining(", "));
        }

        return getTuitionConditionRulesSet().stream().sorted(TuitionConditionRule.COMPARE_BY_CONDITION_RULE_NAME)
                .map(c -> TuitionConditionRule.getPresentationName(c.getClass()) + " [" + c.getDescription() + "]")
                .collect(Collectors.joining(", "));
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

    public TuitionConditionRule getTuitionConditionRule(Class<? extends TuitionConditionRule> clazz) {
        return getTuitionConditionRulesSet().stream().filter(c -> c.getClass().equals(clazz)).findFirst().orElse(null);
    }

    public LocalizedString installmentName(Registration registration, TuitionInstallmentTariff installmentTariff) {
        return installmentTariff.getTuitionPaymentPlan().getTuitionPaymentPlanGroup()
                .buildDebitEntryDescription(installmentTariff, registration, getExecutionYear());
    }

    public boolean isCustomized() {
        return getCustomized();
    }

    public boolean isDefaultPaymentPlan() {
        return getDefaultPaymentPlan();
    }

    public boolean createDebitEntriesForRegistration(DebtAccount debtAccount, AcademicTreasuryEvent academicTreasuryEvent,
            LocalDate when) {
        return createDebitEntriesForRegistration(debtAccount, academicTreasuryEvent, when, null, false);
    }

    public boolean createDebitEntriesForRegistration(DebtAccount debtAccount, AcademicTreasuryEvent tuitionAcademicTreasuryEvent,
            LocalDate when, Set<Product> restrictCreationToInstallments, boolean forceEvenTreasuryEventIsCharged) {
        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        Registration registration = tuitionAcademicTreasuryEvent.getRegistration();

        if (!getTuitionPaymentPlanGroup().isForRegistration()) {
            throw new RuntimeException("wrong call");
        }

        if (!forceEvenTreasuryEventIsCharged && tuitionAcademicTreasuryEvent.isCharged()) {
            return false;
        }

        StringBuilder strBuilder = new StringBuilder();

        Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap = new HashMap<>();

        if (Boolean.TRUE.equals(getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
            getTuitionInstallmentTariffsSet().stream() //
                    .filter(tariff -> tariff.getTuitionPaymentPlanCalculator() != null) //
                    .map(tariff -> tariff.getTuitionPaymentPlanCalculator()) //
                    .collect(Collectors.toSet()).forEach(calculator -> {
                        strBuilder.append(calculator.getName().getContent(TreasuryConstants.getDefaultLocale())).append(" (")
                                .append(tuitionAcademicTreasuryEvent.formatMoney(calculator.getTotalAmount(registration)))
                                .append("): \n");
                        String description = calculator.getCalculationDescription(registration);
                        strBuilder.append(description).append("\n");
                    });

        } else {
            getTuitionInstallmentTariffsSet().stream().map(tariff -> tariff.getTuitionTariffCustomCalculator())
                    .collect(Collectors.toSet()).forEach(clazz -> {
                        if (clazz != null) {
                            TuitionTariffCustomCalculator newInstanceFor =
                                    TuitionTariffCustomCalculator.getNewInstanceFor(clazz, tuitionAcademicTreasuryEvent.getRegistration(),
                                            this);
                            calculatorsMap.put(clazz, newInstanceFor);
                            strBuilder.append(newInstanceFor.getPresentationName()).append(" (")
                                    .append(tuitionAcademicTreasuryEvent.formatMoney(newInstanceFor.getTotalAmount())).append("): \n");
                            String description = newInstanceFor.getCalculationDescription();
                            strBuilder.append(description).append("\n");
                        }
                    });
        }

        if (strBuilder.length() > 0) {
            Map<String, String> propertiesMap = tuitionAcademicTreasuryEvent.getPropertiesMap();
            propertiesMap.put(TreasuryPlataformDependentServicesFactory.implementation().bundle(AcademicTreasuryConstants.BUNDLE,
                    "label.AcademicTreasury.CustomCalculatorDescription") + " ( " + DateTime.now()
                    .toString("yyyy-MM-dd HH:mm") + " )", strBuilder.toString());
            tuitionAcademicTreasuryEvent.editPropertiesMap(propertiesMap);
        }

        DiscountTuitionInstallmentsHelper discountMapHelper =
                new DiscountTuitionInstallmentsHelper(debtAccount, tuitionAcademicTreasuryEvent, restrictCreationToInstallments,
                        when, calculatorsMap);

        boolean createdDebitEntries = false;
        for (TuitionInstallmentTariff tariff : getTuitionInstallmentTariffsSet().stream()
                .sorted(TuitionInstallmentTariff.COMPARATOR_BY_INSTALLMENT_NUMBER).collect(Collectors.toList())) {

            boolean installmentCreated = discountMapHelper.createInstallmentAndDiscountInstallment(tariff);

            createdDebitEntries = createdDebitEntries || installmentCreated;
        }

        return createdDebitEntries;
    }

    public boolean createDebitEntriesForStandalone(final DebtAccount debtAccount,
            final AcademicTreasuryEvent academicTreasuryEvent, final Enrolment standaloneEnrolment, final LocalDate when) {
        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        Registration registration = academicTreasuryEvent.getRegistration();

        if (!getTuitionPaymentPlanGroup().isForStandalone()) {
            throw new RuntimeException("wrong call");
        }

        if (!standaloneEnrolment.isStandalone()) {
            throw new RuntimeException("error.TuitionPaymentPlan.enrolment.not.standalone");
        }

        StringBuilder strBuilder = new StringBuilder();

        Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap = new HashMap<>();

        if (Boolean.TRUE.equals(getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
            getTuitionInstallmentTariffsSet().stream().filter(tariff -> tariff.getTuitionPaymentPlanCalculator() != null)
                    .map(tariff -> tariff.getTuitionPaymentPlanCalculator()).collect(Collectors.toSet()).forEach(calculator -> {
                        strBuilder.append(calculator.getName().getContent(TreasuryConstants.getDefaultLocale())).append(" (")
                                .append(academicTreasuryEvent.formatMoney(calculator.getTotalAmount(standaloneEnrolment)))
                                .append("): \n");
                        String description = calculator.getCalculationDescription(standaloneEnrolment);
                        strBuilder.append(description).append("\n");
                    });

        } else {
            getTuitionInstallmentTariffsSet().stream().map(tariff -> tariff.getTuitionTariffCustomCalculator())
                    .collect(Collectors.toSet()).forEach(clazz -> {
                        if (clazz != null) {
                            TuitionTariffCustomCalculator newInstanceFor =
                                    TuitionTariffCustomCalculator.getNewInstanceFor(clazz, academicTreasuryEvent.getRegistration(), this,
                                            standaloneEnrolment);
                            calculatorsMap.put(clazz, newInstanceFor);
                            strBuilder.append(newInstanceFor.getPresentationName()).append(" (")
                                    .append(academicTreasuryEvent.formatMoney(newInstanceFor.getTotalAmount())).append("): \n");
                            String description = newInstanceFor.getCalculationDescription();
                            strBuilder.append(description).append("\n");
                        }
                    });

        }

        boolean createdDebitEntries = false;
        final Set<DebitEntry> createdDebitEntriesSet = Sets.newHashSet();
        for (final TuitionInstallmentTariff tariff : getTuitionInstallmentTariffsSet()) {
            if (!academicTreasuryEvent.isChargedWithDebitEntry(standaloneEnrolment)) {
                DebitEntry debitEntry =
                        tariff.createDebitEntryForStandalone(debtAccount, academicTreasuryEvent, standaloneEnrolment, when,
                                calculatorsMap);

                if (strBuilder.length() > 0) {
                    Map<String, String> propertiesMap = debitEntry.getPropertiesMap();
                    propertiesMap.put(TreasuryPlataformDependentServicesFactory.implementation()
                            .bundleI18N(AcademicTreasuryConstants.BUNDLE, "label.AcademicTreasury.CustomCalculatorDescription")
                            .getContent(), strBuilder.toString());
                    debitEntry.editPropertiesMap(propertiesMap);
                }

                createdDebitEntriesSet.add(debitEntry);
                createdDebitEntries = true;
            }
        }

        if (createdDebitEntries) {
            DocumentNumberSeries defaultDocumentNumberSeries =
                    DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), getFinantialEntity());
            final DebitNote debitNote =
                    DebitNote.create(getFinantialEntity(), debtAccount, null, defaultDocumentNumberSeries, new DateTime(),
                            new LocalDate(), null, Collections.emptyMap(), null, null);

            debitNote.addDebitNoteEntries(Lists.newArrayList(createdDebitEntriesSet));

            if (AcademicTreasurySettings.getInstance().isCloseServiceRequestEmolumentsWithDebitNote()) {
                debitNote.closeDocument();
            }
        }

        return createdDebitEntries;
    }

    public boolean createDebitEntriesForExtracurricular(final DebtAccount debtAccount,
            final AcademicTreasuryEvent academicTreasuryEvent, final Enrolment extracurricularEnrolment, final LocalDate when) {
        Registration registration = academicTreasuryEvent.getRegistration();

        if (!getTuitionPaymentPlanGroup().isForExtracurricular()) {
            throw new RuntimeException("wrong call");
        }

        if (!extracurricularEnrolment.isExtraCurricular()) {
            throw new RuntimeException("error.TuitionPaymentPlan.enrolment.not.standalone");
        }
        StringBuilder strBuilder = new StringBuilder();

        Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap = new HashMap<>();

        if (Boolean.TRUE.equals(getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
            getTuitionInstallmentTariffsSet().stream() //
                    .filter(tariff -> tariff.getTuitionPaymentPlanCalculator() != null)
                    .map(tariff -> tariff.getTuitionPaymentPlanCalculator()).collect(Collectors.toSet()).forEach(calculator -> {
                        strBuilder.append(calculator.getName()).append(" (")
                                .append(academicTreasuryEvent.formatMoney(calculator.getTotalAmount(extracurricularEnrolment)))
                                .append("): \n");
                        String description = calculator.getCalculationDescription(extracurricularEnrolment);
                        strBuilder.append(description).append("\n");
                    });

        } else {
            getTuitionInstallmentTariffsSet().stream().map(tariff -> tariff.getTuitionTariffCustomCalculator())
                    .collect(Collectors.toSet()).forEach(clazz -> {
                        if (clazz != null) {
                            TuitionTariffCustomCalculator newInstanceFor =
                                    TuitionTariffCustomCalculator.getNewInstanceFor(clazz, academicTreasuryEvent.getRegistration(), this,
                                            extracurricularEnrolment);
                            calculatorsMap.put(clazz, newInstanceFor);
                            strBuilder.append(newInstanceFor.getPresentationName()).append(" (")
                                    .append(academicTreasuryEvent.formatMoney(newInstanceFor.getTotalAmount())).append("): \n");
                            String description = newInstanceFor.getCalculationDescription();
                            strBuilder.append(description).append("\n");
                        }
                    });

        }

        boolean createdDebitEntries = false;
        final Set<DebitEntry> createdDebitEntriesSet = Sets.newHashSet();
        for (final TuitionInstallmentTariff tariff : getTuitionInstallmentTariffsSet()) {
            if (!academicTreasuryEvent.isChargedWithDebitEntry(extracurricularEnrolment)) {
                DebitEntry debitEntry =
                        tariff.createDebitEntryForExtracurricular(debtAccount, academicTreasuryEvent, extracurricularEnrolment,
                                when, calculatorsMap);

                if (strBuilder.length() > 0) {
                    Map<String, String> propertiesMap = debitEntry.getPropertiesMap();
                    propertiesMap.put(TreasuryPlataformDependentServicesFactory.implementation()
                            .bundleI18N(AcademicTreasuryConstants.BUNDLE, "label.AcademicTreasury.CustomCalculatorDescription")
                            .getContent(), strBuilder.toString());
                    debitEntry.editPropertiesMap(propertiesMap);
                }

                createdDebitEntriesSet.add(debitEntry);
                createdDebitEntries = true;
            }
        }

        if (createdDebitEntries) {
            DocumentNumberSeries defaultDocumentNumberSeries =
                    DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), getFinantialEntity());
            final DebitNote debitNote =
                    DebitNote.create(getFinantialEntity(), debtAccount, null, defaultDocumentNumberSeries, new DateTime(),
                            new LocalDate(), null, Collections.emptyMap(), null, null);

            debitNote.addDebitNoteEntries(Lists.newArrayList(createdDebitEntriesSet));

            if (AcademicTreasurySettings.getInstance().isCloseServiceRequestEmolumentsWithDebitNote()) {
                debitNote.closeDocument();
            }
        }

        return createdDebitEntries;

    }

    public boolean isDeletable() {

        if (getTuitionPaymentPlanGroup().isForRegistration() && isDefaultPaymentPlan()) {

            Set<DegreeCurricularPlan> collectionOfDcp =
                    getTuitionPaymentPlanOrdersSet().stream().map(order -> order.getDegreeCurricularPlan())
                            .collect(Collectors.toSet());

            final Set<TuitionPaymentPlan> allPlans = Sets.newHashSet();
            for (DegreeCurricularPlan degreeCurricularPlan : collectionOfDcp) {
                allPlans.addAll(TuitionPaymentPlan.find(TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get(),
                        degreeCurricularPlan, getExecutionYear()).collect(Collectors.toSet()));
                allPlans.addAll(TuitionPaymentPlan.find(TuitionPaymentPlanGroup.findUniqueDefaultGroupForStandalone().get(),
                        degreeCurricularPlan, getExecutionYear()).collect(Collectors.toSet()));
                allPlans.addAll(TuitionPaymentPlan.find(TuitionPaymentPlanGroup.findUniqueDefaultGroupForExtracurricular().get(),
                        degreeCurricularPlan, getExecutionYear()).collect(Collectors.toSet()));
            }
            for (final TuitionPaymentPlan tuitionPaymentPlan : allPlans) {

                if (tuitionPaymentPlan == this) {
                    continue;
                }

                for (final TuitionInstallmentTariff tuitionInstallmentTariff : tuitionPaymentPlan.getTuitionInstallmentTariffsSet()) {
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

        while (!getTuitionPaymentPlanRecalculationsSet().isEmpty()) {
            getTuitionPaymentPlanRecalculationsSet().iterator().next().delete();
        }

        while (!getTuitionInstallmentTariffsSet().isEmpty()) {
            getTuitionInstallmentTariffsSet().iterator().next().delete();
        }

        getTuitionPaymentPlanCalculatorSet().forEach(c -> c.delete());

        super.getTuitionConditionRulesSet().forEach(rule -> rule.delete());

        while (!getTuitionPaymentPlanOrdersSet().isEmpty()) {
            getTuitionPaymentPlanOrdersSet().iterator().next().delete(false);
        }

        super.setTuitionPaymentPlanGroup(null);
        super.setExecutionYear(null);
        super.setProduct(null);
        this.setFinantialEntity(null);
        this.setPayorDebtAccount(null);

        setCopyFromTuitionPaymentPlan(null);
        while (!getTuitionPaymentPlanCopiesSet().isEmpty()) {
            getTuitionPaymentPlanCopiesSet().iterator().next().setCopyFromTuitionPaymentPlan(null);
        }

        // From old model but migrated tuition payment plans might have associated dcp or other conditions
        super.setDegreeCurricularPlan(null);
        super.setCurricularYear(null);
        super.setRegistrationProtocol(null);
        super.setStatuteType(null);
        super.setIngression(null);

        super.deleteDomainObject();
    }

    // @formatter:off
    /* -------------
     * OTHER METHODS
     * -------------
     */
    // @formatter:on

    // To be extended
    public boolean isStudentMustBeEnrolled() {
        return true;
    }

    @Override
    @Deprecated
    // TODO: Remove the relation payorDebtAccount of TuitionPaymentPlan. 
    // It will be replaced by payorDebtAccount of TuitionInstallmentTariff
    public DebtAccount getPayorDebtAccount() {
        // TODO Auto-generated method stub
        return super.getPayorDebtAccount();
    }

    @Override
    // TODO: Remove the relation payorDebtAccount of TuitionPaymentPlan. 
    // It will be replaced by payorDebtAccount of TuitionInstallmentTariff
    public void setPayorDebtAccount(DebtAccount payorDebtAccount) {
        // TODO Auto-generated method stub
        super.setPayorDebtAccount(payorDebtAccount);
    }

    public boolean isCopyFromOtherExistingTuitionPaymentPlan() {
        return getCopyFromTuitionPaymentPlan() != null;
    }

    public boolean hasCopiesInExecutionInterval(ExecutionInterval executionInterval) {
        return getTuitionPaymentPlanCopiesSet().stream().anyMatch(p -> p.getExecutionYear() == executionInterval);
    }

    public void orderInstallmentsBasedInProductOrder() {
        Comparator<TuitionInstallmentTariff> sortFunc =
                Comparator.comparing(TuitionInstallmentTariff::getProduct, Product.COMPARE_BY_INSTALLMENT_NUMBER_AND_NAME);

        List<TuitionInstallmentTariff> tariffsList =
                getOrderedTuitionInstallmentTariffs().stream().sorted(sortFunc).collect(Collectors.toList());

        int installmentNum = 1;
        for (TuitionInstallmentTariff tariff : tariffsList) {
            tariff.setInstallmentOrder(installmentNum++);
        }

        checkRules();
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

    public static Stream<TuitionPaymentPlan> find(TuitionPaymentPlanGroup tuitionPaymentPlanGroup) {
        return tuitionPaymentPlanGroup.getTuitionPaymentPlansSet().stream();
    }

    public static Stream<TuitionPaymentPlan> find(TuitionPaymentPlanGroup tuitionPaymentPlanGroup, ExecutionYear executionYear) {
        return find(tuitionPaymentPlanGroup).filter(t -> t.getExecutionYear() == executionYear);
    }

    public static Stream<TuitionPaymentPlan> find(TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            FinantialEntity finantialEntity, ExecutionYear executionYear) {
        return find(tuitionPaymentPlanGroup).filter(t -> t.getFinantialEntity() == finantialEntity)
                .filter(t -> t.getExecutionYear() == executionYear);
    }

    @Deprecated
    public static Stream<TuitionPaymentPlan> find(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            final DegreeCurricularPlan degreeCurricularPlan, final ExecutionYear executionYear) {

        return find(tuitionPaymentPlanGroup).filter(
                t -> t.getExecutionYear() == executionYear && t.getTuitionPaymentPlanOrdersSet().stream()
                        .anyMatch(order -> order.getDegreeCurricularPlan() == degreeCurricularPlan));
    }

    @Deprecated
    public static Stream<TuitionPaymentPlan> findSortedByPaymentPlanOrder(final TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            final DegreeCurricularPlan degreeCurricularPlan, final ExecutionYear executionYear) {
        return TuitionPaymentPlanOrder.findSortedByPaymentPlanOrder(tuitionPaymentPlanGroup, degreeCurricularPlan, executionYear)
                .map(order -> order.getTuitionPaymentPlan());
    }

    private static Stream<TuitionPaymentPlan> findDefaultPaymentPlans(final DegreeCurricularPlan degreeCurricularPlan,
            final ExecutionYear executionYear) {
        return find(TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get(), degreeCurricularPlan,
                executionYear).filter(t -> t.isDefaultPaymentPlan());
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
            final ExecutionYear executionYear, Predicate<? super TuitionPaymentPlan> predicate) {

        final List<TuitionPaymentPlan> plans = TuitionPaymentPlan.findSortedByPaymentPlanOrder(
                        TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get(), degreeCurricularPlan, executionYear)
                .collect(Collectors.toList());

        return plans.stream().filter(predicate).findFirst().orElse(null);
    }

    public static TuitionPaymentPlan inferTuitionPaymentPlanForRegistration(final Registration registration,
            final ExecutionYear executionYear, Predicate<? super TuitionPaymentPlan> predicate) {
        final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);

        if (studentCurricularPlan == null) {
            return null;
        }

        final DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();

        return inferTuitionPaymentPlanForRegistration(degreeCurricularPlan, executionYear, predicate);
    }

    public static TuitionPaymentPlan inferTuitionPaymentPlanForRegistration(final Registration registration,
            final ExecutionYear executionYear) {

        final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);

        if (studentCurricularPlan == null) {
            return null;
        }

        Predicate<? super TuitionPaymentPlan> predicate =
                plan -> !plan.isCustomized() && plan.isValidTo(registration, executionYear, null, Collections.emptySet());

        return inferTuitionPaymentPlanForRegistration(registration, executionYear, predicate);
    }

    public static TuitionPaymentPlan inferTuitionPaymentPlanForStandaloneEnrolment(final Registration registration,
            final ExecutionYear executionYear, final Enrolment enrolment) {

        if (!enrolment.isStandalone()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.enrolment.is.not.standalone");
        }

        final DegreeCurricularPlan degreeCurricularPlan = enrolment.getCurricularCourse().getDegreeCurricularPlan();

        final List<TuitionPaymentPlan> filtered = TuitionPaymentPlan.findSortedByPaymentPlanOrder(
                        TuitionPaymentPlanGroup.findUniqueDefaultGroupForStandalone().get(), degreeCurricularPlan, executionYear)
                .collect(Collectors.toList());

        return filtered.stream().filter(plan -> !plan.isCustomized() && plan.isValidTo(registration, executionYear, enrolment,
                Collections.emptySet())).findFirst().orElse(null);
    }

    public static TuitionPaymentPlan inferTuitionPaymentPlanForExtracurricularEnrolment(final Registration registration,
            final ExecutionYear executionYear, final Enrolment enrolment) {

        if (!enrolment.isExtraCurricular()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.enrolment.is.not.extracurricular");
        }

        final DegreeCurricularPlan degreeCurricularPlan = enrolment.getCurricularCourse().getDegreeCurricularPlan();
        final List<TuitionPaymentPlan> filtered = TuitionPaymentPlan.findSortedByPaymentPlanOrder(
                        TuitionPaymentPlanGroup.findUniqueDefaultGroupForExtracurricular().get(), degreeCurricularPlan, executionYear)
                .collect(Collectors.toList());

        return filtered.stream().filter(plan -> !plan.isCustomized() && plan.isValidTo(registration, executionYear, enrolment,
                Collections.emptySet())).findFirst().orElse(null);
    }

    public boolean isValidTo(Registration registration, ExecutionYear executionYear, Enrolment enrolment,
            Set<Class<? extends TuitionConditionRule>> exclude) {
        return isCustomized() || isDefaultPaymentPlan() || getTuitionConditionRulesSet().stream()
                .allMatch(c -> !exclude.contains(c.getClass()) && c.isValidTo(registration, executionYear, enrolment));
    }

    public static boolean firstTimeStudent(final Registration registration, final ExecutionYear executionYear) {
        return registration.isFirstTime(executionYear);
    }

    public static Integer curricularYear(final Registration registration, final ExecutionYear executionYear) {
        return registration.getCurricularYear(executionYear);
    }

    public static Set<Integer> semestersWithEnrolments(final Registration registration, final ExecutionYear executionYear) {
        return registration.getEnrolments(executionYear).stream().map(e -> e.getExecutionPeriod().getChildOrder())
                .collect(Collectors.toSet());
    }

    @Atomic
    public static TuitionPaymentPlan create(final TuitionPaymentPlanBean tuitionPaymentPlanBean) {
        return new TuitionPaymentPlan(tuitionPaymentPlanBean);
    }

    public static TuitionPaymentPlan copy(final TuitionPaymentPlan tuitionPaymentPlanToCopy,
            final ExecutionYear toExecutionYear) {
        return new TuitionPaymentPlan(tuitionPaymentPlanToCopy, toExecutionYear);
    }

    public boolean equalsTuitionPlanConditions(TuitionPaymentPlan plan) {
        return this.containsTuitionPlanConditions(plan) && plan.containsTuitionPlanConditions(this);
    }

    public boolean containsTuitionPlanConditions(TuitionPaymentPlan plan) {

        for (TuitionConditionRule otherCondition : plan.getTuitionConditionRulesSet()) {
            TuitionConditionRule condition = getTuitionConditionRule(otherCondition.getClass());
            if (condition == null || !condition.containsRule(otherCondition)) {
                return false;
            }
        }
        return true;
    }

}
