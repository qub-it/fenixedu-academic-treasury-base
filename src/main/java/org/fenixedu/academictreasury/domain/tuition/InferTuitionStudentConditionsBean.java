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
        bean.setRegistrationProtocol(academicTreasuryServices.registrationProtocol(registration));
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
