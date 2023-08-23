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
package org.fenixedu.academictreasury.dto.tuition;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.emoluments.AcademicTax;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.services.TuitionServices;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;

import pt.ist.fenixframework.Atomic;

public class TuitionDebtCreationBean implements Serializable, ITreasuryBean {

    private static final long serialVersionUID = 1L;

    private LocalDate debtDate;
    private ExecutionYear executionYear;
    private Registration registration;
    private TuitionPaymentPlan tuitionPaymentPlan;
    private Enrolment enrolment;

    private DebtAccount debtAccount;

    private List<TreasuryTupleDataSourceBean> registrationDataSource;
    private List<TreasuryTupleDataSourceBean> executionYearDataSource;
    private List<TreasuryTupleDataSourceBean> tuitionPaymentPlansDataSource;

    private List<TreasuryTupleDataSourceBean> standaloneEnrolmentsDataSource;
    private List<TreasuryTupleDataSourceBean> extracurricularEnrolmentsDataSource;
    private List<TreasuryTupleDataSourceBean> improvementEnrolmentEvaluationsDataSource;

    private String errorMessage;

    private TuitionPaymentPlanGroup tuitionPaymentPlanGroup;
    private AcademicTax academicTax;

    private Boolean allInstallmentProducts;

    private List<Product> installmentProductsList;
    
    private Map<Product, LocalDate> recalculationInstallmentProductsMap = new HashMap<>();
    
    private Boolean applyDefaultEnrolmentCredits;
    
    public TuitionDebtCreationBean(final DebtAccount debtAccount, final TuitionPaymentPlanGroup tuitionPaymentPlanGroup) {
        this.debtAccount = debtAccount;
        this.tuitionPaymentPlanGroup = tuitionPaymentPlanGroup;

        updateData();
    }

    public TuitionDebtCreationBean(final DebtAccount debtAccount, final AcademicTax academicTax) {
        this.debtAccount = debtAccount;
        this.academicTax = academicTax;

        updateData();
    }

    @Atomic
    public void updateData() {
        if (executionYear != null && (registration == null || !possibleExecutionYears().contains(executionYear))) {
            executionYear = null;
            tuitionPaymentPlan = null;
        }

        getRegistrationDataSource();
        getExecutionYearDataSource();
        getTuitionPaymentPlansDataSource();
        getErrorMessage();

        getStandaloneEnrolmentsDataSource();
        getExtracurricularEnrolmentsDataSource();
        getImprovementEnrolmentEvaluationsDataSource();

        if (registration != null && executionYear != null && isRegistrationTuition()
                && !TuitionServices.normalEnrolmentsIncludingAnnuled(registration, executionYear).isEmpty()) {
            debtDate = TuitionServices.enrolmentDate(registration, executionYear, false);
        } else {
            debtDate = new LocalDate();
        }
    }

    public List<TreasuryTupleDataSourceBean> getExecutionYearDataSource() {
        executionYearDataSource = Lists.newArrayList();

        for (final ExecutionYear executionYear : possibleExecutionYears()) {
            final String id = executionYear.getExternalId();
            String text = executionYear.getQualifiedName();

            if (isRegistrationTuition() && registration != null) {
                final Set<Enrolment> normalEnrolments =
                        TuitionServices.normalEnrolmentsIncludingAnnuled(registration, executionYear);

                if (normalEnrolments.size() == 1) {
                    text += " " + academicTreasuryBundle("label.TuitionDebtCreationBean.enrolments.one");
                } else if (normalEnrolments.size() > 1) {
                    text += " " + academicTreasuryBundle("label.TuitionDebtCreationBean.enrolments",
                            String.valueOf(normalEnrolments.size()));
                }
            }

            executionYearDataSource.add(new TreasuryTupleDataSourceBean(id, text));
        }

        return executionYearDataSource;
    }

    private List<ExecutionYear> possibleExecutionYears() {
        final List<ExecutionYear> executionYears = ExecutionYear.readNotClosedExecutionYears().stream()
                .sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR).collect(Collectors.toList());

        return executionYears;
    }

    public List<TreasuryTupleDataSourceBean> getRegistrationDataSource() {
        if (!isStudent()) {
            registrationDataSource = Lists.newArrayList();
            return registrationDataSource;
        }
        
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();

        registrationDataSource =
                ((PersonCustomer) debtAccount.getCustomer()).getPerson().getStudent().getRegistrationsSet().stream().map(r -> {
                    final String degreeCode = r.getDegree().getCode();
                    final String degreePresentationName = r.getDegree().getPresentationName(getExecutionYear());
                    final String registrationDate = r.getStartDate() != null ? r.getStartDate().toString("yyyy-MM-dd") : "";
                    final String agreement =
                            services.registrationProtocol(r) != null ? services.registrationProtocol(r).getDescription().getContent() : "";

                    final TreasuryTupleDataSourceBean t = new TreasuryTupleDataSourceBean(r.getExternalId(),
                            String.format("[%s] %s (%s %s)", degreeCode, degreePresentationName, registrationDate, agreement));

                    return t;
                }).collect(Collectors.toList());

        return registrationDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getTuitionPaymentPlansDataSource() {
        tuitionPaymentPlansDataSource = Lists.newArrayList();

        if (!isStudent()) {
            return tuitionPaymentPlansDataSource;
        }

        if (getRegistration() == null || getExecutionYear() == null) {
            return tuitionPaymentPlansDataSource;
        }

        if ((isStandaloneTuition() || isExtracurricularTuition()) && enrolment == null) {
            return tuitionPaymentPlansDataSource;
        }

        if (isRegistrationTuition()) {
            final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(getExecutionYear());

            if (studentCurricularPlan == null) {
                return tuitionPaymentPlansDataSource;
            }

            tuitionPaymentPlansDataSource = TuitionPaymentPlan
                    .find(tuitionPaymentPlanGroup, studentCurricularPlan.getDegreeCurricularPlan(), getExecutionYear())
                    .map(t -> new TreasuryTupleDataSourceBean(t.getExternalId(), t.getConditionsDescription()))
                    .collect(Collectors.toList());
        } else if (isStandaloneTuition() || isExtracurricularTuition()) {
            tuitionPaymentPlansDataSource = TuitionPaymentPlan
                    .find(tuitionPaymentPlanGroup, enrolment.getCurricularCourse().getDegreeCurricularPlan(), getExecutionYear())
                    .map(t -> new TreasuryTupleDataSourceBean(t.getExternalId(), t.getConditionsDescription()))
                    .collect(Collectors.toList());
        }

        return tuitionPaymentPlansDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getStandaloneEnrolmentsDataSource() {
        if (!isStudent()) {
            standaloneEnrolmentsDataSource = Lists.newArrayList();
            return standaloneEnrolmentsDataSource;
        }

        if (getRegistration() == null || getExecutionYear() == null) {
            standaloneEnrolmentsDataSource = Lists.newArrayList();
            return standaloneEnrolmentsDataSource;
        }

        standaloneEnrolmentsDataSource =
                TuitionServices.standaloneEnrolmentsIncludingAnnuled(getRegistration(), getExecutionYear()).stream()
                        .sorted(TuitionServices.ENROLMENT_COMPARATOR_BY_NAME_AND_ID).collect(Collectors.toList()).stream()
                        .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getName().getContent()))
                        .collect(Collectors.toList());

        return standaloneEnrolmentsDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getExtracurricularEnrolmentsDataSource() {
        if (!isStudent()) {
            extracurricularEnrolmentsDataSource = Lists.newArrayList();
            return extracurricularEnrolmentsDataSource;
        }

        if (getRegistration() == null || getExecutionYear() == null) {
            extracurricularEnrolmentsDataSource = Lists.newArrayList();
            return extracurricularEnrolmentsDataSource;
        }

        extracurricularEnrolmentsDataSource =
                TuitionServices.extracurricularEnrolmentsIncludingAnnuled(getRegistration(), getExecutionYear()).stream()
                        .sorted(TuitionServices.ENROLMENT_COMPARATOR_BY_NAME_AND_ID).collect(Collectors.toList()).stream()
                        .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getName().getContent()))
                        .collect(Collectors.toList());

        return extracurricularEnrolmentsDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getImprovementEnrolmentEvaluationsDataSource() {
        if (!isStudent()) {
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
                        .collect(Collectors.toList()).stream().sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT)
                        .collect(Collectors.toList());

        return improvementEnrolmentEvaluationsDataSource;
    }

    public boolean isTuitionCharged() {
        if (registration == null || executionYear == null) {
            return false;
        }

        if ((isStandaloneTuition() || isExtracurricularTuition()) && enrolment == null) {
            return false;
        }

        if (isRegistrationTuition()) {
            return TuitionServices.isTuitionForRegistrationCharged(registration, executionYear);
        } else if (isStandaloneTuition()) {
            return TuitionServices.isTuitionForStandaloneCharged(registration, executionYear, enrolment);
        } else if (isExtracurricularTuition()) {
            return TuitionServices.isTuitionForExtracurricularCharged(registration, executionYear, enrolment);
        }

        return false;
    }

    public String getErrorMessage() {
        this.errorMessage = "";
        this.tuitionPaymentPlan = null;

        if (registration == null || executionYear == null) {
            errorMessage = academicTreasuryBundle("label.TuitionDebtCreationBean.infer.select.registration.and.executionYear");
            return errorMessage;
        }

        if ((isStandaloneTuition() || isExtracurricularTuition()) && enrolment == null) {
            errorMessage = academicTreasuryBundle("label.TuitionDebtCreationBean.infer.select.enrolment");
            return errorMessage;
        }

        if (isRegistrationTuition()) {
            if (TuitionServices.isTuitionForRegistrationCharged(registration, executionYear)) {
                errorMessage = academicTreasuryBundle("error.TuitionDebtCreationBean.tuition.registration.already.charged");
                return errorMessage;
            }

            this.tuitionPaymentPlan = TuitionPaymentPlan.inferTuitionPaymentPlanForRegistration(registration, executionYear);

            if (tuitionPaymentPlan == null) {
                errorMessage = academicTreasuryBundle("label.TuitionDebtCreationBean.infer.impossible");
            }

        } else if (isStandaloneTuition()) {
            if (TuitionServices.isTuitionForStandaloneCharged(registration, executionYear, enrolment)) {
                errorMessage = academicTreasuryBundle("error.TuitionDebtCreationBean.tuition.registration.already.charged");
                return errorMessage;
            }

            this.tuitionPaymentPlan =
                    TuitionPaymentPlan.inferTuitionPaymentPlanForStandaloneEnrolment(registration, executionYear, enrolment);

            if (this.tuitionPaymentPlan == null) {
                errorMessage = academicTreasuryBundle("label.TuitionDebtCreationBean.infer.impossible");
            }

        } else if (isExtracurricularTuition()) {
            if (TuitionServices.isTuitionForExtracurricularCharged(registration, executionYear, enrolment)) {
                errorMessage = academicTreasuryBundle("error.TuitionDebtCreationBean.tuition.registration.already.charged");
                return errorMessage;
            }

            this.tuitionPaymentPlan =
                    TuitionPaymentPlan.inferTuitionPaymentPlanForExtracurricularEnrolment(registration, executionYear, enrolment);

            if (this.tuitionPaymentPlan == null) {
                errorMessage = academicTreasuryBundle("label.TuitionDebtCreationBean.infer.impossible");
            }
        }

        return errorMessage;
    }

    public boolean isImprovementTax() {
        return this.academicTax != null && this.academicTax == AcademicTreasurySettings.getInstance().getImprovementAcademicTax();
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

    public void addRecalculationInstallmentProduct(Product product, LocalDate dueDate) {
        this.recalculationInstallmentProductsMap.put(product, dueDate);
    }

    public void removeRecalculationInstallmentProduct(Product product) {
        this.recalculationInstallmentProductsMap.remove(product);
    }

    /* -----------------
     * GETTERS & SETTERS
     * -----------------
     */

    public boolean isStudent() {
        return debtAccount.getCustomer().isPersonCustomer()
                && ((PersonCustomer) debtAccount.getCustomer()).getPerson().getStudent() != null;
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

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    public TuitionPaymentPlan getTuitionPaymentPlan() {
        return tuitionPaymentPlan;
    }

    public void setTuitionPaymentPlan(TuitionPaymentPlan tuitionPaymentPlan) {
        this.tuitionPaymentPlan = tuitionPaymentPlan;
    }

    public Enrolment getEnrolment() {
        return enrolment;
    }

    public void setEnrolment(Enrolment enrolment) {
        this.enrolment = enrolment;
    }
    
    public Boolean getAllInstallmentProducts() {
        return allInstallmentProducts;
    }
    
    public void setAllInstallmentProducts(Boolean allInstallmentProducts) {
        this.allInstallmentProducts = allInstallmentProducts;
    }
    
    public List<Product> getInstallmentProductsList() {
        return installmentProductsList;
    }
    
    public void setInstallmentProductsList(List<Product> installmentProductsList) {
        this.installmentProductsList = installmentProductsList;
    }

    public Map<Product, LocalDate> getRecalculationInstallmentProductsMap() {
        return recalculationInstallmentProductsMap;
    }
    
    public void setRecalculationInstallmentProductsMap(Map<Product, LocalDate> recalculationInstallmentProductsMap) {
        this.recalculationInstallmentProductsMap = recalculationInstallmentProductsMap;
    }
    
    public Boolean getApplyDefaultEnrolmentCredits() {
        return applyDefaultEnrolmentCredits;
    }
    
    public void setApplyDefaultEnrolmentCredits(Boolean applyDefaultEnrolmentCredits) {
        this.applyDefaultEnrolmentCredits = applyDefaultEnrolmentCredits;
    }
}
