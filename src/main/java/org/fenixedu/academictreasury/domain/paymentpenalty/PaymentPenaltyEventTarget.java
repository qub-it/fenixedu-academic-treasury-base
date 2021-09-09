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

@Deprecated
// Replaced by fenixedu-treasury-base PaymentPenaltyTaxTreasuryEvent
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
        throw new RuntimeException("deprecated");
    }
    
    public static Stream<PaymentPenaltyEventTarget> findAll() {
        return FenixFramework.getDomainRoot().getPaymentPenaltyEventTargetsSet().stream();
    }
    
    public static Optional<PaymentPenaltyEventTarget> findUnique(DebitEntry originDebitEntry) {
        return Optional.ofNullable(originDebitEntry.getPaymentPenaltyEventTarget());
    }
    
}
