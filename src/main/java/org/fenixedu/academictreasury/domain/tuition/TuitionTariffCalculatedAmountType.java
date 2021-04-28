package org.fenixedu.academictreasury.domain.tuition;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundleI18N;

import org.fenixedu.commons.i18n.LocalizedString;

public enum TuitionTariffCalculatedAmountType {
    CAPTIVE, REMAINING, PERCENTAGE;

    public boolean isCaptive() {
        return this == CAPTIVE;
    }

    public boolean isRemaining() {
        return this == REMAINING;
    }

    public boolean isPercentage() {
        return this == PERCENTAGE;
    }

    public LocalizedString getDescriptionI18N() {
        return academicTreasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }
}
