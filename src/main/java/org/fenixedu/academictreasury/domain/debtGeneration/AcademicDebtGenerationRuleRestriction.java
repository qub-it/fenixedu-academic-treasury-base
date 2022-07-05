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

    /* TODO: Transform this method as abstract method
     * 
     * For now this method throws an exception instead of being abstract, due to many subclasses in various modules.
     * To not risk errors due to unimplemented method, throw an exception until all subclasses of various modules are covered
     */
    public String exportDataAsJson() {
        throw new UnsupportedOperationException("This operation must be implemented by subclass: " + getClass().getName());
    }
    
    /* TODO: Transform this method as abstract method
     * 
     * For now this method throws an exception instead of being abstract, due to many subclasses in various modules.
     * To not risk errors due to unimplemented method, throw an exception until all subclasses of various modules are covered
     */
    public void fillDataFromJsonSerializedObject(String jsonSerializedObject) {
        throw new UnsupportedOperationException("This operation must be implemented by subclass" + getClass().getName());
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
