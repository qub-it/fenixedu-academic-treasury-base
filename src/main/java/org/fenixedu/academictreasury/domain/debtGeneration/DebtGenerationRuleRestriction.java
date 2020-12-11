/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * * Neither the name of Quorum Born IT nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 * * Universidade de Lisboa and its respective subsidiary Serviços Centrais da Universidade
 * de Lisboa (Departamento de Informática), hereby referred to as the Beneficiary, is the
 * sole demonstrated end-user and ultimately the only beneficiary of the redistributed binary
 * form and/or source code.
 * * The Beneficiary is entrusted with either the binary form, the source code, or both, and
 * by accepting it, accepts the terms of this License.
 * * Redistribution of any binary form and/or source code is only allowed in the scope of the
 * Universidade de Lisboa FenixEdu(™)’s implementation projects.
 * * This license and conditions of redistribution of source code/binary can only be reviewed
 * by the Steering Comittee of FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT” BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
