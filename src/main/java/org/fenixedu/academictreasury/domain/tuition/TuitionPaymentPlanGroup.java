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
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.util.LocalizedStringUtil;
import pt.ist.fenixframework.FenixFramework;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Product;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;

public class TuitionPaymentPlanGroup extends TuitionPaymentPlanGroup_Base {

    public static final Comparator<TuitionPaymentPlanGroup> COMPARE_BY_NAME = new Comparator<TuitionPaymentPlanGroup>() {

        @Override
        public int compare(TuitionPaymentPlanGroup o1, TuitionPaymentPlanGroup o2) {
            int c = o1.getName().getContent().compareTo(o2.getName().getContent());

            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    protected TuitionPaymentPlanGroup() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected TuitionPaymentPlanGroup(final String code, final LocalizedString name, boolean forRegistration,
            boolean forStandalone, boolean forExtracurricular, final Product currentProduct) {
        this();
        setCode(code);
        setName(name);

        setForRegistration(forRegistration);
        setForStandalone(forStandalone);
        setForExtracurricular(forExtracurricular);
        setCurrentProduct(currentProduct);

        checkRules();
    }

    private void checkRules() {
        if (Strings.isNullOrEmpty(getCode())) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.name.required");
        }

        if (!(isForRegistration() ^ isForStandalone() ^ isForExtracurricular())) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.only.one.type.supported");
        }

        if (findDefaultGroupForRegistration().count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.for.registration.already.exists");
        }

        if (findDefaultGroupForStandalone().count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.for.standalone.already.exists");
        }

        if (findDefaultGroupForExtracurricular().count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.for.extracurricular.already.exists");
        }

        if (findByCode(getCode()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.code.already.exists");
        }
    }

    @Atomic
    public void edit(final String code, final LocalizedString name, final boolean forRegistration, final boolean forStandalone,
            final boolean forExtracurricular, final Product currentProduct) {
        setCode(code);
        setName(name);
        setForRegistration(forRegistration);
        setForStandalone(forStandalone);
        setForExtracurricular(forExtracurricular);
        setCurrentProduct(currentProduct);

        checkRules();
    }

    public boolean isForRegistration() {
        return getForRegistration();
    }

    public boolean isForStandalone() {
        return getForStandalone();
    }

    public boolean isForExtracurricular() {
        return getForExtracurricular();
    }

    public boolean isForImprovement() {
        return getForImprovement();
    }

    public boolean isDeletable() {
        // ACFSILVA
        return getAcademicTreasuryEventSet().isEmpty() && getTuitionPaymentPlansSet().isEmpty();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.delete.impossible");
        }

        setDomainRoot(null);

        super.deleteDomainObject();
    }

    public static Stream<TuitionPaymentPlanGroup> findAll() {
        return FenixFramework.getDomainRoot().getTuitionPaymentPlanGroupsSet().stream();
    }

    protected static Stream<TuitionPaymentPlanGroup> findDefaultGroupForRegistration() {
        return findAll().filter(t -> t.isForRegistration());
    }

    protected static Stream<TuitionPaymentPlanGroup> findDefaultGroupForStandalone() {
        return findAll().filter(t -> t.isForStandalone());
    }

    protected static Stream<TuitionPaymentPlanGroup> findDefaultGroupForImprovement() {
        return findAll().filter(t -> t.isForImprovement());
    }

    protected static Stream<TuitionPaymentPlanGroup> findDefaultGroupForExtracurricular() {
        return findAll().filter(t -> t.isForExtracurricular());
    }

    protected static Stream<TuitionPaymentPlanGroup> findByCode(final String code) {
        return findAll().filter(l -> StringNormalizer.normalize(l.getCode().toLowerCase())
                .equals(StringNormalizer.normalize(code).toLowerCase()));
    }

    public static Optional<TuitionPaymentPlanGroup> findUniqueDefaultGroupForRegistration() {
        return findDefaultGroupForRegistration().findFirst();
    }

    public static Optional<TuitionPaymentPlanGroup> findUniqueDefaultGroupForStandalone() {
        return findDefaultGroupForStandalone().findFirst();
    }

    public static Optional<TuitionPaymentPlanGroup> findUniqueDefaultGroupForExtracurricular() {
        return findDefaultGroupForExtracurricular().findFirst();
    }

    public static Optional<TuitionPaymentPlanGroup> findUniqueDefaultGroupForImprovement() {
        return findDefaultGroupForImprovement().findFirst();
    }

    @Atomic
    public static TuitionPaymentPlanGroup create(final String code, final LocalizedString name, boolean forRegistration,
            boolean forStandalone, boolean forExtracurricular, final Product currentProduct) {
        return new TuitionPaymentPlanGroup(code, name, forRegistration, forStandalone, forExtracurricular, currentProduct);
    }

}
