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
package org.fenixedu.academictreasury.domain.treasury;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.treasury.IAcademicServiceRequestAndAcademicTaxTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEvent;
import org.fenixedu.academic.domain.treasury.IImprovementTreasuryEvent;
import org.fenixedu.academic.domain.treasury.ITreasuryBridgeAPI;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.services.PersonServices;
import org.fenixedu.academictreasury.services.TuitionServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.util.FiscalCodeValidation;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;

@Deprecated
public class AcademicTreasuryBridgeImpl implements ITreasuryBridgeAPI {

    private HandleSettlementNotePayment handleSettlementNotePayment = new HandleSettlementNotePayment();

    /* ----------
     * ENROLMENTS
     * ----------
     */

    @Override
    @Deprecated
    public void standaloneUnenrolment(Enrolment standaloneEnrolment) {
        TuitionServices.removeDebitEntryForStandaloneEnrolment(standaloneEnrolment);
    }

    @Override
    @Deprecated
    public void extracurricularUnenrolment(Enrolment extracurricularEnrolment) {
        TuitionServices.removeDebitEntryForExtracurricularEnrolment(extracurricularEnrolment);
    }

    /* --------------
     * ACADEMIC TAXES
     * --------------
     */

    @Override
    @Deprecated
    public IImprovementTreasuryEvent getImprovementTaxTreasuryEvent(Registration registration, ExecutionYear executionYear) {
        if (!AcademicTreasuryEvent.findUniqueForImprovementTuition(registration, executionYear).isPresent()) {
            return null;
        }

        return AcademicTreasuryEvent.findUniqueForImprovementTuition(registration, executionYear).get();
    }

    /* --------------
     * ACADEMICAL ACT
     * --------------
     */

    @Override
    @Deprecated
    public boolean isAcademicalActsBlocked(final Person person, final LocalDate when) {
        return PersonServices.isAcademicalActsBlocked(person, when);
    }

    /* -----
     * OTHER
     * -----
     */

    @Override
    @Deprecated
    public List<IAcademicTreasuryEvent> getAllAcademicTreasuryEventsList(final Person person) {
        return AcademicTreasuryEvent.find(person).collect(Collectors.<IAcademicTreasuryEvent> toList());
    }

    @Override
    @Deprecated
    public String getPersonAccountTreasuryManagementURL(final Person person) {
        return AcademicTreasurySettings.getInstance().getAcademicTreasuryAccountUrl()
                .getPersonAccountTreasuryManagementURL(person);
    }

    @Override
    @Deprecated
    public String getRegistrationAccountTreasuryManagementURL(Registration registration) {
        return AcademicTreasurySettings.getInstance().getAcademicTreasuryAccountUrl()
                .getRegistrationAccountTreasuryManagementURL(registration);
    }

    // TODO After the virtual payments going to production, replace Bennu Signals  with settlement note handlers
    // The settlement creation is now in SettlementNote.processSettlementNoteCreation(SettlementNoteBean) except 
    // when TreasurySettings.isRestrictPaymentMixingLegacyInvoices==true . 
    // This handle could be called in SettlementNote.processSettlementNoteCreation(SettlementNoteBean)
    // For now maintain it
    @Subscribe
    public void handle(final DomainObjectEvent<SettlementNote> settlementNoteEvent) {
        final SettlementNote settlementNote = settlementNoteEvent.getInstance();

        this.handleSettlementNotePayment.handleObject(settlementNote);
    }

    @Override
    @Deprecated
    public boolean isValidFiscalNumber(final String fiscalAddressCountryCode, final String fiscalNumber) {
        return FiscalCodeValidation.isValidFiscalNumber(fiscalAddressCountryCode, fiscalNumber);
    }

    @Override
    @Deprecated
    public boolean updateCustomer(final Person person, final String fiscalCountryCode, final String fiscalNumber) {
        return PersonCustomer.switchCustomer(person, fiscalCountryCode, fiscalNumber);
    }

    @Override
    @Deprecated
    public boolean createCustomerIfMissing(final Person person) {
        final String fiscalCountryCode = PersonCustomer.addressCountryCode(person);
        final String fiscalNumber = PersonCustomer.fiscalNumber(person);

        if (Strings.isNullOrEmpty(fiscalCountryCode) || Strings.isNullOrEmpty(fiscalNumber)) {
            // 2022-09-21: Remove the restriction of fiscal information requirement in
            // creating a registration. The caller decide whether to throw exception or not
            // Simply return false and delegate the customer creation requirement by the caller
            return false;
        }

        final Optional<? extends PersonCustomer> findUnique = PersonCustomer.findUnique(person, fiscalCountryCode, fiscalNumber);
        if (findUnique.isPresent()) {
            // If it is present return true, to inform that customer is created
            return true;
        }

        PersonCustomer.create(person, fiscalCountryCode, fiscalNumber);

        return true;
    }

    @Override
    @Deprecated
    public void saveFiscalAddressFieldsFromPersonInActiveCustomer(final Person person) {
        if (person.getPersonCustomer() == null) {
            return;
        }

        person.getPersonCustomer().saveFiscalAddressFieldsFromPersonInCustomer();
    }

    @Override
    @Deprecated
    public PhysicalAddress createSaftDefaultPhysicalAddress(final Person person) {
        String unknownAddress =
                AcademicTreasuryConstants.academicTreasuryBundle("label.AcademicTreasuryBridgeImpl.unknown.address");
        PhysicalAddress result =
                AcademicTreasuryConstants.createPhysicalAddress(person, Country.readDefault(), unknownAddress, unknownAddress,
                        "0000-000", unknownAddress);
        result.setValid();

        return result;
    }

}
