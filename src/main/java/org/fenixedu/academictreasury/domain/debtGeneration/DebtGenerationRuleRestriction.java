package org.fenixedu.academictreasury.domain.debtGeneration;

import java.util.List;
import java.util.stream.Stream;

import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class DebtGenerationRuleRestriction extends DebtGenerationRuleRestriction_Base {
    
    public DebtGenerationRuleRestriction() {
        super();

        setDomainRoot(FenixFramework.getDomainRoot());
    }
    
    protected DebtGenerationRuleRestriction(final String name, final String strategyImplementation) {
        this();
        
        setName(name);
        setStrategyImplementation(strategyImplementation);
    }
    
    public IDebtGenerationRuleRestrictionStrategy strategyImplementation() {
        try {
            final Class<IDebtGenerationRuleRestrictionStrategy> clazz = (Class<IDebtGenerationRuleRestrictionStrategy>) Class.forName(getStrategyImplementation());
            final IDebtGenerationRuleRestrictionStrategy strategy = clazz.newInstance();

            return strategy;
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void delete() {
        if(!getAcademicDebtGenerationRulesSet().isEmpty()) {
            throw new AcademicTreasuryDomainException("error.DebtGenerationRuleRestriction.academicDebtGenerationRulesSet.not.empty");
        }
        
        setDomainRoot(null);
        
        deleteDomainObject();
    }
    
    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<DebtGenerationRuleRestriction> findAll() {
        return FenixFramework.getDomainRoot().getDebtGenerationRuleRestrictionsSet().stream();
    }
    
    public static Stream<DebtGenerationRuleRestriction> findByStrategyImplementation(final String strategyImplementation) {
        return findAll().filter(r -> strategyImplementation.equals(r.getStrategyImplementation()));
    }
    
    public static DebtGenerationRuleRestriction create(final String name, final String strategyImplementation) {
        return new DebtGenerationRuleRestriction(name, strategyImplementation);
    }

}
