package org.fenixedu.academictreasury.domain.tuition.customcalculators;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundleI18N;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.domain.tuition.TuitionTariffCustomCalculator;
import org.joda.time.LocalDate;

public class Teste999CustomCalculator implements TuitionTariffCustomCalculator {

    @Override
    public BigDecimal getTotalAmount(Registration registration, LocalDate debtDate, TuitionPaymentPlan tuitionPaymentPlan) {
        return new BigDecimal(999.99);
    }

    @Override
    public String getPresentationName() {
        return academicTreasuryBundleI18N(getClass().getName()).getContent();
    }

}
