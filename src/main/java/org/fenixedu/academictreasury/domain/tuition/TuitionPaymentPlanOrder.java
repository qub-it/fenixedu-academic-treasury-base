package org.fenixedu.academictreasury.domain.tuition;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class TuitionPaymentPlanOrder extends TuitionPaymentPlanOrder_Base {

    public static final Comparator<? super TuitionPaymentPlanOrder> COMPARE_BY_PAYMENT_PLAN_ORDER = (o1, o2) -> {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return 1;
        }
        if (o2 == null) {
            return -1;
        }
        return (o1.getPaymentPlanOrder() < o2.getPaymentPlanOrder() ? -1 : 1);
    };

    public TuitionPaymentPlanOrder() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public TuitionPaymentPlanOrder(TuitionPaymentPlan tuitionPaymentPlan, DegreeCurricularPlan degreeCurricularPlan) {
        this();

        TuitionPaymentPlanOrder planOrder1 = findSortedByPaymentPlanOrder(tuitionPaymentPlan.getTuitionPaymentPlanGroup(),
                degreeCurricularPlan, tuitionPaymentPlan.getExecutionYear())
                        .sorted(TuitionPaymentPlanOrder.COMPARE_BY_PAYMENT_PLAN_ORDER.reversed()).findFirst().orElse(null);

        setPaymentPlanOrder(planOrder1 == null ? 1 : planOrder1.getPaymentPlanOrder() + 1);
        setTuitionPaymentPlan(tuitionPaymentPlan);
        setDegreeCurricularPlan(degreeCurricularPlan);

        checkRules();

    }

    public static TuitionPaymentPlanOrder create(TuitionPaymentPlan tuitionPaymentPlan,
            DegreeCurricularPlan degreeCurricularPlan) {
        TuitionPaymentPlanOrder tuitionPaymentPlanOrder = new TuitionPaymentPlanOrder(tuitionPaymentPlan, degreeCurricularPlan);
        tuitionPaymentPlanOrder.setOnReachablePosition();
        return tuitionPaymentPlanOrder;
    }

    private void checkRules() {
        if (getTuitionPaymentPlan() == null) {
            throw new AcademicTreasuryDomainException("error.tuitionPaymentPlanOrder.tuitionPaymentPlan.required");
        }
        if (getDegreeCurricularPlan() == null) {
            throw new AcademicTreasuryDomainException("error.tuitionPaymentPlanOrder.degreeCurricularPlan.required");
        }

        if (findSortedByPaymentPlanOrder(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear())
                .filter(order -> order.getTuitionPaymentPlan() == getTuitionPaymentPlan()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.tuitionPaymentPlanOrder.tuitionPaymentPlan.duplicated");
        }
    }

    public void delete() {
        delete(true);
    }

    private void setOnReachablePosition() {
        while (!isReacheble()) {
            orderUp();
        }
    }

    public boolean isReacheble() {
        List<TuitionPaymentPlanOrder> tuitionPaymentPlanOrders =
                findSortedByPaymentPlanOrder(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear())
                        .collect(Collectors.toList());
        for (TuitionPaymentPlanOrder tuitionPlanOrder : tuitionPaymentPlanOrders) {
            if (this.getTuitionPaymentPlan() == tuitionPlanOrder.getTuitionPaymentPlan()) {
                return true;
            }
            if (this.compareOrderWithTuitionPlan(tuitionPlanOrder.getTuitionPaymentPlan()) < 0) {
                return false;
            }
        }
        return true;
    }

    private int compareOrderWithTuitionPlan(TuitionPaymentPlan plan) {
        TuitionPaymentPlan thisPlan = getTuitionPaymentPlan();

        if (thisPlan.isCustomized() || plan.isCustomized()) {
            return 0;
        }
        if (thisPlan.isDefaultPaymentPlan()) {
            return 1;
        }
        if (plan.isDefaultPaymentPlan()) {
            return -1;
        }
        if (thisPlan.equalsTuitionPlanConditions(plan)) {
            return -1;
        }
        if (thisPlan.containsTuitionPlanConditions(plan)) {
            return -1;
        }
        if (plan.containsTuitionPlanConditions(thisPlan)) {
            return 1;
        }
        return 0;
    }

    @Atomic
    public void orderUp() {
        if (isFirst()) {
            return;
        }

        final TuitionPaymentPlanOrder previous = getPreviousTuitionPaymentPlan();

        int order = previous.getPaymentPlanOrder();
        previous.setPaymentPlanOrder(getPaymentPlanOrder());
        setPaymentPlanOrder(order);
    }

    @Atomic
    public void orderDown() {
        if (isLast()) {
            return;
        }

        final TuitionPaymentPlanOrder next = getNextTuitionPaymentPlan();

        int order = next.getPaymentPlanOrder();
        next.setPaymentPlanOrder(getPaymentPlanOrder());
        setPaymentPlanOrder(order);
    }

    public boolean isFirst() {
        return findSortedByPaymentPlanOrder(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear())
                .collect(Collectors.toList()).get(0) == this;
    }

    public boolean isLast() {
        final List<TuitionPaymentPlanOrder> list =
                findSortedByPaymentPlanOrder(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear())
                        .collect(Collectors.toList());

        return list.get(list.size() - 1) == this;
    }

    public static Stream<TuitionPaymentPlanOrder> findSortedByPaymentPlanOrder(
            final TuitionPaymentPlanGroup tuitionPaymentPlanGroup, final DegreeCurricularPlan degreeCurricularPlan,
            final ExecutionYear executionYear) {
        return degreeCurricularPlan.getTuitionPaymentPlanOrdersSet().stream()
                .filter(o -> o.getTuitionPaymentPlan().getTuitionPaymentPlanGroup() == tuitionPaymentPlanGroup
                        && o.getTuitionPaymentPlan().getExecutionYear() == executionYear)
                .sorted(COMPARE_BY_PAYMENT_PLAN_ORDER);
    }

    private TuitionPaymentPlanOrder getPreviousTuitionPaymentPlan() {
        if (isFirst()) {
            return null;
        }

        return findSortedByPaymentPlanOrder(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear())
                .collect(Collectors.toList()).get(getPaymentPlanOrder() - 2);
    }

    private TuitionPaymentPlanOrder getNextTuitionPaymentPlan() {
        if (isLast()) {
            return null;
        }

        return findSortedByPaymentPlanOrder(getTuitionPaymentPlanGroup(), getDegreeCurricularPlan(), getExecutionYear())
                .collect(Collectors.toList()).get(getPaymentPlanOrder());
    }

    private ExecutionYear getExecutionYear() {
        return getTuitionPaymentPlan().getExecutionYear();
    }

    private TuitionPaymentPlanGroup getTuitionPaymentPlanGroup() {
        return getTuitionPaymentPlan().getTuitionPaymentPlanGroup();
    }

    public void delete(boolean deletePlan) {
        TuitionPaymentPlan tuitionPaymentPlan = getTuitionPaymentPlan();
        List<TuitionPaymentPlanOrder> collect = getDegreeCurricularPlan()
                .getTuitionPaymentPlanOrdersSet().stream().filter(
                        order -> order.getTuitionPaymentPlan().getExecutionYear() == tuitionPaymentPlan.getExecutionYear()
                                && order != this
                                && tuitionPaymentPlan.getTuitionPaymentPlanGroup() == order.getTuitionPaymentPlan()
                                        .getTuitionPaymentPlanGroup())
                .sorted(COMPARE_BY_PAYMENT_PLAN_ORDER).collect(Collectors.toList());
        setDomainRoot(null);
        setTuitionPaymentPlan(null);
        setDegreeCurricularPlan(null);
        if (deletePlan && tuitionPaymentPlan.getTuitionPaymentPlanOrdersSet().isEmpty()) {
            tuitionPaymentPlan.delete();
        }
        int i = 1;
        for (TuitionPaymentPlanOrder order : collect) {
            order.setPaymentPlanOrder(i);
            i++;
        }
        super.deleteDomainObject();
    }

    public TuitionPaymentPlanOrder copyToPlan(TuitionPaymentPlan tuitionPaymentPlan) {
        TuitionPaymentPlanOrder result = new TuitionPaymentPlanOrder();
        result.setDomainRoot(getDomainRoot());
        result.setDegreeCurricularPlan(getDegreeCurricularPlan());
        result.setPaymentPlanOrder(getPaymentPlanOrder());

        result.setTuitionPaymentPlan(tuitionPaymentPlan);
        result.checkRules();
        return result;
    }
}
