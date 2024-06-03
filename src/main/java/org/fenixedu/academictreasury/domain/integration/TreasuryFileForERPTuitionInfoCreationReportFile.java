package org.fenixedu.academictreasury.domain.integration;

import org.fenixedu.bennu.core.domain.User;

public class TreasuryFileForERPTuitionInfoCreationReportFile extends TreasuryFileForERPTuitionInfoCreationReportFile_Base {
    
    public TreasuryFileForERPTuitionInfoCreationReportFile() {
        /* This is a replacement for existings treasury files. It should not be created for new files */

        throw new RuntimeException("error");
    }

    @Override
    public boolean isAccessible(User arg0) {
        return false;
    }
    
}
