package org.fenixedu.academictreasury.domain.tuition;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.joda.time.LocalDate;

public interface ITuitionRegistrationServiceParameters {

    Registration getRegistration();
    ExecutionYear getExecutionYear();
    Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> getCustomCalculatorsMap();
    
    BigDecimal getEnrolledEctsUnits();
    BigDecimal getEnrolledCoursesCount();
    
    boolean isApplyDefaultEnrolmentCredits();
    
    Optional<DebtAccount> getDebtAccount();
    Optional<AcademicTreasuryEvent> getAcademicTreasuryEvent();
    LocalDate getDebtDate();
    
}
