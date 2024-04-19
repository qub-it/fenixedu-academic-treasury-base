package org.fenixedu.academictreasury.services.reports;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.treasury.domain.debt.DebtAccount;

public interface AcademicDocumentPrinterInterface {

    void init();
    byte[] printRegistrationTuititionPaymentPlan(Registration registration, String outputMimeType);
    byte[] printRegistrationTuititionPaymentPlan(final DebtAccount debtAccount, String outputMimeType);
}
