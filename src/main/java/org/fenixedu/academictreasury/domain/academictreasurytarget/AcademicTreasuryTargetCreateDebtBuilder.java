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
package org.fenixedu.academictreasury.domain.academictreasurytarget;

import java.math.BigDecimal;
import java.util.Collections;

import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryTarget;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tariff.AcademicTariff;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.paymentcodes.integration.ISibsPaymentCodePoolService;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.domain.tariff.InterestRateType;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class AcademicTreasuryTargetCreateDebtBuilder {

    private FinantialEntity finantialEntity;
    private Product product;
    private IAcademicTreasuryTarget target;

    private LocalDate when;
    private boolean createPaymentCode;
    private ISibsPaymentCodePoolService paymentCodePool;

    public class DebtBuilderWithAmountAndDueDate {
        private BigDecimal amount;
        private LocalDate dueDate;

        private InterestRateType interestRateType;
        private BigDecimal interestFixedAmount;

        public DebtBuilderWithAmountAndDueDate setAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public DebtBuilderWithAmountAndDueDate setWhen(LocalDate when) {
            AcademicTreasuryTargetCreateDebtBuilder.this.when = when;
            return this;
        }

        public DebtBuilderWithAmountAndDueDate setDueDate(LocalDate dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public DebtBuilderWithAmountAndDueDate setCreatePaymentCode(boolean createPaymentCode) {
            AcademicTreasuryTargetCreateDebtBuilder.this.createPaymentCode = createPaymentCode;
            return this;
        }

        public DebtBuilderWithAmountAndDueDate setPaymentCodePool(ISibsPaymentCodePoolService paymentCodePool) {
            AcademicTreasuryTargetCreateDebtBuilder.this.paymentCodePool = paymentCodePool;
            return this;
        }

        public DebtBuilderWithAmountAndDueDate setFinantialEntity(FinantialEntity finantialEntity) {
            AcademicTreasuryTargetCreateDebtBuilder.this.finantialEntity = finantialEntity;
            return this;
        }

        public DebtBuilderWithAmountAndDueDate setProduct(Product product) {
            AcademicTreasuryTargetCreateDebtBuilder.this.product = product;
            return this;
        }

        public DebtBuilderWithAmountAndDueDate setTarget(IAcademicTreasuryTarget target) {
            AcademicTreasuryTargetCreateDebtBuilder.this.target = target;
            return this;
        }

        public DebtBuilderWithAmountAndDueDate setInterestRateType(InterestRateType interestRateType) {
            this.interestRateType = interestRateType;
            return this;
        }

        public DebtBuilderWithAmountAndDueDate setInterestFixedAmount(BigDecimal interestFixedAmount) {
            this.interestFixedAmount = interestFixedAmount;
            return this;
        }

        public IAcademicTreasuryEvent createDebt() {
            checkParameters();

            var api = TreasuryBridgeAPIFactory.implementation();

            var finantialInstitution = finantialEntity.getFinantialInstitution();
            var documentNumberSeries =
                    DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get();
            var now = new DateTime();
            var vat = Vat.findActiveUnique(product.getVatType(), finantialInstitution, new DateTime()).get();
            var person = target.getAcademicTreasuryTargetPerson();

            var personCustomer = AcademicTreasuryPlataformDependentServicesFactory.implementation().personCustomer(person);
            if (personCustomer == null) {
                personCustomer = PersonCustomer.createWithCurrentFiscalInformation(person);
            }

            var debtAccount = DebtAccount.findUnique(finantialInstitution, personCustomer).orElse(null);
            if (debtAccount == null) {
                debtAccount = DebtAccount.create(finantialInstitution, personCustomer);
            }

            var treasuryEvent = (AcademicTreasuryEvent) api.getAcademicTreasuryEventForTarget(target);
            if (treasuryEvent == null) {
                treasuryEvent = AcademicTreasuryEvent.createForAcademicTreasuryEventTarget(product, target);
            }

            var debitNote = DebitNote.create(debtAccount, documentNumberSeries, now);
            var debitEntry = DebitEntry.create(finantialEntity, debtAccount, treasuryEvent, vat, amount, dueDate,
                    target.getAcademicTreasuryTargetPropertiesMap(), product,
                    target.getAcademicTreasuryTargetDescription().getContent(TreasuryConstants.DEFAULT_LANGUAGE), BigDecimal.ONE,
                    null, when.toDateTimeAtStartOfDay(), false, false, debitNote);

            if (this.interestRateType != null) {
                InterestRate.createForDebitEntry(debitEntry, this.interestRateType, 1, false, 0, this.interestFixedAmount, null);
            }

            if (createPaymentCode) {
                createPaymentReferenceCode(debitEntry, dueDate);
            }

            return treasuryEvent;
        }

        private void checkParameters() {
            if (AcademicTreasuryTargetCreateDebtBuilder.this.finantialEntity == null) {
                throw new IllegalArgumentException("Finantial entity required");
            }

            if (AcademicTreasuryTargetCreateDebtBuilder.this.product == null) {
                throw new IllegalArgumentException("Product required");
            }

            if (AcademicTreasuryTargetCreateDebtBuilder.this.target == null) {
                throw new IllegalArgumentException("AcademicTreasuryTarget required");
            }

            if (this.amount == null) {
                throw new IllegalArgumentException("Amount required");
            }

            if (this.dueDate == null) {
                throw new IllegalArgumentException("Due date required");
            }

            if (AcademicTreasuryTargetCreateDebtBuilder.this.createPaymentCode
                    && AcademicTreasuryTargetCreateDebtBuilder.this.paymentCodePool == null) {
                throw new IllegalArgumentException("Payment code pool required");
            }
        }

    }

    public class DebtBuilderWithAcademicTariff {
        private int numberOfUnits;
        private boolean urgentRequest;
        private boolean applyLanguageRate;

        public DebtBuilderWithAcademicTariff setWhen(LocalDate when) {
            AcademicTreasuryTargetCreateDebtBuilder.this.when = when;
            return this;
        }

        public DebtBuilderWithAcademicTariff setCreatePaymentCode(boolean createPaymentCode) {
            AcademicTreasuryTargetCreateDebtBuilder.this.createPaymentCode = createPaymentCode;
            return this;
        }

        public DebtBuilderWithAcademicTariff setPaymentCodePool(ISibsPaymentCodePoolService paymentCodePool) {
            AcademicTreasuryTargetCreateDebtBuilder.this.paymentCodePool = paymentCodePool;
            return this;
        }

        public DebtBuilderWithAcademicTariff setNumberOfUnits(int numberOfUnits) {
            this.numberOfUnits = numberOfUnits;
            return this;
        }

        public DebtBuilderWithAcademicTariff setUrgentRequest(boolean urgentRequest) {
            this.urgentRequest = urgentRequest;
            return this;
        }

        public DebtBuilderWithAcademicTariff setApplyLanguageRate(boolean applyLanguageRate) {
            this.applyLanguageRate = applyLanguageRate;
            return this;
        }

        public DebtBuilderWithAcademicTariff setFinantialEntity(FinantialEntity finantialEntity) {
            AcademicTreasuryTargetCreateDebtBuilder.this.finantialEntity = finantialEntity;
            return this;
        }

        public DebtBuilderWithAcademicTariff setProduct(Product product) {
            AcademicTreasuryTargetCreateDebtBuilder.this.product = product;
            return this;
        }

        public DebtBuilderWithAcademicTariff setTarget(IAcademicTreasuryTarget target) {
            AcademicTreasuryTargetCreateDebtBuilder.this.target = target;
            return this;
        }

        public IAcademicTreasuryEvent createDebt() {
            checkParameters();

            var api = TreasuryBridgeAPIFactory.implementation();
            var finantialInstitution = finantialEntity.getFinantialInstitution();
            var documentNumberSeries =
                    DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get();
            var now = new DateTime();
            var vat = Vat.findActiveUnique(product.getVatType(), finantialInstitution, new DateTime()).get();
            var person = target.getAcademicTreasuryTargetPerson();

            var personCustomer = AcademicTreasuryPlataformDependentServicesFactory.implementation().personCustomer(person);
            if (personCustomer == null) {
                personCustomer = PersonCustomer.createWithCurrentFiscalInformation(person);
            }

            var debtAccount = DebtAccount.findUnique(finantialInstitution, personCustomer)
                    .orElse(DebtAccount.create(finantialInstitution, personCustomer));

            var treasuryEvent = (AcademicTreasuryEvent) api.getAcademicTreasuryEventForTarget(target);

            if (treasuryEvent == null) {
                treasuryEvent = AcademicTreasuryEvent.createForAcademicTreasuryEventTarget(product, target);
            }

            if (treasuryEvent.isCharged()) {
                return treasuryEvent;
            }

            AcademicTariff academicTariff = null;
            if (target.getAcademicTreasuryTargetDegree() != null) {
                academicTariff = AcademicTariff.findMatch(finantialEntity, product, target.getAcademicTreasuryTargetDegree(),
                        when.toDateTimeAtStartOfDay());
            } else {
                academicTariff = AcademicTariff.findMatch(finantialEntity, product, when.toDateTimeAtStartOfDay());
            }

            if (academicTariff == null) {
                throw new AcademicTreasuryDomainException("error.EmolumentServices.tariff.not.found",
                        when.toString(TreasuryConstants.DATE_FORMAT_YYYY_MM_DD));
            }

            var dueDate = academicTariff.dueDate(when);
            var debitNote = DebitNote.create(debtAccount, documentNumberSeries, now);

            var amount = academicTariff.amountToPay(this.numberOfUnits, 0, this.applyLanguageRate, this.urgentRequest);
            var debitEntry = DebitEntry.create(finantialEntity, debtAccount, treasuryEvent, vat, amount, dueDate,
                    target.getAcademicTreasuryTargetPropertiesMap(), product,
                    target.getAcademicTreasuryTargetDescription().getContent(TreasuryConstants.DEFAULT_LANGUAGE), BigDecimal.ONE,
                    academicTariff.getInterestRate(), when.toDateTimeAtStartOfDay(), false, false, debitNote);

            if (createPaymentCode) {
                createPaymentReferenceCode(debitEntry, dueDate);
            }

            return treasuryEvent;
        }

        private void checkParameters() {
            if (AcademicTreasuryTargetCreateDebtBuilder.this.finantialEntity == null) {
                throw new IllegalArgumentException("Finantial entity required");
            }

            if (AcademicTreasuryTargetCreateDebtBuilder.this.product == null) {
                throw new IllegalArgumentException("Product required");
            }

            if (AcademicTreasuryTargetCreateDebtBuilder.this.target == null) {
                throw new IllegalArgumentException("AcademicTreasuryTarget required");
            }

            if (AcademicTreasuryTargetCreateDebtBuilder.this.createPaymentCode
                    && AcademicTreasuryTargetCreateDebtBuilder.this.paymentCodePool == null) {
                throw new IllegalArgumentException("Payment code pool required");
            }
        }

    }

    protected AcademicTreasuryTargetCreateDebtBuilder() {
    }

    private SibsPaymentRequest createPaymentReferenceCode(DebitEntry debitEntry, LocalDate dueDate) {
        return paymentCodePool.createSibsPaymentRequest(debitEntry.getDebtAccount(), Collections.singleton(debitEntry),
                Collections.emptySet());
    }

    public static AcademicTreasuryTargetCreateDebtBuilder createBuilder() {
        return new AcademicTreasuryTargetCreateDebtBuilder();
    }

    public DebtBuilderWithAmountAndDueDate explicitAmountAndDueDate(FinantialEntity finantialEntity, Product product,
            IAcademicTreasuryTarget target, LocalDate when) {
        var result = new DebtBuilderWithAmountAndDueDate();
        result.setFinantialEntity(finantialEntity);
        result.setProduct(product);
        result.setTarget(target);
        result.setWhen(when);

        return result;
    }

    public DebtBuilderWithAcademicTariff useInferedAcademicTariff(FinantialEntity finantialEntity, Product product,
            IAcademicTreasuryTarget target, LocalDate when) {
        var result = new DebtBuilderWithAcademicTariff();
        result.setFinantialEntity(finantialEntity);
        result.setProduct(product);
        result.setTarget(target);
        result.setWhen(when);

        return result;
    }
}
