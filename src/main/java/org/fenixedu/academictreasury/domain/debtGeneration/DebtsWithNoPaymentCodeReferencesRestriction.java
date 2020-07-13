package org.fenixedu.academictreasury.domain.debtGeneration;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundleI18N;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.debtGeneration.strategies.CreatePaymentReferencesStrategy;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentcodes.MultipleEntriesPaymentCode;

public class DebtsWithNoPaymentCodeReferencesRestriction extends DebtsWithNoPaymentCodeReferencesRestriction_Base {
    
    public DebtsWithNoPaymentCodeReferencesRestriction() {
        super();
    }
    
    protected DebtsWithNoPaymentCodeReferencesRestriction(AcademicDebtGenerationRule rule, boolean excludeIfMatches) {
        this();
        super.init(rule, excludeIfMatches);
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
            if(MultipleEntriesPaymentCode.find(debitEntry).filter(p -> !p.getPaymentReferenceCode().isAnnulled()).count() > 0) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public boolean test(Registration registration) {
        return isToInclude() ? evaluateResult(registration) : !evaluateResult(registration);
    }

    @Override
    public DebtsWithNoPaymentCodeReferencesRestriction makeCopy(AcademicDebtGenerationRule ruleToCreate) {
        return create(ruleToCreate, getExcludeIfMatches());
    }    
    
    /*
     * ********
     * SERVICES
     * ********
     */
    
    public static DebtsWithNoPaymentCodeReferencesRestriction create(AcademicDebtGenerationRule rule, boolean excludeIfMatches) {
        return new DebtsWithNoPaymentCodeReferencesRestriction(rule, excludeIfMatches);
    }

    public static LocalizedString RESTRICTION_NAME() {
        return academicTreasuryBundleI18N("label.DebtsWithNoPaymentCodeReferences.restrictionName");
    }
    
}
