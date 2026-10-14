/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/> All rights reserved.
 *
 * Redistribution and use in source and binary forms, without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution. * Neither the name of Quorum Born IT nor
 * the names of its contributors may be used to endorse or promote products derived from this software without specific prior
 * written permission. * Universidade de Lisboa and its respective subsidiary Serviços Centrais da Universidade de Lisboa
 * (Departamento de Informática), hereby referred to as the Beneficiary, is the sole demonstrated end-user and ultimately the only
 * beneficiary of the redistributed binary form and/or source code. * The Beneficiary is entrusted with either the binary form,
 * the source code, or both, and by accepting it, accepts the terms of this License. * Redistribution of any binary form and/or
 * source code is only allowed in the scope of the Universidade de Lisboa FenixEdu(™)’s implementation projects. * This license
 * and conditions of redistribution of source code/binary can only be reviewed by the Steering Comittee of FenixEdu(™)
 * <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL “Quorum Born IT” BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.academictreasury.services;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.serviceRequests.ITreasuryServiceRequest;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.domain.tariff.AcademicTariff;
import org.fenixedu.academictreasury.dto.academictax.AcademicDebitEntryBean;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

public class EmolumentServices {

    public static Stream<Product> findEmoluments(final FinantialEntity finantialEntity) {
        if (AcademicTreasurySettings.getInstance().getEmolumentsProductGroup() == null) {
            throw new AcademicTreasuryDomainException("error.EmolumentServices.emoluments.product.group.not.defined");
        }

        return AcademicTreasurySettings.getInstance().getEmolumentsProductGroup().getProductsSet().stream()
                .filter(l -> l.getFinantialInstitutionsSet().contains(finantialEntity.getFinantialInstitution()));
    }

    public static AcademicTreasuryEvent findAcademicTreasuryEvent(final ITreasuryServiceRequest iTreasuryServiceRequest) {
        return AcademicTreasuryEvent.findUnique(iTreasuryServiceRequest).orElse(null);
    }

    /* Custom Academic Debt */

    @Deprecated
    /*
     * 2026-09-02 (#qubIT-Fenix-9301)
     * TODO: Replace invocations by the method of the same name
     */ public static AcademicDebitEntryBean calculateForCustomAcademicDebt(final FinantialEntity finantialEntity,
            final Product product, final Registration registration, final ExecutionYear executionYear, final int numberOfUnits,
            final int numberOfPages, boolean urgentRequest, final LocalDate debtDate) {
        return calculateForCustomAcademicDebt(finantialEntity, product, registration, executionYear, numberOfUnits, numberOfPages,
                urgentRequest, null, debtDate);
    }

    public static AcademicDebitEntryBean calculateForCustomAcademicDebt(final FinantialEntity finantialEntity, Product product,
            Registration registration, ExecutionYear executionYear, int numberOfUnits, int numberOfPages, boolean urgentRequest,
            Locale language, LocalDate debtDate) {
        // Find tariff

        final AcademicTariff academicTariff =
                AcademicTariff.findMatch(finantialEntity, product, registration.getDegree(), debtDate.toDateTimeAtStartOfDay());

        if (academicTariff == null) {
            throw new AcademicTreasuryDomainException("error.EmolumentServices.tariff.not.found",
                    debtDate.toString(TreasuryConstants.DATE_FORMAT));
        }

        final LocalizedString debitEntryName =
                AcademicTreasuryEvent.nameForCustomAcademicDebt(product, registration, executionYear);
        final LocalDate dueDate = academicTariff.dueDate(debtDate);
        final Vat vat = academicTariff.vat(debtDate);
        final BigDecimal amount = academicTariff.amountToPay(numberOfUnits, numberOfPages, language, urgentRequest);

        return new AcademicDebitEntryBean(debitEntryName, dueDate, vat.getTaxRate(), amount);
    }

    @Deprecated
    /*
     * 2026-09-02 (#qubIT-Fenix-9301)
     * TODO: Replace invocations by the method of the same name
     */ public static boolean createCustomAcademicDebtForDefaultFinantialEntity(Product product, Registration registration,
            ExecutionYear executionYear, int numberOfUnits, int numberOfPages, boolean urgentRequest, LocalDate when) {
        return createCustomAcademicDebtForDefaultFinantialEntity(product, registration, executionYear, numberOfUnits,
                numberOfPages, urgentRequest, null, when);
    }

    public static boolean createCustomAcademicDebtForDefaultFinantialEntity(Product product, Registration registration,
            ExecutionYear executionYear, int numberOfUnits, int numberOfPages, boolean urgentRequest, Locale language,
            LocalDate when) {
        FinantialEntity finantialEntity = AcademicTreasuryConstants.getFinantialEntityOfDegree(registration.getDegree(), when);

        return createCustomAcademicDebt(finantialEntity, product, registration, executionYear, numberOfUnits, numberOfPages,
                urgentRequest, when);
    }

    @Deprecated
    /*
     * 2026-09-02 (#qubIT-Fenix-9301)
     * TODO: Replace invocations by the method of the same name
     */ public static boolean createCustomAcademicDebt(FinantialEntity finantialEntity, Product product,
            Registration registration, ExecutionYear executionYear, int numberOfUnits, int numberOfPages, boolean urgentRequest,
            LocalDate when) {
        return createCustomAcademicDebt(finantialEntity, product, registration, executionYear, numberOfUnits, numberOfPages,
                urgentRequest, null, when);
    }

    public static boolean createCustomAcademicDebt(FinantialEntity finantialEntity, Product product, Registration registration,
            ExecutionYear executionYear, int numberOfUnits, int numberOfPages, boolean urgentRequest, Locale language,
            LocalDate when) {

        final Person person = registration.getPerson();
        final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);

        // Read person customer
        if (!PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).isPresent()) {
            PersonCustomer.create(person, addressFiscalCountryCode, fiscalNumber);
        }

        final PersonCustomer personCustomer = PersonCustomer.findUnique(person, addressFiscalCountryCode, fiscalNumber).get();
        if (!personCustomer.isActive()) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.not.active", addressFiscalCountryCode, fiscalNumber);
        }

        final DebtAccount debtAccount = DebtAccount.findUnique(finantialEntity.getFinantialInstitution(), personCustomer).get();

        final AcademicTreasuryEvent academicTreasuryEvent =
                AcademicTreasuryEvent.createForCustomAcademicDebt(product, registration, executionYear, numberOfUnits,
                        numberOfPages, urgentRequest, when, language, null);

        final AcademicTariff academicTariff =
                AcademicTariff.findMatch(finantialEntity, product, registration.getDegree(), when.toDateTimeAtStartOfDay());

        if (academicTariff == null) {
            return false;
        }

        academicTariff.createDebitEntryForCustomAcademicDebt(debtAccount, academicTreasuryEvent, when);

        return true;
    }

}
