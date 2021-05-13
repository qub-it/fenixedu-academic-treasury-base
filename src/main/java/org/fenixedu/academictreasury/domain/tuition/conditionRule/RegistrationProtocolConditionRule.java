package org.fenixedu.academictreasury.domain.tuition.conditionRule;

import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionAnnotation;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionRule;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;

@TuitionConditionAnnotation(RegistrationProtocolConditionRule.BUNDLE_NAME)
public class RegistrationProtocolConditionRule extends RegistrationProtocolConditionRule_Base {

    public static final String BUNDLE_NAME = AcademicTreasuryConstants.BUNDLE;

    public RegistrationProtocolConditionRule() {
        super();
    }

    @Override
    public boolean containsRule(TuitionConditionRule tuitionConditionRule) {
        if (!(tuitionConditionRule instanceof RegistrationProtocolConditionRule)) {
            return false;
        }
        RegistrationProtocolConditionRule rule = (RegistrationProtocolConditionRule) tuitionConditionRule;
        return getRegistrationProtocolSet().containsAll(rule.getRegistrationProtocolSet());
    }

    @Override
    public boolean isValidTo(Registration registration, ExecutionYear executionYear, Enrolment enrolment) {
        return getRegistrationProtocolSet().contains(registration.getRegistrationProtocol());
    }

    @Override
    public boolean checkRules() {
        if (getRegistrationProtocolSet() == null || getRegistrationProtocolSet().isEmpty()) {
            throw new DomainException(i18n(
                    "org.fenixedu.academictreasury.domain.tuition.conditionRule.RegistrationProtocolConditionRule.registrationProtocolSet.cannotBeEmpty"));
        }
        return true;
    }

    @Override
    public String getDescription() {
        return getRegistrationProtocolSet().stream().sorted((o1, o2) -> o1.compareTo(o2))
                .map(c -> c.getDescription().getContent()).collect(Collectors.joining(", "));
    }

    @Override
    protected String getBundle() {
        return BUNDLE_NAME;
    }

    @Override
    public void delete() {
        getRegistrationProtocolSet().clear();
        setTuitionPaymentPlan(null);
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Override
    public TuitionConditionRule duplicate() {
        RegistrationProtocolConditionRule result = new RegistrationProtocolConditionRule();
        getRegistrationProtocolSet().forEach(c -> result.addRegistrationProtocol(c));
        return result;
    }

    @Override
    public void fillRuleFromImporter(String string) {
        String[] split = string.split("\\|");
        for (String s : split) {
            RegistrationProtocol value =
                    RegistrationProtocol.findByPredicate(i -> i.getDescription().getContent().equals(s)).findFirst().orElse(null);
            if (value == null) {
                throw new IllegalArgumentException();
            }
            addRegistrationProtocol(value);
        }
    }
}
