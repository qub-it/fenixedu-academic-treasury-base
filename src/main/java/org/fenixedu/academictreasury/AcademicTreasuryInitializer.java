package org.fenixedu.academictreasury;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academictreasury.domain.listeners.DebitEntryDeletionListener;
import org.fenixedu.academictreasury.domain.listeners.FinantialEntityListener;
import org.fenixedu.academictreasury.domain.listeners.ProductDeletionListener;

import pt.ist.fenixframework.FenixFramework;

@WebListener
public class AcademicTreasuryInitializer implements ServletContextListener {

    @Override
    public void contextDestroyed(final ServletContextEvent arg0) {
    }

    @Override
    public void contextInitialized(final ServletContextEvent arg0) {
        DebitEntryDeletionListener.attach();
        ProductDeletionListener.attach();
        FinantialEntityListener.attach();

        addDeletionListeners();
    }

    private void addDeletionListeners() {
        FenixFramework.getDomainModel().registerDeletionListener(Person.class, p -> {
            if(p.getPersonCustomer() != null) {
                p.getPersonCustomer().delete(); 
            }
            
            p.getInactivePersonCustomersSet().forEach(ipc -> ipc.delete());
        });
    }

}
