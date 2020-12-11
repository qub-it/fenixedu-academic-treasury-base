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
package org.fenixedu.academictreasury.dto.reports;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.math.BigDecimal;

import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryTarget;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.reports.DebtReportRequest;
import org.fenixedu.academictreasury.domain.reports.ErrorsLog;
import org.fenixedu.academictreasury.domain.serviceRequests.ITreasuryServiceRequest;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;
import org.fenixedu.treasury.util.streaming.spreadsheet.SpreadsheetRow;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;

public class SettlementReportEntryBean implements SpreadsheetRow {

    public static String[] SPREADSHEET_HEADERS =
            { academicTreasuryBundle("label.SettlementReportEntryBean.header.identification"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.creationDate"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.responsible"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.settlementNoteNumber"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.settlementNoteDocumentDate"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.paymentDate"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.settlementNoteAnnuled"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.documentExportationPending"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.settlementEntryOrder"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.amount"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.productCode"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.settlementEntryDescription"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.invoiceEntryIdentification"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.invoiceEntryType"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.invoiceEntryAmountToPay"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.invoiceDocumentNumber"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.customerId"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.debtAccountId"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.name"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.identificationType"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.identificationNumber"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.vatNumber"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.email"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.address"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.studentNumber"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.closeDate"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.degreeType"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.degreeCode"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.degreeName"),
                    academicTreasuryBundle("label.SettlementReportEntryBean.header.executionYear"), };

    private SettlementEntry settlementEntry;
    private SettlementNote settlementNote;
    private boolean completed;

    private String identification;
    private DateTime creationDate;
    private String responsible;
    private String invoiceEntryIdentification;
    private String invoiceEntryType;
    private BigDecimal invoiceEntryAmountToPay;
    private String invoiceDocumentNumber;
    private String settlementNoteNumber;
    private DateTime settlementNoteDocumentDate;
    private DateTime paymentDate;
    private Boolean settlementNoteAnnuled;
    private Boolean documentExportationPending;
    private Integer settlementEntryOrder;
    private BigDecimal amount;
    private String productCode;
    private String settlementEntryDescription;
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
    private String executionYear;
    private String executionSemester;

    private DateTime closeDate;
    private Boolean exportedInLegacyERP;

    private LocalDate erpCertificationDate;
    private String erpCertificateDocumentReference;

    private String erpCustomerId;
    private String erpPayorCustomerId;

    private String decimalSeparator;

    public SettlementReportEntryBean(final SettlementEntry entry, final DebtReportRequest request, final ErrorsLog errorsLog) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        this.decimalSeparator = request != null ? request.getDecimalSeparator() : DebtReportRequest.DOT;

        this.settlementEntry = entry;
        this.settlementNote = (SettlementNote) entry.getFinantialDocument();

        try {
            final Currency currency = settlementNote.getDebtAccount().getFinantialInstitution().getCurrency();

            this.identification = entry.getExternalId();
            this.creationDate = treasuryServices.versioningCreationDate(entry);
            this.responsible = treasuryServices.versioningCreatorUsername(entry);
            this.invoiceEntryIdentification = entry.getInvoiceEntry().getExternalId();
            this.settlementNoteNumber = settlementNote.getUiDocumentNumber();
            this.settlementNoteDocumentDate = settlementNote.getDocumentDate();
            this.paymentDate = settlementNote.getPaymentDate();
            this.settlementNoteAnnuled = settlementNote.isAnnulled();
            this.documentExportationPending = settlementNote.isDocumentToExport();
            this.invoiceEntryType = entryType(entry.getInvoiceEntry());
            this.invoiceEntryAmountToPay = currency.getValueWithScale(entry.getInvoiceEntry().getAmountWithVat());
            this.invoiceDocumentNumber = entry.getInvoiceEntry().getFinantialDocument().getUiDocumentNumber();
            this.settlementEntryOrder = entry.getEntryOrder();
            this.amount = settlementNote.getDebtAccount().getFinantialInstitution().getCurrency()
                    .getValueWithScale(entry.getTotalAmount());

            this.productCode = entry.getInvoiceEntry().getProduct().getCode();
            this.settlementEntryDescription = entry.getDescription();

            fillStudentInformation(entry);

            fillAcademicInformation(entry.getInvoiceEntry());

            fillERPInformation(entry);

            this.completed = true;
        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(entry, e);
        }

    }

    private void fillERPInformation(final SettlementEntry entry) {
        this.closeDate = entry.getFinantialDocument() != null ? entry.getFinantialDocument().getCloseDate() : null;
        this.exportedInLegacyERP =
                entry.getFinantialDocument() != null ? entry.getFinantialDocument().isExportedInLegacyERP() : false;

        this.erpCertificationDate =
                entry.getFinantialDocument() != null ? entry.getFinantialDocument().getErpCertificationDate() : null;

        this.erpCertificateDocumentReference =
                entry.getFinantialDocument() != null ? entry.getFinantialDocument().getErpCertificateDocumentReference() : null;

        this.erpCustomerId = entry.getFinantialDocument().getDebtAccount().getCustomer().getErpCustomerId();

        if (entry.getInvoiceEntry().getFinantialDocument() != null
                && ((Invoice) entry.getInvoiceEntry().getFinantialDocument()).getPayorDebtAccount() != null) {
            this.erpPayorCustomerId = ((Invoice) entry.getInvoiceEntry().getFinantialDocument()).getPayorDebtAccount()
                    .getCustomer().getErpCustomerId();
        }
    }

    private void fillAcademicInformation(final InvoiceEntry invoiceEntry) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        final DebitEntry debitEntry =
                invoiceEntry.isDebitNoteEntry() ? (DebitEntry) invoiceEntry : ((CreditEntry) invoiceEntry).getDebitEntry();

        if (debitEntry != null) {

            // Degree && ExecutionYear && ExecutionInterval
            if (debitEntry.getTreasuryEvent() != null && debitEntry.getTreasuryEvent() instanceof AcademicTreasuryEvent) {
                final AcademicTreasuryEvent academicTreasuryEvent = (AcademicTreasuryEvent) debitEntry.getTreasuryEvent();

                if (academicTreasuryEvent.isForRegistrationTuition()) {
                    this.registrationNumber = academicTreasuryEvent.getRegistration().getNumber();
                    this.degreeType = academicTreasuryServices
                            .localizedNameOfDegreeType(academicTreasuryEvent.getRegistration().getDegree().getDegreeType());
                    this.degreeCode = academicTreasuryEvent.getRegistration().getDegree().getCode();
                    this.degreeName = academicTreasuryEvent.getRegistration().getDegree().getPresentationName();
                    this.executionYear = academicTreasuryEvent.getExecutionYear().getQualifiedName();

                } else if (academicTreasuryEvent.isForStandaloneTuition()
                        || academicTreasuryEvent.isForExtracurricularTuition()) {
                    if (debitEntry.getCurricularCourse() != null) {
                        this.degreeType = academicTreasuryServices
                                .localizedNameOfDegreeType(debitEntry.getCurricularCourse().getDegree().getDegreeType());
                        this.degreeCode = debitEntry.getCurricularCourse().getDegree().getCode();
                        this.degreeName = debitEntry.getCurricularCourse().getDegree().getPresentationName();
                    }

                    if (debitEntry.getExecutionSemester() != null) {
                        this.executionYear = ((ExecutionSemester) debitEntry.getExecutionSemester()).getExecutionYear().getQualifiedName();
                        this.executionSemester = debitEntry.getExecutionSemester().getQualifiedName();
                    }

                } else if (academicTreasuryEvent.isForImprovementTax()) {
                    if (debitEntry.getCurricularCourse() != null) {
                        this.degreeType = academicTreasuryServices
                                .localizedNameOfDegreeType(debitEntry.getCurricularCourse().getDegree().getDegreeType());
                        this.degreeCode = debitEntry.getCurricularCourse().getDegree().getCode();
                        this.degreeName = debitEntry.getCurricularCourse().getDegree().getPresentationName();
                    }

                    if (debitEntry.getExecutionSemester() != null) {
                        this.executionYear =
                                ((ExecutionSemester) debitEntry.getExecutionSemester()).getExecutionYear().getQualifiedName();
                        this.executionSemester = debitEntry.getExecutionSemester().getQualifiedName();
                    }

                } else if (academicTreasuryEvent.isForAcademicTax()) {

                    this.registrationNumber = academicTreasuryEvent.getRegistration().getNumber();
                    this.degreeType = academicTreasuryServices
                            .localizedNameOfDegreeType(academicTreasuryEvent.getRegistration().getDegree().getDegreeType());
                    this.degreeCode = academicTreasuryEvent.getRegistration().getDegree().getCode();
                    this.degreeName = academicTreasuryEvent.getRegistration().getDegree().getPresentationName();
                    this.executionYear = academicTreasuryEvent.getExecutionYear().getQualifiedName();

                } else if (academicTreasuryEvent.isForAcademicServiceRequest()) {

                    final ITreasuryServiceRequest iTreasuryServiceRequest = academicTreasuryEvent.getITreasuryServiceRequest();

                    this.registrationNumber = iTreasuryServiceRequest.getRegistration().getNumber();
                    this.degreeType = academicTreasuryServices
                            .localizedNameOfDegreeType(iTreasuryServiceRequest.getRegistration().getDegree().getDegreeType());
                    this.degreeCode = iTreasuryServiceRequest.getRegistration().getDegree().getCode();
                    this.degreeName = iTreasuryServiceRequest.getRegistration().getDegree().getPresentationName();

                    if (iTreasuryServiceRequest.hasExecutionYear()) {
                        this.executionYear = iTreasuryServiceRequest.getExecutionYear().getQualifiedName();
                    }
                } else if (academicTreasuryEvent.isForTreasuryEventTarget()) {
                    IAcademicTreasuryTarget treasuryEventTarget =
                            (IAcademicTreasuryTarget) academicTreasuryEvent.getTreasuryEventTarget();

                    if (treasuryEventTarget.getAcademicTreasuryTargetRegistration() != null) {
                        this.registrationNumber = treasuryEventTarget.getAcademicTreasuryTargetRegistration().getNumber();
                        this.degreeType = treasuryEventTarget.getAcademicTreasuryTargetRegistration().getDegree().getDegreeType()
                                .getName().getContent();
                        this.degreeCode = treasuryEventTarget.getAcademicTreasuryTargetRegistration().getDegree().getCode();
                        this.degreeName =
                                treasuryEventTarget.getAcademicTreasuryTargetRegistration().getDegree().getPresentationName();
                    }

                    if (treasuryEventTarget.getAcademicTreasuryTargetExecutionYear() != null) {
                        this.executionYear = treasuryEventTarget.getAcademicTreasuryTargetExecutionYear().getQualifiedName();
                    }

                    if (treasuryEventTarget.getAcademicTreasuryTargetExecutionSemester() != null) {
                        this.executionSemester =
                                treasuryEventTarget.getAcademicTreasuryTargetExecutionSemester().getQualifiedName();
                    }
                }
            } else if (debitEntry.getTreasuryEvent() != null) {
                final TreasuryEvent treasuryEvent = debitEntry.getTreasuryEvent();

                if (!Strings.isNullOrEmpty(treasuryEvent.getDegreeCode())) {
                    this.degreeCode = treasuryEvent.getDegreeCode();
                }

                if (!Strings.isNullOrEmpty(treasuryEvent.getDegreeName())) {
                    this.degreeName = treasuryEvent.getDegreeName();
                }

                if (!Strings.isNullOrEmpty(treasuryEvent.getExecutionYearName())) {
                    this.executionYear = treasuryEvent.getExecutionYearName();
                }
            }

            if (Strings.isNullOrEmpty(this.degreeCode)) {
                this.degreeCode = debitEntry.getDegreeCode();
            }

            if (Strings.isNullOrEmpty(this.executionYear)) {
                this.executionYear = debitEntry.getExecutionYearName();
            }
        }
    }

    private void fillStudentInformation(final SettlementEntry entry) {
        final Customer customer = entry.getFinantialDocument().getDebtAccount().getCustomer();

        this.customerId = customer.getExternalId();
        this.debtAccountId = entry.getFinantialDocument().getDebtAccount().getExternalId();

        this.name = customer.getName();

        if (customer.isPersonCustomer() && ((PersonCustomer) customer).getAssociatedPerson() != null
                && ((PersonCustomer) customer).getAssociatedPerson().getIdDocumentType() != null) {
            this.identificationType = ((PersonCustomer) customer).getAssociatedPerson().getIdDocumentType().getLocalizedName();
        }

        this.identificationNumber = customer.getIdentificationNumber();
        this.vatNumber = customer.getUiFiscalNumber();

        if (customer.isPersonCustomer() && ((PersonCustomer) customer).getAssociatedPerson() != null) {
            final Person person = ((PersonCustomer) customer).getAssociatedPerson();
            this.institutionalOrDefaultEmail = person.getInstitutionalOrDefaultEmailAddressValue();
            this.emailForSendingEmails = person.getEmailForSendingEmails();
            this.personalEmail = DebtReportEntryBean.personalEmail(person) != null ? DebtReportEntryBean.personalEmail(person).getValue() : "";
        }

        this.address = customer.getAddress();

        if (customer.isPersonCustomer() && ((PersonCustomer) customer).getAssociatedPerson() != null
                && ((PersonCustomer) customer).getAssociatedPerson().getStudent() != null) {
            this.studentNumber = ((PersonCustomer) customer).getAssociatedPerson().getStudent().getNumber();
        }
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
            row.createCell(0).setCellValue(identification);

            if (!completed) {
                row.createCell(1)
                        .setCellValue(academicTreasuryBundle("error.DebtReportEntryBean.report.generation.verify.entry"));
                return;
            }

            int i = 1;

            row.createCell(i++).setCellValue(valueOrEmpty(creationDate));
            row.createCell(i++).setCellValue(valueOrEmpty(responsible));
            row.createCell(i++).setCellValue(valueOrEmpty(settlementNoteNumber));
            row.createCell(i++).setCellValue(valueOrEmpty(settlementNoteDocumentDate));
            row.createCell(i++).setCellValue(valueOrEmpty(paymentDate));
            row.createCell(i++).setCellValue(valueOrEmpty(settlementNoteAnnuled));
            row.createCell(i++).setCellValue(valueOrEmpty(documentExportationPending));
            row.createCell(i++).setCellValue(valueOrEmpty(settlementEntryOrder));

            {
                String value = amount != null ? amount.toString() : "";
                if (DebtReportRequest.COMMA.equals(decimalSeparator)) {
                    value = value.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
                }

                row.createCell(i++).setCellValue(valueOrEmpty(value));
            }

            row.createCell(i++).setCellValue(valueOrEmpty(productCode));
            row.createCell(i++).setCellValue(valueOrEmpty(settlementEntryDescription));
            row.createCell(i++).setCellValue(valueOrEmpty(invoiceEntryIdentification));
            row.createCell(i++).setCellValue(valueOrEmpty(invoiceEntryType));

            {
                String value = invoiceEntryAmountToPay != null ? invoiceEntryAmountToPay.toString() : "";
                if (DebtReportRequest.COMMA.equals(decimalSeparator)) {
                    value = value.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
                }

                row.createCell(i++).setCellValue(valueOrEmpty(value));
            }

            row.createCell(i++).setCellValue(valueOrEmpty(invoiceDocumentNumber));
            row.createCell(i++).setCellValue(customerId);
            row.createCell(i++).setCellValue(debtAccountId);
            row.createCell(i++).setCellValue(valueOrEmpty(name));
            row.createCell(i++).setCellValue(valueOrEmpty(identificationType));
            row.createCell(i++).setCellValue(valueOrEmpty(identificationNumber));
            row.createCell(i++).setCellValue(valueOrEmpty(vatNumber));
            row.createCell(i++).setCellValue(valueOrEmpty(institutionalOrDefaultEmail));
            row.createCell(i++).setCellValue(valueOrEmpty(address));
            row.createCell(i++).setCellValue(valueOrEmpty(studentNumber));
            row.createCell(i++).setCellValue(valueOrEmpty(closeDate));

            row.createCell(i++).setCellValue(valueOrEmpty(this.degreeType));
            row.createCell(i++).setCellValue(valueOrEmpty(this.degreeCode));
            row.createCell(i++).setCellValue(valueOrEmpty(this.degreeName));
            row.createCell(i++).setCellValue(valueOrEmpty(this.executionYear));

        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(settlementEntry, e);
        }
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

    // @formatter:off
    /* *****************
     * GETTERS & SETTERS
     * *****************
     */
    // @formatter:on

    public SettlementEntry getSettlementEntry() {
        return settlementEntry;
    }

    public void setSettlementEntry(SettlementEntry settlementEntry) {
        this.settlementEntry = settlementEntry;
    }

    public SettlementNote getSettlementNote() {
        return settlementNote;
    }

    public void setSettlementNote(SettlementNote settlementNote) {
        this.settlementNote = settlementNote;
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

    public DateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(DateTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public String getInvoiceEntryIdentification() {
        return invoiceEntryIdentification;
    }

    public void setInvoiceEntryIdentification(String invoiceEntryIdentification) {
        this.invoiceEntryIdentification = invoiceEntryIdentification;
    }

    public String getInvoiceEntryType() {
        return invoiceEntryType;
    }

    public void setInvoiceEntryType(String invoiceEntryType) {
        this.invoiceEntryType = invoiceEntryType;
    }

    public BigDecimal getInvoiceEntryAmountToPay() {
        return invoiceEntryAmountToPay;
    }

    public void setInvoiceEntryAmountToPay(BigDecimal invoiceEntryAmountToPay) {
        this.invoiceEntryAmountToPay = invoiceEntryAmountToPay;
    }

    public String getInvoiceDocumentNumber() {
        return invoiceDocumentNumber;
    }

    public void setInvoiceDocumentNumber(String invoiceDocumentNumber) {
        this.invoiceDocumentNumber = invoiceDocumentNumber;
    }

    public String getSettlementNoteNumber() {
        return settlementNoteNumber;
    }

    public void setSettlementNoteNumber(String settlementNoteNumber) {
        this.settlementNoteNumber = settlementNoteNumber;
    }

    public DateTime getSettlementNoteDocumentDate() {
        return settlementNoteDocumentDate;
    }

    public void setSettlementNoteDocumentDate(DateTime settlementNoteDocumentDate) {
        this.settlementNoteDocumentDate = settlementNoteDocumentDate;
    }

    public DateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(DateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public boolean getSettlementNoteAnnuled() {
        return settlementNoteAnnuled;
    }

    public void setSettlementNoteAnnuled(Boolean settlementNoteAnnuled) {
        this.settlementNoteAnnuled = settlementNoteAnnuled;
    }

    public boolean getDocumentExportationPending() {
        return documentExportationPending;
    }

    public void setDocumentExportationPending(Boolean documentExportationPending) {
        this.documentExportationPending = documentExportationPending;
    }

    public Integer getSettlementEntryOrder() {
        return settlementEntryOrder;
    }

    public void setSettlementEntryOrder(Integer settlementEntryOrder) {
        this.settlementEntryOrder = settlementEntryOrder;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getSettlementEntryDescription() {
        return settlementEntryDescription;
    }

    public void setSettlementEntryDescription(String settlementEntryDescription) {
        this.settlementEntryDescription = settlementEntryDescription;
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

    public void setInstitutionalOrDefaultEmailEmail(String institutionalOrDefaultEmail) {
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

    public DateTime getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(DateTime closeDate) {
        this.closeDate = closeDate;
    }

    public Boolean getExportedInLegacyERP() {
        return exportedInLegacyERP;
    }

    public void setExportedInLegacyERP(Boolean exportedInLegacyERP) {
        this.exportedInLegacyERP = exportedInLegacyERP;
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

    public String getDecimalSeparator() {
        return decimalSeparator;
    }

    public void setDecimalSeparator(String decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

}
