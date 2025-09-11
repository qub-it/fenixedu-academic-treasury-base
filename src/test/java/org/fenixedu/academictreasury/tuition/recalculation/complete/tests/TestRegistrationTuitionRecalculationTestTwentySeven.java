package org.fenixedu.academictreasury.tuition.recalculation.complete.tests;

import org.fenixedu.academic.domain.*;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.base.BasicAcademicTreasuryUtils;
import org.fenixedu.academictreasury.base.FenixFrameworkRunner;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.reservationtax.ReservationTax;
import org.fenixedu.academictreasury.domain.reservationtax.ReservationTaxEventTarget;
import org.fenixedu.academictreasury.domain.tuition.EctsCalculationType;
import org.fenixedu.academictreasury.domain.tuition.TuitionCalculationType;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.academictreasury.dto.tariff.AcademicTariffBean;
import org.fenixedu.academictreasury.dto.tariff.TuitionPaymentPlanBean;
import org.fenixedu.academictreasury.services.tuition.RegistrationTuitionService;
import org.fenixedu.academictreasury.util.AcademicTreasuryBootstrapper;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(FenixFrameworkRunner.class)
public class TestRegistrationTuitionRecalculationTestTwentySeven {

    private static Registration registration;
    private static ExecutionInterval executionInterval;
    private static ExecutionYear executionYear;

    @BeforeClass
    public static void init() {
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                EnrolmentTest.initEnrolments();

                org.fenixedu.academictreasury.tuition.TuitionPaymentPlanTestsUtilities.startUp();
                AcademicTreasuryBootstrapper.bootstrap();
                BasicAcademicTreasuryUtils.createReservationTaxes();
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
    public void doRecalculation() {
        FinantialInstitution.findAll().iterator().next().setSupportCreditTreasuryExemptions(true);
        FinantialInstitution.findAll().iterator().next().setSplitDebitEntriesWithSettledAmount(true);
        FinantialInstitution.findAll().iterator().next().setSplitCreditEntriesWithSettledAmount(true);

        createTuitionPaymentPlanWithAmountByEcts();
        ensureNecessaryAcademicDataIsAvailable();

        ReservationTaxEventTarget.createReservationTaxDebt(
                ReservationTax.findUniqueActiveByProduct(Product.findUniqueByCode("RT1").get()).get(), registration.getPerson(),
                registration.getLastStudentCurricularPlan().getDegreeCurricularPlan(), executionYear, new LocalDate());

        Product firstInstallmentProduct = Product.findUniqueByCode("PROP_1_PREST_1_CIC").get();
        RegistrationTuitionService.startServiceInvocation(registration, executionYear, new LocalDate()) //
                .applyEnrolledEctsUnits(new BigDecimal("40")) //
                .applyEnrolledCoursesCount(new BigDecimal("5")) //
                .withInferedTuitionPaymentPlan() //
                .restrictForInstallmentProducts(Set.of(firstInstallmentProduct)) //
                .withoutInstallmentsRecalculation() //
                .executeTuitionPaymentPlanCreation();

        AcademicTreasuryEvent academicTreasuryEvent =
                AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).get();

        assertEquals(new BigDecimal("50.00"), academicTreasuryEvent.getAmountWithVatToPay());
        assertEquals(new BigDecimal("350.00"), academicTreasuryEvent.getNetExemptedAmount());
        assertEquals(1, DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).count());

        DebitEntry firstInstallment = DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).iterator().next();

        DocumentNumberSeries settleDocumentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForSettlementNote(),
                        firstInstallment.getFinantialEntity());

        SettlementNoteBean settlementNoteBean = new SettlementNoteBean(firstInstallment.getDebtAccount(), false, false);
        settlementNoteBean.setDocNumSeries(settleDocumentNumberSeries);
        settlementNoteBean.getInvoiceEntryBean(firstInstallment).setSettledAmount(new BigDecimal("40.00"));
        settlementNoteBean.getInvoiceEntryBean(firstInstallment).setIncluded(true);
        settlementNoteBean.setFinantialEntity(firstInstallment.getFinantialEntity());

        settlementNoteBean.getPaymentEntries()
                .add(new SettlementNoteBean.PaymentEntryBean(new BigDecimal("40.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(settlementNoteBean);

        assertEquals(2, DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).count());

        assertEquals(new BigDecimal("40.00"), firstInstallment.getAmountWithVat());
        assertEquals(new BigDecimal("280.00"), firstInstallment.getNetExemptedAmount());

        DebitEntry secondFirstInstallment =
                DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).filter(d -> d != firstInstallment)
                        .iterator().next();

        assertEquals(new BigDecimal("10.00"), secondFirstInstallment.getAmountWithVat());
        assertEquals(new BigDecimal("70.00"), secondFirstInstallment.getNetExemptedAmount());

        RegistrationTuitionService.startServiceInvocation(registration, executionYear, new LocalDate())
                .applyEnrolledEctsUnits(new BigDecimal("42.5")) //
                .applyEnrolledCoursesCount(new BigDecimal("5")) //
                .withInferedTuitionPaymentPlan() //
                .withAllInstallments() //
                .forceInstallmentsEvenTreasuryEventIsCharged(true) //
                .recalculateInstallments(Map.of(firstInstallmentProduct, new LocalDate())) //
                .executeTuitionPaymentPlanCreation();

        assertEquals(false, secondFirstInstallment.isAnnulled());
        assertEquals(3, DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).count());
        assertEquals(0, firstInstallment.getCreditEntriesSet().size());

        DebitEntry thirdFirstInstallment  = DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct)
                .filter(d -> d != firstInstallment && d != secondFirstInstallment)
                .iterator().next();

        assertEquals(new BigDecimal("25.00"), thirdFirstInstallment.getAmountWithVat());
        assertEquals(new BigDecimal("0"), thirdFirstInstallment.getNetExemptedAmount());

        Product secondInstallmentProduct = Product.findUniqueByCode("PROP_2_PREST_1_CIC").get();

        assertEquals(1, DebitEntry.findActive(academicTreasuryEvent, secondInstallmentProduct).count());

        DebitEntry secondInstallment = DebitEntry.findActive(academicTreasuryEvent, secondInstallmentProduct).iterator().next();
        assertEquals(new BigDecimal("425.00"), secondInstallment.getAmountWithVat());
        assertEquals(new BigDecimal("0"), secondInstallment.getNetExemptedAmount());

        assertEquals(new BigDecimal("1350.00"), academicTreasuryEvent.getAmountWithVatToPay());
        assertEquals(new BigDecimal("350.00"), academicTreasuryEvent.getNetExemptedAmount());

        assertEquals(6, DebitEntry.findActive(academicTreasuryEvent).count());

        assertEquals(new BigDecimal("75.00"),
                DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).map(DebitEntry::getAvailableAmountWithVatForCredit)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        assertEquals(new BigDecimal("350.00"), DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct)
                .map(DebitEntry::getAvailableNetExemptedAmountForCredit).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static FinantialEntity readFinantialEntity() {
        return FinantialEntity.findAll().iterator().next();
    }

    public static LocalizedString ls(String string) {
        return new LocalizedString(TreasuryConstants.DEFAULT_LANGUAGE, string);
    }

}