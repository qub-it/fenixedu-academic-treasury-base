package org.fenixedu.academictreasury.domain.tuition.conditionRule;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionAnnotation;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionRule;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;

@TuitionConditionAnnotation(FirstTimeStudentConditionRule.BUNDLE_NAME)
public class FirstTimeStudentConditionRule extends FirstTimeStudentConditionRule_Base {

    public static final String BUNDLE_NAME = AcademicTreasuryConstants.BUNDLE;

    public FirstTimeStudentConditionRule() {
        super();
    }

    @Override
    public boolean containsRule(TuitionConditionRule tuitionConditionRule) {
        if (!(tuitionConditionRule instanceof FirstTimeStudentConditionRule)) {
            return false;
        }
        FirstTimeStudentConditionRule rule = (FirstTimeStudentConditionRule) tuitionConditionRule;
        return getFirstTimeStudent().equals(rule.getFirstTimeStudent());
    }

    @Override
    public boolean isValidTo(Registration registration, ExecutionYear executionYear, Enrolment enrolment) {
        boolean registrationFirstTime = registration.isFirstTime(executionYear);
        return Boolean.logicalAnd(getFirstTimeStudent(), registrationFirstTime);
    }

    @Override
    public boolean checkRules() {
        if (getFirstTimeStudent() == null) {
            throw new DomainException(i18n(
                    "org.fenixedu.academictreasury.domain.tuition.conditionRule.firstTimeStudentConditionRule.firstTimeStudent.cannotBeNull"));
        }
        return true;
    }

    @Override
    public String getDescription() {
        return (Boolean.TRUE.equals(getFirstTimeStudent()) ? i18n("label.true") : i18n("label.false"));
    }

    @Override
    protected String getBundle() {
        return BUNDLE_NAME;
    }

    @Override
    public void delete() {
        setTuitionPaymentPlan(null);
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Override
    public TuitionConditionRule duplicate() {
        FirstTimeStudentConditionRule result = new FirstTimeStudentConditionRule();
        result.setFirstTimeStudent(getFirstTimeStudent());
        return result;
    }

    @Override
    public void fillRuleFromImporter(String value) {
        if (value.equals(i18n("label.true"))) {
            setFirstTimeStudent(Boolean.TRUE);
            return;
        }

        if (value.equals(i18n("label.false"))) {
            setFirstTimeStudent(Boolean.FALSE);
            return;
        }
        throw new IllegalArgumentException();
    }

}
