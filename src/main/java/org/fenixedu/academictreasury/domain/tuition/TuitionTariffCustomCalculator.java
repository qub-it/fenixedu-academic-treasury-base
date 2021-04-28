package org.fenixedu.academictreasury.domain.tuition;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

import org.fenixedu.academic.domain.student.Registration;
import org.joda.time.LocalDate;

public interface TuitionTariffCustomCalculator {
    public BigDecimal getTotalAmount(final Registration registration, final LocalDate debtDate,
            TuitionPaymentPlan tuitionPaymentPlan);

    public String getPresentationName();

    public static TuitionTariffCustomCalculator instanceOf(
            Class<? extends TuitionTariffCustomCalculator> tuitionTariffCustomCalculator) {

        try {
            return tuitionTariffCustomCalculator.getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException("error.create.instance.of.TuitionTariffCustomCalculator");

        }
    }
}
