package org.fenixedu.academictreasury;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.treasury.ITreasuryBridgeAPI;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.EmolumentServices;
import org.fenixedu.academictreasury.services.FenixEduAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.services.signals.AcademicServiceRequestCancelOrRejectHandler;
import org.fenixedu.academictreasury.services.signals.ExtracurricularEnrolmentHandler;
import org.fenixedu.academictreasury.services.signals.ImprovementEnrolmentHandler;
import org.fenixedu.academictreasury.services.signals.StandaloneEnrolmentHandler;
import org.fenixedu.bennu.core.signals.Signal;

@WebListener
public class AcademicTreasuryUiInitializer implements ServletContextListener {

    @Override
    public void contextDestroyed(final ServletContextEvent arg0) {
    }

    @Override
    public void contextInitialized(final ServletContextEvent arg0) {
        AcademicTreasuryPlataformDependentServicesFactory
                .registerImplementation(new FenixEduAcademicTreasuryPlatformDependentServices());

        registerNewAcademicServiceRequestSituationHandler();
        registerAcademicServiceRequestCancelOrRejectHandler();
        registerStandaloneEnrolmentHandler();
        registerExtracurricularEnrolmentHandler();
        registerImprovementEnrolmentHandler();
    }

    private static void registerNewAcademicServiceRequestSituationHandler() {
        Signal.register(ITreasuryBridgeAPI.ACADEMIC_SERVICE_REQUEST_NEW_SITUATION_EVENT, new EmolumentServices());
    }

    private static void registerAcademicServiceRequestCancelOrRejectHandler() {
        Signal.register(ITreasuryBridgeAPI.ACADEMIC_SERVICE_REQUEST_REJECT_OR_CANCEL_EVENT,
                new AcademicServiceRequestCancelOrRejectHandler());
    }

    private static void registerStandaloneEnrolmentHandler() {
        Signal.register(ITreasuryBridgeAPI.STANDALONE_ENROLMENT, new StandaloneEnrolmentHandler());
    }

    private static void registerExtracurricularEnrolmentHandler() {
        Signal.register(ITreasuryBridgeAPI.EXTRACURRICULAR_ENROLMENT, new ExtracurricularEnrolmentHandler());
    }

    private static void registerImprovementEnrolmentHandler() {
        Signal.register(ITreasuryBridgeAPI.IMPROVEMENT_ENROLMENT, new ImprovementEnrolmentHandler());
    }

}
