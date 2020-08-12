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
package org.fenixedu.academictreasury.domain.serviceRequests;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituation;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituationType;
import org.fenixedu.academic.domain.serviceRequests.ServiceRequestType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.curriculum.ICurriculumEntry;
import org.joda.time.DateTime;

/**
 * 
 * Defines the {@link AcademicServiceRequest} contract for FenixeduAcademicTreasury
 *
 */
public interface ITreasuryServiceRequest {

    public Registration getRegistration();

    public ServiceRequestType getServiceRequestType();

    public Person getPerson();

    public AcademicServiceRequestSituation getSituationByType(AcademicServiceRequestSituationType type);

    public boolean hasExecutionYear();

    public ExecutionYear getExecutionYear();

    public String getServiceRequestNumberYear();

    public boolean hasRegistation();

    public Locale getLanguage();

    public boolean hasLanguage();

    public boolean isDetailed();

    public Integer getNumberOfUnits();

    public boolean hasNumberOfUnits();

    public Integer getNumberOfDays();

    public boolean hasNumberOfDays();

    public boolean isUrgent();

    public Integer getNumberOfPages();

    public boolean hasNumberOfPages();

    public CycleType getCycleType();

    public boolean hasCycleType();

    public DateTime getRequestDate();

    public String getDescription();

    public boolean hasApprovedExtraCurriculum();

    public Set<ICurriculumEntry> getApprovedExtraCurriculum();

    public boolean hasApprovedStandaloneCurriculum();

    public Set<ICurriculumEntry> getApprovedStandaloneCurriculum();

    public boolean hasApprovedEnrolments();

    public Set<ICurriculumEntry> getApprovedEnrolments();

    public boolean hasCurriculum();

    public Set<ICurriculumEntry> getCurriculum();

    public boolean hasEnrolmentsByYear();

    public Set<ICurriculumEntry> getEnrolmentsByYear();

    public boolean hasStandaloneEnrolmentsByYear();

    public Set<ICurriculumEntry> getStandaloneEnrolmentsByYear();

    public boolean hasExtracurricularEnrolmentsByYear();

    public Set<ICurriculumEntry> getExtracurricularEnrolmentsByYear();

    public String getExternalId();
    
    public Map<String, String> getPropertyValuesMap();

}
