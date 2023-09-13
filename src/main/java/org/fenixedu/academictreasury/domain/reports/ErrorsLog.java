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


import org.apache.commons.lang.exception.ExceptionUtils;
import org.fenixedu.academictreasury.domain.academicalAct.AcademicActBlockingSuspension;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.PaymentEntry;
import org.fenixedu.treasury.domain.document.ReimbursementEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentCodeTransaction;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;

public class ErrorsLog implements IErrorsLog {
    final StringBuffer sb = new StringBuffer();

    public void addError(final InvoiceEntry entry, final Exception e) {
        final String oid = entry.getExternalId();
        final String documentNumber =
                entry.getFinantialDocument() != null ? entry.getFinantialDocument().getUiDocumentNumber() : "";
        final String description = entry.getDescription();

        sb.append(String.format("[%s/%s] - '%s'\n%s\n\n", oid, documentNumber, description, ExceptionUtils.getFullStackTrace(e)));
    }

    public void addError(final SettlementEntry entry, final Exception e) {
        synchronized (this) {
            final String oid = entry.getExternalId();
            final String documentNumber =
                    entry.getFinantialDocument() != null ? entry.getFinantialDocument().getUiDocumentNumber() : "";
            final String description = entry.getDescription() != null ? entry.getDescription() : "";

            sb.append(String.format("[%s/%s] - '%s'\n%s\n\n", oid, documentNumber, description, ExceptionUtils.getFullStackTrace(e)));
        }
    }

    public void addError(final PaymentEntry entry, final Exception e) {
        synchronized (this) {
            final String oid = entry.getExternalId();
            final String documentNumber =
                    entry.getSettlementNote() != null ? entry.getSettlementNote().getUiDocumentNumber() : "";

            sb.append(String.format("[%s/%s]\n%s\n\n", oid, documentNumber, ExceptionUtils.getFullStackTrace(e)));

        }
    }

    public void addError(final ReimbursementEntry entry, final Exception e) {
        synchronized (this) {
            final String oid = entry.getExternalId();
            final String documentNumber =
                    entry.getSettlementNote() != null ? entry.getSettlementNote().getUiDocumentNumber() : "";

            sb.append(String.format("[%s/%s]\n%s\n\n", oid, documentNumber, ExceptionUtils.getFullStackTrace(e)));
        }
    }

    public void addError(final DebtAccount debtAccount, final Exception e) {
        synchronized (this) {
            final String oid = debtAccount.getExternalId();
            String identification = "N/A";
            String name = "N/A";
            try {
                identification = debtAccount.getCustomer().getIdentificationNumber();
                name = debtAccount.getCustomer().getName();
            } catch(Exception e2) {
            }

            sb.append(String.format("[%s/%s] - %s\n%s\n\n", oid, identification, name, ExceptionUtils.getFullStackTrace(e)));
        }
    }
    
    public void addError(final AcademicActBlockingSuspension academicActBlockingSuspension, final Exception e) {
        synchronized (this) {
            final String oid = academicActBlockingSuspension.getExternalId();
            final String name = academicActBlockingSuspension.getPerson().getName();

            sb.append(String.format("[%s] - %s\n%s\n\n", oid, name, ExceptionUtils.getFullStackTrace(e)));
        }        
    }
    
    public void addError(SibsPaymentRequest paymentRequest, Exception e) {
        synchronized (this) {
            final String oid = paymentRequest.getExternalId();
            final String referenceCode = paymentRequest.getReferenceCode();

            sb.append(String.format("[%s] - %s\n%s\n\n", oid, referenceCode, ExceptionUtils.getFullStackTrace(e)));
        }
    }
    
    public void addError(SibsPaymentCodeTransaction sibsTransactionDetail, Exception e) {
        synchronized (this) {
            final String oid = sibsTransactionDetail.getExternalId();
            final String referenceCode = sibsTransactionDetail.getSibsPaymentReferenceCode();

            sb.append(String.format("[%s] - %s\n%s\n\n", oid, referenceCode, ExceptionUtils.getFullStackTrace(e)));
        }        
    }
    
    public void addError(TreasuryExemption treasuryExemption, Exception e) {
        synchronized(this) {
            final String oid = treasuryExemption.getExternalId();
            
            sb.append(String.format("[%s]\n%s\n\n", oid, ExceptionUtils.getFullStackTrace(e)));
        }
    }

    public void addError(Product product, Exception e) {
        synchronized(this) {
            final String oid = product.getExternalId();

            sb.append(String.format("[%s]\n%s\n\n", oid, ExceptionUtils.getFullStackTrace(e)));
        }
    }
    
    public String getLog() {
        return sb.toString();
    }

}
