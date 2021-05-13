package org.fenixedu.academictreasury.domain.tuition.conditionRule;

import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionAnnotation;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionRule;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;

@TuitionConditionAnnotation(ExecutionIntervalConditionRule.BUNDLE_NAME)
public class ExecutionIntervalConditionRule extends ExecutionIntervalConditionRule_Base {

    public static final String BUNDLE_NAME = AcademicTreasuryConstants.BUNDLE;

    public ExecutionIntervalConditionRule() {
        super();
    }

    @Override
    public boolean containsRule(TuitionConditionRule tuitionConditionRule) {
        if (!(tuitionConditionRule instanceof ExecutionIntervalConditionRule)) {
            return false;
        }
        ExecutionIntervalConditionRule rule = (ExecutionIntervalConditionRule) tuitionConditionRule;
        return getExecutionIntervalSet().containsAll(rule.getExecutionIntervalSet());
    }

    @Override
    public boolean isValidTo(Registration registration, ExecutionYear executionYear, Enrolment enrolment) {
        Set<ExecutionInterval> collect =
                registration.getEnrolments(executionYear).stream().map(e -> e.getExecutionInterval()).collect(Collectors.toSet());
        if (collect.size() != 1) {
            return false;
        }
        return getExecutionIntervalSet().contains(collect.iterator().next());
    }

    @Override
    public boolean checkRules() {
        if (getExecutionIntervalSet() == null || getExecutionIntervalSet().isEmpty()) {
            throw new DomainException(i18n(
                    "org.fenixedu.academictreasury.domain.tuition.conditionRule.executionIntervalConditionRule.executionIntervalSet.cannotBeEmpty"));
        }
        return true;
    }

    @Override
    public String getDescription() {
        return getExecutionIntervalSet().stream().sorted(ExecutionInterval.COMPARATOR_BY_BEGIN_DATE).map(c -> c.getName())
                .collect(Collectors.joining(", "));
    }

    @Override
    protected String getBundle() {
        return BUNDLE_NAME;
    }

    @Override
    public void delete() {
        getExecutionIntervalSet().clear();
        setTuitionPaymentPlan(null);
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Override
    public TuitionConditionRule copyToPlan(TuitionPaymentPlan tuitionPaymentPlan) {
        ExecutionIntervalConditionRule result = new ExecutionIntervalConditionRule();
        result.setTuitionPaymentPlan(tuitionPaymentPlan);
        getExecutionIntervalSet().forEach(c -> {
            ExecutionInterval executionInterval =
                    tuitionPaymentPlan.getExecutionYear().getChildInterval(c.getChildOrder(), c.getAcademicPeriod());
            if (executionInterval != null) {
                result.addExecutionInterval(executionInterval);
            }
        });
        return result;
    }

    @Override
    public TuitionConditionRule duplicate() {
        ExecutionIntervalConditionRule result = new ExecutionIntervalConditionRule();
        getExecutionIntervalSet().forEach(c -> result.addExecutionInterval(c));
        return result;
    }

    @Override
    public void fillRuleFromImporter(String string) {
        String[] split = string.split("\\|");
        for (String s : split) {
            ExecutionInterval value = ExecutionInterval.getExecutionInterval(s);
            if (value == null) {
                throw new IllegalArgumentException();
            }
            addExecutionInterval(value);
        }
    }

}
