/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without modification, are permitted
 * provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 * * Neither the name of Quorum Born IT nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 * * Universidade de Lisboa and its respective subsidiary Serviços Centrais da Universidade
 * de Lisboa (Departamento de Informática), hereby referred to as the Beneficiary, is the
 * sole demonstrated end-user and ultimately the only beneficiary of the redistributed binary
 * form and/or source code.
 * * The Beneficiary is entrusted with either the binary form, the source code, or both, and
 * by accepting it, accepts the terms of this License.
 * * Redistribution of any binary form and/or source code is only allowed in the scope of the
 * Universidade de Lisboa FenixEdu(™)’s implementation projects.
 * * This license and conditions of redistribution of source code/binary can only be reviewed
 * by the Steering Comittee of FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT” BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.academictreasury.domain.tuition;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
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

        setPaymentPlanOrder(planOrder1 == null ? 1 : planOrder1.getPaymentPlanOrder());
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
        return find(degreeCurricularPlan)
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
        List<TuitionPaymentPlanOrder> collect = find(getDegreeCurricularPlan())
                .filter(
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
    
    // Services
    
    public static Stream<TuitionPaymentPlanOrder> findAll() {
        return FenixFramework.getDomainRoot().getTuitionPaymentPlansOrderSet().stream();
    }
    
    public static Stream<TuitionPaymentPlanOrder> find(DegreeCurricularPlan dcp) {
        return findAll().filter(o -> o.getDegreeCurricularPlan() == dcp);
    }
}
