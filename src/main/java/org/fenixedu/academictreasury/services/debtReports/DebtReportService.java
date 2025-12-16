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
package org.fenixedu.academictreasury.services.debtReports;

import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academictreasury.domain.academicalAct.AcademicActBlockingSuspension;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.reports.DebtReportRequest;
import org.fenixedu.academictreasury.domain.reports.ErrorsLog;
import org.fenixedu.academictreasury.dto.reports.AcademicActBlockingSuspensionReportEntryBean;
import org.fenixedu.academictreasury.dto.reports.DebtAccountReportEntryBean;
import org.fenixedu.academictreasury.dto.reports.DebtReportEntryBean;
import org.fenixedu.academictreasury.dto.reports.PaymentReferenceCodeEntryBean;
import org.fenixedu.academictreasury.dto.reports.PaymentReportEntryBean;
import org.fenixedu.academictreasury.dto.reports.ProductReportEntryBean;
import org.fenixedu.academictreasury.dto.reports.ReimbursementReportEntryBean;
import org.fenixedu.academictreasury.dto.reports.SettlementReportEntryBean;
import org.fenixedu.academictreasury.dto.reports.SibsTransactionDetailEntryBean;
import org.fenixedu.academictreasury.dto.reports.TreasuryExemptionReportEntryBean;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.PaymentEntry;
import org.fenixedu.treasury.domain.document.ReimbursementEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentCodeTransaction;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.paymentcodes.SibsTransactionDetail;
import org.fenixedu.treasury.services.integration.FenixEDUTreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

public class DebtReportService {

    public static Stream<DebtReportEntryBean> debitEntriesReport(final DebtReportRequest request, final ErrorsLog log) {
        return DebitEntry.findAll()
                .filter(i -> AcademicTreasuryConstants.isDateBetween(request.getBeginDate(), request.getEndDate(),
                        FenixEDUTreasuryPlatformDependentServices.getVersioningCreationDate(i)))
                .filter(i -> request.isIncludeAnnuledEntries() || !i.isAnnulled())
                .filter(i -> request.getDegreeType() == null || request.getDegreeType() == degreeType(i))
                .filter(i -> request.getExecutionYear() == null || request.getExecutionYear() == executionYear(i))
                .map(i -> new DebtReportEntryBean(i, request, log));
    }

    public static Stream<DebtReportEntryBean> creditEntriesReport(final DebtReportRequest request, final ErrorsLog log) {
        return CreditEntry.findAll()
                .filter(i -> AcademicTreasuryConstants.isDateBetween(request.getBeginDate(), request.getEndDate(),
                        FenixEDUTreasuryPlatformDependentServices.getVersioningCreationDate(i)))
                .filter(i -> request.isIncludeAnnuledEntries() || !i.isAnnulled())
                .filter(i -> request.getDegreeType() == null || request.getDegreeType() == degreeType(i))
                .filter(i -> request.getExecutionYear() == null || request.getExecutionYear() == executionYear(i))
                .map(i -> new DebtReportEntryBean(i, request, log));
    }

    public static Stream<SettlementReportEntryBean> settlementEntriesReport(final DebtReportRequest request,
            final ErrorsLog log) {
        return SettlementEntry.findAll()
                .filter(i -> AcademicTreasuryConstants.isDateBetween(request.getBeginDate(), request.getEndDate(),
                        i.getFinantialDocument().getDocumentDate()))
                .filter(i -> request.isIncludeAnnuledEntries() || !i.isAnnulled())
                .map(i -> new SettlementReportEntryBean(i, request, log));
    }

    public static Stream<PaymentReportEntryBean> paymentEntriesReport(final DebtReportRequest request, final ErrorsLog log) {
        return PaymentEntry.findAll()
                .filter(i -> AcademicTreasuryConstants.isDateBetween(request.getBeginDate(), request.getEndDate(),
                        i.getSettlementNote().getDocumentDate()))
                .filter(i -> request.isIncludeAnnuledEntries() || !i.getSettlementNote().isAnnulled())
                .map(i -> new PaymentReportEntryBean(i, request, log));
    }

    public static Stream<ReimbursementReportEntryBean> reimbursementEntriesReport(final DebtReportRequest request,
            final ErrorsLog log) {
        return ReimbursementEntry.findAll()
                .filter(i -> AcademicTreasuryConstants.isDateBetween(request.getBeginDate(), request.getEndDate(),
                        i.getSettlementNote().getDocumentDate()))
                .filter(i -> request.isIncludeAnnuledEntries() || !i.getSettlementNote().isAnnulled())
                .map(i -> new ReimbursementReportEntryBean(i, request, log));
    }

    public static Stream<DebtAccountReportEntryBean> debtAccountEntriesReport(final DebtReportRequest request,
            final ErrorsLog log) {
        return DebtAccount.findAll().map(i -> new DebtAccountReportEntryBean(i, request, log));
    }

    public static Stream<AcademicActBlockingSuspensionReportEntryBean> academicActBlockingSuspensionReport(
            final DebtReportRequest request, final ErrorsLog log) {
        return AcademicActBlockingSuspension.findAll().map(i -> new AcademicActBlockingSuspensionReportEntryBean(i, log));
    }

    public static Stream<PaymentReferenceCodeEntryBean> paymentReferenceCodeReport(DebtReportRequest request, ErrorsLog log) {
        return SibsPaymentRequest.findAll().filter(i -> request.isIncludeAnnuledEntries() || !i.isInAnnuledState())
                .map(i -> new PaymentReferenceCodeEntryBean(i, request, log));
    }

    public static Stream<SibsTransactionDetailEntryBean> sibsTransactionDetailReport(final DebtReportRequest request,
            final ErrorsLog log) {
        return SibsPaymentCodeTransaction.findAll()
                .filter(i -> request.getBeginDate() == null || (i.getPaymentDate() != null && !request.getBeginDate()
                        .toDateTimeAtStartOfDay().isAfter(i.getPaymentDate())))
                .filter(i -> request.getEndDate() == null || (i.getPaymentDate() != null && !request.getEndDate()
                        .toDateTimeAtStartOfDay().plusDays(1).minusSeconds(1).isBefore(i.getPaymentDate())))
                .map(i -> new SibsTransactionDetailEntryBean(i, request, log));
    }

    public static Stream<TreasuryExemptionReportEntryBean> treasuryExemptionReport(final DebtReportRequest request,
            final ErrorsLog log) {
        return TreasuryExemption.findAll()
                .filter(i -> i.getDebitEntry() != null && AcademicTreasuryConstants.isDateBetween(request.getBeginDate(),
                        request.getEndDate(), i.getDebitEntry().getEntryDateTime()))
                .filter(i -> request.getDegreeType() == null || request.getDegreeType() == degreeType(i))
                .filter(i -> request.getExecutionYear() == null || request.getExecutionYear() == executionYear(i))
                .map(i -> new TreasuryExemptionReportEntryBean(i, request, log));
    }

    public static Stream<ProductReportEntryBean> productReport(final DebtReportRequest request, final ErrorsLog log) {
        return Product.findAll().map(i -> new ProductReportEntryBean(i, request, log));
    }

    private static ExecutionYear executionYear(final DebitEntry debitEntry) {
        if (debitEntry.getTreasuryEvent() == null) {
            return null;
        }

        if (!(debitEntry.getTreasuryEvent() instanceof AcademicTreasuryEvent)) {
            return null;
        }

        return ((AcademicTreasuryEvent) debitEntry.getTreasuryEvent()).getExecutionYear();
    }

    private static DegreeType degreeType(final DebitEntry debitEntry) {
        if (debitEntry.getTreasuryEvent() == null) {
            return null;
        }

        if (!(debitEntry.getTreasuryEvent() instanceof AcademicTreasuryEvent)) {
            return null;
        }

        if (((AcademicTreasuryEvent) debitEntry.getTreasuryEvent()).getRegistration() == null) {
            return null;
        }

        return ((AcademicTreasuryEvent) debitEntry.getTreasuryEvent()).getRegistration().getDegreeType();
    }

    private static ExecutionYear executionYear(final CreditEntry creditEntry) {
        if (creditEntry.getDebitEntry() == null) {
            return null;
        }

        return executionYear(creditEntry.getDebitEntry());
    }

    private static DegreeType degreeType(final CreditEntry creditEntry) {
        if (creditEntry.getDebitEntry() == null) {
            return null;
        }

        return degreeType(creditEntry.getDebitEntry());
    }

    private static ExecutionYear executionYear(final TreasuryExemption exemption) {
        return executionYear(exemption.getDebitEntry());
    }

    private static DegreeType degreeType(final TreasuryExemption exemption) {
        return degreeType(exemption.getDebitEntry());
    }

}
