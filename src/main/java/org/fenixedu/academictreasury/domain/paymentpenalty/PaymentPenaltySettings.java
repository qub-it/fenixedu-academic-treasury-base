package org.fenixedu.academictreasury.domain.paymentpenalty;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PaymentPenaltySettings extends PaymentPenaltySettings_Base {

    public PaymentPenaltySettings() {
        super();

        setDomainRoot(FenixFramework.getDomainRoot());
        setActive(false);
        setCreatePaymentCode(false);

        checkRules();
    }

    private void checkRules() {
        if (super.getDomainRoot() == null) {
            throw new IllegalStateException("error.PaymentPenaltySettings.domainRoot.required");
        }

        if (super.getActive() == null) {
            throw new IllegalStateException("error.PaymentPenaltySettings.active.required");
        }

        if (Boolean.TRUE.equals(super.getActive()) && getEmolumentDescription() == null) {
            throw new IllegalStateException("error.PaymentPenaltySettings.is.active.but.emolumentDescription.is.null");
        }

        if (Boolean.TRUE.equals(super.getActive()) && getPenaltyProduct() == null) {
            throw new IllegalStateException("error.PaymentPenaltySettings.is.active.but.penaltyProduct.is.null");
        }

        if (super.getCreatePaymentCode() == null) {
            throw new IllegalStateException("error.PaymentPenaltySettings.createPaymentCode.required");
        }

        if (findAll().count() > 1) {
            throw new IllegalStateException("error.PaymentPenaltySettings.already.created");
        }
    }

    public LocalizedString buildEmolumentDescription(PaymentPenaltyEventTarget target) {
        LocalizedString result = new LocalizedString();
        for (Locale locale : TreasuryPlataformDependentServicesFactory.implementation().availableLocales()) {
            Map<String, String> valueMap = new HashMap<String, String>();
            valueMap.put("debitEntryDescription", target.getOriginDebitEntry().getDescription());
            valueMap.put("penaltyProductName", getPenaltyProduct().getName().getContent(locale));

            result = result.with(locale, StrSubstitutor.replace(getEmolumentDescription().getContent(locale), valueMap));
        }

        return result;
    }

    @Atomic
    public void delete() {
        setDomainRoot(null);
        setPenaltyProduct(null);

        super.deleteDomainObject();
    }

    public void edit(boolean active, Product penaltyProduct, LocalizedString emolumentDescription, boolean createPaymentCode) {
        super.setActive(active);

        super.setPenaltyProduct(penaltyProduct);
        super.setEmolumentDescription(emolumentDescription);
        super.setCreatePaymentCode(createPaymentCode);

        checkRules();
    }

    /*
     * SERVICES
     */

    public static PaymentPenaltySettings create() {
        return new PaymentPenaltySettings();
    }

    public static Stream<PaymentPenaltySettings> findAll() {
        return FenixFramework.getDomainRoot().getPaymentPenaltySettingsSet().stream();
    }

    public static PaymentPenaltySettings getInstance() {
        return FenixFramework.getDomainRoot().getPaymentPenaltySettingsSet().stream().findFirst().orElse(null);
    }

}
