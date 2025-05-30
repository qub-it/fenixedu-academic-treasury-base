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
package org.fenixedu.academictreasury.dto.tuition;

import java.math.BigDecimal;
import java.util.Map;

import org.fenixedu.academictreasury.domain.tuition.TuitionInstallmentTariff;
import org.fenixedu.academictreasury.dto.calculation.DebitEntryCalculationBean;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.joda.time.LocalDate;

public class TuitionDebitEntryBean implements DebitEntryCalculationBean {

    private int installmentOrder;
    private TuitionInstallmentTariff tuitionInstallmentTariff;
    private LocalizedString description;
    private LocalDate dueDate;
    private BigDecimal vatRate;
    private BigDecimal amount;
    private BigDecimal exemptedAmount;
    private Currency currency;

    private Map<TreasuryExemptionType, BigDecimal> exemptionsMap;

    private BigDecimal netAmountAlreadyCreated;

    public TuitionDebitEntryBean(int installmentOrder, TuitionInstallmentTariff tuitionInstallmentTariff,
            LocalizedString description, LocalDate dueDate, BigDecimal vatRate, BigDecimal amount, Currency currency) {
        super();
        this.installmentOrder = installmentOrder;
        this.tuitionInstallmentTariff = tuitionInstallmentTariff;
        this.description = description;
        this.dueDate = dueDate;
        this.vatRate = vatRate;
        this.amount = amount;
        this.currency = currency;
    }

    public TuitionDebitEntryBean(int installmentOrder, TuitionInstallmentTariff tuitionInstallmentTariff,
            LocalizedString description, LocalDate dueDate, BigDecimal vatRate, BigDecimal amount, BigDecimal exemptedAmount,
            Map<TreasuryExemptionType, BigDecimal> exemptionsMap, Currency currency) {
        this(installmentOrder, tuitionInstallmentTariff, description, dueDate, vatRate, amount, currency);
        this.exemptedAmount = exemptedAmount;
        this.exemptionsMap = exemptionsMap;
    }
    
    public int getInstallmentOrder() {
        return installmentOrder;
    }

    public void setInstallmentOrder(int installmentOrder) {
        this.installmentOrder = installmentOrder;
    }

    public TuitionInstallmentTariff getTuitionInstallmentTariff() {
        return tuitionInstallmentTariff;
    }

    public void setTuitionInstallmentTariff(TuitionInstallmentTariff tuitionInstallmentTariff) {
        this.tuitionInstallmentTariff = tuitionInstallmentTariff;
    }

    @Override
    public LocalizedString getDescription() {
        return description;
    }

    public void setDescription(LocalizedString description) {
        this.description = description;
    }

    @Override
    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getExemptedAmount() {
        return exemptedAmount;
    }

    public void setExemptedAmount(BigDecimal exemptedAmount) {
        this.exemptedAmount = exemptedAmount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Map<TreasuryExemptionType, BigDecimal> getExemptionsMap() {
        return exemptionsMap;
    }

    public void setExemptionsMap(Map<TreasuryExemptionType, BigDecimal> exemptionsMap) {
        this.exemptionsMap = exemptionsMap;
    }

    public BigDecimal getNetAmountAlreadyCreated() {
        return netAmountAlreadyCreated;
    }

    public void setNetAmountAlreadyCreated(BigDecimal netAmountAlreadyCreated) {
        this.netAmountAlreadyCreated = netAmountAlreadyCreated;
    }

    @Override
    public BigDecimal getAmountWithVat() {
        return getAmount();
    }

    @Override
    public BigDecimal getNetExemptedAmount() {
        return getExemptedAmount();
    }

}
