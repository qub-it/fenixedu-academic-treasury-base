package org.fenixedu.academictreasury.domain.tuition.calculators;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.services.TuitionServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestTuitionPaymentPlanCalculator extends TestTuitionPaymentPlanCalculator_Base {

    public TestTuitionPaymentPlanCalculator() {
        super();
    }

    public TestTuitionPaymentPlanCalculator(LocalizedString name) {
        this();

        super.setName(name);

        checkRules();
    }

    private void checkRules() {
    }

    public void edit(LocalizedString name, BigDecimal amount) {
        super.setName(name);
        super.setAmount(amount);

        checkRules();
    }

    public boolean isValid() {
        return getAmount() != null && TreasuryConstants.isPositive(getAmount());
    }

    @Override
    public BigDecimal getTotalAmount(Registration registration) {
        if (!isValid()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanCalculator.not.valid");
        }

        BigDecimal normalEnrolmentsEcts =
                TuitionServices.normalEnrolmentsIncludingAnnuled(registration, getTuitionPaymentPlan().getExecutionYear())
                        .stream().map(e -> e.getEctsCreditsForCurriculum()).reduce(BigDecimal.ZERO, BigDecimal::add);

        return getAmount().multiply(normalEnrolmentsEcts);
    }

    @Override
    public BigDecimal getTotalAmount(Enrolment enrolment) {
        return enrolment.getEctsCreditsForCurriculum().multiply(getAmount());
    }

    @Override
    public String getCalculationDescription(Registration registration) {
        BigDecimal normalEnrolmentsEcts =
                TuitionServices.normalEnrolmentsIncludingAnnuled(registration, getTuitionPaymentPlan().getExecutionYear())
                        .stream().map(e -> e.getEctsCreditsForCurriculum()).reduce(BigDecimal.ZERO, BigDecimal::add);

        String description = BundleUtil.getString(AcademicTreasuryConstants.BUNDLE, TreasuryConstants.getDefaultLocale(),
                "label.TestTuitionPaymentPlanCalculator.calculationDescription", getAmount().toString(),
                normalEnrolmentsEcts.toString(), getTotalAmount(registration).toString());

        return description;
    }

    @Override
    public String getCalculationDescription(Enrolment enrolment) {
        String description = BundleUtil.getString(AcademicTreasuryConstants.BUNDLE, TreasuryConstants.getDefaultLocale(),
                "label.TestTuitionPaymentPlanCalculator.calculationDescription", getAmount().toString(),
                enrolment.getEctsCreditsForCurriculum().toString(), getTotalAmount(enrolment).toString());

        return description;
    }

    @Override
    public LocalizedString getParametersDescription() {
        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        LocalizedString result = TreasuryConstants.getAvailableLocales().stream().map(locale -> new LocalizedString(locale,
                BundleUtil.getString(AcademicTreasuryConstants.BUNDLE, locale,
                        "label.TestTuitionPaymentPlanCalculator.parametersDescription",
                        getAmount() != null ? getAmount().toString() : "N/A"))).reduce((a, c) -> a.append(c)).get();

        return result;
    }

    public static LocalizedString getCalculatorPresentationName() {
        return TreasuryConstants.getAvailableLocales().stream() //
                .map(locale -> {
                    return new LocalizedString(locale, BundleUtil.getString(AcademicTreasuryConstants.BUNDLE, locale,
                            TestTuitionPaymentPlanCalculator.class.getName()));
                }).reduce((a, c) -> a.append(c)).get();
    }

    @Override
    public TuitionPaymentPlanCalculator copyTo(TuitionPaymentPlan tuitionPaymentPlanTarget) {
        TestTuitionPaymentPlanCalculator copy = new TestTuitionPaymentPlanCalculator();

        copy.setAmount(getAmount());
        copy.setTuitionPaymentPlan(tuitionPaymentPlanTarget);

        return copy;
    }

    @Override
    public TuitionPaymentPlanCalculator copyTo(TuitionCalculatorAggregator tuitionCalculatorAggregatorTarget) {
        TestTuitionPaymentPlanCalculator calculator = new TestTuitionPaymentPlanCalculator();

        calculator.setAmount(getAmount());
        calculator.setTuitionCalculatorParentAggregator(tuitionCalculatorAggregatorTarget);

        return calculator;
    }

    @Override
    public void fillWithParametersFromImportation(String parameters) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            BigDecimal amount = objectMapper.readValue(parameters, BigDecimal.class);

            super.setAmount(amount);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete() {
        super.delete();
    }

    public static TestTuitionPaymentPlanCalculator create(LocalizedString name) {
        return new TestTuitionPaymentPlanCalculator(name);
    }

}
