/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * * Neither the name of Quorum Born IT nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 * * Universidade de Lisboa and its respective subsidiary Serviços Centrais da Universidade
 * de Lisboa (Departamento de Informática), hereby referred to as the Beneficiary, is the
 * sole demonstrated end-user and ultimately the only beneficiary of the redistributed binary
 * form and/or source code.
 * * The Beneficiary is entrusted with either the binary form, the source code, or both, and
 * by accepting it, accepts the terms of this License.
 * * Redistribution of any binary form and/or source code is only allowed in the scope of the
 * Universidade de Lisboa FenixEdu(™)’s implementation projects.
 * * This license and conditions of redistribution of source code/binary can only be reviewed
 * by the Steering Comittee of FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT” BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
