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

import static com.qubit.qubEdu.module.base.util.XLSxUtil.*;
import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.academictreasury.domain.reports.DebtReportRequest;
import org.fenixedu.academictreasury.domain.reports.ErrorsLog;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;

public class ProductReportEntryBean extends AbstractReportEntryBean {

    // @formatter:off
    public static final String[] SPREADSHEET_HEADERS = { 
            academicTreasuryBundle("label.ProductReportEntryBean.header.identification"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.group.code"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.group"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.code"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.description.pt"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.description.en"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.unitOfMeasure.pt"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.unitOfMeasure.en"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.active"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.legacy"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.tuitionInstallmentOrder"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.vatType.code"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.vatType"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.exemptionReason.code"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.exemptionReason"),
            academicTreasuryBundle("label.ProductReportEntryBean.header.finantialInstitution") };
    // @formatter:on

    private String identification;
    private String groupCode;
    private String group;
    private String code;
    private String descriptionPt;
    private String descriptionEn;
    private String unitOfMeasurePt;
    private String unitOfMeasureEn;
    private boolean active;
    private boolean legacy;
    private int tuitionInstallmentOrder;
    private String vatTypeCode;
    private String vatType;
    private String exemptionReasonCode;
    private String exemptionReason;
    private String finantialInstitution;

    private Product product;

    boolean completed = false;

    public ProductReportEntryBean(final Product p, final DebtReportRequest request, final ErrorsLog errorsLog) {
        this.product = p;

        try {
            this.identification = p.getExternalId();
            this.groupCode = p.getProductGroup() != null ? p.getProductGroup().getCode() : "";
            this.group = p.getProductGroup().getName().getContent();
            this.code = p.getCode();
            this.descriptionPt = p.getName().getContent(AcademicTreasuryConstants.DEFAULT_LANGUAGE);
            this.descriptionEn = p.getName().getContent(AcademicTreasuryConstants.ENGLISH_LANGUAGE);
            this.unitOfMeasurePt = p.getUnitOfMeasure().getContent(AcademicTreasuryConstants.DEFAULT_LANGUAGE);
            this.unitOfMeasureEn = p.getUnitOfMeasure().getContent(AcademicTreasuryConstants.ENGLISH_LANGUAGE);
            this.active = p.isActive();
            this.legacy = p.isLegacy();
            this.tuitionInstallmentOrder = p.getTuitionInstallmentOrder();
            this.vatTypeCode = p.getVatType() != null ? p.getVatType().getCode() : "";
            this.vatType = p.getVatType() != null ? p.getVatType().getName().getContent() : "";
            this.exemptionReasonCode = p.getVatExemptionReason() != null ? p.getVatExemptionReason().getCode() : "";
            this.exemptionReason = p.getVatExemptionReason() != null ? p.getVatExemptionReason().getName().getContent() : "";
            this.finantialInstitution = !p.getFinantialInstitutionsSet().isEmpty() ? p.getFinantialInstitutionsSet().iterator()
                    .next().getFiscalNumber() : "";

            this.completed = true;
        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(this.product, e);
        }
    }

    @Override
    public void writeCellValues(final Row row, final IErrorsLog ierrorsLog) {
        final ErrorsLog errorsLog = (ErrorsLog) ierrorsLog;

        try {
            createTextCellWithValue(row, 0, identification);

            if (!completed) {
                createTextCellWithValue(row, 1, academicTreasuryBundle("error.DebtReportEntryBean.report.generation.verify.entry"));
                return;
            }

            int i = 1;

            createTextCellWithValue(row, i++, valueOrEmpty(groupCode));
            createTextCellWithValue(row, i++, valueOrEmpty(group));
            createTextCellWithValue(row, i++, valueOrEmpty(code));
            createTextCellWithValue(row, i++, valueOrEmpty(descriptionPt));
            createTextCellWithValue(row, i++, valueOrEmpty(descriptionEn));
            createTextCellWithValue(row, i++, valueOrEmpty(unitOfMeasurePt));
            createTextCellWithValue(row, i++, valueOrEmpty(unitOfMeasureEn));
            createTextCellWithValue(row, i++, valueOrEmpty(active));
            createTextCellWithValue(row, i++, valueOrEmpty(legacy));
            createTextCellWithValue(row, i++, valueOrEmpty(tuitionInstallmentOrder));
            createTextCellWithValue(row, i++, valueOrEmpty(vatTypeCode));
            createTextCellWithValue(row, i++, valueOrEmpty(vatType));
            createTextCellWithValue(row, i++, valueOrEmpty(exemptionReasonCode));
            createTextCellWithValue(row, i++, valueOrEmpty(exemptionReason));
            createTextCellWithValue(row, i++, valueOrEmpty(finantialInstitution));
            
        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(this.product, e);
        }
    }

}
