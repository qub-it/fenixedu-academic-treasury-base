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
package org.fenixedu.academictreasury.domain.debtGeneration.requests;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academictreasury.domain.emoluments.AcademicTax;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;

public class MassiveDebtGenerationRequestFileBean implements Serializable, ITreasuryBean {

    private static final long serialVersionUID = 1L;

    private MassiveDebtGenerationType massiveDebtGenerationType;
    private LocalDate debtDate;
    private ExecutionYear executionYear;
    private TuitionPaymentPlanGroup tuitionPaymentPlanGroup;
    private AcademicTax academicTax;
    private String reason;
    private FinantialInstitution finantialInstitution;

    private List<TreasuryTupleDataSourceBean> massiveDebtGenerationTypeDataSource;
    private List<TreasuryTupleDataSourceBean> executionYearDataSource;
    private List<TreasuryTupleDataSourceBean> academicTaxesDataSource;
    private List<TreasuryTupleDataSourceBean> finantialInstitutionDataSource;

    private boolean forAcademicTax = false;

    private boolean executionYearRequired;
    private boolean academicTaxRequired;
    private boolean debtDateRequired;
    private boolean reasonRequired;
    private boolean finantialInstitutionRequired;

    public MassiveDebtGenerationRequestFileBean() {
        this.debtDate = new LocalDate();

        if (FinantialInstitution.findAll().count() == 1) {
            this.finantialInstitution = FinantialInstitution.findAll().findFirst().get();
        }

        updateData();
    }

    public MassiveDebtGenerationRequestFileBean(final MassiveDebtGenerationRequestFile file) {
        setMassiveDebtGenerationType(file.getMassiveDebtGenerationType());
        setTuitionPaymentPlanGroup(file.getTuitionPaymentPlanGroup());
        setAcademicTax(file.getAcademicTax());
        setExecutionYear(file.getExecutionYear());
        setDebtDate(file.getDebtDate());
        setReason(file.getReason());
        setFinantialInstitution(file.getFinantialInstitution());
    }

    @Atomic
    public void updateData() {
        getMassiveDebtGenerationTypeDataSource();
        getExecutionYearDataSource();
        getAcademicTaxesDataSource();
        getMassiveDebtGenerationTypeDataSource();
        getFinantialInstitutionDataSource();

        if (isForAcademicTax()) {
            this.tuitionPaymentPlanGroup = null;
        } else {
            this.tuitionPaymentPlanGroup = TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get();
            this.academicTax = null;
        }

        setDebtDateRequired(getMassiveDebtGenerationType() != null ? getMassiveDebtGenerationType().isDebtDateRequired() : false);

        setAcademicTaxRequired(
                getMassiveDebtGenerationType() != null ? getMassiveDebtGenerationType().isForAcademicTaxRequired() : false);

        setExecutionYearRequired(
                getMassiveDebtGenerationType() != null ? getMassiveDebtGenerationType().isExecutionRequired() : false);

        setReasonRequired(getMassiveDebtGenerationType() != null ? getMassiveDebtGenerationType().isReasonRequired() : false);

        setFinantialInstitutionRequired(
                getMassiveDebtGenerationType() != null ? getMassiveDebtGenerationType().isFinantialInstitutionRequired() : false);
    }

    public List<TreasuryTupleDataSourceBean> getMassiveDebtGenerationTypeDataSource() {
        return massiveDebtGenerationTypeDataSource = MassiveDebtGenerationType.findAllActive()
                .map(e -> new TreasuryTupleDataSourceBean(e.getExternalId(), e.getName())).collect(Collectors.toList());
    }

    public List<TreasuryTupleDataSourceBean> getExecutionYearDataSource() {
        executionYearDataSource = possibleExecutionYears().stream()
                .map(e -> new TreasuryTupleDataSourceBean(e.getExternalId(), e.getQualifiedName())).collect(Collectors.toList());

        return executionYearDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getAcademicTaxesDataSource() {
        academicTaxesDataSource =
                AcademicTax.findAll().map(e -> new TreasuryTupleDataSourceBean(e.getExternalId(), e.getProduct().getName().getContent()))
                        .collect(Collectors.toList());

        return academicTaxesDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getFinantialInstitutionDataSource() {
        finantialInstitutionDataSource = FinantialInstitution.findAll()
                .map(e -> new TreasuryTupleDataSourceBean(e.getExternalId(), e.getName())).collect(Collectors.toList());
        
        return finantialInstitutionDataSource;
    }

    private List<ExecutionYear> possibleExecutionYears() {
        final List<ExecutionYear> executionYears = ExecutionYear.readNotClosedExecutionYears().stream()
                .sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR).collect(Collectors.toList());

        return executionYears;
    }

    public boolean isStandaloneTuition() {
        return this.tuitionPaymentPlanGroup != null && this.tuitionPaymentPlanGroup.isForStandalone();
    }

    public boolean isExtracurricularTuition() {
        return this.tuitionPaymentPlanGroup != null && this.tuitionPaymentPlanGroup.isForExtracurricular();
    }

    public boolean isRegistrationTuition() {
        return this.tuitionPaymentPlanGroup != null && this.tuitionPaymentPlanGroup.isForRegistration();
    }

    // @formatter:off
    /* -----------------
     * GETTERS & SETTERS
     * -----------------
     */
    // @formatter:on

    public boolean isForAcademicTax() {
        return forAcademicTax;
    }

    public void setForAcademicTax(boolean forAcademicTax) {
        this.forAcademicTax = forAcademicTax;
    }

    public TuitionPaymentPlanGroup getTuitionPaymentPlanGroup() {
        return tuitionPaymentPlanGroup;
    }

    public void setTuitionPaymentPlanGroup(TuitionPaymentPlanGroup tuitionPaymentPlanGroup) {
        this.tuitionPaymentPlanGroup = tuitionPaymentPlanGroup;
    }

    public AcademicTax getAcademicTax() {
        return academicTax;
    }

    public void setAcademicTax(AcademicTax academicTax) {
        this.academicTax = academicTax;
    }

    public LocalDate getDebtDate() {
        return debtDate;
    }

    public void setDebtDate(LocalDate debtDate) {
        this.debtDate = debtDate;
    }

    public ExecutionYear getExecutionYear() {
        return executionYear;
    }

    public void setExecutionYear(ExecutionYear executionYear) {
        this.executionYear = executionYear;
    }

    public MassiveDebtGenerationType getMassiveDebtGenerationType() {
        return massiveDebtGenerationType;
    }

    public void setMassiveDebtGenerationType(MassiveDebtGenerationType massiveDebtGenerationType) {
        this.massiveDebtGenerationType = massiveDebtGenerationType;
    }

    public boolean isDebtDateRequired() {
        return debtDateRequired;
    }

    public void setDebtDateRequired(boolean debtDateRequired) {
        this.debtDateRequired = debtDateRequired;
    }

    public boolean isExecutionYearRequired() {
        return executionYearRequired;
    }

    public void setExecutionYearRequired(boolean executionYearRequired) {
        this.executionYearRequired = executionYearRequired;
    }

    public void setExecutionYearDataSource(List<TreasuryTupleDataSourceBean> executionYearDataSource) {
        this.executionYearDataSource = executionYearDataSource;
    }

    public boolean isAcademicTaxRequired() {
        return this.academicTaxRequired;
    }

    public void setAcademicTaxRequired(boolean forAcademicTaxRequired) {
        this.academicTaxRequired = forAcademicTaxRequired;
    }

    public boolean isReasonRequired() {
        return reasonRequired;
    }

    public void setReasonRequired(boolean reasonRequired) {
        this.reasonRequired = reasonRequired;
    }

    public boolean isFinantialInstitutionRequired() {
        return finantialInstitutionRequired;
    }

    public void setFinantialInstitutionRequired(boolean finantialInstitutionRequired) {
        this.finantialInstitutionRequired = finantialInstitutionRequired;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public FinantialInstitution getFinantialInstitution() {
        return finantialInstitution;
    }

    public void setFinantialInstitution(FinantialInstitution finantialInstitution) {
        this.finantialInstitution = finantialInstitution;
    }
}
