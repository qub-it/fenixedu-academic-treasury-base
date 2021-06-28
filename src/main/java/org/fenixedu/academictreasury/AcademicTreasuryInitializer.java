package org.fenixedu.academictreasury;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.domain.listeners.DebitEntryDeletionListener;
import org.fenixedu.academictreasury.domain.listeners.FinantialEntityListener;
import org.fenixedu.academictreasury.domain.listeners.ProductDeletionListener;
import org.fenixedu.academictreasury.domain.treasury.AcademicTreasuryBridgeImpl;
import org.fenixedu.academictreasury.services.accesscontrol.spi.AcademicTreasuryAccessControlExtension;
import org.fenixedu.treasury.domain.bennu.signals.BennuSignalsServices;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;

import pt.ist.fenixframework.FenixFramework;

@WebListener
// TODO Check code Refactor/20210624-MergeWithISCTE
public class AcademicTreasuryInitializer implements ServletContextListener {

    @Override
    public void contextDestroyed(final ServletContextEvent arg0) {
    }

    @Override
    public void contextInitialized(final ServletContextEvent arg0) {
        TreasuryAccessControlAPI.registerExtension(new AcademicTreasuryAccessControlExtension());

        DebitEntryDeletionListener.attach();
        ProductDeletionListener.attach();
        FinantialEntityListener.attach();

        final AcademicTreasuryBridgeImpl impl = new AcademicTreasuryBridgeImpl();

        TreasuryBridgeAPIFactory.registerImplementation(impl);
        BennuSignalsServices.registerSettlementEventHandler(impl);

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
