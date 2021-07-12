package org.fenixedu.academictreasury.domain.debtGeneration;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundleI18N;

import java.util.Collections;
import java.util.List;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.debtGeneration.strategies.CreatePaymentReferencesStrategy;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;

public class DebtsWithNoPaymentCodeReferencesRestriction extends DebtsWithNoPaymentCodeReferencesRestriction_Base {
    
    public DebtsWithNoPaymentCodeReferencesRestriction() {
        super();
    }
    
    protected DebtsWithNoPaymentCodeReferencesRestriction(AcademicDebtGenerationRule rule) {
        this();
        super.init(rule);
    }

    @Override
    public LocalizedString getName() {
        return RESTRICTION_NAME();
    }

    @Override
    public List<LocalizedString> getParametersDescriptions() {
        return Collections.emptyList();
    }
    
    private boolean evaluateResult(final Registration registration) {
        for (final DebitEntry debitEntry : CreatePaymentReferencesStrategy.grabDebitEntries(getAcademicDebtGenerationRule(), registration)) {
            if(debitEntry.getSibsPaymentRequests().stream().filter(p -> !p.isInAnnuledState()).count() > 0) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public boolean test(Registration registration) {
        return evaluateResult(registration);
    }

    @Override
    public DebtsWithNoPaymentCodeReferencesRestriction makeCopy(AcademicDebtGenerationRule ruleToCreate) {
        return create(ruleToCreate);
    }
    
    /*
     * ********
     * SERVICES
     * ********
     */
    
    public static DebtsWithNoPaymentCodeReferencesRestriction create(AcademicDebtGenerationRule rule) {
        return new DebtsWithNoPaymentCodeReferencesRestriction(rule);
    }

    public static LocalizedString RESTRICTION_NAME() {
        return academicTreasuryBundleI18N("label.DebtsWithNoPaymentCodeReferences.restrictionName");
    }
    
}
