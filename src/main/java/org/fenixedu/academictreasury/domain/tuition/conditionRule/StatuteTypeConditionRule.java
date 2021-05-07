package org.fenixedu.academictreasury.domain.tuition.conditionRule;

import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionAnnotation;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionRule;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;

@TuitionConditionAnnotation(StatuteTypeConditionRule.BUNDLE_NAME)
public class StatuteTypeConditionRule extends StatuteTypeConditionRule_Base {

    public static final String BUNDLE_NAME = AcademicTreasuryConstants.BUNDLE;

    public StatuteTypeConditionRule() {
        super();
    }

    @Override
    public boolean containsRule(TuitionConditionRule tuitionConditionRule) {
        if (!(tuitionConditionRule instanceof StatuteTypeConditionRule)) {
            return false;
        }
        StatuteTypeConditionRule rule = (StatuteTypeConditionRule) tuitionConditionRule;
        return getStatuteTypeSet().containsAll(rule.getStatuteTypeSet());
    }

    @Override
    public boolean isValidTo(Registration registration, ExecutionYear executionYear, Enrolment enrolment) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        Set<StatuteType> statutesTypesValidOnAnyExecutionSemesterFor =
                academicTreasuryServices.statutesTypesValidOnAnyExecutionSemesterFor(registration, executionYear);

        return getStatuteTypeSet().stream().anyMatch(statute -> statutesTypesValidOnAnyExecutionSemesterFor.contains(statute));
    }

    @Override
    public boolean checkRules() {
        if (getStatuteTypeSet() == null || getStatuteTypeSet().isEmpty()) {
            throw new DomainException(i18n(
                    "org.fenixedu.academictreasury.domain.tuition.conditionRule.StatuteTypeConditionRule.statuteTypeSet.cannotBeEmpty"));
        }
        return true;
    }

    @Override
    public String getDescription() {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        return getStatuteTypeSet().stream().sorted(StatuteType.COMPARATOR_BY_NAME)
                .map(c -> academicTreasuryServices.localizedNameOfStatuteType(c)).collect(Collectors.joining(", "));
    }

    @Override
    protected String getBundle() {
        return BUNDLE_NAME;
    }

    @Override
    public void delete() {
        getStatuteTypeSet().clear();
        setTuitionPaymentPlan(null);
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Override
    public TuitionConditionRule copyToPlan(TuitionPaymentPlan tuitionPaymentPlan) {
        StatuteTypeConditionRule result = new StatuteTypeConditionRule();
        result.setTuitionPaymentPlan(tuitionPaymentPlan);
        getStatuteTypeSet().forEach(c -> result.addStatuteType(c));
        return result;
    }

    @Override
    public void fillRuleFromImporter(String string) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        String[] split = string.split("\\|");
        for (String s : split) {
            StatuteType value = academicTreasuryServices.readAllStatuteTypesSet().stream()
                    .filter(statute -> academicTreasuryServices.localizedNameOfStatuteType(statute).equals(s)).findFirst()
                    .orElse(null);
            if (value == null) {
                throw new IllegalArgumentException();
            }
            addStatuteType(value);
        }
    }
}
