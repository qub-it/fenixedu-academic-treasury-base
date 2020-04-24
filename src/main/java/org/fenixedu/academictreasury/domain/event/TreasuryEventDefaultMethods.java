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
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentConfiguration;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCode;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

public class TreasuryEventDefaultMethods {

    private static class PaymentReferenceCodeImpl implements IPaymentReferenceCode {

        private final PaymentReferenceCode paymentReferenceCode;

        private PaymentReferenceCodeImpl(final PaymentReferenceCode referenceCode) {
            this.paymentReferenceCode = referenceCode;

        }

        @Override
        public LocalDate getEndDate() {
            return paymentReferenceCode.getEndDate();
        }

        @Override
        public String getEntityCode() {
            return paymentReferenceCode.getPaymentCodePool().getEntityReferenceCode();
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
        return DebitEntry.findActive(treasuryEvent).flatMap(d -> d.getPaymentCodesSet().stream())
                .map(t -> t.getPaymentReferenceCode()).map(p -> new PaymentReferenceCodeImpl(p)).collect(Collectors.toList());
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

        return ForwardPaymentConfiguration.isActive(finantialInstitution);
    }

}
