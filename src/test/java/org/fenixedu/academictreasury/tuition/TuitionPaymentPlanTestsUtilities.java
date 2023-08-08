package org.fenixedu.academictreasury.tuition;

import org.fenixedu.academictreasury.base.BasicAcademicTreasuryUtils;

public class TuitionPaymentPlanTestsUtilities {

    public static void startUp() {
        BasicAcademicTreasuryUtils.startup(() -> {
            return null;
        });
    }
    
}