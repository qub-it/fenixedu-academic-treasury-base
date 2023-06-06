package org.fenixedu.academictreasury.domain.tuition.calculators;

import java.math.BigDecimal;
import java.util.Locale;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.services.TuitionServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;

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
        if(!isValid()) {
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

        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        String description = services.bundle(services.defaultLocale(), AcademicTreasuryConstants.BUNDLE,
                "label.TestTuitionPaymentPlanCalculator.calculationDescription", getAmount().toString(),
                normalEnrolmentsEcts.toString(), getTotalAmount(registration).toString());

        return description;
    }

    @Override
    public String getCalculationDescription(Enrolment enrolment) {
        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        String description = services.bundle(services.defaultLocale(), AcademicTreasuryConstants.BUNDLE,
                "label.TestTuitionPaymentPlanCalculator.calculationDescription", getAmount().toString(),
                enrolment.getEctsCreditsForCurriculum().toString(), getTotalAmount(enrolment).toString());

        return description;
    }

    @Override
    public LocalizedString getParametersDescription() {
        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        LocalizedString result = services.availableLocales().stream()
                .map(locale -> new LocalizedString(locale, services.bundle(locale, AcademicTreasuryConstants.BUNDLE,
                        "label.TestTuitionPaymentPlanCalculator.parametersDescription", getAmount() != null ? getAmount().toString() : "N/A")))
                .reduce((a, c) -> a.append(c)).get();

        return result;
    }

    public static LocalizedString getCalculatorPresentationName() {
        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        return TreasuryPlataformDependentServicesFactory.implementation().availableLocales().stream() //
                .map(locale -> {
                    return new LocalizedString(locale, services.bundle(locale, AcademicTreasuryConstants.BUNDLE,
                            TestTuitionPaymentPlanCalculator.class.getName()));
                }).reduce((a, c) -> a.append(c)).get();
    }

    @Override
    public TuitionPaymentPlanCalculator copyTo(TuitionPaymentPlan tuitionPaymentPlanTarget) {
        TestTuitionPaymentPlanCalculator calculator = new TestTuitionPaymentPlanCalculator();

        calculator.setAmount(getAmount());
        calculator.setTuitionPaymentPlan(tuitionPaymentPlanTarget);

        return calculator;
    }

    public void delete() {
        super.delete();
    }

    public static TestTuitionPaymentPlanCalculator create(LocalizedString name) {
        return new TestTuitionPaymentPlanCalculator(name);
    }

}
