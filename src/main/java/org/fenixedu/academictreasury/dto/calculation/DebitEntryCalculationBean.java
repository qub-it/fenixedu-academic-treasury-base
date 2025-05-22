package org.fenixedu.academictreasury.dto.calculation;

import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.LocalDate;

import java.math.BigDecimal;

// ANIL 2025-05-22 (qubIT-Fenix-6753)
//
// This interface is useful to present emoluments and tuitions at the same time
public interface DebitEntryCalculationBean {

    public LocalizedString getDescription();
    public LocalDate getDueDate();
    public BigDecimal getAmountWithVat();
    public BigDecimal getNetExemptedAmount();

}
