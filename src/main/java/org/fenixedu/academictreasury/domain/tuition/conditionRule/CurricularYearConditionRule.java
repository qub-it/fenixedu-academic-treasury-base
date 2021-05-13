package org.fenixedu.academictreasury.domain.tuition.conditionRule;

import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionAnnotation;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionRule;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;

@TuitionConditionAnnotation(CurricularYearConditionRule.BUNDLE_NAME)
public class CurricularYearConditionRule extends CurricularYearConditionRule_Base {

    public static final String BUNDLE_NAME = AcademicTreasuryConstants.BUNDLE;

    public CurricularYearConditionRule() {
        super();
    }

    @Override
    public boolean containsRule(TuitionConditionRule tuitionConditionRule) {
        if (!(tuitionConditionRule instanceof CurricularYearConditionRule)) {
            return false;
        }
        CurricularYearConditionRule rule = (CurricularYearConditionRule) tuitionConditionRule;
        return getCurricularYearSet().containsAll(rule.getCurricularYearSet());
    }

    @Override
    public boolean isValidTo(Registration registration, ExecutionYear executionYear, Enrolment enrolment) {
        return getCurricularYearSet().contains(CurricularYear.readByYear(registration.getCurricularYear(executionYear)));
    }

    @Override
    public boolean checkRules() {
        if (getCurricularYearSet() == null || getCurricularYearSet().isEmpty()) {
            throw new DomainException(i18n(
                    "org.fenixedu.academictreasury.domain.tuition.conditionRule.curricularYearConditionRule.curricularYearSet.cannotBeEmpty"));
        }
        return true;
    }

    @Override
    protected String getBundle() {
        return BUNDLE_NAME;
    }

    @Override
    public String getDescription() {
        return getCurricularYearSet().stream().sorted(CurricularYear.CURRICULAR_YEAR_COMPARATORY_BY_YEAR)
                .map(c -> c.getYear().toString()).collect(Collectors.joining(", "));
    }

    @Override
    public void delete() {
        getCurricularYearSet().clear();
        setTuitionPaymentPlan(null);
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Override
    public TuitionConditionRule duplicate() {
        CurricularYearConditionRule result = new CurricularYearConditionRule();
        getCurricularYearSet().forEach(c -> result.addCurricularYear(c));
        return result;
    }

    @Override
    public void fillRuleFromImporter(String string) {
        String[] split = string.split("\\|");
        final IAcademicTreasuryPlatformDependentServices services =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        for (String s : split) {
            CurricularYear readByYear = services.readAllCurricularYearsSet().stream()
                    .filter(c -> c.getYear().toString().equals(s)).findFirst().orElse(null); //CurricularYear.readByYear(Integer.getInteger(s));
            if (readByYear == null) {
                throw new IllegalArgumentException();
            }
            addCurricularYear(readByYear);
        }
    }

}
