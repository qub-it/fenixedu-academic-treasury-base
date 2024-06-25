package org.fenixedu.academictreasury.base;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.contacts.PhysicalAddressData;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

public class AcademicTreasuryPlatformDependentServicesForTests implements IAcademicTreasuryPlatformDependentServices {

    @Override
    public Set<DegreeType> readAllDegreeTypes() {
        return DegreeType.all().collect(Collectors.toSet());
    }

    @Override
    public Set<DegreeCurricularPlan> readAllDegreeCurricularPlansSet() {
        return DegreeCurricularPlan.readBolonhaDegreeCurricularPlans();
    }

    @Override
    public Set<DegreeCurricularPlan> readDegreeCurricularPlansWithExecutionDegree(ExecutionYear executionYear,
            DegreeType degreeType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<CurricularYear> readAllCurricularYearsSet() {
        return Collections.emptySet();
    }

    @Override
    public Set<IngressionType> readAllIngressionTypesSet() {
        return Collections.emptySet();
    }

    @Override
    public Set<RegistrationProtocol> readAllRegistrationProtocol() {
        return Collections.emptySet();
    }

    @Override
    public Set<StatuteType> readAllStatuteTypesSet() {
        return Collections.emptySet();
    }

    @Override
    public Set<StatuteType> readAllStatuteTypesSet(boolean active) {
        return Collections.emptySet();
    }

    @Override
    public Set<Registration> readAllRegistrations(RegistrationProtocol registrationProtocol) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Registration> readAllRegistrations(IngressionType ingressionType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Person> readAllPersonsSet() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PersonCustomer personCustomer(Person person) {
        return person.getPersonCustomer();
    }

    @Override
    public Set<PersonCustomer> inactivePersonCustomers(Person person) {
        return person.getInactivePersonCustomersSet();
    }

    @Override
    public PhysicalAddress fiscalAddress(Person person) {
        return person.getFiscalAddress();
    }

    @Override
    public String iban(Person person) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<AcademicTreasuryEvent> academicTreasuryEventsSet(Person person) {
        return person.getAcademicTreasuryEventSet();
    }

    @Override
    public String defaultPhoneNumber(Person person) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String defaultMobilePhoneNumber(Person person) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<PhysicalAddress> pendingOrValidPhysicalAddresses(Person person) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<? extends PartyContact> pendingOrValidPartyContacts(Person person,
            Class<? extends PartyContact> partyContactType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void editSocialSecurityNumber(Person person, String fiscalNumber, PhysicalAddress fiscalAddress) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setFiscalAddress(PhysicalAddress physicalAddress, boolean fiscalAddress) {
        // TODO Auto-generated method stub

    }

    @Override
    public PhysicalAddress createPhysicalAddress(Person person, Country countryOfResidence, String districtOfResidence,
            String districtSubdivisionOfResidence, String areaCode, String address) {
        PhysicalAddressData data = new PhysicalAddressData();

        data.setAddress(address);
        data.setCountryOfResidence(countryOfResidence);
        data.setDistrictOfResidence(districtOfResidence);
        data.setDistrictSubdivisionOfResidence(districtSubdivisionOfResidence);
        data.setAreaCode(areaCode);

        final PhysicalAddress physicalAddress =
                PhysicalAddress.createPhysicalAddress(person, data, PartyContactType.PERSONAL, false);

        physicalAddress.setValid();

        return physicalAddress;
    }

    @Override
    public String fiscalCountry(Person person) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String fiscalNumber(Person person) {
        return person.getSocialSecurityNumber();
    }

    @Override
    public boolean isFrontOfficeMember(String username, FinantialEntity finantialEntity) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isBackOfficeMember(String username, FinantialEntity finantialEntity) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAllowToModifySettlements(String username, FinantialEntity finantialEntity) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAllowToModifyInvoices(String username, FinantialEntity finantialEntity) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<Degree> readDegrees(FinantialEntity finantialEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FinantialEntity finantialEntityOfDegree(Degree degree, LocalDate when) {
        return FinantialEntity.findAll().iterator().next();
    }

    @Override
    public Optional<FinantialEntity> finantialEntity(Unit unit) {
        return FinantialEntity.findAll().findFirst();
    }

    @Override
    public Set<String> getFrontOfficeMemberUsernames(FinantialEntity finantialEntity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getBackOfficeMemberUsernames(FinantialEntity finantialEntity) {
        // TODO Auto-generated method stub
        return null;
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

    @Override
    public String localizedNameOfEnrolment(Enrolment enrolment) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String localizedNameOfEnrolment(Enrolment enrolment, Locale locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RegistrationDataByExecutionYear findRegistrationDataByExecutionYear(Registration registration,
            ExecutionYear executionYear) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IngressionType ingression(Registration registration) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RegistrationProtocol registrationProtocol(Registration registration) {
        return registration.getRegistrationProtocol();
    }

    @Override
    public RegistrationRegimeType registrationRegimeType(Registration registration, ExecutionYear executionYear) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<StatuteType> statutesTypesValidOnAnyExecutionSemesterFor(Registration registration,
            ExecutionInterval executionInterval) {
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

    @Override
    public ExecutionInterval executionSemester(Enrolment enrolment) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExecutionInterval executionSemester(EnrolmentEvaluation enrolmentEvaluation) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExecutionYear executionYearOfExecutionSemester(ExecutionInterval executionInterval) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Integer executionIntervalChildOrder(ExecutionInterval executionInterval) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExecutionInterval getExecutionIntervalByName(String s) {
        // TODO Auto-generated method stub
        return null;
    }

}