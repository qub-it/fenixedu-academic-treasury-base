package org.fenixedu.academictreasury.services.tuition;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.joda.time.LocalDate;

import java.math.BigDecimal;

public class RegistrationOptions {
    private final RegistrationTuitionService registrationTuitionService;
    Registration registration;
    ExecutionYear executionYear;
    LocalDate debtDate;
    boolean useDefaultEnrolledEctsCredits = false;

    BigDecimal enrolledEctsUnits;
    BigDecimal enrolledCoursesCount;

    boolean applyDefaultEnrolmentCredits = false;

    RegistrationOptions(RegistrationTuitionService registrationTuitionService, Registration registration,
            ExecutionYear executionYear, LocalDate debtDate) {
        this.registrationTuitionService = registrationTuitionService;
        this.registration = registration;
        this.executionYear = executionYear;
        this.debtDate = debtDate;

        registrationTuitionService.registrationOptions = this;
    }

    public RegistrationOptions useDefaultEnrolledEctsCredits(boolean value) {
        this.useDefaultEnrolledEctsCredits = value;
        return this;
    }

    public RegistrationOptions applyEnrolledEctsUnits(BigDecimal enrolledEctsUnits) {
        this.enrolledEctsUnits = enrolledEctsUnits;
        return this;
    }

    public RegistrationOptions applyEnrolledCoursesCount(BigDecimal enrolledCoursesCount) {
        this.enrolledCoursesCount = enrolledCoursesCount;
        return this;
    }

    public RegistrationOptions applyDefaultEnrolmentCredits(boolean value) {
        this.applyDefaultEnrolmentCredits = value;
        return this;
    }

    public TuitionOptions withTuitionPaymentPlan(TuitionPaymentPlan tuitionPaymentPlan) {
        return new TuitionOptions(registrationTuitionService, tuitionPaymentPlan);
    }

    public TuitionOptions withInferedTuitionPaymentPlan() {
        return new TuitionOptions(registrationTuitionService);
    }

}
