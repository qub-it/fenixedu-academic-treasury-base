package org.fenixedu.academictreasury.domain.academictreasurytarget;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryTarget;
import org.fenixedu.academic.domain.treasury.ITreasuryBridgeAPI;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tariff.AcademicTariff;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.dto.document.managepayments.PaymentReferenceCodeBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class AcademicTreasuryTargetCreateDebtBuilder {

    private FinantialEntity finantialEntity;
    private Product product;
    private IAcademicTreasuryTarget target;

    private LocalDate when;
    private boolean createPaymentCode;
    private PaymentCodePool paymentCodePool;

    public class DebtBuilderWithAmountAndDueDate {
        private BigDecimal amount;
        private LocalDate dueDate;

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

        public DebtBuilderWithAmountAndDueDate setPaymentCodePool(PaymentCodePool paymentCodePool) {
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

        public IAcademicTreasuryEvent createDebt() {
            checkParameters();

            var api = TreasuryBridgeAPIFactory.implementation();

            var finantialInstitution = finantialEntity.getFinantialInstitution();
            var documentNumberSeries =
                    DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get();
            var now = new DateTime();
            var vat = Vat.findActiveUnique(((Product) product).getVatType(), finantialInstitution, when.toDateTimeAtStartOfDay())
                    .get();
            var person = target.getAcademicTreasuryTargetPerson();

            var personCustomer = person.getPersonCustomer();
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
            var debitEntry = DebitEntry.create(Optional.of(debitNote), debtAccount, treasuryEvent, vat, amount,
                    dueDate, target.getAcademicTreasuryTargetPropertiesMap(), product,
                    target.getAcademicTreasuryTargetDescription().getContent(TreasuryConstants.DEFAULT_LANGUAGE), BigDecimal.ONE,
                    null, when.toDateTimeAtStartOfDay());

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

        public DebtBuilderWithAcademicTariff setPaymentCodePool(PaymentCodePool paymentCodePool) {
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
            var vat = Vat.findActiveUnique(((Product) product).getVatType(), finantialInstitution, when.toDateTimeAtStartOfDay())
                    .get();
            var administrativeOffice = finantialEntity.getAdministrativeOffice();
            var person = target.getAcademicTreasuryTargetPerson();

            var personCustomer = person.getPersonCustomer();
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
                academicTariff =
                        AcademicTariff.findMatch(finantialEntity, product, administrativeOffice, when.toDateTimeAtStartOfDay());
            }

            if (academicTariff == null) {
                throw new AcademicTreasuryDomainException("error.EmolumentServices.tariff.not.found",
                        when.toString(TreasuryConstants.DATE_FORMAT_YYYY_MM_DD));
            }

            var dueDate = academicTariff.dueDate(when);
            var debitNote = DebitNote.create(debtAccount, documentNumberSeries, now);

            var amount = academicTariff.amountToPay(this.numberOfUnits, 0, this.applyLanguageRate, this.urgentRequest);
            var debitEntry = DebitEntry.create(Optional.of(debitNote), debtAccount, treasuryEvent, vat, amount, dueDate,
                    target.getAcademicTreasuryTargetPropertiesMap(), product,
                    target.getAcademicTreasuryTargetDescription().getContent(TreasuryConstants.DEFAULT_LANGUAGE), BigDecimal.ONE,
                    academicTariff.getInterestRate(), when.toDateTimeAtStartOfDay());

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

    private PaymentReferenceCode createPaymentReferenceCode(DebitEntry debitEntry, LocalDate dueDate) {

        final PaymentReferenceCodeBean referenceCodeBean =
                new PaymentReferenceCodeBean(this.paymentCodePool, debitEntry.getDebtAccount());
        referenceCodeBean.setBeginDate(when);
        referenceCodeBean.setEndDate(dueDate);
        referenceCodeBean.setSelectedDebitEntries(new ArrayList<DebitEntry>());
        referenceCodeBean.getSelectedDebitEntries().add(debitEntry);

        final PaymentReferenceCode paymentReferenceCode = PaymentReferenceCode
                .createPaymentReferenceCodeForMultipleDebitEntries(debitEntry.getDebtAccount(), referenceCodeBean);

        return paymentReferenceCode;
    }

    public static AcademicTreasuryTargetCreateDebtBuilder createBuilder() {
        return new AcademicTreasuryTargetCreateDebtBuilder();
    }

    public DebtBuilderWithAmountAndDueDate explicitAmountAndDueDate(FinantialEntity finantialEntity, Product product,
            IAcademicTreasuryTarget target) {
        var result = new DebtBuilderWithAmountAndDueDate();
        result.setFinantialEntity(finantialEntity);
        result.setProduct(product);
        result.setTarget(target);

        return result;
    }

    public DebtBuilderWithAcademicTariff useInferedAcademicTariff(FinantialEntity finantialEntity, Product product,
            IAcademicTreasuryTarget target) {
        var result = new DebtBuilderWithAcademicTariff();
        result.setFinantialEntity(finantialEntity);
        result.setProduct(product);
        result.setTarget(target);

        return result;
    }
}
