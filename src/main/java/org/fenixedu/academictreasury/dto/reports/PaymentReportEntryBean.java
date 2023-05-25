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


import static org.fenixedu.academictreasury.dto.reports.DebtReportEntryBean.personalEmail;
import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.math.BigDecimal;

import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.reports.DebtReportRequest;
import org.fenixedu.academictreasury.domain.reports.ErrorsLog;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.PaymentEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;
import org.fenixedu.treasury.util.streaming.spreadsheet.SpreadsheetRow;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;

public class PaymentReportEntryBean implements SpreadsheetRow {

    public static String[] SPREADSHEET_HEADERS = { 
        academicTreasuryBundle("label.PaymentReportEntryBean.header.identification"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.creationDate"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.responsible"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.settlementNoteNumber"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.settlementNoteDocumentDate"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.settlementOriginDocumentNumber"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.paymentDate"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.settlementNoteAnnuled"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.documentExportationPending"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.paymentMethod"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.amount"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.customerId"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.debtAccountId"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.name"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.identificationType"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.identificationNumber"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.vatNumber"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.email"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.address"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.studentNumber"),
        academicTreasuryBundle("label.PaymentReportEntryBean.header.closeDate"),
        academicTreasuryBundle("label.DebtReportEntryBean.header.erpCertificationDate"),
        academicTreasuryBundle("label.DebtReportEntryBean.header.erpCertificateDocumentReference"),
            academicTreasuryBundle("label.PaymentReportEntryBean.header.documentObservations"),
            academicTreasuryBundle("label.PaymentReportEntryBean.header.documentTermsAndConditions"),
        
    };
 
    
    private PaymentEntry paymentEntry;
    private boolean completed;

    private String identification;
    private DateTime creationDate;
    private String responsible;
    private String settlementNoteNumber;
    private DateTime settlementNoteDocumentDate;
    private String settlementOriginDocumentNumber;
    private DateTime paymentDate;
    private Boolean settlementNoteAnnuled;
    private Boolean documentExportationPending;
    private String paymentMethod;
    private BigDecimal amount;
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

    private DateTime closeDate;
    private Boolean exportedInLegacyERP;

    private LocalDate erpCertificationDate;
    private String erpCertificateDocumentReference;
    
    private String erpCustomerId;
    private String erpPayorCustomerId;
    
    private String decimalSeparator;
    
    private String documentObservations;
    private String documentTermsAndConditions;
    
    public PaymentReportEntryBean(final PaymentEntry entry, final DebtReportRequest request, final ErrorsLog errorsLog) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        this.decimalSeparator = request != null ? request.getDecimalSeparator() : DebtReportRequest.COMMA;
        
        paymentEntry = entry;
        
        try {
            final SettlementNote settlementNote = entry.getSettlementNote();

            this.identification = entry.getExternalId();
            this.creationDate = treasuryServices.versioningCreationDate(entry);
            this.responsible = treasuryServices.versioningCreatorUsername(entry);
            this.settlementNoteNumber = settlementNote.getUiDocumentNumber();
            this.settlementNoteDocumentDate = settlementNote.getDocumentDate();
            this.settlementOriginDocumentNumber = settlementNote.getOriginDocumentNumber();
            this.paymentDate = settlementNote.getPaymentDate();
            this.settlementNoteAnnuled = settlementNote.isAnnulled();
            this.documentExportationPending = settlementNote.isDocumentToExport();
            this.paymentMethod = entry.getPaymentMethod().getName().getContent();
            this.amount = settlementNote.getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(entry.getPayedAmount());
            
            fillStudentInformation(entry);
            
            fillERPInformation(settlementNote);
            
            this.documentObservations = entry.getSettlementNote().getDocumentObservations();
            this.documentTermsAndConditions = entry.getSettlementNote().getDocumentTermsAndConditions();

            this.completed = true;
            
        } catch(final Exception e) {
            e.printStackTrace();
            errorsLog.addError(entry, e);
        }
    }
    
    private void fillERPInformation(final SettlementNote settlementNote) {
        ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();
        
        this.closeDate = settlementNote != null ? settlementNote.getCloseDate() : null;
        this.exportedInLegacyERP =
                settlementNote != null ? settlementNote.isExportedInLegacyERP() : false;

        this.erpCertificationDate =
                settlementNote != null ? settlementNote.getErpCertificationDate() : null;

        this.erpCertificateDocumentReference = settlementNote != null ? settlementNote
                .getErpCertificateDocumentReference() : null;

        this.erpCustomerId = settlementNote.getDebtAccount().getCustomer().getErpCustomerId();

        
        if(!settlementNote.getSettlemetEntriesSet().isEmpty()) {
            final SettlementEntry settlementEntry = settlementNote.getSettlemetEntriesSet().iterator().next();
            if(settlementEntry.getInvoiceEntry().getFinantialDocument() != null && ((Invoice) settlementEntry.getInvoiceEntry().getFinantialDocument()).getPayorDebtAccount() != null) {
                this.erpPayorCustomerId = ((Invoice) settlementEntry.getInvoiceEntry().getFinantialDocument()).getPayorDebtAccount().getCustomer().getErpCustomerId();
            }
        }
        
        if(treasuryServices.hasCertifiedDocument(settlementNote)) {
            this.erpCertificationDate = treasuryServices.getCertifiedDocumentDate(settlementNote);
            this.erpCertificateDocumentReference = treasuryServices.getCertifiedDocumentNumber(settlementNote);
        }
    }
    
    private void fillStudentInformation(final PaymentEntry entry) {
        final Customer customer = entry.getSettlementNote().getDebtAccount().getCustomer();

        this.customerId = customer.getExternalId();
        this.debtAccountId = entry.getSettlementNote().getDebtAccount().getExternalId();
        
        this.name = customer.getName();

        if (customer.isPersonCustomer() && ((PersonCustomer) customer).getAssociatedPerson() != null && ((PersonCustomer) customer).getAssociatedPerson().getIdDocumentType() != null) {
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

        this.address = customer.getAddress();

        if (customer.isPersonCustomer() && ((PersonCustomer) customer).getAssociatedPerson() != null && ((PersonCustomer) customer).getAssociatedPerson().getStudent() != null) {
            final Person person = ((PersonCustomer) customer).getAssociatedPerson();
            this.studentNumber = person.getStudent().getNumber();
        }
    }

    @Override
    public void writeCellValues(final Row row, final IErrorsLog ierrorsLog) {
        final ErrorsLog errorsLog = (ErrorsLog) ierrorsLog;
        
        try {
            row.createCell(0).setCellValue(identification);

            if(!completed) {
                row.createCell(1).setCellValue(academicTreasuryBundle("error.DebtReportEntryBean.report.generation.verify.entry"));
                return;
            }
            
            int i = 1;
            
            row.createCell(i++).setCellValue(valueOrEmpty(this.creationDate));
            row.createCell(i++).setCellValue(valueOrEmpty(this.responsible));
            row.createCell(i++).setCellValue(valueOrEmpty(this.settlementNoteNumber));
            row.createCell(i++).setCellValue(valueOrEmpty(this.settlementNoteDocumentDate));
            row.createCell(i++).setCellValue(valueOrEmpty(this.settlementOriginDocumentNumber));
            row.createCell(i++).setCellValue(valueOrEmpty(this.paymentDate));
            row.createCell(i++).setCellValue(valueOrEmpty(this.settlementNoteAnnuled));
            row.createCell(i++).setCellValue(valueOrEmpty(this.documentExportationPending));
            row.createCell(i++).setCellValue(valueOrEmpty(this.paymentMethod));

            {
                String value = amount != null ? amount.toString() : "";
                
                if(DebtReportRequest.COMMA.equals(decimalSeparator)) {
                    value = value.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
                }
                
                row.createCell(i++).setCellValue(value);
            }
            
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
   
            row.createCell(i++).setCellValue(valueOrEmpty(erpCertificationDate));
            row.createCell(i++).setCellValue(valueOrEmpty(erpCertificateDocumentReference));
            
            row.createCell(i++).setCellValue(valueOrEmpty(this.documentObservations));
            row.createCell(i++).setCellValue(valueOrEmpty(this.documentTermsAndConditions));

        } catch(final Exception e) {
            e.printStackTrace();
            errorsLog.addError(paymentEntry, e);
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

    
    // @formatter:off
    /* *****************
     * GETTERS & SETTERS
     * *****************
     */
    // @formatter:on
    public PaymentEntry getPaymentEntry() {
        return paymentEntry;
    }

    public void setPaymentEntry(PaymentEntry paymentEntry) {
        this.paymentEntry = paymentEntry;
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
    
    public String getSettlementOriginDocumentNumber() {
        return settlementOriginDocumentNumber;
    }
    
    public void setSettlementOriginDocumentNumber(String settlementOriginDocumentNumber) {
        this.settlementOriginDocumentNumber = settlementOriginDocumentNumber;
    }

    public DateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(DateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Boolean getSettlementNoteAnnuled() {
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
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
