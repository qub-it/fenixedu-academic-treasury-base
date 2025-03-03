package org.fenixedu.academictreasury.domain.tuition;

import java.util.Comparator;

import org.fenixedu.treasury.domain.Product;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public class TuitionPaymentPlanRecalculation extends TuitionPaymentPlanRecalculation_Base {

    public static final Comparator<TuitionPaymentPlanRecalculation> SORT_BY_PRODUCT =
            Comparator.comparing(TuitionPaymentPlanRecalculation::getProduct, Product.COMPARE_BY_INSTALLMENT_NUMBER_AND_NAME)
                    .thenComparing(TuitionPaymentPlanRecalculation::getExternalId);

    public TuitionPaymentPlanRecalculation() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public TuitionPaymentPlanRecalculation(Product product, LocalDate recalculationDueDate) {
        this();

        setProduct(product);
        setRecalculationDueDate(recalculationDueDate);

        checkRules();
    }

    private void checkRules() {
        // TODO Auto-generated method stub

    }

    // ANIL 2025-03-03 (#qubIT-Fenix-6664)
    public TuitionPaymentPlanRecalculation createCopy() {
        return create(getProduct(), getRecalculationDueDate());
    }

    public void delete() {
        setDomainRoot(null);
        setProduct(null);
        setTuitionPaymentPlan(null);

        super.deleteDomainObject();
    }

    public static TuitionPaymentPlanRecalculation create(Product product, LocalDate recalculationDueDate) {
        return new TuitionPaymentPlanRecalculation(product, recalculationDueDate);
    }

}
