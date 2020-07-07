package org.fenixedu.academictreasury.domain.tuition;

import static org.fenixedu.academic.domain.CurricularYear.readByYear;
import static org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan.semestersWithEnrolments;

import java.util.Set;

import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;

import com.google.common.collect.Sets;

public class InferTuitionStudentConditionsBean {

    private RegistrationRegimeType regimeType;
    private RegistrationProtocol registrationProtocol;
    private IngressionType ingression;
    private Set<Integer> semestersWithEnrolments;
    private CurricularYear curricularYear;
    private boolean firstTimeStudent;
    private Set<StatuteType> statutes;

    public InferTuitionStudentConditionsBean() {
    }

    public static InferTuitionStudentConditionsBean build(final Registration registration, final ExecutionYear executionYear) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices = AcademicTreasuryPlataformDependentServicesFactory.implementation();

        
        final InferTuitionStudentConditionsBean bean = new InferTuitionStudentConditionsBean();

        bean.setRegimeType(academicTreasuryServices.registrationRegimeType(registration, executionYear));
        bean.setRegistrationProtocol(registration.getRegistrationProtocol());
        bean.setIngression(academicTreasuryServices.ingression(registration));
        bean.setSemestersWithEnrolments(semestersWithEnrolments(registration, executionYear));
        bean.setCurricularYear(readByYear(TuitionPaymentPlan.curricularYear(registration, executionYear)));
        bean.setFirstTimeStudent(TuitionPaymentPlan.firstTimeStudent(registration, executionYear));
        bean.setStatutes(academicTreasuryServices.statutesTypesValidOnAnyExecutionSemesterFor(registration, executionYear));

        return bean;
    }

    // @formatter:off
    /* *****************
     * GETTERS & SETTERS
     * *****************
     */
    // @formatter:on

    public RegistrationRegimeType getRegimeType() {
        return regimeType;
    }

    public void setRegimeType(RegistrationRegimeType regimeType) {
        this.regimeType = regimeType;
    }

    public RegistrationProtocol getRegistrationProtocol() {
        return registrationProtocol;
    }

    public void setRegistrationProtocol(RegistrationProtocol registrationProtocol) {
        this.registrationProtocol = registrationProtocol;
    }

    public IngressionType getIngression() {
        return ingression;
    }

    public void setIngression(IngressionType ingression) {
        this.ingression = ingression;
    }

    public Set<Integer> getSemestersWithEnrolments() {
        return semestersWithEnrolments;
    }

    public void setSemestersWithEnrolments(Set<Integer> semestersWithEnrolments) {
        this.semestersWithEnrolments = semestersWithEnrolments;
    }

    public CurricularYear getCurricularYear() {
        return curricularYear;
    }

    public void setCurricularYear(CurricularYear curricularYear) {
        this.curricularYear = curricularYear;
    }

    public boolean isFirstTimeStudent() {
        return firstTimeStudent;
    }

    public void setFirstTimeStudent(boolean firstTimeStudent) {
        this.firstTimeStudent = firstTimeStudent;
    }

    public Set<StatuteType> getStatutes() {
        return statutes;
    }

    public void setStatutes(Set<StatuteType> statutes) {
        this.statutes = statutes;
    }

}
