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
package org.fenixedu.academictreasury.domain.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.treasury.AcademicTreasuryEventPayment;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEventPayment;
import org.fenixedu.academic.domain.treasury.IPaymentReferenceCode;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

public class TreasuryEventDefaultMethods {

    private static class PaymentReferenceCodeImpl implements IPaymentReferenceCode {

        private final SibsPaymentRequest paymentReferenceCode;

        private PaymentReferenceCodeImpl(final SibsPaymentRequest referenceCode) {
            this.paymentReferenceCode = referenceCode;

        }

        @Override
        public LocalDate getEndDate() {
            return paymentReferenceCode.getDueDate();
        }

        @Override
        public String getEntityCode() {
            return paymentReferenceCode.getDigitalPaymentPlatform().getSibsPaymentCodePoolService().getEntityReferenceCode();
        }

        @Override
        public String getFormattedCode() {
            return paymentReferenceCode.getFormattedCode();
        }

        @Override
        public String getReferenceCode() {
            return paymentReferenceCode.getReferenceCode();
        }

        @Override
        public boolean isAnnuled() {
            return paymentReferenceCode.getState().isAnnuled();
        }

        @Override
        public boolean isUsed() {
            return paymentReferenceCode.getState().isUsed();
        }

        @Override
        public boolean isProcessed() {
            return paymentReferenceCode.getState().isProcessed();
        }

        @Override
        public BigDecimal getPayableAmount() {
            return paymentReferenceCode.getPayableAmount();
        }

    }

    public static void annulDebts(final TreasuryEvent event, final String reason) {
        event.annulAllDebitEntries(reason);
    }

    public static String formatMoney(final TreasuryEvent event, final BigDecimal moneyValue) {
        if (DebitEntry.findActive(event).findFirst().isPresent()) {
            return DebitEntry.findActive(event).findFirst().get().getDebtAccount().getFinantialInstitution().getCurrency()
                    .getValueFor(moneyValue);
        }

        return FinantialInstitution.findAll().iterator().next().getCurrency().getValueFor(moneyValue);
    }

    public static String getDebtAccountURL(final TreasuryEvent treasuryEvent) {
        return AcademicTreasurySettings.getInstance().getAcademicTreasuryAccountUrl().getDebtAccountURL(treasuryEvent);
    }

    public static String getExemptionReason(final TreasuryEvent treasuryEvent) {
        return String.join(", ", TreasuryExemption.find(treasuryEvent).map(l -> l.getReason()).collect(Collectors.toSet()));
    }

    public static String getExemptionTypeName(final TreasuryEvent treasuryEvent, final Locale locale) {
        return String.join(", ", TreasuryExemption.find(treasuryEvent)
                .map(l -> l.getTreasuryExemptionType().getName().getContent(locale)).collect(Collectors.toSet()));
    }

    public static List<IPaymentReferenceCode> getPaymentReferenceCodesList(final TreasuryEvent treasuryEvent) {
        return DebitEntry.findActive(treasuryEvent)
                .flatMap(d -> d.getSibsPaymentRequestsSet().stream())
                .map(r -> new PaymentReferenceCodeImpl(r))
                .collect(Collectors.toList());
    }

    public static List<IAcademicTreasuryEventPayment> getPaymentsList(final TreasuryEvent event) {
        return DebitEntry.findActive(event).map(l -> l.getSettlementEntriesSet()).reduce((a, b) -> Sets.union(a, b))
                .orElse(Sets.newHashSet()).stream().filter(l -> l.getFinantialDocument().isClosed())
                .map(l -> new AcademicTreasuryEventPayment(l)).collect(Collectors.toList());
    }

    public static boolean isBlockingAcademicalActs(final TreasuryEvent treasuryEvent, final LocalDate when) {
        /* Iterate over active debit entries which
         * are not marked with academicActBlockingSuspension
         * and ask if it is in debt
         */

        return DebitEntry.find(treasuryEvent).filter(l -> PersonCustomer.isDebitEntryBlockingAcademicalActs(l, when)).count() > 0;
    }

    public static boolean isCharged(final TreasuryEvent event) {
        return DebitEntry.findActive(event).count() > 0;
    }

    public static boolean isDueDateExpired(final TreasuryEvent treasuryEvent, final LocalDate when) {
        return DebitEntry.findActive(treasuryEvent).map(l -> l.isDueDateExpired(when)).reduce((a, b) -> a || b).orElse(false);
    }

    public static boolean isExempted(final TreasuryEvent treasuryEvent) {
        return !treasuryEvent.getTreasuryExemptionsSet().isEmpty();
    }

    public static boolean isOnlinePaymentsActive(final TreasuryEvent treasuryEvent) {
        if (!((IAcademicTreasuryEvent) treasuryEvent).isCharged()) {
            return false;
        }

        final FinantialInstitution finantialInstitution =
                DebitEntry.findActive(treasuryEvent).iterator().next().getDebtAccount().getFinantialInstitution();

        return DigitalPaymentPlatform.find(finantialInstitution, TreasurySettings.getInstance().getCreditCardPaymentMethod(), true)
                .findFirst().isPresent();
    }

}