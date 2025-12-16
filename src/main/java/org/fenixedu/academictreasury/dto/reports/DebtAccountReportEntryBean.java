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

import static com.qubit.terra.framework.tools.excel.ExcelUtil.createCellWithValue;
import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.reports.DebtReportRequest;
import org.fenixedu.academictreasury.domain.reports.ErrorsLog;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.services.integration.FenixEDUTreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.FiscalCodeValidation;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;
import org.fenixedu.treasury.util.streaming.spreadsheet.SpreadsheetRow;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

public class DebtAccountReportEntryBean implements SpreadsheetRow {

    // @formatter:off
    public static String[] getSpreadsheetHeaders() {
        return new String[] { academicTreasuryBundle("label.DebtAccountReportEntryBean.header.identification"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.versioningCreator"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.creationDate"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.finantialInstitutionName"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.customerId"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.code"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.customerActive"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.name"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.identificationType"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.identificationNumber"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.vatNumber"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.email"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.address"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.addressCountryCode"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.studentNumber"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.vatNumberValid"),
                academicTreasuryBundle("label.DebtAccountReportEntryBean.header.totalInDebt") };
    }

    // @formatter:on

    final DebtAccount debtAccount;

    private PersonCustomer personCustomer;

    boolean completed = false;

    private String identification;
    private String versioningCreator;
    private DateTime creationDate;
    private String finantialInstitutionName;
    private String customerId;
    private String customerCode;
    private boolean customerActive;
    private String name;
    private String identificationType;
    private String identificationNumber;
    private String vatNumber;
    private String email;
    private String address;
    private String addressCountryCode;
    private Integer studentNumber;
    private boolean vatNumberValid;
    private BigDecimal totalInDebt;
    private BigDecimal dueInDebt;

    private String decimalSeparator;

    private Set<Registration> activeRegistrations;

    public DebtAccountReportEntryBean(final DebtAccount debtAccount, final DebtReportRequest request, final ErrorsLog errorsLog) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        this.decimalSeparator = request != null ? request.getDecimalSeparator() : DebtReportRequest.DOT;

        this.debtAccount = debtAccount;

        try {
            if (this.debtAccount.getCustomer().isPersonCustomer()) {
                this.personCustomer = (PersonCustomer) this.debtAccount.getCustomer();
            }

            this.identification = debtAccount.getExternalId();
            this.versioningCreator = FenixEDUTreasuryPlatformDependentServices.getVersioningCreatorUsername(debtAccount);
            this.creationDate = FenixEDUTreasuryPlatformDependentServices.getVersioningCreationDate(debtAccount);
            this.finantialInstitutionName = debtAccount.getFinantialInstitution().getName();
            this.customerId = debtAccount.getCustomer().getExternalId();
            this.customerCode = debtAccount.getCustomer().getCode();
            this.customerActive = debtAccount.getCustomer().isActive();
            this.name = debtAccount.getCustomer().getName();

            if (debtAccount.getCustomer()
                    .isPersonCustomer() && ((PersonCustomer) debtAccount.getCustomer()).getPerson() != null && ((PersonCustomer) debtAccount.getCustomer()).getPerson()
                    .getIdDocumentType() != null) {
                this.identificationType =
                        ((PersonCustomer) debtAccount.getCustomer()).getPerson().getIdDocumentType().getLocalizedName();
            } else if (debtAccount.getCustomer()
                    .isPersonCustomer() && ((PersonCustomer) debtAccount.getCustomer()).getPersonForInactivePersonCustomer() != null && ((PersonCustomer) debtAccount.getCustomer()).getPersonForInactivePersonCustomer()
                    .getIdDocumentType() != null) {
                this.identificationType =
                        ((PersonCustomer) debtAccount.getCustomer()).getPersonForInactivePersonCustomer().getIdDocumentType()
                                .getLocalizedName();
            }

            this.identificationNumber = debtAccount.getCustomer().getIdentificationNumber();
            this.vatNumber = debtAccount.getCustomer().getUiFiscalNumber();

            if (debtAccount.getCustomer()
                    .isPersonCustomer() && ((PersonCustomer) debtAccount.getCustomer()).getPerson() != null) {
                this.email =
                        ((PersonCustomer) debtAccount.getCustomer()).getPerson().getInstitutionalOrDefaultEmailAddressValue();
            } else if (debtAccount.getCustomer()
                    .isPersonCustomer() && ((PersonCustomer) debtAccount.getCustomer()).getPersonForInactivePersonCustomer() != null) {
                this.email = ((PersonCustomer) debtAccount.getCustomer()).getPersonForInactivePersonCustomer()
                        .getInstitutionalOrDefaultEmailAddressValue();
            }

            this.address = debtAccount.getCustomer().getUiCompleteAddress();
            this.addressCountryCode = valueOrEmpty(debtAccount.getCustomer().getAddressCountryCode());

            if (debtAccount.getCustomer()
                    .isPersonCustomer() && ((PersonCustomer) debtAccount.getCustomer()).getPerson() != null && ((PersonCustomer) debtAccount.getCustomer()).getPerson()
                    .getStudent() != null) {
                this.studentNumber = ((PersonCustomer) debtAccount.getCustomer()).getPerson().getStudent().getNumber();
            } else if (debtAccount.getCustomer()
                    .isPersonCustomer() && ((PersonCustomer) debtAccount.getCustomer()).getPersonForInactivePersonCustomer() != null && ((PersonCustomer) debtAccount.getCustomer()).getPersonForInactivePersonCustomer()
                    .getStudent() != null) {
                this.studentNumber =
                        ((PersonCustomer) debtAccount.getCustomer()).getPersonForInactivePersonCustomer().getStudent()
                                .getNumber();
            }

            this.vatNumberValid = FiscalCodeValidation.isValidFiscalNumber(debtAccount.getCustomer().getAddressCountryCode(),
                    debtAccount.getCustomer().getFiscalNumber());

            this.totalInDebt = debtAccount.getTotalInDebt();
            this.dueInDebt = debtAccount.getDueInDebt();

            this.activeRegistrations = new HashSet<Registration>();

            if (this.debtAccount.getCustomer().isPersonCustomer()) {
                Student student = this.personCustomer.getAssociatedPerson().getStudent();

                if (student != null) {
                    this.activeRegistrations = student.getActiveRegistrationStream().collect(Collectors.toSet());
                }
            }

            this.completed = true;
        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(debtAccount, e);
        }
    }

    @Override
    public void writeCellValues(Row row, IErrorsLog ierrorsLog) {
        final ErrorsLog errorsLog = (ErrorsLog) ierrorsLog;

        try {
            createCellWithValue(row, 0, this.identification);

            if (!this.completed) {
                createCellWithValue(row, 1, academicTreasuryBundle("error.DebtReportEntryBean.report.generation.verify.entry"));
                return;
            }

            int i = 1;

            createCellWithValue(row, i++, this.versioningCreator);
            createCellWithValue(row, i++, valueOrEmpty(this.creationDate));
            createCellWithValue(row, i++, valueOrEmpty(this.finantialInstitutionName));
            createCellWithValue(row, i++, valueOrEmpty(this.customerId));
            createCellWithValue(row, i++, valueOrEmpty(this.customerCode));
            createCellWithValue(row, i++, valueOrEmpty(this.customerActive));
            createCellWithValue(row, i++, valueOrEmpty(this.name));
            createCellWithValue(row, i++, valueOrEmpty(this.identificationType));
            createCellWithValue(row, i++, valueOrEmpty(this.identificationNumber));
            createCellWithValue(row, i++, valueOrEmpty(this.vatNumber));
            createCellWithValue(row, i++, valueOrEmpty(this.email));
            createCellWithValue(row, i++, valueOrEmpty(this.address));
            createCellWithValue(row, i++, valueOrEmpty(this.addressCountryCode));
            createCellWithValue(row, i++, valueOrEmpty(this.studentNumber));
            createCellWithValue(row, i++, valueOrEmpty(this.vatNumberValid));
            createCellWithValue(row, i++, this.totalInDebt.toString());

            if (DebtReportRequest.COMMA.equals(this.decimalSeparator)) {
                createCellWithValue(row, i++,
                        this.totalInDebt.toString().replace(DebtReportRequest.DOT, DebtReportRequest.COMMA));
            } else {
                createCellWithValue(row, i++, this.totalInDebt.toString());
            }

        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(debtAccount, e);
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

    /**
     * GETTERS AND SETTERS
     */

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

    public String getFinantialInstitutionName() {
        return finantialInstitutionName;
    }

    public void setFinantialInstitutionName(String finantialInstitutionName) {
        this.finantialInstitutionName = finantialInstitutionName;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public void setCustomerCode(String customerCode) {
        this.customerCode = customerCode;
    }

    public boolean isCustomerActive() {
        return customerActive;
    }

    public void setCustomerActive(boolean customerActive) {
        this.customerActive = customerActive;
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

    public boolean isVatNumberValid() {
        return vatNumberValid;
    }

    public void setVatNumberValid(boolean vatNumberValid) {
        this.vatNumberValid = vatNumberValid;
    }

    public BigDecimal getTotalInDebt() {
        return totalInDebt;
    }

    public void setTotalInDebt(BigDecimal totalInDebt) {
        this.totalInDebt = totalInDebt;
    }

    public BigDecimal getDueInDebt() {
        return dueInDebt;
    }

    public void setDueInDebt(BigDecimal dueInDebt) {
        this.dueInDebt = dueInDebt;
    }

    public DebtAccount getDebtAccount() {
        return debtAccount;
    }

    public PersonCustomer getPersonCustomer() {
        return personCustomer;
    }

    public void setPersonCustomer(PersonCustomer personCustomer) {
        this.personCustomer = personCustomer;
    }

    public Set<Registration> getActiveRegistrations() {
        return activeRegistrations;
    }

    public void setActiveRegistrations(Set<Registration> activeRegistrations) {
        this.activeRegistrations = activeRegistrations;
    }

}
