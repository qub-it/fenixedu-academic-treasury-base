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
package org.fenixedu.academictreasury.domain.settings;

import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.fenixedu.academictreasury.domain.emoluments.AcademicTax;
import org.fenixedu.academictreasury.domain.treasury.IAcademicTreasuryAccountUrl;

import pt.ist.fenixframework.FenixFramework;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.ProductGroup;

import pt.ist.fenixframework.Atomic;

public class AcademicTreasurySettings extends AcademicTreasurySettings_Base {

    protected AcademicTreasurySettings() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    @Atomic
    public void edit(final ProductGroup emolumentsProductGroup, final ProductGroup tuitionProductGroup,
            final AcademicTax improvementAcademicTax, final boolean closeServiceRequestEmolumentsWithDebitNote,
            boolean runAcademicDebtGenerationRuleOnNormalEnrolment) {
        setEmolumentsProductGroup(emolumentsProductGroup);
        setTuitionProductGroup(tuitionProductGroup);
        setImprovementAcademicTax(improvementAcademicTax);
        setCloseServiceRequestEmolumentsWithDebitNote(closeServiceRequestEmolumentsWithDebitNote);
        setRunAcademicDebtGenerationRuleOnNormalEnrolment(runAcademicDebtGenerationRuleOnNormalEnrolment);
    }

    @Atomic
    public void addAcademicalActBlockingProduct(final Product product) {
        super.addAcademicalActBlockingProducts(product);
    }

    @Atomic
    public void removeAcademicalActBlockingProduct(final Product product) {
        super.removeAcademicalActBlockingProducts(product);
    }

    @Atomic
    public void addProductsForAcademicalActBlocking(final Set<Product> products) {
        for (Product product : products) {
            addAcademicalActBlockingProduct(product);
        }
    }

    @Atomic
    public void removeProductsForAcademicalActBlocking(final Set<Product> products) {
        for (Product product : products) {
            removeAcademicalActBlockingProduct(product);
        }
    }

    public boolean isAcademicalActBlocking(final Product product) {
        return getAcademicalActBlockingProductsSet().contains(product);
    }

    public boolean isCloseServiceRequestEmolumentsWithDebitNote() {
        return getCloseServiceRequestEmolumentsWithDebitNote();
    }

    public boolean isRunAcademicDebtGenerationRuleOnNormalEnrolment() {
        return getRunAcademicDebtGenerationRuleOnNormalEnrolment();
    }
    
    public IAcademicTreasuryAccountUrl getAcademicTreasuryAccountUrl() {
        try {
            return (IAcademicTreasuryAccountUrl) ClassUtils.getClass(getAcademicTreasuryAccountUrlImpl()).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    protected static Optional<AcademicTreasurySettings> find() {
        return FenixFramework.getDomainRoot().getAcademicTreasurySettingsSet().stream().findFirst();
    }

    @Atomic
    public static AcademicTreasurySettings getInstance() {
        if (!find().isPresent()) {
            return new AcademicTreasurySettings();
        }

        return find().get();
    }

}
