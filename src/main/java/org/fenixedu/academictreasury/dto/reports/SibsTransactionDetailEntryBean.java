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

import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.academictreasury.domain.reports.DebtReportRequest;
import org.fenixedu.academictreasury.domain.reports.ErrorsLog;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentCodeTransaction;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;
import org.joda.time.DateTime;

public class SibsTransactionDetailEntryBean extends AbstractReportEntryBean {

    public static String[] getSpreadsheetHeaders() {
        return new String[] { academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.identification"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.versioningCreator"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.creationDate"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.whenProcessed"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.whenRegistered"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.amountPayed"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.sibsEntityReferenceCode"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.sibsPaymentReferenceCode"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.sibsTransactionId"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.debtAccountId"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.customerId"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.businessIdentification"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.fiscalNumber"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.customerName"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.settlementDocumentNumber"),
                academicTreasuryBundle("label.SibsTransactionDetailEntryBean.header.comments") };
    }

    private String identification;
    private String versioningCreator;
    private DateTime creationDate;
    private DateTime whenProcessed;
    private DateTime whenRegistered;
    private String amountPayed;
    private String sibsEntityReferenceCode;
    private String sibsPaymentReferenceCode;
    private String sibsTransactionId;
    private String debtAccountId;
    private String customerId;
    private String businessIdentification;
    private String fiscalNumber;
    private String customerName;
    private String settlementDocumentNumber;
    private String comments;

    private SibsPaymentCodeTransaction sibsTransactionDetail;

    boolean completed = false;

    public SibsTransactionDetailEntryBean(SibsPaymentCodeTransaction detail, DebtReportRequest request, ErrorsLog errorsLog) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        final String decimalSeparator = request.getDecimalSeparator();

        try {
            this.sibsTransactionDetail = detail;

            this.identification = detail.getExternalId();
            this.versioningCreator = treasuryServices.versioningCreatorUsername(detail);
            this.creationDate = treasuryServices.versioningCreationDate(detail);
            this.whenProcessed = detail.getSibsProcessingDate();
            this.whenRegistered = detail.getPaymentDate();
            this.amountPayed = detail.getPaidAmount() != null ? detail.getPaidAmount().toString() : "";
            this.sibsEntityReferenceCode = detail.getSibsEntityReferenceCode();
            this.sibsPaymentReferenceCode = detail.getSibsPaymentReferenceCode();
            this.sibsTransactionId = detail.getSibsTransactionId();
            this.debtAccountId = detail.getPaymentRequest().getDebtAccount().getExternalId();
            this.customerId = detail.getPaymentRequest().getDebtAccount().getCustomer().getExternalId();
            this.businessIdentification = detail.getPaymentRequest().getDebtAccount().getCustomer().getBusinessIdentification();
            this.fiscalNumber = detail.getPaymentRequest().getDebtAccount().getCustomer().getUiFiscalNumber();
            this.customerName = detail.getPaymentRequest().getDebtAccount().getCustomer().getName();
            this.settlementDocumentNumber = String.join(",",
                    detail.getSettlementNotesSet().stream().map(s -> s.getUiDocumentNumber()).collect(Collectors.toSet()));
            this.comments = detail.getComments();

            if (DebtReportRequest.COMMA.equals(decimalSeparator)) {
                this.amountPayed = this.amountPayed.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
            }

            this.completed = true;
        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(sibsTransactionDetail, e);
        }

    }

    @Override
    public void writeCellValues(final Row row, final IErrorsLog ierrorsLog) {
        final ErrorsLog errorsLog = (ErrorsLog) ierrorsLog;

        try {
            createCellWithValue(row, 0, identification);

            if (!completed) {
                createCellWithValue(row, 1, academicTreasuryBundle("error.DebtReportEntryBean.report.generation.verify.entry"));
                return;
            }

            int i = 1;

            createCellWithValue(row, i++, valueOrEmpty(versioningCreator));
            createCellWithValue(row, i++, valueOrEmpty(creationDate));
            createCellWithValue(row, i++, valueOrEmpty(whenProcessed));
            createCellWithValue(row, i++, valueOrEmpty(whenRegistered));
            createCellWithValue(row, i++, valueOrEmpty(amountPayed));
            createCellWithValue(row, i++, valueOrEmpty(sibsEntityReferenceCode));
            createCellWithValue(row, i++, valueOrEmpty(sibsPaymentReferenceCode));
            createCellWithValue(row, i++, valueOrEmpty(sibsTransactionId));
            createCellWithValue(row, i++, valueOrEmpty(debtAccountId));
            createCellWithValue(row, i++, valueOrEmpty(customerId));
            createCellWithValue(row, i++, valueOrEmpty(businessIdentification));
            createCellWithValue(row, i++, valueOrEmpty(fiscalNumber));
            createCellWithValue(row, i++, valueOrEmpty(customerName));
            createCellWithValue(row, i++, valueOrEmpty(settlementDocumentNumber));
            createCellWithValue(row, i++, valueOrEmpty(comments));

        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(sibsTransactionDetail, e);
        }
    }

}
