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
package org.fenixedu.academictreasury.domain.academicalAct;

import java.util.Comparator;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import pt.ist.fenixframework.FenixFramework;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;

import com.google.common.base.Strings;

public class AcademicActBlockingSuspension extends AcademicActBlockingSuspension_Base {
    
    public static Comparator<AcademicActBlockingSuspension> COMPARE_BY_BEGIN_DATE = new Comparator<AcademicActBlockingSuspension>() {
        
        @Override
        public int compare(AcademicActBlockingSuspension o1, AcademicActBlockingSuspension o2) {
            return o1.getBeginDate().compareTo(o2.getBeginDate());
        }
    };
    
    protected AcademicActBlockingSuspension() {
        super();
        
        setDomainRoot(FenixFramework.getDomainRoot());
    }
    
    protected AcademicActBlockingSuspension(final Person person, final LocalDate beginDate, final LocalDate endDate, final String reason) {
        this();
        
        setPerson(person);
        setBeginDate(beginDate);
        setEndDate(endDate);
        setReason(reason);
        
        checkRules();
    }
    
    private void checkRules() {
        if(getDomainRoot() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicActBlockingSuspension.bennu.required");
        }
        
        if(getPerson() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicActBlockingSuspension.person.required");
        }
        
        if(getBeginDate() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicActBlockingSuspension.beginDate.required");
        }
        
        if(getEndDate() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicActBlockingSuspension.endDate.required");
        }
        
        if(getEndDate().isBefore(getBeginDate())) {
            throw new AcademicTreasuryDomainException("error.AcademicActBlockingSuspension.endDate.must.be.after.or.equal.beginDate");
        }
        
        if(Strings.isNullOrEmpty(getReason())) {
            throw new AcademicTreasuryDomainException("error.AcademicActBlockingSuspension.reason.required");
        }
        
    }
    
    public Interval getDateInterval() {
        return new Interval(getBeginDate().toDateTimeAtStartOfDay(), getEndDate().plusDays(1).toDateTimeAtStartOfDay().minusSeconds(1));
    }
    
    public boolean isBlockingSuspended(final LocalDate when) {
        return getDateInterval().contains(when.toDateTimeAtStartOfDay());
    }

    @Atomic
    public void edit(final LocalDate beginDate, final LocalDate endDate, final String reason) {
        setBeginDate(beginDate);
        setEndDate(endDate);
        setReason(reason);
        
        checkRules();
    }

    private boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        if(!isDeletable()) {
            throw new AcademicTreasuryDomainException("error.AcademicActBlockingSuspension.delete.impossible");
        }
        
        setDomainRoot(null);
        setPerson(null);
        
        super.deleteDomainObject();
    }
    
    // @formatter:off
    /* --------
     * SERVICES
     * --------
     */
    // @formatter:on
    
    
    public static Stream<AcademicActBlockingSuspension> findAll() {
        return FenixFramework.getDomainRoot().getAcademicActBlockingSuspensionsSet().stream();
    }
    
    public static Stream<AcademicActBlockingSuspension> find(final Person person) {
         return findAll().filter(f -> f.getPerson() == person);
    }
    
    public static Stream<AcademicActBlockingSuspension> find(final Person person, final LocalDate when) {
        return find(person).filter(l -> l.isBlockingSuspended(when));
    }
    
    public static boolean isBlockingSuspended(final Person person, final LocalDate when) {
        return find(person, when).count() > 0;
    }
    
    @Atomic
    public static AcademicActBlockingSuspension create(final Person person, final LocalDate beginDate, final LocalDate endDate, final String reason) {
        return new AcademicActBlockingSuspension(person, beginDate, endDate, reason);
    }

}
