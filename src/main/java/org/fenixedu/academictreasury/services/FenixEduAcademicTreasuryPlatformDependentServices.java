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
package org.fenixedu.academictreasury.services;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicAccessRule;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

public class FenixEduAcademicTreasuryPlatformDependentServices implements IAcademicTreasuryPlatformDependentServices {
	
	@Override
	public Set<Degree> readDegrees(FinantialEntity finantialEntity) {
	    if(finantialEntity.getAdministrativeOffice() != null) {
	        return finantialEntity.getAdministrativeOffice().getAdministratedDegrees();
	    }
	    
	    return Collections.emptySet();
	}

    public boolean isFrontOfficeMember(final String username, final FinantialEntity finantialEntity) {
        final User user = User.findByUsername(username);

        return AcademicAccessRule.isMember(user, AcademicOperationType.MANAGE_STUDENT_PAYMENTS, emptySet(),
                singleton(finantialEntity.getAdministrativeOffice()));
    }
    
    public boolean isBackOfficeMember(final String username, final FinantialEntity finantialEntity) {
        final User user = User.findByUsername(username);

        return AcademicAccessRule.isMember(user, AcademicOperationType.MANAGE_STUDENT_PAYMENTS_ADV, emptySet(),
                singleton(finantialEntity.getAdministrativeOffice()));
    }
    
    public boolean isAllowToModifySettlements(final String username, final FinantialEntity finantialEntity) {
        final User user = User.findByUsername(username);

        return AcademicAccessRule.isMember(user, AcademicOperationType.PAYMENTS_MODIFY_SETTLEMENTS, emptySet(),
                singleton(finantialEntity.getAdministrativeOffice()));
    }

    public boolean isAllowToModifyInvoices(final String username, final FinantialEntity finantialEntity) {
        final User user = User.findByUsername(username);

        return AcademicAccessRule.isMember(user, AcademicOperationType.PAYMENTS_MODIFY_INVOICES, emptySet(),
                singleton(finantialEntity.getAdministrativeOffice()));
    }

    @Override
    public Set<String> getFrontOfficeMemberUsernames(final FinantialEntity finantialEntity) {
        return AcademicAccessRule.getMembers(AcademicOperationType.MANAGE_STUDENT_PAYMENTS, 
                emptySet(), singleton(finantialEntity.getAdministrativeOffice()))
                .filter(u -> !isNullOrEmpty(u.getUsername()))
                .map(u -> u.getUsername()).collect(Collectors.toSet());
    }

    @Override
    public Set<String> getBackOfficeMemberUsernames(final FinantialEntity finantialEntity) {
        return AcademicAccessRule.getMembers(AcademicOperationType.MANAGE_STUDENT_PAYMENTS_ADV, 
                emptySet(), singleton(finantialEntity.getAdministrativeOffice()))
                .filter(u -> !isNullOrEmpty(u.getUsername()))
                .map(u -> u.getUsername()).collect(Collectors.toSet());
    }

    @Override
    public String fiscalCountry(final Person person) {
        return person.getFiscalAddress() != null && person.getFiscalAddress().getCountryOfResidence() != null ? person.getFiscalAddress().getCountryOfResidence().getCode() : null;
    }
    
    @Override
    public String fiscalNumber(final Person person) {
        return person.getSocialSecurityNumber();
    }
    
	@Override
	public String localizedNameOfDegreeType(DegreeType degreeType) {
		return degreeType.getName().getContent();
	}

    @Override
    public String localizedNameOfDegreeType(DegreeType degreeType, Locale locale) {
        return degreeType.getName().getContent(locale);
    }

	@Override
	public String localizedNameOfStatuteType(StatuteType statuteType) {
		return statuteType.getName().getContent();
	}

    @Override
    public String localizedNameOfStatuteType(StatuteType statuteType, Locale locale) {
        return statuteType.getName().getContent(locale);
    }

    /* Student & Registration */
	
	public IngressionType ingression(final Registration registration) {
	    return registration.getIngressionType();
	}

    @Override
    public FinantialEntity finantialEntityOfDegree(Degree degree, LocalDate when) {
        final AdministrativeOffice administrativeOffice = degree.getAdministrativeOffice();
        return administrativeOffice.getFinantialEntity();
    }

    @Override
    public String localizedNameOfEnrolment(Enrolment enrolment) {
        return localizedNameOfEnrolment(enrolment, I18N.getLocale());
    }

    @Override
    public String localizedNameOfEnrolment(Enrolment enrolment, Locale locale) {
        return enrolment.getName().getContent(locale);
    }

    @Override
    public String localizedNameOfAdministrativeOffice(AdministrativeOffice administrativeOffice) {
        return administrativeOffice.getName().getContent();
    }
    
    @Override
    public String localizedNameOfAdministrativeOffice(AdministrativeOffice administrativeOffice, Locale locale) {
        return administrativeOffice.getName().getContent(locale);
    }
    
    /* Student & Registration */
    
    @Override
    public RegistrationRegimeType registrationRegimeType(Registration registration, ExecutionYear executionYear) {
        return registration.getRegimeType(executionYear);
    }

    @Override
    public Set<StatuteType> statutesTypesValidOnAnyExecutionSemesterFor(Registration registration, ExecutionInterval executionInterval) {
    	return Sets.newHashSet(findStatuteTypes(registration, executionInterval));
    }
	
	
    static public Collection<StatuteType> findStatuteTypes(final Registration registration,
            final ExecutionInterval executionInterval) {

        if (executionInterval instanceof ExecutionYear) {
            return findStatuteTypesByYear(registration, (ExecutionYear) executionInterval);
        }

        return findStatuteTypesByChildInterval(registration, executionInterval);
    }

    static private Collection<StatuteType> findStatuteTypesByYear(final Registration registration,
            final ExecutionYear executionYear) {

        final Set<StatuteType> result = Sets.newHashSet();
        for (final ExecutionInterval executionInterval : executionYear.getExecutionPeriodsSet()) {
            result.addAll(findStatuteTypesByChildInterval(registration, executionInterval));
        }

        return result;

    }

    static private Collection<StatuteType> findStatuteTypesByChildInterval(final Registration registration,
            final ExecutionInterval executionInterval) {

        return registration.getStudent().getStudentStatutesSet().stream()
                .filter(s -> s.isValidInExecutionInterval(executionInterval)
                        && (s.getRegistration() == null || s.getRegistration() == registration))
                .map(s -> s.getType()).collect(Collectors.toSet());
    }

    static public String getVisibleStatuteTypesDescription(final Registration registration,
            final ExecutionInterval executionInterval) {
        return findVisibleStatuteTypes(registration, executionInterval).stream().map(s -> s.getName().getContent()).distinct()
                .collect(Collectors.joining(", "));

    }

    static public Collection<StatuteType> findVisibleStatuteTypes(final Registration registration,
            final ExecutionInterval executionInterval) {
        return findStatuteTypes(registration, executionInterval).stream().filter(s -> s.getVisible()).collect(Collectors.toSet());
    }
    
    /* Execution Intervals */
    public Integer executionIntervalChildOrder(ExecutionInterval executionInterval) {
        return executionInterval.getChildOrder();
    }
    
}
