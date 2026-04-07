package org.fenixedu.academictreasury.domain.tuition;

import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;

public enum TuitionPaymentPlanEnrolmentGroupType {
    NORMAL, STANDALONE, EXTRACURRICULAR, ATTENDS;

    TuitionPaymentPlanEnrolmentGroupType() {
    }

    public LocalizedString getDescriptionI18N() {
        return AcademicTreasuryConstants.academicTreasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }
}
