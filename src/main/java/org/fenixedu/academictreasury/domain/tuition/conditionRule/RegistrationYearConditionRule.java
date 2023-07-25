package org.fenixedu.academictreasury.domain.tuition.conditionRule;

import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionAnnotation;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionRule;
import org.fenixedu.academictreasury.dto.tariff.TuitionPaymentPlanBean;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;

@TuitionConditionAnnotation(RegistrationYearConditionRule.BUNDLE_NAME)
public class RegistrationYearConditionRule extends RegistrationYearConditionRule_Base {

    public static final String BUNDLE_NAME = AcademicTreasuryConstants.BUNDLE;

    public RegistrationYearConditionRule() {
        super();
    }

    @Override
    public boolean containsRule(TuitionConditionRule tuitionConditionRule) {
        if (!(tuitionConditionRule instanceof RegistrationYearConditionRule)) {
            return false;
        }

        RegistrationYearConditionRule rule = (RegistrationYearConditionRule) tuitionConditionRule;
        return getExecutionIntervalsSet().containsAll(rule.getExecutionIntervalsSet());
    }

    @Override
    public boolean isValidTo(Registration registration, ExecutionYear executionYear, Enrolment enrolment) {
        return getExecutionIntervalsSet().contains(registration.getRegistrationYear());
    }

    @Override
    public boolean checkRules() {
        if (getExecutionIntervalsSet() == null || getExecutionIntervalsSet().isEmpty()) {
            throw new IllegalStateException(
                    "org.fenixedu.academictreasury.domain.tuition.conditionRule.RegistrationYearConditionRule.executionIntervalsSet.cannotBeEmpty");
        }

        return true;
    }

    @Override
    public String getDescription() {
        return getExecutionIntervalsSet().stream().sorted(ExecutionInterval.COMPARATOR_BY_BEGIN_DATE)
                .map(ExecutionInterval::getQualifiedName).collect(Collectors.joining(", "));
    }

    @Override
    protected String getBundle() {
        return BUNDLE_NAME;
    }

    @Override
    public void delete() {
        getExecutionIntervalsSet().clear();
        setTuitionPaymentPlan(null);
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Override
    public TuitionConditionRule duplicate() {
        RegistrationYearConditionRule result = new RegistrationYearConditionRule();
        getExecutionIntervalsSet().addAll(getExecutionIntervalsSet());
        return result;
    }
    
    @Override
    public void fillRuleFromImporter(TuitionPaymentPlanBean bean) {
        String string = bean.getImporterRules().get(this.getClass());
        String[] split = string.split("\\|");
        
        for (String s : split) {
            ExecutionYear executionYear = ExecutionYear.readExecutionYearByName(s);

            if (executionYear == null) {
                throw new AcademicTreasuryDomainException("error.RegistrationYearConditionRule.executionInterval.invalid", s);
            }
            
            addExecutionIntervals(executionYear);
        }
    }
    
}
