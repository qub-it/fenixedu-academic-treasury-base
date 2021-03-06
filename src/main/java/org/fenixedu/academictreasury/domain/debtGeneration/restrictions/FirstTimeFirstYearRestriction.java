package org.fenixedu.academictreasury.domain.debtGeneration.restrictions;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtGenerationRule;
import org.fenixedu.academictreasury.domain.debtGeneration.IDebtGenerationRuleRestrictionStrategy;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;

public class FirstTimeFirstYearRestriction implements IDebtGenerationRuleRestrictionStrategy {

    @Override
    public boolean isToApply(AcademicDebtGenerationRule rule, Registration registration) {
        return TuitionPaymentPlan.firstTimeStudent(registration, rule.getExecutionYear());
    }

}
