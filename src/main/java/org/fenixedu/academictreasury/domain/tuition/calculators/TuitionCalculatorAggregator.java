package org.fenixedu.academictreasury.domain.tuition.calculators;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.tuition.ITuitionRegistrationServiceParameters;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.fenixedu.treasury.util.TreasuryConstants;

public class TuitionCalculatorAggregator extends TuitionCalculatorAggregator_Base {

    public TuitionCalculatorAggregator() {
        super();
    }

    public TuitionCalculatorAggregator(LocalizedString name) {
        this();
        setName(name);
    }

    public void delete() {
        getTuitionPaymentPlanCalculatorChildSet().forEach(TuitionPaymentPlanCalculator::delete);

        super.delete();
    }

    @Override
    public boolean isValid() {
        return getTuitionPaymentPlanCalculatorChildSet().stream().map(c -> c.isValid()).reduce((a, c) -> a && c).orElse(false);
    }

    @Override
    public BigDecimal getTotalAmount(Registration registration) {
        return getTotalAmount(registration, (ITuitionRegistrationServiceParameters) null);
    }

    public BigDecimal getTotalAmount(Registration registration, ITuitionRegistrationServiceParameters parameters) {
        Predicate<TuitionPaymentPlanCalculator> conditionValidTo = (tuitionPaymentPlanCalculator) -> tuitionPaymentPlanCalculator
                .getTuitionConditionRulesSet().stream().allMatch(c -> c.isValidTo(registration, getExecutionYear(), null));

        BigDecimal totalAmount = getTuitionPaymentPlanCalculatorChildSet().stream().filter(conditionValidTo)
                .map(c -> c.getTotalAmount(registration, parameters)).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (getMinimumAmount() != null) {
            totalAmount = totalAmount.max(getMinimumAmount());
        }

        if (getMaximumAmount() != null) {
            totalAmount = totalAmount.min(getMaximumAmount());
        }

        return totalAmount;
    }

    @Override
    public BigDecimal getTotalAmount(Enrolment enrolment) {
        return getTotalAmount(enrolment, (ITuitionRegistrationServiceParameters) null);
    }

    public BigDecimal getTotalAmount(Enrolment enrolment, ITuitionRegistrationServiceParameters parameters) {
        Predicate<TuitionPaymentPlanCalculator> conditionValidTo =
                (tuitionPaymentPlanCalculator) -> tuitionPaymentPlanCalculator.getTuitionConditionRulesSet().isEmpty()
                        || tuitionPaymentPlanCalculator.getTuitionConditionRulesSet().stream()
                                .allMatch(c -> c.isValidTo(enrolment.getRegistration(), getExecutionYear(), enrolment));

        BigDecimal totalAmount = getTuitionPaymentPlanCalculatorChildSet().stream().filter(conditionValidTo)
                .map(c -> c.getTotalAmount(enrolment, parameters)).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (getMinimumAmount() != null) {
            totalAmount = totalAmount.max(getMinimumAmount());
        }

        if (getMaximumAmount() != null) {
            totalAmount = totalAmount.min(getMaximumAmount());
        }

        return totalAmount;
    }

    @Override
    public String getCalculationDescription(Registration registration) {
        Predicate<TuitionPaymentPlanCalculator> conditionValidTo =
                (tuitionPaymentPlanCalculator) -> tuitionPaymentPlanCalculator.getTuitionConditionRulesSet().isEmpty()
                        || tuitionPaymentPlanCalculator.getTuitionConditionRulesSet().stream()
                                .allMatch(c -> c.isValidTo(registration, getExecutionYear(), null));

        return getTuitionPaymentPlanCalculatorChildSet().stream().filter(conditionValidTo)
                .map(c -> c.getCalculationDescription(registration)).reduce((a, c) -> a + c).get();
    }

    @Override
    public String getCalculationDescription(Enrolment enrolment) {
        Predicate<TuitionPaymentPlanCalculator> conditionValidTo =
                (tuitionPaymentPlanCalculator) -> tuitionPaymentPlanCalculator.getTuitionConditionRulesSet().isEmpty()
                        || tuitionPaymentPlanCalculator.getTuitionConditionRulesSet().stream()
                                .allMatch(c -> c.isValidTo(enrolment.getRegistration(), getExecutionYear(), enrolment));

        return getTuitionPaymentPlanCalculatorChildSet().stream().filter(conditionValidTo)
                .map(c -> c.getCalculationDescription(enrolment)).reduce((a, c) -> a + c).get();
    }

    @Override
    public LocalizedString getParametersDescription() {
        LocalizedString description =
                AcademicTreasuryConstants.academicTreasuryBundleI18N("label.TuitionCalculatorAggregator.description");

        BinaryOperator<LocalizedString> appendLocalizedString = (l1, l2) -> l1.append(l2, "\n");
        description.append(getTuitionPaymentPlanCalculatorChildSet().stream() //
                .sorted((a, c) -> a.getExternalId().compareTo(c.getExternalId())) //
                .map(c -> c.getParametersDescription()).reduce(new LocalizedString(), appendLocalizedString), "\n");

        return description;
    }

    @Override
    public TuitionPaymentPlanCalculator copyTo(TuitionPaymentPlan tuitionPaymentPlanTarget) {
        TuitionCalculatorAggregator copy = create(getName());

        copy.setTuitionPaymentPlan(tuitionPaymentPlanTarget);

        getTuitionPaymentPlanCalculatorChildSet().forEach(c -> c.copyTo(copy));

        return copy;
    }

    @Override
    public TuitionPaymentPlanCalculator copyTo(TuitionCalculatorAggregator tuitionCalculatorAggregatorTarget) {
        TuitionCalculatorAggregator copy = create(getName());

        copy.setTuitionCalculatorParentAggregator(tuitionCalculatorAggregatorTarget);

        getTuitionPaymentPlanCalculatorChildSet().forEach(c -> c.copyTo(copy));

        return copy;
    }

    private static class ConfigChildJsonType {
        String childClassName;
        String childName;
        String childParameters;
    }

    private static class ConfigJsonType {
        List<ConfigChildJsonType> childs;
    }

    @Override
    public void fillWithParametersFromImportation(String parameters) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            ConfigJsonType rootNode = objectMapper.readValue(parameters, ConfigJsonType.class);

            if (rootNode == null) {
                throw new IllegalArgumentException(
                        "error.TuitionCalculatorAggregator.fillWithParametersFromImportation.root.empty");
            }

            if (rootNode.childs == null || rootNode.childs.isEmpty()) {
                throw new IllegalArgumentException(
                        "error.TuitionCalculatorAggregator.fillWithParametersFromImportation.childs.empty");
            }

            for (ConfigChildJsonType configChildJsonType : rootNode.childs) {
                TuitionPaymentPlanCalculator childCalculator =
                        (TuitionPaymentPlanCalculator) Class.forName(configChildJsonType.childClassName)
                                .getConstructor(LocalizedString.class)
                                .newInstance(new LocalizedString(
                                        TreasuryConstants.getDefaultLocale(),
                                        configChildJsonType.childName));

                childCalculator.setTuitionCalculatorParentAggregator(this);
                childCalculator.fillWithParametersFromImportation(configChildJsonType.childParameters);
            }
        } catch (IOException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    // Service methods

    public static LocalizedString getCalculatorPresentationName() {
        LocalizedString description =
                AcademicTreasuryConstants.academicTreasuryBundleI18N("label.TuitionCalculatorAggregator.description");

        return description;
    }

    public static TuitionCalculatorAggregator create(LocalizedString name) {
        return new TuitionCalculatorAggregator(name);
    }

}
