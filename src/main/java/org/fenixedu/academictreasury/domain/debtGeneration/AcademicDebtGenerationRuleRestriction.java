package org.fenixedu.academictreasury.domain.debtGeneration;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.FenixFramework;

public abstract class AcademicDebtGenerationRuleRestriction extends AcademicDebtGenerationRuleRestriction_Base implements Predicate<Registration> {
    
    public AcademicDebtGenerationRuleRestriction() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }
    
    protected AcademicDebtGenerationRuleRestriction(AcademicDebtGenerationRule rule) {
        this();
        
        init(rule);
    }

    protected void init(AcademicDebtGenerationRule rule) {
        super.setAcademicDebtGenerationRule(rule);
        
        checkRules();
    }

    private void checkRules() {
        if(getDomainRoot() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRuleRestriction.domainRoot.required");
        }
        
        if(getAcademicDebtGenerationRule() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRuleRestriction.academicDebtGenerationRule.required");
        }
    }
    
    public abstract LocalizedString getName();
    
    public abstract List<LocalizedString> getParametersDescriptions();
    
    public abstract AcademicDebtGenerationRuleRestriction makeCopy(AcademicDebtGenerationRule academicDebtGenerationRule);
    
    public void delete() {
        setDomainRoot(null);
        setAcademicDebtGenerationRule(null);
        
        super.deleteDomainObject();
    }
    
    /*
     * ********
     * SERVICES
     * ********
     */
    
    public static Stream<AcademicDebtGenerationRuleRestriction> findAll() {
        return FenixFramework.getDomainRoot().getAcademicDebtGenerationRuleRestrictionsSet().stream();
    }
    
    public static Stream<AcademicDebtGenerationRuleRestriction> find(AcademicDebtGenerationRule rule) {
        return rule.getAcademicDebtGenerationRuleRestrictionsSet().stream();
    }
    
}
