package org.fenixedu.academictreasury.services.tuition;

import com.google.common.base.Strings;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tuition.*;
import org.fenixedu.academictreasury.dto.tuition.TuitionDebitEntryBean;
import org.fenixedu.academictreasury.services.ITuitionServiceExtension;
import org.fenixedu.academictreasury.services.TuitionServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RegistrationTuitionService implements ITuitionRegistrationServiceParameters {

    boolean isForCalculationsOfOriginalAmounts = false;

    RegistrationOptions registrationOptions;
    TuitionOptions tuitionOptions;
    InstallmentOptions installmentOptions;
    InstallmentRecalculationOptions installmentRecalculationOptions;

    // This map will hold the exemption amounts to apply in installment debit entries
    TreasuryExemptionsTeller _treasuryExemptionsTeller;

    Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> _calculatorsMap;
    String _calculationDescription;

    RegistrationTuitionService _originalAmountsCalculator;

    // algorithm
    public boolean executeTuitionPaymentPlanCreation() {
        String reason =
                AcademicTreasuryConstants.academicTreasuryBundle("label.RegistrationTuitionService.tuitionRecalculationReason");

        Registration registration = this.registrationOptions.registration;
        ExecutionYear executionYear = this.registrationOptions.executionYear;

        LocalDate debtDate = this.registrationOptions.debtDate;

        boolean forceCreationIfNotEnrolled = this.tuitionOptions.forceCreationIfNotEnrolled;

        initializeTuitionPaymentPlan();

        TuitionPaymentPlan tuitionPaymentPlan = this.tuitionOptions.tuitionPaymentPlan;
        boolean applyTuitionServiceExtensions = this.tuitionOptions.applyTuitionServiceExtensions;

        boolean forceEvenTreasuryEventIsCharged = this.installmentOptions.forceInstallmentsEvenTreasuryEventIsCharged;

        if (!TuitionServices.isToPayRegistrationTuition(registration, executionYear) && !forceCreationIfNotEnrolled) {
            return false;
        }

        if (applyTuitionServiceExtensions) {
            for (final ITuitionServiceExtension iTuitionServiceExtension : TuitionServices.TUITION_SERVICE_EXTENSIONS()) {
                if (iTuitionServiceExtension.applyExtension(registration, executionYear)) {
                    return iTuitionServiceExtension.createTuitionForRegistration(registration, executionYear, debtDate,
                            forceCreationIfNotEnrolled, tuitionPaymentPlan);
                }
            }
        }

        if (tuitionPaymentPlan == null) {
            return false;
        }

        if (!forceCreationIfNotEnrolled && tuitionPaymentPlan.isStudentMustBeEnrolled() && TuitionServices.normalEnrolmentsIncludingAnnuled(
                registration, executionYear).isEmpty()) {
            return false;
        }

        final Person person = registration.getPerson();
        final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);
        if (Strings.isNullOrEmpty(addressFiscalCountryCode) || Strings.isNullOrEmpty(fiscalNumber)) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalInformation.required");
        }

        // Read person customer

        if (PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).isEmpty()) {
            PersonCustomer.create(person, addressFiscalCountryCode, fiscalNumber);
        }

        final PersonCustomer personCustomer = PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).get();
        if (!personCustomer.isActive()) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.not.active",
                    personCustomer.getBusinessIdentification(), personCustomer.getName());
        }

        if (DebtAccount.findUnique(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer).isEmpty()) {
            DebtAccount.create(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer);
        }

        if (AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).isEmpty()) {
            AcademicTreasuryEvent.createForRegistrationTuition(tuitionPaymentPlan.getProduct(), registration, executionYear);
        }

        final DebtAccount debtAccount =
                DebtAccount.findUnique(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer).get();

        FinantialInstitution finantialInstitution = tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution();
        Currency currency = finantialInstitution.getCurrency();

        final AcademicTreasuryEvent academicTreasuryEvent =
                AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).get();

        if (!tuitionPaymentPlan.getTuitionPaymentPlanGroup().isForRegistration()) {
            throw new RuntimeException("wrong call");
        }

        if (!forceEvenTreasuryEventIsCharged && academicTreasuryEvent.isCharged()) {
            return false;
        }

        initializeCreditsAndEnrolledCoursesCount();
        initializeCustomCalculators();
        initializeOriginalAmountsCalculator();

        this._treasuryExemptionsTeller = new TreasuryExemptionsTeller(this);

        Map<TuitionInstallmentTariff, TuitionDebitEntryBean> calculatedDebitEntryBeansMap =
                this._originalAmountsCalculator.executeInstallmentDebitEntryBeansCalculation().stream()
                        .collect(Collectors.toMap(TuitionDebitEntryBean::getTuitionInstallmentTariff, Function.identity()));

        if (!this._calculationDescription.isEmpty()) {
            Map<String, String> propertiesMap = academicTreasuryEvent.getPropertiesMap();

            String key = AcademicTreasuryConstants.academicTreasuryBundle(
                    "label.AcademicTreasury.CustomCalculatorDescription") + " ( " + DateTime.now()
                    .toString("yyyy-MM-dd HH:mm") + " )";

            propertiesMap.put(key, this._calculationDescription);

            academicTreasuryEvent.editPropertiesMap(propertiesMap);
        }

        Stream<TuitionInstallmentTariff> installments = tuitionPaymentPlan.getTuitionInstallmentTariffsSet().stream()
                .sorted(TuitionInstallmentTariff.COMPARATOR_BY_INSTALLMENT_NUMBER);

        Function<TuitionInstallmentTariff, Boolean> func = tariff -> {
            Product product = tariff.getProduct();

            this._treasuryExemptionsTeller.createDiscountExemptionsMapForOnlyThisInstallment(tariff);

            boolean isToRecalculateInstallment =
                    this.installmentRecalculationOptions.recalculateInstallments != null && this.installmentRecalculationOptions.recalculateInstallments.containsKey(
                            product);
            if (isToRecalculateInstallment && isTuitionInstallmentCharged(product)) {

                // Recalculate

                // Get the amounts that should be debited
                TuitionDebitEntryBean originalBean = calculatedDebitEntryBeansMap.computeIfAbsent(tariff,
                        t -> new TuitionDebitEntryBean(t.getInstallmentOrder(), t, ls("Installment bean"), debtDate,
                                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new HashMap<>(), currency));

                BigDecimal differenceNetAmount = originalBean.getAmount().subtract(getNetAmountAlreadyDebited(product));
                BigDecimal differenceInNetExemptedAmount =
                        originalBean.getExemptedAmount().subtract(getNetAmountAlreadyExempted(product));

                // Test with the order from the easiest cases to the most complex

                boolean isExemptionsMapAreEqual =
                        this._treasuryExemptionsTeller.isExemptionsMapAreEqual(tariff, academicTreasuryEvent, originalBean);
                boolean isThereIsOnlyRemovalsOrDecrementsInExemptions =
                        this._treasuryExemptionsTeller.isThereIsOnlyRemovalsOrDecrementsInExemptions(tariff,
                                academicTreasuryEvent, originalBean);
                boolean isThereAreOnlyNewEntriesOrIncrementsInExemptions =
                        this._treasuryExemptionsTeller.isThereAreOnlyNewEntriesOrIncrementsInExemptions(tariff,
                                academicTreasuryEvent, originalBean);

                boolean isThereAreRemovalOrDecrementsInExemptions =
                        this._treasuryExemptionsTeller.isThereAreRemovalOrDecrementsInExemptions(tariff, academicTreasuryEvent,
                                originalBean);

                boolean isDifferenceNetAmountZero = TreasuryConstants.isZero(differenceNetAmount);
                boolean isDifferenceNetAmountNegativeOrZero = !TreasuryConstants.isPositive(differenceNetAmount);
                boolean isDifferenceNetAmountPositiveOrZero = !TreasuryConstants.isNegative(differenceNetAmount);

                if (isDifferenceNetAmountZero && isExemptionsMapAreEqual) {
                    // Nothing to do, there are no differences in the amounts
                    return false;
                } else if (isDifferenceNetAmountNegativeOrZero && (isExemptionsMapAreEqual || isThereIsOnlyRemovalsOrDecrementsInExemptions)) {
                    return runLogicToDecrementOnlyNetAmountOrExemptions(tariff, academicTreasuryEvent, originalBean, product,
                            differenceNetAmount);
                } else if (isDifferenceNetAmountPositiveOrZero && (isExemptionsMapAreEqual || isThereAreOnlyNewEntriesOrIncrementsInExemptions)) {
                    return runLogicToIncrementOnlyNetAmountOrExemptions(tariff, product, originalBean,
                            differenceInNetExemptedAmount, differenceNetAmount, debtDate, debtAccount, academicTreasuryEvent);
                } else if (TreasuryConstants.isNegative(differenceInNetExemptedAmount) || (TreasuryConstants.isNegative(
                        differenceNetAmount) && !TreasuryConstants.isZero(
                        differenceInNetExemptedAmount)) || isThereAreRemovalOrDecrementsInExemptions) {
                    // The difference in the exemption amount is negative, or there is a credit to create but
                    // the difference in the exemption is different from zero. Then we have to annul the all debit entries
                    // of installment and create again

                    // ANIL 2024-09-26 (#qubIT-Fenix-5854)
                    //
                    // Before this date, the algorithm was closing the debit entries, which is not 
                    // acceptable for some schools.

                    this._treasuryExemptionsTeller.revertExemptionAmountsFromAcademicTreasuryToDiscountExemptionsMapForAllInstallments(
                            tariff);

                    // ANIL 2025-03-03 (#qubIT-Fenix-6662)
                    //
                    // Interest entries cannot be annulled
                    DebitEntry.findActive(academicTreasuryEvent, product)
                            .forEach(d -> d.annulOnlyThisDebitEntryAndInterestsInBusinessContext(reason, false));

                    BigDecimal tuitionInstallmentAmountToPay = originalBean.getAmount().add(originalBean.getExemptedAmount());
                    Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap =
                            this._treasuryExemptionsTeller.retrieveUnchargedExemptionsToApplyMapForTariff(tariff,
                                    tuitionInstallmentAmountToPay);

                    createDebitEntryForRegistrationAndExempt(tariff, exemptionsToApplyMap);

                    return true;
                }

                throw new IllegalStateException("recalculation: do not know how to handle this case???");
            } else if (!isTuitionInstallmentCharged(product)) {
                return createUnchargedInstallmentDebitEntry(tariff);
            } else if (isTuitionInstallmentCharged(product)) {
                return false;
            } else {
                throw new IllegalStateException("recalculation: do not know how to handle this case???");
            }
        };

        Predicate<TuitionInstallmentTariff> isToCalculateInstallmentProduct =
                t -> (this.installmentOptions.installments == null || this.installmentOptions.installments.contains(
                        t.getProduct())) || (this.installmentRecalculationOptions.recalculateInstallments != null && this.installmentRecalculationOptions.recalculateInstallments.containsKey(
                        t.getProduct()));

        return installments //
                .filter(isToCalculateInstallmentProduct) //
                .map(func) //
                .reduce(Boolean.FALSE, Boolean::logicalOr);
    }

    private Boolean runLogicToIncrementOnlyNetAmountOrExemptions(final TuitionInstallmentTariff tariff, final Product product,
            final TuitionDebitEntryBean originalBean, final BigDecimal differenceInNetExemptedAmount,
            final BigDecimal differenceNetAmount, LocalDate debtDate, DebtAccount debtAccount,
            final AcademicTreasuryEvent academicTreasuryEvent) {
        // This combination of difference amounts, allow us to create a new recalculation debit entry
        LocalDate recalculationDueDate = this.installmentRecalculationOptions.recalculateInstallments.get(product);

        // Now it is time to discount from both discount exemption maps,
        // for this installment and from all installments
        Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap =
                this._treasuryExemptionsTeller.retrieveAdditionalExemptionsToApplyMapForTariff(tariff, product, originalBean,
                        differenceInNetExemptedAmount);

        BigDecimal debitEntryNetAmount = differenceNetAmount.add(differenceInNetExemptedAmount);

        return createRecalculationAdditionalDebitEntryForRegistrationAndExempt(debtDate, debtAccount, academicTreasuryEvent,
                tariff, exemptionsToApplyMap, debitEntryNetAmount, recalculationDueDate);
    }

    private Boolean runLogicToDecrementOnlyNetAmountOrExemptions(TuitionInstallmentTariff tariff,
            AcademicTreasuryEvent academicTreasuryEvent, TuitionDebitEntryBean originalBean, Product product,
            BigDecimal differenceNetAmount) {
        String recalculationReason =
                AcademicTreasuryConstants.academicTreasuryBundle("label.RegistrationTuitionService.tuitionRecalculationReason");

        Map<TreasuryExemptionType, BigDecimal> exemptionDecrementAmountsByTypeMap =
                this._treasuryExemptionsTeller.getTreasuryExemptionDecrementsByTypeMap(tariff, academicTreasuryEvent,
                        originalBean);

        List<DebitEntry> debitEntriesToCreditList =
                DebitEntry.findActive(academicTreasuryEvent, product).sorted(DebitEntry.COMPARE_BY_EXTERNAL_ID.reversed())
                        .collect(Collectors.toList());

        BigDecimal remainingAmountToCredit = differenceNetAmount.negate();
        for (DebitEntry d : debitEntriesToCreditList) {
            BigDecimal sumOfExemptionDecrementAmountsByType =
                    exemptionDecrementAmountsByTypeMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            if (!TreasuryConstants.isPositive(remainingAmountToCredit) && !TreasuryConstants.isPositive(
                    sumOfExemptionDecrementAmountsByType)) {
                // Nothing more to decrement
                return true;
            }

            BigDecimal netAmountToCredit = remainingAmountToCredit.min(d.getAvailableNetAmountForCredit());
            Map<TreasuryExemptionType, BigDecimal> netExemptedAmountToCreditByTypeMap = new HashMap<>();

            for (TreasuryExemptionType t : exemptionDecrementAmountsByTypeMap.keySet()) {
                BigDecimal netExemptedAmountToCredit =
                        exemptionDecrementAmountsByTypeMap.get(t).min(d.getAvailableNetExemptedAmountForCredit(t));

                if (TreasuryConstants.isPositive(netExemptedAmountToCredit)) {
                    netExemptedAmountToCreditByTypeMap.put(t, netExemptedAmountToCredit);
                }
            }

            remainingAmountToCredit = netAmountToCredit.subtract(netAmountToCredit);
            netExemptedAmountToCreditByTypeMap.keySet().forEach(t -> exemptionDecrementAmountsByTypeMap.put(t,
                    exemptionDecrementAmountsByTypeMap.get(t).subtract(netExemptedAmountToCreditByTypeMap.get(t))));

            // Distinguish between debit entry in preparing state or closed
            if (d.getFinantialDocument() == null || d.getFinantialDocument().isPreparing()) {
                // we are going to annul this item
                // fill back the exemption amount for all installments
                // til the limit
                d.getEffectiveNetExemptionAmountsMapByType().entrySet().forEach(
                        entry -> this._treasuryExemptionsTeller.fillBackExemptionAmountForAllInstallments(entry.getKey(),
                                entry.getValue()));

                // When it is preparing, we proceed with the following
                //
                // 1. Remove the debit entry from the debit note, if it is associated
                DebitNote debitNote = d.getDebitNote();

                if (d.getDebitNote() != null) {
                    d.removeFromDocument();
                }

                // 2. Calculate the exempted amounts to credit

                Map<TreasuryExemption, BigDecimal> creditExemptionsMap =
                        d.calculateNetExemptedAmountsToCreditMapBasedInExplicitAmounts(netExemptedAmountToCreditByTypeMap, true);

                // 3. Create the debitEntry with the amount equals to the following formula
                // netAmount + netExemptedAmount - netAmountToCredit - exemptedNetAmountToCredit

                BigDecimal sumOfNetExemptedAmountToCreditByTypeMap =
                        netExemptedAmountToCreditByTypeMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal newNetAmount = d.getNetAmount().add(d.getNetExemptedAmount()).subtract(netAmountToCredit)
                        .subtract(sumOfNetExemptedAmountToCreditByTypeMap);

                DebitEntry newDebitEntry =
                        DebitEntry.create(d.getFinantialEntity(), d.getDebtAccount(), d.getTreasuryEvent(), d.getVat(), //
                                newNetAmount, d.getDueDate(), d.getPropertiesMap(), d.getProduct(), //
                                d.getDescription(), BigDecimal.ONE, d.getInterestRate(), d.getEntryDateTime(), //
                                d.isAcademicalActBlockingSuspension(), d.isBlockAcademicActsOnDebt(), debitNote);

                Map<TreasuryExemptionType, BigDecimal> newTreasuryExemptionMapByType =
                        this._treasuryExemptionsTeller.retrieveUnchargedExemptionsToApplyMapForTariff(tariff, newNetAmount);

                newTreasuryExemptionMapByType.entrySet().forEach(entry -> {
                    String exemptionReason = entry.getKey().getName()
                            .getContent(TreasuryPlataformDependentServicesFactory.implementation().defaultLocale());

                    TreasuryExemption.create(entry.getKey(), exemptionReason, entry.getValue(), newDebitEntry);

                });

                // 4. Annul the debit entry, interest entries cannot be annulled
                d.annulOnlyThisDebitEntryAndInterestsInBusinessContext(recalculationReason, false);
            } else if (d.getFinantialDocument().isClosed()) {
                Map<TreasuryExemption, BigDecimal> creditExemptionsMap =
                        d.calculateNetExemptedAmountsToCreditMapBasedInExplicitAmounts(netExemptedAmountToCreditByTypeMap, true);

                d.creditDebitEntry(netAmountToCredit, recalculationReason, false, creditExemptionsMap);

                if (isToBeAnnulledInTreasuryEvent(d)) {
                    // The net amount of debit entry is zero and the net exempted amount is zero,
                    // which means can be annulled in academic treasury event
                    d.annulOnEvent();
                }

            } else {
                throw new IllegalStateException("how to handle this case?");
            }
        }

        return true;
    }

    private boolean isToBeAnnulledInTreasuryEvent(DebitEntry debitEntry) {
        BigDecimal netExemptedAmount = debitEntry.getNetExemptedAmount();
        BigDecimal creditedNetExemptedAmount =
                debitEntry.getCreditEntriesSet().stream().filter(c -> !c.isAnnulled()).map(c -> c.getNetExemptedAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean availableNetAmountForCreditNotPositive =
                !TreasuryConstants.isPositive(debitEntry.getAvailableNetAmountForCredit());
        boolean netExemptedAmountNotPositive =
                !TreasuryConstants.isPositive(netExemptedAmount.subtract(creditedNetExemptedAmount));

        return availableNetAmountForCreditNotPositive && netExemptedAmountNotPositive;
    }

    private Boolean createUnchargedInstallmentDebitEntry(TuitionInstallmentTariff tariff) {
        BigDecimal tuitionInstallmentAmountToPay = tariff.amountToPay(this);

        if (!TreasuryConstants.isPositive(tuitionInstallmentAmountToPay)) {
            return false;
        }

        Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap =
                this._treasuryExemptionsTeller.retrieveUnchargedExemptionsToApplyMapForTariff(tariff,
                        tuitionInstallmentAmountToPay);

        createDebitEntryForRegistrationAndExempt(tariff, exemptionsToApplyMap);
        return true;
    }

    private LocalizedString ls(String value) {
        return new LocalizedString(TreasuryPlataformDependentServicesFactory.implementation().defaultLocale(), value);
    }

    private Boolean createRecalculationAdditionalDebitEntryForRegistrationAndExempt(LocalDate debtDate, DebtAccount debtAccount,
            AcademicTreasuryEvent academicTreasuryEvent, TuitionInstallmentTariff tariff,
            Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap, BigDecimal recalculatedAmount,
            LocalDate recalculationDueDate) {
        DebitEntry installmentDebitEntry =
                tariff.createRecalculationDebitEntryForRegistration(debtAccount, academicTreasuryEvent, debtDate,
                        this._calculatorsMap, recalculatedAmount, recalculationDueDate);

        for (Entry<TreasuryExemptionType, BigDecimal> entry : exemptionsToApplyMap.entrySet()) {
            TreasuryExemptionType treasuryExemptionType = entry.getKey();
            BigDecimal amountToExempt = entry.getValue();

            String reason = treasuryExemptionType.getName()
                    .getContent(TreasuryPlataformDependentServicesFactory.implementation().defaultLocale());
            TreasuryExemption.create(treasuryExemptionType, reason, amountToExempt, installmentDebitEntry);
        }

        return true;
    }

    private DebitEntry createDebitEntryForRegistrationAndExempt(TuitionInstallmentTariff tariff,
            Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap) {
        DebitEntry installmentDebitEntry = tariff.createDebitEntryForRegistration(this);

        for (Entry<TreasuryExemptionType, BigDecimal> entry : exemptionsToApplyMap.entrySet()) {
            TreasuryExemptionType treasuryExemptionType = entry.getKey();
            BigDecimal amountToExempt = entry.getValue();

            String reason = treasuryExemptionType.getName()
                    .getContent(TreasuryPlataformDependentServicesFactory.implementation().defaultLocale());
            TreasuryExemption.create(treasuryExemptionType, reason, amountToExempt, installmentDebitEntry);
        }

        return installmentDebitEntry;
    }

    public List<TuitionDebitEntryBean> executeInstallmentDebitEntryBeansCalculation() {
        initializeCreditsAndEnrolledCoursesCount();
        initializeTuitionPaymentPlan();
        initializeCustomCalculators();
        initializeOriginalAmountsCalculator();

        this._treasuryExemptionsTeller = new TreasuryExemptionsTeller(this);

        TuitionPaymentPlan tuitionPaymentPlan = this.tuitionOptions.tuitionPaymentPlan;

        if (tuitionPaymentPlan == null) {
            return Collections.emptyList();
        }

        if (!tuitionPaymentPlan.getTuitionPaymentPlanGroup().isForRegistration()) {
            throw new RuntimeException("wrong call");
        }

        Predicate<TuitionInstallmentTariff> isToCalculateInstallmentProduct =
                t -> (this.installmentOptions.installments == null || this.installmentOptions.installments.contains(
                        t.getProduct())) || (this.installmentRecalculationOptions.recalculateInstallments != null && this.installmentRecalculationOptions.recalculateInstallments.containsKey(
                        t.getProduct()));

        final Map<TuitionInstallmentTariff, TuitionDebitEntryBean> calculatedDebitEntryBeansMap = new HashMap<>();

        if (!this.isForCalculationsOfOriginalAmounts) {
            this._originalAmountsCalculator.executeInstallmentDebitEntryBeansCalculation().stream()
                    .forEach(e -> calculatedDebitEntryBeansMap.put(e.getTuitionInstallmentTariff(), e));
        }

        final List<TuitionDebitEntryBean> entries = tuitionPaymentPlan.getTuitionInstallmentTariffsSet().stream() //
                .sorted(TuitionInstallmentTariff.COMPARATOR_BY_INSTALLMENT_NUMBER) //
                .filter(isToCalculateInstallmentProduct) //
                .flatMap(t -> buildInstallmentDebitEntryBeanWithDiscount(calculatedDebitEntryBeansMap, t).stream()) //
                .filter(Objects::nonNull) //
                .collect(Collectors.toList());

        return entries;
    }

    private void initializeOriginalAmountsCalculator() {
        var registration = this.registrationOptions.registration;
        var executionYear = this.registrationOptions.executionYear;
        var debtDate = this.registrationOptions.debtDate;
        var tuitionPaymentPlan = this.tuitionOptions.tuitionPaymentPlan;
        var tuitionAllocation = this.tuitionOptions.tuitionAllocation;

        var serviceBuilder = startServiceInvocation(registration, executionYear, debtDate);

        serviceBuilder.applyEnrolledEctsUnits(this.registrationOptions.enrolledEctsUnits);
        serviceBuilder.applyEnrolledCoursesCount(this.registrationOptions.enrolledCoursesCount);
        serviceBuilder.applyDefaultEnrolmentCredits(this.registrationOptions.applyDefaultEnrolmentCredits);

        var tuitionPaymentPlanBuilder = serviceBuilder.withTuitionPaymentPlan(tuitionPaymentPlan);

        // ANIL 2025-03-03 (#qubIT-Fenix-6662)
        //
        // Very important to initialize the allocation, because of the
        // associated exemption types
        tuitionPaymentPlanBuilder.applyTuitionAllocation(tuitionAllocation);

        if (this.installmentOptions.installments != null) {
            var products = new HashSet<>(this.installmentOptions.installments);

            if (this.installmentRecalculationOptions.recalculateInstallments != null) {
                products.addAll(this.installmentRecalculationOptions.recalculateInstallments.keySet());
            }

            this._originalAmountsCalculator =
                    tuitionPaymentPlanBuilder.forceCreationIfNotEnrolled(true).restrictForInstallmentProducts(products)
                            .forceInstallmentsEvenTreasuryEventIsCharged(true).withoutInstallmentsRecalculation();
        } else {
            this._originalAmountsCalculator = tuitionPaymentPlanBuilder.forceCreationIfNotEnrolled(true).withAllInstallments()
                    .forceInstallmentsEvenTreasuryEventIsCharged(true).withoutInstallmentsRecalculation();
        }

        this._originalAmountsCalculator.isForCalculationsOfOriginalAmounts = true;
    }

    private void initializeCreditsAndEnrolledCoursesCount() {
        Registration registration = this.registrationOptions.registration;
        ExecutionYear executionYear = this.registrationOptions.executionYear;
        TuitionPaymentPlanGroup tuitionPaymentPlanGroup = TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get();

        if (this.registrationOptions.enrolledEctsUnits == null) {
            this.registrationOptions.enrolledEctsUnits =
                    AcademicTreasuryEvent.getEnrolledEctsUnits(tuitionPaymentPlanGroup, registration, executionYear);
        }

        if (this.registrationOptions.enrolledCoursesCount == null) {
            this.registrationOptions.enrolledCoursesCount =
                    AcademicTreasuryEvent.getEnrolledCoursesCount(tuitionPaymentPlanGroup, registration, executionYear);
        }
    }

    // algorithm
    private List<TuitionDebitEntryBean> buildInstallmentDebitEntryBeanWithDiscount(
            Map<TuitionInstallmentTariff, TuitionDebitEntryBean> calculatedDebitEntryBeansMap, TuitionInstallmentTariff tariff) {
        Product product = tariff.getProduct();
        Currency currency = tariff.getFinantialEntity().getFinantialInstitution().getCurrency();

        TuitionPaymentPlan tuitionPaymentPlan = this.tuitionOptions.tuitionPaymentPlan;
        Registration registration = this.registrationOptions.registration;
        LocalDate debtDate = this.registrationOptions.debtDate;

        final int installmentOrder = tariff.getInstallmentOrder();
        final LocalizedString installmentName = tuitionPaymentPlan.installmentName(registration, tariff);
        final LocalDate dueDate = tariff.dueDate(debtDate);
        final Vat vat = tariff.vat(debtDate);

        this._treasuryExemptionsTeller.createDiscountExemptionsMapForOnlyThisInstallment(tariff);

        boolean isToRecalculateInstallment =
                this.installmentRecalculationOptions.recalculateInstallments != null && this.installmentRecalculationOptions.recalculateInstallments.containsKey(
                        product);

        if (isToRecalculateInstallment && isTuitionInstallmentCharged(product)) {
            // Recalculate instead of new installment debt

            // Get the amounts that should be debited
            TuitionDebitEntryBean originalBean = calculatedDebitEntryBeansMap.computeIfAbsent(tariff,
                    t -> new TuitionDebitEntryBean(installmentOrder, t, ls("Dummy installment"), debtDate, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, new HashMap<>(), currency));

            BigDecimal differenceNetAmount = originalBean.getAmount().subtract(getNetAmountAlreadyDebited(product));
            BigDecimal differenceInNetExemptedAmount =
                    originalBean.getExemptedAmount().subtract(getNetAmountAlreadyExempted(product));

            AcademicTreasuryEvent academicTreasuryEvent =
                    AcademicTreasuryEvent.findUniqueForRegistrationTuition(this.registrationOptions.registration,
                            this.registrationOptions.executionYear).get();

            boolean isExemptionsMapAreEqual =
                    this._treasuryExemptionsTeller.isExemptionsMapAreEqual(tariff, academicTreasuryEvent, originalBean);
            boolean isThereIsOnlyRemovalsOrDecrementsInExemptions =
                    this._treasuryExemptionsTeller.isThereIsOnlyRemovalsOrDecrementsInExemptions(tariff, academicTreasuryEvent,
                            originalBean);
            boolean isThereAreOnlyNewEntriesOrIncrementsInExemptions =
                    this._treasuryExemptionsTeller.isThereAreOnlyNewEntriesOrIncrementsInExemptions(tariff, academicTreasuryEvent,
                            originalBean);
            boolean isThereAreRemovalOrDecrementsInExemptions =
                    this._treasuryExemptionsTeller.isThereAreRemovalOrDecrementsInExemptions(tariff, academicTreasuryEvent,
                            originalBean);

            boolean isDifferenceNetAmountZero = TreasuryConstants.isZero(differenceNetAmount);
            boolean isDifferenceNetAmountNegativeOrZero = !TreasuryConstants.isPositive(differenceNetAmount);
            boolean isDifferenceNetAmountPositiveOrZero = !TreasuryConstants.isNegative(differenceNetAmount);

            if (isDifferenceNetAmountZero && isExemptionsMapAreEqual) {
                // Before and after is equal, nothing to do
                return Collections.emptyList();
            } else if (isDifferenceNetAmountNegativeOrZero && (isExemptionsMapAreEqual || isThereIsOnlyRemovalsOrDecrementsInExemptions)) {
                LocalDate recalculationDueDate = this.installmentRecalculationOptions.recalculateInstallments.get(product);

                LocalizedString recalculationInstallmentName = installmentName;
                if (tuitionPaymentPlan.getTuitionPaymentPlanGroup().getTuitionRecalculationDebitEntryPrefix() != null) {
                    recalculationInstallmentName =
                            tuitionPaymentPlan.getTuitionPaymentPlanGroup().getTuitionRecalculationDebitEntryPrefix()
                                    .append(recalculationInstallmentName, " ");
                }

                if (tuitionPaymentPlan.getTuitionPaymentPlanGroup().getTuitionRecalculationDebitEntrySuffix() != null) {
                    recalculationInstallmentName = recalculationInstallmentName.append(
                            tuitionPaymentPlan.getTuitionPaymentPlanGroup().getTuitionRecalculationDebitEntrySuffix(), " ");
                }

                recalculationInstallmentName = trim(recalculationInstallmentName);

                return Collections.singletonList(
                        new TuitionDebitEntryBean(installmentOrder, tariff, recalculationInstallmentName, recalculationDueDate,
                                vat.getTaxRate(), differenceNetAmount, differenceInNetExemptedAmount, new HashMap<>(), currency));
            } else if (isDifferenceNetAmountPositiveOrZero && (isExemptionsMapAreEqual || isThereAreOnlyNewEntriesOrIncrementsInExemptions)) {

                // Create debit entry with the positive difference
                LocalDate recalculationDueDate = this.installmentRecalculationOptions.recalculateInstallments.get(product);

                LocalizedString recalculationInstallmentName = installmentName;
                if (tuitionPaymentPlan.getTuitionPaymentPlanGroup().getTuitionRecalculationDebitEntryPrefix() != null) {
                    recalculationInstallmentName =
                            tuitionPaymentPlan.getTuitionPaymentPlanGroup().getTuitionRecalculationDebitEntryPrefix()
                                    .append(recalculationInstallmentName, " ");
                }

                if (tuitionPaymentPlan.getTuitionPaymentPlanGroup().getTuitionRecalculationDebitEntrySuffix() != null) {
                    recalculationInstallmentName = recalculationInstallmentName.append(
                            tuitionPaymentPlan.getTuitionPaymentPlanGroup().getTuitionRecalculationDebitEntrySuffix(), " ");
                }

                recalculationInstallmentName = trim(recalculationInstallmentName);

                this._treasuryExemptionsTeller.retrieveAdditionalExemptionsToApplyMapForTariff(tariff, product, originalBean,
                        differenceInNetExemptedAmount);

                return Collections.singletonList(
                        new TuitionDebitEntryBean(installmentOrder, tariff, recalculationInstallmentName, recalculationDueDate,
                                vat.getTaxRate(), differenceNetAmount, differenceInNetExemptedAmount, new HashMap<>(), currency));
            } else if (TreasuryConstants.isNegative(differenceInNetExemptedAmount) || (TreasuryConstants.isNegative(
                    differenceNetAmount) && !TreasuryConstants.isZero(
                    differenceInNetExemptedAmount)) || isThereAreRemovalOrDecrementsInExemptions) {

                // It is easier and safe to annul the created debit entries
                // and create a new one

                // Annulment bean
                LocalizedString annulmentInstallmentName = AcademicTreasuryConstants.academicTreasuryBundleI18N(
                        "label.RegistrationTuitionService.annulment.installmentName.prefix").append(installmentName);
                LocalDate recalculationDueDate = this.installmentRecalculationOptions.recalculateInstallments.get(product);

                TuitionDebitEntryBean annulmentBean =
                        new TuitionDebitEntryBean(installmentOrder, tariff, annulmentInstallmentName, recalculationDueDate,
                                vat.getTaxRate(), getNetAmountAlreadyDebited(product).negate(),
                                getNetAmountAlreadyExempted(product).negate(), new HashMap<>(), currency);

                TuitionDebitEntryBean recalculationBean =
                        new TuitionDebitEntryBean(installmentOrder, tariff, installmentName, recalculationDueDate,
                                vat.getTaxRate(), originalBean.getAmount(), originalBean.getExemptedAmount(), new HashMap<>(),
                                currency);

                // We have to remove from the map, in order to not be used in the subsequent installments
                this._treasuryExemptionsTeller.retrieveUnchargedExemptionsToApplyMapForTariff(tariff,
                        originalBean.getExemptedAmount());

                return List.of(annulmentBean, recalculationBean);
            }

            throw new IllegalStateException("recalculation: do not know how to handle this case???");
        } else if (!isTuitionInstallmentCharged(product) || this.isForCalculationsOfOriginalAmounts) {
            BigDecimal tuitionInstallmentAmountToPay = tariff.amountToPay(this);

            if (!TreasuryConstants.isPositive(tuitionInstallmentAmountToPay)) {
                return Collections.emptyList();
            }

            Map<TreasuryExemptionType, BigDecimal> exemptionsMapToApply =
                    this._treasuryExemptionsTeller.retrieveUnchargedExemptionsToApplyMapForTariff(tariff,
                            tuitionInstallmentAmountToPay);

            BigDecimal totalAmountToExempt = exemptionsMapToApply.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal finalAmountToPay = tuitionInstallmentAmountToPay.subtract(totalAmountToExempt);

            return Collections.singletonList(
                    new TuitionDebitEntryBean(installmentOrder, tariff, installmentName, dueDate, vat.getTaxRate(),
                            finalAmountToPay, totalAmountToExempt, exemptionsMapToApply, currency));
        } else {
            return Collections.emptyList();
        }
    }

    private LocalizedString trim(LocalizedString value) {
        return value.getLocales().stream()
                .map(l -> new LocalizedString(l, value.getContent(l) != null ? value.getContent(l).trim() : null))
                .reduce(new LocalizedString(), LocalizedString::append);
    }

    private BigDecimal getNetAmountAlreadyExempted(Product product) {
        return AcademicTreasuryEvent.findUniqueForRegistrationTuition(this.registrationOptions.registration,
                this.registrationOptions.executionYear).map(e -> e.getNetExemptedAmount(product)).orElse(BigDecimal.ZERO);
    }

    private BigDecimal getNetAmountAlreadyDebited(Product product) {
        return AcademicTreasuryEvent.findUniqueForRegistrationTuition(this.registrationOptions.registration,
                this.registrationOptions.executionYear).map(e -> e.getNetAmountToPay(product)).orElse(BigDecimal.ZERO);
    }

    private boolean isTuitionInstallmentCharged(Product product) {
        return AcademicTreasuryEvent.findUniqueForRegistrationTuition(this.registrationOptions.registration,
                this.registrationOptions.executionYear).map(e -> e.isChargedWithDebitEntry(product)).orElse(false);
    }

    private void initializeCustomCalculators() {
        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        TuitionPaymentPlan tuitionPaymentPlan = this.tuitionOptions.tuitionPaymentPlan;
        Registration registration = this.registrationOptions.registration;

        StringBuilder strBuilder = new StringBuilder();

        if (Boolean.TRUE.equals(tuitionPaymentPlan.getTuitionPaymentPlanGroup().getApplyDomainObjectCalculators())) {
            tuitionPaymentPlan.getTuitionInstallmentTariffsSet().stream() //
                    .filter(tariff -> tariff.getTuitionPaymentPlanCalculator() != null) //
                    .map(tariff -> tariff.getTuitionPaymentPlanCalculator()) //
                    .collect(Collectors.toSet()).forEach(calculator -> {
                        strBuilder.append(calculator.getName().getContent(services.defaultLocale())).append(" (")
                                .append(calculator.getTotalAmount(registration, this)).append("): \n");

                        strBuilder.append(calculator.getCalculationDescription(registration)).append("\n");
                    });

        } else {
            this._calculatorsMap = new HashMap<>();

            tuitionPaymentPlan.getTuitionInstallmentTariffsSet().stream()
                    .filter(t -> t.getTuitionTariffCustomCalculator() != null)
                    .map(TuitionInstallmentTariff::getTuitionTariffCustomCalculator).collect(Collectors.toSet())
                    .forEach(clazz -> {
                        if (clazz != null) {
                            TuitionTariffCustomCalculator newInstanceFor =
                                    TuitionTariffCustomCalculator.getNewInstanceFor(clazz, registration, tuitionPaymentPlan);

                            this._calculatorsMap.put(clazz, newInstanceFor);

                            strBuilder.append(newInstanceFor.getPresentationName()).append(" (")
                                    .append(newInstanceFor.getTotalAmount()).append("): \n");

                            strBuilder.append(newInstanceFor.getCalculationDescription()).append("\n");
                        }
                    });
        }

        this._calculationDescription = strBuilder.toString();
    }

    private void initializeTuitionPaymentPlan() {
        if (this.tuitionOptions.tuitionPaymentPlan == null) {
            this.tuitionOptions.tuitionPaymentPlan =
                    TuitionPaymentPlan.inferTuitionPaymentPlanForRegistration(this.registrationOptions.registration,
                            this.registrationOptions.executionYear);
        }

        // Load recalculations from tuition payment plan
        if (!this.isForCalculationsOfOriginalAmounts && this.tuitionOptions.tuitionPaymentPlan != null) {
            this.tuitionOptions.tuitionPaymentPlan.getTuitionPaymentPlanRecalculationsSet().stream().forEach(r -> {
                if (this.installmentRecalculationOptions.recalculateInstallments == null) {
                    this.installmentRecalculationOptions.recalculateInstallments = new HashMap<>();
                }

                this.installmentRecalculationOptions.recalculateInstallments.putIfAbsent(r.getProduct(),
                        r.getRecalculationDueDate());
            });
        }

    }

    // SERVICE METHODS

    public static RegistrationOptions startServiceInvocation(Registration registration, ExecutionYear executionYear,
            LocalDate debtDate) {
        RegistrationTuitionService service = new RegistrationTuitionService();

        return new RegistrationOptions(service, registration, executionYear, debtDate);
    }

    // HELPER CLASSES

    @Override
    public Registration getRegistration() {
        return this.registrationOptions.registration;
    }

    @Override
    public ExecutionYear getExecutionYear() {
        return this.registrationOptions.executionYear;
    }

    @Override
    public Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> getCustomCalculatorsMap() {
        return this._calculatorsMap;
    }

    @Override
    public BigDecimal getEnrolledEctsUnits() {
        return this.registrationOptions.enrolledEctsUnits;
    }

    @Override
    public BigDecimal getEnrolledCoursesCount() {
        return this.registrationOptions.enrolledCoursesCount;
    }

    @Override
    public boolean isApplyDefaultEnrolmentCredits() {
        return this.registrationOptions.applyDefaultEnrolmentCredits;
    }

    @Override
    public Optional<DebtAccount> getDebtAccount() {
        final Person person = getRegistration().getPerson();
        final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);
        if (Strings.isNullOrEmpty(addressFiscalCountryCode) || Strings.isNullOrEmpty(fiscalNumber)) {
            return Optional.empty();
        }

        // Read person customer

        if (PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).isEmpty()) {
            return Optional.empty();
        }

        final PersonCustomer personCustomer = PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).get();
        if (!personCustomer.isActive()) {
            return Optional.empty();
        }

        if (this.tuitionOptions.tuitionPaymentPlan == null) {
            return Optional.empty();
        }

        TuitionPaymentPlan tuitionPaymentPlan = this.tuitionOptions.tuitionPaymentPlan;

        return DebtAccount.findUnique(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer);
    }

    @Override
    public Optional<AcademicTreasuryEvent> getAcademicTreasuryEvent() {
        return AcademicTreasuryEvent.findUniqueForRegistrationTuition(getRegistration(), getExecutionYear())
                .map(AcademicTreasuryEvent.class::cast);
    }

    @Override
    public LocalDate getDebtDate() {
        return this.registrationOptions.debtDate;
    }

}
