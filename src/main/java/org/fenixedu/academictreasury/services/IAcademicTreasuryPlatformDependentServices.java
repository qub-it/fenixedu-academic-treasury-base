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


import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOfficeType;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

public interface IAcademicTreasuryPlatformDependentServices {

	
    /* Read data sets */
    
//    default Set<DegreeType> readAllDegreeTypes() {
//        return DegreeType.all().collect(Collectors.toSet());
//    }
	Set<DegreeType> readAllDegreeTypes();
	
//	default Set<DegreeCurricularPlan> readAllDegreeCurricularPlansSet() {
//    	return Degree.readAllDegrees().stream().filter((dt) -> true).flatMap(d -> d.getDegreeCurricularPlansSet().stream())
//    			.collect(Collectors.toSet());
//    }
    Set<DegreeCurricularPlan> readAllDegreeCurricularPlansSet();
    
//	default Set<DegreeCurricularPlan> readDegreeCurricularPlansWithExecutionDegree(final ExecutionYear executionYear, final DegreeType degreeType) {
//    	return ExecutionDegree.getAllByExecutionYearAndDegreeType(executionYear, degreeType).stream()
//    				.map(e -> e.getDegreeCurricularPlan())
//    				.collect(Collectors.toSet());
//    }
	Set<DegreeCurricularPlan> readDegreeCurricularPlansWithExecutionDegree(final ExecutionYear executionYear, final DegreeType degreeType) ;
	
//	default Set<CurricularYear> readAllCurricularYearsSet() {
//    	final Set<CurricularYear> result = Sets.newHashSet();
//    	
//    	for(int i = 1; i <= 10; i++) {
//    		if(CurricularYear.readByYear(i) == null) {
//    			return result;
//    		}
//    		
//    		result.add(CurricularYear.readByYear(i));
//    	}
//    	
//    	return result;
//    }
	 Set<CurricularYear> readAllCurricularYearsSet();
    
//	default Set<IngressionType> readAllIngressionTypesSet() {
//    	return IngressionType.findAllByPredicate((i) -> true).collect(Collectors.toSet());
//    }
	Set<IngressionType> readAllIngressionTypesSet();
	
	
//	default Set<RegistrationProtocol> readAllRegistrationProtocol() {
//    	return RegistrationProtocol.findByPredicate((p) -> true).collect(Collectors.toSet());
//    }
	Set<RegistrationProtocol> readAllRegistrationProtocol();
	
//	default Set<StatuteType> readAllStatuteTypesSet() {
//    	return StatuteType.readAll((s) -> true).collect(Collectors.toSet());
//    }
	Set<StatuteType> readAllStatuteTypesSet();
	
//    default Set<StatuteType> readAllStatuteTypesSet(boolean active) {
//        return StatuteType.readAll((s) -> s.isActive() == active).collect(Collectors.toSet());
//    }
	Set<StatuteType> readAllStatuteTypesSet(boolean active);
	
//	default Set<Person> readAllPersonsSet() { 
//    	return Party.readAllPersons();
//    }
	Set<Person> readAllPersonsSet();
	
//	default Set<Registration> readAllRegistrations(RegistrationProtocol registrationProtocol) {
//	    return registrationProtocol.getRegistrationsSet();
//	}
	Set<Registration> readAllRegistrations(RegistrationProtocol registrationProtocol);
	
//	default Set<Registration> readAllRegistrations(IngressionType ingressionType) {
//	    return ingressionType.getRegistrationSet();
//	}
	Set<Registration> readAllRegistrations(IngressionType ingressionType);
	
	PersonCustomer personCustomer(Person person);
	
	Set<PersonCustomer> inactivePersonCustomers(Person person);
	
	PhysicalAddress fiscalAddress(Person person);
	
	String iban(Person person);
	
	Set<AcademicTreasuryEvent> academicTreasuryEventsSet(Person person);
	
	String defaultPhoneNumber(Person person);
	
	String defaultMobilePhoneNumber(Person person);
	
	List<PhysicalAddress> pendingOrValidPhysicalAddresses(Person person);

	List<? extends PartyContact> pendingOrValidPartyContacts(Person person, Class<? extends PartyContact> partyContactType);

	void editSocialSecurityNumber(Person person, String fiscalNumber, PhysicalAddress fiscalAddress);
	
	void setFiscalAddress(PhysicalAddress physicalAddress, boolean fiscalAddress);
	
	PhysicalAddress createPhysicalAddress(Person person, Country countryOfResidence, String districtOfResidence, String districtSubdivisionOfResidence, String areaCode, String address);
	
	/* Fiscal Information */
	
	String fiscalCountry(final Person person);
	
	String fiscalNumber(final Person person);
	
	/* Permissions */
	
    boolean isFrontOfficeMember(final String username, final FinantialEntity finantialEntity);
    
    boolean isBackOfficeMember(final String username, final FinantialEntity finantialEntity); 
    
    boolean isAllowToModifySettlements(final String username, final FinantialEntity finantialEntity);

    boolean isAllowToModifyInvoices(final String username, final FinantialEntity finantialEntity);

    Set<Degree> readDegrees(final FinantialEntity finantialEntity);
    
    FinantialEntity finantialEntityOfDegree(final Degree degree, final LocalDate when);
    
//    default Optional<FinantialEntity> finantialEntity(AdministrativeOffice administrativeOffice) {
//        return Optional.ofNullable(administrativeOffice.getFinantialEntity());
//    }
    Optional<FinantialEntity> finantialEntity(AdministrativeOffice administrativeOffice);
    
//    default Optional<FinantialEntity> finantialEntity(Unit unit) {
//        return Optional.ofNullable(unit.getFinantialEntity());
//    }
    Optional<FinantialEntity> finantialEntity(Unit unit);
    
    Set<String> getFrontOfficeMemberUsernames(final FinantialEntity finantialEntity);

    Set<String> getBackOfficeMemberUsernames(final FinantialEntity finantialEntity);

    /* Localized names */
    
    String localizedNameOfDegreeType(DegreeType degreeType);
    
    String localizedNameOfDegreeType(DegreeType degreeType, Locale locale);
    
    String localizedNameOfStatuteType(StatuteType statuteType);
    
    String localizedNameOfStatuteType(StatuteType statuteType, Locale locale);
    
    String localizedNameOfEnrolment(Enrolment enrolment);

    String localizedNameOfEnrolment(Enrolment enrolment, Locale locale);
    
    String localizedNameOfAdministrativeOffice(AdministrativeOffice administrativeOffice);
    
    String localizedNameOfAdministrativeOffice(AdministrativeOffice administrativeOffice, Locale locale);
    
    /* Student & Registration */
    
//    default RegistrationDataByExecutionYear findRegistrationDataByExecutionYear(Registration registration, ExecutionYear executionYear) {
//        if(registration == null || executionYear == null) {
//            return null;
//        }
//        
//        return registration.getRegistrationDataByExecutionYearSet().stream()
//                .filter(r -> r.getExecutionYear() == executionYear).findFirst().orElse(null);
//    }
    RegistrationDataByExecutionYear findRegistrationDataByExecutionYear(Registration registration, ExecutionYear executionYear);
    
    IngressionType ingression(Registration registration);
    
//    default RegistrationProtocol registrationProtocol(Registration registration) {
//        return registration.getRegistrationProtocol();
//    }
    RegistrationProtocol registrationProtocol(Registration registration);
    
    RegistrationRegimeType registrationRegimeType(Registration registration, ExecutionYear executionYear);

    Set<StatuteType> statutesTypesValidOnAnyExecutionSemesterFor(Registration registration, ExecutionInterval executionInterval);
    
    /* AdministrativeOffice */
//    default Stream<AdministrativeOffice> findAdministrativeOfficesByPredicate(Predicate<AdministrativeOffice> predicate) {
//        Set<AdministrativeOffice> allSet = new HashSet<AdministrativeOffice>();
//        allSet.add(AdministrativeOffice.readByAdministrativeOfficeType(AdministrativeOfficeType.DEGREE));
//        allSet.add(AdministrativeOffice.readByAdministrativeOfficeType(AdministrativeOfficeType.MASTER_DEGREE));
//        
//        return allSet.stream().filter(predicate);
//    }
    Stream<AdministrativeOffice> findAdministrativeOfficesByPredicate(Predicate<AdministrativeOffice> predicate);
    
    /* Execution Intervals */
    Integer executionIntervalChildOrder(ExecutionInterval executionInterval);

}
