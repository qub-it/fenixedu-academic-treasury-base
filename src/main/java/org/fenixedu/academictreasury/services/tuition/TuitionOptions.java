package org.fenixedu.academictreasury.services.tuition;

import org.fenixedu.academictreasury.domain.tuition.TuitionAllocation;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.treasury.domain.Product;

import java.util.Set;

public class TuitionOptions {
    private final RegistrationTuitionService registrationTuitionService;
    TuitionPaymentPlan tuitionPaymentPlan = null;
    boolean forceCreationIfNotEnrolled = false;
    boolean applyTuitionServiceExtensions = true;
    TuitionAllocation tuitionAllocation;

    TuitionOptions(RegistrationTuitionService registrationTuitionService) {
        this.registrationTuitionService = registrationTuitionService;
        registrationTuitionService.tuitionOptions = this;
    }

    TuitionOptions(RegistrationTuitionService registrationTuitionService, TuitionPaymentPlan tuitionPaymentPlan) {
        this.registrationTuitionService = registrationTuitionService;
        this.tuitionPaymentPlan = tuitionPaymentPlan;

        registrationTuitionService.tuitionOptions = this;
    }

    public TuitionOptions discardTuitionServiceExtensions(boolean value) {
        this.applyTuitionServiceExtensions = value;
        return this;
    }

    public TuitionOptions forceCreationIfNotEnrolled(boolean value) {
        this.forceCreationIfNotEnrolled = value;
        return this;
    }

    public TuitionOptions applyTuitionAllocation(TuitionAllocation tuitionAllocation) {
        this.tuitionAllocation = tuitionAllocation;
        return this;
    }

    public InstallmentOptions withAllInstallments() {
        return registrationTuitionService.installmentOptions = new InstallmentOptions(registrationTuitionService);
    }

    public InstallmentOptions restrictForInstallmentProducts(Set<Product> installmentProducts) {
        return registrationTuitionService.installmentOptions =
                new InstallmentOptions(registrationTuitionService, installmentProducts);
    }

}
