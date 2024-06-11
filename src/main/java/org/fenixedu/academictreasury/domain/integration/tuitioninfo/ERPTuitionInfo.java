/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, without modification, are permitted
 * provided that the following conditions are met:
 * <p>
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
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT” BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.academictreasury.domain.integration.tuitioninfo;

import static com.qubit.qubEdu.module.base.util.XLSxUtil.createTextCellWithValue;
import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.integration.ERPTuitionInfoCreationReportFile;
import org.fenixedu.academictreasury.domain.integration.ERPTuitionInfoExportOperation;
import org.fenixedu.academictreasury.domain.integration.tuitioninfo.exceptions.ERPTuitionInfoNoDifferencesException;
import org.fenixedu.academictreasury.domain.integration.tuitioninfo.exceptions.ERPTuitionInfoPendingException;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import pt.ist.fenixframework.FenixFramework;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Series;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.streaming.spreadsheet.ExcelSheet;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;
import org.fenixedu.treasury.util.streaming.spreadsheet.Spreadsheet;
import org.fenixedu.treasury.util.streaming.spreadsheet.SpreadsheetRow;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class ERPTuitionInfo extends ERPTuitionInfo_Base {

    public static Comparator<ERPTuitionInfo> COMPARE_BY_CREATION_DATE = new Comparator<ERPTuitionInfo>() {

        @Override
        public int compare(final ERPTuitionInfo o1, final ERPTuitionInfo o2) {
            int c = o1.getCreationDate().compareTo(o2.getCreationDate());
            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    public ERPTuitionInfo() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public ERPTuitionInfo(final Customer customer, final ERPTuitionInfoType erpTuitionInfoType,
            final BigDecimal tuitionTotalAmount, final BigDecimal deltaTuitionAmount, final LocalDate beginDate,
            final LocalDate endDate, final ERPTuitionInfo lastSucessfulSentErpTuitionInfo) {
        this();

        setCreationDate(new DateTime());
        setCustomer(customer);
        setErpTuitionInfoType(erpTuitionInfoType);
        setTuitionTotalAmount(tuitionTotalAmount);
        setTuitionDeltaAmount(deltaTuitionAmount);
        setBeginDate(beginDate);
        setEndDate(endDate);

        setLastSuccessfulSentERPTuitionInfo(lastSucessfulSentErpTuitionInfo);

        final Series series = ERPTuitionInfoSettings.getInstance().getSeries();
        DocumentNumberSeries documentNumberSeries = null;

        if (AcademicTreasuryConstants.isPositive(getTuitionDeltaAmount())) {
            documentNumberSeries = DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), series);
        } else if (AcademicTreasuryConstants.isNegative(getTuitionDeltaAmount())) {
            documentNumberSeries = DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(), series);
        }

        setDocumentNumberSeries(documentNumberSeries);

        this.setDocumentNumber("" + this.getDocumentNumberSeries().getSequenceNumberAndIncrement());

        setFirstERPTuitionInfo(findFirstIntegratedWithSuccess(this).orElse(null));

        checkRules();

        markToInfoExport();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.bennu.required");
        }

        if (getCreationDate() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.createDate.required");
        }

        if (getErpTuitionInfoType() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.erpTuitionInfoType.required");
        }

        if (getDocumentNumberSeries() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.documentNumberSeries.required");
        }

        if (Strings.isNullOrEmpty(getDocumentNumber())) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.documentNumber.required");
        }

        if (getTuitionTotalAmount() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.tuitionTotalAmount.required");
        }

        if (AcademicTreasuryConstants.isNegative(getTuitionTotalAmount())) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.tuitionTotalAmount.negative");
        }

        if (getTuitionDeltaAmount() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.tuitionDeltaAmount.required");
        }

        if (AcademicTreasuryConstants.isZero(getTuitionDeltaAmount())) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.tuitionDeltaAmount.cannot.be.zero");
        }

        if (getBeginDate() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.beginDate.required");
        }

        if (getEndDate() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.endDate.required");
        }

        if (getBeginDate().isAfter(getEndDate())) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.beginDate.after.endDate");
        }

        if (AcademicTreasuryConstants.isPositive(getTuitionDeltaAmount())
                && !getDocumentNumberSeries().getFinantialDocumentType().getType().isDebitNote()) {
            throw new AcademicTreasuryDomainException(
                    "error.ERPTuitionInfo.tuitionDeltaAmount.positive.but.finantialDocument.not.debit.note");
        }

        if (AcademicTreasuryConstants.isNegative(getTuitionDeltaAmount())
                && !getDocumentNumberSeries().getFinantialDocumentType().getType().isCreditNote()) {
            throw new AcademicTreasuryDomainException(
                    "error.ERPTuitionInfo.tuitionDeltaAmount.negative.but.finantialDocument.not.credit.note");
        }

        if (findPendingToExport(getCustomer(), getErpTuitionInfoType()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.pending.to.export.already.exists");
        }

        if (isSubsequent() ^ isFollowedBySuccessfulSent()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.first.and.last.successful.sent.incoerent");

        }
    }

    public String getUiDocumentNumber() {
        return String.format("%s %s/%s", this.getDocumentNumberSeries().documentNumberSeriesPrefix(),
                this.getDocumentNumberSeries().getSeries().getCode(), Strings.padStart(this.getDocumentNumber(), 7, '0'));
    }

    public boolean isDebit() {
        return getDocumentNumberSeries().getFinantialDocumentType().getType().isDebitNote();
    }

    public boolean isCredit() {
        return getDocumentNumberSeries().getFinantialDocumentType().getType().isCreditNote();
    }

    public String getUiSettlementDocumentNumberForERP() {
        return getUiDocumentNumber().replaceAll(this.getDocumentNumberSeries().documentNumberSeriesPrefix(),
                FinantialDocumentType.findForSettlementNote().getDocumentNumberSeriesPrefix());
    }

    public boolean isPendingToExport() {
        return getDomainRootPendingToExport() != null;
    }

    public boolean isExportationSuccess() {
        return getExportationSuccess();
    }

    public boolean isSubsequent() {
        return getFirstERPTuitionInfo() != null;
    }

    public boolean isFollowedBySuccessfulSent() {
        return getLastSuccessfulSentERPTuitionInfo() != null;
    }

    public void export() {
        ERPTuitionInfoSettings.getInstance().exporter().export(this);
    }

    @Atomic
    public void markToInfoExport() {
        setDomainRootPendingToExport(FenixFramework.getDomainRoot());
    }

    public void markIntegratedWithSuccess(final String message) {
        setDomainRootPendingToExport(null);
        setExportationMessage(message);
        setExportationSuccess(true);

        checkRules();
    }

    private void cancelExportation(final String reason) {
        setDomainRootPendingToExport(null);
        setExportationMessage(reason);
        setExportationSuccess(false);

        checkRules();
    }

    public void editPendingToExport(final BigDecimal tuitionTotalAmount, final BigDecimal tuitionDeltaAmount,
            final LocalDate beginDate, final LocalDate endDate) {
        if (!isPendingToExport()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.editPendingToExport.already.exported");
        }

        setTuitionTotalAmount(tuitionTotalAmount);
        setTuitionDeltaAmount(tuitionDeltaAmount);
        setBeginDate(beginDate);
        setEndDate(endDate);

        checkRules();
    }

    public Optional<ERPTuitionInfoExportOperation> getLastERPExportOperation() {
        if (getErpTuitionInfoExportOperationsSet().isEmpty()) {
            return Optional.empty();
        }

        return getErpTuitionInfoExportOperationsSet().stream()
                .sorted(ERPTuitionInfoExportOperation.COMPARE_BY_VERSIONING_CREATION_DATE.reversed()).findFirst();
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<ERPTuitionInfo> findAll() {
        return FenixFramework.getDomainRoot().getErpTuitionInfosSet().stream();
    }

    public static Stream<ERPTuitionInfo> find(final Customer customer) {
        return customer.getErpTuitionInfosSet().stream();
    }

    public static Stream<ERPTuitionInfo> find(final Customer customer, final ERPTuitionInfoType type) {
        return find(customer).filter(i -> i.getErpTuitionInfoType() == type);
    }

    public static Optional<ERPTuitionInfo> findUniqueByDocumentNumber(final String documentNumber) {
        return findAll().filter(e -> documentNumber.equals(e.getUiDocumentNumber())).findFirst();
    }

    public static Stream<ERPTuitionInfo> findPendingToExport(final Customer customer, final ERPTuitionInfoType type) {
        return find(customer, type).filter(i -> i.isPendingToExport());
    }

    public static Optional<ERPTuitionInfo> findUniquePendingToExport(final Customer customer, final ERPTuitionInfoType type) {
        return findPendingToExport(customer, type).findFirst();
    }

    public static Optional<ERPTuitionInfo> findFirstIntegratedWithSuccess(final Customer customer,
            final ERPTuitionInfoType type) {
        return find(customer, type).filter(t -> t.isExportationSuccess()).sorted(COMPARE_BY_CREATION_DATE).findFirst();
    }

    public static Optional<ERPTuitionInfo> findFirstIntegratedWithSuccess(final ERPTuitionInfo erpTuitionInfo) {
        return findFirstIntegratedWithSuccess(erpTuitionInfo.getCustomer(), erpTuitionInfo.getErpTuitionInfoType());
    }

    public static Optional<ERPTuitionInfo> findLastIntegratedWithSuccess(final Customer customer, final ERPTuitionInfoType type) {
        return find(customer, type).filter(t -> t.isExportationSuccess()).sorted(COMPARE_BY_CREATION_DATE.reversed()).findFirst();
    }

    private static ERPTuitionInfo create(final Customer customer, final ERPTuitionInfoType type,
            final BigDecimal tuitionTotalAmount, final BigDecimal deltaTuitionAmount, final LocalDate beginDate,
            final LocalDate endDate, final ERPTuitionInfo lastSucessfulSentErpTuitionInfo) {

        if (findUniquePendingToExport(customer, type).isPresent()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.pending.to.export.already.exists");
        }

        return new ERPTuitionInfo(customer, type, tuitionTotalAmount, deltaTuitionAmount, beginDate, endDate,
                lastSucessfulSentErpTuitionInfo);
    }

    private static class ERPTuitionInfoCalculationReportEntry implements SpreadsheetRow {
        private String executionDate;

        private String studentNumber;
        private String studentName;
        private String customerFiscalNumber;

        private String erpTuitionInfoTypeCode;
        private String erpTuitionInfoTypeName;
        private String executionYearQualifiedName;

        private String erpTuitionInfoExternalId;
        private String erpTuitionInfoCreationDate;
        private String erpTuitionInfoUpdateDate;
        private String erpTuitionInfoDocumentNumber;
        private String totalAmount;
        private String deltaAmount;

        private String errorOccured;
        private String errorDescription;

        @Override
        public void writeCellValues(final Row row, final IErrorsLog log) {
            int i = 0;

            createTextCellWithValue(row, i++, executionDate);

            createTextCellWithValue(row, i++, studentNumber);
            createTextCellWithValue(row, i++, studentName);
            createTextCellWithValue(row, i++, customerFiscalNumber);

            createTextCellWithValue(row, i++, erpTuitionInfoTypeCode);
            createTextCellWithValue(row, i++, erpTuitionInfoTypeName);
            createTextCellWithValue(row, i++, executionYearQualifiedName);

            createTextCellWithValue(row, i++, erpTuitionInfoExternalId);
            createTextCellWithValue(row, i++, erpTuitionInfoCreationDate);
            createTextCellWithValue(row, i++, erpTuitionInfoUpdateDate);
            createTextCellWithValue(row, i++, erpTuitionInfoDocumentNumber);
            createTextCellWithValue(row, i++, totalAmount);
            createTextCellWithValue(row, i++, deltaAmount);

            createTextCellWithValue(row, i++, errorOccured);
            createTextCellWithValue(row, i++, errorDescription);
        }
    }

    public static void triggerTuitionInfoCalculation(Predicate<ERPTuitionInfoType> erpTuitionInfoTypeFilterPredicate,
            Predicate<PersonCustomer> personCustomerPredicate) {
        if (!ERPTuitionInfoSettings.getInstance().isExportationActive()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.exportation.active.disabled");
        }

        if (erpTuitionInfoTypeFilterPredicate == null) {
            erpTuitionInfoTypeFilterPredicate = t -> true;
        }

        if (personCustomerPredicate == null) {
            personCustomerPredicate = t -> true;
        }

        List<Callable<ERPTuitionInfo>> callablesList = Lists.newArrayList();

        final List<ERPTuitionInfoCalculationReportEntry> reportEntries = Collections.synchronizedList(Lists.newArrayList());
        for (final ExecutionYear executionYear : ERPTuitionInfoSettings.getInstance().getActiveExecutionYearsSet()) {
            for (final ERPTuitionInfoType type : ERPTuitionInfoType.findActiveForExecutionYear(executionYear)
                    .collect(Collectors.toSet())) {

                if (!erpTuitionInfoTypeFilterPredicate.test(type)) {
                    continue;
                }

                for (final PersonCustomer customer : PersonCustomer.findAll().collect(Collectors.<PersonCustomer> toSet())) {
                    if (customer.getAssociatedPerson().getStudent() == null) {
                        continue;
                    }

                    if (!personCustomerPredicate.test(customer)) {
                        continue;
                    }

                    callablesList.add(createTuitionInformationCallable(customer, type, reportEntries));
                }
            }
        }

        try {
            final ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.invokeAll(callablesList);
            executor.shutdown();
            executor.awaitTermination(3, TimeUnit.HOURS);

        } catch (final InterruptedException e) {
        } finally {
            writeSpreadsheet(reportEntries);
        }
    }

    private static void writeSpreadsheet(final List<ERPTuitionInfoCalculationReportEntry> reportEntries) {
        final Spreadsheet spreadsheet = new Spreadsheet() {

            @Override
            public ExcelSheet[] getSheets() {
                return new ExcelSheet[] { new ExcelSheet() {

                    @Override
                    public Stream<? extends SpreadsheetRow> getRows() {
                        return reportEntries.stream();
                    }

                    @Override
                    public String getName() {
                        return academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.sheet.name");
                    }

                    @Override
                    public String[] getHeaders() {
                        return new String[] { academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.executionDate"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.studentNumber"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.studentName"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.customerFiscalNumber"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.erpTuitionInfoTypeCode"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.erpTuitionInfoTypeName"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.executionYearQualifiedName"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.erpTuitionInfoExternalId"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.erpTuitionInfoCreationDate"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.erpTuitionInfoUpdateDate"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.erpTuitionInfoDocumentNumber"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.totalAmount"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.deltaAmount"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.errorOccured"),
                                academicTreasuryBundle("label.ERPTuitionInfoCalculationReportEntry.errorDescription") };
                    }
                } };
            }
        };

        final byte[] spreadsheetContent = Spreadsheet.buildSpreadsheetContent(spreadsheet, null);

        final DateTime now = new DateTime();
        final String filename =
                academicTreasuryBundle("label.ERPTuitionInfoCreationReportFile.filename", now.toString("yyyyMMddHHmmss"));
        ERPTuitionInfoCreationReportFile.create(filename, filename, spreadsheetContent);

    }

    public static void triggerTuitionExportationToERP(Predicate<ERPTuitionInfo> erpTuitionInfoPredicate) {
        if (!ERPTuitionInfoSettings.getInstance().isExportationActive()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.exportation.active.disabled");
        }

        if (erpTuitionInfoPredicate == null) {
            erpTuitionInfoPredicate = t -> true;
        }

        final List<Callable<ERPTuitionInfo>> callablesList = Lists.newArrayList();
        for (ERPTuitionInfo info : ERPTuitionInfo.findPendingToExport().collect(Collectors.toSet())) {
            if (erpTuitionInfoPredicate.test(info)) {
                callablesList.add(exportTuitionInformationCallable(info));
            }
        }

        try {
            final ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.invokeAll(callablesList);
            executor.shutdown();
            executor.awaitTermination(3, TimeUnit.HOURS);
        } catch (final InterruptedException e) {
        }
    }

    private static Stream<ERPTuitionInfo> findPendingToExport() {
        return FenixFramework.getDomainRoot().getErpTuitionInfosPendingToExportSet().stream();
    }

    @Atomic(mode = TxMode.WRITE)
    public static ERPTuitionInfo exportTuitionInformation(final PersonCustomer customer, final ERPTuitionInfoType type) {

        if (!ERPTuitionInfoSettings.getInstance().isExportationActive()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.exportation.active.disabled");
        }

        final ExecutionYear executionYear = type.getExecutionYear();

        BigDecimal totalAmount = BigDecimal.ZERO;

        final Set<AcademicTreasuryEvent> treasuryEventsSet = AcademicTreasuryEvent.find(customer.getAssociatedPerson())
                .filter(e -> e.getExecutionYear() == executionYear).collect(Collectors.<AcademicTreasuryEvent> toSet());

        academicTreasuryEventLoop: for (final AcademicTreasuryEvent event : treasuryEventsSet) {

            for (final ERPTuitionInfoTypeAcademicEntry academicEntry : type.getErpTuitionInfoTypeAcademicEntriesSet()) {
                if (academicEntry.isAppliedOnAcademicTreasuryEvent(event, executionYear)) {
                    totalAmount = totalAmount.add(event.getAmountWithVatToPay(customer))
                            .subtract(event.getInterestsAmountToPay(customer, null));
                    continue academicTreasuryEventLoop;
                }
            }
        }

        final Optional<ERPTuitionInfo> lastIntegratedWithSuccess = findLastIntegratedWithSuccess(customer, type);
        final BigDecimal deltaAmount = totalAmount.subtract(lastIntegratedWithSuccess.isPresent() ? lastIntegratedWithSuccess
                .get().getTuitionTotalAmount() : BigDecimal.ZERO);

        if (findUniquePendingToExport(customer, type).isPresent()) {
            final ERPTuitionInfo pendingErpTuitionInfo = findUniquePendingToExport(customer, type).get();

            throw new ERPTuitionInfoPendingException("error.ERPTuitionInfo.pending.to.export",
                    pendingErpTuitionInfo.getUiDocumentNumber());
        }

        if (AcademicTreasuryConstants.isZero(deltaAmount)) {
            throw new ERPTuitionInfoNoDifferencesException("error.ErpTuitionInfo.no.differences.from.last.successul.exportation");
        }

        return ERPTuitionInfo.create(customer, type, totalAmount, deltaAmount, executionYear.getBeginLocalDate(),
                executionYear.getEndLocalDate(), lastIntegratedWithSuccess.orElse(null));
    }

    protected static Callable<ERPTuitionInfo> createTuitionInformationCallable(final PersonCustomer customer,
            final ERPTuitionInfoType type, final List<ERPTuitionInfoCalculationReportEntry> reportEntries) {
        return new Callable<ERPTuitionInfo>() {

            private String customerId = customer.getExternalId();
            private String erpTuitionInfoTypeId = type.getExternalId();

            @Override
            @Atomic(mode = TxMode.READ)
            public ERPTuitionInfo call() throws Exception {
                if (!ERPTuitionInfoSettings.getInstance().isExportationActive()) {
                    throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.exportation.active.disabled");
                }

                ERPTuitionInfoCalculationReportEntry reportEntry = new ERPTuitionInfoCalculationReportEntry();
                reportEntries.add(reportEntry);

                try {
                    final PersonCustomer c = FenixFramework.getDomainObject(customerId);
                    final ERPTuitionInfoType t = FenixFramework.getDomainObject(erpTuitionInfoTypeId);

                    reportEntry.executionDate = new DateTime().toString(AcademicTreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD);

                    reportEntry.studentNumber = c.getAssociatedPerson().getStudent().getNumber().toString();
                    reportEntry.studentName = c.getName();
                    reportEntry.customerFiscalNumber = c.getUiFiscalNumber();

                    reportEntry.erpTuitionInfoTypeCode = t.getErpTuitionInfoProduct().getCode();
                    reportEntry.erpTuitionInfoTypeName = t.getErpTuitionInfoProduct().getName();
                    reportEntry.executionYearQualifiedName = t.getExecutionYear().getQualifiedName();

                    final ERPTuitionInfo erpTuitionInfo = exportTuitionInformation(c, t);

                    reportEntry.erpTuitionInfoExternalId = erpTuitionInfo.getExternalId();
                    reportEntry.erpTuitionInfoCreationDate =
                            TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(erpTuitionInfo)
                                    .toString(AcademicTreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD);

                    final DateTime versioningUpdateDate =
                            TreasuryPlataformDependentServicesFactory.implementation().versioningUpdateDate(erpTuitionInfo);
                    reportEntry.erpTuitionInfoUpdateDate = versioningUpdateDate != null ? versioningUpdateDate
                            .toString(AcademicTreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD) : "";
                    reportEntry.erpTuitionInfoDocumentNumber = erpTuitionInfo.getUiDocumentNumber();

                    reportEntry.totalAmount = erpTuitionInfo.getTuitionTotalAmount().toString();
                    reportEntry.deltaAmount = erpTuitionInfo.getTuitionDeltaAmount().toString();

                    return erpTuitionInfo;
                } catch (final ERPTuitionInfoNoDifferencesException e) {
                    reportEntries.remove(reportEntry);

                    throw e;
                } catch (final AcademicTreasuryDomainException e) {
                    reportEntry.errorOccured = Boolean.TRUE.toString();
                    reportEntry.errorDescription = e.getLocalizedMessage();

                    throw e;
                } catch (final TreasuryDomainException e) {
                    reportEntry.errorOccured = Boolean.TRUE.toString();
                    reportEntry.errorDescription = e.getLocalizedMessage();

                    throw e;
                } catch (final Throwable e) {
                    reportEntry.errorOccured = Boolean.TRUE.toString();
                    reportEntry.errorDescription = e.getClass().getSimpleName() + " - " + e.getMessage();

                    final List<String> exceptionStackTraceList =
                            Lists.newArrayList(ExceptionUtils.getFullStackTrace(e).split("\n"));
                    reportEntry.errorDescription += "\n" + String.join("\n",
                            exceptionStackTraceList.subList(0, Integer.min(exceptionStackTraceList.size(), 5)));

                    throw e;
                }

            }
        };
    }

    protected static Callable<ERPTuitionInfo> exportTuitionInformationCallable(final ERPTuitionInfo erpTuitionInfo) {
        if (!ERPTuitionInfoSettings.getInstance().isExportationActive()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.exportation.active.disabled");
        }

        return new Callable<ERPTuitionInfo>() {
            private String erpTuitionInfoId = erpTuitionInfo.getExternalId();

            @Override
            @Atomic(mode = TxMode.READ)
            public ERPTuitionInfo call() throws Exception {
                final ERPTuitionInfo info = FenixFramework.getDomainObject(erpTuitionInfoId);

                info.export();
                return info;
            }
        };
    }

}
