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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PaymentPenaltySettings extends PaymentPenaltySettings_Base {

    public PaymentPenaltySettings() {
        super();

        setDomainRoot(FenixFramework.getDomainRoot());
        setActive(false);
        setCreatePaymentCode(false);
        setPaymentCodePool(null);

        checkRules();
    }

    private void checkRules() {
        if (super.getDomainRoot() == null) {
            throw new IllegalStateException("error.PaymentPenaltySettings.domainRoot.required");
        }

        if (super.getActive() == null) {
            throw new IllegalStateException("error.PaymentPenaltySettings.active.required");
        }

        if (Boolean.TRUE.equals(super.getActive()) && getEmolumentDescription() == null) {
            throw new IllegalStateException("error.PaymentPenaltySettings.is.active.but.emolumentDescription.is.null");
        }

        if (Boolean.TRUE.equals(super.getActive()) && getPenaltyProduct() == null) {
            throw new IllegalStateException("error.PaymentPenaltySettings.is.active.but.penaltyProduct.is.null");
        }

        if (super.getCreatePaymentCode() == null) {
            throw new IllegalStateException("error.PaymentPenaltySettings.createPaymentCode.required");
        }

        if (Boolean.TRUE.equals(super.getCreatePaymentCode()) && getPaymentCodePool() == null) {
            throw new IllegalStateException("error.PaymentPenaltySettings.paymentCodePool.required");
        }
        
        if(findAll().count() > 1) {
            throw new IllegalStateException("error.PaymentPenaltySettings.already.created");
        }
    }

    public LocalizedString buildEmolumentDescription(PaymentPenaltyEventTarget target) {
        LocalizedString result = new LocalizedString();
        for (Locale locale : TreasuryPlataformDependentServicesFactory.implementation().availableLocales()) {
            Map<String, String> valueMap = new HashMap<String, String>();
            valueMap.put("debitEntryDescription", target.getOriginDebitEntry().getDescription());
            valueMap.put("penaltyProductName", getPenaltyProduct().getName().getContent(locale));

            result = result.with(locale, StrSubstitutor.replace(getEmolumentDescription().getContent(locale), valueMap));
        }
        
        return result;
    }

    @Atomic
    public void delete() {
        setDomainRoot(null);
        setPaymentCodePool(null);
        super.deleteDomainObject();
    }

    public void edit(boolean active, Product penaltyProduct, LocalizedString emolumentDescription, boolean createPaymentCode,
            PaymentCodePool paymentCodePool) {
        super.setActive(active);

        super.setPenaltyProduct(penaltyProduct);
        super.setEmolumentDescription(emolumentDescription);
        super.setCreatePaymentCode(createPaymentCode);
        super.setPaymentCodePool(paymentCodePool);

        checkRules();
    }

    /*
     * SERVICES
     */

    public static PaymentPenaltySettings create() {
        return new PaymentPenaltySettings();
    }
    
    public static Stream<PaymentPenaltySettings> findAll() {
        return FenixFramework.getDomainRoot().getPaymentPenaltySettingsSet().stream();
    }

    public static PaymentPenaltySettings getInstance() {
        return FenixFramework.getDomainRoot().getPaymentPenaltySettingsSet().stream().findFirst().orElse(null);
    }

}
