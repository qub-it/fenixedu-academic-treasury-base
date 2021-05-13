package org.fenixedu.academictreasury.domain.tuition.conditionRule;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionAnnotation;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionRule;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;

@TuitionConditionAnnotation(WithLaboratorialClassesConditionRule.BUNDLE_NAME)
public class WithLaboratorialClassesConditionRule extends WithLaboratorialClassesConditionRule_Base {

    public static final String BUNDLE_NAME = AcademicTreasuryConstants.BUNDLE;

    public WithLaboratorialClassesConditionRule() {
        super();
    }

    @Override
    public boolean containsRule(TuitionConditionRule tuitionConditionRule) {
        if (!(tuitionConditionRule instanceof WithLaboratorialClassesConditionRule)) {
            return false;
        }
        WithLaboratorialClassesConditionRule rule = (WithLaboratorialClassesConditionRule) tuitionConditionRule;
        return getWithLaboratorialClasses().equals(rule.getWithLaboratorialClasses());
    }

    @Override
    public boolean isValidTo(Registration registration, ExecutionYear executionYear, Enrolment enrolment) {
        if (enrolment == null) {
            return false;
        }
        boolean hasLaboratorialClasses = AcademicTreasuryConstants.isPositive(new BigDecimal(
                enrolment.getCurricularCourse().getCompetenceCourse().getLaboratorialHours(enrolment.getExecutionInterval())));

        return Boolean.TRUE.equals(getWithLaboratorialClasses()) ? hasLaboratorialClasses : !hasLaboratorialClasses;
    }

    @Override
    public boolean checkRules() {
        if (getWithLaboratorialClasses() == null) {
            throw new DomainException(i18n(
                    "org.fenixedu.academictreasury.domain.tuition.conditionRule.withLaboratorialClassesConditionRule.withLaboratorialClasses.cannotBeNull"));
        }
        return true;
    }

    @Override
    public String getDescription() {
        return (Boolean.TRUE.equals(getWithLaboratorialClasses()) ? i18n("label.true") : i18n("label.false"));
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
        WithLaboratorialClassesConditionRule result = new WithLaboratorialClassesConditionRule();
        result.setWithLaboratorialClasses(getWithLaboratorialClasses());
        return result;
    }

    @Override
    public void fillRuleFromImporter(String value) {
        if (value.equals(i18n("label.true"))) {
            setWithLaboratorialClasses(Boolean.TRUE);
            return;
        }

        if (value.equals(i18n("label.false"))) {
            setWithLaboratorialClasses(Boolean.FALSE);
            return;
        }
        throw new IllegalArgumentException();
    }
}
