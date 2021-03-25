package org.fenixedu.academictreasury.domain.reports;


import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;

public enum DebtReportRequestType {

    INVOICE_ENTRIES,
    PAYMENT_REFERENCE_CODES,
    OTHER_DATA;

    public LocalizedString getDescriptionI18N() {
        return AcademicTreasuryConstants.academicTreasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }
    
    public boolean isRequestForInvoiceEntries() {
        return this == INVOICE_ENTRIES;
    }
    
    public boolean isRequestForPaymentReferenceCodes() {
        return this == PAYMENT_REFERENCE_CODES;
    }
    
    public boolean isRequestForOtherData() {
        return this == OTHER_DATA;
    }
}
