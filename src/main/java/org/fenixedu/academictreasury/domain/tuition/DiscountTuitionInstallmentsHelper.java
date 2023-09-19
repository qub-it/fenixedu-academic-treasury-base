package org.fenixedu.academictreasury.domain.tuition;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.TreeMap;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.dto.tuition.TuitionDebitEntryBean;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

public class DiscountTuitionInstallmentsHelper {

    // These fields are used when creating the installments into debt account
    private DebtAccount debtAccount;
    private AcademicTreasuryEvent tuitionAcademicTreasuryEvent;
    private Set<Product> restrictCreationToInstallments;
    private LocalDate when;

    // The following are used to calculate the installments that will be created (something like a preview)
    private Registration registration;
    private TuitionPaymentPlan tuitionPaymentPlan;
    private LocalDate debtDate;
    private BigDecimal enrolledEctsUnits;
    private BigDecimal enrolledCoursesCount;

    // This field is common to both cases
    private Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap;
    private Person person;
    private ExecutionYear executionYear;

    private TreeMap<TreasuryEvent, BigDecimal> discountMap;

    // Used to create the tuition payment plan into debt account
    public DiscountTuitionInstallmentsHelper(DebtAccount debtAccount, AcademicTreasuryEvent tuitionAcademicTreasuryEvent,
            Set<Product> restrictCreationToInstallments, LocalDate when,
            Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap) {

        this.debtAccount = debtAccount;
        this.tuitionAcademicTreasuryEvent = tuitionAcademicTreasuryEvent;
        this.restrictCreationToInstallments = restrictCreationToInstallments;
        this.when = when;
        this.calculatorsMap = calculatorsMap;

        this.person = tuitionAcademicTreasuryEvent.getPerson();
        this.executionYear = tuitionAcademicTreasuryEvent.getExecutionYear();
        this.registration = tuitionAcademicTreasuryEvent.getRegistration();

        this.discountMap = buildDiscountMap();
    }

    // Used to calculate the installments to preview
    public DiscountTuitionInstallmentsHelper(Registration registration, TuitionPaymentPlan tuitionPaymentPlan, LocalDate debtDate,
            BigDecimal enrolledEctsUnits, BigDecimal enrolledCoursesCount,
            Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> calculatorsMap) {

        this.tuitionPaymentPlan = tuitionPaymentPlan;
        this.debtDate = debtDate;
        this.enrolledEctsUnits = enrolledEctsUnits;
        this.enrolledCoursesCount = enrolledCoursesCount;
        this.calculatorsMap = calculatorsMap;

        this.person = registration.getPerson();
        this.executionYear = tuitionPaymentPlan.getExecutionYear();
        this.registration = registration;

        this.discountMap = buildDiscountMap();
    }

    public boolean createInstallmentAndDiscountInstallment(TuitionInstallmentTariff tariff) {
        final BigDecimal tuitionInstallmentAmountToPay =
                tariff.amountToPay(this.tuitionAcademicTreasuryEvent, this.calculatorsMap);

        BigDecimal amountToSubtractFromAmountToPay = BigDecimal.ZERO;
        Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap = new HashMap<>();

        BigDecimal availableAmountToPay = tuitionInstallmentAmountToPay;
        for (TreasuryEvent treasuryEvent : this.discountMap.navigableKeySet()) {
            if (!TreasuryConstants.isPositive(this.discountMap.get(treasuryEvent))) {
                continue;
            }

            BigDecimal availableAmountToDiscount = this.discountMap.get(treasuryEvent);
            BigDecimal amountToDiscount = availableAmountToDiscount.min(availableAmountToPay);
            availableAmountToPay = availableAmountToPay.subtract(amountToDiscount);

            this.discountMap.put(treasuryEvent, availableAmountToDiscount.subtract(amountToDiscount));

            if (treasuryEvent.isEventDiscountInTuitionFeeWithTreasuryExemption()) {
                TreasuryExemptionType treasuryExemption = treasuryEvent.getTreasuryExemptionToApplyInEventDiscountInTuitionFee();
                if (!exemptionsToApplyMap.containsKey(treasuryExemption)) {
                    exemptionsToApplyMap.put(treasuryExemption, BigDecimal.ZERO);
                }

                exemptionsToApplyMap.put(treasuryExemption, exemptionsToApplyMap.get(treasuryExemption).add(amountToDiscount));
            } else {
                amountToSubtractFromAmountToPay = amountToSubtractFromAmountToPay.add(amountToDiscount);
            }

            if (!TreasuryConstants.isPositive(availableAmountToPay)) {
                break;
            }
        }

        if (!TreasuryConstants.isPositive(tuitionInstallmentAmountToPay.subtract(amountToSubtractFromAmountToPay))) {
            return false;
        }

        boolean allowToCreateTheInstallment =
                this.restrictCreationToInstallments == null || this.restrictCreationToInstallments.contains(tariff.getProduct());

        if (allowToCreateTheInstallment && !this.tuitionAcademicTreasuryEvent.isChargedWithDebitEntry(tariff)) {
            DebitEntry installmentDebitEntry = tariff.createDebitEntryForRegistration(this.debtAccount,
                    this.tuitionAcademicTreasuryEvent, this.when, calculatorsMap, amountToSubtractFromAmountToPay);

            for (Entry<TreasuryExemptionType, BigDecimal> entry : exemptionsToApplyMap.entrySet()) {
                TreasuryExemptionType treasuryExemptionType = entry.getKey();
                BigDecimal amountToExempt = entry.getValue();

                String reason = treasuryExemptionType.getName()
                        .getContent(TreasuryPlataformDependentServicesFactory.implementation().defaultLocale());
                TreasuryExemption.create(treasuryExemptionType, reason, amountToExempt,
                        installmentDebitEntry);
            }

            return true;
        }

        return false;
    }

    public TuitionDebitEntryBean buildInstallmentDebitEntryBeanWithDiscount(TuitionInstallmentTariff tuitionInstallmentTariff) {

        final int installmentOrder = tuitionInstallmentTariff.getInstallmentOrder();
        final LocalizedString installmentName = this.tuitionPaymentPlan.installmentName(this.registration, tuitionInstallmentTariff);
        final LocalDate dueDate = tuitionInstallmentTariff.dueDate(this.debtDate);
        final Vat vat = tuitionInstallmentTariff.vat(this.debtDate);
        BigDecimal tuitionInstallmentAmountToPay =
                tuitionInstallmentTariff.amountToPay(this.registration, this.enrolledEctsUnits, this.enrolledCoursesCount, this.calculatorsMap);
        final Currency currency = tuitionInstallmentTariff.getFinantialEntity().getFinantialInstitution().getCurrency();

        BigDecimal amountToSubtractFromAmountToPay = BigDecimal.ZERO;
        Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap = new HashMap<>();

        BigDecimal availableAmountToPay = tuitionInstallmentAmountToPay;
        for (TreasuryEvent treasuryEvent : this.discountMap.navigableKeySet()) {
            if (!TreasuryConstants.isPositive(this.discountMap.get(treasuryEvent))) {
                continue;
            }

            BigDecimal availableAmountToDiscount = this.discountMap.get(treasuryEvent);
            BigDecimal amountToDiscount = availableAmountToDiscount.min(availableAmountToPay);
            availableAmountToPay = availableAmountToPay.subtract(amountToDiscount);

            this.discountMap.put(treasuryEvent, availableAmountToDiscount.subtract(amountToDiscount));

            if (treasuryEvent.isEventDiscountInTuitionFeeWithTreasuryExemption()) {
                TreasuryExemptionType treasuryExemption = treasuryEvent.getTreasuryExemptionToApplyInEventDiscountInTuitionFee();
                if (!exemptionsToApplyMap.containsKey(treasuryExemption)) {
                    exemptionsToApplyMap.put(treasuryExemption, BigDecimal.ZERO);
                }

                exemptionsToApplyMap.put(treasuryExemption, exemptionsToApplyMap.get(treasuryExemption).add(amountToDiscount));
            } else {
                amountToSubtractFromAmountToPay = amountToSubtractFromAmountToPay.add(amountToDiscount);
            }

            if (!TreasuryConstants.isPositive(availableAmountToPay)) {
                break;
            }
        }

        BigDecimal finalAmountToPay = tuitionInstallmentAmountToPay.subtract(amountToSubtractFromAmountToPay);
        if (!TreasuryConstants.isPositive(finalAmountToPay)) {
            return null;
        }

        BigDecimal totalAmountToExempt = exemptionsToApplyMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        finalAmountToPay = finalAmountToPay.subtract(totalAmountToExempt);

        if (TreasuryConstants.isPositive(totalAmountToExempt)) {
            return new TuitionDebitEntryBean(installmentOrder, tuitionInstallmentTariff, installmentName, dueDate, vat.getTaxRate(), finalAmountToPay,
                    totalAmountToExempt, null, currency);
        } else {
            return new TuitionDebitEntryBean(installmentOrder, tuitionInstallmentTariff, installmentName, dueDate, vat.getTaxRate(), finalAmountToPay,
                    currency);
        }
    }

    private TreeMap<TreasuryEvent, BigDecimal> buildDiscountMap() {
        Comparator<? super TreasuryEvent> TREASURY_EVENT_COMPARATOR = (o1, o2) -> {
            if (!o1.isEventDiscountInTuitionFeeWithTreasuryExemption() && o2.isEventDiscountInTuitionFeeWithTreasuryExemption()) {
                return -1;
            } else if (o1.isEventDiscountInTuitionFeeWithTreasuryExemption()
                    && !o2.isEventDiscountInTuitionFeeWithTreasuryExemption()) {
                return 1;
            }

            return o1.getExternalId().compareTo(o2.getExternalId());
        };

        StudentCurricularPlan studentCurricularPlan = this.registration.getStudentCurricularPlan(this.executionYear);
        DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();

        TreeMap<TreasuryEvent, BigDecimal> result = new TreeMap<>(TREASURY_EVENT_COMPARATOR);
        getOtherEventsToDiscountInTuitionFee(this.person, this.executionYear, degreeCurricularPlan)
                .forEach(ev -> result.put(ev, ev.getAmountWithVatToPay()));

        return result;
    }

    private static List<TreasuryEvent> getOtherEventsToDiscountInTuitionFee(Person person, ExecutionYear executionYear,
            DegreeCurricularPlan degreeCurricularPlan) {
        return TreasuryBridgeAPIFactory.implementation().getAllAcademicTreasuryEventsList(person) //
                .stream() //
                .map(TreasuryEvent.class::cast) //
                .filter(t -> t.isEventDiscountInTuitionFee()) //
                .filter(t -> executionYear.getQualifiedName().equals(t.getExecutionYearName())) //
                .filter(t -> degreeCurricularPlan.getDegree().getCode().equals(t.getDegreeCode())) //
                .collect(Collectors.toList());
    }

}