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
package org.fenixedu.academictreasury.dto.academictax;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.emoluments.AcademicTax;
import org.fenixedu.academictreasury.services.AcademicTaxServices;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.services.TuitionServices;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class AcademicTaxDebtCreationBean implements Serializable, ITreasuryBean {

    private static final long serialVersionUID = 1L;

    private LocalDate debtDate;
    private ExecutionYear executionYear;
    private Registration registration;
    private FinantialEntity finantialEntity;
    private EnrolmentEvaluation improvementEvaluation;

    private DebtAccount debtAccount;

    private List<TreasuryTupleDataSourceBean> registrationDataSource;
    private List<TreasuryTupleDataSourceBean> executionYearDataSource;
    private List<TreasuryTupleDataSourceBean> academicTaxesDataSource;
    private List<TreasuryTupleDataSourceBean> improvementEnrolmentEvaluationsDataSource;

    private AcademicTax academicTax;

    private boolean improvementTaxSelected;

    private String errorMessage;

    public AcademicTaxDebtCreationBean(final DebtAccount debtAccount) {
        this.debtAccount = debtAccount;

        updateData();
    }

    @Atomic
    public void updateData() {
        if (executionYear != null && (registration == null || !possibleExecutionYears().contains(executionYear))) {
            executionYear = null;
            improvementEvaluation = null;
        }

        getAcademicTaxesDataSource();
        getRegistrationDataSource();
        getExecutionYearDataSource();
        getImprovementEnrolmentEvaluationsDataSource();

        isImprovementTaxSelected();

        IAcademicTreasuryPlatformDependentServices academicTreasuryServices = AcademicTreasuryPlataformDependentServicesFactory.implementation();

        if (registration != null && executionYear != null) {
            final RegistrationDataByExecutionYear findRegistrationDataByExecutionYear = academicTreasuryServices.findRegistrationDataByExecutionYear(registration, executionYear);

            debtDate = findRegistrationDataByExecutionYear != null ? findRegistrationDataByExecutionYear.getEnrolmentDate() : new LocalDate();
        } else {
            debtDate = new LocalDate();
        }
        
        getErrorMessage();
    }

    public List<TreasuryTupleDataSourceBean> getExecutionYearDataSource() {
        executionYearDataSource = Lists.newArrayList();

        for (final ExecutionYear executionYear : possibleExecutionYears()) {
            final String id = executionYear.getExternalId();
            String text = executionYear.getQualifiedName();

            if (registration != null) {
                final Set<?> enrolments = isImprovementTax() ? TuitionServices.improvementEnrolments(registration,
                        executionYear) : TuitionServices.normalEnrolmentsIncludingAnnuled(registration, executionYear);

                if (enrolments.size() == 1) {
                    text += " " + academicTreasuryBundle("label.AcademicTaxDebtCreationBean.enrolments.one");
                } else if (enrolments.size() > 1) {
                    text += " " + academicTreasuryBundle("label.AcademicTaxDebtCreationBean.enrolments", String.valueOf(enrolments.size()));
                }
            }

            executionYearDataSource.add(new TreasuryTupleDataSourceBean(id, text));
        }

        return executionYearDataSource;
    }

    private List<ExecutionYear> possibleExecutionYears() {
        if (academicTax == null || registration == null) {
            return Lists.newArrayList();
        }

        return Sets.newHashSet(ExecutionYear.readNotClosedExecutionYears()).stream()
                .sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR).collect(Collectors.toList());
    }

    public List<TreasuryTupleDataSourceBean> getRegistrationDataSource() {
        if (!isStudent()) {
            registrationDataSource = Lists.newArrayList();
            return registrationDataSource;
        }

        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        
        registrationDataSource =
                ((PersonCustomer) debtAccount.getCustomer()).getPerson().getStudent().getRegistrationsSet().stream()
                        .map(r -> {
                            
                            final String degreeCode = r.getDegree().getCode();
                            final String degreePresentationName = r.getDegree().getPresentationName(getExecutionYear());
                            final String registrationDate = r.getStartDate() != null ? r.getStartDate().toString("yyyy-MM-dd") : "";
                            final String agreement = services.registrationProtocol(r) != null ? services.registrationProtocol(r).getDescription().getContent() : "";

                            final TreasuryTupleDataSourceBean t = new TreasuryTupleDataSourceBean(r.getExternalId(),
                                String.format("[%s] %s (%s %s)", degreeCode, degreePresentationName, registrationDate, agreement));
                            
                            return t; 
                            
                        }).collect(Collectors.toList());

        return registrationDataSource;
    }

    public boolean isCharged() {
        if (academicTax == null) {
            return false;
        }
        
        if (registration == null || executionYear == null) {
            return false;
        }
        
        if(isImprovementTax() && this.improvementEvaluation == null) {
            return false;
        }
        
        if(isImprovementTax()) {
            return AcademicTaxServices.isImprovementAcademicTaxCharged(registration, executionYear, improvementEvaluation);
        } else {
            return AcademicTaxServices.isAcademicTaxCharged(registration, executionYear, academicTax);
        }
    }

    public String getErrorMessage() {
        errorMessage = "";

        if (academicTax == null) {
            errorMessage = academicTreasuryBundle("error.AcademicTaxDebtCreation.select.academic.tax");
            return errorMessage;
        }
        
        if (registration == null || executionYear == null) {
            errorMessage = academicTreasuryBundle("error.AcademicTaxDebtCreation.select.registration.and.execution.year");
            return errorMessage;
        }
        
        if(isImprovementTax() && this.improvementEvaluation == null) {
            errorMessage = academicTreasuryBundle("error.AcademicTaxDebtCreation.select.improvement.evaluation");
            return errorMessage;
        }
        
        if(isCharged()) {
            errorMessage = academicTreasuryBundle("error.AcademicTaxDebtCreation.academic.tax.already.charged");
            return errorMessage;
        }
            
        return errorMessage;
    }

    public List<TreasuryTupleDataSourceBean> getAcademicTaxesDataSource() {
        academicTaxesDataSource =
                AcademicTax.findAll().map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getProduct().getName().getContent()))
                        .sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT).collect(Collectors.toList());

        return academicTaxesDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getImprovementEnrolmentEvaluationsDataSource() {
        if (!isStudent()) {
            improvementEnrolmentEvaluationsDataSource = Lists.newArrayList();
            return improvementEnrolmentEvaluationsDataSource;
        }

        if (academicTax == null) {
            improvementEnrolmentEvaluationsDataSource = Lists.newArrayList();
            return improvementEnrolmentEvaluationsDataSource;
        }

        if (!academicTax.isImprovementTax()) {
            improvementEnrolmentEvaluationsDataSource = Lists.newArrayList();
            return improvementEnrolmentEvaluationsDataSource;
        }

        if (getRegistration() == null || getExecutionYear() == null) {
            improvementEnrolmentEvaluationsDataSource = Lists.newArrayList();
            return improvementEnrolmentEvaluationsDataSource;
        }

        improvementEnrolmentEvaluationsDataSource =
                TuitionServices.improvementEnrolments(getRegistration(), getExecutionYear()).stream()
                        .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(),
                                l.getEnrolment().getName().getContent() + " - " + l.getExecutionPeriod().getQualifiedName()))
                .collect(Collectors.toList()).stream().sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT).collect(Collectors.toList());

        return improvementEnrolmentEvaluationsDataSource;
    }

    public boolean isImprovementTax() {
        return this.academicTax != null && this.academicTax.isImprovementTax();
    }

    public boolean isStudent() {
        return debtAccount.getCustomer().isPersonCustomer()
                && ((PersonCustomer) debtAccount.getCustomer()).getPerson().getStudent() != null;
    }

    public boolean isImprovementTaxSelected() {
        this.improvementTaxSelected = isImprovementTax();

        return this.improvementTaxSelected;
    }

    /* -----------------
     * GETTERS & SETTERS
     * -----------------
     */
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

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    public FinantialEntity getFinantialEntity() {
        return finantialEntity;
    }

    public void setFinantialEntity(FinantialEntity finantialEntity) {
        this.finantialEntity = finantialEntity;
    }

    public AcademicTax getAcademicTax() {
        return academicTax;
    }

    public void setAcademicTax(AcademicTax academicTax) {
        this.academicTax = academicTax;
    }

    public EnrolmentEvaluation getImprovementEvaluation() {
        return improvementEvaluation;
    }

    public void setImprovementEvaluation(EnrolmentEvaluation improvementEvaluation) {
        this.improvementEvaluation = improvementEvaluation;
    }

}
