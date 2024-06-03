package org.fenixedu.academictreasury.domain.debtGeneration.requests;

import org.fenixedu.bennu.core.domain.User;

public class TreasuryFileForMassiveDebtGenerationRequestFile extends TreasuryFileForMassiveDebtGenerationRequestFile_Base {
    
    public TreasuryFileForMassiveDebtGenerationRequestFile() {
        /* This is a replacement for existings treasury files. It should not be created for new files */

        throw new RuntimeException("error");
    }

    @Override
    public boolean isAccessible(User arg0) {
        return false;
    }
    
}
