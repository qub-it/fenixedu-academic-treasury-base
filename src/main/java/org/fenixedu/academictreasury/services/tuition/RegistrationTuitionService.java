package org.fenixedu.academictreasury.services.tuition;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tuition.ITuitionRegistrationServiceParameters;
import org.fenixedu.academictreasury.domain.tuition.TuitionInstallmentTariff;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.academictreasury.domain.tuition.TuitionTariffCustomCalculator;
import org.fenixedu.academictreasury.domain.tuition.exemptions.StatuteExemptionByIntervalMapEntry;
import org.fenixedu.academictreasury.dto.tuition.TuitionDebitEntryBean;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
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
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;

public class RegistrationTuitionService implements ITuitionRegistrationServiceParameters {

    boolean isForCalculationsOfOriginalAmounts = false;

    RegistrationOptions registrationOptions;
    TuitionOptions tuitionOptions;
    InstallmentOptions installmentOptions;
    InstallmentRecalculationOptions installmentRecalculationOptions;

    // This map will hold the exemption amounts to apply in installment debit entries
    TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> _discountExemptionsMapForAllInstallments;

    Map<Class<? extends TuitionTariffCustomCalculator>, TuitionTariffCustomCalculator> _calculatorsMap;
    String _calculationDescription;

    RegistrationTuitionService _originalAmountsCalculator;

    public boolean executeTuitionPaymentPlanCreation() {

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

        if (!forceCreationIfNotEnrolled && tuitionPaymentPlan.isStudentMustBeEnrolled()
                && TuitionServices.normalEnrolmentsIncludingAnnuled(registration, executionYear).isEmpty()) {
            return false;
        }

        final Person person = registration.getPerson();
        final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);
        if (Strings.isNullOrEmpty(addressFiscalCountryCode) || Strings.isNullOrEmpty(fiscalNumber)) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalInformation.required");
        }

        // Read person customer

        if (!PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).isPresent()) {
            PersonCustomer.create(person, addressFiscalCountryCode, fiscalNumber);
        }

        final PersonCustomer personCustomer = PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).get();
        if (!personCustomer.isActive()) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.not.active",
                    personCustomer.getBusinessIdentification(), personCustomer.getName());
        }

        if (!DebtAccount.findUnique(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer)
                .isPresent()) {
            DebtAccount.create(tuitionPaymentPlan.getFinantialEntity().getFinantialInstitution(), personCustomer);
        }

        if (!AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).isPresent()) {
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

        initializeDiscountExemptionsMapForAllInstallments();

        Map<TuitionInstallmentTariff, TuitionDebitEntryBean> calculatedDebitEntryBeansMap =
                this._originalAmountsCalculator.executeInstallmentDebitEntryBeansCalculation().stream()
                        .collect(Collectors.toMap(TuitionDebitEntryBean::getTuitionInstallmentTariff, Function.identity()));

        if (this._calculationDescription.length() > 0) {
            Map<String, String> propertiesMap = academicTreasuryEvent.getPropertiesMap();

            String key = AcademicTreasuryConstants.academicTreasuryBundle("label.AcademicTreasury.CustomCalculatorDescription")
                    + " ( " + DateTime.now().toString("yyyy-MM-dd HH:mm") + " )";

            propertiesMap.put(key, this._calculationDescription);

            academicTreasuryEvent.editPropertiesMap(propertiesMap);
        }

        Stream<TuitionInstallmentTariff> installments = tuitionPaymentPlan.getTuitionInstallmentTariffsSet().stream()
                .sorted(TuitionInstallmentTariff.COMPARATOR_BY_INSTALLMENT_NUMBER);

        Function<TuitionInstallmentTariff, Boolean> func = tariff -> {
            Product product = tariff.getProduct();

            TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> _discountExemptionsMapForOnlyThisInstallment =
                    buildDiscountExemptionsMapForOnlyThisInstallment(tariff);

            boolean isToRecalculateInstallment = this.installmentRecalculationOptions.recalculateInstallments != null
                    && this.installmentRecalculationOptions.recalculateInstallments.containsKey(product);
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

                if (TreasuryConstants.isZero(differenceNetAmount) && TreasuryConstants.isZero(differenceInNetExemptedAmount)
                        && isExemptionsMapAreEqual(tariff, academicTreasuryEvent, originalBean)) {
                    // Nothing to do, there are no differences in the amounts
                    return false;
                }

                // This is the case where we only need to create a credit
                if (TreasuryConstants.isNegative(differenceNetAmount) && TreasuryConstants.isZero(differenceInNetExemptedAmount)
                        && isExemptionsMapAreEqual(tariff, academicTreasuryEvent, originalBean)) {
                    // Close the debit entries and credit with the differenceAmount
                    closeDebitEntriesInDebitNote(academicTreasuryEvent, tariff.getProduct());

                    BigDecimal remainingAmountToCredit = differenceNetAmount.negate();
                    List<DebitEntry> debitEntriesToCreditList = DebitEntry.findActive(academicTreasuryEvent, product)
                            .sorted(DebitEntry.COMPARE_BY_EXTERNAL_ID.reversed()).collect(Collectors.toList());
                    for (DebitEntry d : debitEntriesToCreditList) {
                        BigDecimal netAmountToCredit = remainingAmountToCredit.min(d.getAvailableNetAmountForCredit());
                        remainingAmountToCredit = netAmountToCredit.subtract(netAmountToCredit);

                        if (TreasuryConstants.isPositive(netAmountToCredit)) {
                            String reason = TreasuryConstants
                                    .treasuryBundle("label.RegistrationTuitionService.tuitionRecalculationReason");
                            d.creditDebitEntry(netAmountToCredit, reason, false);

                            if (!TreasuryConstants.isPositive(d.getAvailableNetAmountForCredit())
                                    && !TreasuryConstants.isPositive(d.getNetExemptedAmount())) {
                                // The net amount of debit entry is zero, which means can be annuled in academic treasury event
                                d.annulOnEvent();
                            }
                        }
                    }

                    return true;
                }

                if (!TreasuryConstants.isNegative(differenceNetAmount)
                        && !TreasuryConstants.isNegative(differenceInNetExemptedAmount)
                        && isThereAreOnlyNewEntriesOrIncrementsInExemptions(tariff, academicTreasuryEvent, originalBean)) {
                    // This combination of difference amounts, allow us to create a new recalculation debit entry
                    LocalDate recalculationDueDate = this.installmentRecalculationOptions.recalculateInstallments.get(product);

                    // Now it is time to discount from both discount exemption maps, 
                    // for this installment and from all installments
                    Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap = new HashMap<>();

                    // We need how to take the difference exemption. We take from the
                    // mapForOnlyThisInstallment or the mapForAllInstallments exemption map? And how much to take
                    // from one and the other?

                    // 1. Know how much it will take from the mapForOnlyThisInstallment, we need to make a simulation
                    BigDecimal simulationOfTotalExemptedAmountForOnlyThisInstallment =
                            getAndDecrementFromDiscountMap(buildDiscountExemptionsMapForOnlyThisInstallment(tariff),
                                    originalBean.getExemptedAmount(), new HashMap<>());

                    // 2. Know how much was taken from the mapForOnlyThisInstallment in the already exempted amount
                    BigDecimal simulationOfAlreadyExemptedAmountForOnlyThisInstallment = getAndDecrementFromDiscountMap(
                            buildDiscountExemptionsMapForOnlyThisInstallmentByAmount(
                                    getNetAmountAlreadyDebited(product).add(getNetAmountAlreadyExempted(product))),
                            getNetAmountAlreadyExempted(product), new HashMap<>());

                    BigDecimal netExemptedAmountToTakeFromExemptionsMapForAllInstallments = differenceInNetExemptedAmount;
                    if (TreasuryConstants.isGreaterOrEqualThan(simulationOfTotalExemptedAmountForOnlyThisInstallment,
                            simulationOfAlreadyExemptedAmountForOnlyThisInstallment)) {
                        // 3. With the difference exempted amount, get the exemptions to apply for the debit entry

                        getAndDecrementFromDiscountMap(_discountExemptionsMapForOnlyThisInstallment,
                                simulationOfAlreadyExemptedAmountForOnlyThisInstallment, new HashMap<>());

                        BigDecimal totalExemptedAmountFromExemptionsMapForOnlyThisInstallment =
                                getAndDecrementFromDiscountMap(_discountExemptionsMapForOnlyThisInstallment,
                                        simulationOfTotalExemptedAmountForOnlyThisInstallment.subtract(
                                                simulationOfAlreadyExemptedAmountForOnlyThisInstallment),
                                        exemptionsToApplyMap);

                        netExemptedAmountToTakeFromExemptionsMapForAllInstallments =
                                netExemptedAmountToTakeFromExemptionsMapForAllInstallments
                                        .subtract(totalExemptedAmountFromExemptionsMapForOnlyThisInstallment);
                    } else {
                        // Nothing to do, we only need to take the difference and decrement
                        // with mapForAllInstallments
                    }

                    // 5. Now discount from the acrossAllInstallments exemption map
                    getAndDecrementFromDiscountMap(this._discountExemptionsMapForAllInstallments,
                            netExemptedAmountToTakeFromExemptionsMapForAllInstallments, exemptionsToApplyMap);

                    BigDecimal debitEntryNetAmount = differenceNetAmount.add(differenceInNetExemptedAmount);

                    return createRecalculationAdditionalDebitEntryForRegistrationAndExempt(debtDate, debtAccount,
                            academicTreasuryEvent, tariff, exemptionsToApplyMap, debitEntryNetAmount, recalculationDueDate);
                }

                if (TreasuryConstants.isNegative(differenceInNetExemptedAmount)
                        || (TreasuryConstants.isNegative(differenceNetAmount)
                                && !TreasuryConstants.isZero(differenceInNetExemptedAmount))
                        || isThereAreRemovalOrDecrementsInExemptions(tariff, academicTreasuryEvent, originalBean)) {
                    // The difference in the exemption amount is negative, or there is a credit to create but the
                    // the difference in the exemption is different than zero. Then we have to annul the all debit entries
                    // of installment and create again

                    // TODO: Consider the annullment of debit entry, instead of closing and crediting
                    closeDebitEntriesInDebitNote(academicTreasuryEvent, tariff.getProduct());
                    revertExemptionAmountsFromAcademicTreasuryToDiscountExemptionsMapForAllInstallments(tariff);

                    DebitEntry.findActive(academicTreasuryEvent, product)
                            .forEach(d -> d.annulOnlyThisDebitEntryAndInterestsInBusinessContext(AcademicTreasuryConstants
                                    .academicTreasuryBundle("label.RegistrationTuitionService.tuitionRecalculationReason")));

                    Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap = new HashMap<>();
                    BigDecimal tuitionInstallmentAmountToPay = originalBean.getAmount().add(originalBean.getExemptedAmount());
                    BigDecimal netAmountToExemptForOnlyThisInstallment = getAndDecrementFromDiscountMap(
                            _discountExemptionsMapForOnlyThisInstallment, tuitionInstallmentAmountToPay, exemptionsToApplyMap);

                    getAndDecrementFromDiscountMap(this._discountExemptionsMapForAllInstallments,
                            tuitionInstallmentAmountToPay.subtract(netAmountToExemptForOnlyThisInstallment),
                            exemptionsToApplyMap);

                    return createDebitEntryForRegistrationAndExempt(debtDate, debtAccount, academicTreasuryEvent, tariff,
                            exemptionsToApplyMap);
                }

                throw new IllegalStateException("reculation: do not know how to handle this case???");
            } else if (!isTuitionInstallmentCharged(product)) {
                return createUnchargedInstallmentDebitEntry(debtDate, debtAccount, academicTreasuryEvent, tariff,
                        _discountExemptionsMapForOnlyThisInstallment);
            } else if (isTuitionInstallmentCharged(product)) {
                return false;
            } else {
                throw new IllegalStateException("reculation: do not know how to handle this case???");
            }
        };

        Predicate<TuitionInstallmentTariff> isToCalculateInstallmentProduct = t -> (this.installmentOptions.installments == null
                || this.installmentOptions.installments.contains(t.getProduct()))
                || (this.installmentRecalculationOptions.recalculateInstallments != null
                        && this.installmentRecalculationOptions.recalculateInstallments.containsKey(t.getProduct()));

        return installments //
                .filter(isToCalculateInstallmentProduct) //
                .map(func) //
                .reduce(Boolean.FALSE, Boolean::logicalOr);
    }

    private boolean isThereAreRemovalOrDecrementsInExemptions(TuitionInstallmentTariff tariff,
            AcademicTreasuryEvent academicTreasuryEvent, TuitionDebitEntryBean newTuitionDebitEntryBean) {
        Map<TreasuryExemptionType, BigDecimal> alreadyCreatedMap =
                academicTreasuryEvent.getNetExemptedAmountsMap(tariff.getProduct());
        Map<TreasuryExemptionType, BigDecimal> newTuitionDebitEntryBeanExemptionsMap =
                newTuitionDebitEntryBean.getExemptionsMap();

        Predicate<Map.Entry<TreasuryExemptionType, BigDecimal>> predicate =
                alreadyExemptedEntry -> !newTuitionDebitEntryBeanExemptionsMap.containsKey(alreadyExemptedEntry.getKey())
                        || TreasuryConstants.isLessThan(newTuitionDebitEntryBeanExemptionsMap.get(alreadyExemptedEntry.getKey()),
                                alreadyExemptedEntry.getValue());

        if (alreadyCreatedMap.entrySet().stream().anyMatch(predicate)) {
            return true;
        }

        return false;
    }

    private boolean isThereAreOnlyNewEntriesOrIncrementsInExemptions(TuitionInstallmentTariff tariff,
            AcademicTreasuryEvent academicTreasuryEvent, TuitionDebitEntryBean newTuitionDebitEntryBean) {
        Map<TreasuryExemptionType, BigDecimal> alreadyCreatedMap =
                academicTreasuryEvent.getNetExemptedAmountsMap(tariff.getProduct());
        Map<TreasuryExemptionType, BigDecimal> newTuitionDebitEntryBeanExemptionsMap =
                newTuitionDebitEntryBean.getExemptionsMap();

        if (alreadyCreatedMap.keySet().size() > newTuitionDebitEntryBeanExemptionsMap.keySet().size()) {
            return false;
        }

        Predicate<Map.Entry<TreasuryExemptionType, BigDecimal>> predicate =
                e -> !newTuitionDebitEntryBeanExemptionsMap.containsKey(e.getKey())
                        || TreasuryConstants.isLessThan(newTuitionDebitEntryBeanExemptionsMap.get(e.getKey()), e.getValue());

        if (alreadyCreatedMap.entrySet().stream().anyMatch(predicate)) {
            return false;
        }

        return true;
    }

    private boolean isExemptionsMapAreEqual(TuitionInstallmentTariff tariff, AcademicTreasuryEvent academicTreasuryEvent,
            TuitionDebitEntryBean newTuitionDebitEntryBean) {
        Map<TreasuryExemptionType, BigDecimal> alreadyCreatedMap =
                academicTreasuryEvent.getNetExemptedAmountsMap(tariff.getProduct());
        Map<TreasuryExemptionType, BigDecimal> originalExemptionsMap = newTuitionDebitEntryBean.getExemptionsMap();

        if (alreadyCreatedMap.keySet().equals(originalExemptionsMap.keySet())) {
            return true;
        }

        Predicate<Map.Entry<TreasuryExemptionType, BigDecimal>> predicate =
                e -> TreasuryConstants.isEqual(e.getValue(), originalExemptionsMap.get(e.getKey()));

        return alreadyCreatedMap.entrySet().stream().allMatch(predicate);
    }

    private Boolean createUnchargedInstallmentDebitEntry(LocalDate debtDate, final DebtAccount debtAccount,
            final AcademicTreasuryEvent academicTreasuryEvent, TuitionInstallmentTariff tariff,
            TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> _discountExemptionsMapForOnlyThisInstallment) {
        BigDecimal tuitionInstallmentAmountToPay = tariff.amountToPay(this);

        if (!TreasuryConstants.isPositive(tuitionInstallmentAmountToPay)) {
            return false;
        }

        Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap = new HashMap<>();
        BigDecimal netAmountToExemptForOnlyThisInstallment = getAndDecrementFromDiscountMap(
                _discountExemptionsMapForOnlyThisInstallment, tuitionInstallmentAmountToPay, exemptionsToApplyMap);

        getAndDecrementFromDiscountMap(this._discountExemptionsMapForAllInstallments,
                tuitionInstallmentAmountToPay.subtract(netAmountToExemptForOnlyThisInstallment), exemptionsToApplyMap);

        return createDebitEntryForRegistrationAndExempt(debtDate, debtAccount, academicTreasuryEvent, tariff,
                exemptionsToApplyMap);
    }

    private void closeDebitEntriesInDebitNote(AcademicTreasuryEvent academicTreasuryEvent, Product product) {
        Predicate<DebitEntry> isDebitEntryInPreparingDocument =
                de -> de.getFinantialDocument() != null && de.getFinantialDocument().isPreparing();

        Map<DebtAccount, DebitNote> preparingDebitNotesMapByDebtAccount = DebitEntry.findActive(academicTreasuryEvent, product)
                .filter(isDebitEntryInPreparingDocument).map(DebitEntry::getDebitNote)
                .collect(Collectors.toMap(
                        note -> note.getPayorDebtAccount() != null ? note.getPayorDebtAccount() : note.getDebtAccount(),
                        Function.identity()));

        DebitEntry.findActive(academicTreasuryEvent, product).filter(de -> de.getFinantialDocument() == null).forEach(de -> {
            DebtAccount debitEntryDebtAccount = de.getPayorDebtAccount() != null ? de.getPayorDebtAccount() : de.getDebtAccount();

            DebitNote newlyDebitNote = preparingDebitNotesMapByDebtAccount.computeIfAbsent(debitEntryDebtAccount,
                    da -> createDebitNoteForPayorDebtAccount(de.getDebtAccount(), da));

            preparingDebitNotesMapByDebtAccount.put(debitEntryDebtAccount, newlyDebitNote);
            de.setFinantialDocument(newlyDebitNote);
        });

        preparingDebitNotesMapByDebtAccount.values().forEach(note -> note.closeDocument());
    }

    private DebitNote createDebitNoteForPayorDebtAccount(DebtAccount studentDebtAccount, DebtAccount debitEntryDebtAccount) {
        DebtAccount payorDebtAccount = studentDebtAccount != debitEntryDebtAccount ? debitEntryDebtAccount : null;
        FinantialInstitution finantialInstitution = studentDebtAccount.getFinantialInstitution();

        return DebitNote.create(studentDebtAccount, payorDebtAccount,
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get(),
                new DateTime(), new LocalDate(), null);
    }

    private void revertExemptionAmountsFromAcademicTreasuryToDiscountExemptionsMapForAllInstallments(
            TuitionInstallmentTariff tariff) {
        Registration registration = this.registrationOptions.registration;
        ExecutionYear executionYear = this.registrationOptions.executionYear;

        AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).ifPresent(tuitionEvent -> {
            // We will need to calculate the correct exempted amount to remove from the 
            // discount map for all installments (given by other treasury events), so
            // we have to iterate by installmentTariff

            Product product = tariff.getProduct();

            Map<TreasuryExemptionType, BigDecimal> exemptedAmountsByTypeMap =
                    DebitEntry.findActive(tuitionEvent, product).flatMap(d -> d.getTreasuryExemptionsSet().stream())
                            .collect(Collectors.toMap(TreasuryExemption::getTreasuryExemptionType,
                                    TreasuryExemption::getNetAmountToExempt, BigDecimal::add));

            for (Entry<TreasuryExemptionType, BigDecimal> entry : exemptedAmountsByTypeMap.entrySet()) {
                TreasuryExemptionType treasuryExemptionType = entry.getKey();

                if (!this._discountExemptionsMapForAllInstallments.containsKey(treasuryExemptionType)) {
                    // Not in the discountMap, nothing to revert, continue
                    continue;
                }

                BigDecimal netExemptedAmount = entry.getValue();

                BigDecimal totalDebitCreatedIncludingExemptions = DebitEntry.findActive(tuitionEvent, product)
                        .map(d -> d.getNetAmount().add(d.getNetExemptedAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);

                // First, the already exempted amount must fill the map for the exemptions map
                // for this installment until it's maximum amount
                BigDecimal maximumAmountToExemptFromDiscountExemptionsMapForOnlyThisInstallment =
                        buildDiscountExemptionsMapForOnlyThisInstallmentByAmount(totalDebitCreatedIncludingExemptions)
                                .computeIfAbsent(treasuryExemptionType,
                                        t -> TreasuryExemptionMoneyBox.zero()).maximumNetAmountForExemption
                                                .min(netExemptedAmount);

                BigDecimal remainingAmountToRemoveFromDiscountMapForAllInstallments =
                        netExemptedAmount.subtract(maximumAmountToExemptFromDiscountExemptionsMapForOnlyThisInstallment);

                this._discountExemptionsMapForAllInstallments.get(treasuryExemptionType)
                        .addToAvailableNetAmountForExemption(remainingAmountToRemoveFromDiscountMapForAllInstallments);
            }

        });
    }

    private LocalizedString ls(String value) {
        return new LocalizedString(TreasuryPlataformDependentServicesFactory.implementation().defaultLocale(), value);
    }

    private Boolean createRecalculationAdditionalDebitEntryForRegistrationAndExempt(LocalDate debtDate, DebtAccount debtAccount,
            AcademicTreasuryEvent academicTreasuryEvent, TuitionInstallmentTariff tariff,
            Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap, BigDecimal recalculatedAmount,
            LocalDate recalculationDueDate) {
        DebitEntry installmentDebitEntry = tariff.createRecalculationDebitEntryForRegistration(debtAccount, academicTreasuryEvent,
                debtDate, this._calculatorsMap, recalculatedAmount, recalculationDueDate);

        for (Entry<TreasuryExemptionType, BigDecimal> entry : exemptionsToApplyMap.entrySet()) {
            TreasuryExemptionType treasuryExemptionType = entry.getKey();
            BigDecimal amountToExempt = entry.getValue();

            String reason = treasuryExemptionType.getName()
                    .getContent(TreasuryPlataformDependentServicesFactory.implementation().defaultLocale());
            TreasuryExemption.create(treasuryExemptionType, academicTreasuryEvent, reason, amountToExempt, installmentDebitEntry);
        }

        return true;
    }

    private Boolean createDebitEntryForRegistrationAndExempt(LocalDate debtDate, DebtAccount debtAccount,
            AcademicTreasuryEvent academicTreasuryEvent, TuitionInstallmentTariff tariff,
            Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap) {
        DebitEntry installmentDebitEntry = tariff.createDebitEntryForRegistration(this);

        for (Entry<TreasuryExemptionType, BigDecimal> entry : exemptionsToApplyMap.entrySet()) {
            TreasuryExemptionType treasuryExemptionType = entry.getKey();
            BigDecimal amountToExempt = entry.getValue();

            String reason = treasuryExemptionType.getName()
                    .getContent(TreasuryPlataformDependentServicesFactory.implementation().defaultLocale());
            TreasuryExemption.create(treasuryExemptionType, academicTreasuryEvent, reason, amountToExempt, installmentDebitEntry);
        }

        return true;
    }

    public List<TuitionDebitEntryBean> executeInstallmentDebitEntryBeansCalculation() {
        initializeCreditsAndEnrolledCoursesCount();
        initializeTuitionPaymentPlan();
        initializeCustomCalculators();
        initializeOriginalAmountsCalculator();

        initializeDiscountExemptionsMapForAllInstallments();

        TuitionPaymentPlan tuitionPaymentPlan = this.tuitionOptions.tuitionPaymentPlan;

        if (tuitionPaymentPlan == null) {
            return Collections.emptyList();
        }

        if (!tuitionPaymentPlan.getTuitionPaymentPlanGroup().isForRegistration()) {
            throw new RuntimeException("wrong call");
        }

        Predicate<TuitionInstallmentTariff> isToCalculateInstallmentProduct = t -> (this.installmentOptions.installments == null
                || this.installmentOptions.installments.contains(t.getProduct()))
                || (this.installmentRecalculationOptions.recalculateInstallments != null
                        && this.installmentRecalculationOptions.recalculateInstallments.containsKey(t.getProduct()));

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

        var serviceBuilder = startServiceInvocation(registration, executionYear, debtDate);

        serviceBuilder.applyEnrolledEctsUnits(this.registrationOptions.enrolledEctsUnits);
        serviceBuilder.applyEnrolledCoursesCount(this.registrationOptions.enrolledCoursesCount);
        serviceBuilder.applyDefaultEnrolmentCredits(this.registrationOptions.applyDefaultEnrolmentCredits);

        var tuitionPaymentPlanBuilder = serviceBuilder.withTuitionPaymentPlan(tuitionPaymentPlan);

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

    Comparator<TreasuryExemptionType> TREASURY_EVENT_COMPARATOR = (o1, o2) -> o1.getExternalId().compareTo(o2.getExternalId());

    private void initializeDiscountExemptionsMapForAllInstallments() {

        Registration registration = this.registrationOptions.registration;
        ExecutionYear executionYear = this.registrationOptions.executionYear;
        Person person = registration.getPerson();

        StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);
        DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();

        TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> result = new TreeMap<>(TREASURY_EVENT_COMPARATOR);
        getOtherEventsToDiscountInTuitionFee(person, executionYear, degreeCurricularPlan).stream()
                .collect(Collectors.toMap(TreasuryEvent::getTreasuryExemptionToApplyInEventDiscountInTuitionFee,
                        ev -> new TreasuryExemptionMoneyBox(ev.getNetAmountToPay(), ev.getNetAmountToPay()),
                        TreasuryExemptionMoneyBox::mergeBySumming, () -> result));

        if (!this.isForCalculationsOfOriginalAmounts) {
            removeAlreadyCreatedExemptedAmountsFromTheDiscountMapForAllInstallments(result);
        }

        this._discountExemptionsMapForAllInstallments = result;
    }

    private TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> removeAlreadyCreatedExemptedAmountsFromTheDiscountMapForAllInstallments(
            TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> discountMapForAllInstallments) {
        Registration registration = this.registrationOptions.registration;
        ExecutionYear executionYear = this.registrationOptions.executionYear;

        AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).ifPresent(tuitionEvent -> {

            // We will need to calculate the correct exempted amount to remove from the 
            // discount map for all installments (given by other treasury events), so
            // we have to iterate by installmentTariff
            this.tuitionOptions.tuitionPaymentPlan.getOrderedTuitionInstallmentTariffs().forEach(tariff -> {
                Product product = tariff.getProduct();

                Map<TreasuryExemptionType, BigDecimal> exemptedAmountsByTypeMap =
                        DebitEntry.findActive(tuitionEvent, product).flatMap(d -> d.getTreasuryExemptionsSet().stream())
                                .collect(Collectors.toMap(TreasuryExemption::getTreasuryExemptionType,
                                        TreasuryExemption::getNetAmountToExempt, BigDecimal::add));

                for (Entry<TreasuryExemptionType, BigDecimal> entry : exemptedAmountsByTypeMap.entrySet()) {
                    TreasuryExemptionType treasuryExemptionType = entry.getKey();

                    if (!discountMapForAllInstallments.containsKey(treasuryExemptionType)) {
                        // Not in the discountMap, nothing to decrement, continue
                        continue;
                    }

                    BigDecimal netExemptedAmount = entry.getValue();

                    BigDecimal totalDebitCreatedIncludingExemptions = DebitEntry.findActive(tuitionEvent, product)
                            .map(d -> d.getNetAmount().add(d.getNetExemptedAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);

                    // First, the already exempted amount must fill the map for the exemptions map
                    // for this installment until it's maximum amount
                    BigDecimal maximumAmountToExemptFromDiscountExemptionsMapForOnlyThisInstallment =
                            buildDiscountExemptionsMapForOnlyThisInstallmentByAmount(totalDebitCreatedIncludingExemptions)
                                    .computeIfAbsent(treasuryExemptionType,
                                            t -> TreasuryExemptionMoneyBox.zero()).maximumNetAmountForExemption
                                                    .min(netExemptedAmount);

                    BigDecimal remainingAmountToRemoveFromDiscountMapForAllInstallments =
                            netExemptedAmount.subtract(maximumAmountToExemptFromDiscountExemptionsMapForOnlyThisInstallment);

                    discountMapForAllInstallments.get(treasuryExemptionType)
                            .subtractFromCurrentNetAmount(remainingAmountToRemoveFromDiscountMapForAllInstallments);
                }
            });

        });

        return discountMapForAllInstallments;
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

        TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> _discountExemptionsMapForOnlyThisInstallment =
                buildDiscountExemptionsMapForOnlyThisInstallment(tariff);

        boolean isToRecalculateInstallment = this.installmentRecalculationOptions.recalculateInstallments != null
                && this.installmentRecalculationOptions.recalculateInstallments.containsKey(product);

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

            if (TreasuryConstants.isZero(differenceNetAmount) && TreasuryConstants.isZero(differenceInNetExemptedAmount)
                    && isExemptionsMapAreEqual(tariff, academicTreasuryEvent, originalBean)) {
                // Before and after is equal, nothing to do
                return Collections.emptyList();
            }

            if (TreasuryConstants.isNegative(differenceNetAmount) && TreasuryConstants.isZero(differenceInNetExemptedAmount)
                    && isExemptionsMapAreEqual(tariff, academicTreasuryEvent, originalBean)) {

                LocalDate recalculationDueDate = this.installmentRecalculationOptions.recalculateInstallments.get(product);

                LocalizedString recalculationInstallmentName = AcademicTreasuryConstants
                        .academicTreasuryBundleI18N("label.RegistrationTuitionService.recalculation.installmentName.prefix")
                        .append(installmentName);

                return Collections.singletonList(
                        new TuitionDebitEntryBean(installmentOrder, tariff, recalculationInstallmentName, recalculationDueDate,
                                vat.getTaxRate(), differenceNetAmount.negate(), BigDecimal.ZERO, new HashMap<>(), currency));
            }

            if (!TreasuryConstants.isNegative(differenceNetAmount) && !TreasuryConstants.isNegative(differenceInNetExemptedAmount)
                    && isThereAreOnlyNewEntriesOrIncrementsInExemptions(tariff, academicTreasuryEvent, originalBean)) {

                // Create debit entry with the positive difference
                LocalDate recalculationDueDate = this.installmentRecalculationOptions.recalculateInstallments.get(product);
                LocalizedString recalculationInstallmentName = AcademicTreasuryConstants
                        .academicTreasuryBundleI18N("label.RegistrationTuitionService.recalculation.installmentName.prefix")
                        .append(installmentName);

                // We need how to take the difference exemption. We take from the
                // mapForOnlyThisInstallment or the mapForAllInstallments exemption map? And how much to take
                // from one and the other?

                // 1. Know how much it will take from the mapForOnlyThisInstallment, we need to make a simulation
                BigDecimal simulationOfTotalExemptedAmountForOnlyThisInstallment =
                        getAndDecrementFromDiscountMap(buildDiscountExemptionsMapForOnlyThisInstallment(tariff),
                                originalBean.getExemptedAmount(), new HashMap<>());

                if (TreasuryConstants.isLessOrEqualThan(simulationOfTotalExemptedAmountForOnlyThisInstallment,
                        getNetAmountAlreadyDebited(product))) {
                    // 2. The exemption amount taken from the mapForOnlyThisInstallment is below the already exempted amount,
                    // it is safe to discount with the differenceInNetExemptedAmount from the mapForAllInstallments exemption map

                    getAndDecrementFromDiscountMap(this._discountExemptionsMapForAllInstallments, differenceInNetExemptedAmount,
                            new HashMap<>());
                } else {
                    // 3. It is greater, then we have to empty the recurring map to throw away the exemption amount already
                    // exempted in debit entries
                    getAndDecrementFromDiscountMap(_discountExemptionsMapForOnlyThisInstallment,
                            getNetAmountAlreadyDebited(product), new HashMap<>());

                    // 4. With the difference exempted amount, get the exemptions to apply for the debit entry
                    BigDecimal totalExemptedAmountFromExemptionsMapForOnlyThisInstallment = getAndDecrementFromDiscountMap(
                            _discountExemptionsMapForOnlyThisInstallment, differenceInNetExemptedAmount, new HashMap<>());

                    // 5. Now discount from the acrossAllInstallments exemption map
                    getAndDecrementFromDiscountMap(this._discountExemptionsMapForAllInstallments,
                            differenceInNetExemptedAmount.subtract(totalExemptedAmountFromExemptionsMapForOnlyThisInstallment),
                            new HashMap<>());
                }

                return Collections.singletonList(
                        new TuitionDebitEntryBean(installmentOrder, tariff, recalculationInstallmentName, recalculationDueDate,
                                vat.getTaxRate(), differenceNetAmount, differenceInNetExemptedAmount, new HashMap<>(), currency));
            }

            if (TreasuryConstants.isNegative(differenceInNetExemptedAmount)
                    || (TreasuryConstants.isNegative(differenceNetAmount)
                            && !TreasuryConstants.isZero(differenceInNetExemptedAmount))
                    || isThereAreRemovalOrDecrementsInExemptions(tariff, academicTreasuryEvent, originalBean)) {

                // It is easier and safe to anull the created debit entries
                // and create a new one

                // Annullment bean
                LocalizedString annullmentInstallmentName = AcademicTreasuryConstants
                        .academicTreasuryBundleI18N("label.RegistrationTuitionService.annulment.installmentName.prefix")
                        .append(installmentName);
                LocalDate recalculationDueDate = this.installmentRecalculationOptions.recalculateInstallments.get(product);

                TuitionDebitEntryBean annulmentBean =
                        new TuitionDebitEntryBean(installmentOrder, tariff, annullmentInstallmentName, recalculationDueDate,
                                vat.getTaxRate(), getNetAmountAlreadyDebited(product).negate(),
                                getNetAmountAlreadyExempted(product).negate(), new HashMap<>(), currency);

                LocalizedString recalculationInstallmentName = AcademicTreasuryConstants
                        .academicTreasuryBundleI18N("label.RegistrationTuitionService.recalculation.installmentName.prefix")
                        .append(installmentName);

                TuitionDebitEntryBean recalculationBean = new TuitionDebitEntryBean(installmentOrder, tariff,
                        recalculationInstallmentName, recalculationDueDate, vat.getTaxRate(), originalBean.getAmount(),
                        originalBean.getExemptedAmount(), new HashMap<>(), currency);

                BigDecimal recurringAmountToExempt = getAndDecrementFromDiscountMap(_discountExemptionsMapForOnlyThisInstallment,
                        originalBean.getExemptedAmount(), new HashMap<>());

                // We have to remove from the map, in order to not be used in the subsequent installments
                getAndDecrementFromDiscountMap(this._discountExemptionsMapForAllInstallments,
                        originalBean.getExemptedAmount().subtract(recurringAmountToExempt), new HashMap<>());

                return List.of(annulmentBean, recalculationBean);
            }

            throw new IllegalStateException("reculation: do not know how to handle this case???");
        } else {
            BigDecimal tuitionInstallmentAmountToPay = tariff.amountToPay(this);

            if (!TreasuryConstants.isPositive(tuitionInstallmentAmountToPay)) {
                return null;
            }

            Map<TreasuryExemptionType, BigDecimal> exemptionsMapToApply = new HashMap<>();

            BigDecimal netAmountToExemptFromMapForOnlyThisInstallment = getAndDecrementFromDiscountMap(
                    _discountExemptionsMapForOnlyThisInstallment, tuitionInstallmentAmountToPay, exemptionsMapToApply);

            BigDecimal totalAmountToExempt = getAndDecrementFromDiscountMap(this._discountExemptionsMapForAllInstallments,
                    tuitionInstallmentAmountToPay.subtract(netAmountToExemptFromMapForOnlyThisInstallment), exemptionsMapToApply);

            totalAmountToExempt = totalAmountToExempt.add(netAmountToExemptFromMapForOnlyThisInstallment);

            BigDecimal finalAmountToPay = tuitionInstallmentAmountToPay.subtract(totalAmountToExempt);

            return Collections.singletonList(new TuitionDebitEntryBean(installmentOrder, tariff, installmentName, dueDate,
                    vat.getTaxRate(), finalAmountToPay, totalAmountToExempt, exemptionsMapToApply, currency));
        }
    }

    private TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> buildDiscountExemptionsMapForOnlyThisInstallment(
            TuitionInstallmentTariff tuitionInstallmentTariff) {
        BigDecimal tuitionInstallmentAmountToPay = tuitionInstallmentTariff.amountToPay(this);

        return buildDiscountExemptionsMapForOnlyThisInstallmentByAmount(tuitionInstallmentAmountToPay);
    }

    private TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> buildDiscountExemptionsMapForOnlyThisInstallmentByAmount(
            BigDecimal tuitionInstallmentAmountToPay) {
        var registration = this.registrationOptions.registration;
        var executionYear = this.registrationOptions.executionYear;
        var finantialEntity = AcademicTreasuryPlataformDependentServicesFactory.implementation()
                .finantialEntityOfDegree(registration.getDegree(), executionYear.getBeginLocalDate());

        Set<StatuteType> statutesOfStudent = AcademicTreasuryPlataformDependentServicesFactory.implementation()
                .statutesTypesValidOnAnyExecutionSemesterFor(registration, executionYear);

        return StatuteExemptionByIntervalMapEntry.find(finantialEntity, executionYear)
                .filter(s -> statutesOfStudent.contains(s.getStatuteType()))
                .collect(Collectors.toMap(s -> s.getTreasuryExemptionType(), s -> {
                    BigDecimal val =
                            s.getTreasuryExemptionType().calculateDefaultNetAmountToExempt(tuitionInstallmentAmountToPay);
                    return new TreasuryExemptionMoneyBox(val, val);
                }, TreasuryExemptionMoneyBox::mergeByChoosingTheGreaterMaximumAmount,
                        () -> new TreeMap<>(TREASURY_EVENT_COMPARATOR)));
    }

    private BigDecimal getAndDecrementFromDiscountMap(
            TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> _discountExemptionsMap, BigDecimal maximumAmountToDiscount,
            Map<TreasuryExemptionType, BigDecimal> treasuryExemptionsToApplyMap) {
        BigDecimal result = BigDecimal.ZERO;

        for (TreasuryExemptionType treasuryExemptionType : _discountExemptionsMap.navigableKeySet()) {
            if (!_discountExemptionsMap.get(treasuryExemptionType).isAvailableNetAmountForExemptionPositive()) {
                continue;
            }

            BigDecimal availableAmountToDiscount =
                    _discountExemptionsMap.get(treasuryExemptionType).availableNetAmountForExemption;

            BigDecimal amountToDiscount = availableAmountToDiscount.min(maximumAmountToDiscount);
            result = result.add(amountToDiscount);

            maximumAmountToDiscount = maximumAmountToDiscount.subtract(amountToDiscount);

            _discountExemptionsMap.get(treasuryExemptionType).subtractFromCurrentNetAmount(amountToDiscount);

            treasuryExemptionsToApplyMap.merge(treasuryExemptionType, amountToDiscount, BigDecimal::add);

            if (!TreasuryConstants.isPositive(maximumAmountToDiscount)) {
                break;
            }
        }

        return result;
    }

    private BigDecimal getNetAmountAlreadyExempted(Product product) {
        return AcademicTreasuryEvent
                .findUniqueForRegistrationTuition(this.registrationOptions.registration, this.registrationOptions.executionYear)
                .map(e -> e.getNetExemptedAmount(product)).orElse(BigDecimal.ZERO);
    }

    private BigDecimal getNetAmountAlreadyDebited(Product product) {
        return AcademicTreasuryEvent
                .findUniqueForRegistrationTuition(this.registrationOptions.registration, this.registrationOptions.executionYear)
                .map(e -> e.getNetAmountToPay(product)).orElse(BigDecimal.ZERO);
    }

    private boolean isTuitionInstallmentCharged(Product product) {
        return AcademicTreasuryEvent
                .findUniqueForRegistrationTuition(this.registrationOptions.registration, this.registrationOptions.executionYear)
                .map(e -> e.isChargedWithDebitEntry(product)).orElse(false);
    }

    private void initializeCustomCalculators() {
        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        TuitionPaymentPlan tuitionPaymentPlan = this.tuitionOptions.tuitionPaymentPlan;
        Registration registration = this.registrationOptions.registration;
        ExecutionYear executionYear = this.registrationOptions.executionYear;

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
                    .map(tariff -> tariff.getTuitionTariffCustomCalculator()).collect(Collectors.toSet()).forEach(clazz -> {
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
            this.tuitionOptions.tuitionPaymentPlan = TuitionPaymentPlan.inferTuitionPaymentPlanForRegistration(
                    this.registrationOptions.registration, this.registrationOptions.executionYear);
        }
    }

    // SERVICE METHODS

    public static RegistrationOptions startServiceInvocation(Registration registration, ExecutionYear executionYear,
            LocalDate debtDate) {
        RegistrationTuitionService service = new RegistrationTuitionService();

        return service.new RegistrationOptions(registration, executionYear, debtDate);
    }

    // HELPER CLASSES

    public class RegistrationOptions {
        Registration registration;
        ExecutionYear executionYear;
        LocalDate debtDate;
        boolean useDefaultEnrolledEctsCredits = false;

        BigDecimal enrolledEctsUnits;
        BigDecimal enrolledCoursesCount;

        boolean applyDefaultEnrolmentCredits = false;

        RegistrationOptions(Registration registration, ExecutionYear executionYear, LocalDate debtDate) {
            this.registration = registration;
            this.executionYear = executionYear;
            this.debtDate = debtDate;

            RegistrationTuitionService.this.registrationOptions = this;
        }

        public RegistrationOptions useDefaultEnrolledEctsCredits(boolean value) {
            this.useDefaultEnrolledEctsCredits = value;
            return this;
        }

        public RegistrationOptions applyEnrolledEctsUnits(BigDecimal enrolledEctsUnits) {
            this.enrolledEctsUnits = enrolledEctsUnits;
            return this;
        }

        public RegistrationOptions applyEnrolledCoursesCount(BigDecimal enrolledCoursesCount) {
            this.enrolledCoursesCount = enrolledCoursesCount;
            return this;
        }

        public RegistrationOptions applyDefaultEnrolmentCredits(boolean value) {
            this.applyDefaultEnrolmentCredits = value;
            return this;
        }

        public TuitionOptions withTuitionPaymentPlan(TuitionPaymentPlan tuitionPaymentPlan) {
            return new TuitionOptions(tuitionPaymentPlan);
        }

        public TuitionOptions withInferedTuitionPaymentPlan() {
            return new TuitionOptions();
        }

    }

    public class TuitionOptions {
        TuitionPaymentPlan tuitionPaymentPlan = null;
        boolean forceCreationIfNotEnrolled = false;
        boolean applyTuitionServiceExtensions = true;

        TuitionOptions() {
            RegistrationTuitionService.this.tuitionOptions = this;
        }

        TuitionOptions(TuitionPaymentPlan tuitionPaymentPlan) {
            this.tuitionPaymentPlan = tuitionPaymentPlan;

            RegistrationTuitionService.this.tuitionOptions = this;
        }

        public TuitionOptions discardTuitionServiceExtensions(boolean value) {
            this.applyTuitionServiceExtensions = value;
            return this;
        }

        public TuitionOptions forceCreationIfNotEnrolled(boolean value) {
            this.forceCreationIfNotEnrolled = value;
            return this;
        }

        public InstallmentOptions withAllInstallments() {
            return RegistrationTuitionService.this.installmentOptions = new InstallmentOptions();
        }

        public InstallmentOptions restrictForInstallmentProducts(Set<Product> installmentProducts) {
            return RegistrationTuitionService.this.installmentOptions = new InstallmentOptions(installmentProducts);
        }

    }

    public class InstallmentOptions {
        Set<Product> installments = null;
        boolean forceInstallmentsEvenTreasuryEventIsCharged = false;

        InstallmentOptions() {
        }

        InstallmentOptions(Set<Product> installments) {
            this.installments = installments;
        }

        public InstallmentOptions forceInstallmentsEvenTreasuryEventIsCharged(boolean value) {
            this.forceInstallmentsEvenTreasuryEventIsCharged = value;
            return this;
        }

        public RegistrationTuitionService withoutInstallmentsRecalculation() {
            RegistrationTuitionService.this.installmentRecalculationOptions = new InstallmentRecalculationOptions();

            return RegistrationTuitionService.this;
        }

        public RegistrationTuitionService recalculateInstallments(Map<Product, LocalDate> recalculateInstallments) {
            RegistrationTuitionService.this.installmentRecalculationOptions =
                    new InstallmentRecalculationOptions(recalculateInstallments);

            return RegistrationTuitionService.this;
        }
    }

    class InstallmentRecalculationOptions {

        Map<Product, LocalDate> recalculateInstallments;

        InstallmentRecalculationOptions() {
            this.recalculateInstallments = null;
        }

        InstallmentRecalculationOptions(Map<Product, LocalDate> recalculateInstallments) {
            this.recalculateInstallments = recalculateInstallments;
        }
    }

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

        if (!PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).isPresent()) {
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

    // Helper classes

    private static class TreasuryExemptionMoneyBox {
        private BigDecimal maximumNetAmountForExemption;
        private BigDecimal availableNetAmountForExemption;

        private TreasuryExemptionMoneyBox(BigDecimal maximumNetAmountForExemption, BigDecimal availableNetAmountForExemption) {
            this.maximumNetAmountForExemption = maximumNetAmountForExemption;
            this.availableNetAmountForExemption = availableNetAmountForExemption;
        }

        private void addToAvailableNetAmountForExemption(BigDecimal netAmount) {
            this.availableNetAmountForExemption = this.availableNetAmountForExemption.add(netAmount);

            // Ensure this will not overflow the maximum amount that can be exempted
            this.availableNetAmountForExemption = this.availableNetAmountForExemption.min(this.maximumNetAmountForExemption);
        }

        private void subtractFromCurrentNetAmount(BigDecimal netAmount) {
            this.availableNetAmountForExemption = this.availableNetAmountForExemption.subtract(netAmount);

            // Ensure it will not go below zero
            this.availableNetAmountForExemption = this.availableNetAmountForExemption.max(BigDecimal.ZERO);
        }

        private TreasuryExemptionMoneyBox mergeBySumming(TreasuryExemptionMoneyBox o) {
            return new TreasuryExemptionMoneyBox(this.maximumNetAmountForExemption.add(o.maximumNetAmountForExemption),
                    this.availableNetAmountForExemption.add(o.availableNetAmountForExemption));
        }

        private TreasuryExemptionMoneyBox mergeByChoosingTheGreaterMaximumAmount(TreasuryExemptionMoneyBox o) {
            if (TreasuryConstants.isGreaterOrEqualThan(o.maximumNetAmountForExemption, this.maximumNetAmountForExemption)) {
                return o;
            } else {
                return this;
            }
        }

        private boolean isAvailableNetAmountForExemptionPositive() {
            return TreasuryConstants.isPositive(this.availableNetAmountForExemption);
        }

        private static TreasuryExemptionMoneyBox zero() {
            return new TreasuryExemptionMoneyBox(BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }
}
