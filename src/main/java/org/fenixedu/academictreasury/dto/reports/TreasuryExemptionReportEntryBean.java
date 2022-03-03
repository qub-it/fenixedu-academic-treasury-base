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

import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.academictreasury.domain.reports.DebtReportRequest;
import org.fenixedu.academictreasury.domain.reports.ErrorsLog;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;
import org.joda.time.DateTime;

public class TreasuryExemptionReportEntryBean extends AbstractReportEntryBean {

    public static String[] SPREADSHEET_HEADERS = {
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.identification"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.versioningCreator"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.creationDate"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.customerId"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.debtAccountId"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.customerName"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.debitEntryId"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.debitEntryDescription"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.exemptedAmount"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.reason") };

    private String identification;
    private String versioningCreator;
    private DateTime creationDate;
    private String customerId;
    private String customerName;
    private String debtAccountId;
    private String debitEntryId;
    private String debitEntryDescription;
    private String exemptedAmount;
    private String reason;

    private TreasuryExemption treasuryExemption;

    boolean completed = false;

    public TreasuryExemptionReportEntryBean(final TreasuryExemption treasuryExemption, final DebtReportRequest request, final ErrorsLog errorsLog) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        final String decimalSeparator = request.getDecimalSeparator();
        
        try {
            this.treasuryExemption = treasuryExemption;

            this.identification = treasuryExemption.getExternalId();
            this.versioningCreator = treasuryServices.versioningCreatorUsername(treasuryExemption);
            this.creationDate = treasuryServices.versioningCreationDate(treasuryExemption);
            this.customerId = treasuryExemption.getDebitEntry().getDebtAccount().getCustomer().getExternalId();
            this.customerName = treasuryExemption.getDebitEntry().getDebtAccount().getCustomer().getName();
            this.debtAccountId = treasuryExemption.getDebitEntry().getDebtAccount().getExternalId();
            this.debitEntryId = treasuryExemption.getDebitEntry().getExternalId();
            this.debitEntryDescription = treasuryExemption.getDebitEntry().getDescription();
            this.exemptedAmount =
                    treasuryExemption.getDebitEntry().getCurrency().getValueFor(treasuryExemption.getNetAmountToExempt());
            
            if(DebtReportRequest.COMMA.equals(decimalSeparator)) {
                this.exemptedAmount = this.exemptedAmount.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
            }
            
            this.reason = treasuryExemption.getReason();

            this.completed = true;
        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(treasuryExemption, e);
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

            row.createCell(i++).setCellValue(valueOrEmpty(versioningCreator));
            row.createCell(i++).setCellValue(valueOrEmpty(creationDate));
            row.createCell(i++).setCellValue(valueOrEmpty(customerId));
            row.createCell(i++).setCellValue(valueOrEmpty(debtAccountId));
            row.createCell(i++).setCellValue(valueOrEmpty(customerName));
            row.createCell(i++).setCellValue(valueOrEmpty(debitEntryId));
            row.createCell(i++).setCellValue(valueOrEmpty(debitEntryDescription));
            row.createCell(i++).setCellValue(valueOrEmpty(exemptedAmount));
            row.createCell(i++).setCellValue(valueOrEmpty(reason));

        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(this.treasuryExemption, e);
        }
    }

}
