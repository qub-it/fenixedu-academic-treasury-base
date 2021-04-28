package org.fenixedu.academictreasury.domain.tuition;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.util.LocalizedStringUtil;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Product;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class TuitionPaymentPlanGroup extends TuitionPaymentPlanGroup_Base {

    public static final Comparator<TuitionPaymentPlanGroup> COMPARE_BY_NAME = (o1, o2) -> {
        int c = o1.getName().getContent().compareTo(o2.getName().getContent());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    protected TuitionPaymentPlanGroup() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected TuitionPaymentPlanGroup(final String code, final LocalizedString name, boolean forRegistration,
            boolean forStandalone, boolean forExtracurricular, final Product currentProduct) {
        this();
        setCode(code);
        setName(name);

        setForRegistration(forRegistration);
        setForStandalone(forStandalone);
        setForExtracurricular(forExtracurricular);
        setCurrentProduct(currentProduct);

        checkRules();
    }

    private void checkRules() {
        if (Strings.isNullOrEmpty(getCode())) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.name.required");
        }

        if (!(isForRegistration() ^ isForStandalone() ^ isForExtracurricular())) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.only.one.type.supported");
        }

        if (findDefaultGroupForRegistration().count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.for.registration.already.exists");
        }

        if (findDefaultGroupForStandalone().count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.for.standalone.already.exists");
        }

        if (findDefaultGroupForExtracurricular().count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.for.extracurricular.already.exists");
        }

        if (findByCode(getCode()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.code.already.exists");
        }
    }

    @Atomic
    public void edit(final String code, final LocalizedString name, final boolean forRegistration, final boolean forStandalone,
            final boolean forExtracurricular, final Product currentProduct) {
        setCode(code);
        setName(name);
        setForRegistration(forRegistration);
        setForStandalone(forStandalone);
        setForExtracurricular(forExtracurricular);
        setCurrentProduct(currentProduct);

        checkRules();
    }

    public boolean isForRegistration() {
        return getForRegistration();
    }

    public boolean isForStandalone() {
        return getForStandalone();
    }

    public boolean isForExtracurricular() {
        return getForExtracurricular();
    }

    public boolean isForImprovement() {
        return getForImprovement();
    }

    public boolean isDeletable() {
        // ACFSILVA
        return getAcademicTreasuryEventSet().isEmpty() && getTuitionPaymentPlansSet().isEmpty();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.delete.impossible");
        }

        setDomainRoot(null);

        super.deleteDomainObject();
    }

    public static Stream<TuitionPaymentPlanGroup> findAll() {
        return FenixFramework.getDomainRoot().getTuitionPaymentPlanGroupsSet().stream();
    }

    protected static Stream<TuitionPaymentPlanGroup> findDefaultGroupForRegistration() {
        return findAll().filter(t -> t.isForRegistration());
    }

    protected static Stream<TuitionPaymentPlanGroup> findDefaultGroupForStandalone() {
        return findAll().filter(t -> t.isForStandalone());
    }

    protected static Stream<TuitionPaymentPlanGroup> findDefaultGroupForImprovement() {
        return findAll().filter(t -> t.isForImprovement());
    }

    protected static Stream<TuitionPaymentPlanGroup> findDefaultGroupForExtracurricular() {
        return findAll().filter(t -> t.isForExtracurricular());
    }

    protected static Stream<TuitionPaymentPlanGroup> findByCode(final String code) {
        return findAll().filter(l -> StringNormalizer.normalize(l.getCode().toLowerCase())
                .equals(StringNormalizer.normalize(code).toLowerCase()));
    }

    public static Optional<TuitionPaymentPlanGroup> findUniqueDefaultGroupForRegistration() {
        return findDefaultGroupForRegistration().findFirst();
    }

    public static Optional<TuitionPaymentPlanGroup> findUniqueDefaultGroupForStandalone() {
        return findDefaultGroupForStandalone().findFirst();
    }

    public static Optional<TuitionPaymentPlanGroup> findUniqueDefaultGroupForExtracurricular() {
        return findDefaultGroupForExtracurricular().findFirst();
    }

    public static Optional<TuitionPaymentPlanGroup> findUniqueDefaultGroupForImprovement() {
        return findDefaultGroupForImprovement().findFirst();
    }

    @Atomic
    public static TuitionPaymentPlanGroup create(final String code, final LocalizedString name, boolean forRegistration,
            boolean forStandalone, boolean forExtracurricular, final Product currentProduct) {
        return new TuitionPaymentPlanGroup(code, name, forRegistration, forStandalone, forExtracurricular, currentProduct);
    }

    public Set<Class<? extends TuitionConditionRule>> getAllowedConditionRules() {
        if (getAllowedConditionRulesSerialized() == null) {
            return new HashSet<>();
        }
        String[] allowedConditionRulesSerialized = getAllowedConditionRulesSerialized().split(",");
        Set<Class<? extends TuitionConditionRule>> result = new HashSet<>();
        for (String allowedConditionRule : allowedConditionRulesSerialized) {
            try {
                result.add((Class<? extends TuitionConditionRule>) Class.forName(allowedConditionRule));
            } catch (ClassNotFoundException e) {
                continue;
            }
        }
        return result;
    }

    public void addAllowedConditionRules(Class<? extends TuitionConditionRule> allowedConditionRules) {
        Set<Class<? extends TuitionConditionRule>> classes = getAllowedConditionRules();
        classes.add(allowedConditionRules);
        setAllowedConditionRulesSerialized(classes.stream().map(clazz -> clazz.getName()).collect(Collectors.joining(",")));
    }

    public void removeAllowedConditionRules(Class<? extends TuitionConditionRule> allowedConditionRules) {
        Set<Class<? extends TuitionConditionRule>> classes = getAllowedConditionRules();
        classes.remove(allowedConditionRules);
        setAllowedConditionRulesSerialized(classes.stream().map(t -> t.getName()).collect(Collectors.joining(",")));
    }

    public Set<Class<? extends TuitionTariffCustomCalculator>> getAllowedCalculatedAmountCalculators() {
        if (getAllowedCalculatedAmountCalculatorsSerialized() == null) {
            return new HashSet<>();
        }
        String[] allowedCalculatedAmountCalculatorsSerialized = getAllowedCalculatedAmountCalculatorsSerialized().split(",");
        Set<Class<? extends TuitionTariffCustomCalculator>> result = new HashSet<>();
        for (String allowedCalculatedAmountCalculators : allowedCalculatedAmountCalculatorsSerialized) {
            try {
                result.add((Class<? extends TuitionTariffCustomCalculator>) Class.forName(allowedCalculatedAmountCalculators));
            } catch (ClassNotFoundException e) {
                continue;
            }
        }
        return result;
    }

    public void addAllowedCalculatedAmountCalculators(
            Class<? extends TuitionTariffCustomCalculator> allowedCalculatedAmountCalculators) {
        Set<Class<? extends TuitionTariffCustomCalculator>> classes = getAllowedCalculatedAmountCalculators();
        classes.add(allowedCalculatedAmountCalculators);
        setAllowedCalculatedAmountCalculatorsSerialized(
                classes.stream().map(clazz -> clazz.getName()).collect(Collectors.joining(",")));
    }

    public void removeAllowedCalculatedAmountCalculators(
            Class<? extends TuitionTariffCustomCalculator> allowedCalculatedAmountCalculators) {
        Set<Class<? extends TuitionTariffCustomCalculator>> classes = getAllowedCalculatedAmountCalculators();
        classes.remove(allowedCalculatedAmountCalculators);
        setAllowedCalculatedAmountCalculatorsSerialized(classes.stream().map(t -> t.getName()).collect(Collectors.joining(",")));
    }

}
