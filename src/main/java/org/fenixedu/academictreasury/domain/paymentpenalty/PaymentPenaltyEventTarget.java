package org.fenixedu.academictreasury.domain.paymentpenalty;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryTarget;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PaymentPenaltyEventTarget extends PaymentPenaltyEventTarget_Base implements IAcademicTreasuryTarget {

    public PaymentPenaltyEventTarget() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }
    
    protected PaymentPenaltyEventTarget(DebitEntry originDebitEntry) {
        this();
        
        setOriginDebitEntry(originDebitEntry);
        checkRules();
    }

    private void checkRules() {
        if(getDomainRoot() == null) {
            throw new IllegalStateException("error.PaymentPenaltyEventTarget.domainRoot.required");
        }
        
        if(getOriginDebitEntry() == null) {
            throw new IllegalStateException("error.PaymentPenaltyEventTarget.originDebitEntry.required");
        }
    }

    @Atomic
    public void delete() {
        setDomainRoot(null);
        setOriginDebitEntry(null);
        
        super.deleteDomainObject();
    }

    @Override
    public Degree getAcademicTreasuryTargetDegree() {
        if(getOriginDebitEntry().getTreasuryEvent() != null && getOriginDebitEntry().getTreasuryEvent() instanceof AcademicTreasuryEvent) {
            AcademicTreasuryEvent academicTreasuryEvent = (AcademicTreasuryEvent) getOriginDebitEntry().getTreasuryEvent();
            if(academicTreasuryEvent.getRegistration() != null) {
                return academicTreasuryEvent.getRegistration().getDegree();
            }
            
            return academicTreasuryEvent.getDegree();
        }
        
        return null;
    }

    @Override
    public LocalizedString getAcademicTreasuryTargetDescription() {
        return PaymentPenaltySettings.getInstance().buildEmolumentDescription(this);
    }

    @Override
    public LocalDate getAcademicTreasuryTargetEventDate() {
        return super.getOriginDebitEntry().getLastPaymentDate().toLocalDate();
    }

    @Override
    public ExecutionInterval getAcademicTreasuryTargetExecutionSemester() {
        return null;
    }

    @Override
    public ExecutionYear getAcademicTreasuryTargetExecutionYear() {
        if(getOriginDebitEntry().getTreasuryEvent() != null && getOriginDebitEntry().getTreasuryEvent() instanceof AcademicTreasuryEvent) {
            return ((AcademicTreasuryEvent) getOriginDebitEntry().getTreasuryEvent()).getExecutionYear();
        }
        
        return null;
    }

    @Override
    public Person getAcademicTreasuryTargetPerson() {
        if(getOriginDebitEntry().getDebtAccount().getCustomer().isPersonCustomer()) {
            return ((PersonCustomer) getOriginDebitEntry().getDebtAccount().getCustomer()).getAssociatedPerson();
        }
        
        throw new IllegalStateException("error.PaymentPenaltyEventTarget.person.not.found");
    }

    @Override
    public Map<String, String> getAcademicTreasuryTargetPropertiesMap() {
        return Collections.emptyMap();
    }

    @Override
    public Registration getAcademicTreasuryTargetRegistration() {
        if(getOriginDebitEntry().getTreasuryEvent() != null && getOriginDebitEntry().getTreasuryEvent() instanceof AcademicTreasuryEvent) {
            return ((AcademicTreasuryEvent) getOriginDebitEntry().getTreasuryEvent()).getRegistration();
        }
        
        return null;
    }

    @Override
    public void handleTotalPayment(IAcademicTreasuryEvent event) {
        
    }
    
    /* ********
     * SERVICES
     * ********
     */
    
    public static PaymentPenaltyEventTarget create(DebitEntry originDebitEntry) {
        return new PaymentPenaltyEventTarget(originDebitEntry);
    }
    
    public static Stream<PaymentPenaltyEventTarget> findAll() {
        return FenixFramework.getDomainRoot().getPaymentPenaltyEventTargetsSet().stream();
    }
    
    public static Optional<PaymentPenaltyEventTarget> findUnique(DebitEntry originDebitEntry) {
        return Optional.ofNullable(originDebitEntry.getPaymentPenaltyEventTarget());
    }
    
}
