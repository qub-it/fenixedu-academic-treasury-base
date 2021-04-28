package org.fenixedu.academictreasury.domain.tuition;

import java.util.Comparator;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import pt.ist.fenixframework.FenixFramework;

public abstract class TuitionConditionRule extends TuitionConditionRule_Base {

    public static final Comparator<TuitionConditionRule> COMPARE_BY_CONDITION_RULE_NAME = (o1, o2) -> {
        return getPresentationName(o1.getClass()).compareTo(getPresentationName(o2.getClass()));
    };

    public TuitionConditionRule() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public abstract boolean containsRule(TuitionConditionRule tuitionConditionRule);

    public boolean isValidTo(final Registration registration, final ExecutionYear executionYear) {
        return isValidTo(registration, executionYear, null);
    }

    public abstract boolean checkRules();

    public String getDescription() {
        throw new IllegalArgumentException("description not implemented");
    }

    public abstract void delete();

    public abstract boolean isValidTo(final Registration registration, final ExecutionYear executionYear,
            final Enrolment enrolment);

    protected abstract String getBundle();

    public static String getPresentationName(Class<? extends TuitionConditionRule> tuitionConditionRule) {
        return TreasuryPlataformDependentServicesFactory.implementation().bundle(
                tuitionConditionRule.getAnnotation(TuitionConditionAnnotation.class).value(), tuitionConditionRule.getName());
    }

    public String i18n(String key, String... args) {
        return TreasuryPlataformDependentServicesFactory.implementation().bundle(getBundle(), key, args);
    }

    protected abstract TuitionConditionRule copyToPlan(TuitionPaymentPlan tuitionPaymentPlan);

}
