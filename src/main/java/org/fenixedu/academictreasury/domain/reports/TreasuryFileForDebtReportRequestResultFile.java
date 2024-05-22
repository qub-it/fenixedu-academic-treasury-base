package org.fenixedu.academictreasury.domain.reports;

import org.fenixedu.bennu.core.domain.User;

public class TreasuryFileForDebtReportRequestResultFile extends TreasuryFileForDebtReportRequestResultFile_Base {
    
    public TreasuryFileForDebtReportRequestResultFile() {
        /* This is a replacement for existings treasury files. It should not be created for new files */

        throw new RuntimeException("error");
    }

    @Override
    public boolean isAccessible(User arg0) {
        return false;
    }
    
}
