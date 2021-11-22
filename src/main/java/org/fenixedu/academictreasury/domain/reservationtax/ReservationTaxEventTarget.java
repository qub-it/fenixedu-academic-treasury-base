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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryTarget;
import org.fenixedu.academictreasury.domain.academictreasurytarget.AcademicTreasuryTargetCreateDebtBuilder;
import org.fenixedu.academictreasury.domain.academictreasurytarget.AcademicTreasuryTargetCreateDebtBuilder.DebtBuilderWithAmountAndDueDate;
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

    public ReservationTaxEventTarget() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected ReservationTaxEventTarget(FinantialEntity finantialEntity, Product product, Person person,
            DegreeCurricularPlan degreeCurricularPlan, ExecutionYear executionYear, boolean discountInTuitionFee,
            LocalDate taxReservationDate, LocalizedString taxReservationDescription) {
        this();

        super.setFinantialEntity(finantialEntity);
        super.setProduct(product);
        super.setPerson(person);
        super.setDegreeCurricularPlan(degreeCurricularPlan);
        super.setExecutionYear(executionYear);
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

        if (getExecutionYear() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTaxEventTarget.executionYear.required");
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

    public static ReservationTaxEventTarget createReservationTaxDebt(ReservationTax reservationTax, Person person,
            DegreeCurricularPlan degreeCurricularPlan, ExecutionYear executionYear, LocalDate taxReservationDate) {
        return createReservationTaxDebt(reservationTax, person, degreeCurricularPlan, executionYear, taxReservationDate, null);
    }

    public static ReservationTaxEventTarget createReservationTaxDebt(ReservationTax reservationTax, Person person,
            DegreeCurricularPlan degreeCurricularPlan, ExecutionYear executionYear, LocalDate taxReservationDate,
            LocalizedString additionalDescription) {
        LocalizedString emolumentDescription = reservationTax.buildEmolumentDescription(executionYear);

        if (additionalDescription != null) {
            for (Locale locale : TreasuryPlataformDependentServicesFactory.implementation().availableLocales()) {
                emolumentDescription = emolumentDescription.with(locale,
                        emolumentDescription.getContent(locale) + additionalDescription.getContent(locale));
            }
        }

        FinantialEntity finantialEntity = reservationTax.getFinantialEntity();
        Product product = reservationTax.getProduct();

        Optional<ReservationTaxEventTarget> target =
                ReservationTaxEventTarget.findUnique(person, product, degreeCurricularPlan, executionYear);

        if (!target.isPresent()) {
            target = Optional.of(new ReservationTaxEventTarget(finantialEntity, product, person, degreeCurricularPlan,
                    executionYear,
                    Boolean.TRUE.equals(reservationTax.getDiscountInTuitionFee()), taxReservationDate, emolumentDescription));
        }

        Optional<ReservationTaxTariff> tariff =
                ReservationTaxTariff.findUniqueTariff(reservationTax, degreeCurricularPlan.getDegree(), executionYear);

        if (!tariff.isPresent()) {
            throw new AcademicTreasuryDomainException("error.ReservationTaxEventTarget.createReservationTaxDebt.tariff.not.found",
                    degreeCurricularPlan.getDegree().getPresentationName(), executionYear.getQualifiedName());
        }

        BigDecimal amount = tariff.get().getBaseAmount();
        LocalDate dueDate = tariff.get().calculateDueDate(taxReservationDate);

        DebtBuilderWithAmountAndDueDate debtBuilder = AcademicTreasuryTargetCreateDebtBuilder.createBuilder()
                .explicitAmountAndDueDate(finantialEntity, product, target.get(), taxReservationDate).setAmount(amount)
                .setDueDate(dueDate).setCreatePaymentCode(Boolean.TRUE.equals(reservationTax.getCreatePaymentReferenceCode()))
                .setInterestType(tariff.get().getInterestType()).setInterestFixedAmount(tariff.get().getInterestFixedAmount())
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
        return super.getExecutionYear();
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
    }

    @Override
    public boolean isEventDiscountInTuitionFee() {
        return Boolean.TRUE.equals(super.getDiscountInTuitionFee());
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
            DegreeCurricularPlan degreeCurricularPlan, ExecutionYear executionYear) {
        return findAll().filter(t -> t.getPerson() == person).filter(r -> r.getProduct() == product)
                .filter(r -> r.getDegreeCurricularPlan() == degreeCurricularPlan)
                .filter(r -> r.getExecutionYear() == executionYear);
    }

    private static Optional<ReservationTaxEventTarget> findUnique(Person person, Product product,
            DegreeCurricularPlan degreeCurricularPlan, ExecutionYear executionYear) {
        return find(person, product, degreeCurricularPlan, executionYear).findFirst();
    }

}
