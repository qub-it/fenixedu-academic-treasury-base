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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.DomainObjectUtil;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.coursefunctioncost.CourseFunctionCost;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent.AcademicTreasuryEventKeys;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tuition.calculators.TuitionPaymentPlanCalculator;
import org.fenixedu.academictreasury.dto.tariff.AcademicTariffBean;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.domain.tariff.InterestRateType;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Maps;

import pt.ist.fenixframework.Atomic;

public class TuitionInstallmentTariff extends TuitionInstallmentTariff_Base {

    public static final Comparator<? super TuitionInstallmentTariff> COMPARATOR_BY_INSTALLMENT_NUMBER = (o1, o2) -> {
        int c = Integer.compare(o1.getInstallmentOrder(), o2.getInstallmentOrder());
        return c != 0 ? c : DomainObjectUtil.COMPARATOR_BY_ID.compare(o1, o2);
    };

    protected TuitionInstallmentTariff() {
        super();
    }

    protected TuitionInstallmentTariff(final FinantialEntity finantialEntity, final TuitionPaymentPlan tuitionPaymentPlan,
            final AcademicTariffBean bean) {
        this();

        this.init(finantialEntity, tuitionPaymentPlan, bean);
    }

    public TuitionInstallmentTariff(TuitionInstallmentTariff t, TuitionPaymentPlan copyOfTuitionPaymentPlan,
            Map<TuitionPaymentPlanCalculator, TuitionPaymentPlanCalculator> calculatorsCopyMap) {
        this();

        setTuitionPaymentPlan(copyOfTuitionPaymentPlan);
        this.init(t, calculatorsCopyMap);
    }

    @Override
    protected void init(FinantialEntity finantialEntity, Product product, DateTime beginDate, DateTime endDate,
            DueDateCalculationType dueDateCalculationType, LocalDate fixedDueDate, int numberOfDaysAfterCreationForDueDate,
            boolean applyInterests, InterestRateType interestRateType, int numberOfDaysAfterDueDate, boolean applyInFirstWorkday,
            int maximumDaysToApplyPenalty, BigDecimal interestFixedAmount, BigDecimal rate) {
        throw new RuntimeException("wrong call");
    }

    protected void init(final FinantialEntity finantialEntity, final TuitionPaymentPlan tuitionPaymentPlan,
            final AcademicTariffBean bean) {

        Product product = bean.getTuitionInstallmentProduct();

        if (product == null && (tuitionPaymentPlan.getTuitionPaymentPlanGroup()
                .isForStandalone() || tuitionPaymentPlan.getTuitionPaymentPlanGroup().isForExtracurricular())) {
            product = tuitionPaymentPlan.getProduct();
        }

        super.init(finantialEntity, product, bean.getBeginDate().toDateTimeAtStartOfDay(),
                bean.getEndDate() != null ? bean.getEndDate().toDateTimeAtStartOfDay() : null, bean.getDueDateCalculationType(),
                bean.getFixedDueDate(), bean.getNumberOfDaysAfterCreationForDueDate(), bean.isApplyInterests(),
                bean.getInterestRateType(), bean.getNumberOfDaysAfterDueDate(), bean.isApplyInFirstWorkday(),
                bean.getMaximumDaysToApplyPenalty(), bean.getInterestFixedAmount(), bean.getRate());

        super.setTuitionPaymentPlan(tuitionPaymentPlan);
        super.setInstallmentOrder(bean.getInstallmentOrder());
        super.setTuitionCalculationType(bean.getTuitionCalculationType());
        super.setTuitionTariffCalculatedAmountType(bean.getTuitionTariffCalculatedAmountType());
        setTuitionTariffCustomCalculator(bean.getTuitionTariffCustomCalculator());
        super.setTuitionPaymentPlanCalculator(bean.getTuitionPaymentPlanCalculator());
        super.setFixedAmount(bean.getFixedAmount());
        super.setEctsCalculationType(bean.getEctsCalculationType());
        super.setFactor(bean.getFactor());
        super.setTotalEctsOrUnits(bean.getTotalEctsOrUnits());
        super.setAcademicalActBlockingOff(bean.isAcademicalActBlockingOff());
        this.setBlockAcademicActsOnDebt(bean.isBlockAcademicActsOnDebt());

        if (bean.isApplyMaximumAmount()) {
            if (bean.getMaximumAmount() == null || !AcademicTreasuryConstants.isPositive(bean.getMaximumAmount())) {
                throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.maximum.amount.required",
                        getTuitionPaymentPlan().getConditionsDescription());
            }

            this.setMaximumAmount(bean.getMaximumAmount());
        } else {
            this.setMaximumAmount(BigDecimal.ZERO);
        }

        super.setPayorDebtAccount(bean.getPayorDebtAccount());

        checkRules();
    }

    protected void init(TuitionInstallmentTariff tariff,
            Map<TuitionPaymentPlanCalculator, TuitionPaymentPlanCalculator> calculatorsCopyMap) {
        FinantialEntity finantialEntity = tariff.getFinantialEntity();
        Product product = tariff.getProduct();
        ExecutionYear executionYear = getTuitionPaymentPlan().getExecutionYear();
        ExecutionYear copiedExecutionYear = tariff.getTuitionPaymentPlan().getExecutionYear();

        int executionYearInterval =
                executionYear.getAcademicInterval().getStart().getYear() - copiedExecutionYear.getAcademicInterval().getStart()
                        .getYear();

        super.init(finantialEntity, product, tariff.getBeginDate().plusYears(executionYearInterval),
                tariff.getEndDate() != null ? tariff.getEndDate().plusYears(executionYearInterval) : null,
                tariff.getDueDateCalculationType(),
                tariff.getFixedDueDate() != null ? tariff.getFixedDueDate().plusYears(executionYearInterval) : null,
                tariff.getNumberOfDaysAfterCreationForDueDate(), tariff.isApplyInterests(),
                tariff.getInterestRate() != null ? tariff.getInterestRate().getInterestRateType() : null,
                tariff.getInterestRate() != null ? tariff.getInterestRate().getNumberOfDaysAfterDueDate() : 0,
                tariff.getInterestRate() != null ? tariff.getInterestRate().isApplyInFirstWorkday() : false,
                tariff.getInterestRate() != null ? tariff.getInterestRate().getMaximumDaysToApplyPenalty() : 0,
                tariff.getInterestRate() != null ? tariff.getInterestRate().getInterestFixedAmount() : null,
                tariff.getInterestRate() != null ? tariff.getInterestRate().getRate() : null);

        super.setTuitionTariffCalculatedAmountType(tariff.getTuitionTariffCalculatedAmountType());
        setTuitionTariffCustomCalculator(tariff.getTuitionTariffCustomCalculator());

        if (tariff.getTuitionPaymentPlanCalculator() != null) {
            super.setTuitionPaymentPlanCalculator(calculatorsCopyMap.get(tariff.getTuitionPaymentPlanCalculator()));
        }

        super.setInstallmentOrder(tariff.getInstallmentOrder());
        super.setTuitionCalculationType(tariff.getTuitionCalculationType());
        super.setFixedAmount(tariff.getFixedAmount());
        super.setEctsCalculationType(tariff.getEctsCalculationType());
        super.setFactor(tariff.getFactor());
        super.setTotalEctsOrUnits(tariff.getTotalEctsOrUnits());
        super.setAcademicalActBlockingOff(tariff.isAcademicalActBlockingOff());
        super.setBlockAcademicActsOnDebt(tariff.isBlockAcademicActsOnDebt());
        super.setMaximumAmount(tariff.getMaximumAmount());
        super.setPayorDebtAccount(tariff.getPayorDebtAccount());

        checkRules();
    }

    @Override
    protected void checkRules() {
        super.checkRules();

        if (getTuitionPaymentPlan() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.tuitionPaymentPlan.required");
        }

        if (getFinantialEntity() != getTuitionPaymentPlan().getFinantialEntity()) {
            throw new AcademicTreasuryDomainException(
                    "error.TuitionInstallmentTariff.finantialEntity.different.from.payment.plan");
        }

        if (getInstallmentOrder() <= 0) {
            throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.installmentOrder.must.be.positive");
        }

        if (find(getTuitionPaymentPlan(), getInstallmentOrder()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.tariff.installment.order.already.exists");
        }

        if (getTuitionCalculationType() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.tuitionCalculationType.required");
        }

        if (isTuitionCalculationByEctsOrUnits() && getEctsCalculationType() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.ectsCalculationType.required");
        }

        if (isFixedAmountRequired() && getFixedAmount() == null) {
            throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.fixedAmount.required");
        }

        if (isFixedAmountRequired() && !isPositive(getFixedAmount())) {
            throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.fixedAmount.must.be.positive");
        }

        if (getTuitionPaymentPlan().getTuitionPaymentPlanGroup()
                .isForRegistration() && isTuitionCalculationByEctsOrUnits() && getEctsCalculationType().isDefaultPaymentPlanCourseFunctionCostIndexed()) {
            throw new AcademicTreasuryDomainException(
                    "error.TuitionInstallmentTariff.defaultPaymentPlanCourseFunctionCostIndexed.not.supported.for.registrationTuition");
        }

        if (isDefaultPaymentPlanDependent()) {

            if (getFactor() == null) {
                throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.factor.required",
                        getTuitionCalculationType().getDescriptionI18N().getContent());
            }

            if (getTotalEctsOrUnits() == null) {
                throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.totalEctsOrUnits.required",
                        getTuitionCalculationType().getDescriptionI18N().getContent());
            }

            if (!isPositive(getFactor())) {
                throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.factor.must.be.positive",
                        getTuitionCalculationType().getDescriptionI18N().getContent());
            }

            if (!isPositive(getTotalEctsOrUnits())) {
                throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.totalEctsOrUnits.must.be.positive",
                        getTuitionCalculationType().getDescriptionI18N().getContent());
            }
        }

        if (isAcademicalActBlockingOff() && isBlockAcademicActsOnDebt()) {
            throw new AcademicTreasuryDomainException(
                    "error.TuitionInstallmentTariff.cannot.suspend.and.also.block.academical.acts.on.debt.detailed",
                    getProduct().getName().getContent());
        }

        if (getTuitionPaymentPlanCalculator() != null && !getTuitionPaymentPlan().getTuitionPaymentPlanCalculatorSet()
                .contains(getTuitionPaymentPlanCalculator())) {
            throw new AcademicTreasuryDomainException(
                    "error.TuitionInstallmentTariff.tuitionPaymentPlanCalculator.not.in.tuition.payment.plan");
        }
    }

    private boolean isFixedAmountRequired() {
        // TODO Check code Refactor/20210624-MergeWithISCTE
        // getEctsCalculationType() != null and getTuitionTariffCalculatedAmountType() != null
        // should not be necessary

        if (isTuitionCalculationByEctsOrUnits() && getEctsCalculationType() != null && getEctsCalculationType().isDependentOnDefaultPaymentPlan()) {
            return false;
        }

        List<TuitionTariffCalculatedAmountType> doNotRequiredFixedAmountList =
                List.of(TuitionTariffCalculatedAmountType.PERCENTAGE, TuitionTariffCalculatedAmountType.REMAINING,
                        TuitionTariffCalculatedAmountType.TOTAL_CALCULATED_AMOUNT);

        if (getTuitionCalculationType().isCalculatedAmount() && doNotRequiredFixedAmountList.contains(
                getTuitionTariffCalculatedAmountType())) {
            return false;
        }

        return true;
    }

    private boolean isTuitionCalculationByEctsOrUnits() {
        return getTuitionCalculationType().isEcts() || getTuitionCalculationType().isUnits();
    }

    public boolean isDefaultPaymentPlanDependent() {
        // TODO Check code Refactor/20210624-MergeWithISCTE
        // getEctsCalculationType() != null and getTuitionTariffCalculatedAmountType() != null
        // should not be necessary

        return isTuitionCalculationByEctsOrUnits() && getEctsCalculationType() != null && getEctsCalculationType().isDependentOnDefaultPaymentPlan();
    }

    public boolean isDefaultPaymentPlanDefined() {
        return getTuitionPaymentPlan().getTuitionPaymentPlanOrdersSet().stream().map(order -> order.getDegreeCurricularPlan())
                .allMatch(dcp -> TuitionPaymentPlan.isDefaultPaymentPlanDefined(dcp, getTuitionPaymentPlan().getExecutionYear()));
    }

    public boolean isAcademicalActBlockingOff() {
        return super.getAcademicalActBlockingOff();
    }

    public boolean isBlockAcademicActsOnDebt() {
        return super.getBlockAcademicActsOnDebt();
    }

    public boolean isApplyMaximumAmount() {
        return getMaximumAmount() != null && isPositive(getMaximumAmount());
    }

    public BigDecimal getAmountPerEctsOrUnit(DegreeCurricularPlan dcp) {
        if (getTuitionCalculationType().isFixedAmount()) {
            throw new RuntimeException("invalid call");
        }

        if (getEctsCalculationType().isFixedAmount()) {
            return getFixedAmount();
        }

        if (!isDefaultPaymentPlanDefined()) {
            throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.default.payment.plan.not.defined");
        }

        TuitionPaymentPlan defaultPaymentPlan =
                TuitionPaymentPlan.findUniqueDefaultPaymentPlan(dcp, getTuitionPaymentPlan().getExecutionYear()).get();

        for (final TuitionInstallmentTariff tuitionInstallmentTariff : defaultPaymentPlan.getTuitionInstallmentTariffsSet()) {
            if (!tuitionInstallmentTariff.getTuitionCalculationType().isFixedAmount()) {
                throw new AcademicTreasuryDomainException(
                        "error.TuitionPaymentPlan.default.payment.plan.tariffs.calculation.type.not.fixed.amount");
            }
        }

        return AcademicTreasuryConstants.divide(
                AcademicTreasuryConstants.defaultScale(defaultPaymentPlan.tuitionTotalAmount()).multiply(getFactor()),
                getTotalEctsOrUnits());
    }

    private BigDecimal getAmountPerEctsOrUnitUsingFunctionCostIndexed(final Enrolment enrolment) {
        if (!isTuitionCalculationByEctsOrUnits() || !getEctsCalculationType().isDefaultPaymentPlanCourseFunctionCostIndexed()) {
            throw new RuntimeException("invalid call");
        }

        if (!isDefaultPaymentPlanDefined()) {
            throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.default.payment.plan.not.defined");
        }

        if (!CourseFunctionCost.findUnique(enrolment.getExecutionYear(), enrolment.getCurricularCourse()).isPresent()) {
            throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.courseFunctionCourse.not.defined");
        }

        final CourseFunctionCost cost =
                CourseFunctionCost.findUnique(enrolment.getExecutionYear(), enrolment.getCurricularCourse()).get();
        final TuitionPaymentPlan defaultPaymentPlan =
                TuitionPaymentPlan.findUniqueDefaultPaymentPlan(enrolment.getDegreeCurricularPlanOfDegreeModule(),
                        getTuitionPaymentPlan().getExecutionYear()).get();

        return AcademicTreasuryConstants.divide(
                        AcademicTreasuryConstants.defaultScale(defaultPaymentPlan.tuitionTotalAmount()).multiply(getFactor()),
                        getTotalEctsOrUnits())
                .multiply(AcademicTreasuryConstants.divide(cost.getFunctionCost(), BigDecimal.TEN).add(BigDecimal.ONE));
    }

    // Mark as deprecated
    public BigDecimal amountToPay(final AcademicTreasuryEvent academicTreasuryEvent,
            Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap) {
        final BigDecimal enrolledEctsUnits = academicTreasuryEvent.getEnrolledEctsUnits();
        final BigDecimal enrolledCoursesCount = academicTreasuryEvent.getEnrolledCoursesCount();
        return amountToPay(academicTreasuryEvent.getRegistration(), enrolledEctsUnits, enrolledCoursesCount, calculatorsMap);

    }

    // Mark as deprecated
    public BigDecimal amountToPay(Registration registration, final BigDecimal enrolledEctsUnits,
            final BigDecimal enrolledCoursesCount,
            Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap) {
        if (!getTuitionPaymentPlan().getTuitionPaymentPlanGroup().isForRegistration()) {
            throw new RuntimeException("wrong call");
        }

        BigDecimal amountToPay = null;
        if (getTuitionCalculationType().isFixedAmount()) {
            amountToPay = getFixedAmount();
        } else if (getTuitionCalculationType().isEcts()) {
            amountToPay = enrolledEctsUnits.multiply(getAmountPerEctsOrUnit(registration.getActiveDegreeCurricularPlan()));
        } else if (getTuitionCalculationType().isUnits()) {
            amountToPay = enrolledCoursesCount.multiply(getAmountPerEctsOrUnit(registration.getActiveDegreeCurricularPlan()));
        } else if (getTuitionCalculationType().isCalculatedAmount()) {
            BigDecimal customAmount = null;
            if (Boolean.TRUE.equals(getTuitionPaymentPlan().getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
                customAmount = getTuitionPaymentPlanCalculator().getTotalAmount(registration);
            } else {
                customAmount = calculatorsMap.get(getTuitionTariffCustomCalculator()).getTotalAmount();
            }

            BigDecimal totalCaptiveAmount =
                    getTuitionPaymentPlan().getOrderedTuitionInstallmentTariffs().stream().filter(tariff -> {
                        boolean isSameCalculator = false;

                        if (Boolean.TRUE.equals(
                                getTuitionPaymentPlan().getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
                            isSameCalculator = tariff.getTuitionPaymentPlanCalculator() == getTuitionPaymentPlanCalculator();
                        } else {
                            isSameCalculator =
                                    tariff.getTuitionTariffCustomCalculator().equals(getTuitionTariffCustomCalculator());
                        }

                        return tariff.getTuitionCalculationType()
                                .isCalculatedAmount() && tariff.getTuitionTariffCalculatedAmountType()
                                .isCaptive() && isSameCalculator;
                    }).map(tariff -> tariff.getFixedAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal customAmountWithoutCaptive = customAmount.subtract(totalCaptiveAmount);

            if (getTuitionTariffCalculatedAmountType().isCaptive()) {
                amountToPay = getFixedAmount();
            } else if (getTuitionTariffCalculatedAmountType().isPercentage()) {
                amountToPay = Currency.getValueWithScale(getFactor().multiply(customAmountWithoutCaptive));
            } else if (getTuitionTariffCalculatedAmountType().isRemaining()) {
                BigDecimal totalPercentageAmount =
                        getTuitionPaymentPlan().getOrderedTuitionInstallmentTariffs().stream().filter(tariff -> {
                                    boolean isSameCalculator = false;
                                    if (Boolean.TRUE.equals(
                                            getTuitionPaymentPlan().getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
                                        isSameCalculator = tariff.getTuitionPaymentPlanCalculator() == getTuitionPaymentPlanCalculator();
                                    } else {
                                        isSameCalculator =
                                                tariff.getTuitionTariffCustomCalculator().equals(getTuitionTariffCustomCalculator());
                                    }

                                    return tariff.getTuitionCalculationType()
                                            .isCalculatedAmount() && tariff.getTuitionTariffCalculatedAmountType()
                                            .isPercentage() && isSameCalculator;
                                }).map(tariff -> Currency.getValueWithScale(tariff.getFactor().multiply(customAmountWithoutCaptive)))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalTariffAmount = totalCaptiveAmount.add(totalPercentageAmount);
                amountToPay = customAmount.subtract(totalTariffAmount);
            } else if (getTuitionTariffCalculatedAmountType().isTotalCalculatedAmount()) {
                amountToPay = customAmount;
            }
        }

        if (amountToPay == null) {
            throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.unknown.amountToPay");
        }

        if (isApplyMaximumAmount() && AcademicTreasuryConstants.isGreaterThan(amountToPay, getMaximumAmount())) {
            return getMaximumAmount();
        }

        return amountToPay;
    }

    public BigDecimal amountToPay(ITuitionRegistrationServiceParameters parameters) {
        Registration registration = parameters.getRegistration();
        BigDecimal enrolledEctsUnits = parameters.getEnrolledEctsUnits();
        BigDecimal enrolledCoursesCount = parameters.getEnrolledCoursesCount();
        Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap =
                parameters.getCustomCalculatorsMap();

        if (!getTuitionPaymentPlan().getTuitionPaymentPlanGroup().isForRegistration()) {
            throw new RuntimeException("wrong call");
        }

        BigDecimal amountToPay = null;
        if (getTuitionCalculationType().isFixedAmount()) {
            amountToPay = getFixedAmount();
        } else if (getTuitionCalculationType().isEcts()) {
            amountToPay = enrolledEctsUnits.multiply(getAmountPerEctsOrUnit(registration.getActiveDegreeCurricularPlan()));
        } else if (getTuitionCalculationType().isUnits()) {
            amountToPay = enrolledCoursesCount.multiply(getAmountPerEctsOrUnit(registration.getActiveDegreeCurricularPlan()));
        } else if (getTuitionCalculationType().isCalculatedAmount()) {
            BigDecimal customAmount = null;
            if (Boolean.TRUE.equals(getTuitionPaymentPlan().getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
                customAmount = getTuitionPaymentPlanCalculator().getTotalAmount(registration, parameters);
            } else {
                customAmount = calculatorsMap.get(getTuitionTariffCustomCalculator()).getTotalAmount();
            }

            BigDecimal totalCaptiveAmount =
                    getTuitionPaymentPlan().getOrderedTuitionInstallmentTariffs().stream().filter(tariff -> {
                        boolean isSameCalculator = false;

                        if (Boolean.TRUE.equals(
                                getTuitionPaymentPlan().getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
                            isSameCalculator = tariff.getTuitionPaymentPlanCalculator() == getTuitionPaymentPlanCalculator();
                        } else {
                            isSameCalculator =
                                    tariff.getTuitionTariffCustomCalculator().equals(getTuitionTariffCustomCalculator());
                        }

                        return tariff.getTuitionCalculationType()
                                .isCalculatedAmount() && tariff.getTuitionTariffCalculatedAmountType()
                                .isCaptive() && isSameCalculator;
                    }).map(tariff -> tariff.getFixedAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal customAmountWithoutCaptive = customAmount.subtract(totalCaptiveAmount);

            if (getTuitionTariffCalculatedAmountType().isCaptive()) {
                amountToPay = getFixedAmount();
            } else if (getTuitionTariffCalculatedAmountType().isPercentage()) {
                amountToPay = Currency.getValueWithScale(getFactor().multiply(customAmountWithoutCaptive));
            } else if (getTuitionTariffCalculatedAmountType().isRemaining()) {
                BigDecimal totalPercentageAmount =
                        getTuitionPaymentPlan().getOrderedTuitionInstallmentTariffs().stream().filter(tariff -> {
                                    boolean isSameCalculator = false;
                                    if (Boolean.TRUE.equals(
                                            getTuitionPaymentPlan().getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
                                        isSameCalculator = tariff.getTuitionPaymentPlanCalculator() == getTuitionPaymentPlanCalculator();
                                    } else {
                                        isSameCalculator =
                                                tariff.getTuitionTariffCustomCalculator().equals(getTuitionTariffCustomCalculator());
                                    }

                                    return tariff.getTuitionCalculationType()
                                            .isCalculatedAmount() && tariff.getTuitionTariffCalculatedAmountType()
                                            .isPercentage() && isSameCalculator;
                                }).map(tariff -> Currency.getValueWithScale(tariff.getFactor().multiply(customAmountWithoutCaptive)))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalTariffAmount = totalCaptiveAmount.add(totalPercentageAmount);
                amountToPay = customAmount.subtract(totalTariffAmount);
            } else if (getTuitionTariffCalculatedAmountType().isTotalCalculatedAmount()) {
                amountToPay = customAmount;
            }
        }

        if (amountToPay == null) {
            throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.unknown.amountToPay");
        }

        if (isApplyMaximumAmount() && AcademicTreasuryConstants.isGreaterThan(amountToPay, getMaximumAmount())) {
            return getMaximumAmount();
        }

        return amountToPay;
    }

    public BigDecimal amountToPay(final AcademicTreasuryEvent academicTreasuryEvent, final Enrolment enrolment,
            Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap) {
        if (!(getTuitionPaymentPlan().getTuitionPaymentPlanGroup()
                .isForStandalone() || getTuitionPaymentPlan().getTuitionPaymentPlanGroup().isForExtracurricular())) {
            throw new RuntimeException("wrong call");
        }

        BigDecimal amountToPay = null;
        if (getTuitionCalculationType().isFixedAmount()) {
            amountToPay = getFixedAmount();
        } else if (getTuitionCalculationType().isUnits() && !getEctsCalculationType().isDefaultPaymentPlanCourseFunctionCostIndexed()) {
            amountToPay = getAmountPerEctsOrUnit(enrolment.getDegreeCurricularPlanOfDegreeModule());
        } else if (getTuitionCalculationType().isEcts() && !getEctsCalculationType().isDefaultPaymentPlanCourseFunctionCostIndexed()) {
            amountToPay = new BigDecimal(enrolment.getCurricularCourse().getEctsCredits()).multiply(
                    getAmountPerEctsOrUnit(enrolment.getDegreeCurricularPlanOfDegreeModule()));
        } else if (getTuitionCalculationType().isUnits() && getEctsCalculationType().isDefaultPaymentPlanCourseFunctionCostIndexed()) {
            amountToPay = getAmountPerEctsOrUnitUsingFunctionCostIndexed(enrolment);
        } else if (getTuitionCalculationType().isEcts() && getEctsCalculationType().isDefaultPaymentPlanCourseFunctionCostIndexed()) {
            amountToPay = new BigDecimal(enrolment.getCurricularCourse().getEctsCredits()).multiply(
                    getAmountPerEctsOrUnitUsingFunctionCostIndexed(enrolment));
        } else if (getTuitionCalculationType().isCalculatedAmount()) {
            BigDecimal customAmount = null;

            if (Boolean.TRUE.equals(getTuitionPaymentPlan().getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
                customAmount = getTuitionPaymentPlanCalculator().getTotalAmount(enrolment);
            } else {
                customAmount = calculatorsMap.get(getTuitionTariffCustomCalculator()).getTotalAmount();
            }

            BigDecimal totalCaptiveAmount =
                    getTuitionPaymentPlan().getOrderedTuitionInstallmentTariffs().stream().filter(tariff -> {
                        boolean isSameCalculator = false;

                        if (Boolean.TRUE.equals(
                                getTuitionPaymentPlan().getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
                            isSameCalculator = tariff.getTuitionPaymentPlanCalculator() == getTuitionPaymentPlanCalculator();
                        } else {
                            isSameCalculator =
                                    tariff.getTuitionTariffCustomCalculator().equals(getTuitionTariffCustomCalculator());
                        }

                        return tariff.getTuitionCalculationType()
                                .isCalculatedAmount() && tariff.getTuitionTariffCalculatedAmountType()
                                .isCaptive() && isSameCalculator;
                    }).map(tariff -> tariff.getFixedAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal customAmountWithoutCaptive = customAmount.subtract(totalCaptiveAmount);

            if (getTuitionTariffCalculatedAmountType().isCaptive()) {
                amountToPay = getFixedAmount();
            }
            if (getTuitionTariffCalculatedAmountType().isPercentage()) {
                amountToPay = Currency.getValueWithScale(getFactor().multiply(customAmountWithoutCaptive));
            }
            if (getTuitionTariffCalculatedAmountType().isRemaining()) {
                BigDecimal totalPercentageAmount =
                        getTuitionPaymentPlan().getOrderedTuitionInstallmentTariffs().stream().filter(tariff -> {
                                    boolean isSameCalculator = false;

                                    if (Boolean.TRUE.equals(
                                            getTuitionPaymentPlan().getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
                                        isSameCalculator = tariff.getTuitionPaymentPlanCalculator() == getTuitionPaymentPlanCalculator();
                                    } else {
                                        isSameCalculator =
                                                tariff.getTuitionTariffCustomCalculator().equals(getTuitionTariffCustomCalculator());
                                    }

                                    return tariff.getTuitionCalculationType()
                                            .isCalculatedAmount() && tariff.getTuitionTariffCalculatedAmountType()
                                            .isPercentage() && isSameCalculator;
                                }).map(tariff -> Currency.getValueWithScale(tariff.getFactor().multiply(customAmountWithoutCaptive)))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalTariffAmount = totalCaptiveAmount.add(totalPercentageAmount);
                amountToPay = customAmount.subtract(totalTariffAmount);
            }
        }

        if (amountToPay == null) {
            throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.unknown.amountToPay");
        }

        if (isApplyMaximumAmount() && AcademicTreasuryConstants.isGreaterThan(amountToPay, getMaximumAmount())) {
            return getMaximumAmount();
        }

        return amountToPay;
    }

    public DebitEntry createDebitEntryForRegistration(final DebtAccount debtAccount,
            final AcademicTreasuryEvent academicTreasuryEvent, final LocalDate when,
            Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap,
            BigDecimal amountToDiscount) {

        if (amountToDiscount == null) {
            amountToDiscount = BigDecimal.ZERO;
        }

        if (TreasuryConstants.isNegative(amountToDiscount)) {
            throw new RuntimeException("invalid amount to discount");
        }

        if (!getTuitionPaymentPlan().getTuitionPaymentPlanGroup().isForRegistration()) {
            throw new RuntimeException("wrong call");
        }

        final BigDecimal amount = amountToPay(academicTreasuryEvent, calculatorsMap).subtract(amountToDiscount);

        if (TreasuryConstants.isNegative(amount)) {
            throw new RuntimeException("invalid tuition installment amount");
        }

        final LocalDate dueDate = dueDate(when != null ? when : new LocalDate());

        final Map<String, String> fillPriceProperties =
                fillPricePropertiesForRegistration(academicTreasuryEvent, dueDate, when, calculatorsMap);

        FinantialEntity finantialEntity = getTuitionPaymentPlan().getFinantialEntity();
        final DebitEntry debitEntry =
                DebitEntry.create(finantialEntity, debtAccount, academicTreasuryEvent, vat(when), amount, dueDate,
                        fillPriceProperties, getProduct(), installmentName(academicTreasuryEvent.getRegistration()).getContent(
                                AcademicTreasuryConstants.DEFAULT_LANGUAGE), AcademicTreasuryConstants.DEFAULT_QUANTITY,
                        this.getInterestRate(), when.toDateTimeAtStartOfDay(), false, false, null);

        if (isAcademicalActBlockingOff()) {
            debitEntry.markAcademicalActBlockingSuspension();
        }

        if (isBlockAcademicActsOnDebt()) {
            debitEntry.markBlockAcademicActsOnDebt();
        }

        debitEntry.setPayorDebtAccount(getPayorDebtAccount());

        return debitEntry;
    }

    public DebitEntry createDebitEntryForRegistration(ITuitionRegistrationServiceParameters parameters) {
        final DebtAccount debtAccount =
                parameters.getDebtAccount().orElseThrow(() -> new IllegalStateException("debt account not created"));
        final AcademicTreasuryEvent academicTreasuryEvent = parameters.getAcademicTreasuryEvent()
                .orElseThrow(() -> new IllegalStateException("academic treasury event not created"));

        final LocalDate when = parameters.getDebtDate();

        Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap =
                parameters.getCustomCalculatorsMap();

        if (!getTuitionPaymentPlan().getTuitionPaymentPlanGroup().isForRegistration()) {
            throw new RuntimeException("wrong call");
        }

        final BigDecimal amount = amountToPay(parameters);

        if (TreasuryConstants.isNegative(amount)) {
            throw new RuntimeException("invalid tuition installment amount");
        }

        final LocalDate dueDate = dueDate(when != null ? when : new LocalDate());

        final Map<String, String> fillPriceProperties =
                fillPricePropertiesForRegistration(academicTreasuryEvent, dueDate, when, calculatorsMap);

        FinantialEntity finantialEntity = getTuitionPaymentPlan().getFinantialEntity();
        final DebitEntry debitEntry =
                DebitEntry.create(finantialEntity, debtAccount, academicTreasuryEvent, vat(when), amount, dueDate,
                        fillPriceProperties, getProduct(), installmentName(academicTreasuryEvent.getRegistration()).getContent(
                                AcademicTreasuryConstants.DEFAULT_LANGUAGE), AcademicTreasuryConstants.DEFAULT_QUANTITY,
                        this.getInterestRate(), when.toDateTimeAtStartOfDay(), false, false, null);

        if (isAcademicalActBlockingOff()) {
            debitEntry.markAcademicalActBlockingSuspension();
        }

        if (isBlockAcademicActsOnDebt()) {
            debitEntry.markBlockAcademicActsOnDebt();
        }

        debitEntry.setPayorDebtAccount(getPayorDebtAccount());

        return debitEntry;
    }

    public DebitEntry createRecalculationDebitEntryForRegistration(DebtAccount debtAccount,
            AcademicTreasuryEvent academicTreasuryEvent, LocalDate when,
            Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap,
            BigDecimal recalculatedAmount, LocalDate recalculationDueDate) {

        if (!getTuitionPaymentPlan().getTuitionPaymentPlanGroup().isForRegistration()) {
            throw new RuntimeException("wrong call");
        }

        if (TreasuryConstants.isNegative(recalculatedAmount)) {
            throw new RuntimeException("invalid tuition installment amount");
        }

        Map<String, String> fillPriceProperties =
                fillPricePropertiesForRegistration(academicTreasuryEvent, recalculationDueDate, when, calculatorsMap);

        fillPriceProperties.put("RECALCULATED_AMOUNT", recalculatedAmount.toString());

        String installmentName =
                installmentName(academicTreasuryEvent.getRegistration()).getContent(AcademicTreasuryConstants.DEFAULT_LANGUAGE);

        String installmentDebitEntryName = installmentName;

        TuitionPaymentPlanGroup group = getTuitionPaymentPlan().getTuitionPaymentPlanGroup();
        if (group.getTuitionRecalculationDebitEntryPrefix() != null && StringUtils.isNotEmpty(
                group.getTuitionRecalculationDebitEntryPrefix().getContent(AcademicTreasuryConstants.DEFAULT_LANGUAGE))) {
            installmentDebitEntryName = group.getTuitionRecalculationDebitEntryPrefix()
                    .getContent(AcademicTreasuryConstants.DEFAULT_LANGUAGE) + " " + installmentDebitEntryName;
        }

        if (group.getTuitionRecalculationDebitEntrySuffix() != null && StringUtils.isNotEmpty(
                group.getTuitionRecalculationDebitEntrySuffix().getContent(AcademicTreasuryConstants.DEFAULT_LANGUAGE))) {
            installmentDebitEntryName = installmentDebitEntryName + " " + group.getTuitionRecalculationDebitEntrySuffix()
                    .getContent(AcademicTreasuryConstants.DEFAULT_LANGUAGE);
        }

        DebitEntry debitEntry =
                DebitEntry.create(getTuitionPaymentPlan().getFinantialEntity(), debtAccount, academicTreasuryEvent, vat(when),
                        recalculatedAmount, recalculationDueDate, fillPriceProperties, getProduct(),
                        installmentDebitEntryName.trim(), AcademicTreasuryConstants.DEFAULT_QUANTITY, this.getInterestRate(),
                        when.toDateTimeAtStartOfDay(), false, false, null);

        if (isAcademicalActBlockingOff()) {
            debitEntry.markAcademicalActBlockingSuspension();
        }

        if (isBlockAcademicActsOnDebt()) {
            debitEntry.markBlockAcademicActsOnDebt();
        }

        debitEntry.setPayorDebtAccount(getPayorDebtAccount());

        return debitEntry;
    }

    public DebitEntry createDebitEntryForStandalone(final DebtAccount debtAccount,
            final AcademicTreasuryEvent academicTreasuryEvent, final Enrolment standaloneEnrolment, final LocalDate when,
            Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap) {

        if (!getTuitionPaymentPlan().getTuitionPaymentPlanGroup().isForStandalone()) {
            throw new RuntimeException("wrong call");
        }

        if (!standaloneEnrolment.isStandalone()) {
            throw new RuntimeException("error.TuitionPaymentPlan.enrolment.not.standalone");
        }

        final BigDecimal amount = amountToPay(academicTreasuryEvent, standaloneEnrolment, calculatorsMap);
        final LocalDate dueDate = dueDate(when != null ? when : new LocalDate());

        final Map<String, String> fillPriceProperties =
                fillPricePropertiesForStandaloneOrExtracurricular(academicTreasuryEvent, standaloneEnrolment, dueDate,
                        calculatorsMap);

        FinantialEntity finantialEntity = getTuitionPaymentPlan().getFinantialEntity();
        final DebitEntry debitEntry =
                DebitEntry.create(finantialEntity, debtAccount, academicTreasuryEvent, vat(when), amount, dueDate,
                        fillPriceProperties, getProduct(),
                        standaloneDebitEntryName(standaloneEnrolment).getContent(AcademicTreasuryConstants.DEFAULT_LANGUAGE),
                        AcademicTreasuryConstants.DEFAULT_QUANTITY, this.getInterestRate(), when.toDateTimeAtStartOfDay(), false,
                        false, null);

        if (isAcademicalActBlockingOff()) {
            debitEntry.markAcademicalActBlockingSuspension();
        }

        if (isBlockAcademicActsOnDebt()) {
            debitEntry.markBlockAcademicActsOnDebt();
        }

        academicTreasuryEvent.associateEnrolment(debitEntry, standaloneEnrolment);

        return debitEntry;
    }

    public DebitEntry createDebitEntryForExtracurricular(DebtAccount debtAccount, AcademicTreasuryEvent academicTreasuryEvent,
            Enrolment extracurricularEnrolment, LocalDate when,
            Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap) {
        if (!getTuitionPaymentPlan().getTuitionPaymentPlanGroup().isForExtracurricular()) {
            throw new RuntimeException("wrong call");
        }

        if (!extracurricularEnrolment.isExtraCurricular()) {
            throw new RuntimeException("error.TuitionPaymentPlan.enrolment.not.extracurricular");
        }

        final BigDecimal amount = amountToPay(academicTreasuryEvent, extracurricularEnrolment, calculatorsMap);

        final LocalDate dueDate = dueDate(when != null ? when : new LocalDate());

        final Map<String, String> fillPriceProperties =
                fillPricePropertiesForStandaloneOrExtracurricular(academicTreasuryEvent, extracurricularEnrolment, dueDate,
                        calculatorsMap);

        FinantialEntity finantialEntity = getTuitionPaymentPlan().getFinantialEntity();
        final DebitEntry debitEntry =
                DebitEntry.create(finantialEntity, debtAccount, academicTreasuryEvent, vat(when), amount, dueDate,
                        fillPriceProperties, getProduct(), extracurricularDebitEntryName(extracurricularEnrolment).getContent(
                                AcademicTreasuryConstants.DEFAULT_LANGUAGE), AcademicTreasuryConstants.DEFAULT_QUANTITY,
                        this.getInterestRate(), when.toDateTimeAtStartOfDay(), false, false, null);

        if (isAcademicalActBlockingOff()) {
            debitEntry.markAcademicalActBlockingSuspension();
        }

        if (isBlockAcademicActsOnDebt()) {
            debitEntry.markBlockAcademicActsOnDebt();
        }

        academicTreasuryEvent.associateEnrolment(debitEntry, extracurricularEnrolment);

        return debitEntry;
    }

    public LocalizedString installmentName(Registration registration) {
        return getTuitionPaymentPlan().installmentName(registration, this);
    }

    public LocalizedString standaloneDebitEntryName(final Enrolment standaloneEnrolment) {
        if (!standaloneEnrolment.isStandalone()) {
            throw new RuntimeException("wrong call");
        }

        LocalizedString result = new LocalizedString();
        for (final Locale locale : TreasuryConstants.getAvailableLocales()) {
            result = result.with(locale, AcademicTreasuryConstants.academicTreasuryBundle(locale,
                    "label.TuitionPaymentPlan.standalone.debit.entry.name", standaloneEnrolment.getName().getContent(locale),
                    standaloneEnrolment.getExecutionPeriod().getQualifiedName(),
                    new BigDecimal(standaloneEnrolment.getCurricularCourse().getEctsCredits()).toString()));
        }

        return result;
    }

    public LocalizedString extracurricularDebitEntryName(final Enrolment extracurricularEnrolment) {
        if (!extracurricularEnrolment.isExtraCurricular()) {
            throw new RuntimeException("wrong call");
        }

        LocalizedString result = new LocalizedString();
        for (final Locale locale : TreasuryConstants.getAvailableLocales()) {
            result = result.with(locale, AcademicTreasuryConstants.academicTreasuryBundle(locale,
                    "label.TuitionPaymentPlan.extracurricular.debit.entry.name",
                    extracurricularEnrolment.getName().getContent(locale),
                    extracurricularEnrolment.getExecutionPeriod().getQualifiedName(),
                    new BigDecimal(extracurricularEnrolment.getCurricularCourse().getEctsCredits()).toString()));
        }

        return result;
    }

    public Vat vat(final LocalDate when) {
        return Vat.findActiveUnique(getProduct().getVatType(), getFinantialEntity().getFinantialInstitution(), new DateTime())
                .get();
    }

    private Map<String, String> fillPricePropertiesForStandaloneOrExtracurricular(
            final AcademicTreasuryEvent academicTreasuryEvent, final Enrolment enrolment, final LocalDate dueDate,
            Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap) {
        if (!(getTuitionPaymentPlan().getTuitionPaymentPlanGroup()
                .isForStandalone() || getTuitionPaymentPlan().getTuitionPaymentPlanGroup().isForExtracurricular())) {
            throw new RuntimeException("wrong call");
        }

        final Map<String, String> propertiesMap = Maps.newHashMap();

        propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.ENROLMENT.getDescriptionI18N().getContent(),
                enrolment.getName().getContent(AcademicTreasuryConstants.DEFAULT_LANGUAGE));

        propertiesMap.put(
                AcademicTreasuryEvent.AcademicTreasuryEventKeys.DEGREE_CURRICULAR_PLAN.getDescriptionI18N().getContent(),
                enrolment.getCurricularCourse().getDegreeCurricularPlan().getName());
        propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.DEGREE.getDescriptionI18N().getContent(),
                enrolment.getCurricularCourse().getDegree().getPresentationName());
        propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.DEGREE_CODE.getDescriptionI18N().getContent(),
                enrolment.getCurricularCourse().getDegree().getCode());

        if (getTuitionCalculationType().isFixedAmount()) {
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FIXED_AMOUNT.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency().getValueFor(getFixedAmount()));
        } else if (getTuitionCalculationType().isEcts() && !getEctsCalculationType().isDefaultPaymentPlanCourseFunctionCostIndexed()) {
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.ECTS_CREDITS.getDescriptionI18N().getContent(),
                    new BigDecimal(enrolment.getCurricularCourse().getEctsCredits()).toString());
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.AMOUNT_PER_ECTS.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency()
                            .getValueFor(getAmountPerEctsOrUnit(enrolment.getDegreeCurricularPlanOfDegreeModule()), 3));
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FINAL_AMOUNT.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency()
                            .getValueFor(amountToPay(academicTreasuryEvent, enrolment, calculatorsMap)));
        } else if (getTuitionCalculationType().isUnits() && !getEctsCalculationType().isDefaultPaymentPlanCourseFunctionCostIndexed()) {
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.AMOUNT_PER_COURSE.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency()
                            .getValueFor(getAmountPerEctsOrUnit(enrolment.getDegreeCurricularPlanOfDegreeModule()), 3));
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FINAL_AMOUNT.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency()
                            .getValueFor(amountToPay(academicTreasuryEvent, enrolment, calculatorsMap)));
        } else if (getTuitionCalculationType().isEcts() && getEctsCalculationType().isDefaultPaymentPlanCourseFunctionCostIndexed()) {

            final TuitionPaymentPlan defaultPaymentPlan =
                    TuitionPaymentPlan.findUniqueDefaultPaymentPlan(enrolment.getDegreeCurricularPlanOfDegreeModule(),
                            getTuitionPaymentPlan().getExecutionYear()).get();

            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.DEFAULT_TUITION_TOTAL_AMOUNT.getDescriptionI18N()
                    .getContent(), defaultPaymentPlan.tuitionTotalAmount().toString());

            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.ECTS_CREDITS.getDescriptionI18N().getContent(),
                    new BigDecimal(enrolment.getCurricularCourse().getEctsCredits()).toString());
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.AMOUNT_PER_ECTS.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency()
                            .getValueFor(getAmountPerEctsOrUnitUsingFunctionCostIndexed(enrolment), 3));
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FINAL_AMOUNT.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency()
                            .getValueFor(amountToPay(academicTreasuryEvent, enrolment, calculatorsMap)));

            final CourseFunctionCost cost =
                    CourseFunctionCost.findUnique(enrolment.getExecutionYear(), enrolment.getCurricularCourse()).get();

            propertiesMap.put(
                    AcademicTreasuryEvent.AcademicTreasuryEventKeys.COURSE_FUNCTION_COST.getDescriptionI18N().getContent(),
                    cost.getFunctionCost().toPlainString());

        } else if (getTuitionCalculationType().isUnits() && getEctsCalculationType().isDefaultPaymentPlanCourseFunctionCostIndexed()) {
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.AMOUNT_PER_COURSE.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency()
                            .getValueFor(getAmountPerEctsOrUnitUsingFunctionCostIndexed(enrolment), 3));
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FINAL_AMOUNT.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency()
                            .getValueFor(amountToPay(academicTreasuryEvent, enrolment, calculatorsMap)));

            final CourseFunctionCost cost =
                    CourseFunctionCost.findUnique(enrolment.getExecutionYear(), enrolment.getCurricularCourse()).get();

            propertiesMap.put(
                    AcademicTreasuryEvent.AcademicTreasuryEventKeys.COURSE_FUNCTION_COST.getDescriptionI18N().getContent(),
                    cost.getFunctionCost().toPlainString());
        } else if (getTuitionCalculationType().isCalculatedAmount()) {
            if (getTuitionPaymentPlanCalculator() != null) {
                propertiesMap.put(
                        AcademicTreasuryEvent.AcademicTreasuryEventKeys.CUSTOM_CALCULATOR.getDescriptionI18N().getContent(),
                        getTuitionPaymentPlanCalculator().getClass().getSimpleName());
            } else if (getTuitionTariffCustomCalculator() != null) {
                propertiesMap.put(
                        AcademicTreasuryEvent.AcademicTreasuryEventKeys.CUSTOM_CALCULATOR.getDescriptionI18N().getContent(),
                        getTuitionTariffCustomCalculator().getSimpleName());
            }

            propertiesMap.put(
                    AcademicTreasuryEvent.AcademicTreasuryEventKeys.CALCULATED_AMOUNT_TYPE.getDescriptionI18N().getContent(),
                    getTuitionTariffCalculatedAmountType().getDescriptionI18N().getContent());

            if (getTuitionTariffCalculatedAmountType().isCaptive()) {
                propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FINAL_AMOUNT.getDescriptionI18N().getContent(),
                        getFinantialEntity().getFinantialInstitution().getCurrency()
                                .getValueFor(amountToPay(academicTreasuryEvent, enrolment, calculatorsMap)));
            }

            if (getTuitionTariffCalculatedAmountType().isPercentage()) {
                propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FACTOR.getDescriptionI18N().getContent(),
                        getFactor().toPlainString());
            }

            if (getTuitionTariffCalculatedAmountType().isRemaining()) {
                propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FINAL_AMOUNT.getDescriptionI18N().getContent(),
                        getFinantialEntity().getFinantialInstitution().getCurrency()
                                .getValueFor(amountToPay(academicTreasuryEvent, enrolment, calculatorsMap)));
            }

            if (getTuitionTariffCalculatedAmountType().isTotalCalculatedAmount()) {
                propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FINAL_AMOUNT.getDescriptionI18N().getContent(),
                        getFinantialEntity().getFinantialInstitution().getCurrency()
                                .getValueFor(amountToPay(academicTreasuryEvent, enrolment, calculatorsMap)));
            }
        }

        if (isTuitionCalculationByEctsOrUnits() && getEctsCalculationType().isDependentOnDefaultPaymentPlan()) {
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FACTOR.getDescriptionI18N().getContent(),
                    getFactor().toPlainString());
            propertiesMap.put(
                    AcademicTreasuryEvent.AcademicTreasuryEventKeys.TOTAL_ECTS_OR_UNITS.getDescriptionI18N().getContent(),
                    getTotalEctsOrUnits().toPlainString());
        }

        if (isApplyMaximumAmount()) {
            propertiesMap.put(AcademicTreasuryEventKeys.MAXIMUM_AMOUNT.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency().getValueFor(getMaximumAmount()));
        }

        propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.DUE_DATE.getDescriptionI18N().getContent(),
                dueDate.toString(AcademicTreasuryConstants.DATE_FORMAT));

        return propertiesMap;
    }

    public Map<String, String> fillPricePropertiesForRegistration(AcademicTreasuryEvent event, LocalDate dueDate,
            LocalDate usedDate,
            Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap) {

        if (!getTuitionPaymentPlan().getTuitionPaymentPlanGroup().isForRegistration()) {
            throw new RuntimeException("wrong call");
        }

        final Map<String, String> propertiesMap = Maps.newHashMap();

        propertiesMap.put(
                AcademicTreasuryEvent.AcademicTreasuryEventKeys.TUITION_CALCULATION_TYPE.getDescriptionI18N().getContent(),
                getTuitionCalculationType().getDescriptionI18N().getContent(AcademicTreasuryConstants.DEFAULT_LANGUAGE));
        propertiesMap.put(
                AcademicTreasuryEvent.AcademicTreasuryEventKeys.TUITION_PAYMENT_PLAN_CONDITIONS.getDescriptionI18N().getContent(),
                getTuitionPaymentPlan().getConditionsDescription());

        if (getPayorDebtAccount() != null) {
            propertiesMap.put(
                    AcademicTreasuryEvent.AcademicTreasuryEventKeys.TUITION_PAYOR_DEBT_ACCOUNT.getDescriptionI18N().getContent(),
                    getPayorDebtAccount().getCustomer().getUiFiscalNumber());
        }

        final TuitionPaymentPlanGroup tuitionPaymentPlanGroup = event.getTuitionPaymentPlanGroup();
        final Registration registration = event.getRegistration();
        final ExecutionYear executionYear = event.getExecutionYear();

        if (getTuitionCalculationType().isFixedAmount()) {
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FIXED_AMOUNT.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency().getValueFor(getFixedAmount()));
        } else if (getTuitionCalculationType().isEcts()) {
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.ECTS_CREDITS.getDescriptionI18N().getContent(),
                    AcademicTreasuryEvent.getEnrolledEctsUnits(tuitionPaymentPlanGroup, registration, executionYear).toString());
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.AMOUNT_PER_ECTS.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency()
                            .getValueFor(getAmountPerEctsOrUnit(registration.getActiveDegreeCurricularPlan()), 3));
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FINAL_AMOUNT.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency().getValueFor(amountToPay(event, calculatorsMap)));

        } else if (getTuitionCalculationType().isUnits()) {
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.ENROLLED_COURSES.getDescriptionI18N().getContent(),
                    AcademicTreasuryEvent.getEnrolledCoursesCount(tuitionPaymentPlanGroup, registration, executionYear)
                            .toString());
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.AMOUNT_PER_COURSE.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency()
                            .getValueFor(getAmountPerEctsOrUnit(registration.getActiveDegreeCurricularPlan()), 3));
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FINAL_AMOUNT.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency().getValueFor(amountToPay(event, calculatorsMap)));
        } else if (getTuitionCalculationType().isCalculatedAmount()) {
            if (getTuitionPaymentPlanCalculator() != null) {
                propertiesMap.put(
                        AcademicTreasuryEvent.AcademicTreasuryEventKeys.CUSTOM_CALCULATOR.getDescriptionI18N().getContent(),
                        getTuitionPaymentPlanCalculator().getClass().getSimpleName());
            } else if (getTuitionTariffCustomCalculator() != null) {
                propertiesMap.put(
                        AcademicTreasuryEvent.AcademicTreasuryEventKeys.CUSTOM_CALCULATOR.getDescriptionI18N().getContent(),
                        getTuitionTariffCustomCalculator().getSimpleName());
            }

            propertiesMap.put(
                    AcademicTreasuryEvent.AcademicTreasuryEventKeys.CALCULATED_AMOUNT_TYPE.getDescriptionI18N().getContent(),
                    getTuitionTariffCalculatedAmountType().getDescriptionI18N().getContent());
            if (getTuitionTariffCalculatedAmountType().isCaptive()) {
                propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FINAL_AMOUNT.getDescriptionI18N().getContent(),
                        getFinantialEntity().getFinantialInstitution().getCurrency()
                                .getValueFor(amountToPay(event, calculatorsMap)));
            }
            if (getTuitionTariffCalculatedAmountType().isPercentage()) {
                propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FACTOR.getDescriptionI18N().getContent(),
                        getFactor().toPlainString());
            }
            if (getTuitionTariffCalculatedAmountType().isRemaining()) {
                propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FINAL_AMOUNT.getDescriptionI18N().getContent(),
                        getFinantialEntity().getFinantialInstitution().getCurrency()
                                .getValueFor(amountToPay(event, calculatorsMap)));
            }
        }

        if (isTuitionCalculationByEctsOrUnits() && getEctsCalculationType().isDefaultPaymentPlanIndexed()) {
            propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.FACTOR.getDescriptionI18N().getContent(),
                    getFactor().toPlainString());
            propertiesMap.put(
                    AcademicTreasuryEvent.AcademicTreasuryEventKeys.TOTAL_ECTS_OR_UNITS.getDescriptionI18N().getContent(),
                    getTotalEctsOrUnits().toPlainString());
        }

        propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.USED_DATE.getDescriptionI18N().getContent(),
                usedDate.toString(AcademicTreasuryConstants.DATE_FORMAT));
        propertiesMap.put(AcademicTreasuryEvent.AcademicTreasuryEventKeys.DUE_DATE.getDescriptionI18N().getContent(),
                dueDate.toString(AcademicTreasuryConstants.DATE_FORMAT));

        propertiesMap.put(AcademicTreasuryEventKeys.DEGREE_CODE.getDescriptionI18N().getContent(),
                event.getRegistration().getDegree().getCode());
        propertiesMap.put(AcademicTreasuryEventKeys.DEGREE.getDescriptionI18N().getContent(),
                event.getRegistration().getDegree().getPresentationName(event.getExecutionYear()));
        propertiesMap.put(AcademicTreasuryEventKeys.DEGREE_CURRICULAR_PLAN.getDescriptionI18N().getContent(),
                event.getRegistration().getDegreeCurricularPlanName());

        if (isApplyMaximumAmount()) {
            propertiesMap.put(AcademicTreasuryEventKeys.MAXIMUM_AMOUNT.getDescriptionI18N().getContent(),
                    getFinantialEntity().getFinantialInstitution().getCurrency().getValueFor(getMaximumAmount()));
        }

        return propertiesMap;
    }

    @Atomic
    public void edit(final AcademicTariffBean bean) {
        super.setApplyInterests(bean.isApplyInterests());
        if (getInterestRate() == null && bean.isApplyInterests()) {
            setInterestRate(
                    InterestRate.createForTariff(this, bean.getInterestRateType(), bean.getNumberOfDaysAfterCreationForDueDate(),
                            bean.isApplyInFirstWorkday(), bean.getMaximumDaysToApplyPenalty(), bean.getInterestFixedAmount(),
                            bean.getRate()));
        } else if (getInterestRate() != null && !bean.isApplyInterests()) {
            getInterestRate().delete();
        } else if (getInterestRate() != null && bean.isApplyInterests()) {
            getInterestRate().edit(bean.getInterestRateType(), bean.getNumberOfDaysAfterDueDate(), bean.isApplyInFirstWorkday(),
                    bean.getMaximumDaysToApplyPenalty(), bean.getInterestFixedAmount(), bean.getRate());
        }

        super.setInstallmentOrder(bean.getInstallmentOrder());
        super.setBeginDate(bean.getBeginDate().toDateTimeAtStartOfDay());
        super.setEndDate(bean.getEndDate() != null ? bean.getEndDate().toDateTimeAtStartOfDay() : null);

        super.setTuitionCalculationType(bean.getTuitionCalculationType());
        super.setTuitionTariffCalculatedAmountType(bean.getTuitionTariffCalculatedAmountType());

        super.setTuitionTariffCustomCalculatorClassName(
                bean.getTuitionTariffCustomCalculator() != null ? bean.getTuitionTariffCustomCalculator().getName() : null);

        super.setTuitionPaymentPlanCalculator(bean.getTuitionPaymentPlanCalculator());

        this.setDueDateCalculationType(bean.getDueDateCalculationType());

        this.setFixedDueDate(bean.getFixedDueDate());

        super.setNumberOfDaysAfterCreationForDueDate(bean.getNumberOfDaysAfterCreationForDueDate());

        super.setFixedAmount(bean.getFixedAmount());
        super.setEctsCalculationType(bean.getEctsCalculationType());
        super.setFactor(bean.getFactor());
        super.setTotalEctsOrUnits(bean.getTotalEctsOrUnits());
        super.setAcademicalActBlockingOff(bean.isAcademicalActBlockingOff());
        super.setBlockAcademicActsOnDebt(bean.isBlockAcademicActsOnDebt());

        if (bean.isApplyMaximumAmount()) {
            if (bean.getMaximumAmount() == null || !AcademicTreasuryConstants.isPositive(bean.getMaximumAmount())) {
                throw new AcademicTreasuryDomainException("error.TuitionInstallmentTariff.maximum.amount.required",
                        getTuitionPaymentPlan().getConditionsDescription());
            }

            super.setMaximumAmount(bean.getMaximumAmount());
        } else {
            this.setMaximumAmount(BigDecimal.ZERO);
        }

        super.setPayorDebtAccount(bean.getPayorDebtAccount());

        checkRules();
    }

    @Override
    public void delete() {
        super.setTuitionPaymentPlan(null);
        super.setPayorDebtAccount(null);
        super.setTuitionPaymentPlanCalculator(null);

        super.delete();
    }

    @Override
    public BigDecimal amountToPay() {
        throw new RuntimeException("not supported");
    }

    @Override
    public boolean isBroadTariffForFinantialEntity() {
        return false;
    }

    // @formatter:off
    /* --------
     * SERVICES
     * --------
     */
    // @formatter:on

    protected static Stream<TuitionInstallmentTariff> find(final TuitionPaymentPlan tuitionPaymentPlan,
            final int installmentOrder) {
        return tuitionPaymentPlan.getTuitionInstallmentTariffsSet().stream()
                .filter(t -> t.getInstallmentOrder() == installmentOrder);
    }

    public static Optional<TuitionInstallmentTariff> findUnique(final TuitionPaymentPlan tuitionPaymentPlan,
            final int installmentOrder) {
        return find(tuitionPaymentPlan, installmentOrder).findFirst();
    }

    public static TuitionInstallmentTariff create(final FinantialEntity finantialEntity,
            final TuitionPaymentPlan tuitionPaymentPlan, final AcademicTariffBean bean) {
        return new TuitionInstallmentTariff(finantialEntity, tuitionPaymentPlan, bean);
    }

    public static TuitionInstallmentTariff copy(TuitionInstallmentTariff tuitionInstallmentTariffToCopy,
            TuitionPaymentPlan copyOfTuitionPaymentPlan,
            Map<TuitionPaymentPlanCalculator, TuitionPaymentPlanCalculator> calculatorsCopyMap) {
        return new TuitionInstallmentTariff(tuitionInstallmentTariffToCopy, copyOfTuitionPaymentPlan, calculatorsCopyMap);
    }

    public Class<? extends TuitionTariffCustomCalculator> getTuitionTariffCustomCalculator() {
        try {
            return getTuitionTariffCustomCalculatorClassName() == null ? null : (Class<? extends TuitionTariffCustomCalculator>) Class.forName(
                    getTuitionTariffCustomCalculatorClassName());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public void setTuitionTariffCustomCalculator(Class<? extends TuitionTariffCustomCalculator> clazz) {
        setTuitionTariffCustomCalculatorClassName(clazz != null ? clazz.getName() : "");
    }

    @Override
    public void setFixedDueDate(LocalDate fixedDueDate) {
        if (getDueDateCalculationType().isBestOfFixedDateAndDaysAfterCreation()) {
            super.setFixedDueDate(fixedDueDate);
        } else {
            super.setFixedDueDate(null);
        }
    }

    @Override
    public void setDueDateCalculationType(DueDateCalculationType dueDateCalculationType) {
        super.setDueDateCalculationType(dueDateCalculationType);
        if (!dueDateCalculationType.isBestOfFixedDateAndDaysAfterCreation()) {
            setFixedDueDate(null);
        }

    }

}
