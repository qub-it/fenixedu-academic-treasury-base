package org.fenixedu.academictreasury.domain.tuition;

import java.util.Optional;
import java.util.Set;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class TuitionAllocation extends TuitionAllocation_Base {

    public TuitionAllocation() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
        setResponsible(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());
    }

    public TuitionAllocation(TuitionPaymentPlanGroup tuitionPaymentPlanGroup, FinantialEntity finantialEntity,
            Registration registration, ExecutionInterval executionInterval, TuitionDebtPostingType tuitionDebtPostingType,
            TuitionPaymentPlan tuitionPaymentPlan, Set<TreasuryExemptionType> treasuryExemptionTypesSet) {
        this();

        setTuitionPaymentPlanGroup(tuitionPaymentPlanGroup);
        setFinantialEntity(finantialEntity);
        setRegistration(registration);
        setExecutionInterval(executionInterval);
        setTuitionDebtPostingType(tuitionDebtPostingType);
        setTuitionPaymentPlan(tuitionPaymentPlan);
        getTreasuryExemptionTypesSet().addAll(treasuryExemptionTypesSet);

        checkRules();
    }

    public void checkRules() {
        if (getDomainRoot() == null) {
            throw new RuntimeException("error.TuitionAllocation.domainRoot.required");
        }

        if (getFinantialEntity() == null) {
            throw new RuntimeException("error.TuitionAllocation.finantialEntity.required");
        }

        if (getRegistration() == null) {
            throw new RuntimeException("error.TuitionAllocation.registration.required");
        }

        if (getTuitionPaymentPlanGroup() == null) {
            throw new RuntimeException("error.TuitionAllocation.tuitionPaymentPlanGroup.required");
        }

        if (getExecutionInterval() == null) {
            throw new RuntimeException("error.TuitionAllocation.executionInterval.required");
        }

        if (getTuitionPaymentPlan() == null) {
            throw new RuntimeException("error.TuitionAllocation.tuitionPaymentPlan.required");
        }

        if (getTuitionDebtPostingType() == null) {
            throw new RuntimeException("error.TuitionAllocation.tuitionDebtPostingType.required");
        }

        if (getTuitionDebtPostingType().getFinantialEntity() != getFinantialEntity()) {
            throw new RuntimeException("error.TuitionAllocation.tuitionDebtPostingType.and.finantialEntity.differs");
        }

        if (getRegistration().getTuitionAllocationsSet().stream().filter(e -> e != this)
                .filter(e -> e.getTuitionPaymentPlanGroup() == getTuitionPaymentPlanGroup())
                .filter(e -> e.getTuitionDebtPostingType() == getTuitionDebtPostingType())
                .anyMatch(e -> e.getExecutionInterval() == getExecutionInterval())) {
            throw new AcademicTreasuryDomainException("error.TuitionAllocation.duplicated");
        }
    }

    public void delete() {
        super.setDomainRoot(null);
        super.setFinantialEntity(null);
        super.setExecutionInterval(null);
        super.setRegistration(null);
        super.setTuitionDebtPostingType(null);
        super.setTuitionPaymentPlanGroup(null);
        super.setTuitionPaymentPlan(null);

        getTreasuryExemptionTypesSet().forEach(t -> removeTreasuryExemptionTypes(t));

        super.deleteDomainObject();
    }

    public static TuitionAllocation create(TuitionPaymentPlanGroup tuitionPaymentPlanGroup, FinantialEntity finantialEntity,
            Registration registration, ExecutionInterval executionInterval, TuitionDebtPostingType tuitionDebtPostingType,
            TuitionPaymentPlan tuitionPaymentPlan, Set<TreasuryExemptionType> treasuryExemptionTypesSet) {

        return new TuitionAllocation(tuitionPaymentPlanGroup, finantialEntity, registration, executionInterval,
                tuitionDebtPostingType, tuitionPaymentPlan, treasuryExemptionTypesSet);

    }

    public static Optional<TuitionAllocation> findUnique(TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            Registration registration, ExecutionInterval executionInterval, TuitionDebtPostingType tuitionDebtPostingType) {

        return registration.getTuitionAllocationsSet().stream()
                .filter(t -> t.getTuitionPaymentPlanGroup() == tuitionPaymentPlanGroup)
                .filter(t -> t.getExecutionInterval() == executionInterval)
                .filter(t -> t.getTuitionDebtPostingType() == tuitionDebtPostingType).findFirst();
    }

}
