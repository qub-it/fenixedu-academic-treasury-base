package org.fenixedu.academictreasury.domain.tuition.calculators;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.tuition.ITuitionRegistrationServiceParameters;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.commons.i18n.LocalizedString;

import pt.ist.fenixframework.FenixFramework;

abstract public class TuitionPaymentPlanCalculator extends TuitionPaymentPlanCalculator_Base {

    public TuitionPaymentPlanCalculator() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public ExecutionYear getExecutionYear() {
        return getTuitionCalculatorParentAggregator() != null ? getTuitionCalculatorParentAggregator()
                .getExecutionYear() : getTuitionPaymentPlan().getExecutionYear();
    }

    public abstract boolean isValid();

    public abstract BigDecimal getTotalAmount(Registration registration);
    
    public BigDecimal getTotalAmount(Registration registration, ITuitionRegistrationServiceParameters parameters) {
        return getTotalAmount(registration);
    }

    public abstract BigDecimal getTotalAmount(Enrolment enrolment);
    
    public BigDecimal getTotalAmount(Enrolment enrolment, ITuitionRegistrationServiceParameters parameters) {
        return getTotalAmount(enrolment);
    }

    public abstract String getCalculationDescription(Registration registration);

    public abstract String getCalculationDescription(Enrolment enrolment);

    public abstract LocalizedString getParametersDescription();

    public abstract TuitionPaymentPlanCalculator copyTo(TuitionPaymentPlan tuitionPaymentPlanTarget);
    
    public abstract TuitionPaymentPlanCalculator copyTo(TuitionCalculatorAggregator tuitionCalculatorAggregatorTarget);
    
    public abstract void fillWithParametersFromImportation(String parameters);

    public void editName(LocalizedString name) {
        super.setName(name);
    }

    public void delete() {
        super.setDomainRoot(null);
        super.setTuitionPaymentPlan(null);
        super.setTuitionCalculatorParentAggregator(null);
        
        getTuitionConditionRulesSet().forEach(r -> r.delete());
        
        super.deleteDomainObject();
    }

    public LocalizedString getCalculatorImplementationName() {
        return getPresentationNameFor(getClass());
    }

    public static LocalizedString getPresentationNameFor(
            Class<? extends TuitionPaymentPlanCalculator> tuitionPaymentPlanCalculatorClass) {
        try {
            final Method method = tuitionPaymentPlanCalculatorClass.getMethod("getCalculatorPresentationName", new Class[] {});
            return (LocalizedString) method.invoke(null, new Object[] {});
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
