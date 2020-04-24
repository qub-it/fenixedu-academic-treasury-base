package org.fenixedu.academictreasury.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.serviceRequests.ServiceRequestType;
import org.fenixedu.academictreasury.domain.emoluments.ServiceRequestMapEntry;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.FenixEduAcademicTreasuryPlatformDependentServices;

import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.dml.DeletionListener;

@WebListener
public class FenixeduAcademicTreasuryBaseInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        AcademicTreasuryPlataformDependentServicesFactory.registerImplementation(new FenixEduAcademicTreasuryPlatformDependentServices());
        
        setupListenerForServiceRequestTypeDelete();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    private void setupListenerForServiceRequestTypeDelete() {
        FenixFramework.getDomainModel().registerDeletionListener(ServiceRequestType.class,
                new DeletionListener<ServiceRequestType>() {
                    @Override
                    public void deleting(ServiceRequestType serviceRequestType) {
                        for (ServiceRequestMapEntry mapEntry : serviceRequestType.getServiceRequestMapEntriesSet()) {
                            mapEntry.delete();
                        }
                    }
                });
    }
    
}