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
package org.fenixedu.academictreasury.domain.emoluments;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class AcademicTax extends AcademicTax_Base {

    public static final Comparator<AcademicTax> COMPARE_BY_PRODUCT_NAME = new Comparator<AcademicTax>() {

        @Override
        public int compare(final AcademicTax o1, final AcademicTax o2) {
            int c = o1.getProduct().getName().getContent().compareTo(o2.getProduct().getName().getContent());

            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    protected AcademicTax() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected AcademicTax(final Product product, final boolean appliedOnRegistration,
            final boolean appliedOnRegistrationFirstYear, final boolean appliedOnRegistrationSubsequentYears,
            final boolean appliedAutomatically) {
        this();

        setProduct(product);
        setAppliedOnRegistration(appliedOnRegistration);
        setAppliedOnRegistrationFirstYear(appliedOnRegistrationFirstYear);
        setAppliedOnRegistrationSubsequentYears(appliedOnRegistrationSubsequentYears);
        setAppliedAutomatically(appliedAutomatically);

        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTax.bennu.required");
        }

        if (getProduct() == null) {
            throw new AcademicTreasuryDomainException("error.AcademicTax.product.required");
        }

        if (!isAppliedOnRegistrationFirstYear() && !isAppliedOnRegistrationSubsequentYears()) {
            throw new AcademicTreasuryDomainException("error.AcademicTax.must.be.applied.on.some.registration.enrolment.year");
        }
    }

    public boolean isAppliedOnRegistration() {
        return super.getAppliedOnRegistration();
    }

    public boolean isAppliedOnRegistrationFirstYear() {
        return super.getAppliedOnRegistrationFirstYear();
    }

    public boolean isAppliedOnRegistrationSubsequentYears() {
        return super.getAppliedOnRegistrationSubsequentYears();
    }

    public boolean isAppliedAutomatically() {
        return super.getAppliedAutomatically();
    }

    public boolean isImprovementTax() {
        return this == AcademicTreasurySettings.getInstance().getImprovementAcademicTax();
    }

    @Atomic
    public void edit(boolean appliedOnRegistration, boolean appliedOnRegistrationFirstYear,
            boolean appliedOnRegistrationSubsequentYears, final boolean appliedAutomatically) {
        setAppliedOnRegistration(appliedOnRegistration);
        setAppliedOnRegistrationFirstYear(appliedOnRegistrationFirstYear);
        setAppliedOnRegistrationSubsequentYears(appliedOnRegistrationSubsequentYears);
        setAppliedAutomatically(appliedAutomatically);

        checkRules();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        if (!getAcademicTreasuryEventSet().isEmpty()) {
            blockers.add(academicTreasuryBundle("error.AcademicTax.delete.has.treasury.events"));
        }
        super.checkForDeletionBlockers(blockers);
    }

    private boolean isDeletable() {
        return getAcademicTreasuryEventSet().isEmpty();
    }

    @Atomic
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        if (!isDeletable()) {
            throw new AcademicTreasuryDomainException("error.AcademicTax.delete.impossible");
        }

        setDomainRoot(null);
        setProduct(null);

        super.deleteDomainObject();
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<AcademicTax> findAll() {
        return FenixFramework.getDomainRoot().getAcademicTaxesSet().stream();
    }

    public static Optional<AcademicTax> findUnique(final Product product) {
        return Optional.ofNullable(product.getAcademicTax());
    }

    @Atomic
    public static AcademicTax create(final Product product, final boolean appliedOnRegistration,
            final boolean appliedOnRegistrationFirstYear, final boolean appliedOnRegistrationSubsequentYears,
            final boolean appliedAutomatically) {
        if(product.getAcademicTax() != null) {
            throw new AcademicTreasuryDomainException("error.AcademicTax.create.academicTax.already.defined.for.product");
        }
        
        return new AcademicTax(product, appliedOnRegistration, appliedOnRegistrationFirstYear,
                appliedOnRegistrationSubsequentYears, appliedAutomatically);
    }
}
