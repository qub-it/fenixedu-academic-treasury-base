package org.fenixedu.academictreasury.schoolsbootstrapscript;

import java.util.stream.Collectors;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.VatExemptionReason;
import org.fenixedu.treasury.domain.VatType;

public class ActivateAllProducts extends CustomTask {

    @Override
    public void runTask() throws Exception {
        doIt();
        
        throw new RuntimeException("abort");
    }

    private void doIt() {
        for (Product product : Product.findAll().collect(Collectors.toSet())) {
            if(!product.isActive()) {
                continue;
            }
            
            if(!product.isLegacy()) {
                continue;
            }
            
            if(!"TUITION".equals(product.getProductGroup().getCode())) {
                continue;
            }
            
            taskLog("C\tDEACTIVATE PRODUCT\t%s\t%s\n", product.getCode(), product.isActive());
            
            product.setActive(false);
            
        }
    }

}
