package org.fenixedu.academictreasury.domain.debtGeneration.restrictions;

import java.util.Set;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtGenerationRule;
import org.fenixedu.academictreasury.domain.debtGeneration.IDebtGenerationRuleRestrictionStrategy;
import org.fenixedu.academictreasury.domain.debtGeneration.strategies.CreatePaymentReferencesStrategy;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentcodes.MultipleEntriesPaymentCode;

public class DebtsWithNoPaymentCodeReferences implements IDebtGenerationRuleRestrictionStrategy {

    @Override
    public boolean isToApply(final AcademicDebtGenerationRule rule, final Registration registration) {
        
        final Set<DebitEntry> debitEntries = CreatePaymentReferencesStrategy.grabDebitEntries(rule, registration);

        for (final DebitEntry debitEntry : debitEntries) {
            if(debitEntry.getSibsPaymentRequestsSet().stream().filter(p -> !p.isInAnnuledState()).count() > 0) {
                return false;
            }
        }
        
        return true;
    }
    
}
