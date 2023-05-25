package org.fenixedu.academictreasury.domain.reservationtax;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.domain.tariff.InterestRateType;
import org.fenixedu.treasury.domain.tariff.InterestType;
import org.fenixedu.treasury.domain.tariff.Tariff;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public class ReservationTaxTariff extends ReservationTaxTariff_Base {
    
    public ReservationTaxTariff() {
        super();
        
        setDomainRoot(FenixFramework.getDomainRoot());
    }
    
    protected ReservationTaxTariff(ReservationTax reservationTax, ExecutionInterval executionInterval, BigDecimal baseAmount,
            DueDateCalculationType dueDateCalculationType, int numberOfDaysAfterCreationForDueDate, LocalDate fixedDueDate,
            boolean applyInterests, InterestRateType interestRateType, BigDecimal interestFixedAmount) {

        this();

        super.setReservationTax(reservationTax);
        super.setExecutionInterval(executionInterval);

        super.setBaseAmount(baseAmount);
        super.setDueDateCalculationType(dueDateCalculationType);
        super.setNumberOfDaysAfterCreationForDueDate(numberOfDaysAfterCreationForDueDate);
        super.setFixedDueDate(fixedDueDate);

        super.setApplyInterests(applyInterests);
        
        if(applyInterests) {
            super.setInterestRateType(interestRateType);
            super.setInterestFixedAmount(interestFixedAmount);
        } else {
            super.setInterestRateType(null);
            super.setInterestFixedAmount(null);
        }

        checkRules();
    }

    private void checkRules() {
        // TODO
    }

    public void edit(BigDecimal baseAmount, DueDateCalculationType dueDateCalculationType,
            int numberOfDaysAfterCreationForDueDate, LocalDate fixedDueDate, boolean applyInterests, InterestRateType interestRateType,
            BigDecimal interestFixedAmount) {

        super.setBaseAmount(baseAmount);
        super.setDueDateCalculationType(dueDateCalculationType);
        super.setNumberOfDaysAfterCreationForDueDate(numberOfDaysAfterCreationForDueDate);
        super.setFixedDueDate(fixedDueDate);

        super.setApplyInterests(applyInterests);

        if(applyInterests) {
            super.setInterestRateType(interestRateType);
            super.setInterestFixedAmount(interestFixedAmount);
        } else {
            super.setInterestRateType(null);
            super.setInterestFixedAmount(null);
        }
        
        checkRules();
    }

    public void addDegrees(Set<Degree> degrees) {
        super.getDegreesSet().addAll(degrees);
    }

    public void removeDegrees(Set<Degree> degrees) {
        super.getDegreesSet().removeAll(degrees);
    }

    public void delete() {
        super.setDomainRoot(null);
        super.setReservationTax(null);
        super.setExecutionInterval(null);
        super.setInterestRateType(null);

        getDegreesSet().clear();

        super.deleteDomainObject();
    }

    public LocalDate calculateDueDate(final LocalDate requestDate) {
        return Tariff.calculateDueDate(requestDate, getDueDateCalculationType(), getNumberOfDaysAfterCreationForDueDate(),
                getFixedDueDate());
    }
    
    @Override
    @Deprecated
    public InterestType getInterestType() {
        return super.getInterestType();
    }
    
    @Override
    @Deprecated
    public void setInterestType(InterestType interestType) {
        super.setInterestType(interestType);
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static ReservationTaxTariff create(
            ReservationTax reservationTax, ExecutionInterval executionInterval, 
            BigDecimal baseAmount, DueDateCalculationType dueDateCalculationType, int numberOfDaysAfterCreationForDueDate,
            LocalDate fixedDueDate,
            boolean applyInterests, InterestRateType interestRateType, BigDecimal interestFixedAmount) {

        return new ReservationTaxTariff(reservationTax, executionInterval, baseAmount, dueDateCalculationType,
                numberOfDaysAfterCreationForDueDate, fixedDueDate, applyInterests, interestRateType, interestFixedAmount);
    }

    public static Stream<ReservationTaxTariff> findAll() {
        return FenixFramework.getDomainRoot().getReservationTaxTariffsSet().stream();
    }

    public static Stream<ReservationTaxTariff> find(ReservationTax reservationTax) {
        return reservationTax.getReservationTaxTariffsSet().stream();
    }

    public static Stream<ReservationTaxTariff> find(ReservationTax reservationTax, Degree degree, ExecutionInterval executionInterval) {
        return find(reservationTax).filter(t -> t.getDegreesSet().contains(degree) && t.getExecutionInterval() == executionInterval);
    }

    public static Optional<ReservationTaxTariff> findUniqueTariff(ReservationTax reservationTax, Degree degree,
            ExecutionInterval executionInterval) {
        return find(reservationTax, degree, executionInterval).findFirst();
    }
}
