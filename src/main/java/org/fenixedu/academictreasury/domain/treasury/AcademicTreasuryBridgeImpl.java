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
package org.fenixedu.academictreasury.domain.treasury;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EnrolmentEvaluation;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.administrativeOffice.AdministrativeOffice;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.contacts.PhysicalAddressData;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.treasury.IAcademicServiceRequestAndAcademicTaxTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryTarget;
import org.fenixedu.academic.domain.treasury.IImprovementTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IPaymentCodePool;
import org.fenixedu.academic.domain.treasury.ITreasuryBridgeAPI;
import org.fenixedu.academic.domain.treasury.ITreasuryCustomer;
import org.fenixedu.academic.domain.treasury.ITreasuryDebtAccount;
import org.fenixedu.academic.domain.treasury.ITreasuryEntity;
import org.fenixedu.academic.domain.treasury.ITreasuryProduct;
import org.fenixedu.academic.domain.treasury.ITuitionTreasuryEvent;
import org.fenixedu.academictreasury.domain.academicalAct.AcademicActBlockingSuspension;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtGenerationRule;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.domain.tariff.AcademicTariff;
import org.fenixedu.academictreasury.services.AcademicTaxServices;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.services.PersonServices;
import org.fenixedu.academictreasury.services.TuitionServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.util.FiscalCodeValidation;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;

import pt.ist.fenixframework.FenixFramework;

public class AcademicTreasuryBridgeImpl implements ITreasuryBridgeAPI {

    private static final Logger logger = LoggerFactory.getLogger(AcademicTreasuryBridgeImpl.class);

    private static class AcademicProduct implements ITreasuryProduct {

        private Product product;

        private AcademicProduct(final Product product) {
            this.product = product;
        }

        @Override
        public String getCode() {
            return product.getCode();
        }

        @Override
        public String getName() {
            return product.getName().getContent();
        }

        @Override
        public int hashCode() {
            return product.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof AcademicProduct) && ((AcademicProduct) obj).product == this.product;
        }
    }

    private static class TreasuryEntity implements ITreasuryEntity {

        private FinantialEntity finantialEntity;

        public TreasuryEntity(final FinantialEntity finantialEntity) {
            this.finantialEntity = finantialEntity;
        }

        @Override
        public String getCode() {
            return finantialEntity.getExternalId();
        }

        @Override
        public String getName() {
            return finantialEntity.getName().getContent();
        }

        @Override
        public int hashCode() {
            return finantialEntity.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof TreasuryEntity) && ((TreasuryEntity) obj).finantialEntity == this.finantialEntity;
        }
    }

    private static class PaymentCodePoolImpl implements IPaymentCodePool {

        private DigitalPaymentPlatform digitalPaymentPlatform;

        public PaymentCodePoolImpl(final DigitalPaymentPlatform digitalPaymentPlatform) {
            this.digitalPaymentPlatform = digitalPaymentPlatform;
        }

        @Override
        public String getCode() {
            return this.digitalPaymentPlatform.getExternalId();
        }

        @Override
        public String getName() {
            return this.digitalPaymentPlatform.getName();
        }

        @Override
        public boolean isActive() {
            return this.digitalPaymentPlatform.isActive();
        }

        @Override
        public int hashCode() {
            return this.digitalPaymentPlatform.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj instanceof PaymentCodePoolImpl
                    && ((PaymentCodePoolImpl) obj).digitalPaymentPlatform == this.digitalPaymentPlatform;
        }
    }

    private static class TreasuryCustomer implements ITreasuryCustomer {

        private PersonCustomer personCustomer;

        public TreasuryCustomer(final PersonCustomer personCustomer) {
            this.personCustomer = personCustomer;
        }

        @Override
        public String getExternalId() {
            return personCustomer.getExternalId();
        }

        @Override
        @Deprecated
        public String getFiscalCountry() {
            return personCustomer.getFiscalCountry();
        }

        @Override
        public String getFiscalNumber() {
            return personCustomer.getFiscalNumber();
        }

        @Override
        public String getUiFiscalNumber() {
            return personCustomer.getUiFiscalNumber();
        }
    }

    private static class TreasuryDebtAccount implements ITreasuryDebtAccount {

        private DebtAccount debtAccount;

        public TreasuryDebtAccount(final DebtAccount debtAccount) {
            this.debtAccount = debtAccount;
        }

        @Override
        public String getExternalId() {
            return debtAccount.getExternalId();
        }

    }

    // @formatter:off
    /* ---------------------------------
     * TREASURY INSTITUTION AND PRODUCTS
     * ---------------------------------
     */
    // @formatter:on

    @Override
    public Set<ITreasuryEntity> getTreasuryEntities() {
        return FinantialEntity.findAll().map(f -> new TreasuryEntity(f)).collect(Collectors.toSet());
    }

    @Override
    public ITreasuryEntity getTreasuryEntityByCode(final String code) {
        if (FenixFramework.getDomainObject(code) == null) {
            throw new AcademicTreasuryDomainException("error.ITreasuryBridgeAPI.finantial.entity.not.found");
        }

        return new TreasuryEntity(FenixFramework.getDomainObject(code));
    }

    @Override
    public Set<ITreasuryProduct> getProducts(final ITreasuryEntity treasuryEntity) {
        return Product.findAllActive()
                .filter(p -> p.getFinantialInstitutionsSet()
                        .contains(((TreasuryEntity) treasuryEntity).finantialEntity.getFinantialInstitution()))
                .map(p -> new AcademicProduct(p)).collect(Collectors.toSet());
    }

    @Override
    public ITreasuryProduct getProductByCode(final String code) {
        return Product.findUniqueByCode(code).map(p -> new AcademicProduct(p)).orElse(null);
    }

    @Override
    public List<IPaymentCodePool> getPaymentCodePools(final ITreasuryEntity treasuryEntity) {
        final FinantialInstitution finantialInstitution =
                ((TreasuryEntity) treasuryEntity).finantialEntity.getFinantialInstitution();

        return DigitalPaymentPlatform.findForSibsPaymentCodeServiceByActive(finantialInstitution, true)
                .map(p -> new PaymentCodePoolImpl(p)).collect(Collectors.toList());
    }

    @Override
    public IPaymentCodePool getPaymentCodePoolByCode(final String code) {
        if (FenixFramework.getDomainObject(code) == null) {
            throw new AcademicTreasuryDomainException("error.ITreasuryBridgeAPI.paymentCodePool.not.found");
        }

        return new PaymentCodePoolImpl((DigitalPaymentPlatform) FenixFramework.getDomainObject(code));
    }

    /* ------------------------
     * ACADEMIC SERVICE REQUEST
     * ------------------------
     */

    @Override
    public IAcademicServiceRequestAndAcademicTaxTreasuryEvent academicTreasuryEventForAcademicServiceRequest(
            AcademicServiceRequest academicServiceRequest) {
        return AcademicTreasuryPlataformDependentServicesFactory.implementation()
                .academicTreasuryEventsSet(academicServiceRequest.getPerson()).stream()
                .filter(e -> e.getAcademicServiceRequest() == academicServiceRequest).findFirst().orElse(null);
    }

    /* ----------
     * ENROLMENTS
     * ----------
     */

    @Override
    public void standaloneUnenrolment(Enrolment standaloneEnrolment) {
        TuitionServices.removeDebitEntryForStandaloneEnrolment(standaloneEnrolment);
    }

    @Override
    public void extracurricularUnenrolment(Enrolment extracurricularEnrolment) {
        TuitionServices.removeDebitEntryForExtracurricularEnrolment(extracurricularEnrolment);
    }

    /* --------
     * TUITIONS
     * --------
     */

    @Override
    public ITuitionTreasuryEvent getTuitionForRegistrationTreasuryEvent(final Registration registration,
            final ExecutionYear executionYear) {
        return TuitionServices.findAcademicTreasuryEventTuitionForRegistration(registration, executionYear);
    }

    @Override
    public ITuitionTreasuryEvent getTuitionForStandaloneTreasuryEvent(final Registration registration,
            final ExecutionYear executionYear) {
        return TuitionServices.findAcademicTreasuryEventTuitionForStandalone(registration, executionYear);
    }

    @Override
    public ITuitionTreasuryEvent getTuitionForExtracurricularTreasuryEvent(final Registration registration,
            final ExecutionYear executionYear) {
        return null;
    }

    @Override
    public ITuitionTreasuryEvent getTuitionForImprovementTreasuryEvent(final Registration registration,
            final ExecutionYear executionYear) {
        return AcademicTaxServices.findAcademicTreasuryEventForImprovementTax(registration, executionYear);
    }

    @Override
    public void improvementUnrenrolment(EnrolmentEvaluation improvementEnrolmentEvaluation) {
        AcademicTaxServices.removeDebitEntryForImprovement(improvementEnrolmentEvaluation);
    }

    @Override
    public boolean isToPayTuition(final Registration registration, final ExecutionYear executionYear) {
        return TuitionServices.isToPayRegistrationTuition(registration, executionYear);
    }

    /* --------------
     * ACADEMIC TAXES
     * --------------
     */

    @Override
    public List<IAcademicTreasuryEvent> getAcademicTaxesList(final Registration registration, final ExecutionYear executionYear) {
        return AcademicTaxServices.findAllTreasuryEventsForAcademicTaxes(registration, executionYear);
    }

    @Override
    public IImprovementTreasuryEvent getImprovementTaxTreasuryEvent(Registration registration, ExecutionYear executionYear) {
        if (!AcademicTreasuryEvent.findUniqueForImprovementTuition(registration, executionYear).isPresent()) {
            return null;
        }

        return AcademicTreasuryEvent.findUniqueForImprovementTuition(registration, executionYear).get();
    }

    // @formatter:off
    /* ------------------------
     * ACADEMIC TREASURY TARGET
     * ------------------------
     */
    // @formatter:on

    @Override
    public IAcademicTreasuryEvent getAcademicTreasuryEventForTarget(final IAcademicTreasuryTarget target) {
        final Person person = target.getAcademicTreasuryTargetPerson();
        return AcademicTreasuryEvent.findUniqueForTarget(person, target).orElse(null);
    }

    @Override
    public void anullDebtsForTarget(final IAcademicTreasuryTarget target, final String reason) {
        final IAcademicTreasuryEvent event = getAcademicTreasuryEventForTarget(target);

        if (event != null) {
            event.annulDebts(reason);
        }
    }

    @Override
    // TODO: Use AcademicTreasuryTargetCreateDebtBuilder class
    @Deprecated
    public IAcademicTreasuryEvent createDebt(final ITreasuryEntity treasuryEntity, final ITreasuryProduct treasuryProduct,
            final IAcademicTreasuryTarget target, final LocalDate when, final boolean createPaymentCode,
            final IPaymentCodePool paymentCodePool, final int numberOfUnits, final int numberOfPages) {

        final FinantialEntity finantialEntity = ((TreasuryEntity) treasuryEntity).finantialEntity;
        final Product product = ((AcademicProduct) treasuryProduct).product;
        final DigitalPaymentPlatform platform = ((PaymentCodePoolImpl) paymentCodePool).digitalPaymentPlatform;

        return createDebt(finantialEntity, product, target, when, createPaymentCode, platform, numberOfUnits, numberOfPages);
    }

    // TODO: Use AcademicTreasuryTargetCreateDebtBuilder class
    @Deprecated
    public IAcademicTreasuryEvent createDebt(final FinantialEntity finantialEntity, final Product product,
            final IAcademicTreasuryTarget target, final LocalDate when, final boolean createPaymentCode,
            final DigitalPaymentPlatform platform, final int numberOfUnits, final int numberOfPages) {

        IAcademicTreasuryPlatformDependentServices implementation =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        final FinantialInstitution finantialInstitution = finantialEntity.getFinantialInstitution();
        final DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get();
        final DateTime now = new DateTime();
        final Vat vat = Vat.findActiveUnique(product.getVatType(), finantialInstitution, when.toDateTimeAtStartOfDay()).get();
        final AdministrativeOffice administrativeOffice = finantialEntity.getAdministrativeOffice();
        final Person person = target.getAcademicTreasuryTargetPerson();

        PersonCustomer personCustomer = implementation.personCustomer(person);
        if (personCustomer == null) {
            personCustomer = PersonCustomer.createWithCurrentFiscalInformation(person);
        }

        DebtAccount debtAccount = DebtAccount.findUnique(finantialInstitution, personCustomer).orElse(null);
        if (debtAccount == null) {
            debtAccount = DebtAccount.create(finantialInstitution, personCustomer);
        }

        AcademicTreasuryEvent treasuryEvent = (AcademicTreasuryEvent) getAcademicTreasuryEventForTarget(target);

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

        final LocalDate dueDate = academicTariff.dueDate(when);
        final DebitNote debitNote = DebitNote.create(debtAccount, documentNumberSeries, now);

        final BigDecimal amount = academicTariff.amountToPay(numberOfUnits, numberOfPages);
        final DebitEntry debitEntry = DebitEntry.create(Optional.of(debitNote), debtAccount, treasuryEvent, vat, amount, dueDate,
                target.getAcademicTreasuryTargetPropertiesMap(), product,
                target.getAcademicTreasuryTargetDescription().getContent(TreasuryConstants.DEFAULT_LANGUAGE), BigDecimal.ONE,
                academicTariff.getInterestRate(), when.toDateTimeAtStartOfDay());

        if (createPaymentCode) {
            createPaymentReferenceCode(platform, debitEntry, when);
        }

        return treasuryEvent;
    }

    @Override
    // TODO: Use AcademicTreasuryTargetCreateDebtBuilder class
    @Deprecated
    public IAcademicTreasuryEvent createDebt(final ITreasuryEntity treasuryEntity, final ITreasuryProduct treasuryProduct,
            final IAcademicTreasuryTarget target, final BigDecimal amount, final LocalDate when, final LocalDate dueDate,
            final boolean createPaymentCode, final IPaymentCodePool paymentCodePool) {
        IAcademicTreasuryPlatformDependentServices implementation =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        final FinantialInstitution finantialInstitution =
                ((TreasuryEntity) treasuryEntity).finantialEntity.getFinantialInstitution();
        final DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get();
        final DateTime now = new DateTime();
        final Product product = ((AcademicProduct) treasuryProduct).product;
        final Vat vat = Vat.findActiveUnique(product.getVatType(), finantialInstitution, when.toDateTimeAtStartOfDay()).get();
        final DigitalPaymentPlatform platform = ((PaymentCodePoolImpl) paymentCodePool).digitalPaymentPlatform;
        final Person person = target.getAcademicTreasuryTargetPerson();

        PersonCustomer personCustomer = implementation.personCustomer(person);
        if (personCustomer == null) {
            personCustomer = PersonCustomer.createWithCurrentFiscalInformation(person);
        }

        DebtAccount debtAccount = DebtAccount.findUnique(finantialInstitution, personCustomer).orElse(null);
        if (debtAccount == null) {
            debtAccount = DebtAccount.create(finantialInstitution, personCustomer);
        }

        AcademicTreasuryEvent treasuryEvent = (AcademicTreasuryEvent) getAcademicTreasuryEventForTarget(target);

        if (treasuryEvent == null) {
            treasuryEvent = AcademicTreasuryEvent.createForAcademicTreasuryEventTarget(product, target);
        }

        final DebitNote debitNote = DebitNote.create(debtAccount, documentNumberSeries, now);
        final DebitEntry debitEntry = DebitEntry.create(Optional.of(debitNote), debtAccount, treasuryEvent, vat, amount, dueDate,
                target.getAcademicTreasuryTargetPropertiesMap(), product,
                target.getAcademicTreasuryTargetDescription().getContent(TreasuryConstants.DEFAULT_LANGUAGE), BigDecimal.ONE,
                null, when.toDateTimeAtStartOfDay());

        if (createPaymentCode) {
            createPaymentReferenceCode(platform, debitEntry, when);
        }

        return treasuryEvent;
    }

    // TODO: Use AcademicTreasuryTargetCreateDebtBuilder class
    @Deprecated
    public IAcademicTreasuryEvent createDebt(final FinantialEntity finantialEntity, final Product product,
            final IAcademicTreasuryTarget target, final BigDecimal amount, final LocalDate when, final LocalDate dueDate,
            final boolean createPaymentCode, final DigitalPaymentPlatform platform) {
        IAcademicTreasuryPlatformDependentServices implementation =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        final FinantialInstitution finantialInstitution = finantialEntity.getFinantialInstitution();
        final DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get();
        final DateTime now = new DateTime();
        final Vat vat = Vat.findActiveUnique(product.getVatType(), finantialInstitution, when.toDateTimeAtStartOfDay()).get();
        final Person person = target.getAcademicTreasuryTargetPerson();

        PersonCustomer personCustomer = implementation.personCustomer(person);
        if (personCustomer == null) {
            personCustomer = PersonCustomer.createWithCurrentFiscalInformation(person);
        }

        DebtAccount debtAccount = DebtAccount.findUnique(finantialInstitution, personCustomer).orElse(null);
        if (debtAccount == null) {
            debtAccount = DebtAccount.create(finantialInstitution, personCustomer);
        }

        AcademicTreasuryEvent treasuryEvent = (AcademicTreasuryEvent) getAcademicTreasuryEventForTarget(target);

        if (treasuryEvent == null) {
            treasuryEvent = AcademicTreasuryEvent.createForAcademicTreasuryEventTarget(product, target);
        }

        final DebitNote debitNote = DebitNote.create(debtAccount, documentNumberSeries, now);
        final DebitEntry debitEntry = DebitEntry.create(Optional.of(debitNote), debtAccount, treasuryEvent, vat, amount, dueDate,
                target.getAcademicTreasuryTargetPropertiesMap(), product,
                target.getAcademicTreasuryTargetDescription().getContent(TreasuryConstants.DEFAULT_LANGUAGE), BigDecimal.ONE,
                null, when.toDateTimeAtStartOfDay());

        if (createPaymentCode) {
            createPaymentReferenceCode(platform, debitEntry, when);
        }

        return treasuryEvent;
    }

    private SibsPaymentRequest createPaymentReferenceCode(final DigitalPaymentPlatform platform, DebitEntry debitEntry,
            LocalDate when) {
        return platform.castToSibsPaymentCodePoolService().createSibsPaymentRequest(debitEntry.getDebtAccount(),
                Collections.singleton(debitEntry), Collections.emptySet());
    }

    /* --------------
     * ACADEMICAL ACT
     * --------------
     */

    @Override
    public boolean isAcademicalActsBlocked(final Person person, final LocalDate when) {
        return PersonServices.isAcademicalActsBlocked(person, when);
    }

    @Override
    public boolean isAcademicalActBlockingSuspended(final Person person, final LocalDate when) {
        return AcademicActBlockingSuspension.isBlockingSuspended(person, when);
    }

    /* -----
     * OTHER
     * -----
     */

    @Override
    public List<IAcademicTreasuryEvent> getAllAcademicTreasuryEventsList(final Person person) {
        return AcademicTreasuryEvent.find(person).collect(Collectors.<IAcademicTreasuryEvent> toList());
    }

    @Override
    public String getPersonAccountTreasuryManagementURL(final Person person) {
        return AcademicTreasurySettings.getInstance().getAcademicTreasuryAccountUrl()
                .getPersonAccountTreasuryManagementURL(person);
    }

    @Override
    public boolean isPersonAccountTreasuryManagementAvailable(Person person) {
        final String addressCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);
        return PersonCustomer.findUnique(person, addressCountryCode, fiscalNumber).isPresent();
    }

    @Override
    public String getRegistrationAccountTreasuryManagementURL(Registration registration) {
        return AcademicTreasurySettings.getInstance().getAcademicTreasuryAccountUrl()
                .getRegistrationAccountTreasuryManagementURL(registration);
    }

    @Override
    public void createAcademicDebts(final Registration registration) {
        AcademicDebtGenerationRule.runAllActiveForRegistration(registration, true);
    }

    // TODO After the virtual payments going to production, replace Bennu Signals  with settlement note handlers
    // The settlement creation is now in SettlementNote.processSettlementNoteCreation(SettlementNoteBean) except 
    // when TreasurySettings.isRestrictPaymentMixingLegacyInvoices==true . 
    // This handle could be called in SettlementNote.processSettlementNoteCreation(SettlementNoteBean)
    // For now maintain it
    @Subscribe
    public void handle(final DomainObjectEvent<SettlementNote> settlementNoteEvent) {
        final SettlementNote settlementNote = settlementNoteEvent.getInstance();

        // @formatter:off
        /*
         * Check if settlementNote was deleted to avoid process of deleted objects in erp integration.
         * Unfortunately FenixFramework.isDomainObjectValid is throwing the same ClassCastException
         * over settlementNote so is wrapped in try-catch
         */
        // @formatter:on
        boolean toReturn = true;
        try {
            toReturn = !FenixFramework.isDomainObjectValid(settlementNote);
        } catch (Throwable t) {
            toReturn = true;
        }

        if (toReturn) {
            return;
        }

        for (final SettlementEntry s : settlementNote.getSettlemetEntries().collect(Collectors.toSet())) {
            final InvoiceEntry invoiceEntry = s.getInvoiceEntry();

            if (!(invoiceEntry instanceof DebitEntry)) {
                continue;
            }

            final DebitEntry d = (DebitEntry) invoiceEntry;

            if (d.getTreasuryEvent() == null) {
                continue;
            }

            if (!(d.getTreasuryEvent() instanceof AcademicTreasuryEvent)) {
                continue;
            }

            AcademicTreasuryEvent academicTreasuryEvent = (AcademicTreasuryEvent) d.getTreasuryEvent();
            if (!academicTreasuryEvent.isForTreasuryEventTarget()) {
                continue;
            }

            ((IAcademicTreasuryTarget) academicTreasuryEvent.getTreasuryEventTarget()).handleSettlement(academicTreasuryEvent);
        }
    }

    @Override
    public boolean isValidFiscalNumber(final String fiscalAddressCountryCode, final String fiscalNumber) {
        return FiscalCodeValidation.isValidFiscalNumber(fiscalAddressCountryCode, fiscalNumber);
    }

    @Override
    public boolean updateCustomer(final Person person, final String fiscalCountryCode, final String fiscalNumber) {
        return PersonCustomer.switchCustomer(person, fiscalCountryCode, fiscalNumber);
    }

    @Override
    public boolean createCustomerIfMissing(final Person person) {
        final String fiscalCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);

        if (Strings.isNullOrEmpty(fiscalCountryCode) || Strings.isNullOrEmpty(fiscalNumber)) {
            AcademicTreasuryDomainException exception = new AcademicTreasuryDomainException(
                    "error.RegistrationServices.createPersonCustomer.fiscalInformation.required", person.getName());
            logger.warn(exception.getLocalizedMessage());

            throw exception;
        }

        final Optional<? extends PersonCustomer> findUnique = PersonCustomer.findUnique(person, fiscalCountryCode, fiscalNumber);
        if (findUnique.isPresent()) {
            return false;
        }

        PersonCustomer.create(person, fiscalCountryCode, fiscalNumber);

        return true;
    }

    @Override
    public void saveFiscalAddressFieldsFromPersonInActiveCustomer(final Person person) {
        IAcademicTreasuryPlatformDependentServices implementation =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        if (implementation.personCustomer(person) == null) {
            return;
        }

        implementation.personCustomer(person).saveFiscalAddressFieldsFromPersonInCustomer();
    }

    @Override
    public PhysicalAddress createSaftDefaultPhysicalAddress(final Person person) {
        IAcademicTreasuryPlatformDependentServices implementation =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        String unknownAddress =
                AcademicTreasuryConstants.academicTreasuryBundle("label.AcademicTreasuryBridgeImpl.unknown.address");
        PhysicalAddress result = implementation.createPhysicalAddress(person, Country.readDefault(), unknownAddress,
                unknownAddress, "0000-000", unknownAddress);
        result.setValid();

        return result;

    }

    // @formatter:off
    /* ----------------------
     * TREASURY CUSTOMER INFO
     * ----------------------
     */
    // @formatter:on

    @Override
    public ITreasuryCustomer getActiveCustomer(final Person person) {
        PersonCustomer customer = PersonCustomer
                .findUnique(person, PersonCustomer.addressCountryCode(person), PersonCustomer.fiscalNumber(person)).orElse(null);

        if (customer == null) {
            return null;
        }

        return new TreasuryCustomer(customer);
    }

    @Override
    public List<ITreasuryCustomer> getCustomersForFiscalNumber(final Person person, final String fiscalCountry,
            final String fiscalNumber) {
        return PersonCustomer.find(person, fiscalCountry, fiscalNumber).map(pc -> new TreasuryCustomer(pc))
                .collect(Collectors.toList());
    }

    @Override
    public ITreasuryDebtAccount getActiveDebtAccountForRegistration(final Registration registration) {
        IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        Person person = registration.getPerson();

        String addressCountryCode = PersonCustomer.addressCountryCode(person);
        String fiscalNumber = PersonCustomer.fiscalNumber(person);
        PersonCustomer customer = PersonCustomer.findUnique(person, addressCountryCode, fiscalNumber).orElse(null);

        if (customer == null) {
            return null;
        }

        FinantialEntity finantialEntity =
                academicTreasuryServices.finantialEntityOfDegree(registration.getDegree(), new LocalDate());
        
        if(finantialEntity == null) {
            return null;
        }
        
        FinantialInstitution finantialInstitution = finantialEntity.getFinantialInstitution();

        DebtAccount debtAccount = DebtAccount.findUnique(finantialInstitution, customer).orElse(null);

        if (debtAccount == null) {
            return null;
        }

        return new TreasuryDebtAccount(debtAccount);
    }

}
