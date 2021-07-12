package org.fenixedu.academictreasury.services;

import java.util.Locale;
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
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.organizationalStructure.Party;
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
    
    default public Set<DegreeType> readAllDegreeTypes() {
        return DegreeType.all().collect(Collectors.toSet());
    }
    
	default public Set<DegreeCurricularPlan> readAllDegreeCurricularPlansSet() {
    	return Degree.readAllMatching((dt) -> true).stream().flatMap(d -> d.getDegreeCurricularPlansSet().stream())
    			.collect(Collectors.toSet());
    }
    
	default public Set<DegreeCurricularPlan> readDegreeCurricularPlansWithExecutionDegree(final ExecutionYear executionYear, final DegreeType degreeType) {
    	return ExecutionDegree.getAllByExecutionYearAndDegreeType(executionYear, degreeType).stream()
    				.map(e -> e.getDegreeCurricularPlan())
    				.collect(Collectors.toSet());
    }
    
	default public Set<CurricularYear> readAllCurricularYearsSet() {
    	final Set<CurricularYear> result = Sets.newHashSet();
    	
    	for(int i = 1; i <= 10; i++) {
    		if(CurricularYear.readByYear(i) == null) {
    			return result;
    		}
    		
    		result.add(CurricularYear.readByYear(i));
    	}
    	
    	return result;
    }
    
	default public Set<IngressionType> readAllIngressionTypesSet() {
    	return IngressionType.findAllByPredicate((i) -> true).collect(Collectors.toSet());
    }
    
	default public Set<RegistrationProtocol> readAllRegistrationProtocol() {
    	return RegistrationProtocol.findByPredicate((p) -> true).collect(Collectors.toSet());
    }
    
	default public Set<StatuteType> readAllStatuteTypesSet() {
    	return StatuteType.readAll((s) -> true).collect(Collectors.toSet());
    }

    default public Set<StatuteType> readAllStatuteTypesSet(boolean active) {
        return StatuteType.readAll((s) -> true).filter(s -> s.isActive() == active).collect(Collectors.toSet());
    }

    
	default public Set<Person> readAllPersonsSet() { 
    	return Party.readAllPersons();
    }
	
	/* Fiscal Information */
	
	public String fiscalCountry(final Person person);
	
	public String fiscalNumber(final Person person);
	
	/* Permissions */
	
    public boolean isFrontOfficeMember(final String username, final FinantialEntity finantialEntity);
    
    public boolean isBackOfficeMember(final String username, final FinantialEntity finantialEntity); 
    
    public boolean isAllowToModifySettlements(final String username, final FinantialEntity finantialEntity);

    public boolean isAllowToModifyInvoices(final String username, final FinantialEntity finantialEntity);

    public Set<Degree> readDegrees(final FinantialEntity finantialEntity);
    
    public FinantialEntity finantialEntityOfDegree(final Degree degree, final LocalDate when);
    
    public Set<String> getFrontOfficeMemberUsernames(final FinantialEntity finantialEntity);

    public Set<String> getBackOfficeMemberUsernames(final FinantialEntity finantialEntity);

    /* Localized names */
    
    public String localizedNameOfDegreeType(final DegreeType degreeType);
    
    public String localizedNameOfStatuteType(final StatuteType statuteType);

    public String localizedNameOfStatuteType(StatuteType statuteType, Locale locale);
    
    public String localizedNameOfEnrolment(final Enrolment enrolment);

    public String localizedNameOfEnrolment(final Enrolment enrolment, final Locale locale);
    
    /* Student & Registration */
    
    default public RegistrationDataByExecutionYear findRegistrationDataByExecutionYear(final Registration registration, final ExecutionYear executionYear) {
        if(registration == null || executionYear == null) {
            return null;
        }
        
        return registration.getRegistrationDataByExecutionYearSet().stream()
                .filter(r -> r.getExecutionYear() == executionYear).findFirst().orElse(null);
    }
    
    
    public IngressionType ingression(final Registration registration);
    
    public RegistrationRegimeType registrationRegimeType(final Registration registration, final ExecutionYear executionYear);

    public Set<StatuteType> statutesTypesValidOnAnyExecutionSemesterFor(final Registration registration, final ExecutionInterval executionInterval);
    
    /* AdministrativeOffice */
    
    default public Stream<AdministrativeOffice> findAdministrativeOfficesByPredicate(Predicate<AdministrativeOffice> predicate) {
        return Bennu.getInstance().getAdministrativeOfficesSet().stream().filter(predicate);
    }


    
}
