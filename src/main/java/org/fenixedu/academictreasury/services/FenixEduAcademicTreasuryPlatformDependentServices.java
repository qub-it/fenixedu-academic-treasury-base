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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionDegree;
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
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

public class FenixEduAcademicTreasuryPlatformDependentServices implements IAcademicTreasuryPlatformDependentServices {

    /* **************
     * Read data sets
     * ************** */

    @Override
    public Set<DegreeType> readAllDegreeTypes() {
        return DegreeType.all().collect(Collectors.toSet());
    }

    @Override
    public Set<DegreeCurricularPlan> readAllDegreeCurricularPlansSet() {
        return Degree.readAllMatching((dt) -> true).stream().flatMap(d -> d.getDegreeCurricularPlansSet().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<DegreeCurricularPlan> readDegreeCurricularPlansWithExecutionDegree(final ExecutionYear executionYear,
            final DegreeType degreeType) {
        return ExecutionDegree.getAllByExecutionYearAndDegreeType(executionYear, degreeType).stream()
                .map(e -> e.getDegreeCurricularPlan()).collect(Collectors.toSet());
    }

    @Override
    public Set<CurricularYear> readAllCurricularYearsSet() {
        final Set<CurricularYear> result = new HashSet<>();

        for (int i = 1; i <= 10; i++) {
            if (CurricularYear.readByYear(i) == null) {
                return result;
            }

            result.add(CurricularYear.readByYear(i));
        }

        return result;
    }

    @Override
    public Set<IngressionType> readAllIngressionTypesSet() {
        return IngressionType.findAllByPredicate((i) -> true).collect(Collectors.toSet());
    }

    @Override
    public Set<RegistrationProtocol> readAllRegistrationProtocol() {
        return RegistrationProtocol.findByPredicate((p) -> true).collect(Collectors.toSet());
    }

    @Override
    public Set<StatuteType> readAllStatuteTypesSet() {
        return StatuteType.readAll((s) -> true).collect(Collectors.toSet());
    }

    @Override
    public Set<StatuteType> readAllStatuteTypesSet(boolean active) {
        return readAllStatuteTypesSet().stream().filter(s -> s.getActive() == active).collect(Collectors.toSet());
    }

    /* *************
     * Registrations
     * ************* */

    @Override
    public Set<Registration> readAllRegistrations(RegistrationProtocol registrationProtocol) {
        return registrationProtocol.getRegistrationsSet();
    }

    @Override
    public Set<Registration> readAllRegistrations(IngressionType ingressionType) {
        return ingressionType.getRegistrationSet();
    }

    /* ***********************
     * Person & PersonCustomer
     * *********************** */

    @Override
    public Set<Person> readAllPersonsSet() {
        return Person.readAllPersons();
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
        return person.getIban();
    }

    @Override
    public Set<AcademicTreasuryEvent> academicTreasuryEventsSet(Person person) {
        return person.getAcademicTreasuryEventSet();
    }

    @Override
    public String defaultPhoneNumber(Person person) {
        return person.getDefaultPhoneNumber();
    }

    @Override
    public String defaultMobilePhoneNumber(Person person) {
        return person.getDefaultMobilePhoneNumber();
    }

    @Override
    public List<PhysicalAddress> pendingOrValidPhysicalAddresses(Person person) {
        Comparator<? super PhysicalAddress> comparator = (o1, o2) -> {
            if (o1.isValid() && !o2.isValid()) {
                return -1;
            } else if (!o1.isValid() && o2.isValid()) {
                return 1;
            }

            return 10 * PhysicalAddress.COMPARATOR_BY_ADDRESS.compare(o1, o2) + o1.getExternalId().compareTo(o2.getExternalId());
        };

        return person.getAllPartyContacts(PhysicalAddress.class).stream().map(PhysicalAddress.class::cast)
                .filter(pc -> pc.isValid()).sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public List<? extends PartyContact> pendingOrValidPartyContacts(Person person,
            Class<? extends PartyContact> partyContactType) {
        Comparator<? super PartyContact> comparator = (o1, o2) -> {
            if (o1.isValid() && !o2.isValid()) {
                return -1;
            } else if (!o1.isValid() && o2.isValid()) {
                return 1;
            }

            return o1.getExternalId().compareTo(o2.getExternalId());
        };

        return person.getAllPartyContacts(partyContactType).stream().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public void editSocialSecurityNumber(Person person, String fiscalNumber, PhysicalAddress fiscalAddress) {
        person.editSocialSecurityNumber(fiscalNumber, fiscalAddress);
    }

    @Override
    public void setFiscalAddress(PhysicalAddress physicalAddress, boolean fiscalAddress) {
        physicalAddress.setFiscalAddress(fiscalAddress);
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

    /* ******************
     * Fiscal Information
     * ****************** */

    @Override
    public String fiscalCountry(final Person person) {
        return person.getFiscalAddress() != null && person.getFiscalAddress().getCountryOfResidence() != null ? person
                .getFiscalAddress().getCountryOfResidence().getCode() : null;
    }

    @Override
    public String fiscalNumber(final Person person) {
        return person.getSocialSecurityNumber();
    }

    /* ***********
     * Permissions
     * *********** */

    @Override
    @Deprecated
    public boolean isFrontOfficeMember(String username, FinantialEntity finantialEntity) {
        throw new RuntimeException("deprecated");
    }

    @Override
    @Deprecated
    public boolean isBackOfficeMember(String username, FinantialEntity finantialEntity) {
        throw new RuntimeException("deprecated");
    }

    @Override
    @Deprecated
    public boolean isAllowToModifySettlements(String username, FinantialEntity finantialEntity) {
        throw new RuntimeException("deprecated");
    }

    @Override
    @Deprecated
    public boolean isAllowToModifyInvoices(String username, FinantialEntity finantialEntity) {
        throw new RuntimeException("deprecated");
    }

    @Override
    public Set<Degree> readDegrees(FinantialEntity finantialEntity) {
        if (finantialEntity.getUnit() != null) {
            return finantialEntity.getUnit().getAllSubUnits().stream() //
                    .filter(u -> u.isDegreeUnit()) //
                    .filter(u -> u.getDegree() != null) //
                    .map(u -> u.getDegree()) //
                    .collect(Collectors.toSet()); //
        }

        return Collections.emptySet();
    }

    @Override
    /*
     * This method returns the Finantial Entity responsible for the degree.
     * 
     * This method is used to get the academic tariffs associated with the Finantial Entity.
     * Also it is used to get the finantial institution, in order to know the debt account associated
     * with the customer.
     * 
     * For tuitions it is different, in the sense that the TuitionPaymentPlan is obtained with
     * DegreeCurricularPlan. With the TuitionPaymentPlan we get the FinantialEntity.
     * 
     */
    public FinantialEntity finantialEntityOfDegree(Degree degree, LocalDate when) {
        // Look at the organizational structure
        Unit degreeUnit = degree.getUnit();

        if (degreeUnit == null) {
            return null;
        }

        List<FinantialEntity> candidateFinantialEntities =
                degreeUnit.getAllParentUnits().stream().map(parent -> parent.getFinantialEntity()).filter(Objects::nonNull)
                        .sorted(FinantialEntity.COMPARE_BY_NAME).collect(Collectors.toList());

        if (candidateFinantialEntities.size() == 1) {
            // There is no ambiguity. The degree descendent of one unit
            // associated with the finantial entity

            return candidateFinantialEntities.iterator().next();
        } else if (candidateFinantialEntities.size() > 1) {
            // The degree is descendent of more than one unit
            // associated with the finantial entity
            //
            // We need to untie and find which finantial entity
            // is responsible for finantial entity

            // TODO: Find with a specific accountability type?
            // It is not desirable to return the first ordered
            // alphabetically

            return candidateFinantialEntities.iterator().next();
        }

        return null;
    }

    @Override
    public Optional<FinantialEntity> finantialEntity(Unit unit) {
        return Optional.ofNullable(unit.getFinantialEntity());
    }

    @Override
    @Deprecated
    public Set<String> getFrontOfficeMemberUsernames(final FinantialEntity finantialEntity) {
        throw new RuntimeException("deprecated");
    }

    @Override
    @Deprecated
    public Set<String> getBackOfficeMemberUsernames(final FinantialEntity finantialEntity) {
        throw new RuntimeException("deprecated");
    }

    /* ***************
     * Localized names
     * *************** */

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
        return localizedNameOfEnrolment(enrolment, I18N.getLocale());
    }

    @Override
    public String localizedNameOfEnrolment(Enrolment enrolment, Locale locale) {
        return enrolment.getName().getContent(locale);
    }

    /* **********************
     * Student & Registration
     * ********************** */

    @Override
    public RegistrationDataByExecutionYear findRegistrationDataByExecutionYear(Registration registration,
            ExecutionYear executionYear) {
        return registration.getRegistrationDataByExecutionYearSet().stream().filter(rd -> rd.getExecutionYear() == executionYear)
                .findAny().orElse(null);
    }

    @Override
    public IngressionType ingression(Registration registration) {
        return registration.getIngressionType();
    }

    @Override
    public RegistrationProtocol registrationProtocol(Registration registration) {
        return registration.getRegistrationProtocol();
    }

    @Override
    public RegistrationRegimeType registrationRegimeType(Registration registration, ExecutionYear executionYear) {
        return registration.getRegimeType(executionYear);
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

    /* *******************
     * Execution Intervals
     * ******************* */

    @Override
    public ExecutionInterval executionSemester(Enrolment enrolment) {
        return enrolment.getExecutionInterval();
    }

    @Override
    public ExecutionInterval executionSemester(EnrolmentEvaluation enrolmentEvaluation) {
        return enrolmentEvaluation.getExecutionInterval();
    }

    @Override
    public ExecutionYear executionYearOfExecutionSemester(ExecutionInterval executionInterval) {
        return executionInterval.getExecutionYear();
    }

    @Override
    public Integer executionIntervalChildOrder(ExecutionInterval executionInterval) {
        return executionInterval.getChildOrder();
    }

    @Override
    public ExecutionInterval getExecutionIntervalByName(String s) {
        return ExecutionInterval.getExecutionInterval(s);
    }

}
