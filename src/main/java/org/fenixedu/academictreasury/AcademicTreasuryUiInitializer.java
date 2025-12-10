package org.fenixedu.academictreasury;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.studentCurriculum.StudentCurricularPlanEnrolmentManager;
import org.fenixedu.academic.domain.studentCurriculum.StudentCurricularPlanNoCourseGroupEnrolmentManager;
import org.fenixedu.academictreasury.services.EmolumentServices;
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
        registerNewAcademicServiceRequestSituationHandler();
        registerAcademicServiceRequestCancelOrRejectHandler();
        registerStandaloneEnrolmentHandler();
        registerExtracurricularEnrolmentHandler();
        registerImprovementEnrolmentHandler();
    }

    private static void registerNewAcademicServiceRequestSituationHandler() {
        Signal.register(AcademicServiceRequest.ACADEMIC_SERVICE_REQUEST_NEW_SITUATION_EVENT, new EmolumentServices());
    }

    private static void registerAcademicServiceRequestCancelOrRejectHandler() {
        Signal.register(AcademicServiceRequest.ACADEMIC_SERVICE_REQUEST_REJECT_OR_CANCEL_EVENT,
                new AcademicServiceRequestCancelOrRejectHandler());
    }

    private static void registerStandaloneEnrolmentHandler() {
        Signal.register(StudentCurricularPlanNoCourseGroupEnrolmentManager.STANDALONE_ENROLMENT, new StandaloneEnrolmentHandler());
    }

    private static void registerExtracurricularEnrolmentHandler() {
        Signal.register(StudentCurricularPlanNoCourseGroupEnrolmentManager.EXTRACURRICULAR_ENROLMENT, new ExtracurricularEnrolmentHandler());
    }

    private static void registerImprovementEnrolmentHandler() {
        Signal.register(StudentCurricularPlanEnrolmentManager.IMPROVEMENT_ENROLMENT, new ImprovementEnrolmentHandler());
    }

}
