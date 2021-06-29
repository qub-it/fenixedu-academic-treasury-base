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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.degree.DegreeType;
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

public interface IAcademicTreasuryPlatformDependentServices {

    /* **************
     * Read data sets 
     * ************** */

    Set<DegreeType> readAllDegreeTypes();

    Set<DegreeCurricularPlan> readAllDegreeCurricularPlansSet();

    Set<DegreeCurricularPlan> readDegreeCurricularPlansWithExecutionDegree(final ExecutionYear executionYear,
            final DegreeType degreeType);

    Set<CurricularYear> readAllCurricularYearsSet();

    Set<IngressionType> readAllIngressionTypesSet();

    Set<RegistrationProtocol> readAllRegistrationProtocol();

    Set<StatuteType> readAllStatuteTypesSet();

    Set<StatuteType> readAllStatuteTypesSet(boolean active);

    /* *************
     * Registrations 
     * ************* */

    Set<Registration> readAllRegistrations(RegistrationProtocol registrationProtocol);

    Set<Registration> readAllRegistrations(IngressionType ingressionType);
    
    /* ***********************
     * Person & PersonCustomer 
     * *********************** */
    
    Set<Person> readAllPersonsSet();
    
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

    PhysicalAddress createPhysicalAddress(Person person, Country countryOfResidence, String districtOfResidence,
            String districtSubdivisionOfResidence, String areaCode, String address);

    /* ******************
     * Fiscal Information 
     * ****************** */

    String fiscalCountry(final Person person);

    String fiscalNumber(final Person person);

    /* ***********
     * Permissions 
     * *********** */

    @Deprecated
    // TreasuryAccressControl is used for this purpose
    boolean isFrontOfficeMember(String username, FinantialEntity finantialEntity);

    @Deprecated
    // TreasuryAccressControl is used for this purpose
    boolean isBackOfficeMember(String username, FinantialEntity finantialEntity);

    @Deprecated
    // TreasuryAccressControl is used for this purpose
    boolean isAllowToModifySettlements(String username, FinantialEntity finantialEntity);

    @Deprecated
    // TreasuryAccressControl is used for this purpose
    boolean isAllowToModifyInvoices(String username, FinantialEntity finantialEntity);

    Set<Degree> readDegrees(FinantialEntity finantialEntity);

    FinantialEntity finantialEntityOfDegree(Degree degree, LocalDate when);

    Optional<FinantialEntity> finantialEntity(AdministrativeOffice administrativeOffice);

    Optional<FinantialEntity> finantialEntity(Unit unit);

    @Deprecated
    // TreasuryAccressControl is used for this purpose
    Set<String> getFrontOfficeMemberUsernames(final FinantialEntity finantialEntity);

    @Deprecated
    // TreasuryAccressControl is used for this purpose
    Set<String> getBackOfficeMemberUsernames(final FinantialEntity finantialEntity);

    /* ***************
     * Localized names 
     * *************** */

    String localizedNameOfDegreeType(DegreeType degreeType);

    String localizedNameOfDegreeType(DegreeType degreeType, Locale locale);

    String localizedNameOfStatuteType(StatuteType statuteType);

    String localizedNameOfStatuteType(StatuteType statuteType, Locale locale);

    String localizedNameOfEnrolment(Enrolment enrolment);

    String localizedNameOfEnrolment(Enrolment enrolment, Locale locale);

    String localizedNameOfAdministrativeOffice(AdministrativeOffice administrativeOffice);

    String localizedNameOfAdministrativeOffice(AdministrativeOffice administrativeOffice, Locale locale);

    /* **********************
     * Student & Registration 
     * ********************** */

    RegistrationDataByExecutionYear findRegistrationDataByExecutionYear(Registration registration, ExecutionYear executionYear);

    IngressionType ingression(Registration registration);

    RegistrationProtocol registrationProtocol(Registration registration);

    RegistrationRegimeType registrationRegimeType(Registration registration, ExecutionYear executionYear);

    Set<StatuteType> statutesTypesValidOnAnyExecutionSemesterFor(Registration registration, ExecutionInterval executionInterval);

    Stream<AdministrativeOffice> findAdministrativeOfficesByPredicate(Predicate<AdministrativeOffice> predicate);

    /* *******************
     * Execution Intervals 
     * ******************* */
    
    ExecutionInterval executionSemester(Enrolment enrolment);
    ExecutionInterval executionSemester(EnrolmentEvaluation enrolmentEvaluation);
    ExecutionYear executionYearOfExecutionSemester(ExecutionInterval executionInterval);
    Integer executionIntervalChildOrder(ExecutionInterval executionInterval);

}
