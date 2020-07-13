package org.fenixedu.academictreasury.domain.debtGeneration.restrictions;

import java.util.Collections;
import java.util.List;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtGenerationRule;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtGenerationRuleRestriction;
import org.fenixedu.academictreasury.domain.debtGeneration.IDebtGenerationRuleRestrictionStrategy;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.commons.i18n.LocalizedString;

public class EnrolmentRenewalRestriction implements IDebtGenerationRuleRestrictionStrategy {

    @Override
    public boolean isToApply(final AcademicDebtGenerationRule rule, final Registration registration) {
        return !TuitionPaymentPlan.firstTimeStudent(registration, rule.getExecutionYear());
    }
        
}
