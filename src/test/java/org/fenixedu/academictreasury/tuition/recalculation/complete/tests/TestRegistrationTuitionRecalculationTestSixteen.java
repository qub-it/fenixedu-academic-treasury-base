package org.fenixedu.academictreasury.tuition.recalculation.complete.tests;

import org.fenixedu.academic.domain.*;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academictreasury.base.FenixFrameworkRunner;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
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
import org.fenixedu.treasury.domain.document.*;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
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

/**
 * ****************
 * TEST DESCRIPTION
 * ****************
 *
 * 1.º Moment:
 *
 * Creation of the 1st instalment 30 ECTS, €10 per ECTS
 * 1st instalment is paid but it was annulled and credit entry is created
 *
 * 2.ª Moment:
 *
 * Creation of 4 instalments at 32.5 ECTS, €10 per ECTS
 * Recalculation of the 1st instalment
 *
 * Result:
 *
 * THe first installment must be created again
 * The remaining three instalments are created.
 *
 */


@RunWith(FenixFrameworkRunner.class)
public class TestRegistrationTuitionRecalculationTestSixteen {

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
                PersonCustomer.createSaftDefaultPhysicalAddress(registration.getPerson()));
    }

    @Test
    public void doRecalculation() {
        FinantialInstitution.findAll().iterator().next().setSplitCreditEntriesWithSettledAmount(true);
        FinantialInstitution.findAll().iterator().next().setSplitDebitEntriesWithSettledAmount(true);

        createTuitionPaymentPlanWithAmountByEcts();
        ensureNecessaryAcademicDataIsAvailable();

        Product firstInstallmentProduct = Product.findUniqueByCode("PROP_1_PREST_1_CIC").get();
        RegistrationTuitionService.startServiceInvocation(registration, executionYear, new LocalDate()) //
                .applyEnrolledEctsUnits(new BigDecimal("30")) //
                .applyEnrolledCoursesCount(new BigDecimal("5")) //
                .withInferedTuitionPaymentPlan() //
                .restrictForInstallmentProducts(Set.of(firstInstallmentProduct)) //
                .withoutInstallmentsRecalculation() //
                .executeTuitionPaymentPlanCreation();

        AcademicTreasuryEvent academicTreasuryEvent =
                AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).get();

        assertEquals(new BigDecimal("300.00"), academicTreasuryEvent.getAmountWithVatToPay());
        assertEquals(1, DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).count());

        DebitEntry firstInstallment = DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).iterator().next();

        DocumentNumberSeries debitDocumentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(),
                        firstInstallment.getFinantialEntity());
        DebitNote.createDebitNoteForDebitEntry(firstInstallment, null, debitDocumentNumberSeries, new DateTime(), new LocalDate(),
                null, null, null);

        DocumentNumberSeries settleDocumentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForSettlementNote(),
                        firstInstallment.getFinantialEntity());

        SettlementNoteBean settlementNoteBean = new SettlementNoteBean(firstInstallment.getDebtAccount(), false, false);
        settlementNoteBean.setDocNumSeries(settleDocumentNumberSeries);
        settlementNoteBean.getInvoiceEntryBean(firstInstallment).setSettledAmount(new BigDecimal("300.00"));
        settlementNoteBean.getInvoiceEntryBean(firstInstallment).setIncluded(true);
        settlementNoteBean.setFinantialEntity(firstInstallment.getFinantialEntity());

        settlementNoteBean.getPaymentEntries()
                .add(new SettlementNoteBean.PaymentEntryBean(new BigDecimal("300.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(settlementNoteBean);

        assertEquals(1, firstInstallment.getSettlementEntriesSet().size());

        firstInstallment.annulOnlyThisDebitEntryAndInterestsInBusinessContext("Test", false);

        assertEquals(0, DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).count());
        assertEquals(new BigDecimal("300.00"), firstInstallment.getAmountWithVat());
        assertEquals(true, firstInstallment.getDebitNote().isClosed());
        assertEquals(true, firstInstallment.isEventAnnuled());

        CreditEntry creditEntry = firstInstallment.getCreditEntriesSet().iterator().next();
        assertEquals(new BigDecimal("300.00"), creditEntry.getAmountWithVat());
        assertEquals(true, creditEntry.getCreditNote().isPreparing());

        RegistrationTuitionService.startServiceInvocation(registration, executionYear, new LocalDate())
                .applyEnrolledEctsUnits(new BigDecimal("32.5")) //
                .applyEnrolledCoursesCount(new BigDecimal("5")) //
                .withInferedTuitionPaymentPlan() //
                .withAllInstallments() //
                .forceInstallmentsEvenTreasuryEventIsCharged(true) //
                .recalculateInstallments(Map.of(firstInstallmentProduct, new LocalDate())) //
                .executeTuitionPaymentPlanCreation();

        DebitEntry secondFirstInstallment = DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).iterator().next();

        assertEquals(true, firstInstallment != secondFirstInstallment);

        assertEquals(new BigDecimal("1300.00"), academicTreasuryEvent.getAmountWithVatToPay());
        assertEquals(4, DebitEntry.findActive(academicTreasuryEvent).count());
        assertEquals(1, DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).count());

        assertEquals(new BigDecimal("325.00"),
                DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).map(DebitEntry::getAmountWithVat)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        assertEquals(true, secondFirstInstallment.getDebitNote() == null || secondFirstInstallment.getDebitNote().isPreparing());

        assertEquals(new BigDecimal("300.00"), creditEntry.getAmountWithVat());
        assertEquals(true, creditEntry.getCreditNote().isPreparing());
    }

    private static FinantialEntity readFinantialEntity() {
        return FinantialEntity.findAll().iterator().next();
    }

    public static LocalizedString ls(String string) {
        return new LocalizedString(TreasuryConstants.DEFAULT_LANGUAGE, string);
    }

}