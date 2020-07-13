package org.fenixedu.academictreasury.domain.debtGeneration;

import org.fenixedu.academic.domain.student.Registration;

@Deprecated
public interface IDebtGenerationRuleRestrictionStrategy {

    public boolean isToApply(AcademicDebtGenerationRule rule, Registration registration);

}
