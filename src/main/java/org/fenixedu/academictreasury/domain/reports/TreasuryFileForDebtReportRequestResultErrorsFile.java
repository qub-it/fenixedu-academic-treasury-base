package org.fenixedu.academictreasury.domain.reports;

import org.fenixedu.bennu.core.domain.User;

public class TreasuryFileForDebtReportRequestResultErrorsFile extends TreasuryFileForDebtReportRequestResultErrorsFile_Base {
    
    public TreasuryFileForDebtReportRequestResultErrorsFile() {
        /* This is a replacement for existings treasury files. It should not be created for new files */

        throw new RuntimeException("error");
    }

    @Override
    public boolean isAccessible(User arg0) {
        return false;
    }
    
}
