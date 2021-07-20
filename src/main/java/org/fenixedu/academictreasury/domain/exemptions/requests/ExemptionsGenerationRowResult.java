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
package org.fenixedu.academictreasury.domain.exemptions.requests;

import java.math.BigDecimal;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.event.TreasuryEvent;

public class ExemptionsGenerationRowResult {

    private int rowNum;
    private Registration registration;
    private ExecutionYear executionYear;
    private TreasuryEvent treasuryEvent;
    private DebitEntry debitEntry;
    private BigDecimal amountToExempt;

    private String reason;

    private SortedSet<Integer> tuitionInstallmentsOrderSet;

    public ExemptionsGenerationRowResult(final int rowNum, final Registration registration, final ExecutionYear executionYear,
            final TreasuryEvent treasuryEvent, final DebitEntry debitEntry, final BigDecimal amountToExempt,
            final String reason, final SortedSet<Integer> tuitionInstallmentsOrderSet) {

        this.rowNum = rowNum;
        this.registration = registration;
        this.executionYear = executionYear;
        this.treasuryEvent = treasuryEvent;
        this.debitEntry = debitEntry;
        this.amountToExempt = amountToExempt;
        this.reason = reason;
        this.tuitionInstallmentsOrderSet = tuitionInstallmentsOrderSet;
    }

    public BigDecimal getDiscountAmount() {
        if (treasuryEvent instanceof AcademicTreasuryEvent && getAcademicTreasuryEvent().isForRegistrationTuition()) {
            throw new RuntimeException("invalid call");
        }

        if (debitEntry == null) {
            throw new RuntimeException("debit entry must not be null");
        }

        return calculateDebitEntryDiscount(debitEntry);
    }

    public BigDecimal getDiscountAmount(final int tuitionInstallmentOrder) {
        final DebitEntry tuitionDebitEntry = getTuitionDebitEntry(tuitionInstallmentOrder);
        
        if(tuitionDebitEntry == null) {
            throw new RuntimeException("debit entry must not be null");
        }
        
        return calculateDebitEntryDiscount(tuitionDebitEntry);
    }
    
    public DebitEntry getTuitionDebitEntry(final int tuitionInstallmentOrder) {
        final Set<? extends DebitEntry> debitEntriesSet = DebitEntry.findActive(getTreasuryEvent())
                .filter(d -> d.getProduct().getTuitionInstallmentOrder() == tuitionInstallmentOrder).collect(Collectors.<DebitEntry> toSet());

        if (debitEntriesSet.size() == 0) {
            return null;
        } else if (debitEntriesSet.size() > 1) {
            throw new AcademicTreasuryDomainException(
                    "error.ExemptionsGenerationRequestFile.installmentOrder.debit.entries.found.more.than.one",
                    String.valueOf(rowNum), String.valueOf(tuitionInstallmentOrder));
        }

        return debitEntriesSet.iterator().next();
    }

    public boolean isTreasuryEventForRegistrationTuition() {
        return  treasuryEvent instanceof AcademicTreasuryEvent && ((AcademicTreasuryEvent) treasuryEvent).isTuitionEvent();
    }
    
    private BigDecimal calculateDebitEntryDiscount(final DebitEntry debitEntry) {
        return amountToExempt;
    }

    // @formatter:off
    /* *****************
     * GETTERS & SETTERS
     * *****************
     */
    // @formatter:on

    
    
    public AcademicTreasuryEvent getAcademicTreasuryEvent() {
        return (AcademicTreasuryEvent) getTreasuryEvent();
    }
    
    public int getRowNum() {
        return rowNum;
    }

    public Registration getRegistration() {
        return registration;
    }

    public ExecutionYear getExecutionYear() {
        return executionYear;
    }

    public TreasuryEvent getTreasuryEvent() {
        return treasuryEvent;
    }

    public DebitEntry getDebitEntry() {
        return debitEntry;
    }

    public BigDecimal getAmountToExempt() {
        return amountToExempt;
    }

    public String getReason() {
        return reason;
    }

    public SortedSet<Integer> getTuitionInstallmentsOrderSet() {
        return tuitionInstallmentsOrderSet;
    }
}
