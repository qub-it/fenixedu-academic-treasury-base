package org.fenixedu.academictreasury.dto.tuition;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.io.Serializable;
import java.util.List;
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
import org.fenixedu.academictreasury.services.TuitionServices;
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

            if(isRegistrationTuition() && registration != null) {
                final Set<Enrolment> normalEnrolments = TuitionServices.normalEnrolmentsIncludingAnnuled(registration, executionYear);
                
                if(normalEnrolments.size() == 1) {
                    text += " " + academicTreasuryBundle("label.TuitionDebtCreationBean.enrolments.one");
                } else if (normalEnrolments.size() > 1) {
                    text += " " + academicTreasuryBundle("label.TuitionDebtCreationBean.enrolments", String.valueOf(normalEnrolments.size()));
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

        registrationDataSource =
                ((PersonCustomer) debtAccount.getCustomer()).getPerson().getStudent().getRegistrationsSet().stream()
                        .map(r -> { 
                            final String degreeCode = r.getDegree().getCode();
                            final String degreePresentationName = r.getDegree().getPresentationName(getExecutionYear());
                            final String registrationDate = r.getStartDate() != null ? r.getStartDate().toString("yyyy-MM-dd") : "";
                            final String agreement = r.getRegistrationProtocol() != null ? r.getRegistrationProtocol().getDescription().getContent() : "";

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
            
            if(studentCurricularPlan == null) {
                return tuitionPaymentPlansDataSource;
            }
            
            tuitionPaymentPlansDataSource = TuitionPaymentPlan
                    .find(tuitionPaymentPlanGroup,
                            studentCurricularPlan.getDegreeCurricularPlan(),
                            getExecutionYear())
                    .map(t -> new TreasuryTupleDataSourceBean(t.getExternalId(), t.getConditionsDescription().getContent()))
                    .collect(Collectors.toList());
        } else if (isStandaloneTuition() || isExtracurricularTuition()) {
            tuitionPaymentPlansDataSource =
                    TuitionPaymentPlan
                            .find(tuitionPaymentPlanGroup, enrolment.getCurricularCourse().getDegreeCurricularPlan(),
                                    getExecutionYear())
                            .map(t -> new TreasuryTupleDataSourceBean(t.getExternalId(), t.getConditionsDescription().getContent()))
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

        standaloneEnrolmentsDataSource = TuitionServices.standaloneEnrolmentsIncludingAnnuled(getRegistration(), getExecutionYear()).stream()
                .sorted(Enrolment.COMPARATOR_BY_NAME_AND_ID).collect(Collectors.toList()).stream()
                .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getName().getContent())).collect(Collectors.toList());

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

        extracurricularEnrolmentsDataSource = TuitionServices.extracurricularEnrolmentsIncludingAnnuled(getRegistration(), getExecutionYear())
                .stream().sorted(Enrolment.COMPARATOR_BY_NAME_AND_ID).collect(Collectors.toList()).stream()
                .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getName().getContent())).collect(Collectors.toList());

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
                .collect(Collectors.toList()).stream().sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT).collect(Collectors.toList());

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

}
