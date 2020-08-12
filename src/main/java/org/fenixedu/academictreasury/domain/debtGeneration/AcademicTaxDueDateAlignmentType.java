package org.fenixedu.academictreasury.domain.debtGeneration;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import org.fenixedu.academictreasury.domain.emoluments.AcademicTax;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.joda.time.LocalDate;

public enum AcademicTaxDueDateAlignmentType {

    TUITION_INSTALLMENT_MAX_DUE_DATE, TUITION_INSTALLMENT_MIN_DUE_DATE;

    public boolean isTuitionInstallmentMaxDueDate() {
        return this == TUITION_INSTALLMENT_MAX_DUE_DATE;
    }

    public boolean isTuitionInstallmentMinDueDate() {
        return this == TUITION_INSTALLMENT_MIN_DUE_DATE;
    }

    public LocalizedString getDescriptionI18N() {
        return AcademicTreasuryConstants.academicTreasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }

    // @formatter:off
    /* **************
     * BUSINESS LOGIC
     * **************
     */
    // @formatter:on

    public void applyDueDate(final AcademicDebtGenerationRule rule, final Set<DebitEntry> debitEntriesSet) {
        final LocalDate dueDateToApply = dueDateToApply(debitEntriesSet);

        if(dueDateToApply == null) {
            return;
        }
        
        for (final DebitEntry debitEntry : debitEntriesSet) {
            if (!AcademicTax.findUnique(debitEntry.getProduct()).isPresent()) {
                continue;
            }

            LocalDate oldDueDate = debitEntry.getDueDate();
            
            if(oldDueDate != null && dueDateToApply != null && oldDueDate.equals(dueDateToApply)) {
                continue;
            }
            
            debitEntry.setDueDate(dueDateToApply);
        }
    }

    private LocalDate dueDateToApply(final Set<DebitEntry> debitEntriesSet) {

        if (isTuitionInstallmentMaxDueDate()) {
            final Optional<DebitEntry> maxDebitEntry = debitEntriesSet.stream().filter(
                    d -> d.getProduct().getProductGroup() == AcademicTreasurySettings.getInstance().getTuitionProductGroup())
                    .max(Comparator.comparing(DebitEntry::getDueDate));

            if(maxDebitEntry.isPresent()) {
                return maxDebitEntry.get().getDueDate();
            }
        } else if (isTuitionInstallmentMinDueDate()) {
            final Optional<DebitEntry> minDebitEntry = debitEntriesSet.stream().filter(
                    d -> d.getProduct().getProductGroup() == AcademicTreasurySettings.getInstance().getTuitionProductGroup())
                    .min(Comparator.comparing(DebitEntry::getDueDate));

            if(minDebitEntry.isPresent()) {
                return minDebitEntry.get().getDueDate();
            }
        } else {
            throw new AcademicTreasuryDomainException("error.AcademicTaxDueDateAlignmentType.unknown.rule.to.apply");
        }

        return null;
    }

}
