package org.fenixedu.academictreasury.domain.listeners;

import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.treasury.domain.FinantialEntity;

import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.dml.DeletionListener;

public class FinantialEntityListener {
    
    public static void attach() {
        
        FenixFramework.getDomainModel().registerDeletionListener(FinantialEntity.class, new DeletionListener<FinantialEntity>() {

            @Override
            public void deleting(final FinantialEntity finantialEntity) {
                if(!finantialEntity.getTuitionPaymentPlansSet().isEmpty()) {
                    throw new AcademicTreasuryDomainException("error.FinantialEntity.cannot.delete.tuitionPaymentPlans.not.empty");
                }
                
                finantialEntity.setAdministrativeOffice(null);
                finantialEntity.setUnit(null);                
            }
        });
    }
    
}
