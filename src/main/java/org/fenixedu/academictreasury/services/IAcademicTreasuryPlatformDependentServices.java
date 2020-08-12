package org.fenixedu.academictreasury.services;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

public interface IAcademicTreasuryPlatformDependentServices {

	
    /* Read data sets */
    
    default Set<DegreeType> readAllDegreeTypes() {
        return DegreeType.all().collect(Collectors.toSet());
    }
    
	default Set<DegreeCurricularPlan> readAllDegreeCurricularPlansSet() {
    	return Degree.readAllMatching((dt) -> true).stream().flatMap(d -> d.getDegreeCurricularPlansSet().stream())
    			.collect(Collectors.toSet());
    }
    
	default Set<DegreeCurricularPlan> readDegreeCurricularPlansWithExecutionDegree(final ExecutionYear executionYear, final DegreeType degreeType) {
    	return ExecutionDegree.getAllByExecutionYearAndDegreeType(executionYear, degreeType).stream()
    				.map(e -> e.getDegreeCurricularPlan())
    				.collect(Collectors.toSet());
    }
    
	default Set<CurricularYear> readAllCurricularYearsSet() {
    	final Set<CurricularYear> result = Sets.newHashSet();
    	
    	for(int i = 1; i <= 10; i++) {
    		if(CurricularYear.readByYear(i) == null) {
    			return result;
    		}
    		
    		result.add(CurricularYear.readByYear(i));
    	}
    	
    	return result;
    }
    
	default Set<IngressionType> readAllIngressionTypesSet() {
    	return IngressionType.findAllByPredicate((i) -> true).collect(Collectors.toSet());
    }
    
	default Set<RegistrationProtocol> readAllRegistrationProtocol() {
    	return RegistrationProtocol.findByPredicate((p) -> true).collect(Collectors.toSet());
    }
    
	default Set<StatuteType> readAllStatuteTypesSet() {
    	return StatuteType.readAll((s) -> true).collect(Collectors.toSet());
    }
	
    default Set<StatuteType> readAllStatuteTypesSet(boolean active) {
        return StatuteType.readAll((s) -> s.isActive() == active).collect(Collectors.toSet());
    }
    
	default Set<Person> readAllPersonsSet() { 
    	return Party.readAllPersons();
    }
	
	default Set<Registration> readAllRegistrations(RegistrationProtocol registrationProtocol) {
	    return registrationProtocol.getRegistrationsSet();
	}
	
	default Set<Registration> readAllRegistrations(IngressionType ingressionType) {
	    return ingressionType.getRegistrationSet();
	}
	
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
    
    default Optional<FinantialEntity> finantialEntity(AdministrativeOffice administrativeOffice) {
        return Optional.ofNullable(administrativeOffice.getFinantialEntity());
    }
    
    default Optional<FinantialEntity> finantialEntity(Unit unit) {
        return Optional.ofNullable(unit.getFinantialEntity());
    }
    
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
    
    default RegistrationDataByExecutionYear findRegistrationDataByExecutionYear(Registration registration, ExecutionYear executionYear) {
        if(registration == null || executionYear == null) {
            return null;
        }
        
        return registration.getRegistrationDataByExecutionYearSet().stream()
                .filter(r -> r.getExecutionYear() == executionYear).findFirst().orElse(null);
    }
    
    
    IngressionType ingression(Registration registration);
    
    default RegistrationProtocol registrationProtocol(Registration registration) {
        return registration.getRegistrationProtocol();
    }
    
    RegistrationRegimeType registrationRegimeType(Registration registration, ExecutionYear executionYear);

    Set<StatuteType> statutesTypesValidOnAnyExecutionSemesterFor(Registration registration, ExecutionInterval executionInterval);
    
    /* AdministrativeOffice */

    default Stream<AdministrativeOffice> findAdministrativeOfficesByPredicate(Predicate<AdministrativeOffice> predicate) {
        return Bennu.getInstance().getAdministrativeOfficesSet().stream().filter(predicate);
    }
    
    /* Execution Intervals */
    Integer executionIntervalChildOrder(ExecutionInterval executionInterval);

}
