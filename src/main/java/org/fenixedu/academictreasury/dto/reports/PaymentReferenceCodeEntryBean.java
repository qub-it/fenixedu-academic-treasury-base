package org.fenixedu.academictreasury.dto.reports;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.reports.DebtReportRequest;
import org.fenixedu.academictreasury.domain.reports.ErrorsLog;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;
import org.joda.time.DateTime;

public class PaymentReferenceCodeEntryBean extends AbstractReportEntryBean {

    private static final String TARGET_TYPE_FINANTIAL_DOCUMENT = "F";
    private static final String TARGET_TYPE_MULTIPLE_ENTRIES = "M";
    private static final String TARGET_TYPE_NOT_DEFINED = "N";

    public static String[] SPREADSHEET_HEADERS = { 
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.identification"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.versioningCreator"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.creationDate"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.customerId"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.debtAccountId"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.name"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.identificationType"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.identificationNumber"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.vatNumber"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.email"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.address"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.addressCountryCode"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.studentNumber"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.entityCode"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.referenceCode"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.finantialDocumentNumber"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.payableAmount"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.description"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.target.type"),
            academicTreasuryBundle("label.PaymentReferenceCodeEntryBean.header.state") };

    private String identification;
    private String versioningCreator;
    private DateTime creationDate;
    private String customerId;
    private String debtAccountId;
    private String name;
    private String identificationType;
    private String identificationNumber;
    private String vatNumber;
    private String email;
    private String address;
    private String addressCountryCode;
    private Integer studentNumber;
    private String entityCode;
    private String referenceCode;
    private String finantialDocumentNumber;
    private BigDecimal payableAmount;
    private String description;
    private String targetType;
    private String state;

    private SibsPaymentRequest sibsPaymentRequest;

    boolean completed = false;

    private String decimalSeparator;

    public PaymentReferenceCodeEntryBean(SibsPaymentRequest sibsPaymentRequest, DebtReportRequest request,
            ErrorsLog errorsLog) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();
        
        this.decimalSeparator = request != null ? request.getDecimalSeparator() : DebtReportRequest.DOT;

        try {
            this.sibsPaymentRequest = sibsPaymentRequest;

            this.identification = sibsPaymentRequest.getExternalId();
            this.versioningCreator = treasuryServices.versioningCreatorUsername(sibsPaymentRequest);
            this.creationDate = treasuryServices.versioningCreationDate(sibsPaymentRequest);

            DebtAccount referenceDebtAccount = sibsPaymentRequest.getDebtAccount();
            this.customerId = referenceDebtAccount.getCustomer().getExternalId();
            this.debtAccountId = referenceDebtAccount.getExternalId();
            this.name = referenceDebtAccount.getCustomer().getName();

            if (referenceDebtAccount.getCustomer().isPersonCustomer()
                    && ((PersonCustomer) referenceDebtAccount.getCustomer()).getPerson() != null
                    && ((PersonCustomer) referenceDebtAccount.getCustomer()).getPerson().getIdDocumentType() != null) {
                this.identificationType = ((PersonCustomer) referenceDebtAccount.getCustomer()).getPerson()
                        .getIdDocumentType().getLocalizedName();
            } else if (referenceDebtAccount.getCustomer().isPersonCustomer()
                    && ((PersonCustomer) referenceDebtAccount.getCustomer()).getPersonForInactivePersonCustomer() != null
                    && ((PersonCustomer) referenceDebtAccount.getCustomer()).getPersonForInactivePersonCustomer()
                            .getIdDocumentType() != null) {
                this.identificationType = ((PersonCustomer) referenceDebtAccount.getCustomer())
                        .getPersonForInactivePersonCustomer().getIdDocumentType().getLocalizedName();
            }

            this.identificationNumber = referenceDebtAccount.getCustomer().getIdentificationNumber();
            this.vatNumber = referenceDebtAccount.getCustomer().getUiFiscalNumber();

            if (referenceDebtAccount.getCustomer().isPersonCustomer()
                    && ((PersonCustomer) referenceDebtAccount.getCustomer()).getPerson() != null) {
                this.email = ((PersonCustomer) referenceDebtAccount.getCustomer()).getPerson()
                        .getInstitutionalOrDefaultEmailAddressValue();
            } else if (referenceDebtAccount.getCustomer().isPersonCustomer()
                    && ((PersonCustomer) referenceDebtAccount.getCustomer()).getPersonForInactivePersonCustomer() != null) {
                this.email = ((PersonCustomer) referenceDebtAccount.getCustomer()).getPersonForInactivePersonCustomer()
                        .getInstitutionalOrDefaultEmailAddressValue();
            }

            this.address = referenceDebtAccount.getCustomer().getAddress();
            this.addressCountryCode = referenceDebtAccount.getCustomer().getAddressCountryCode();

            if (referenceDebtAccount.getCustomer().isPersonCustomer()
                    && ((PersonCustomer) referenceDebtAccount.getCustomer()).getPerson() != null
                    && ((PersonCustomer) referenceDebtAccount.getCustomer()).getPerson().getStudent() != null) {
                this.studentNumber =
                        ((PersonCustomer) referenceDebtAccount.getCustomer()).getPerson().getStudent().getNumber();
            } else if (referenceDebtAccount.getCustomer().isPersonCustomer()
                    && ((PersonCustomer) referenceDebtAccount.getCustomer()).getPersonForInactivePersonCustomer() != null
                    && ((PersonCustomer) referenceDebtAccount.getCustomer()).getPersonForInactivePersonCustomer()
                            .getStudent() != null) {
                this.studentNumber = ((PersonCustomer) referenceDebtAccount.getCustomer())
                        .getPersonForInactivePersonCustomer().getStudent().getNumber();
            }

            this.entityCode = sibsPaymentRequest.getDigitalPaymentPlatform().castToSibsPaymentCodePoolService().getEntityReferenceCode();
            this.referenceCode = sibsPaymentRequest.getReferenceCode();

                this.finantialDocumentNumber =
                        String.join(", ",
                                sibsPaymentRequest.getDebitEntriesSet().stream()
                                        .filter(i -> i.getFinantialDocument() != null)
                                        .map(i -> i.getFinantialDocument().getUiDocumentNumber())
                                        .collect(Collectors.toList()));

            this.payableAmount = Currency.getValueWithScale(sibsPaymentRequest.getPayableAmount());
            this.description = sibsPaymentRequest.getDescription();
            this.targetType = TARGET_TYPE_MULTIPLE_ENTRIES;

            this.state = sibsPaymentRequest.getState().getDescriptionI18N().getContent();

            completed = true;
        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(sibsPaymentRequest, e);
        }
    }

    @Override
    public void writeCellValues(final Row row, final IErrorsLog ierrorsLog) {
        final ErrorsLog errorsLog = (ErrorsLog) ierrorsLog;

        try {
            row.createCell(0).setCellValue(identification);

            if (!completed) {
                row.createCell(1).setCellValue(academicTreasuryBundle("error.DebtReportEntryBean.report.generation.verify.entry"));
                return;
            }

            int i = 1;

            row.createCell(i++).setCellValue(versioningCreator);
            row.createCell(i++).setCellValue(valueOrEmpty(creationDate));
            row.createCell(i++).setCellValue(valueOrEmpty(customerId));
            row.createCell(i++).setCellValue(valueOrEmpty(debtAccountId));
            row.createCell(i++).setCellValue(valueOrEmpty(name));
            row.createCell(i++).setCellValue(valueOrEmpty(identificationType));
            row.createCell(i++).setCellValue(valueOrEmpty(identificationNumber));
            row.createCell(i++).setCellValue(valueOrEmpty(vatNumber));
            row.createCell(i++).setCellValue(valueOrEmpty(email));
            row.createCell(i++).setCellValue(valueOrEmpty(address));
            row.createCell(i++).setCellValue(valueOrEmpty(addressCountryCode));
            row.createCell(i++).setCellValue(valueOrEmpty(studentNumber));
            row.createCell(i++).setCellValue(valueOrEmpty(entityCode));
            row.createCell(i++).setCellValue(valueOrEmpty(referenceCode));
            row.createCell(i++).setCellValue(valueOrEmpty(finantialDocumentNumber));

            {

                String value = this.payableAmount != null ? this.payableAmount.toString() : "";
                
                if (DebtReportRequest.COMMA.equals(decimalSeparator)) {
                    value = value.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
                }
                
                row.createCell(i++).setCellValue(valueOrEmpty(value));
            }
            row.createCell(i++).setCellValue(valueOrEmpty(description));
            row.createCell(i++).setCellValue(valueOrEmpty(targetType));
            row.createCell(i++).setCellValue(valueOrEmpty(state));

        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(sibsPaymentRequest, e);
        }
    }

    // @formatter:off
    /* *****************
     * GETTERS & SETTERS
     * *****************
     */
    // @formatter:on
    
    public String getIdentification() {
        return identification;
    }

    public void setIdentification(String identification) {
        this.identification = identification;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddressCountryCode() {
        return addressCountryCode;
    }

    public void setAddressCountryCode(String addressCountryCode) {
        this.addressCountryCode = addressCountryCode;
    }

    public Integer getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(Integer studentNumber) {
        this.studentNumber = studentNumber;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getFinantialDocumentNumber() {
        return finantialDocumentNumber;
    }

    public void setFinantialDocumentNumber(String finantialDocumentNumber) {
        this.finantialDocumentNumber = finantialDocumentNumber;
    }

    public BigDecimal getPayableAmount() {
        return payableAmount;
    }

    public void setPayableAmount(BigDecimal payableAmount) {
        this.payableAmount = payableAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public SibsPaymentRequest getSibsPaymentRequest() {
        return sibsPaymentRequest;
    }

    public void setSibsPaymentRequest(SibsPaymentRequest sibsPaymentRequest) {
        this.sibsPaymentRequest = sibsPaymentRequest;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public String getDecimalSeparator() {
        return decimalSeparator;
    }
    
    public void setDecimalSeparator(String decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    
}