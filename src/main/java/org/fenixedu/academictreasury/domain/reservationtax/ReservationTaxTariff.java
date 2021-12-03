package org.fenixedu.academictreasury.domain.reservationtax;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.domain.tariff.InterestType;
import org.fenixedu.treasury.domain.tariff.Tariff;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public class ReservationTaxTariff extends ReservationTaxTariff_Base {
    
    public ReservationTaxTariff() {
        super();
        
        setDomainRoot(FenixFramework.getDomainRoot());
    }
    
    protected ReservationTaxTariff(ReservationTax reservationTax, ExecutionYear executionYear, BigDecimal baseAmount,
            DueDateCalculationType dueDateCalculationType, int numberOfDaysAfterCreationForDueDate, LocalDate fixedDueDate,
            boolean applyInterests, InterestType interestType, BigDecimal interestFixedAmount) {

        this();

        super.setReservationTax(reservationTax);
        super.setExecutionYear(executionYear);

        super.setBaseAmount(baseAmount);
        super.setDueDateCalculationType(dueDateCalculationType);
        super.setNumberOfDaysAfterCreationForDueDate(numberOfDaysAfterCreationForDueDate);
        super.setFixedDueDate(fixedDueDate);

        super.setApplyInterests(applyInterests);
        super.setInterestType(interestType);
        super.setInterestFixedAmount(interestFixedAmount);

        checkRules();
    }

    private void checkRules() {
        // TODO
    }

    public void edit(BigDecimal baseAmount, DueDateCalculationType dueDateCalculationType,
            int numberOfDaysAfterCreationForDueDate, LocalDate fixedDueDate, boolean applyInterests, InterestType interestType,
            BigDecimal interestFixedAmount) {

        super.setBaseAmount(baseAmount);
        super.setDueDateCalculationType(dueDateCalculationType);
        super.setNumberOfDaysAfterCreationForDueDate(numberOfDaysAfterCreationForDueDate);
        super.setFixedDueDate(fixedDueDate);

        super.setApplyInterests(applyInterests);
        super.setInterestType(interestType);
        super.setInterestFixedAmount(interestFixedAmount);

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
        super.setExecutionYear(null);

        getDegreesSet().clear();

        super.deleteDomainObject();
    }

    public LocalDate calculateDueDate(final LocalDate requestDate) {
        return Tariff.calculateDueDate(requestDate, getDueDateCalculationType(), getNumberOfDaysAfterCreationForDueDate(),
                getFixedDueDate());
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static ReservationTaxTariff create(
            ReservationTax reservationTax, ExecutionYear executionYear, 
            BigDecimal baseAmount, DueDateCalculationType dueDateCalculationType, int numberOfDaysAfterCreationForDueDate,
            LocalDate fixedDueDate,
            boolean applyInterests, InterestType interestType, BigDecimal interestFixedAmount) {

        return new ReservationTaxTariff(reservationTax, executionYear, baseAmount, dueDateCalculationType,
                numberOfDaysAfterCreationForDueDate, fixedDueDate, applyInterests, interestType, interestFixedAmount);
    }

    public static Stream<ReservationTaxTariff> findAll() {
        return FenixFramework.getDomainRoot().getReservationTaxTariffsSet().stream();
    }

    public static Stream<ReservationTaxTariff> find(ReservationTax reservationTax) {
        return reservationTax.getReservationTaxTariffsSet().stream();
    }

    public static Stream<ReservationTaxTariff> find(ReservationTax reservationTax, Degree degree, ExecutionYear executionYear) {
        return find(reservationTax).filter(t -> t.getDegreesSet().contains(degree) && t.getExecutionYear() == executionYear);
    }

    public static Optional<ReservationTaxTariff> findUniqueTariff(ReservationTax reservationTax, Degree degree,
            ExecutionYear executionYear) {
        return find(reservationTax, degree, executionYear).findFirst();
    }
}