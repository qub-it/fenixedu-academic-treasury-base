package org.fenixedu.academictreasury.domain.exemptions.requests;

import org.fenixedu.bennu.core.domain.User;

public class TreasuryFileForExemptionsGenerationRequestFile extends TreasuryFileForExemptionsGenerationRequestFile_Base {
    
    public TreasuryFileForExemptionsGenerationRequestFile() {
        /* This is a replacement for existings treasury files. It should not be created for new files */

        throw new RuntimeException("error");
    }

    @Override
    public boolean isAccessible(User arg0) {
        return false;
    }
    
}
