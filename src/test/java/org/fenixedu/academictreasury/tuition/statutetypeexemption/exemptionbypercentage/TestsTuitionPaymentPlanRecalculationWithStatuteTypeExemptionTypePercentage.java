package org.fenixedu.academictreasury.tuition.statutetypeexemption.exemptionbypercentage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.StudentStatute;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.base.BasicAcademicTreasuryUtils;
import org.fenixedu.academictreasury.base.FenixFrameworkRunner;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.reservationtax.ReservationTax;
import org.fenixedu.academictreasury.domain.reservationtax.ReservationTaxEventTarget;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.domain.tuition.EctsCalculationType;
import org.fenixedu.academictreasury.domain.tuition.TuitionCalculationType;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.academictreasury.dto.tariff.AcademicTariffBean;
import org.fenixedu.academictreasury.dto.tariff.TuitionPaymentPlanBean;
import org.fenixedu.academictreasury.services.tuition.RegistrationTuitionService;
import org.fenixedu.academictreasury.tuition.TuitionPaymentPlanTestsUtilities;
import org.fenixedu.academictreasury.util.AcademicTreasuryBootstrapper;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.VatExemptionReason;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class TestsTuitionPaymentPlanRecalculationWithStatuteTypeExemptionTypePercentage {

    private static Registration registration;
    private static ExecutionInterval executionInterval;
    private static ExecutionYear executionYear;

    @BeforeClass
    public static void init() {
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                org.fenixedu.academic.domain.EnrolmentTest.initEnrolments();

                TuitionPaymentPlanTestsUtilities.startUp();
                AcademicTreasuryBootstrapper.bootstrap();
                BasicAcademicTreasuryUtils.createReservationTaxes();
                BasicAcademicTreasuryUtils.createStatuteTypeExemptionsMap();
                createTuitionPaymentPlanWithAmountByEcts();

                return null;
            }, new AtomicInstance(TxMode.WRITE, true));
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private static TuitionPaymentPlan createTuitionPaymentPlanWithAmountByEcts() {
        registration = Student.readStudentByNumber(1).getRegistrationStream().findAny().orElseThrow();
        final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();

        executionInterval = ExecutionInterval.findFirstCurrentChild(scp.getDegree().getCalendar());
        executionYear = executionInterval.getExecutionYear();

        TuitionPaymentPlanBean bean = new TuitionPaymentPlanBean(
                TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get().getCurrentProduct(),
                TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get(), readFinantialEntity(), executionYear);

        bean.setDefaultPaymentPlan(true);

        DegreeCurricularPlan degreeCurricularPlan = registration.getLastStudentCurricularPlan().getDegreeCurricularPlan();
        bean.addDegreeCurricularPlans(degreeCurricularPlan);
        bean.setDegreeType(degreeCurricularPlan.getDegreeType());

        {
            AcademicTariffBean academicTariffBean = new AcademicTariffBean(1);
            academicTariffBean.setTuitionInstallmentProduct(Product.findUniqueByCode("PROP_1_PREST_1_CIC").get());
            academicTariffBean.setTuitionCalculationType(TuitionCalculationType.ECTS);
            academicTariffBean.setEctsCalculationType(EctsCalculationType.FIXED_AMOUNT);
            academicTariffBean.setFixedAmount(new BigDecimal("10"));
            academicTariffBean.setBeginDate(executionYear.getBeginLocalDate());
            academicTariffBean.setDueDateCalculationType(DueDateCalculationType.DAYS_AFTER_CREATION);
            academicTariffBean.setNumberOfDaysAfterCreationForDueDate(7);
            academicTariffBean.setInterestRateType(null);

            bean.getTuitionInstallmentBeans().add(academicTariffBean);
        }

        {
            AcademicTariffBean academicTariffBean = new AcademicTariffBean(2);
            academicTariffBean.setTuitionInstallmentProduct(Product.findUniqueByCode("PROP_2_PREST_1_CIC").get());
            academicTariffBean.setTuitionCalculationType(TuitionCalculationType.ECTS);
            academicTariffBean.setEctsCalculationType(EctsCalculationType.FIXED_AMOUNT);
            academicTariffBean.setFixedAmount(new BigDecimal("10"));
            academicTariffBean.setBeginDate(executionYear.getBeginLocalDate());
            academicTariffBean.setDueDateCalculationType(DueDateCalculationType.DAYS_AFTER_CREATION);
            academicTariffBean.setNumberOfDaysAfterCreationForDueDate(30);
            academicTariffBean.setInterestRateType(null);

            bean.getTuitionInstallmentBeans().add(academicTariffBean);
        }

        {
            AcademicTariffBean academicTariffBean = new AcademicTariffBean(3);
            academicTariffBean.setTuitionInstallmentProduct(Product.findUniqueByCode("PROP_3_PREST_1_CIC").get());
            academicTariffBean.setTuitionCalculationType(TuitionCalculationType.ECTS);
            academicTariffBean.setEctsCalculationType(EctsCalculationType.FIXED_AMOUNT);
            academicTariffBean.setFixedAmount(new BigDecimal("10"));
            academicTariffBean.setBeginDate(executionYear.getBeginLocalDate());
            academicTariffBean.setDueDateCalculationType(DueDateCalculationType.DAYS_AFTER_CREATION);
            academicTariffBean.setNumberOfDaysAfterCreationForDueDate(60);
            academicTariffBean.setInterestRateType(null);

            bean.getTuitionInstallmentBeans().add(academicTariffBean);
        }

        {
            AcademicTariffBean academicTariffBean = new AcademicTariffBean(4);
            academicTariffBean.setTuitionInstallmentProduct(Product.findUniqueByCode("PROP_4_PREST_1_CIC").get());
            academicTariffBean.setTuitionCalculationType(TuitionCalculationType.ECTS);
            academicTariffBean.setEctsCalculationType(EctsCalculationType.FIXED_AMOUNT);
            academicTariffBean.setFixedAmount(new BigDecimal("10"));
            academicTariffBean.setBeginDate(executionYear.getBeginLocalDate());
            academicTariffBean.setDueDateCalculationType(DueDateCalculationType.DAYS_AFTER_CREATION);
            academicTariffBean.setNumberOfDaysAfterCreationForDueDate(90);
            academicTariffBean.setInterestRateType(null);

            bean.getTuitionInstallmentBeans().add(academicTariffBean);
        }

        return TuitionPaymentPlan.create(bean);
    }

    private static void ensureNecessaryAcademicDataIsAvailable() {
        registration.getRegistrationProtocol().setPayGratuity(true);

        if (Country.readByTwoLetterCode("PT") == null) {
            new Country(ls("Portugal"), ls("Portugal"), "PT", "PRT").setDefaultCountry(true);
        }

        registration.getPerson().editSocialSecurityNumber("999999990",
                TreasuryBridgeAPIFactory.implementation().createSaftDefaultPhysicalAddress(registration.getPerson()));
    }

    @Test
    public void tuitionPaymentPlanWithReservationTaxExemption() {
        createTuitionPaymentPlanWithAmountByEcts();
        ensureNecessaryAcademicDataIsAvailable();

        new StudentStatute(registration.getStudent(), StatuteType.findByCode("ST3").get(), executionInterval,
                executionInterval.getNext(), null, null, null, registration);

        Product firstInstallmentProduct = Product.findUniqueByCode("PROP_1_PREST_1_CIC").get();
        Product secondInstallmentProduct = Product.findUniqueByCode("PROP_2_PREST_1_CIC").get();

        RegistrationTuitionService.startServiceInvocation(registration, executionYear, new LocalDate()) //
                .applyEnrolledEctsUnits(new BigDecimal("30")) //
                .applyEnrolledCoursesCount(new BigDecimal("5")) //
                .withInferedTuitionPaymentPlan() //
                .restrictForInstallmentProducts(Set.of(firstInstallmentProduct)) //
                .withoutInstallmentsRecalculation() //
                .executeTuitionPaymentPlanCreation();

        AcademicTreasuryEvent academicTreasuryEvent =
                AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).get();

        assertEquals(new BigDecimal("201.00"), academicTreasuryEvent.getAmountWithVatToPay());
        assertEquals(new BigDecimal("99.00"), academicTreasuryEvent.getNetExemptedAmount(firstInstallmentProduct).setScale(2));

        RegistrationTuitionService.startServiceInvocation(registration, executionYear, new LocalDate())
                .applyEnrolledEctsUnits(new BigDecimal("30")) //
                .applyEnrolledCoursesCount(new BigDecimal("5")) //
                .withInferedTuitionPaymentPlan() //
                .withAllInstallments() //
                .forceInstallmentsEvenTreasuryEventIsCharged(true) //
                .recalculateInstallments(Map.of(firstInstallmentProduct, new LocalDate())) //
                .executeTuitionPaymentPlanCreation();

        assertEquals(new BigDecimal("804.00"), academicTreasuryEvent.getAmountWithVatToPay());
        assertEquals(new BigDecimal("396.00"), academicTreasuryEvent.getNetExemptedAmount());
        assertEquals(new BigDecimal("99.00"), academicTreasuryEvent.getNetExemptedAmount(secondInstallmentProduct));

        TreasuryExemptionType usedTreasuryExemptionType = TreasuryExemptionType.findByCode("TET3").findFirst().get();
        assertTrue("Treasury exemption type used", academicTreasuryEvent.getTreasuryExemptionsSet().stream()
                .allMatch(e -> e.getTreasuryExemptionType() == usedTreasuryExemptionType));
    }

    private static FinantialEntity readFinantialEntity() {
        return FinantialEntity.findAll().iterator().next();
    }

    public static LocalizedString ls(String string) {
        return new LocalizedString(TreasuryConstants.DEFAULT_LANGUAGE, string);
    }

}