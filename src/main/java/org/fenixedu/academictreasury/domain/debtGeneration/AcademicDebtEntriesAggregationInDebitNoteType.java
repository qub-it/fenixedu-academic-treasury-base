package org.fenixedu.academictreasury.domain.debtGeneration;

import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;

public enum AcademicDebtEntriesAggregationInDebitNoteType {
    AGGREGATE_IN_UNIQUE_DEBIT_NOTE,
    AGGREGATE_IN_INDIVIDUAL_DEBIT_NOTE;
    
    public LocalizedString getDescriptionI18N() {
        return AcademicTreasuryConstants.academicTreasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }
    
}
