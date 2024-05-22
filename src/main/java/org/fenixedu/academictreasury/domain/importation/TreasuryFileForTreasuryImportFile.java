package org.fenixedu.academictreasury.domain.importation;

import org.fenixedu.bennu.core.domain.User;

public class TreasuryFileForTreasuryImportFile extends TreasuryFileForTreasuryImportFile_Base {
    
    public TreasuryFileForTreasuryImportFile() {
        /* This is a replacement for existings treasury files. It should not be created for new files */

        throw new RuntimeException("error");
    }

    @Override
    public boolean isAccessible(User arg0) {
        return false;
    }
    
}
