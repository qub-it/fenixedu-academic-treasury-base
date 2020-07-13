package org.fenixedu.academictreasury.domain.debtGeneration;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundleI18N;

import java.util.Collections;
import java.util.List;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.commons.i18n.LocalizedString;

public class FirstTimeFirstYearRestriction extends FirstTimeFirstYearRestriction_Base {
    
    public FirstTimeFirstYearRestriction() {
        super();
    }
    
    protected FirstTimeFirstYearRestriction(AcademicDebtGenerationRule rule, boolean excludeIfMatches) {
        this();
        super.init(rule, excludeIfMatches);
    }
    
    @Override
    public LocalizedString getName() {
        return RESTRICTION_NAME();
    }

    @Override
    public List<LocalizedString> getParametersDescriptions() {
        return Collections.emptyList();
    }
    
    private boolean evaluateResult(final Registration registration) {
        return TuitionPaymentPlan.firstTimeStudent(registration, getAcademicDebtGenerationRule().getExecutionYear());
    }
    
    @Override
    public boolean test(Registration registration) {
        return isToInclude() ? evaluateResult(registration) : !evaluateResult(registration);
    }

    @Override
    public FirstTimeFirstYearRestriction makeCopy(AcademicDebtGenerationRule ruleToCreate) {
        return create(ruleToCreate, getExcludeIfMatches());
    }
    
    /*
     * ********
     * SERVICES
     * ********
     */

    public static FirstTimeFirstYearRestriction create(AcademicDebtGenerationRule rule, boolean excludeIfMatches) {
        return new FirstTimeFirstYearRestriction(rule, excludeIfMatches);
    }

    public static LocalizedString RESTRICTION_NAME() {
        return academicTreasuryBundleI18N("label.FirstTimeFirstYearRestriction.restrictionName");
    }
    
}
