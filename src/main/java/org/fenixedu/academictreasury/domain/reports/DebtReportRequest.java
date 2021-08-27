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
package org.fenixedu.academictreasury.domain.reports;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.dto.reports.AcademicActBlockingSuspensionReportEntryBean;
import org.fenixedu.academictreasury.dto.reports.DebtAccountReportEntryBean;
import org.fenixedu.academictreasury.dto.reports.DebtReportEntryBean;
import org.fenixedu.academictreasury.dto.reports.DebtReportRequestBean;
import org.fenixedu.academictreasury.dto.reports.PaymentReferenceCodeEntryBean;
import org.fenixedu.academictreasury.dto.reports.PaymentReportEntryBean;
import org.fenixedu.academictreasury.dto.reports.ProductReportEntryBean;
import org.fenixedu.academictreasury.dto.reports.SettlementReportEntryBean;
import org.fenixedu.academictreasury.dto.reports.SibsTransactionDetailEntryBean;
import org.fenixedu.academictreasury.dto.reports.TreasuryExemptionReportEntryBean;
import org.fenixedu.academictreasury.services.debtReports.DebtReportService;
import org.fenixedu.treasury.util.streaming.spreadsheet.ExcelSheet;
import org.fenixedu.treasury.util.streaming.spreadsheet.Spreadsheet;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class DebtReportRequest extends DebtReportRequest_Base {

    private static Logger logger = LoggerFactory.getLogger(DebtReportRequest.class);
    
    public static final String DOT = ".";
    public static final String COMMA = ",";

    protected DebtReportRequest() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setDomainRootForPendingReportRequests(FenixFramework.getDomainRoot());
    }

    protected DebtReportRequest(final DebtReportRequestBean bean) {
        this();

        this.setBeginDate(bean.getBeginDate());
        this.setEndDate(bean.getEndDate());
        this.setType(bean.getType());
        this.setDecimalSeparator(bean.getDecimalSeparator());
        this.setIncludeAnnuledEntries(bean.isIncludeAnnuledEntries());

        this.setIncludeExtraAcademicInfo(bean.isIncludeExtraAcademicInfo());
        this.setIncludeErpIntegrationInfo(bean.isIncludeErpIntegrationInfo());
        this.setIncludeSibsInfo(bean.isIncludeSibsInfo());
        this.setIncludeProductsInfo(bean.isIncludeProductsInfo());

        this.setDegreeType(bean.getDegreeType());
        this.setExecutionYear(bean.getExecutionYear());

        checkRules();
    }

    private void checkRules() {

        if (getBeginDate() == null) {
            throw new AcademicTreasuryDomainException("error.DebtReportRequest.beginDate.required");
        }

        if (getEndDate() == null) {
            throw new AcademicTreasuryDomainException("error.DebtReportRequest.endDate.required");
        }

        if (getType() == null) {
            throw new AcademicTreasuryDomainException("error.DebtReportRequest.type.required");
        }
        
        if(getEndDate().isBefore(getBeginDate())) {
            throw new AcademicTreasuryDomainException("error.DebtReportRequest.endDate.before.beginDate");
        }

        if (Strings.isNullOrEmpty(getDecimalSeparator())
                || (!getDecimalSeparator().equals(COMMA) && !getDecimalSeparator().equals(DOT))) {
            throw new AcademicTreasuryDomainException("error.DebtReportRequest.decimalSeparator.invalid");
        }

    }

    public boolean isPending() {
        return getDomainRootForPendingReportRequests() != null;
    }

    public boolean isIncludeAnnuledEntries() {
        return super.getIncludeAnnuledEntries();
    }

    public boolean isIncludeExtraAcademicInfo() {
        return getIncludeExtraAcademicInfo();
    }

    public boolean isIncludeErpIntegrationInfo() {
        return getIncludeErpIntegrationInfo();
    }

    public boolean isIncludeSibsInfo() {
        return getIncludeSibsInfo();
    }

    public boolean isIncludeProductsInfo() {
        return getIncludeProductsInfo();
    }

    @Atomic(mode = TxMode.READ)
    public void processRequest() {

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        final ErrorsLog errorsLog = new ErrorsLog();

        if (getType().isRequestForInvoiceEntries()) {
            executorService.submit(() -> {
                try {
                    extractInformationForDebitAndCredits(errorsLog, zos);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });

            executorService.submit(() -> {
                try {
                    extractInformationForSettlements(errorsLog, zos);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });

            executorService.submit(() -> {
                try {
                    extractInformationForPaymentCodesTransactions(errorsLog, zos);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });

        } else if (getType().isRequestForPaymentReferenceCodes()) {
            executorService.submit(() -> {
                try {
                    extractInformationForPaymentCodes(errorsLog, zos);
                    extractInformationForPaymentCodesTransactions(errorsLog, zos);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });
        } else if(getType().isRequestForOtherData()) {
            executorService.submit(() -> {
                try {
                    extractOtherTreasuryData(errorsLog, zos);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });
        }

        executorService.submit(() -> {
            logger.info("writeReportResultFile");

            try {
                zos.close();
                baos.close();

                byte[] contents = baos.toByteArray();
                writeReportResultFile(errorsLog, contents);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            } finally {
            }
        });

        executorService.shutdown();
        try {
            logger.info("awaitTermination");

            // TODO: This should be configured because the timeout depends in the size of data
            executorService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    @Atomic(mode = TxMode.WRITE)
    private void writeReportResultFile(final ErrorsLog errorsLog, final byte[] content) {
        DebtReportRequestResultFile.create(this, content);
        DebtReportRequestResultErrorsFile.create(this, errorsLog.getLog().getBytes());
        setDomainRootForPendingReportRequests(null);
    }

    @Atomic(mode = TxMode.READ)
    private void extractInformationForSettlements(final ErrorsLog errorsLog, ZipOutputStream zos) throws IOException {
        logger.info("START extractInformationForSettlements");
        zos.putNextEntry(new ZipEntry(academicTreasuryBundle("label.DebtReportRequestResultFile.SETTLEMENT_ENTRIES.filename",
                new DateTime().toString("YYYYMMddHHmmss"))));
        logger.info("CREATED extractInformationForSettlements");

        try {

            Spreadsheet.buildSpreadsheetContent(new Spreadsheet() {

                @Override
                public ExcelSheet[] getSheets() {
                    return new ExcelSheet[] {
                            ExcelSheet.create(settlementEntriesSheetName(), SettlementReportEntryBean.SPREADSHEET_HEADERS,
                                    DebtReportService.settlementEntriesReport(DebtReportRequest.this, errorsLog)),

                            ExcelSheet.create(paymentEntriesSheetName(), PaymentReportEntryBean.SPREADSHEET_HEADERS,
                                    DebtReportService.paymentEntriesReport(DebtReportRequest.this, errorsLog)),

                            ExcelSheet.create(reimbursementEntriesSheetName(), PaymentReportEntryBean.SPREADSHEET_HEADERS,
                                    DebtReportService.reimbursementEntriesReport(DebtReportRequest.this, errorsLog)) };
                }

            }, errorsLog, zos);
        } catch (Throwable e) {
            e.printStackTrace();

            throw new RuntimeException(e);
        }

        zos.closeEntry();
        logger.info("END extractInformationForSettlements");
    }

    @Atomic(mode = TxMode.READ)
    private void extractInformationForPaymentCodesTransactions(final ErrorsLog errorsLog, ZipOutputStream zos)
            throws IOException {
        logger.info("START extractInformationForPaymentCodeTransactions");

        zos.putNextEntry(new ZipEntry(academicTreasuryBundle("label.DebtReportRequestResultFile.PAYMENT_CODES.filename",
                new DateTime().toString("YYYYMMddHHmmss"))));

        Spreadsheet.buildSpreadsheetContent(new Spreadsheet() {

            @Override
            public ExcelSheet[] getSheets() {
                return new ExcelSheet[] {
                        ExcelSheet.create(sibsTransactionDetailSheetName(), SibsTransactionDetailEntryBean.SPREADSHEET_HEADERS,
                                DebtReportService.sibsTransactionDetailReport(DebtReportRequest.this, errorsLog)), };
            }
        }, errorsLog, zos);

        zos.closeEntry();
        logger.info("END extractInformationForPaymentCodeTransactions");
    }

    @Atomic(mode = TxMode.READ)
    private void extractInformationForPaymentCodes(final ErrorsLog errorsLog, ZipOutputStream zos) throws IOException {
        logger.info("START extractInformationForPaymentCodes");

        zos.putNextEntry(new ZipEntry(academicTreasuryBundle("label.DebtReportRequestResultFile.PAYMENT_CODES.filename",
                new DateTime().toString("YYYYMMddHHmmss"))));

        Spreadsheet.buildSpreadsheetContent(new Spreadsheet() {

            @Override
            public ExcelSheet[] getSheets() {
                return new ExcelSheet[] {
                        ExcelSheet.create(paymentReferenceCodeSheetName(), PaymentReferenceCodeEntryBean.SPREADSHEET_HEADERS,
                                DebtReportService.paymentReferenceCodeReport(DebtReportRequest.this, errorsLog)), };
            }
        }, errorsLog, zos);

        zos.closeEntry();
        logger.info("END extractInformationForPaymentCodes");
    }

    @Atomic(mode = TxMode.READ)
    private void extractOtherTreasuryData(final ErrorsLog errorsLog, ZipOutputStream zos) throws IOException {
        logger.info("START extractOtherTreasuryData");

        zos.putNextEntry(new ZipEntry(academicTreasuryBundle("label.DebtReportRequestResultFile.OTHER.filename",
                new DateTime().toString("YYYYMMddHHmmss"))));

        Spreadsheet.buildSpreadsheetContent(new Spreadsheet() {

            @Override
            public ExcelSheet[] getSheets() {
                return new ExcelSheet[] {
                        ExcelSheet.create(debtAccountEntriesSheetName(), DebtAccountReportEntryBean.SPREADSHEET_HEADERS,
                                DebtReportService.debtAccountEntriesReport(DebtReportRequest.this, errorsLog)),

                        ExcelSheet.create(academicActBlockingSuspensionSheetName(),
                                AcademicActBlockingSuspensionReportEntryBean.SPREADSHEET_HEADERS,
                                DebtReportService.academicActBlockingSuspensionReport(DebtReportRequest.this, errorsLog)),

                        ExcelSheet.create(treasuryExemptionSheetName(), TreasuryExemptionReportEntryBean.SPREADSHEET_HEADERS,
                                DebtReportService.treasuryExemptionReport(DebtReportRequest.this, errorsLog)),

                        ExcelSheet.create(productSheetName(), ProductReportEntryBean.SPREADSHEET_HEADERS,
                                DebtReportService.productReport(DebtReportRequest.this, errorsLog)) };
            };
        }, errorsLog, zos);

        zos.closeEntry();
        logger.info("END extractOtherTreasuryData");
    }

    @Atomic(mode = TxMode.READ)
    private void extractInformationForDebitAndCredits(final ErrorsLog errorsLog, ZipOutputStream zos) throws IOException {
        logger.info("START extractInformationForDebitAndCredits");

        zos.putNextEntry(new ZipEntry(academicTreasuryBundle("label.DebtReportRequestResultFile.INVOICE_ENTRIES.filename",
                new DateTime().toString("YYYYMMddHHmmss"))));
        logger.info("CREATED extractInformationForDebitAndCredits");

        Spreadsheet.buildSpreadsheetContent(new Spreadsheet() {

            @Override
            public ExcelSheet[] getSheets() {
                return new ExcelSheet[] {

                        ExcelSheet.create(debitEntriesSheetName(), DebtReportEntryBean.SPREADSHEET_DEBIT_HEADERS,
                                DebtReportService.debitEntriesReport(DebtReportRequest.this, errorsLog)),

                        ExcelSheet.create(creditEntriesSheetName(), DebtReportEntryBean.SPREADSHEET_CREDIT_HEADERS,
                                DebtReportService.creditEntriesReport(DebtReportRequest.this, errorsLog)) };
            }

            private String decimalSeparator() {
                if (Strings.isNullOrEmpty(getDecimalSeparator())) {
                    return DOT;
                }

                return getDecimalSeparator();
            }

        }, errorsLog, zos);

        logger.info("CLOSE extractInformationForDebitAndCredits");
        zos.closeEntry();
        logger.info("END extractInformationForDebitAndCredits");
    }

    @Atomic
    public void cancelRequest() {
        setDomainRootForPendingReportRequests(null);
    }

    protected String treasuryExemptionSheetName() {
        return academicTreasuryBundle("label.DebtReportRequest.treasuryExemptionSheetName");
    }

    protected String sibsTransactionDetailSheetName() {
        return academicTreasuryBundle("label.DebtReportRequest.sibsTransactionDetailSheetName");
    }

    protected String paymentReferenceCodeSheetName() {
        return academicTreasuryBundle("label.DebtReportRequest.paymentReferenceCodeSheetName");
    }

    private String academicActBlockingSuspensionSheetName() {
        return academicTreasuryBundle("label.DebtReportRequest.academicActBlockingSuspensionSheetName");
    }

    private String debitEntriesSheetName() {
        return academicTreasuryBundle("label.DebtReportRequest.debitEntriesSheetName");
    }

    private String creditEntriesSheetName() {
        return academicTreasuryBundle("label.DebtReportRequest.creditEntriesSheetName");
    }

    private String paymentEntriesSheetName() {
        return academicTreasuryBundle("label.DebtReportRequest.paymentEntriesSheetName");
    }

    private String settlementEntriesSheetName() {
        return academicTreasuryBundle("label.DebtReportRequest.settlementEntriesSheetName");
    }

    private String reimbursementEntriesSheetName() {
        return academicTreasuryBundle("label.DebtReportRequest.reimbursementEntriesSheetName");
    }

    private String debtAccountEntriesSheetName() {
        return academicTreasuryBundle("label.DebtReportRequest.debtAccountEntriesSheetName");
    }

    private String productSheetName() {
        return academicTreasuryBundle("label.DebtReportRequest.productsSheetName");
    }

    public static Stream<DebtReportRequest> findAll() {
        return FenixFramework.getDomainRoot().getDebtReportRequestsSet().stream();
    }

    public static Stream<DebtReportRequest> findPending() {
        return FenixFramework.getDomainRoot().getDebtReportRequestsSet().stream().filter(i -> i.isPending());
    }

    @Atomic
    public static DebtReportRequest create(final DebtReportRequestBean bean) {
        return new DebtReportRequest(bean);
    }

}
