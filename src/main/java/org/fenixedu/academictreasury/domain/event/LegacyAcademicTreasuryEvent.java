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
package org.fenixedu.academictreasury.domain.event;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.joda.time.LocalDate;

public class LegacyAcademicTreasuryEvent extends LegacyAcademicTreasuryEvent_Base {

    protected LegacyAcademicTreasuryEvent(final Person person, Product product, Degree degree, ExecutionYear executionYear,
            LocalizedString description) {
        super();
        this.setPerson(person);
        this.setProduct(product);
        this.setExecutionYear(executionYear);
        this.setDegree(degree);
        super.setDescription(description);

        checkRules();
    }

    public static LegacyAcademicTreasuryEvent create(final Person person, final Product product, final Degree degree,
            final ExecutionYear executionYear, LocalizedString description) {
        return new LegacyAcademicTreasuryEvent(person, product, degree, executionYear, description);
    }

    @Override
    protected void checkRules() {
        if (getPerson() == null) {
            throw new AcademicTreasuryDomainException("error.LegacyAcademicTreasuryEvent.person.required");
        }

        if (getProduct() == null) {
            throw new AcademicTreasuryDomainException("error.LegacyAcademicTreasuryEvent.product.required");
        }
    }

    @Override
    public String getDegreeCode() {
        if (getDegree() == null) {
            return null;
        }

        return getDegree().getCode();
    }
    
    @Override
    public TreasuryExemptionType getTreasuryExemptionToApplyInEventDiscountInTuitionFee() {
        return super.getExemptionToApplyInEventDiscountInTuitionFee();
    }
    
    @Override
    public boolean isAcademicServiceRequestEvent() {
        return false;
    }

    @Override
    public boolean isAcademicTax() {
        return false;
    }

    @Override
    public boolean isForAcademicServiceRequest() {
        return false;
    }

    @Override
    public boolean isForExtracurricularTuition() {
        return false;
    }

    @Override
    public boolean isForAcademicTax() {
        return false;
    }

    @Override
    public boolean isForImprovementTax() {
        return false;
    }

    @Override
    public boolean isForRegistrationTuition() {
        return false;
    }

    @Override
    public boolean isForStandaloneTuition() {
        return false;
    }

    @Override
    public boolean isImprovementTax() {
        return false;
    }

    @Override
    public boolean isLegacy() {
        return true;
    }

    @Override
    public boolean isUrgentRequest() {
        return false;
    }

    @Override
    public boolean isTuitionEvent() {
        return false;
    }

    @Override
    public LocalDate getTreasuryEventDate() {
        return this.getDueDate();
    }
    
    @Override
    public boolean isEventAccountedAsTuition() {
        return Boolean.TRUE.equals(getLegacyEventAccountedAsTuition());
    }
    
    @Override
    public boolean isEventDiscountInTuitionFee() {
        return Boolean.TRUE.equals(getLegacyEventDiscountInTuitionFee());
    }
    
}
