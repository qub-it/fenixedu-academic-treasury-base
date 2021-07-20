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

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import pt.ist.fenixframework.FenixFramework;

import com.google.common.base.Strings;

public class AcademicDebtGenerationRuleType extends AcademicDebtGenerationRuleType_Base {
    
    public static Comparator<AcademicDebtGenerationRuleType> COMPARE_BY_ORDER_NUMBER = new Comparator<AcademicDebtGenerationRuleType>() {

        @Override
        public int compare(final AcademicDebtGenerationRuleType o1, final AcademicDebtGenerationRuleType o2) {
            int c = Integer.compare(o1.getOrderNumber(), o2.getOrderNumber());
            
            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
        
    };
    
    protected AcademicDebtGenerationRuleType() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }
    
    public AcademicDebtGenerationRuleType(final String code, final String name, final String strategyImplementation, final int orderNumber) {
        this();
        setCode(code);
        setName(name);
        setStrategyImplementation(strategyImplementation);
        setOrderNumber(orderNumber);
        
        checkRules();
    }
    
    private void checkRules() {
        if(Strings.isNullOrEmpty(getCode())) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRuleType.code.required");
        }
        
        if(findByCodeIgnoresCase(getCode()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRuleType.code.already.exists");
        }

        if(Strings.isNullOrEmpty(getName())) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRuleType.name.required");
        }
        
        strategyImplementation();
        
        if(findAll().filter(i -> i.getOrderNumber() == getOrderNumber()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRuleType.orderNumber.already.exists");
        }
        
        if(findAll().filter(l -> l.getStrategyImplementation().equals(getStrategyImplementation())).count() > 1) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRuleType.strategyImplementation.already.exists");
        }
    }

    public IAcademicDebtGenerationRuleStrategy strategyImplementation() {
        try {
            return (IAcademicDebtGenerationRuleStrategy) Class.forName(getStrategyImplementation()).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on
    
    public static Stream<AcademicDebtGenerationRuleType> findAll() {
        return FenixFramework.getDomainRoot().getAcademicDebtGenerationRuleTypesSet().stream();
    }
    
    public static Optional<AcademicDebtGenerationRuleType> findByCode(final String code) {
        return findAll().filter(l -> l.getCode().equals(code)).findAny();
    }
    
    private static Stream<AcademicDebtGenerationRuleType> findByCodeIgnoresCase(final String code) {
        return findAll().filter(l -> l.getCode().toLowerCase().equals(code.toLowerCase()));
    }
    
    public static AcademicDebtGenerationRuleType create(final String code, final String name, final String strategyImplementation, final int orderNumber) {
        return new AcademicDebtGenerationRuleType(code, name, strategyImplementation, orderNumber);
    }

}
