package org.fenixedu.academictreasury.base;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.qubit.terra.framework.services.ServiceProvider;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.PartyContactValidationState;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.curriculum.grade.GradeScale;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.degreeStructure.CurricularStage;
import org.fenixedu.academic.domain.person.IDDocumentType;
import org.fenixedu.academic.domain.person.IdDocumentTypeObject;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicCalendarRootEntry;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicInterval;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicIntervalCE;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicPeriod;
import org.fenixedu.academic.domain.time.calendarStructure.AcademicYearCE;
import org.fenixedu.academic.domain.treasury.ITreasuryBridgeAPI;
import org.fenixedu.academic.dto.person.PersonBean;
import org.fenixedu.academic.util.PeriodState;
import org.fenixedu.academictreasury.domain.reservationtax.ReservationTax;
import org.fenixedu.academictreasury.domain.reservationtax.ReservationTaxTariff;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.domain.treasury.AcademicTreasuryBridgeImpl;
import org.fenixedu.academictreasury.domain.tuition.exemptions.StatuteExemptionByIntervalMapEntry;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.util.AcademicTreasuryBootstrapper;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.UserProfile;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.VatExemptionReason;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryBootstrapper;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class BasicAcademicTreasuryUtils {

    public static void startup(Callable<?> startup) {
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                Locale.setDefault(new Locale("PT", "pt"));

                TreasuryPlataformDependentServicesFactory.registerImplementation(new TreasuryPlatformDependentServicesForTests());
                AcademicTreasuryPlataformDependentServicesFactory
                        .registerImplementation(new AcademicTreasuryPlatformDependentServicesForTests());

                ServiceProvider.registerService(ITreasuryBridgeAPI.class, AcademicTreasuryBridgeImpl.class);

                TreasuryBootstrapper.bootstrap("teste", "teste", "PT");
                AcademicTreasuryBootstrapper.bootstrap();

                startup.call();
                return null;
            }, new AtomicInstance(TxMode.WRITE, true));
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    public static Degree findOrCreateDegree() {
        if (Degree.find("9999") == null) {
            GradeScale.create("TYPE20", ls("Type 20"), new BigDecimal(0), new BigDecimal(9), new BigDecimal(10),
                    new BigDecimal(20), true, true);

            GradeScale numericGradeScale = GradeScale.getGradeScaleByCode("TYPE20");

            DegreeType degreeType = findOrCreateDegreeType();

            Degree degree = new Degree("Curso Teste", "Degree Test", "9999", degreeType, numericGradeScale, numericGradeScale,
                    findOrCreateExecutionYear("2022/2023"));

            degree.setCalendar(findOrCreateExecutionYear("2022/2023").getAcademicInterval().getAcademicCalendar());

            degree.createDegreeCurricularPlan("DCP", findOrCreatePerson(), AcademicPeriod.YEAR);
            degree.setCode("9999");
            degree.getDegreeCurricularPlansSet().iterator().next().setCurricularStage(CurricularStage.APPROVED);

            degree.getDegreeCurricularPlansSet().iterator().next().createExecutionDegree(findOrCreateExecutionYear("2022/2023"),
                    null, false);

            return degree;
        }

        return Degree.find("9999");
    }

    private static DegreeType findOrCreateDegreeType() {
        if (DegreeType.matching(dt -> "First Cycle".equals(dt.getName().getContent())).isEmpty()) {
            new DegreeType(ls("First Cycle"));
        }

        return DegreeType.matching(dt -> "First Cycle".equals(dt.getName().getContent())).get();
    }

    private static Person findOrCreatePerson() {
        if (Person.readAllPersons().isEmpty()) {
            if (IdDocumentTypeObject.readByIDDocumentType(IDDocumentType.OTHER) == null) {
                new IdDocumentTypeObject().setValue(IDDocumentType.OTHER);
            }

            PersonBean personBean = new PersonBean("Test person", "999999990", IDDocumentType.OTHER,
                    new LocalDate(2000, 1, 1).toDateTimeAtStartOfDay().toYearMonthDay());

            personBean.setGivenNames("Test");
            personBean.setFamilyNames("person");

            Person person = new Person(personBean);

            if (Country.readByTwoLetterCode("PT") == null) {
                new Country(ls("Portugal"), ls("Portugal"), "PT", "PRT");
            }

            PhysicalAddress physicalAddress =
                    new PhysicalAddress(person, PartyContactType.PERSONAL, true, "Desconhecido", "Desconhecido", "Desconhecido",
                            "Desconhecido", "Desconhecido", "Desconhecido", "Desconhecido", Country.readByTwoLetterCode("PT"));

            physicalAddress.setActive(true);
            physicalAddress.getPartyContactValidation().setState(PartyContactValidationState.VALID);

            person.editSocialSecurityNumber("999999990", physicalAddress);
        }

        return Person.readAllPersons().iterator().next();
    }

    public static ExecutionYear findOrCreateExecutionYear(String string) {
        if (ExecutionYear.readExecutionYearByName("2022/2023") != null) {
            return ExecutionYear.readExecutionYearByName("2022/2023");
        }

        AcademicCalendarRootEntry rootEntry = new AcademicCalendarRootEntry(ls("root calendar entry"), ls("root calendar entry"));

        AcademicCalendarEntry calendarYear = new AcademicYearCE(rootEntry, ls("2022/2023"), ls("2022/2023"),
                new LocalDate(2022, 9, 1).toDateTimeAtStartOfDay(), new LocalDate(2023, 8, 31).toDateTimeAtStartOfDay(),
                rootEntry);

//        AcademicCalendarEntry calendarEntry = new AcademicIntervalCE(AcademicPeriod.YEAR, rootEntry, ls(""), ls(""),
//                ,
//                rootEntry);

        AcademicInterval academicInterval = new AcademicInterval(calendarYear, rootEntry);
        ExecutionYear.readExecutionYearByName("2022/2023").setState(PeriodState.CURRENT);

        new AcademicIntervalCE(AcademicPeriod.SEMESTER, calendarYear, ls("1st Semester"), ls("1st Semester"),
                new LocalDate(2022, 9, 1).toDateTimeAtStartOfDay(), new LocalDate(2023, 2, 28).toDateTimeAtStartOfDay(),
                rootEntry);

        return ExecutionYear.readExecutionYearByName("2022/2023");
    }

    public static LocalizedString ls(String string) {
        return new LocalizedString(TreasuryConstants.DEFAULT_LANGUAGE, string);
    }

    public static Registration findOrCreateRegistration() {
        RegistrationStateType.findByCode(RegistrationStateType.REGISTERED_CODE).ifPresentOrElse(t -> {
        }, () -> {
            RegistrationStateType.create(RegistrationStateType.REGISTERED_CODE, ls("Registered"), true);
        });

        if (findOrCreateStudent().getRegistrationsFor(findOrCreateDegree()).isEmpty()) {
            Registration.create(findOrCreateStudent(), findOrCreateDegree().getDegreeCurricularPlansSet().iterator().next(),
                    findOrCreateExecutionYear("2022/2023"), findOrCreateRegistrationProtocol(), findOrCreateIngressionType());

        }

        return findOrCreateStudent().getRegistrationsFor(findOrCreateDegree()).iterator().next();
    }

    private static IngressionType findOrCreateIngressionType() {
        if (IngressionType.findByPredicate(t -> "PSA1".equals(t.getCode())).isEmpty()) {
            IngressionType.createIngressionType("PSA1", ls("default ingression"));
        }

        return IngressionType.findByPredicate(t -> "PSA1".equals(t.getCode())).get();
    }

    private static RegistrationProtocol findOrCreateRegistrationProtocol() {
        if (RegistrationProtocol.getDefault() == null) {
            RegistrationProtocol registrationProtocol = new RegistrationProtocol("NORMAL", ls("Normal"), true, true, false, false,
                    false, false, false, false, false, false, false);
            registrationProtocol.setDefaultStatus(true);
        }

        return RegistrationProtocol.getDefault();
    }

    private static Student findOrCreateStudent() {
        Person person = findOrCreatePerson();

        if (person.getStudent() == null) {
            new Student(findOrCreatePerson());
        }

        return person.getStudent();
    }

    public static void createReservationTaxes() {
        TreasuryExemptionType tet1 =
                TreasuryExemptionType.create("TET1", ls("Treasury Exemption 1"), new BigDecimal("12.21"), true);

        TreasuryExemptionType tet2 =
                TreasuryExemptionType.create("TET2", ls("Treasury Exemption 2"), new BigDecimal("100.00"), true);

        Product prt1 = Product.create(AcademicTreasurySettings.getInstance().getEmolumentsProductGroup(), "RT1",
                ls("Reservation Tax 1"), ls("Unit"), true, false, 0, VatType.findByCode("ISE"),
                List.of(FinantialInstitution.findAll().iterator().next()), VatExemptionReason.findByCode("M07"));

        Product prt2 = Product.create(AcademicTreasurySettings.getInstance().getEmolumentsProductGroup(), "RT2",
                ls("Reservation Tax 2"), ls("Unit"), true, false, 0, VatType.findByCode("ISE"),
                List.of(FinantialInstitution.findAll().iterator().next()), VatExemptionReason.findByCode("M07"));

        ReservationTax rt1 = ReservationTax.create("RT1", ls("Reservation Tax 1"), FinantialEntity.findAll().iterator().next(),
                prt1, true, true, false, ls("Reservation Tax 1"), tet1);

        ReservationTax rt2 = ReservationTax.create("RT2", ls("Reservation Tax 2"), FinantialEntity.findAll().iterator().next(),
                prt2, true, true, false, ls("Reservation Tax 2"), tet2);

        ExecutionYear.readNotClosedExecutionYears().forEach(ey -> {
            ReservationTaxTariff.create(rt1, ey, new BigDecimal("350.00"), DueDateCalculationType.DAYS_AFTER_CREATION, 1, null,
                    false, null, null).addDegrees(Degree.findAll().collect(Collectors.toSet()));

            ReservationTaxTariff.create(rt2, ey, new BigDecimal("100.00"), DueDateCalculationType.DAYS_AFTER_CREATION, 1, null,
                    false, null, null).addDegrees(Degree.findAll().collect(Collectors.toSet()));
        });

    }

    public static void createStatuteTypeExemptionsMap() {
        TreasuryExemptionType tet1 = TreasuryExemptionType.findByCode("TET1").iterator().next();
        
        TreasuryExemptionType tet3 =
                TreasuryExemptionType.create("TET3", ls("Treasury Exemption 3"), new BigDecimal("33.00"), true);

        TreasuryExemptionType tet4 =
                TreasuryExemptionType.create("TET4", ls("Treasury Exemption 4"), new BigDecimal("11.00"), true);

        StatuteType st3 = StatuteType.create("ST3", ls("Statute Type 3"));
        StatuteType st4 = StatuteType.create("ST4", ls("Statute Type 4"));
        StatuteType st5 = StatuteType.create("ST_SHARED", ls("Statute Type Shared"));

        ExecutionYear.readNotClosedExecutionYears().forEach(ey -> {
            StatuteExemptionByIntervalMapEntry.create(FinantialEntity.findAll().iterator().next(), ey, st3, tet3);
            StatuteExemptionByIntervalMapEntry.create(FinantialEntity.findAll().iterator().next(), ey, st4, tet4);
            StatuteExemptionByIntervalMapEntry.create(FinantialEntity.findAll().iterator().next(), ey, st5, tet1);
        });

        TreasuryExemptionType tet5
                = TreasuryExemptionType.create("TET5", ls("Treasury Exemption 5"), new BigDecimal("25"), true);
    }

}