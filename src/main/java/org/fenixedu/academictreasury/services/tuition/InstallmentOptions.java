package org.fenixedu.academictreasury.services.tuition;

import org.fenixedu.treasury.domain.Product;
import org.joda.time.LocalDate;

import java.util.Map;
import java.util.Set;

public class InstallmentOptions {
    private final RegistrationTuitionService registrationTuitionService;
    Set<Product> installments = null;
    boolean forceInstallmentsEvenTreasuryEventIsCharged = false;

    InstallmentOptions(RegistrationTuitionService registrationTuitionService) {
        this.registrationTuitionService = registrationTuitionService;
    }

    InstallmentOptions(RegistrationTuitionService registrationTuitionService, Set<Product> installments) {
        this.registrationTuitionService = registrationTuitionService;
        this.installments = installments;
    }

    public InstallmentOptions forceInstallmentsEvenTreasuryEventIsCharged(boolean value) {
        this.forceInstallmentsEvenTreasuryEventIsCharged = value;
        return this;
    }

    public RegistrationTuitionService withoutInstallmentsRecalculation() {
        registrationTuitionService.installmentRecalculationOptions = new InstallmentRecalculationOptions();

        return registrationTuitionService;
    }

    public RegistrationTuitionService recalculateInstallments(Map<Product, LocalDate> recalculateInstallments) {
        registrationTuitionService.installmentRecalculationOptions = new InstallmentRecalculationOptions(recalculateInstallments);

        return registrationTuitionService;
    }
}
