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
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tuition.ITuitionRegistrationServiceParameters;
import org.fenixedu.academictreasury.domain.tuition.TuitionInstallmentTariff;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.academictreasury.domain.tuition.TuitionTariffCustomCalculator;
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

    TreeMap<TreasuryEvent, BigDecimal> _discountMap;
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

        initializeDiscountMap();

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

            boolean allowToCreateTheInstallment = this.installmentOptions.installments == null
                    || this.installmentOptions.installments.contains(tariff.getProduct());

            if (allowToCreateTheInstallment && !academicTreasuryEvent.isChargedWithDebitEntry(tariff)) {
                BigDecimal tuitionInstallmentAmountToPay = tariff.amountToPay(this);

                Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap = new HashMap<>();
                getAndDecrementFromDiscountMap(tuitionInstallmentAmountToPay, exemptionsToApplyMap);

                if (!TreasuryConstants.isPositive(tuitionInstallmentAmountToPay)) {
                    return false;
                }

                return createDebitEntryForRegistrationAndExempt(debtDate, debtAccount, academicTreasuryEvent, tariff,
                        exemptionsToApplyMap);
            } else if (this.installmentRecalculationOptions.recalculateInstallments != null
                    && this.installmentRecalculationOptions.recalculateInstallments.containsKey(product)) {

                if (isTuitionInstallmentCharged(product)) {
                    // Recalculate

                    // Get the amounts that should be debited
                    TuitionDebitEntryBean originalBean = calculatedDebitEntryBeansMap.computeIfAbsent(tariff,
                            t -> new TuitionDebitEntryBean(t.getInstallmentOrder(), t, ls("Installment bean"), debtDate,
                                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, currency));

                    BigDecimal differenceAmount = originalBean.getAmount().subtract(getNetAmountAlreadyDebited(product));
                    BigDecimal differenceInExemptedAmount =
                            originalBean.getExemptedAmount().subtract(getNetAmountAlreadyExempted(product));

                    if (TreasuryConstants.isZero(differenceAmount) && TreasuryConstants.isZero(differenceInExemptedAmount)) {

                        // Nothing to do
                        return false;

                    } else if (!TreasuryConstants.isNegative(differenceAmount)
                            && !TreasuryConstants.isNegative(differenceInExemptedAmount)) {
                        // There are amounts in which we can create a new recalculation debit entry
                        LocalDate recalculationDueDate =
                                this.installmentRecalculationOptions.recalculateInstallments.get(product);

                        Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap = new HashMap<>();
                        getAndDecrementFromDiscountMap(differenceInExemptedAmount, exemptionsToApplyMap);

                        BigDecimal debitEntryNetAmount = differenceAmount.add(differenceInExemptedAmount);

                        return createRecalculationAdditionalDebitEntryForRegistrationAndExempt(debtDate, debtAccount,
                                academicTreasuryEvent, tariff, exemptionsToApplyMap, debitEntryNetAmount, recalculationDueDate);
                    } else if (TreasuryConstants.isNegative(differenceAmount)
                            && TreasuryConstants.isZero(differenceInExemptedAmount)) {
                        // Close the debit entries and credit with the differenceAmount
                        closeDebitEntriesInDebitNote(academicTreasuryEvent, tariff.getProduct());

                        BigDecimal remainingAmountToCredit = differenceAmount.negate();
                        List<DebitEntry> debitEntriesToCreditList = DebitEntry.findActive(academicTreasuryEvent, product)
                                .sorted(DebitEntry.COMPARE_BY_EXTERNAL_ID.reversed()).collect(Collectors.toList());
                        for (DebitEntry d : debitEntriesToCreditList) {
                            BigDecimal netAmountToCredit = remainingAmountToCredit.min(d.getAvailableNetAmountForCredit());
                            remainingAmountToCredit = netAmountToCredit.subtract(netAmountToCredit);

                            if (TreasuryConstants.isPositive(netAmountToCredit)) {
                                d.creditDebitEntry(netAmountToCredit, "Acerto de propinas", false);

                                if (!TreasuryConstants.isPositive(d.getAvailableNetAmountForCredit())
                                        && !TreasuryConstants.isPositive(d.getNetExemptedAmount())) {
                                    // The net amount of debit entry is zero, which means can be annuled in academic treasury event
                                    d.annulOnEvent();
                                }
                            }
                        }
                    } else {
                        // The exemption amount is negative. Then we have to annul the all debit entries
                        // of installment and create again

                        closeDebitEntriesInDebitNote(academicTreasuryEvent, tariff.getProduct());
                        DebitEntry.findActive(academicTreasuryEvent, product).forEach(d -> {
                            // Revert the exemption amounts to discount map

                            d.getTreasuryExemptionsSet().stream().forEach(this::revertExemptionAmountToDiscountMap);
                            d.annulOnlyThisDebitEntryAndInterestsInBusinessContext("Acerto de propinas");
                        });

                        // Create a new debit entry
                        Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap = new HashMap<>();
                        getAndDecrementFromDiscountMap(originalBean.getAmount().add(originalBean.getExemptedAmount()),
                                exemptionsToApplyMap);

                        createDebitEntryForRegistrationAndExempt(debtDate, debtAccount, academicTreasuryEvent, tariff,
                                exemptionsToApplyMap);
                    }
                } else {
                    BigDecimal tuitionInstallmentAmountToPay = tariff.amountToPay(this);

                    Map<TreasuryExemptionType, BigDecimal> exemptionsToApplyMap = new HashMap<>();
                    getAndDecrementFromDiscountMap(tuitionInstallmentAmountToPay, exemptionsToApplyMap);

                    if (!TreasuryConstants.isPositive(tuitionInstallmentAmountToPay)) {
                        return false;
                    }

                    return createDebitEntryForRegistrationAndExempt(debtDate, debtAccount, academicTreasuryEvent, tariff,
                            exemptionsToApplyMap);
                }
            }

            return false;
        };

        return installments.map(func).reduce(Boolean.FALSE, Boolean::logicalOr);

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

    private void revertExemptionAmountToDiscountMap(TreasuryExemption treasuryExemption) {
        Registration registration = this.registrationOptions.registration;
        ExecutionYear executionYear = this.registrationOptions.executionYear;
        Person person = registration.getPerson();

        StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);
        DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();

        BigDecimal remainingExemptedAmountToRevert = treasuryExemption.getNetExemptedAmount();

        List<TreasuryEvent> discountTreasuryEventsToRefillTheRevertedExemptionAmountList =
                getOtherEventsToDiscountInTuitionFee(person, executionYear, degreeCurricularPlan).stream()
                        .filter(te -> te.isEventDiscountInTuitionFeeWithTreasuryExemption())
                        .filter(te -> te.getTreasuryExemptionToApplyInEventDiscountInTuitionFee() == treasuryExemption
                                .getTreasuryExemptionType())
                        .collect(Collectors.toList());

        for (TreasuryEvent discountTreasuryEvent : discountTreasuryEventsToRefillTheRevertedExemptionAmountList) {
            BigDecimal maximumAmount = discountTreasuryEvent.getNetAmountToPay();

            BigDecimal currentAmountInDiscountMap = this._discountMap.get(discountTreasuryEvent);
            BigDecimal freeAmountToRefill = maximumAmount.subtract(currentAmountInDiscountMap);

            BigDecimal effectiveAmountToRefill = freeAmountToRefill.min(remainingExemptedAmountToRevert);
            remainingExemptedAmountToRevert = remainingExemptedAmountToRevert.subtract(effectiveAmountToRefill);
            this._discountMap.merge(discountTreasuryEvent, effectiveAmountToRefill, BigDecimal::add);
        }
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

        initializeDiscountMap();

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

    private void initializeDiscountMap() {
        Comparator<? super TreasuryEvent> TREASURY_EVENT_COMPARATOR = (o1, o2) -> {
            if (!o1.isEventDiscountInTuitionFeeWithTreasuryExemption() && o2.isEventDiscountInTuitionFeeWithTreasuryExemption()) {
                return -1;
            } else if (o1.isEventDiscountInTuitionFeeWithTreasuryExemption()
                    && !o2.isEventDiscountInTuitionFeeWithTreasuryExemption()) {
                return 1;
            }

            return o1.getExternalId().compareTo(o2.getExternalId());
        };

        Registration registration = this.registrationOptions.registration;
        ExecutionYear executionYear = this.registrationOptions.executionYear;
        Person person = registration.getPerson();

        StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);
        DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();

        TreeMap<TreasuryEvent, BigDecimal> result = new TreeMap<>(TREASURY_EVENT_COMPARATOR);
        getOtherEventsToDiscountInTuitionFee(person, executionYear, degreeCurricularPlan)
                .forEach(ev -> result.put(ev, ev.getNetAmountToPay()));

        if (!this.isForCalculationsOfOriginalAmounts) {
            removeExemptedAmountsAlreadyCreated(result);
        }

        this._discountMap = result;
    }

    private TreeMap<TreasuryEvent, BigDecimal> removeExemptedAmountsAlreadyCreated(
            TreeMap<TreasuryEvent, BigDecimal> discountMap) {
        Registration registration = this.registrationOptions.registration;
        ExecutionYear executionYear = this.registrationOptions.executionYear;

        AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).ifPresent(tuitionEvent -> {

            Map<TreasuryExemptionType, BigDecimal> exemptedAmountsByTypeMap = tuitionEvent.getActiveTreasuryExemptions().stream()
                    .collect(Collectors.toMap(TreasuryExemption::getTreasuryExemptionType,
                            TreasuryExemption::getNetExemptedAmount, BigDecimal::add));

            for (TreasuryEvent treasuryEvent : discountMap.navigableKeySet()) {
                if (!TreasuryConstants.isPositive(discountMap.get(treasuryEvent))) {
                    continue;
                }

                if (!treasuryEvent.isEventDiscountInTuitionFeeWithTreasuryExemption()) {
                    throw new RuntimeException("event discount without exemption not supported");
                }

                BigDecimal availableAmountToDiscount = discountMap.get(treasuryEvent);

                TreasuryExemptionType exemptionType = treasuryEvent.getTreasuryExemptionToApplyInEventDiscountInTuitionFee();
                BigDecimal amountToSubtract = availableAmountToDiscount
                        .min(exemptedAmountsByTypeMap.computeIfAbsent(exemptionType, t -> BigDecimal.ZERO));

                discountMap.put(treasuryEvent, availableAmountToDiscount.subtract(amountToSubtract));
                if (exemptedAmountsByTypeMap.containsKey(exemptionType)) {
                    exemptedAmountsByTypeMap.put(exemptionType,
                            exemptedAmountsByTypeMap.get(exemptionType).subtract(amountToSubtract));
                }
            }
        });

        return discountMap;
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
            Map<TuitionInstallmentTariff, TuitionDebitEntryBean> calculatedDebitEntryBeansMap,
            TuitionInstallmentTariff tuitionInstallmentTariff) {
        Product product = tuitionInstallmentTariff.getProduct();
        Currency currency = tuitionInstallmentTariff.getFinantialEntity().getFinantialInstitution().getCurrency();

        TuitionPaymentPlan tuitionPaymentPlan = this.tuitionOptions.tuitionPaymentPlan;
        Registration registration = this.registrationOptions.registration;
        LocalDate debtDate = this.registrationOptions.debtDate;
        BigDecimal enrolledEctsUnits = this.registrationOptions.enrolledEctsUnits;
        BigDecimal enrolledCoursesCount = this.registrationOptions.enrolledCoursesCount;

        final int installmentOrder = tuitionInstallmentTariff.getInstallmentOrder();
        final LocalizedString installmentName = tuitionPaymentPlan.installmentName(registration, tuitionInstallmentTariff);
        final LocalDate dueDate = tuitionInstallmentTariff.dueDate(debtDate);
        final Vat vat = tuitionInstallmentTariff.vat(debtDate);

        if (this.installmentRecalculationOptions.recalculateInstallments != null
                && this.installmentRecalculationOptions.recalculateInstallments.containsKey(product)
                && isTuitionInstallmentCharged(product)) {
            // Recalculate instead of new installment debt

            // Get the amounts that should be debited
            TuitionDebitEntryBean originalBean = calculatedDebitEntryBeansMap.computeIfAbsent(tuitionInstallmentTariff,
                    t -> new TuitionDebitEntryBean(installmentOrder, t, ls("Dummy installment"), debtDate, BigDecimal.ZERO,
                            BigDecimal.ZERO, BigDecimal.ZERO, currency));

            if (TreasuryConstants.isEqual(originalBean.getAmount(), getNetAmountAlreadyDebited(product))
                    && TreasuryConstants.isEqual(originalBean.getExemptedAmount(), getNetAmountAlreadyExempted(product))) {
                // Before and after is equal, nothing to do
                return Collections.emptyList();
            }

            boolean isOriginalAmountAndExemptionGreaterThanAlreadyDebited = TreasuryConstants
                    .isGreaterThan(originalBean.getAmount(), getNetAmountAlreadyDebited(product))
                    && TreasuryConstants.isGreaterThan(originalBean.getExemptedAmount(), getNetAmountAlreadyExempted(product));

            boolean isOriginalAmountGreaterThanAlreadyDebitedAndExemptionIsZero =
                    TreasuryConstants.isLessThan(originalBean.getAmount(), getNetAmountAlreadyDebited(product))
                            && TreasuryConstants.isZero(originalBean.getExemptedAmount())
                            && TreasuryConstants.isZero(getNetAmountAlreadyExempted(product));

            if (isOriginalAmountAndExemptionGreaterThanAlreadyDebited
                    || isOriginalAmountGreaterThanAlreadyDebitedAndExemptionIsZero) {

                // Create debit entry with the positive difference
                LocalDate recalculationDueDate = this.installmentRecalculationOptions.recalculateInstallments.get(product);
                LocalizedString recalculationInstallmentName = AcademicTreasuryConstants
                        .academicTreasuryBundleI18N("label.RegistrationTuitionService.recalculation.installmentName.prefix")
                        .append(installmentName);

                BigDecimal amount = originalBean.getAmount().subtract(getNetAmountAlreadyDebited(product));
                BigDecimal exemptedAmount = originalBean.getExemptedAmount().subtract(getNetAmountAlreadyExempted(product));

                // We have to remove from the map, in order to not be used in the subsequent installments
                getAndDecrementFromDiscountMap(exemptedAmount, new HashMap<>());

                return Collections.singletonList(new TuitionDebitEntryBean(installmentOrder, tuitionInstallmentTariff,
                        recalculationInstallmentName, recalculationDueDate, vat.getTaxRate(), amount, exemptedAmount, currency));
            } else {
                // It is easier and safe to anull the created debit entries
                // and create a new one

                // Annullment bean
                LocalizedString annullmentInstallmentName = AcademicTreasuryConstants
                        .academicTreasuryBundleI18N("label.RegistrationTuitionService.annulment.installmentName.prefix")
                        .append(installmentName);
                LocalDate recalculationDueDate = this.installmentRecalculationOptions.recalculateInstallments.get(product);

                TuitionDebitEntryBean annulmentBean = new TuitionDebitEntryBean(installmentOrder, tuitionInstallmentTariff,
                        annullmentInstallmentName, recalculationDueDate, vat.getTaxRate(),
                        getNetAmountAlreadyDebited(product).negate(), getNetAmountAlreadyExempted(product).negate(), currency);

                LocalizedString recalculationInstallmentName = AcademicTreasuryConstants
                        .academicTreasuryBundleI18N("label.RegistrationTuitionService.recalculation.installmentName.prefix")
                        .append(installmentName);

                TuitionDebitEntryBean recalculationBean = new TuitionDebitEntryBean(installmentOrder, tuitionInstallmentTariff,
                        recalculationInstallmentName, recalculationDueDate, vat.getTaxRate(), originalBean.getAmount(),
                        originalBean.getExemptedAmount(), currency);

                // We have to remove from the map, in order to not be used in the subsequent installments
                getAndDecrementFromDiscountMap(originalBean.getExemptedAmount(), new HashMap<>());

                return List.of(annulmentBean, recalculationBean);
            }
        } else {
            BigDecimal tuitionInstallmentAmountToPay = tuitionInstallmentTariff.amountToPay(this);

            if (!TreasuryConstants.isPositive(tuitionInstallmentAmountToPay)) {
                return null;
            }

            BigDecimal totalAmountToExempt = getAndDecrementFromDiscountMap(tuitionInstallmentAmountToPay, new HashMap<>());
            BigDecimal finalAmountToPay = tuitionInstallmentAmountToPay.subtract(totalAmountToExempt);

            return Collections.singletonList(new TuitionDebitEntryBean(installmentOrder, tuitionInstallmentTariff,
                    installmentName, dueDate, vat.getTaxRate(), finalAmountToPay, totalAmountToExempt, currency));
        }
    }

    private BigDecimal getAndDecrementFromDiscountMap(BigDecimal maximumAmountToDiscount,
            Map<TreasuryExemptionType, BigDecimal> treasuryExemptionsToApplyMap) {
        BigDecimal result = BigDecimal.ZERO;

        for (TreasuryEvent treasuryEvent : this._discountMap.navigableKeySet()) {
            if (!TreasuryConstants.isPositive(this._discountMap.get(treasuryEvent))) {
                continue;
            }

            BigDecimal availableAmountToDiscount = this._discountMap.get(treasuryEvent);

            BigDecimal amountToDiscount = availableAmountToDiscount.min(maximumAmountToDiscount);
            result = result.add(amountToDiscount);

            maximumAmountToDiscount = maximumAmountToDiscount.subtract(amountToDiscount);

            this._discountMap.put(treasuryEvent, availableAmountToDiscount.subtract(amountToDiscount));

            if (!treasuryEvent.isEventDiscountInTuitionFeeWithTreasuryExemption()) {
                throw new IllegalStateException("Event discount without treasury exemption not supported");
            }

            treasuryExemptionsToApplyMap.merge(treasuryEvent.getTreasuryExemptionToApplyInEventDiscountInTuitionFee(),
                    amountToDiscount, BigDecimal::add);

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

}
