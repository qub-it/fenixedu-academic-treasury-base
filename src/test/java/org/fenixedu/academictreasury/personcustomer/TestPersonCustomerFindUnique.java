package org.fenixedu.academictreasury.personcustomer;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academictreasury.base.FenixFrameworkRunner;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryBootstrapper;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.CustomerType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class TestPersonCustomerFindUnique {

    @BeforeClass
    public static void init() {
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                new Country(new LocalizedString(new Locale("pt"), "Portugal"), new LocalizedString(new Locale("pt"), "Portugal"),
                        "PT", "PRT");

                org.fenixedu.academic.domain.EnrolmentTest.initEnrolments();

                org.fenixedu.academictreasury.tuition.TuitionPaymentPlanTestsUtilities.startUp();

                AcademicTreasuryBootstrapper.bootstrap();

                createVariousPersonCustomers();

                return null;
            }, new AtomicInstance(TxMode.WRITE, true));
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private static void createVariousPersonCustomers() {
        Person person = Student.readStudentByNumber(1).getPerson();
        PhysicalAddress physicalAddress = createDefaultPhysicalAddress(person);

        person.editSocialSecurityNumber("504024850", physicalAddress);
        person.editSocialSecurityNumber("999999990", physicalAddress);
        person.editSocialSecurityNumber("123456789", physicalAddress);

        createInactivePersonCustomer(person, physicalAddress, "999999990");
        createInactivePersonCustomer(person, physicalAddress, "999999990");
        createInactivePersonCustomer(person, physicalAddress, "999999990");
        createInactivePersonCustomer(person, physicalAddress, "504024850");
        createInactivePersonCustomer(person, physicalAddress, "504024850");
        createInactivePersonCustomer(person, physicalAddress, "504024850");
        createInactivePersonCustomer(person, physicalAddress, "504024850");
        createInactivePersonCustomer(person, physicalAddress, "123456789");
        createInactivePersonCustomer(person, physicalAddress, "123456789");
        createInactivePersonCustomer(person, physicalAddress, "123456789");
        createInactivePersonCustomer(person, physicalAddress, "507113810");
        createInactivePersonCustomer(person, physicalAddress, "507113810");
    }

    public static PersonCustomer createInactivePersonCustomer(Person person, PhysicalAddress fiscalAddress, String fiscalNumber) {
        final PersonCustomer result = new PersonCustomer();

        result.setPersonForInactivePersonCustomer(person);
        result.setFromPersonMerge(true);

        result.setCustomerType(CustomerType.findByCode("STUDENT").iterator().next());
        result.setAddressCountryCode(fiscalAddress.getCountryOfResidence().getCode());
        result.setFiscalNumber(fiscalNumber);

        result.checkRules();

        return result;
    }

    @Test
    public void testChangeCustomerFromPersonMerge() {
        Person person = Student.readStudentByNumber(1).getPerson();
        PhysicalAddress physicalAddress = person.getPhysicalAddresses().stream()
                .filter(a -> "PT".equals(PersonCustomer.addressCountryCode(a))).findFirst().get();

        person.editSocialSecurityNumber("507113810", physicalAddress);

        assertEquals(true, PersonCustomer.findUnique(person, "PT", "507113810").get().isFromPersonMerge());

        assertEquals(Optional.of(person.getPersonCustomer()), PersonCustomer.findUnique(person, "PT", "507113810"));
    }

    @Test
    public void testIsNotFromPersonMerge() {
        Person person = Student.readStudentByNumber(1).getPerson();
        PhysicalAddress physicalAddress = person.getPhysicalAddresses().stream()
                .filter(a -> "PT".equals(PersonCustomer.addressCountryCode(a))).findFirst().get();

        person.editSocialSecurityNumber("504024850", physicalAddress);

        assertEquals(false, PersonCustomer.findUnique(person, "PT", "504024850").get().isFromPersonMerge());

        person.editSocialSecurityNumber("999999990", physicalAddress);

        assertEquals(false, PersonCustomer.findUnique(person, "PT", "999999990").get().isFromPersonMerge());

        person.editSocialSecurityNumber("123456789", physicalAddress);

        assertEquals(false, PersonCustomer.findUnique(person, "PT", "123456789").get().isFromPersonMerge());
    }

    @Test
    public void testChangeNIF() {
        Person person = Student.readStudentByNumber(1).getPerson();
        PhysicalAddress physicalAddress = person.getPhysicalAddresses().stream()
                .filter(a -> "PT".equals(PersonCustomer.addressCountryCode(a))).findFirst().get();

        person.editSocialSecurityNumber("503021202", physicalAddress);

        assertEquals(Optional.of(person.getPersonCustomer()), PersonCustomer.findUnique(person, "PT", "503021202"));

        person.editSocialSecurityNumber("999999990", physicalAddress);

        assertEquals(Optional.of(person.getPersonCustomer()), PersonCustomer.findUnique(person, "PT", "999999990"));

        person.editSocialSecurityNumber("123456789", physicalAddress);

        assertEquals(Optional.of(person.getPersonCustomer()), PersonCustomer.findUnique(person, "PT", "123456789"));
    }

    @Test
    public void testFindUnique() {
        Person person = Student.readStudentByNumber(1).getPerson();

        List.of("504024850", "999999990", "123456789").forEach(nif -> {

            Optional<? extends PersonCustomer> opt1 = PersonCustomer.findUnique(person, "PT", nif);
            Optional<? extends PersonCustomer> opt2 =
                    _findUnique(person, "PT", nif, PersonCustomer.OLD_AND_DEPRECATED_SORT_BY_PERSON_MERGE);

            assertEquals(opt1, opt2);
        });

        assertEquals(Optional.of(person.getPersonCustomer()), PersonCustomer.findUnique(person, "PT", "123456789"));
    }

    @Test
    public void testFindAndSortList() {
        Person person = Student.readStudentByNumber(1).getPerson();

        List.of("504024850", "999999990", "123456789").forEach(nif -> {

            var listA = PersonCustomer.find(person, "PT", nif).sorted(PersonCustomer.IMPROVED_SORT_BY_PERSON_MERGE)
                    .collect(Collectors.toList());

            var listB = PersonCustomer.find(person, "PT", nif).sorted(PersonCustomer.OLD_AND_DEPRECATED_SORT_BY_PERSON_MERGE)
                    .collect(Collectors.toList());

            assertEquals(listB, listA);
        });

    }

    private Optional<? extends PersonCustomer> _findUnique(final Person person, final String fiscalCountryCode,
            final String fiscalNumber, Comparator<PersonCustomer> personComparator) {
        return PersonCustomer.find(person, fiscalCountryCode, fiscalNumber).sorted(personComparator).findFirst();
    }

    private static PhysicalAddress createDefaultPhysicalAddress(final Person person) {
        IAcademicTreasuryPlatformDependentServices implementation =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        PhysicalAddress result = implementation.createPhysicalAddress(person, Country.readByTwoLetterCode("PT"), "unknownAddress",
                "unknownAddress", "0000-000", "unknownAddress");
        result.setValid();

        return result;

    }

}
