package org.fenixedu.academictreasury.tuition.recalculation.complete.tests;

import org.fenixedu.academic.domain.*;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.base.BasicAcademicTreasuryUtils;
import org.fenixedu.academictreasury.base.FenixFrameworkRunner;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.tuition.*;
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
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
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
import java.util.Locale;
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
 * Student has exemption T1 by each installment
 *
 * Creation of the 1st instalment 30 ECTS, €10 per ECTS
 * 1st instalment is partially paid
 *
 * 2.ª Moment:
 * The exemption T1 is removed and replaced by T3 33%
 * Creation of 4 instalments at 24 ECTS, €5 per ECTS
 * Recalculation of the 1st instalment
 *
 * Result:
 *
 * The open first installment is annulled. The paid first installment is credited totally.
 * The four instalments are created with exemption
 * The credit of old paid first installment is settled with new first instalment
 *
 */

@RunWith(FenixFrameworkRunner.class)
public class TestRegistrationTuitionRecalculationTestFortyFour {

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
                BasicAcademicTreasuryUtils.createStatuteTypeExemptionsMap();
                createTuitionAllocationData();

                return null;
            }, new AtomicInstance(TxMode.WRITE, true));
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private static void createTuitionPaymentPlanWithAmountByEcts() {
        registration = Student.readStudentByNumber(1).getRegistrationStream().findAny().orElseThrow();
        final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();

        executionInterval = ExecutionInterval.findFirstCurrentChild(scp.getDegree().getCalendar());
        executionYear = executionInterval.getExecutionYear();

        createTuitionPaymentPlan("1T", new BigDecimal("10"));
        createTuitionPaymentPlan("2T", new BigDecimal("5"));
    }

    private static void createTuitionAllocationData() {
        TuitionDebtPostingType firstMoment = TuitionDebtPostingType.create(FinantialEntity.findAll().iterator().next(),
                new LocalizedString(new Locale("pt"), "1M"));
        TuitionDebtPostingType secondMoment = TuitionDebtPostingType.create(FinantialEntity.findAll().iterator().next(),
                new LocalizedString(new Locale("pt"), "2M"));

        TuitionPaymentPlan tuitionPaymentPlan_1T =
                registration.getLastStudentCurricularPlan().getDegreeCurricularPlan().getTuitionPaymentPlanOrdersSet().stream()
                        .map(t -> t.getTuitionPaymentPlan()).filter(t -> "1T".equals(t.getCustomizedName().getContent()))
                        .findFirst().get();

        TuitionPaymentPlan tuitionPaymentPlan_2T =
                registration.getLastStudentCurricularPlan().getDegreeCurricularPlan().getTuitionPaymentPlanOrdersSet().stream()
                        .map(t -> t.getTuitionPaymentPlan()).filter(t -> "2T".equals(t.getCustomizedName().getContent()))
                        .findFirst().get();

        TreasuryExemptionType exemptionTypeOne = TreasuryExemptionType.findByCode("TET1").findFirst().get();
        TreasuryExemptionType exemptionTypeThree = TreasuryExemptionType.findByCode("TET3").findFirst().get();

        TuitionAllocation.create(tuitionPaymentPlan_1T.getTuitionPaymentPlanGroup(), registration,
                tuitionPaymentPlan_1T.getExecutionYear(), firstMoment, tuitionPaymentPlan_1T, Set.of(exemptionTypeOne));

        TuitionAllocation.create(tuitionPaymentPlan_2T.getTuitionPaymentPlanGroup(), registration,
                tuitionPaymentPlan_2T.getExecutionYear(), secondMoment, tuitionPaymentPlan_2T, Set.of(exemptionTypeThree));
    }

    private static TuitionPaymentPlan createTuitionPaymentPlan(String customizedPlanName, BigDecimal amountByEcts) {
        TuitionPaymentPlanBean bean = new TuitionPaymentPlanBean(
                TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get().getCurrentProduct(),
                TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get(), readFinantialEntity(), executionYear);

        bean.setDefaultPaymentPlan(false);
        bean.setCustomized(true);
        bean.setName(customizedPlanName);

        DegreeCurricularPlan degreeCurricularPlan = registration.getLastStudentCurricularPlan().getDegreeCurricularPlan();
        bean.addDegreeCurricularPlans(degreeCurricularPlan);
        bean.setDegreeType(degreeCurricularPlan.getDegreeType());

        {
            AcademicTariffBean academicTariffBean = new AcademicTariffBean(1);
            academicTariffBean.setTuitionInstallmentProduct(Product.findUniqueByCode("PROP_1_PREST_1_CIC").get());
            academicTariffBean.setTuitionCalculationType(TuitionCalculationType.ECTS);
            academicTariffBean.setEctsCalculationType(EctsCalculationType.FIXED_AMOUNT);
            academicTariffBean.setFixedAmount(amountByEcts);
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
            academicTariffBean.setFixedAmount(amountByEcts);
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
            academicTariffBean.setFixedAmount(amountByEcts);
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
            academicTariffBean.setFixedAmount(amountByEcts);
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

        TuitionDebtPostingType firstMoment =
                TuitionDebtPostingType.findAll().filter(t -> "1M".equals(t.getName().getContent())).findFirst().get();
        TuitionDebtPostingType secondMoment =
                TuitionDebtPostingType.findAll().filter(t -> "2M".equals(t.getName().getContent())).findFirst().get();

        TuitionAllocation firstAllocation =
                TuitionAllocation.findUnique(TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get(), registration,
                        executionYear, firstMoment).get();

        TuitionAllocation secondAllocation =
                TuitionAllocation.findUnique(TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get(), registration,
                        executionYear, secondMoment).get();

        Product firstInstallmentProduct = Product.findUniqueByCode("PROP_1_PREST_1_CIC").get();
        RegistrationTuitionService.startServiceInvocation(registration, executionYear, new LocalDate()) //
                .applyEnrolledEctsUnits(new BigDecimal("30")) //
                .applyEnrolledCoursesCount(new BigDecimal("5")) //
                .withTuitionPaymentPlan(firstAllocation.getTuitionPaymentPlan()) //
                .applyTuitionAllocation(firstAllocation) //
                .restrictForInstallmentProducts(Set.of(firstInstallmentProduct)) //
                .withoutInstallmentsRecalculation() //
                .executeTuitionPaymentPlanCreation();

        AcademicTreasuryEvent academicTreasuryEvent =
                AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).get();

        assertEquals(new BigDecimal("263.37"), academicTreasuryEvent.getAmountWithVatToPay());
        assertEquals(new BigDecimal("36.63"), academicTreasuryEvent.getNetExemptedAmount());
        assertEquals(1, DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).count());

        DebitEntry firstInstallment = DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).iterator().next();

        DocumentNumberSeries debitNoteDocumentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(),
                        firstInstallment.getFinantialEntity());

        DebitNote.createDebitNoteForDebitEntry(firstInstallment, null, debitNoteDocumentNumberSeries, new DateTime(),
                new LocalDate(), null, null, null);

        DocumentNumberSeries settleDocumentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForSettlementNote(),
                        firstInstallment.getFinantialEntity());

        SettlementNoteBean settlementNoteBean = new SettlementNoteBean(firstInstallment.getDebtAccount(), false, false);
        settlementNoteBean.setDocNumSeries(settleDocumentNumberSeries);
        settlementNoteBean.getInvoiceEntryBean(firstInstallment).setSettledAmount(new BigDecimal("163.37"));
        settlementNoteBean.getInvoiceEntryBean(firstInstallment).setIncluded(true);
        settlementNoteBean.setFinantialEntity(firstInstallment.getFinantialEntity());

        settlementNoteBean.getPaymentEntries()
                .add(new SettlementNoteBean.PaymentEntryBean(new BigDecimal("163.37"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(settlementNoteBean);

        assertEquals(1, firstInstallment.getSettlementEntriesSet().size());

        assertEquals(new BigDecimal("163.37"), firstInstallment.getAmountWithVat());
        assertEquals(new BigDecimal("22.72"), firstInstallment.getNetExemptedAmount());

        assertEquals(2, DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).count());

        DebitEntry secondFirstInstallment =
                DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).filter(d -> d != firstInstallment)
                        .iterator().next();

        assertEquals(true, secondFirstInstallment.getDebitNote().isPreparing());
        assertEquals(new BigDecimal("100.00"), secondFirstInstallment.getAmountWithVat());
        assertEquals(new BigDecimal("13.91"), secondFirstInstallment.getNetExemptedAmount());

        RegistrationTuitionService.startServiceInvocation(registration, executionYear, new LocalDate())
                .applyEnrolledEctsUnits(new BigDecimal("24")) //
                .applyEnrolledCoursesCount(new BigDecimal("5")) //
                .withTuitionPaymentPlan(secondAllocation.getTuitionPaymentPlan()) //
                .applyTuitionAllocation(secondAllocation) //
                .withAllInstallments() //
                .forceInstallmentsEvenTreasuryEventIsCharged(true) //
                .recalculateInstallments(Map.of(firstInstallmentProduct, new LocalDate())) //
                .executeTuitionPaymentPlanCreation();

        assertEquals(true, secondFirstInstallment.isAnnulled());

        assertEquals(1, DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).count());
        assertEquals(false, firstInstallment.isAnnulled());
        assertEquals(true, firstInstallment.isEventAnnuled());
        assertEquals(new BigDecimal("80.40"), academicTreasuryEvent.getAmountWithVatToPay(firstInstallmentProduct));
        assertEquals(new BigDecimal("39.60"), academicTreasuryEvent.getNetExemptedAmount(firstInstallmentProduct));

        assertEquals(2, firstInstallment.getCreditEntriesSet().size());

        CreditEntry firstCreditEntry = firstInstallment.getCreditEntriesSet()
                .stream().filter(ce -> ce.getCreditNote().isClosed())
                .iterator().next();

        assertEquals(new BigDecimal("80.40"), firstCreditEntry.getAmountWithVat());
        assertEquals(new BigDecimal("11.18"), firstCreditEntry.getNetExemptedAmount());

        CreditEntry secondCreditEntry = firstInstallment.getCreditEntriesSet()
                .stream().filter(ce -> ce.getCreditNote().isPreparing())
                .iterator().next();

        assertEquals(1, firstCreditEntry.getSettlementEntriesSet().size());

        assertEquals(new BigDecimal("82.97"), secondCreditEntry.getAmountWithVat());
        assertEquals(new BigDecimal("11.54"), secondCreditEntry.getNetExemptedAmount());

        DebitEntry thirdFirstInstallment = DebitEntry.findActive(academicTreasuryEvent, firstInstallmentProduct).iterator().next();

        assertEquals(true, thirdFirstInstallment != firstInstallment);
        assertEquals(true, thirdFirstInstallment != secondFirstInstallment);
        assertEquals(new BigDecimal("80.40"), thirdFirstInstallment.getAmountWithVat());
        assertEquals(new BigDecimal("39.60"), thirdFirstInstallment.getNetExemptedAmount());

        Product secondInstallmentProduct = Product.findUniqueByCode("PROP_2_PREST_1_CIC").get();

        assertEquals(1, DebitEntry.findActive(academicTreasuryEvent, secondInstallmentProduct).count());

        DebitEntry secondInstallment = DebitEntry.findActive(academicTreasuryEvent, secondInstallmentProduct).iterator().next();
        assertEquals(new BigDecimal("80.40"), secondInstallment.getAmountWithVat());
        assertEquals(new BigDecimal("39.60"), secondInstallment.getNetExemptedAmount());

        assertEquals(new BigDecimal("321.60"), academicTreasuryEvent.getAmountWithVatToPay());
        assertEquals(new BigDecimal("158.40"), academicTreasuryEvent.getNetExemptedAmount());

        assertEquals(4, DebitEntry.findActive(academicTreasuryEvent).count());
    }

    private static FinantialEntity readFinantialEntity() {
        return FinantialEntity.findAll().iterator().next();
    }

    public static LocalizedString ls(String string) {
        return new LocalizedString(TreasuryConstants.DEFAULT_LANGUAGE, string);
    }

}