package org.fenixedu.academictreasury.services.tuition;

import org.fenixedu.treasury.util.TreasuryConstants;

import java.math.BigDecimal;

// Helper class for RegistrationTuitionService
class TreasuryExemptionMoneyBox {
    BigDecimal maximumNetAmountForExemption;
    BigDecimal availableNetAmountForExemption;

    TreasuryExemptionMoneyBox(BigDecimal maximumNetAmountForExemption, BigDecimal availableNetAmountForExemption) {
        this.maximumNetAmountForExemption = maximumNetAmountForExemption;
        this.availableNetAmountForExemption = availableNetAmountForExemption;
    }

    void addToAvailableNetAmountForExemption(BigDecimal netAmount) {
        this.availableNetAmountForExemption = this.availableNetAmountForExemption.add(netAmount);

        // Ensure this will not overflow the maximum amount that can be exempted
        this.availableNetAmountForExemption = this.availableNetAmountForExemption.min(this.maximumNetAmountForExemption);
    }

    void subtractFromCurrentNetAmount(BigDecimal netAmount) {
        this.availableNetAmountForExemption = this.availableNetAmountForExemption.subtract(netAmount);

        // Ensure it will not go below zero
        this.availableNetAmountForExemption = this.availableNetAmountForExemption.max(BigDecimal.ZERO);
    }

    TreasuryExemptionMoneyBox mergeBySumming(TreasuryExemptionMoneyBox o) {
        return new TreasuryExemptionMoneyBox(this.maximumNetAmountForExemption.add(o.maximumNetAmountForExemption),
                this.availableNetAmountForExemption.add(o.availableNetAmountForExemption));
    }

    TreasuryExemptionMoneyBox mergeByChoosingTheGreaterMaximumAmount(TreasuryExemptionMoneyBox o) {
        if (TreasuryConstants.isGreaterOrEqualThan(o.maximumNetAmountForExemption, this.maximumNetAmountForExemption)) {
            return o;
        } else {
            return this;
        }
    }

    boolean isAvailableNetAmountForExemptionPositive() {
        return TreasuryConstants.isPositive(this.availableNetAmountForExemption);
    }

    static TreasuryExemptionMoneyBox zero() {
        return new TreasuryExemptionMoneyBox(BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
