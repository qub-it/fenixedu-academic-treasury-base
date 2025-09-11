package org.fenixedu.academictreasury.services.tuition;

import org.fenixedu.treasury.domain.Product;
import org.joda.time.LocalDate;

import java.util.Map;

class InstallmentRecalculationOptions {

    Map<Product, LocalDate> recalculateInstallments;

    InstallmentRecalculationOptions() {
        this.recalculateInstallments = null;
    }

    InstallmentRecalculationOptions(Map<Product, LocalDate> recalculateInstallments) {
        this.recalculateInstallments = recalculateInstallments;
    }
}
