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
package org.fenixedu.academictreasury.domain.integration.tuitioninfo;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class ERPTuitionInfoType extends ERPTuitionInfoType_Base {

    public static final Comparator<ERPTuitionInfoType> COMPARE_BY_NAME = new Comparator<ERPTuitionInfoType>() {

        @Override
        public int compare(final ERPTuitionInfoType o1, final ERPTuitionInfoType o2) {
            int c = o1.getErpTuitionInfoProduct().getName().compareTo(o2.getErpTuitionInfoProduct().getName());

            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }

    };

    public ERPTuitionInfoType() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setErpTuitionInfoSettings(ERPTuitionInfoSettings.getInstance());
        setActive(true);
    }

    private ERPTuitionInfoType(final ERPTuitionInfoTypeBean bean) {
        this();
        
        if(ERPTuitionInfoSettings.getInstance().isExportationActive()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.exportation.active");
        }

        setExecutionYear(bean.getExecutionYear());
        setErpTuitionInfoProduct(bean.getErpTuitionInfoProduct());
        
        getTuitionProductsSet().addAll(bean.getTuitionProducts());

        if(bean.getTuitionPaymentPlanGroup() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.TuitionPaymentPlanGroup.required.to.infer.academic.info");
        }
        
        addAcademicEntries(bean);
        
        checkRules();
    }

    private void addAcademicEntries(final ERPTuitionInfoTypeBean bean) {
        if(bean.getTuitionPaymentPlanGroup().isForExtracurricular()) {

            ERPTuitionInfoTypeAcademicEntry.createForExtracurricularTuition(this);

        } else if(bean.getTuitionPaymentPlanGroup().isForStandalone()) {

            ERPTuitionInfoTypeAcademicEntry.createForStandaloneTuition(this);

        } else if(bean.getTuitionPaymentPlanGroup().isForRegistration()) {
            
            for (DegreeType degreeType : bean.getDegreeTypes()) {
                ERPTuitionInfoTypeAcademicEntry.createForRegistrationTuition(this, degreeType);
            }
            
            for (Degree degree : bean.getDegrees()) {
                ERPTuitionInfoTypeAcademicEntry.createForRegistrationTuition(this, degree);
            }
            
            for (DegreeCurricularPlan degreeCurricularPlan : bean.getDegreeCurricularPlans()) {
                ERPTuitionInfoTypeAcademicEntry.createForRegistrationTuition(this, degreeCurricularPlan);
            }
        }
    }

    private void checkRules() {

        if (getDomainRoot() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.bennu.required");
        }

        if (getExecutionYear() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.executionYear.required");
        }

        if (getTuitionProductsSet().isEmpty()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.tuitionProducts.required");
        }
        
        if(getErpTuitionInfoProduct() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.erpTuitionProduct.required");
        }
        
        final ExecutionYear executionYear = getExecutionYear();
        if(getErpTuitionInfoProduct().getErpTuitionInfoTypesSet().stream().filter(t -> t.getExecutionYear() == executionYear).count() > 1) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.erpTuitionProduct.already.defined");
        }
        
        if(getErpTuitionInfoTypeAcademicEntriesSet().isEmpty()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.academic.entries.required");
        }
        
        for (final ERPTuitionInfoTypeAcademicEntry entry : getErpTuitionInfoTypeAcademicEntriesSet()) {
            entry.checkRules();
        }
        
    }

    @Atomic
    public void edit(final ERPTuitionInfoTypeBean bean) {
        
        if(ERPTuitionInfoSettings.getInstance().isExportationActive()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.exportation.active");
        }

        getTuitionProductsSet().clear();
        getTuitionProductsSet().addAll(bean.getTuitionProducts());
        
        while(!getErpTuitionInfoTypeAcademicEntriesSet().isEmpty()) {
            getErpTuitionInfoTypeAcademicEntriesSet().iterator().next().delete();
        }
        
        addAcademicEntries(bean);
        
        checkRules();
    }

    public boolean isActive() {
        return getActive();
    }
    
    @Atomic
    public void toogleActive() {
        if(ERPTuitionInfoSettings.getInstance().isExportationActive()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.exportation.active");
        }
        
        setActive(!getActive());
        
        checkRules();
    }
    
    @Atomic
    public void delete() {
        if(ERPTuitionInfoSettings.getInstance().isExportationActive()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.exportation.active");
        }

        if (!getErpTuitionInfosSet().isEmpty()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.delete.not.possible");
        }

        setDomainRoot(null);
        setErpTuitionInfoProduct(null);
        setErpTuitionInfoSettings(null);
        setExecutionYear(null);
        getTuitionProductsSet().clear();

        while(!getErpTuitionInfoTypeAcademicEntriesSet().isEmpty()) {
            getErpTuitionInfoTypeAcademicEntriesSet().iterator().next().delete();
        }
        
        deleteDomainObject();
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<? extends ERPTuitionInfoType> findAll() {
        return ERPTuitionInfoSettings.getInstance().getErpTuitionInfoTypesSet().stream();
    }

    public static Stream<? extends ERPTuitionInfoType> findActive() {
        return findAll().filter(e -> e.isActive());
    }
    
    public static Stream<? extends ERPTuitionInfoType> findForExecutionYear(ExecutionYear executionYear) {
        return findAll().filter(e -> e.getExecutionYear() == executionYear);
    }

    public static Stream<? extends ERPTuitionInfoType> findActiveForExecutionYear(ExecutionYear executionYear) {
        return findForExecutionYear(executionYear).filter(e -> e.isActive());
    }

    public static Stream<? extends ERPTuitionInfoType> findByCode(final String code) {
        return findAll().filter(e -> e.getErpTuitionInfoProduct().getCode().equals(code));
    }

    public static Optional<? extends ERPTuitionInfoType> findUniqueByCode(final String code) {
        return findAll().filter(e -> e.getErpTuitionInfoProduct().getCode().equals(code)).findFirst();
    }

    @Atomic
    public static ERPTuitionInfoType create(final ERPTuitionInfoTypeBean bean) {
        return new ERPTuitionInfoType(bean);
    }

}
