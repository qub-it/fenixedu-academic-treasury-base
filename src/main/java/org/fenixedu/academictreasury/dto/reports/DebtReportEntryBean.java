/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/> All rights reserved.
 *
 * Redistribution and use in source and binary forms, without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution. * Neither the name of Quorum Born IT nor
 * the names of its contributors may be used to endorse or promote products derived from this software without specific prior
 * written permission. * Universidade de Lisboa and its respective subsidiary Serviços Centrais da Universidade de Lisboa
 * (Departamento de Informática), hereby referred to as the Beneficiary, is the sole demonstrated end-user and ultimately the only
 * beneficiary of the redistributed binary form and/or source code. * The Beneficiary is entrusted with either the binary form,
 * the source code, or both, and by accepting it, accepts the terms of this License. * Redistribution of any binary form and/or
 * source code is only allowed in the scope of the Universidade de Lisboa FenixEdu(™)’s implementation projects. * This license
 * and conditions of redistribution of source code/binary can only be reviewed by the Steering Comittee of FenixEdu(™)
 * <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL “Quorum Born IT” BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.academictreasury.dto.reports;

import static com.qubit.terra.framework.tools.excel.ExcelUtil.createCellWithValue;
import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.EmailAddress;
import org.fenixedu.academic.domain.contacts.PartyContact;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.reports.DebtReportRequest;
import org.fenixedu.academictreasury.domain.reports.ErrorsLog;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.document.AdvancedPaymentCreditNote;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.CreditNote;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporterUtils;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;
import org.fenixedu.treasury.util.streaming.spreadsheet.SpreadsheetRow;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;

public class DebtReportEntryBean implements SpreadsheetRow, IFinantialReportEntryCommonMethods {

    // @formatter:off
    public static String[] SPREADSHEET_DEBIT_HEADERS =
            { academicTreasuryBundle("label.DebtReportEntryBean.header.identification"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.entryType"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.versioningCreator"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.creationDate"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.entryDate"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.dueDate"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.customerId"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.debtAccountId"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.name"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.identificationType"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.identificationNumber"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.vatNumber"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.email"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.personalEmail"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.address"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.studentNumber"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.registrationNumber"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.degreeType"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.degreeCode"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.degreeName"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.executionYear"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.executionSemester"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.productCode"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.invoiceEntryDescription"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.documentNumber"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.documentExportationPending"), "",// Empty header
                    academicTreasuryBundle("label.DebtReportEntryBean.header.amountToPay"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.openAmountToPay"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.openAmountWithInterestToDate"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.pendingInterestAmount"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.payorDebtAcount.vatNumber"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.payorDebtAcount.name"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.agreement"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.ingression"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.firstTimeStudent"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.partialRegime"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.statutes"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.numberOfNormalEnrolments"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.numberOfStandaloneEnrolments"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.numberOfExtracurricularEnrolments"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.tuitionPaymentPlan"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.tuitionPaymentPlanConditions"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.documentAnnuled"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.documentAnnuledReason"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.closeDate"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.openAmountAtERPStartDate"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.exportedInLegacyERP"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.legacyERPCertificateDocumentReference"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.erpCertificationDate"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.erpCertificateDocumentReference"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.originSettlementNoteForAdvancedCredit"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.code") };

    public static String[] SPREADSHEET_CREDIT_HEADERS =
            { academicTreasuryBundle("label.DebtReportEntryBean.header.identification"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.entryType"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.versioningCreator"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.creationDate"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.entryDate"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.dueDate"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.customerId"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.debtAccountId"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.name"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.identificationType"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.identificationNumber"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.vatNumber"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.email"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.personalEmail"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.address"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.studentNumber"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.registrationNumber"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.degreeType"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.degreeCode"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.degreeName"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.executionYear"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.executionSemester"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.productCode"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.invoiceEntryDescription"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.documentNumber"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.documentExportationPending"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.debitEntry.identification"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.amountToCredit"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.openAmountToCredit"), "",// Empty Header
                    "",// Empty Header
                    academicTreasuryBundle("label.DebtReportEntryBean.header.payorDebtAcount.vatNumber"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.payorDebtAcount.name"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.agreement"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.ingression"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.firstTimeStudent"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.partialRegime"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.statutes"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.numberOfNormalEnrolments"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.numberOfStandaloneEnrolments"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.numberOfExtracurricularEnrolments"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.tuitionPaymentPlan"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.tuitionPaymentPlanConditions"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.documentAnnuled"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.documentAnnuledReason"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.closeDate"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.openAmountAtERPStartDate"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.exportedInLegacyERP"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.legacyERPCertificateDocumentReference"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.erpCertificationDate"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.erpCertificateDocumentReference"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.originSettlementNoteForAdvancedCredit"),
                    academicTreasuryBundle("label.DebtReportEntryBean.header.code") };
    // @formatter:on

    private InvoiceEntry invoiceEntry;
    private boolean completed = false;

    private String identification;
    private String code;
    private String entryType;
    private String versioningCreator;
    private DateTime creationDate;
    private DateTime entryDate;
    private LocalDate dueDate;
    private String customerId;
    private String debtAccountId;
    private String name;
    private String identificationType;
    private String identificationNumber;
    private String vatNumber;
    private String institutionalOrDefaultEmail;
    private String emailForSendingEmails;
    private String personalEmail;
    private String address;
    private Integer studentNumber;
    private Integer registrationNumber;
    private String degreeType;
    private String degreeCode;
    private String degreeName;
    private LocalizedString registrationMajorDcpGroupName;
    private LocalizedString registrationMinorDcpGroupName;
    private String executionYear;
    private String executionSemester;
    private String productCode;
    private String invoiceEntryDescription;
    private String documentNumber;
    private Boolean documentExportationPending;
    private Boolean annuled;
    private String annuledReason;
    private String debitEntryIdentification;
    private BigDecimal amountToPay;
    private BigDecimal netExemptedAmount;
    private BigDecimal openAmountToPay;
    private BigDecimal openAmountWithInterestToDate;
    private BigDecimal pendingInterestAmount;
    private String payorDebtAccountVatNumber;
    private String payorDebtAccountName;
    private LocalizedString agreement;
    private LocalizedString ingression;
    private Boolean firstTimeStudent;
    private Boolean partialRegime;
    private String statutes;
    private Integer numberOfNormalEnrolments;
    private Integer numberOfStandaloneEnrolments;
    private Integer numberOfExtracurricularEnrolments;
    private String tuitionPaymentPlan;
    private String tuitionPaymentPlanConditions;

    private DateTime closeDate;
    private BigDecimal openAmountAtERPStartDate;
    private Boolean exportedInLegacyERP;
    private String legacyERPCertificateDocumentReference;

    private LocalDate erpCertificationDate;
    private String erpCertificateDocumentReference;

    private String erpCustomerId;
    private String erpPayorCustomerId;

    private String certifiedDocumentNumber;
    private LocalDate certifiedDocumentDate;

    private String originSettlementNoteForAdvancedCredit;

    private String decimalSeparator;

    private String documentObservations;
    private String documentTermsAndConditions;

    private String finantialEntityCode;
    private LocalizedString finantialEntityName;

    public DebtReportEntryBean(final InvoiceEntry entry, final DebtReportRequest request, final ErrorsLog errorsLog) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        this.decimalSeparator = request != null ? request.getDecimalSeparator() : DebtReportRequest.DOT;
        final Currency currency = entry.getDebtAccount().getFinantialInstitution().getCurrency();

        this.invoiceEntry = entry;

        try {
            this.identification = entry.getExternalId();
            this.code = entry.getCode();
            this.entryType = entryType(entry);
            this.creationDate = treasuryServices.versioningCreationDate(entry);
            this.versioningCreator = treasuryServices.versioningCreatorUsername(entry);
            this.entryDate = entry.getEntryDateTime();
            this.dueDate = entry.getDueDate();

            this.payorDebtAccountVatNumber = "";
            this.payorDebtAccountName = "";

            if (entry.getFinantialDocument() != null && ((Invoice) entry.getFinantialDocument()).getPayorDebtAccount() != null) {
                this.payorDebtAccountVatNumber =
                        ((Invoice) entry.getFinantialDocument()).getPayorDebtAccount().getCustomer().getUiFiscalNumber();
                this.payorDebtAccountName =
                        ((Invoice) entry.getFinantialDocument()).getPayorDebtAccount().getCustomer().getName();
            }

            fillStudentInformation(entry);

            this.productCode = entry.getProduct().getCode();
            this.invoiceEntryDescription = entry.getDescription();

            if (entry.getFinantialDocument() != null) {
                this.documentNumber = entry.getFinantialDocument().getUiDocumentNumber();
                this.documentExportationPending = entry.getFinantialDocument().isDocumentToExport();
                this.documentObservations = entry.getFinantialDocument().getDocumentObservations();
                this.documentTermsAndConditions = entry.getFinantialDocument().getDocumentTermsAndConditions();
            }

            this.annuled = entry.isAnnulled();

            if (this.annuled && entry.getFinantialDocument() != null) {
                this.annuledReason = entry.getFinantialDocument().getAnnulledReason();
            }

            if (entry.isCreditNoteEntry() && ((CreditEntry) entry).getDebitEntry() != null) {
                this.debitEntryIdentification = ((CreditEntry) entry).getDebitEntry().getExternalId();
            }

            this.amountToPay = currency.getValueWithScale(entry.getAmountWithVat());
            this.openAmountToPay = currency.getValueWithScale(entry.getOpenAmount());
            this.openAmountWithInterestToDate = currency.getValueWithScale(entry.getOpenAmountWithInterests());
            this.pendingInterestAmount =
                    currency.getValueWithScale(entry.getOpenAmountWithInterests().subtract(entry.getOpenAmount()));

            this.netExemptedAmount = entry.getNetExemptedAmount();

            if (entry.getFinantialEntity() != null) {
                this.finantialEntityCode = entry.getFinantialEntity().getCode();
                this.finantialEntityName = entry.getFinantialEntity().getName();
            }

            fillERPInformation(entry);

            this.completed = true;
        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(entry, e);
        }

    }

    private void fillERPInformation(final InvoiceEntry entry) {
        ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        final Currency currency = entry.getDebtAccount().getFinantialInstitution().getCurrency();

        this.closeDate = entry.getFinantialDocument() != null ? entry.getFinantialDocument().getCloseDate() : null;
        this.openAmountAtERPStartDate = currency.getValueWithScale(
                SAPExporterUtils.openAmountAtDate((InvoiceEntry) entry, SAPExporter.ERP_INTEGRATION_START_DATE));
        this.exportedInLegacyERP =
                entry.getFinantialDocument() != null ? entry.getFinantialDocument().isExportedInLegacyERP() : false;

        this.legacyERPCertificateDocumentReference = entry.getFinantialDocument() != null ? entry.getFinantialDocument()
                .getLegacyERPCertificateDocumentReference() : null;

        this.erpCertificationDate =
                entry.getFinantialDocument() != null ? entry.getFinantialDocument().getErpCertificationDate() : null;

        this.erpCertificateDocumentReference =
                entry.getFinantialDocument() != null ? entry.getFinantialDocument().getErpCertificateDocumentReference() : null;

        this.erpCustomerId = entry.getDebtAccount().getCustomer().getErpCustomerId();

        if (entry.getFinantialDocument() != null && ((Invoice) entry.getFinantialDocument()).getPayorDebtAccount() != null) {
            this.erpPayorCustomerId =
                    ((Invoice) entry.getFinantialDocument()).getPayorDebtAccount().getCustomer().getErpCustomerId();
        }

        if (entry.getFinantialDocument() != null && entry.getFinantialDocument()
                .isCreditNote() && ((CreditNote) entry.getFinantialDocument()).isAdvancePayment()) {
            final AdvancedPaymentCreditNote advancedCreditNote = (AdvancedPaymentCreditNote) entry.getFinantialDocument();
            this.originSettlementNoteForAdvancedCredit =
                    advancedCreditNote.getAdvancedPaymentSettlementNote() != null ? advancedCreditNote.getAdvancedPaymentSettlementNote()
                            .getUiDocumentNumber() : "";
        }

        if (entry.getFinantialDocument() != null && treasuryServices.hasCertifiedDocument(entry.getFinantialDocument())) {
            this.erpCertificationDate = treasuryServices.getCertifiedDocumentDate(entry.getFinantialDocument());
            this.erpCertificateDocumentReference = treasuryServices.getCertifiedDocumentNumber(entry.getFinantialDocument());
        }

        if (entry.getFinantialDocument() != null && treasuryServices.hasCertifiedDocument(entry.getFinantialDocument())) {
            this.certifiedDocumentDate = treasuryServices.getCertifiedDocumentDate(entry.getFinantialDocument());
            this.certifiedDocumentNumber = treasuryServices.getCertifiedDocumentNumber(entry.getFinantialDocument());
        }
    }

    private void fillStudentInformation(final InvoiceEntry entry) {
        final Customer customer = entry.getDebtAccount().getCustomer();

        this.customerId = customer.getExternalId();
        this.debtAccountId = entry.getDebtAccount().getExternalId();

        this.name = customer.getName();

        if (customer.isPersonCustomer() && ((PersonCustomer) customer).getAssociatedPerson() != null && ((PersonCustomer) customer).getAssociatedPerson()
                .getIdDocumentType() != null) {
            this.identificationType = ((PersonCustomer) customer).getAssociatedPerson().getIdDocumentType().getLocalizedName();
        }

        this.identificationNumber = customer.getIdentificationNumber();
        this.vatNumber = customer.getUiFiscalNumber();

        if (customer.isPersonCustomer() && ((PersonCustomer) customer).getAssociatedPerson() != null) {
            final Person person = ((PersonCustomer) customer).getAssociatedPerson();
            this.institutionalOrDefaultEmail = person.getInstitutionalOrDefaultEmailAddressValue();
            this.emailForSendingEmails = person.getEmailForSendingEmails();
            this.personalEmail = personalEmail(person) != null ? personalEmail(person).getValue() : "";
        }

        this.address = customer.getUiCompleteAddress();

        if (customer.isPersonCustomer() && ((PersonCustomer) customer).getAssociatedPerson() != null && ((PersonCustomer) customer).getAssociatedPerson()
                .getStudent() != null) {
            this.studentNumber = ((PersonCustomer) customer).getAssociatedPerson().getStudent().getNumber();
        }

        fillAcademicInformation(entry);
    }

    static EmailAddress personalEmail(final Person person) {
        return pendingOrValidPartyContacts(person, EmailAddress.class) //
                .stream() //
                .map(EmailAddress.class::cast) //
                .filter(e -> Boolean.TRUE.equals(e.getActive())) //
                .filter(e -> e.getType() == PartyContactType.PERSONAL) //

                // ANIL 2025-01-14 (#qubIT-Fenix-6541)
                //
                // Exclude personal emails which the property currentPartyContact is not null
                // This rule is in conformity with
                // com.qubit.qubEdu.module.base.presentation.uilayer.components.partyContact.PartyContactsManagerComponentImpl.getPartyContacts

                .filter(e -> e.getCurrentPartyContact() == null) //
                .findFirst() //
                .orElse(null);
    }

    private static List<? extends PartyContact> pendingOrValidPartyContacts(Person person,
            Class<? extends PartyContact> partyContactType) {
        Comparator<? super PartyContact> comparator = (o1, o2) -> {
            if (o1.isValid() && !o2.isValid()) {
                return -1;
            } else if (!o1.isValid() && o2.isValid()) {
                return 1;
            }

            return o1.getExternalId().compareTo(o2.getExternalId());
        };

        return person.getAllPartyContacts(partyContactType).stream().sorted(comparator).collect(Collectors.toList());
    }

    private String entryType(final InvoiceEntry entry) {
        if (entry.isDebitNoteEntry()) {
            return academicTreasuryBundle("label.DebtReportEntryBean.debitNoteEntry");
        } else if (entry.isCreditNoteEntry()) {
            return academicTreasuryBundle("label.DebtReportEntryBean.creditNoteEntry");
        }

        return null;
    }

    @Override
    public void writeCellValues(final Row row, final IErrorsLog ierrorsLog) {
        final ErrorsLog errorsLog = (ErrorsLog) ierrorsLog;

        try {
            createCellWithValue(row, 0, valueOrEmpty(identification));

            if (!completed) {
                createCellWithValue(row, 1, academicTreasuryBundle("error.DebtReportEntryBean.report.generation.verify.entry"));
                return;
            }

            if (invoiceEntry.isDebitNoteEntry()) {
                int i = 1;

                createCellWithValue(row, i++, valueOrEmpty(entryType));
                createCellWithValue(row, i++, valueOrEmpty(versioningCreator));
                createCellWithValue(row, i++, valueOrEmpty(creationDate));
                createCellWithValue(row, i++, entryDate.toString(AcademicTreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD));
                createCellWithValue(row, i++, dueDate.toString(AcademicTreasuryConstants.DATE_FORMAT_YYYY_MM_DD));
                createCellWithValue(row, i++, valueOrEmpty(customerId));
                createCellWithValue(row, i++, valueOrEmpty(debtAccountId));
                createCellWithValue(row, i++, valueOrEmpty(name));
                createCellWithValue(row, i++, valueOrEmpty(identificationType));
                createCellWithValue(row, i++, valueOrEmpty(identificationNumber));
                createCellWithValue(row, i++, valueOrEmpty(vatNumber));
                createCellWithValue(row, i++, valueOrEmpty(institutionalOrDefaultEmail));
                createCellWithValue(row, i++, valueOrEmpty(this.personalEmail));
                createCellWithValue(row, i++, valueOrEmpty(address));
                createCellWithValue(row, i++, valueOrEmpty(studentNumber));
                createCellWithValue(row, i++, valueOrEmpty(registrationNumber));
                createCellWithValue(row, i++, valueOrEmpty(degreeType));
                createCellWithValue(row, i++, valueOrEmpty(degreeCode));
                createCellWithValue(row, i++, valueOrEmpty(degreeName));
                createCellWithValue(row, i++, valueOrEmpty(executionYear));
                createCellWithValue(row, i++, valueOrEmpty(executionSemester));
                createCellWithValue(row, i++, valueOrEmpty(productCode));
                createCellWithValue(row, i++, valueOrEmpty(invoiceEntryDescription));
                createCellWithValue(row, i++, valueOrEmpty(documentNumber));
                createCellWithValue(row, i++, valueOrEmpty(documentExportationPending));
                i++;

                {
                    String value = amountToPay != null ? amountToPay.toString() : "";
                    if (DebtReportRequest.COMMA.equals(decimalSeparator)) {
                        value = value.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
                    }
                    createCellWithValue(row, i++, valueOrEmpty(value));
                }

                {
                    String value = openAmountToPay != null ? openAmountToPay.toString() : "";
                    if (DebtReportRequest.COMMA.equals(decimalSeparator)) {
                        value = value.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
                    }
                    createCellWithValue(row, i++, valueOrEmpty(value));
                }

                {
                    String value = openAmountWithInterestToDate != null ? openAmountWithInterestToDate.toString() : "";
                    if (DebtReportRequest.COMMA.equals(decimalSeparator)) {
                        value = value.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
                    }
                    createCellWithValue(row, i++, valueOrEmpty(value));
                }

                {
                    String value = pendingInterestAmount != null ? pendingInterestAmount.toString() : "0";
                    if (DebtReportRequest.COMMA.equals(decimalSeparator)) {
                        value = value.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
                    }
                    createCellWithValue(row, i++, valueOrEmpty(value));
                }

                createCellWithValue(row, i++, valueOrEmpty(payorDebtAccountVatNumber));
                createCellWithValue(row, i++, valueOrEmpty(payorDebtAccountName));
                createCellWithValue(row, i++, valueOrEmpty(agreement));
                createCellWithValue(row, i++, valueOrEmpty(ingression));
                createCellWithValue(row, i++, valueOrEmpty(firstTimeStudent));
                createCellWithValue(row, i++, valueOrEmpty(partialRegime));
                createCellWithValue(row, i++, valueOrEmpty(statutes));
                createCellWithValue(row, i++, valueOrEmpty(numberOfNormalEnrolments));
                createCellWithValue(row, i++, valueOrEmpty(numberOfStandaloneEnrolments));
                createCellWithValue(row, i++, valueOrEmpty(numberOfExtracurricularEnrolments));
                createCellWithValue(row, i++, valueOrEmpty(tuitionPaymentPlan));
                createCellWithValue(row, i++, valueOrEmpty(tuitionPaymentPlanConditions));
                createCellWithValue(row, i++, valueOrEmpty(annuled));
                createCellWithValue(row, i++, valueOrEmpty(annuledReason));
                createCellWithValue(row, i++, valueOrEmpty(closeDate));

                {
                    String value = openAmountAtERPStartDate != null ? openAmountAtERPStartDate.toString() : "";
                    if (DebtReportRequest.COMMA.equals(decimalSeparator)) {
                        value = value.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
                    }
                    createCellWithValue(row, i++, valueOrEmpty(value));
                }

                createCellWithValue(row, i++, valueOrEmpty(exportedInLegacyERP));
                createCellWithValue(row, i++, valueOrEmpty(legacyERPCertificateDocumentReference));
                createCellWithValue(row, i++, valueOrEmpty(erpCertificationDate));
                createCellWithValue(row, i++, valueOrEmpty(erpCertificateDocumentReference));
                createCellWithValue(row, i++, valueOrEmpty(originSettlementNoteForAdvancedCredit));
                createCellWithValue(row, i++, valueOrEmpty(this.code));

            } else if (invoiceEntry.isCreditNoteEntry()) {
                int i = 1;
                createCellWithValue(row, i++, valueOrEmpty(entryType));
                createCellWithValue(row, i++, valueOrEmpty(versioningCreator));
                createCellWithValue(row, i++, valueOrEmpty(creationDate));
                createCellWithValue(row, i++, entryDate.toString(AcademicTreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD));
                createCellWithValue(row, i++, dueDate.toString(AcademicTreasuryConstants.DATE_FORMAT_YYYY_MM_DD));
                createCellWithValue(row, i++, valueOrEmpty(customerId));
                createCellWithValue(row, i++, valueOrEmpty(debtAccountId));
                createCellWithValue(row, i++, valueOrEmpty(name));
                createCellWithValue(row, i++, valueOrEmpty(identificationType));
                createCellWithValue(row, i++, valueOrEmpty(identificationNumber));
                createCellWithValue(row, i++, valueOrEmpty(vatNumber));
                createCellWithValue(row, i++, valueOrEmpty(institutionalOrDefaultEmail));
                createCellWithValue(row, i++, valueOrEmpty(this.personalEmail));
                createCellWithValue(row, i++, valueOrEmpty(address));
                createCellWithValue(row, i++, valueOrEmpty(studentNumber));
                createCellWithValue(row, i++, valueOrEmpty(registrationNumber));
                createCellWithValue(row, i++, valueOrEmpty(degreeType));
                createCellWithValue(row, i++, valueOrEmpty(degreeCode));
                createCellWithValue(row, i++, valueOrEmpty(degreeName));
                createCellWithValue(row, i++, valueOrEmpty(executionYear));
                createCellWithValue(row, i++, valueOrEmpty(executionSemester));
                createCellWithValue(row, i++, valueOrEmpty(productCode));
                createCellWithValue(row, i++, valueOrEmpty(invoiceEntryDescription));
                createCellWithValue(row, i++, valueOrEmpty(documentNumber));
                createCellWithValue(row, i++, valueOrEmpty(documentExportationPending));
                createCellWithValue(row, i++, valueOrEmpty(debitEntryIdentification));

                {
                    String value = amountToPay != null ? amountToPay.toString() : "";
                    if (DebtReportRequest.COMMA.equals(decimalSeparator)) {
                        value = value.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
                    }
                    createCellWithValue(row, i++, valueOrEmpty(value));
                }

                {
                    String value = openAmountToPay != null ? openAmountToPay.toString() : "";
                    if (DebtReportRequest.COMMA.equals(decimalSeparator)) {
                        value = value.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
                    }
                    createCellWithValue(row, i++, valueOrEmpty(value));
                }

                createCellWithValue(row, i++, "");
                createCellWithValue(row, i++, "");

                createCellWithValue(row, i++, valueOrEmpty(payorDebtAccountVatNumber));
                createCellWithValue(row, i++, valueOrEmpty(payorDebtAccountName));
                createCellWithValue(row, i++, valueOrEmpty(agreement));
                createCellWithValue(row, i++, valueOrEmpty(ingression));
                createCellWithValue(row, i++, valueOrEmpty(firstTimeStudent));
                createCellWithValue(row, i++, valueOrEmpty(partialRegime));
                createCellWithValue(row, i++, valueOrEmpty(statutes));
                createCellWithValue(row, i++, valueOrEmpty(numberOfNormalEnrolments));
                createCellWithValue(row, i++, valueOrEmpty(numberOfStandaloneEnrolments));
                createCellWithValue(row, i++, valueOrEmpty(numberOfExtracurricularEnrolments));
                createCellWithValue(row, i++, valueOrEmpty(tuitionPaymentPlan));
                createCellWithValue(row, i++, valueOrEmpty(tuitionPaymentPlanConditions));
                createCellWithValue(row, i++, valueOrEmpty(annuled));
                createCellWithValue(row, i++, valueOrEmpty(annuledReason));
                createCellWithValue(row, i++, valueOrEmpty(closeDate));

                {
                    String value = openAmountAtERPStartDate != null ? openAmountAtERPStartDate.toString() : "";
                    if (DebtReportRequest.COMMA.equals(decimalSeparator)) {
                        value = value.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
                    }
                    createCellWithValue(row, i++, valueOrEmpty(value));
                }

                createCellWithValue(row, i++, valueOrEmpty(exportedInLegacyERP));
                createCellWithValue(row, i++, valueOrEmpty(legacyERPCertificateDocumentReference));
                createCellWithValue(row, i++, valueOrEmpty(erpCertificationDate));
                createCellWithValue(row, i++, valueOrEmpty(erpCertificateDocumentReference));
                createCellWithValue(row, i++, valueOrEmpty(originSettlementNoteForAdvancedCredit));
                createCellWithValue(row, i++, valueOrEmpty(this.code));

            }

        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(invoiceEntry, e);
        }
    }

    private String valueOrEmpty(final LocalDate value) {
        if (value == null) {
            return "";
        }

        return value.toString(AcademicTreasuryConstants.DATE_FORMAT_YYYY_MM_DD);
    }

    private String valueOrEmpty(final DateTime value) {
        if (value == null) {
            return "";
        }

        return value.toString(AcademicTreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD);
    }

    private String valueOrEmpty(final Boolean value) {
        if (value == null) {
            return "";
        }

        return academicTreasuryBundle(value ? "label.yes" : "label.no");
    }

    private String valueOrEmpty(final Integer value) {
        if (value == null) {
            return null;
        }

        return value.toString();
    }

    private String valueOrEmpty(final LocalizedString value) {
        if (value == null) {
            return "";
        }

        if (Strings.isNullOrEmpty(value.getContent())) {
            return "";
        }

        return value.getContent();
    }

    private String valueOrEmpty(final String value) {
        if (!Strings.isNullOrEmpty(value)) {
            return value;
        }

        return "";
    }

    /* ******************************************************
    /* COMPUTATIONS TO MAKE EASY THE BUILDING OF THE REPORT */
    /* ******************************************************/

    public DebitEntry getAssociatedDebitEntry() {
        return this.invoiceEntry.isDebitNoteEntry() ? (DebitEntry) this.invoiceEntry : null;
    }

    public CreditEntry getAssociatedCreditEntry() {
        return this.invoiceEntry.isCreditNoteEntry() ? (CreditEntry) this.invoiceEntry : null;
    }

    public Person getAssociatedPerson() {
        if (!this.invoiceEntry.getDebtAccount().getCustomer().isPersonCustomer()) {
            return null;
        }

        PersonCustomer personCustomer = (PersonCustomer) this.invoiceEntry.getDebtAccount().getCustomer();

        return personCustomer.getAssociatedPerson();
    }

    // @formatter:off
    /* *****************
     * GETTERS & SETTERS
     * *****************
     */
    // @formatter:on

    public InvoiceEntry getInvoiceEntry() {
        return invoiceEntry;
    }

    public void setInvoiceEntry(InvoiceEntry invoiceEntry) {
        this.invoiceEntry = invoiceEntry;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getIdentification() {
        return identification;
    }

    public void setIdentification(String identification) {
        this.identification = identification;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public String getVersioningCreator() {
        return versioningCreator;
    }

    public void setVersioningCreator(String versioningCreator) {
        this.versioningCreator = versioningCreator;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(DateTime creationDate) {
        this.creationDate = creationDate;
    }

    public DateTime getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(DateTime entryDate) {
        this.entryDate = entryDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getDebtAccountId() {
        return debtAccountId;
    }

    public void setDebtAccountId(String debtAccountId) {
        this.debtAccountId = debtAccountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentificationType() {
        return identificationType;
    }

    public void setIdentificationType(String identificationType) {
        this.identificationType = identificationType;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }

    public void setIdentificationNumber(String identificationNumber) {
        this.identificationNumber = identificationNumber;
    }

    public String getVatNumber() {
        return vatNumber;
    }

    public void setVatNumber(String vatNumber) {
        this.vatNumber = vatNumber;
    }

    public String getInstitutionalOrDefaultEmail() {
        return institutionalOrDefaultEmail;
    }

    public void setInstitutionalOrDefaultEmail(String institutionalOrDefaultEmail) {
        this.institutionalOrDefaultEmail = institutionalOrDefaultEmail;
    }

    public String getEmailForSendingEmails() {
        return emailForSendingEmails;
    }

    public void setEmailForSendingEmails(String emailForSendingEmails) {
        this.emailForSendingEmails = emailForSendingEmails;
    }

    public String getPersonalEmail() {
        return personalEmail;
    }

    public void setPersonalEmail(String personalEmail) {
        this.personalEmail = personalEmail;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(Integer studentNumber) {
        this.studentNumber = studentNumber;
    }

    public Integer getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(Integer registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getDegreeType() {
        return degreeType;
    }

    public void setDegreeType(String degreeType) {
        this.degreeType = degreeType;
    }

    public String getDegreeCode() {
        return degreeCode;
    }

    public void setDegreeCode(String degreeCode) {
        this.degreeCode = degreeCode;
    }

    public String getDegreeName() {
        return degreeName;
    }

    public void setDegreeName(String degreeName) {
        this.degreeName = degreeName;
    }

    public String getExecutionYear() {
        return executionYear;
    }

    public void setExecutionYear(String executionYear) {
        this.executionYear = executionYear;
    }

    public String getExecutionSemester() {
        return executionSemester;
    }

    public void setExecutionSemester(String executionSemester) {
        this.executionSemester = executionSemester;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getInvoiceEntryDescription() {
        return invoiceEntryDescription;
    }

    public void setInvoiceEntryDescription(String invoiceEntryDescription) {
        this.invoiceEntryDescription = invoiceEntryDescription;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public Boolean getDocumentExportationPending() {
        return documentExportationPending;
    }

    public void setDocumentExportationPending(Boolean documentExportationPending) {
        this.documentExportationPending = documentExportationPending;
    }

    public Boolean getAnnuled() {
        return annuled;
    }

    public void setAnnuled(Boolean annuled) {
        this.annuled = annuled;
    }

    public String getAnnuledReason() {
        return annuledReason;
    }

    public void setAnnuledReason(String annuledReason) {
        this.annuledReason = annuledReason;
    }

    public String getDebitEntryIdentification() {
        return debitEntryIdentification;
    }

    public void setDebitEntryIdentification(String debitEntryIdentification) {
        this.debitEntryIdentification = debitEntryIdentification;
    }

    public BigDecimal getAmountToPay() {
        return amountToPay;
    }

    public void setAmountToPay(BigDecimal amountToPay) {
        this.amountToPay = amountToPay;
    }

    public BigDecimal getNetExemptedAmount() {
        return netExemptedAmount;
    }

    public void setNetExemptedAmount(BigDecimal netExemptedAmount) {
        this.netExemptedAmount = netExemptedAmount;
    }

    public BigDecimal getOpenAmountToPay() {
        return openAmountToPay;
    }

    public void setOpenAmountToPay(BigDecimal openAmountToPay) {
        this.openAmountToPay = openAmountToPay;
    }

    public BigDecimal getOpenAmountWithInterestToDate() {
        return openAmountWithInterestToDate;
    }

    public void setOpenAmountWithInterestToDate(BigDecimal openAmountWithInterestToDate) {
        this.openAmountWithInterestToDate = openAmountWithInterestToDate;
    }

    public BigDecimal getPendingInterestAmount() {
        return pendingInterestAmount;
    }

    public void setPendingInterestAmount(BigDecimal pendingInterestAmount) {
        this.pendingInterestAmount = pendingInterestAmount;
    }

    public String getPayorDebtAccountVatNumber() {
        return payorDebtAccountVatNumber;
    }

    public void setPayorDebtAccountVatNumber(String payorDebtAccountVatNumber) {
        this.payorDebtAccountVatNumber = payorDebtAccountVatNumber;
    }

    public String getPayorDebtAccountName() {
        return payorDebtAccountName;
    }

    public void setPayorDebtAccountName(String payorDebtAccountName) {
        this.payorDebtAccountName = payorDebtAccountName;
    }

    public LocalizedString getAgreement() {
        return agreement;
    }

    public void setAgreement(LocalizedString agreement) {
        this.agreement = agreement;
    }

    public LocalizedString getIngression() {
        return ingression;
    }

    public void setIngression(LocalizedString ingression) {
        this.ingression = ingression;
    }

    public Boolean getFirstTimeStudent() {
        return firstTimeStudent;
    }

    public void setFirstTimeStudent(Boolean firstTimeStudent) {
        this.firstTimeStudent = firstTimeStudent;
    }

    public Boolean getPartialRegime() {
        return partialRegime;
    }

    public void setPartialRegime(Boolean partialRegime) {
        this.partialRegime = partialRegime;
    }

    public String getStatutes() {
        return statutes;
    }

    public void setStatutes(String statutes) {
        this.statutes = statutes;
    }

    public Integer getNumberOfNormalEnrolments() {
        return numberOfNormalEnrolments;
    }

    public void setNumberOfNormalEnrolments(Integer numberOfNormalEnrolments) {
        this.numberOfNormalEnrolments = numberOfNormalEnrolments;
    }

    public Integer getNumberOfStandaloneEnrolments() {
        return numberOfStandaloneEnrolments;
    }

    public void setNumberOfStandaloneEnrolments(Integer numberOfStandaloneEnrolments) {
        this.numberOfStandaloneEnrolments = numberOfStandaloneEnrolments;
    }

    public Integer getNumberOfExtracurricularEnrolments() {
        return numberOfExtracurricularEnrolments;
    }

    public void setNumberOfExtracurricularEnrolments(Integer numberOfExtracurricularEnrolments) {
        this.numberOfExtracurricularEnrolments = numberOfExtracurricularEnrolments;
    }

    public String getTuitionPaymentPlan() {
        return tuitionPaymentPlan;
    }

    public void setTuitionPaymentPlan(String tuitionPaymentPlan) {
        this.tuitionPaymentPlan = tuitionPaymentPlan;
    }

    public String getTuitionPaymentPlanConditions() {
        return tuitionPaymentPlanConditions;
    }

    public void setTuitionPaymentPlanConditions(String tuitionPaymentPlanConditions) {
        this.tuitionPaymentPlanConditions = tuitionPaymentPlanConditions;
    }

    public DateTime getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(DateTime closeDate) {
        this.closeDate = closeDate;
    }

    public BigDecimal getOpenAmountAtERPStartDate() {
        return openAmountAtERPStartDate;
    }

    public void setOpenAmountAtERPStartDate(BigDecimal openAmountAtERPStartDate) {
        this.openAmountAtERPStartDate = openAmountAtERPStartDate;
    }

    public Boolean getExportedInLegacyERP() {
        return exportedInLegacyERP;
    }

    public void setExportedInLegacyERP(Boolean exportedInLegacyERP) {
        this.exportedInLegacyERP = exportedInLegacyERP;
    }

    public String getLegacyERPCertificateDocumentReference() {
        return legacyERPCertificateDocumentReference;
    }

    public void setLegacyERPCertificateDocumentReference(String legacyERPCertificateDocumentReference) {
        this.legacyERPCertificateDocumentReference = legacyERPCertificateDocumentReference;
    }

    public LocalDate getErpCertificationDate() {
        return erpCertificationDate;
    }

    public void setErpCertificationDate(LocalDate erpCertificationDate) {
        this.erpCertificationDate = erpCertificationDate;
    }

    public String getErpCertificateDocumentReference() {
        return erpCertificateDocumentReference;
    }

    public void setErpCertificateDocumentReference(String erpCertificateDocumentReference) {
        this.erpCertificateDocumentReference = erpCertificateDocumentReference;
    }

    public String getErpCustomerId() {
        return erpCustomerId;
    }

    public void setErpCustomerId(String erpCustomerId) {
        this.erpCustomerId = erpCustomerId;
    }

    public String getErpPayorCustomerId() {
        return erpPayorCustomerId;
    }

    public void setErpPayorCustomerId(String erpPayorCustomerId) {
        this.erpPayorCustomerId = erpPayorCustomerId;
    }

    public String getCertifiedDocumentNumber() {
        return certifiedDocumentNumber;
    }

    public void setCertifiedDocumentNumber(String certifiedDocumentNumber) {
        this.certifiedDocumentNumber = certifiedDocumentNumber;
    }

    public LocalDate getCertifiedDocumentDate() {
        return certifiedDocumentDate;
    }

    public void setCertifiedDocumentDate(LocalDate certifiedDocumentDate) {
        this.certifiedDocumentDate = certifiedDocumentDate;
    }

    public String getOriginSettlementNoteForAdvancedCredit() {
        return originSettlementNoteForAdvancedCredit;
    }

    public void setOriginSettlementNoteForAdvancedCredit(String originSettlementNoteForAdvancedCredit) {
        this.originSettlementNoteForAdvancedCredit = originSettlementNoteForAdvancedCredit;
    }

    public String getDocumentObservations() {
        return documentObservations;
    }

    public void setDocumentObservations(String documentObservations) {
        this.documentObservations = documentObservations;
    }

    public String getDocumentTermsAndConditions() {
        return documentTermsAndConditions;
    }

    public void setDocumentTermsAndConditions(String documentTermsAndConditions) {
        this.documentTermsAndConditions = documentTermsAndConditions;
    }

    public String getFinantialEntityCode() {
        return finantialEntityCode;
    }

    public void setFinantialEntityCode(String finantialEntityCode) {
        this.finantialEntityCode = finantialEntityCode;
    }

    public LocalizedString getFinantialEntityName() {
        return finantialEntityName;
    }

    public void setFinantialEntityName(LocalizedString finantialEntityName) {
        this.finantialEntityName = finantialEntityName;
    }

}
