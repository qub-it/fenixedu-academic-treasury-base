package org.fenixedu.academictreasury.domain.reservationtax;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryTarget;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.domain.academictreasurytarget.AcademicTreasuryTargetCreateDebtBuilder;
import org.fenixedu.academictreasury.domain.academictreasurytarget.AcademicTreasuryTargetCreateDebtBuilder.DebtBuilderWithAmountAndDueDate;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.paymentcodes.integration.ISibsPaymentCodePoolService;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public class ReservationTaxEventTarget extends ReservationTaxEventTarget_Base implements IAcademicTreasuryTarget {

    private static final Map<String, BiConsumer<ReservationTaxEventTarget, IAcademicTreasuryEvent>> HANDLE_TOTAL_PAYMENT =
            Collections.synchronizedMap(new LinkedHashMap<>());

    public static void registerHandleTotalPaymentHandler(String key,
            BiConsumer<ReservationTaxEventTarget, IAcademicTreasuryEvent> consumer) {
        HANDLE_TOTAL_PAYMENT.put(key, consumer);
    }

    public ReservationTaxEventTarget() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected ReservationTaxEventTarget(FinantialEntity finantialEntity, Product product, Person person,
            DegreeCurricularPlan degreeCurricularPlan, ExecutionInterval executionInterval, boolean discountInTuitionFee,
            LocalDate taxReservationDate, LocalizedString taxReservationDescription) {
        this();

        super.setFinantialEntity(finantialEntity);
        super.setProduct(product);
        super.setPerson(person);
        super.setDegreeCurricularPlan(degreeCurricularPlan);
        super.setExecutionInterval(executionInterval);
        super.setDiscountInTuitionFee(discountInTuitionFee);
        super.setTaxReservationDate(taxReservationDate);
        super.setTaxReservationDescription(taxReservationDescription);

        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTaxEventTarget.domainRoot.required");
        }

        if (getProduct() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTaxEventTarget.product.required");
        }

        if (getPerson() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTaxEventTarget.person.required");
        }

        if (getDegreeCurricularPlan() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTaxEventTarget.degreeCurricularPlan.required");
        }

        if (getExecutionInterval() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTaxEventTarget.executionInterval.required");
        }

        if (getDiscountInTuitionFee() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTaxEventTarget.discountInTuitionFee.required");
        }

        if (getTaxReservationDate() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTaxEventTarget.taxReservationDate.required");
        }

        if (getTaxReservationDescription() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTaxEventTarget.taxReservationDescription.required");
        }
    }

    public void annulDebts(String reason) {
        var api = TreasuryBridgeAPIFactory.implementation();

        IAcademicTreasuryEvent academicTreasuryEvent = api.getAcademicTreasuryEventForTarget(this);
        if (academicTreasuryEvent != null) {
            academicTreasuryEvent.annulDebts(reason);
        }
    }

    public static ReservationTaxEventTarget createReservationTaxDebt(ReservationTax reservationTax, Person person,
            DegreeCurricularPlan degreeCurricularPlan, ExecutionInterval executionInterval, LocalDate taxReservationDate) {
        return createReservationTaxDebt(reservationTax, person, degreeCurricularPlan, executionInterval, taxReservationDate,
                null);
    }

    public static ReservationTaxEventTarget createReservationTaxDebt(ReservationTax reservationTax, Person person,
            DegreeCurricularPlan degreeCurricularPlan, ExecutionInterval executionInterval, LocalDate taxReservationDate,
            LocalizedString additionalDescription) {
        LocalizedString emolumentDescription = reservationTax.buildEmolumentDescription(degreeCurricularPlan, executionInterval);

        if (additionalDescription != null) {
            for (Locale locale : TreasuryPlataformDependentServicesFactory.implementation().availableLocales()) {
                emolumentDescription = emolumentDescription.with(locale,
                        emolumentDescription.getContent(locale) + additionalDescription.getContent(locale));
            }
        }

        FinantialEntity finantialEntity = reservationTax.getFinantialEntity();
        Product product = reservationTax.getProduct();

        Optional<ReservationTaxEventTarget> target =
                ReservationTaxEventTarget.findUnique(person, product, degreeCurricularPlan, executionInterval);

        if (!target.isPresent()) {
            target = Optional.of(new ReservationTaxEventTarget(finantialEntity, product, person, degreeCurricularPlan,
                    executionInterval, Boolean.TRUE.equals(reservationTax.getDiscountInTuitionFee()), taxReservationDate,
                    emolumentDescription));
        }

        var api = TreasuryBridgeAPIFactory.implementation();
        var treasuryEvent = (AcademicTreasuryEvent) api.getAcademicTreasuryEventForTarget(target.get());

        if (treasuryEvent != null && treasuryEvent.isCharged()) {
            return target.get();
        }

        Optional<ReservationTaxTariff> tariff =
                ReservationTaxTariff.findUniqueTariff(reservationTax, degreeCurricularPlan.getDegree(), executionInterval);

        if (!tariff.isPresent()) {
            throw new AcademicTreasuryDomainException("error.ReservationTaxEventTarget.createReservationTaxDebt.tariff.not.found",
                    reservationTax.getName().getContent(), degreeCurricularPlan.getDegree().getPresentationName(),
                    executionInterval.getQualifiedName());
        }

        BigDecimal amount = tariff.get().getBaseAmount();
        LocalDate dueDate = tariff.get().calculateDueDate(taxReservationDate);

        DebtBuilderWithAmountAndDueDate debtBuilder = AcademicTreasuryTargetCreateDebtBuilder.createBuilder()
                .explicitAmountAndDueDate(finantialEntity, product, target.get(), taxReservationDate) //
                .setAmount(amount) //
                .setDueDate(dueDate) //
                .setCreatePaymentCode(Boolean.TRUE.equals(reservationTax.getCreatePaymentReferenceCode())) //
                .setInterestType(tariff.get().getInterestType()).setInterestFixedAmount(tariff.get().getInterestFixedAmount()) //
                .setPaymentCodePool((ISibsPaymentCodePoolService) finantialEntity.getFinantialInstitution()
                        .getDefaultDigitalPaymentPlatform());

        debtBuilder.createDebt();

        return target.get();
    }

    @Override
    public Degree getAcademicTreasuryTargetDegree() {
        return getDegreeCurricularPlan().getDegree();
    }

    @Override
    public LocalizedString getAcademicTreasuryTargetDescription() {
        return super.getTaxReservationDescription();
    }

    @Override
    public LocalDate getAcademicTreasuryTargetEventDate() {
        return super.getTaxReservationDate();
    }

    @Override
    public ExecutionInterval getAcademicTreasuryTargetExecutionSemester() {
        return null;
    }

    @Override
    public ExecutionYear getAcademicTreasuryTargetExecutionYear() {
        if (getExecutionInterval() instanceof ExecutionYear) {
            return (ExecutionYear) getExecutionInterval();
        }

        return super.getExecutionInterval().getExecutionYear();
    }

    @Override
    public Person getAcademicTreasuryTargetPerson() {
        return super.getPerson();
    }

    @Override
    public Map<String, String> getAcademicTreasuryTargetPropertiesMap() {
        return new HashMap<>();
    }

    @Override
    public Registration getAcademicTreasuryTargetRegistration() {
        return null;
    }

    @Override
    public void handleTotalPayment(IAcademicTreasuryEvent event) {
        HANDLE_TOTAL_PAYMENT.forEach((key, consumer) -> consumer.accept(this, event));
    }

    @Override
    public boolean isEventDiscountInTuitionFee() {
        return Boolean.TRUE.equals(super.getDiscountInTuitionFee());
    }

    public BigDecimal getAmountToPay() {
        var api = TreasuryBridgeAPIFactory.implementation();

        IAcademicTreasuryEvent academicTreasuryEvent = api.getAcademicTreasuryEventForTarget(this);
        if (academicTreasuryEvent != null) {
            academicTreasuryEvent.getAmountToPay();
        }

        return BigDecimal.ZERO;
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<ReservationTaxEventTarget> findAll() {
        return FenixFramework.getDomainRoot().getReservationTaxEventTargetsSet().stream();
    }

    public static Stream<ReservationTaxEventTarget> find(Person person, Product product,
            DegreeCurricularPlan degreeCurricularPlan, ExecutionInterval executionInterval) {
        return findAll().filter(t -> t.getPerson() == person).filter(r -> r.getProduct() == product)
                .filter(r -> r.getDegreeCurricularPlan() == degreeCurricularPlan)
                .filter(r -> r.getExecutionInterval() == executionInterval);
    }

    private static Optional<ReservationTaxEventTarget> findUnique(Person person, Product product,
            DegreeCurricularPlan degreeCurricularPlan, ExecutionInterval executionInterval) {
        return find(person, product, degreeCurricularPlan, executionInterval).findFirst();
    }

}
