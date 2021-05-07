package org.fenixedu.academictreasury.domain.tuition.conditionRule;

import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionAnnotation;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionRule;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;

@TuitionConditionAnnotation(IngressionTypeConditionRule.BUNDLE_NAME)
public class IngressionTypeConditionRule extends IngressionTypeConditionRule_Base {

    public static final String BUNDLE_NAME = AcademicTreasuryConstants.BUNDLE;

    public IngressionTypeConditionRule() {
        super();
    }

    @Override
    public boolean containsRule(TuitionConditionRule tuitionConditionRule) {
        if (!(tuitionConditionRule instanceof IngressionTypeConditionRule)) {
            return false;
        }
        IngressionTypeConditionRule rule = (IngressionTypeConditionRule) tuitionConditionRule;
        return getIngressionSet().containsAll(rule.getIngressionSet());
    }

    @Override
    public boolean isValidTo(Registration registration, ExecutionYear executionYear, Enrolment enrolment) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        IngressionType ingression = academicTreasuryServices.ingression(registration);
        return getIngressionSet().contains(ingression);
    }

    @Override
    public boolean checkRules() {
        if (getIngressionSet() == null || getIngressionSet().isEmpty()) {
            throw new DomainException(i18n(
                    "org.fenixedu.academictreasury.domain.tuition.conditionRule.IngressionTypeConditionRule.ingressionSet.cannotBeEmpty"));
        }
        return true;
    }

    @Override
    public String getDescription() {
        return getIngressionSet().stream().sorted((o1, o2) -> o1.getCode().compareTo(o2.getCode())).map(c -> c.getLocalizedName())
                .collect(Collectors.joining(", "));
    }

    @Override
    protected String getBundle() {
        return BUNDLE_NAME;
    }

    @Override
    public void delete() {
        getIngressionSet().clear();
        setTuitionPaymentPlan(null);
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Override
    public TuitionConditionRule copyToPlan(TuitionPaymentPlan tuitionPaymentPlan) {
        IngressionTypeConditionRule result = new IngressionTypeConditionRule();
        result.setTuitionPaymentPlan(tuitionPaymentPlan);
        getIngressionSet().forEach(c -> result.addIngression(c));
        return result;
    }

    @Override
    public void fillRuleFromImporter(String string) {
        String[] split = string.split("\\|");
        for (String s : split) {
            IngressionType value =
                    IngressionType.findAllByPredicate(i -> i.getLocalizedName().equals(s)).findFirst().orElse(null);
            if (value == null) {
                throw new IllegalArgumentException();
            }
            addIngression(value);
        }
    }

}
