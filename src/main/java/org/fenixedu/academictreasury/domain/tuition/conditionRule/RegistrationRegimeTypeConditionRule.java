package org.fenixedu.academictreasury.domain.tuition.conditionRule;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionAnnotation;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionRule;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;

@TuitionConditionAnnotation(RegistrationRegimeTypeConditionRule.BUNDLE_NAME)
public class RegistrationRegimeTypeConditionRule extends RegistrationRegimeTypeConditionRule_Base {

    public static final String BUNDLE_NAME = AcademicTreasuryConstants.BUNDLE;

    public RegistrationRegimeTypeConditionRule() {
        super();
    }

    @Override
    public boolean containsRule(TuitionConditionRule tuitionConditionRule) {
        if (!(tuitionConditionRule instanceof RegistrationRegimeTypeConditionRule)) {
            return false;
        }
        RegistrationRegimeTypeConditionRule rule = (RegistrationRegimeTypeConditionRule) tuitionConditionRule;
        return getRegistrationRegimeTypes().containsAll(rule.getRegistrationRegimeTypes());
    }

    @Override
    public boolean isValidTo(Registration registration, ExecutionYear executionYear, Enrolment enrolment) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        RegistrationRegimeType registrationRegimeType =
                academicTreasuryServices.registrationRegimeType(registration, executionYear);
        return getRegistrationRegimeTypes().contains(registrationRegimeType);
    }

    public Set<RegistrationRegimeType> getRegistrationRegimeTypes() {
        Set<RegistrationRegimeType> result = new RegimeHashSet(this);
        getRegimeTypesConverted().forEach(r -> result.add(r));
        return result;
    }

    private Set<RegistrationRegimeType> getRegimeTypesConverted() {
        Set<RegistrationRegimeType> result = new HashSet<RegistrationRegimeType>();
        if (getRegistrationRegimeTypesSerialized() == null) {
            return result;
        }
        String[] types = getRegistrationRegimeTypesSerialized().split(",");
        for (String type : types) {
            try {
                result.add(RegistrationRegimeType.valueOf(type));
            } catch (IllegalArgumentException e) {
                continue;
            }
        }
        return result;
    }

//    public void setRegistrationRegimeTypes(Set<RegistrationRegimeType> types) {
//        setRegistrationRegimeTypesSerialized(types.stream().map(type -> type.getName()).collect(Collectors.joining(",")));
//    }

    public void addRegistrationRegimeTypes(RegistrationRegimeType type) {
        Set<RegistrationRegimeType> registrationRegimeTypes = getRegimeTypesConverted();
        registrationRegimeTypes.add(type);
        setRegistrationRegimeTypesSerialized(
                registrationRegimeTypes.stream().map(t -> t.getName()).collect(Collectors.joining(",")));
    }

    public void removeRegistrationRegimeTypes(RegistrationRegimeType type) {
        Set<RegistrationRegimeType> registrationRegimeTypes = getRegimeTypesConverted();
        registrationRegimeTypes.remove(type);
        setRegistrationRegimeTypesSerialized(
                registrationRegimeTypes.stream().map(t -> t.getName()).collect(Collectors.joining(",")));
    }

    @Override
    public boolean checkRules() {
        if (getRegistrationRegimeTypesSerialized() == null || getRegistrationRegimeTypesSerialized().isEmpty()) {
            throw new DomainException(i18n(
                    "org.fenixedu.academictreasury.domain.tuition.conditionRule.RegistrationRegimeTypeConditionRule.RegistrationRegimeTypes.cannotBeEmpty"));
        }
        return true;
    }

    @Override
    public String getDescription() {
        return getRegistrationRegimeTypes().stream().map(c -> c.getLocalizedName()).collect(Collectors.joining(", "));
    }

    @Override
    protected String getBundle() {
        return BUNDLE_NAME;
    }

    @Override
    public void delete() {
        setTuitionPaymentPlan(null);
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Override
    protected TuitionConditionRule copyToPlan(TuitionPaymentPlan tuitionPaymentPlan) {
        RegistrationRegimeTypeConditionRule result = new RegistrationRegimeTypeConditionRule();
        result.setTuitionPaymentPlan(tuitionPaymentPlan);
        getRegistrationRegimeTypes().forEach(c -> result.addRegistrationRegimeTypes(c));
        return result;
    }

    private static class RegimeHashSet extends HashSet<RegistrationRegimeType> {
        private RegistrationRegimeTypeConditionRule rule;

        public RegimeHashSet(RegistrationRegimeTypeConditionRule registrationRegimeTypeConditionRule) {
            this.rule = registrationRegimeTypeConditionRule;
        }

        @Override
        public boolean addAll(Collection<? extends RegistrationRegimeType> c) {
            for (RegistrationRegimeType regime : c) {
                rule.addRegistrationRegimeTypes(regime);
            }
            return super.addAll(c);
        }
    }

}
