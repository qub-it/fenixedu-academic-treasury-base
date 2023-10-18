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
package org.fenixedu.academictreasury.domain.customer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEvent;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.CustomerType;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.FiscalDataUpdateLog;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.dto.AdhocCustomerBean;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.FiscalCodeValidation;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class PersonCustomer extends PersonCustomer_Base {

    private static final String STUDENT_CODE = "STUDENT";
    private static final String CANDIDACY_CODE = "CANDIDATE";

    public PersonCustomer() {
        super();
    }

    protected PersonCustomer(final Person person, final String fiscalAddressCountryCode, final String fiscalNumber) {
        this();

        // TODO: CHECK IF THIS CODE DO ANYTHING
        if (!DEFAULT_FISCAL_NUMBER.equals(getFiscalNumber())
                && find(getPerson(), getAddressCountryCode(), getFiscalNumber()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.person.customer.duplicated");
        }

        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        if (services.personCustomer(person) != null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.person.already.has.person.customer");
        }

        setPerson(person);
        setCustomerType(getDefaultCustomerType(this));

        super.setAddressCountryCode(fiscalAddressCountryCode);
        super.setFiscalNumber(fiscalNumber);

        if (!FiscalCodeValidation.isValidFiscalNumber(getAddressCountryCode(), getFiscalNumber())) {
            throw new TreasuryDomainException("error.Customer.fiscal.information.invalid");
        }

        checkRules();

        // create debt accounts for all active finantial instituions
        for (final FinantialInstitution finantialInstitution : FinantialInstitution.findAll().collect(Collectors.toSet())) {
            DebtAccount.create(finantialInstitution, this);
        }
    }

    @Override
    public void checkRules() {
        super.checkRules();

        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        /* Person Customer can be associated to Person with only one of two relations
         */

        if (getPerson() == null && getPersonForInactivePersonCustomer() == null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.person.required");
        }

        if (Strings.isNullOrEmpty(getAddressCountryCode())) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.countryCode");
        }

        if (Strings.isNullOrEmpty(getFiscalNumber())) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalNumber");
        }

        if (getPerson() != null && getPersonForInactivePersonCustomer() != null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.may.only.be.related.to.person.with.one.relation");
        }
        
        if (isActive() && services.fiscalAddress(getPerson()) == null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.person.fiscalAddress.required");
        }
        
        if(isActive() && services.fiscalAddress(getPerson()).getCountryOfResidence() == null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.person.countryOfResidence.required");
        }
        
        if(isActive() && !TreasuryConstants.isSameCountryCode(services.fiscalAddress(getPerson()).getCountryOfResidence().getCode(), getAddressCountryCode())) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.countryOfResidence.is.not.equal.of.customer");
        }

        if (DEFAULT_FISCAL_NUMBER.equals(getFiscalNumber()) && !TreasuryConstants.isDefaultCountry(getAddressCountryCode())) {
            throw new AcademicTreasuryDomainException(
                    "error.PersonCustomer.default.fiscal.number.applied.only.to.default.country");
        }

        if (!TreasuryConstants.isDefaultCountry(getAddressCountryCode()) || !DEFAULT_FISCAL_NUMBER.equals(getFiscalNumber())) {
            final Set<Customer> customers = findByFiscalInformation(getAddressCountryCode(), getFiscalNumber()) //
                    .filter(c -> c.isPersonCustomer()) //
                    .filter(c -> c.isActive()) //
                    .collect(Collectors.<Customer> toSet());

            if (customers.size() > 1) {
                final Customer self = this;
                final Set<String> otherCustomers =
                        customers.stream().filter(c -> c != self).map(c -> c.getName()).collect(Collectors.<String> toSet());

                throw new TreasuryDomainException("error.Customer.customer.with.fiscal.information.exists",
                        Joiner.on(", ").join(otherCustomers));
            }
        }
        
        final Person person = isActive() ? getPerson() : getPersonForInactivePersonCustomer();

        if (!DEFAULT_FISCAL_NUMBER.equals(getFiscalNumber())
                && find(person, getAddressCountryCode(), getFiscalNumber()).filter(pc -> !pc.isFromPersonMerge()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.person.customer.duplicated");
        }
    }

    // TODO: Remove both ::getCode and ::getCodeFromSuper when all Customer::code are set for PersonCustomer
    // in all instances
    
    @Override
    public String getCode() {
        // TODO: Test after deploy in quality that customer code is the same
        return super.getCode();
    }
    
    public String getCodeFromSuper() {
        return super.getCode();
    }

    public Person getAssociatedPerson() {
        return isActive() ? getPerson() : getPersonForInactivePersonCustomer();
    }

    public static String fiscalNumber(final Person person) {
        final IAcademicTreasuryPlatformDependentServices services =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        return services.fiscalNumber(person);
    }

    @Override
    public String getName() {
        return getAssociatedPerson().getName();
    }

    @Override
    public String getFirstNames() {
        return getAssociatedPerson().getGivenNames();
    }

    @Override
    public String getLastNames() {
        return getAssociatedPerson().getFamilyNames();
    }

    @Override
    public String getEmail() {
        return getAssociatedPerson().getDefaultEmailAddressValue();
    }

    @Override
    public String getPhoneNumber() {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        Person person = getAssociatedPerson();

        if (!Strings.isNullOrEmpty(services.defaultPhoneNumber(person))) {
            return services.defaultPhoneNumber(person);
        }

        return services.defaultMobilePhoneNumber(person);
    }

    public String getMobileNumber() {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();

        return services.defaultMobilePhoneNumber(getAssociatedPerson());
    }

    @Override
    public String getIdentificationNumber() {
        return identificationNumber(getAssociatedPerson());
    }

    public static String identificationNumber(final Person person) {
        return person.getDocumentIdNumber();
    }

    @Deprecated
    public PhysicalAddress getPhysicalAddress() {
        return physicalAddress(getAssociatedPerson());
    }

    @Deprecated
    public static PhysicalAddress physicalAddress(final Person person) {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();

        if (person.getDefaultPhysicalAddress() != null) {
            return person.getDefaultPhysicalAddress();
        }

        if (services.pendingOrValidPhysicalAddresses(person).size() == 1) {
            return services.pendingOrValidPhysicalAddresses(person).get(0);
        }

        return null;
    }

    public PhysicalAddress getFiscalAddress() {
        return fiscalAddress(getAssociatedPerson());
    }

    public static PhysicalAddress fiscalAddress(final Person person) {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        
        return services.fiscalAddress(person);
    }

    @Override
    public String getAddress() {
        if (!isActive()) {
            return super.getAddress();
        }

        return address(getFiscalAddress());
    }

    public static String address(final PhysicalAddress physicalAddress) {
        if (physicalAddress == null) {
            return null;
        }

        return physicalAddress.getAddress();
    }

    @Override
    public String getDistrictSubdivision() {
        if (!isActive()) {
            return super.getDistrictSubdivision();
        }

        return districtSubdivision(getFiscalAddress());
    }

    public static String districtSubdivision(final PhysicalAddress physicalAddress) {
        if (physicalAddress == null) {
            return null;
        }

        if (!Strings.isNullOrEmpty(physicalAddress.getDistrictSubdivisionOfResidence())) {
            return physicalAddress.getDistrictSubdivisionOfResidence();
        }

        if (!Strings.isNullOrEmpty(physicalAddress.getArea())) {
            return physicalAddress.getArea();
        }

        if (!Strings.isNullOrEmpty(physicalAddress.getDistrictOfResidence())) {
            return physicalAddress.getDistrictOfResidence();
        }

        return null;
    }

    @Override
    public String getRegion() {
        if (!isActive()) {
            return super.getRegion();
        }

        return region(getFiscalAddress());
    }

    public static String region(final PhysicalAddress physicalAddress) {
        if (physicalAddress == null) {
            return null;
        }

        if (!Strings.isNullOrEmpty(physicalAddress.getDistrictOfResidence())) {
            return physicalAddress.getDistrictOfResidence();
        }

        if (!Strings.isNullOrEmpty(physicalAddress.getDistrictSubdivisionOfResidence())) {
            return physicalAddress.getDistrictSubdivisionOfResidence();
        }

        if (!Strings.isNullOrEmpty(physicalAddress.getArea())) {
            return physicalAddress.getArea();
        }

        return null;
    }

    @Override
    public String getZipCode() {
        if (!isActive()) {
            return super.getZipCode();
        }

        return zipCode(getFiscalAddress());
    }

    public static String zipCode(final PhysicalAddress physicalAddress) {
        if (physicalAddress == null) {
            return null;
        }

        return physicalAddress.getAreaCode();
    }

    public static String addressCountryCode(final Person person) {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        PhysicalAddress fiscalAddress = services.fiscalAddress(person);
        
        return addressCountryCode(fiscalAddress);
    }

    public static String addressCountryCode(final PhysicalAddress physicalAddress) {
        if (physicalAddress == null) {
            return null;
        }

        if (physicalAddress.getCountryOfResidence() == null) {
            return null;
        }

        return physicalAddress.getCountryOfResidence().getCode();
    }

    /*
    @Deprecated
    public static String countryCode(final Person person) {
        final IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
    
        return services.fiscalCountry(person);
    }
    
    @Deprecated
    public static String countryCode(final PersonBean personBean) {
        if (personBean == null) {
            return null;
        }
    
        if (personBean.getFiscalCountry() == null) {
            return null;
        }
    
        return personBean.getFiscalCountry().getCode();
    }
    */

    
    public static String addressUiFiscalPresentationValue(PhysicalAddress pa) {
        final List<String> compounds = new ArrayList<>();
        
        if(StringUtils.isNotEmpty(address(pa))) {
            compounds.add(address(pa));
        }
        
        if(StringUtils.isNotEmpty(zipCode(pa))) {
            compounds.add(zipCode(pa));
        }
        
        if(StringUtils.isNotEmpty(districtSubdivision(pa))) {
            compounds.add(districtSubdivision(pa));
        }
        
        if(pa.getCountryOfResidence() != null) {
            compounds.add(pa.getCountryOfResidence().getLocalizedName().getContent());
        }
        
        return String.join(" ", compounds);
    }
    
    public static boolean isValidFiscalNumber(final Person person) {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        
        return services.fiscalAddress(person) != null && FiscalCodeValidation.isValidFiscalNumber(addressCountryCode(person), fiscalNumber(person));
    }

    @Override
    public String getNationalityCountryCode() {
        final Person person = getAssociatedPerson();

        if (person.getCountry() != null) {
            return person.getCountry().getCode();
        }

        return null;
    }

    @Override
    public String getBusinessIdentification() {
        final Person person = getAssociatedPerson();

        if (person.getStudent() != null) {
            return person.getStudent().getNumber().toString();
        }

        return this.getIdentificationNumber();
    }

    @Override
    public String getPaymentReferenceBaseCode() {
        return this.getCode();
    }

    @Override
    public boolean isPersonCustomer() {
        return true;
    }

    @Override
    public boolean isActive() {
        return getPerson() != null;
    }

    public boolean isFromPersonMerge() {
        return getFromPersonMerge();
    }

    @Override
    public boolean isDeletable() {
        return getDebtAccountsSet().stream().allMatch(da -> da.isDeletable());
    }

    @Override
    public Customer getActiveCustomer() {
        if (isActive()) {
            return this;
        }

        final Person person = getPersonForInactivePersonCustomer();
        final Optional<? extends PersonCustomer> activeCustomer =
                PersonCustomer.findUnique(person, addressCountryCode(person), fiscalNumber(person));

        if (!activeCustomer.isPresent()) {
            return null;
        }

        return activeCustomer.get();
    }

    @Override
    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.PersonCustomer.cannot.delete");
        }

        super.setPersonForInactivePersonCustomer(null);
        super.setPerson(null);

        for (DebtAccount debtAccount : getDebtAccountsSet()) {
            debtAccount.delete();
        }

        super.delete();
    }

    public boolean isBlockingAcademicalActs(final LocalDate when) {

        if (DebtAccount.find(this).map(da -> TreasuryConstants.isGreaterThan(da.getTotalInDebt(), BigDecimal.ZERO))
                .reduce((a, c) -> a || c).orElse(Boolean.FALSE)) {
            return DebitEntry.find(this).map(d -> isDebitEntryBlockingAcademicalActs(d, when)).reduce((a, c) -> a || c)
                    .orElse(Boolean.FALSE);
        }

        if (DebtAccount.find(this).anyMatch(da -> !da.getPaymentPlansNotCompliantSet(when).isEmpty())) {
            return true;
        }

        return false;
    }

    public static boolean isDebitEntryBlockingAcademicalActs(final DebitEntry debitEntry, final LocalDate when) {
        if (debitEntry.isAnnulled()) {
            return false;
        }

        if (!debitEntry.isInDebt()) {
            return false;
        }

        if (!debitEntry.isBlockAcademicActsOnDebt() && !debitEntry.isDueDateExpired(when)) {
            return false;
        }

        if (debitEntry.getOpenPaymentPlan() != null && debitEntry.getOpenPaymentPlan().isCompliant(when)) {
            return false;
        }

        if (!AcademicTreasurySettings.getInstance().isAcademicalActBlocking(debitEntry.getProduct())) {
            return false;
        }

        if (debitEntry.isAcademicalActBlockingSuspension()) {
            return false;
        }

        return true;
    }

    @Override
    public String getUsername() {
        return getAssociatedPerson().getUsername();
    }

    @Override
    public BigDecimal getGlobalBalance() {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();

        BigDecimal globalBalance = BigDecimal.ZERO;

        final Person person = isActive() ? getPerson() : getPersonForInactivePersonCustomer();

        if (services.personCustomer(person) != null) {
            for (final DebtAccount debtAccount : services.personCustomer(person).getDebtAccountsSet()) {
                globalBalance = globalBalance.add(debtAccount.getTotalInDebt());
            }
        }

        for (final PersonCustomer personCustomer : services.inactivePersonCustomers(person)) {
            for (final DebtAccount debtAccount : personCustomer.getDebtAccountsSet()) {
                globalBalance = globalBalance.add(debtAccount.getTotalInDebt());
            }
        }

        return globalBalance;
    }

    @Override
    public Set<Customer> getAllCustomers() {
        return PersonCustomer.find(getAssociatedPerson()).collect(Collectors.<Customer> toSet());
    }

    public void mergeWithPerson(final Person personToDelete) {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();

        if (getPerson() == personToDelete) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.merging.not.happening");
        }

        if (getPersonForInactivePersonCustomer() == personToDelete) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.merged.already.with.person");
        }

        if (services.personCustomer(personToDelete) != null) {
            final PersonCustomer personCustomer = services.personCustomer(personToDelete);
            personCustomer.saveFiscalAddressFieldsFromPersonInCustomer();
            personCustomer.setPersonForInactivePersonCustomer(getPerson());
            personCustomer.setPerson(null);
            personCustomer.setFromPersonMerge(true);
            personCustomer.checkRules();
        }

        for (final PersonCustomer personCustomer : services.inactivePersonCustomers(personToDelete)) {
            personCustomer.setPersonForInactivePersonCustomer(getPerson());
            personCustomer.setFromPersonMerge(true);
            personCustomer.checkRules();
        }

        final Person thisPerson = getAssociatedPerson();
        for (final AcademicTreasuryEvent e : Sets.newHashSet(services.academicTreasuryEventsSet(personToDelete))) {
            e.mergeToTargetPerson(thisPerson);
        }

        checkRules();
    }

    @Atomic
    public void changeFiscalNumber(AdhocCustomerBean bean, PhysicalAddress fiscalAddress) {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();

        if (!Strings.isNullOrEmpty(getErpCustomerId())) {
            throw new TreasuryDomainException("warning.Customer.changeFiscalNumber.maybe.integrated.in.erp");
        }

        final String oldAddressFiscalCountry = getAddressCountryCode();
        final String oldFiscalNumber = getFiscalNumber();

        final boolean changeFiscalNumberConfirmed = bean.isChangeFiscalNumberConfirmed();
        final boolean withFinantialDocumentsIntegratedInERP = isWithFinantialDocumentsIntegratedInERP();
        
        // 2023-02-12 ANIL: The platform no longer check if there are logs with ERP client
        // This was removed because it hinders the tests in quality or development servers, due to
        // lack of log files
        //
        // Checking if there is an operation with success should be enough
        final boolean customerInformationMaybeIntegratedWithSuccess = false;
        
        final boolean customerWithFinantialDocumentsIntegratedInPreviousERP =
                isCustomerWithFinantialDocumentsIntegratedInPreviousERP();

        if (!bean.isChangeFiscalNumberConfirmed()) {
            throw new TreasuryDomainException("message.Customer.changeFiscalNumber.confirmation");
        }

        final String addressCountryCode = fiscalAddress.getCountryOfResidence().getCode();
        final String fiscalNumber = bean.getFiscalNumber();

        if (Strings.isNullOrEmpty(addressCountryCode)) {
            throw new TreasuryDomainException("error.Customer.countryCode.required");
        }

        if (Strings.isNullOrEmpty(fiscalNumber)) {
            throw new TreasuryDomainException("error.Customer.fiscalNumber.required");
        }

        // Check if fiscal information is different from current information
        if (lowerCase(addressCountryCode).equals(lowerCase(getAddressCountryCode())) && fiscalNumber.equals(getFiscalNumber())) {
            throw new TreasuryDomainException("error.Customer.already.with.fiscal.information");
        }

        if (isFiscalValidated() && isFiscalCodeValid()) {
            throw new TreasuryDomainException("error.Customer.changeFiscalNumber.already.valid");
        }

        if (withFinantialDocumentsIntegratedInERP) {
            throw new TreasuryDomainException("error.Customer.changeFiscalNumber.documents.integrated.erp");
        }

        if (!FiscalCodeValidation.isValidFiscalNumber(addressCountryCode, fiscalNumber)) {
            throw new TreasuryDomainException("error.Customer.fiscal.information.invalid");
        }

        final Optional<? extends PersonCustomer> customerOptional =
                findUnique(getAssociatedPerson(), addressCountryCode, fiscalNumber);
        if (isActive()) {
            // Check if this customer has customer with same fiscal information
            if (customerOptional.isPresent()) {
                throw new TreasuryDomainException("error.Customer.changeFiscalNumber.customer.exists.for.fiscal.number");
            }

            setAddressCountryCode(addressCountryCode);
            setFiscalNumber(fiscalNumber);
            if(services.fiscalAddress(getPerson()) != null) {
                services.setFiscalAddress(services.fiscalAddress(getPerson()), false);
            }
            
            services.editSocialSecurityNumber(getPerson(), fiscalNumber, fiscalAddress);
        } else {
            // Check if this customer has customer with same fiscal information
            if (customerOptional.isPresent()) {
                // Mark as merged
                setFromPersonMerge(true);
            }

            setAddressCountryCode(addressCountryCode);
            setFiscalNumber(fiscalNumber);

            saveFiscalAddressFieldsInCustomer(fiscalAddress);
        }

        checkRules();

        FiscalDataUpdateLog.create(this, oldAddressFiscalCountry, oldFiscalNumber, changeFiscalNumberConfirmed,
                withFinantialDocumentsIntegratedInERP, customerInformationMaybeIntegratedWithSuccess,
                customerWithFinantialDocumentsIntegratedInPreviousERP);
    }

    @Override
    public Set<? extends TreasuryEvent> getTreasuryEventsSet() {
        final Person person = isActive() ? getPerson() : getPersonForInactivePersonCustomer();

        final Set<TreasuryEvent> result = Sets.newHashSet();
        for (IAcademicTreasuryEvent event : TreasuryBridgeAPIFactory.implementation().getAllAcademicTreasuryEventsList(person)) {
            result.add((TreasuryEvent) event);
        }

        result.addAll(getDebtAccountsSet().stream().flatMap(d -> d.getTreasuryEventsSet().stream()).collect(Collectors.toSet()));

        return result;
    }

    @Override
    public boolean isUiOtherRelatedCustomerActive() {
        return !isActive() && getActiveCustomer() != null;
    }

    @Override
    public String uiRedirectToActiveCustomer(final String url) {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        if (isActive() || !isUiOtherRelatedCustomerActive()) {
            return url + "/" + getExternalId();
        }

        return url + "/" + getActiveCustomer().getExternalId();
    }

    public static String uiPersonFiscalNumber(final Person person) {
        final String fiscalCountry = !Strings.isNullOrEmpty(addressCountryCode(person)) ? addressCountryCode(person) : "";
        final String fiscalNumber = !Strings.isNullOrEmpty(fiscalNumber(person)) ? fiscalNumber(person) : "";
        return fiscalCountry + " " + fiscalNumber;
    }

    @Override
    public LocalizedString getIdentificationTypeDesignation() {
        ITreasuryPlatformDependentServices implementation = TreasuryPlataformDependentServicesFactory.implementation();
        final Person person = getAssociatedPerson();
        
        if(person.getIdDocumentType() != null) {
            LocalizedString result = new LocalizedString();
            for (Locale locale : implementation.availableLocales()) {
                result = result.with(locale, person.getIdDocumentType().getLocalizedName(locale));
            }
            
            return result;
        }

        return null;
    }

    @Override
    public String getIdentificationTypeCode() {
        final Person person = getAssociatedPerson();

        if (person.getIdDocumentType() != null) {
            return person.getIdDocumentType().getName();
        }

        return null;
    }

    @Override
    public String getIban() {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        return services.iban(getAssociatedPerson());
    }

    public void saveFiscalAddressFieldsFromPersonInCustomer() {
        PhysicalAddress fiscalAddress = fiscalAddress(getAssociatedPerson());
        if (fiscalAddress == null) {
            // No fiscal address assigned, return
            return;
        }

        saveFiscalAddressFieldsInCustomer(fiscalAddress);
    }

    public void saveFiscalAddressFieldsInCustomer(final PhysicalAddress fiscalAddress) {
        if (fiscalAddress == null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.updateFiscalAddress.fiscalAddress.required");
        }

        if (fiscalAddress.getCountryOfResidence() == null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.updateFiscalAddress.countryOfResidence.required");
        }

        if (!TreasuryConstants.isSameCountryCode(getAddressCountryCode(), fiscalAddress.getCountryOfResidence().getCode())) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.countryOfResidence.is.not.equal.of.customer");
        }

        super.setAddress(address(fiscalAddress));
        super.setDistrictSubdivision(districtSubdivision(fiscalAddress));
        super.setRegion(region(fiscalAddress));
        super.setZipCode(zipCode(fiscalAddress));
    }

    protected void inactivateCustomer() {
        if (!isActive()) {
            throw new AcademicTreasuryDomainException("error.Person.inactivateCustomer.customer.not.active");
        }

        final Person person = this.getAssociatedPerson();
        setPerson(null);
        setPersonForInactivePersonCustomer(person);

        this.checkRules();
    }

    protected void activateCustomer() {
        if (isActive()) {
            throw new AcademicTreasuryDomainException("error.Person.activateCustomer.customer.active");
        }

        final Person person = this.getAssociatedPerson();
        setPerson(person);
        setPersonForInactivePersonCustomer(null);

        this.checkRules();
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<? extends PersonCustomer> findAll() {
        return Customer.findAll().filter(c -> c instanceof PersonCustomer).map(PersonCustomer.class::cast);
    }

    public static Stream<? extends PersonCustomer> find(final Person person) {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        final Set<PersonCustomer> result = Sets.newHashSet();

        if (person != null) {

            PersonCustomer personCustomer = services.personCustomer(person);
            if (personCustomer != null) {
                result.add(personCustomer);
            }

            result.addAll(services.inactivePersonCustomers(person));
        }

        return result.stream();
    }

    public static Stream<? extends PersonCustomer> find(final Person person, final String fiscalCountryCode,
            final String fiscalNumber) {
        return find(person).filter(pc -> !Strings.isNullOrEmpty(pc.getAddressCountryCode())
                && TreasuryConstants.isSameCountryCode(pc.getAddressCountryCode(), fiscalCountryCode)
                && !Strings.isNullOrEmpty(pc.getFiscalNumber()) && pc.getFiscalNumber().equals(fiscalNumber));
    }

    private static final Comparator<PersonCustomer> SORT_BY_PERSON_MERGE = (o1, o2) -> {
        if (!o1.isFromPersonMerge() && o2.isFromPersonMerge()) {
            return -1;
        } else if (o1.isFromPersonMerge() && !o2.isFromPersonMerge()) {
            return 1;
        }

        return o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static Optional<? extends PersonCustomer> findUnique(final Person person, final String fiscalCountryCode,
            final String fiscalNumber) {
        return find(person, fiscalCountryCode, fiscalNumber).sorted(SORT_BY_PERSON_MERGE).findFirst();
    }

    public static PersonCustomer createWithCurrentFiscalInformation(final Person person) {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
                
        if (services.fiscalAddress(person) == null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalAddress.required");
        }

        if(services.fiscalAddress(person).getCountryOfResidence() == null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalAddress.countryOfResidence.required");
        }

        if (Strings.isNullOrEmpty(person.getSocialSecurityNumber())) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalNumber.required");
        }
        
        final Country countryOfResidence = services.fiscalAddress(person).getCountryOfResidence();

        return create(person, countryOfResidence.getCode(), person.getSocialSecurityNumber());
    }
    
    public static PersonCustomer activePersonCustomer(Person person) {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        return services.personCustomer(person);
    }
    
    public static Set<PersonCustomer> inactivePersonCustomers(Person person) {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        return services.inactivePersonCustomers(person);
    }

    @Atomic
    public static PersonCustomer create(final Person person, final String fiscalAddressCountryCode, final String fiscalNumber) {
        return new PersonCustomer(person, fiscalAddressCountryCode, fiscalNumber);
    }

    public static boolean switchCustomer(final Person person, final String fiscalAddressCountryCode, final String fiscalNumber) {
        IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();

        final PersonCustomer personCustomer = services.personCustomer(person);
        Optional<? extends PersonCustomer> newCustomer = PersonCustomer.findUnique(person, fiscalAddressCountryCode, fiscalNumber);

        if (newCustomer.isPresent() && newCustomer.get().isActive()) {
            return false;
        }

        if (personCustomer != null) {
            personCustomer.inactivateCustomer();
        }

        if (!newCustomer.isPresent()) {
            PersonCustomer.create(person, fiscalAddressCountryCode, fiscalNumber);
            newCustomer = PersonCustomer.findUnique(person, fiscalAddressCountryCode, fiscalNumber);
        } else {
            newCustomer.get().activateCustomer();
        }

        return true;
    }

    public static CustomerType getDefaultCustomerType(final PersonCustomer person) {
        if (person.getPerson().getStudent() == null && !CustomerType.findByCode(CANDIDACY_CODE).findFirst().isEmpty()) {
            return CustomerType.findByCode(CANDIDACY_CODE).findFirst().get();
        }

        return CustomerType.findByCode(STUDENT_CODE).findFirst().get();
    }

}
