package org.fenixedu.academictreasury.domain.debtGeneration;

import java.util.stream.Stream;

import org.fenixedu.academictreasury.domain.emoluments.AcademicTax;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import pt.ist.fenixframework.FenixFramework;
import org.fenixedu.treasury.domain.Product;

import pt.ist.fenixframework.Atomic;

public class AcademicDebtGenerationRuleEntry extends AcademicDebtGenerationRuleEntry_Base {

    protected AcademicDebtGenerationRuleEntry() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected AcademicDebtGenerationRuleEntry(AcademicDebtGenerationRule rule, Product product,
            boolean createDebt, boolean toCreateAfterLastRegistrationStateDate, boolean forceCreation,
            boolean limitToRegisteredOnExecutionYear) {
        this();

        setAcademicDebtGenerationRule(rule);
        setProduct(product);
        setCreateDebt(createDebt);
        setToCreateAfterLastRegistrationStateDate(toCreateAfterLastRegistrationStateDate);
        setForceCreation(forceCreation);
        setLimitToRegisteredOnExecutionYear(forceCreation && limitToRegisteredOnExecutionYear);

        checkRules();
    }

    public boolean isCreateDebt() {
        return super.getCreateDebt();
    }

    public boolean isToCreateAfterLastRegistrationStateDate() {
        return super.getToCreateAfterLastRegistrationStateDate();
    }

    public boolean isForceCreation() {
        return super.getForceCreation();
    }

    public boolean isLimitToRegisteredOnExecutionYear() {
        return super.getLimitToRegisteredOnExecutionYear();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRuleEntry.bennu.required");
        }

        if (getAcademicDebtGenerationRule() == null) {
            throw new AcademicTreasuryDomainException(
                    "error.AcademicDebtGenerationRuleEntry.academicDebtGenerationRule.required");
        }

        if (getProduct() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRuleEntry.product.required");
        }

        if (find(getAcademicDebtGenerationRule(), getProduct()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRuleEntry.product.duplicated.in.rule");
        }

        if (!isProductTuition() && !isProductAcademicTax() && isCreateDebt()) {
            throw new AcademicTreasuryDomainException(
                    "error.AcademicDebtGenerationRuleEntry.createDebt.only.supported.for.tuition.or.academicTax");
        }
    }

    private boolean isProductAcademicTax() {
        return AcademicTax.findUnique(getProduct()).isPresent();
    }

    private boolean isProductTuition() {
        return getProduct().getProductGroup() == AcademicTreasurySettings.getInstance().getTuitionProductGroup();
    }

    private boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new AcademicTreasuryDomainException("error.AcademicDebtGenerationRuleEntry.delete.impossible");
        }

        setDomainRoot(null);

        setAcademicDebtGenerationRule(null);
        setProduct(null);

        super.deleteDomainObject();
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<AcademicDebtGenerationRuleEntry> findAll() {
        return FenixFramework.getDomainRoot().getAcademicDebtGenerationRuleEntriesSet().stream();
    }

    public static Stream<AcademicDebtGenerationRuleEntry> find(final AcademicDebtGenerationRule academicDebtGenerationRule) {
        return academicDebtGenerationRule.getAcademicDebtGenerationRuleEntriesSet().stream();
    }

    public static Stream<AcademicDebtGenerationRuleEntry> find(final AcademicDebtGenerationRule academicDebtGenerationRule,
            final Product product) {
        return find(academicDebtGenerationRule).filter(l -> l.getProduct() == product);
    }

    public static AcademicDebtGenerationRuleEntry create(final AcademicDebtGenerationRule rule, final Product product,
            final boolean createDebt, final boolean toCreateAfterLastRegistrationStateDate, final boolean forceCreation,
            final boolean limitToRegisteredOnExecutionYear) {
        return new AcademicDebtGenerationRuleEntry(rule, product, createDebt, toCreateAfterLastRegistrationStateDate,
                forceCreation, limitToRegisteredOnExecutionYear);
    }

}
