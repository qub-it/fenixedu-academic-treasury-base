package org.fenixedu.academictreasury.services.tuition;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.tuition.TuitionInstallmentTariff;
import org.fenixedu.academictreasury.domain.tuition.exemptions.StatuteExemptionByIntervalMapEntry;
import org.fenixedu.academictreasury.dto.tuition.TuitionDebitEntryBean;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.util.TreasuryConstants;
import pt.ist.fenixframework.core.AbstractDomainObject;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Helper class for RegistrationTuitionService
class TreasuryExemptionsTeller {
    static Comparator<TreasuryExemptionType> TREASURY_EVENT_COMPARATOR =
            Comparator.comparing(AbstractDomainObject::getExternalId);

    RegistrationTuitionService _registrationTuitionService;

    // This map will hold the exemption amounts to be applied in all installment debit entries
    // if the exemptions for a particular installment is not enough.
    // The exemption amounts come from reservation taxes
    TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> _discountExemptionsMapForAllInstallments;

    // This map will hold the exemption amounts for each installment
    // The amount that is left for an installment should not be used in other
    // installment.
    // The exemption amount come from exemptions given by tuition allocation or
    // student statutes exemption map

    Map<TuitionInstallmentTariff, TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox>>
            _discountExemptionsMapForOnlyThisInstallment = new HashMap<>();

    Map<TuitionInstallmentTariff, TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox>>
            _alreadyCreatedExemptionsMapForOnlyThisInstallment = new HashMap<>();

    TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> _alreadyCreatedExemptionsMapForAllInstallments =
            new TreeMap<>(TREASURY_EVENT_COMPARATOR);

    TreasuryExemptionsTeller(RegistrationTuitionService registrationTuitionService) {
        this._registrationTuitionService = registrationTuitionService;

        initializeDiscountExemptionsMapForAllInstallments();
    }

    private void initializeDiscountExemptionsMapForAllInstallments() {
        Registration registration = this._registrationTuitionService.registrationOptions.registration;
        ExecutionYear executionYear = this._registrationTuitionService.registrationOptions.executionYear;
        Person person = registration.getPerson();

        StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);
        DegreeCurricularPlan degreeCurricularPlan = studentCurricularPlan.getDegreeCurricularPlan();

        Collector<TreasuryEvent, ?, TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox>> collector =
                Collectors.toMap(TreasuryEvent::getTreasuryExemptionToApplyInEventDiscountInTuitionFee,
                        ev -> new TreasuryExemptionMoneyBox(ev.getNetAmountToPay(), ev.getNetAmountToPay()),
                        TreasuryExemptionMoneyBox::mergeBySumming, () -> new TreeMap<>(TREASURY_EVENT_COMPARATOR));

        TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> result =
                getOtherEventsToDiscountInTuitionFee(person, executionYear, degreeCurricularPlan).stream().collect(collector);

        if (!this._registrationTuitionService.isForCalculationsOfOriginalAmounts) {
            removeAlreadyCreatedExemptedAmountsFromTheDiscountMapForAllInstallments(result);
        }

        this._discountExemptionsMapForAllInstallments = result;
    }

    private TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> removeAlreadyCreatedExemptedAmountsFromTheDiscountMapForAllInstallments(
            TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> discountMapForAllInstallments) {
        Registration registration = this._registrationTuitionService.registrationOptions.registration;
        ExecutionYear executionYear = this._registrationTuitionService.registrationOptions.executionYear;

        AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).ifPresent(tuitionEvent -> {

            // We will need to calculate the correct exempted amount to remove from the
            // discount map for all installments (given by other treasury events), so
            // we have to iterate by installmentTariff
            this._registrationTuitionService.tuitionOptions.tuitionPaymentPlan.getOrderedTuitionInstallmentTariffs()
                    .forEach(tariff -> {
                        Product product = tariff.getProduct();

                        Map<TreasuryExemptionType, BigDecimal> exemptedAmountsByTypeMap =
                                tuitionEvent.getNetExemptedAmountsMap(product);

                        for (Map.Entry<TreasuryExemptionType, BigDecimal> entry : exemptedAmountsByTypeMap.entrySet()) {
                            TreasuryExemptionType treasuryExemptionType = entry.getKey();

                            if (!discountMapForAllInstallments.containsKey(treasuryExemptionType)) {
                                // Not in the discountMap, nothing to decrement, continue
                                continue;
                            }

                            BigDecimal netExemptedAmount = entry.getValue();

                            BigDecimal totalDebitCreatedIncludingExemptions = DebitEntry.findActive(tuitionEvent, product)
                                    .map(d -> d.getNetAmount().add(d.getNetExemptedAmount()))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                            // First, the already exempted amount must fill the map for the exemptions map
                            // for this installment until it's maximum amount
                            BigDecimal maximumAmountToExemptFromDiscountExemptionsMapForOnlyThisInstallment =
                                    buildDiscountExemptionsMapForOnlyThisInstallmentByAmount(
                                            totalDebitCreatedIncludingExemptions).computeIfAbsent(treasuryExemptionType,
                                            t -> TreasuryExemptionMoneyBox.zero()).maximumNetAmountForExemption.min(
                                            netExemptedAmount);

                            this._alreadyCreatedExemptionsMapForOnlyThisInstallment.putIfAbsent(tariff,
                                    new TreeMap<>(TREASURY_EVENT_COMPARATOR));

                            this._alreadyCreatedExemptionsMapForOnlyThisInstallment.get(tariff).merge(treasuryExemptionType,
                                    new TreasuryExemptionMoneyBox(
                                            maximumAmountToExemptFromDiscountExemptionsMapForOnlyThisInstallment,
                                            maximumAmountToExemptFromDiscountExemptionsMapForOnlyThisInstallment),
                                    (currentVal, newVal) -> currentVal.mergeBySumming(newVal));

                            BigDecimal remainingAmountToRemoveFromDiscountMapForAllInstallments = netExemptedAmount.subtract(
                                    maximumAmountToExemptFromDiscountExemptionsMapForOnlyThisInstallment);

                            this._alreadyCreatedExemptionsMapForAllInstallments.merge(treasuryExemptionType,
                                    new TreasuryExemptionMoneyBox(remainingAmountToRemoveFromDiscountMapForAllInstallments,
                                            remainingAmountToRemoveFromDiscountMapForAllInstallments),
                                    (currentVal, newVal) -> currentVal.mergeBySumming(newVal));

                            discountMapForAllInstallments.get(treasuryExemptionType)
                                    .subtractFromCurrentNetAmount(remainingAmountToRemoveFromDiscountMapForAllInstallments);
                        }
                    });

        });

        return discountMapForAllInstallments;
    }

    private static List<TreasuryEvent> getOtherEventsToDiscountInTuitionFee(Person person, ExecutionYear executionYear,
            DegreeCurricularPlan degreeCurricularPlan) {
        return AcademicTreasuryEvent.getAllAcademicTreasuryEventsOfPerson(person) //
                .stream() //
                .map(TreasuryEvent.class::cast) //
                .filter(t -> t.isEventDiscountInTuitionFee()) //
                .filter(t -> executionYear.getQualifiedName().equals(t.getExecutionYearName())) //
                .filter(t -> degreeCurricularPlan.getDegree().getCode().equals(t.getDegreeCode())) //
                .collect(Collectors.toList());
    }

    void createDiscountExemptionsMapForOnlyThisInstallment(TuitionInstallmentTariff tuitionInstallmentTariff) {
        this._discountExemptionsMapForOnlyThisInstallment.put(tuitionInstallmentTariff,
                buildDiscountExemptionsMapForOnlyThisInstallment(tuitionInstallmentTariff));
    }

    private TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> buildDiscountExemptionsMapForOnlyThisInstallment(
            TuitionInstallmentTariff tuitionInstallmentTariff) {
        BigDecimal tuitionInstallmentAmountToPay = tuitionInstallmentTariff.amountToPay(this._registrationTuitionService);

        TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> result =
                buildDiscountExemptionsMapForOnlyThisInstallmentByAmount(tuitionInstallmentAmountToPay);

        return result;
    }

    private TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> buildDiscountExemptionsMapForOnlyThisInstallmentByAmount(
            BigDecimal tuitionInstallmentAmountToPay) {
        var registration = this._registrationTuitionService.getRegistration();
        var executionYear = this._registrationTuitionService.getExecutionYear();
        var finantialEntity =
                AcademicTreasuryConstants.getFinantialEntityOfDegree(registration.getDegree(), executionYear.getBeginLocalDate());

        // Construct a map based on statutes and exemptions mapping
        Set<StatuteType> statutesOfStudent =
                AcademicTreasuryConstants.statutesTypesValidOnAnyExecutionSemesterFor(registration, executionYear);

        TreeMap<TreasuryExemptionType, TreasuryExemptionMoneyBox> exemptionMapByStatutes =
                StatuteExemptionByIntervalMapEntry.find(finantialEntity, executionYear)
                        .filter(s -> statutesOfStudent.contains(s.getStatuteType()))
                        .collect(Collectors.toMap(s -> s.getTreasuryExemptionType(), s -> {
                                    BigDecimal val =
                                            s.getTreasuryExemptionType().calculateDefaultNetAmountToExempt(tuitionInstallmentAmountToPay);
                                    return new TreasuryExemptionMoneyBox(val, val);
                                }, TreasuryExemptionMoneyBox::mergeByChoosingTheGreaterMaximumAmount,
                                () -> new TreeMap<>(TREASURY_EVENT_COMPARATOR)));

        // Construct a map based in tuition allocation
        if (this._registrationTuitionService.tuitionOptions.tuitionAllocation != null) {
            this._registrationTuitionService.tuitionOptions.tuitionAllocation.getTreasuryExemptionTypesSet().stream()
                    .forEach(s -> {
                        BigDecimal val = s.calculateDefaultNetAmountToExempt(tuitionInstallmentAmountToPay);
                        var box = new TreasuryExemptionMoneyBox(val, val);

                        exemptionMapByStatutes.merge(s, box, TreasuryExemptionMoneyBox::mergeByChoosingTheGreaterMaximumAmount);
                    });
        }

        return exemptionMapByStatutes;
    }

    Map<TreasuryExemptionType, BigDecimal> retrieveUnchargedExemptionsToApplyMapForTariff(TuitionInstallmentTariff tariff,
            BigDecimal tuitionInstallmentAmountToPay) {

        Map<TreasuryExemptionType, BigDecimal> resultMap = new HashMap<>();

        BigDecimal netAmountToExemptForOnlyThisInstallment =
                getAndDecrementFromDiscountMap(this._discountExemptionsMapForOnlyThisInstallment.get(tariff),
                        tuitionInstallmentAmountToPay, resultMap);

        getAndDecrementFromDiscountMap(this._discountExemptionsMapForAllInstallments,
                tuitionInstallmentAmountToPay.subtract(netAmountToExemptForOnlyThisInstallment), resultMap);

        return resultMap;
    }

    public Map<TreasuryExemptionType, BigDecimal> retrieveAdditionalExemptionsToApplyMapForTariff(TuitionInstallmentTariff tariff,
            Product product, TuitionDebitEntryBean originalBean, BigDecimal differenceInNetExemptedAmount) {
        Map<TreasuryExemptionType, BigDecimal> resultMap = new HashMap<>();

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

            getAndDecrementFromDiscountMap(this._discountExemptionsMapForOnlyThisInstallment.get(tariff),
                    simulationOfAlreadyExemptedAmountForOnlyThisInstallment, new HashMap<>());

            BigDecimal totalExemptedAmountFromExemptionsMapForOnlyThisInstallment =
                    getAndDecrementFromDiscountMap(this._discountExemptionsMapForOnlyThisInstallment.get(tariff),
                            simulationOfTotalExemptedAmountForOnlyThisInstallment.subtract(
                                    simulationOfAlreadyExemptedAmountForOnlyThisInstallment), resultMap);

            netExemptedAmountToTakeFromExemptionsMapForAllInstallments =
                    netExemptedAmountToTakeFromExemptionsMapForAllInstallments.subtract(
                            totalExemptedAmountFromExemptionsMapForOnlyThisInstallment);
        } else {
            // Nothing to do, we only need to take the difference and decrement
            // with mapForAllInstallments
        }

        // 5. Now discount from the acrossAllInstallments exemption map
        getAndDecrementFromDiscountMap(this._discountExemptionsMapForAllInstallments,
                netExemptedAmountToTakeFromExemptionsMapForAllInstallments, resultMap);

        return resultMap;
    }

    private BigDecimal getNetAmountAlreadyExempted(Product product) {
        return AcademicTreasuryEvent.findUniqueForRegistrationTuition(
                        this._registrationTuitionService.registrationOptions.registration,
                        this._registrationTuitionService.registrationOptions.executionYear).map(e -> e.getNetExemptedAmount(product))
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getNetAmountAlreadyDebited(Product product) {
        return AcademicTreasuryEvent.findUniqueForRegistrationTuition(
                        this._registrationTuitionService.registrationOptions.registration,
                        this._registrationTuitionService.registrationOptions.executionYear).map(e -> e.getNetAmountToPay(product))
                .orElse(BigDecimal.ZERO);
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

    public Map<TreasuryExemptionType, BigDecimal> getTreasuryExemptionDecrementsByTypeMap(TuitionInstallmentTariff tariff,
            AcademicTreasuryEvent academicTreasuryEvent, TuitionDebitEntryBean originalBean) {
        final Map<TreasuryExemptionType, BigDecimal> alreadyCreatedMap =
                academicTreasuryEvent.getNetExemptedAmountsMap(tariff.getProduct());
        final Map<TreasuryExemptionType, BigDecimal> newExemptionsMap = originalBean.getExemptionsMap();

        Map<TreasuryExemptionType, BigDecimal> result = new HashMap<>();
        for (TreasuryExemptionType t : alreadyCreatedMap.keySet()) {
            if (newExemptionsMap.containsKey(t) && TreasuryConstants.isLessThan(newExemptionsMap.get(t),
                    alreadyCreatedMap.get(t))) {
                result.put(t, alreadyCreatedMap.get(t).subtract(newExemptionsMap.get(t)));
            } else if (!newExemptionsMap.containsKey(t)) {
                result.put(t, alreadyCreatedMap.get(t));
            }
        }

        return result;
    }

    void revertExemptionAmountsFromAcademicTreasuryToDiscountExemptionsMapForAllInstallments(TuitionInstallmentTariff tariff) {
        Registration registration = this._registrationTuitionService.registrationOptions.registration;
        ExecutionYear executionYear = this._registrationTuitionService.registrationOptions.executionYear;

        AcademicTreasuryEvent.findUniqueForRegistrationTuition(registration, executionYear).ifPresent(tuitionEvent -> {
            // We will need to calculate the correct exempted amount to remove from the
            // discount map for all installments (given by other treasury events), so
            // we have to iterate by installmentTariff

            Product product = tariff.getProduct();

            Map<TreasuryExemptionType, BigDecimal> exemptedAmountsByTypeMap =
                    DebitEntry.findActive(tuitionEvent, product).flatMap(d -> d.getTreasuryExemptionsSet().stream()).collect(
                            Collectors.toMap(TreasuryExemption::getTreasuryExemptionType, TreasuryExemption::getNetAmountToExempt,
                                    BigDecimal::add));

            for (Map.Entry<TreasuryExemptionType, BigDecimal> entry : exemptedAmountsByTypeMap.entrySet()) {
                TreasuryExemptionType treasuryExemptionType = entry.getKey();

                if (!this._discountExemptionsMapForAllInstallments.containsKey(treasuryExemptionType)) {
                    // Not in the discountMap, nothing to revert, continue
                    continue;
                }

                BigDecimal netExemptedAmount = entry.getValue();

                BigDecimal totalDebitCreatedIncludingExemptions =
                        DebitEntry.findActive(tuitionEvent, product).map(d -> d.getNetAmount().add(d.getNetExemptedAmount()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // First, the already exempted amount must fill the map for the exemptions map
                // for this installment until it's maximum amount
                BigDecimal maximumAmountToExemptFromDiscountExemptionsMapForOnlyThisInstallment =
                        buildDiscountExemptionsMapForOnlyThisInstallmentByAmount(
                                totalDebitCreatedIncludingExemptions).computeIfAbsent(treasuryExemptionType,
                                t -> TreasuryExemptionMoneyBox.zero()).maximumNetAmountForExemption.min(netExemptedAmount);

                BigDecimal remainingAmountToRemoveFromDiscountMapForAllInstallments =
                        netExemptedAmount.subtract(maximumAmountToExemptFromDiscountExemptionsMapForOnlyThisInstallment);

                this._discountExemptionsMapForAllInstallments.get(treasuryExemptionType)
                        .addToAvailableNetAmountForExemption(remainingAmountToRemoveFromDiscountMapForAllInstallments);
            }

        });
    }

    boolean isThereIsOnlyRemovalsOrDecrementsInExemptions(TuitionInstallmentTariff tariff,
            AcademicTreasuryEvent academicTreasuryEvent, TuitionDebitEntryBean originalBean) {
        final Map<TreasuryExemptionType, BigDecimal> alreadyCreatedMap =
                academicTreasuryEvent.getNetExemptedAmountsMap(tariff.getProduct());
        final Map<TreasuryExemptionType, BigDecimal> newExemptionsMap = originalBean.getExemptionsMap();

        Set<TreasuryExemptionType> allTypes =
                Stream.concat(alreadyCreatedMap.keySet().stream(), newExemptionsMap.keySet().stream())
                        .collect(Collectors.toSet());

        Predicate<TreasuryExemptionType> typeMatchInValue =
                t -> newExemptionsMap.containsKey(t) && alreadyCreatedMap.containsKey(t) && TreasuryConstants.isEqual(
                        newExemptionsMap.get(t), alreadyCreatedMap.get(t));

        Set<TreasuryExemptionType> differingTypesSet =
                allTypes.stream().filter(typeMatchInValue.negate()).collect(Collectors.toSet());

        return !differingTypesSet.isEmpty() && differingTypesSet.stream().allMatch(
                t -> alreadyCreatedMap.containsKey(t) && (!newExemptionsMap.containsKey(t) || TreasuryConstants.isLessThan(
                        newExemptionsMap.get(t), alreadyCreatedMap.get(t))));
    }

    boolean isThereAreRemovalOrDecrementsInExemptions(TuitionInstallmentTariff tariff,
            AcademicTreasuryEvent academicTreasuryEvent, TuitionDebitEntryBean newTuitionDebitEntryBean) {
        final Map<TreasuryExemptionType, BigDecimal> alreadyCreatedMap =
                academicTreasuryEvent.getNetExemptedAmountsMap(tariff.getProduct());
        final Map<TreasuryExemptionType, BigDecimal> newTuitionDebitEntryBeanExemptionsMap =
                newTuitionDebitEntryBean.getExemptionsMap();

        Predicate<Map.Entry<TreasuryExemptionType, BigDecimal>> predicate =
                alreadyExemptedEntry -> !newTuitionDebitEntryBeanExemptionsMap.containsKey(
                        alreadyExemptedEntry.getKey()) || TreasuryConstants.isLessThan(
                        newTuitionDebitEntryBeanExemptionsMap.get(alreadyExemptedEntry.getKey()),
                        alreadyExemptedEntry.getValue());

        if (alreadyCreatedMap.entrySet().stream().anyMatch(predicate)) {
            return true;
        }

        return false;
    }

    boolean isThereAreOnlyNewEntriesOrIncrementsInExemptions(TuitionInstallmentTariff tariff,
            AcademicTreasuryEvent academicTreasuryEvent, TuitionDebitEntryBean originalBean) {
        final Map<TreasuryExemptionType, BigDecimal> alreadyCreatedMap =
                academicTreasuryEvent.getNetExemptedAmountsMap(tariff.getProduct());
        final Map<TreasuryExemptionType, BigDecimal> newExemptionsMap = originalBean.getExemptionsMap();

        Set<TreasuryExemptionType> allTypes =
                Stream.concat(alreadyCreatedMap.keySet().stream(), newExemptionsMap.keySet().stream())
                        .collect(Collectors.toSet());

        Predicate<TreasuryExemptionType> typeMatchInValue =
                t -> newExemptionsMap.containsKey(t) && alreadyCreatedMap.containsKey(t) && TreasuryConstants.isEqual(
                        newExemptionsMap.get(t), alreadyCreatedMap.get(t));

        Set<TreasuryExemptionType> differingTypesSet =
                allTypes.stream().filter(typeMatchInValue.negate()).collect(Collectors.toSet());

        return !differingTypesSet.isEmpty() && differingTypesSet.stream().allMatch(
                t -> newExemptionsMap.containsKey(t) && (!alreadyCreatedMap.containsKey(t) || TreasuryConstants.isGreaterThan(
                        newExemptionsMap.get(t), alreadyCreatedMap.get(t))));
    }

    boolean isExemptionsMapAreEqual(TuitionInstallmentTariff tariff, AcademicTreasuryEvent academicTreasuryEvent,
            TuitionDebitEntryBean newTuitionDebitEntryBean) {
        Map<TreasuryExemptionType, BigDecimal> alreadyCreatedMap =
                academicTreasuryEvent.getNetExemptedAmountsMap(tariff.getProduct());
        Map<TreasuryExemptionType, BigDecimal> originalExemptionsMap = newTuitionDebitEntryBean.getExemptionsMap();

        if (!alreadyCreatedMap.keySet().equals(originalExemptionsMap.keySet())) {
            return false;
        }

        Predicate<Map.Entry<TreasuryExemptionType, BigDecimal>> predicate =
                e -> TreasuryConstants.isEqual(e.getValue(), originalExemptionsMap.get(e.getKey()));

        return alreadyCreatedMap.entrySet().stream().allMatch(predicate);
    }

    public void fillBackExemptionAmountForAllInstallments(TuitionInstallmentTariff tariff,
            TreasuryExemptionType treasuryExemptionType, BigDecimal exemptionAmount) {

        Supplier<BigDecimal> exemptionAmountMinusOnlyForInstallmentCalc = () -> {
            if (!this._alreadyCreatedExemptionsMapForOnlyThisInstallment.containsKey(tariff)) {
                return BigDecimal.ZERO;
            }

            if (!this._alreadyCreatedExemptionsMapForOnlyThisInstallment.get(tariff).containsKey(treasuryExemptionType)) {
                return BigDecimal.ZERO;
            }

            BigDecimal exemptionRatio = TreasuryConstants.divide(treasuryExemptionType.getDefaultExemptionPercentage(),
                    TreasuryConstants.HUNDRED_PERCENT);

            BigDecimal result = exemptionAmount.multiply(exemptionRatio);

            return Currency.getValueWithScale(this._alreadyCreatedExemptionsMapForOnlyThisInstallment.get(tariff)
                    .get(treasuryExemptionType).availableNetAmountForExemption.min(result));
        };

        BigDecimal exemptionAmountMinusOnlyForInstallment =
                exemptionAmount.subtract(exemptionAmountMinusOnlyForInstallmentCalc.get());

        if (!this._alreadyCreatedExemptionsMapForAllInstallments.containsKey(treasuryExemptionType)) {
            return;
        }

        BigDecimal amountToAdd =
                this._alreadyCreatedExemptionsMapForAllInstallments.get(treasuryExemptionType).availableNetAmountForExemption.min(
                        exemptionAmountMinusOnlyForInstallment);

        this._discountExemptionsMapForAllInstallments.get(treasuryExemptionType).addToAvailableNetAmountForExemption(amountToAdd);
        this._alreadyCreatedExemptionsMapForAllInstallments.get(treasuryExemptionType).subtractFromCurrentNetAmount(amountToAdd);
    }
}
