package org.fenixedu.academictreasury.domain.tuition;

import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;

public enum TuitionPaymentPlanEnrolmentGroupType {
    NORMAL(false), STANDALONE(false), EXTRACURRICULAR(false), ATTENDS(true);

    private boolean evaluationSeasonRequired;

    TuitionPaymentPlanEnrolmentGroupType(boolean evaluationSeasonRequired) {
        this.evaluationSeasonRequired = evaluationSeasonRequired;
    }

    public LocalizedString getDescriptionI18N() {
        return AcademicTreasuryConstants.academicTreasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }

    public boolean isEvaluationSeasonRequired() {
        return this.evaluationSeasonRequired;
    }
}
