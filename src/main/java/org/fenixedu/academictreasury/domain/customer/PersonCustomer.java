package org.fenixedu.academictreasury.domain.customer;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryEvent;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academic.dto.person.PersonBean;
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
import org.fenixedu.treasury.util.TreasuryConstants;
import org.fenixedu.treasury.util.FiscalCodeValidation;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class PersonCustomer extends PersonCustomer_Base {

    private static final String STUDENT_CODE = "STUDENT";
    private static final String CANDIDACY_CODE = "CANDIDATE";

    protected PersonCustomer() {
        super();
    }

    protected PersonCustomer(final Person person, final String fiscalAddressCountryCode, final String fiscalNumber) {
        this();

        // TODO: CHECK IF THIS CODE DO ANYTHING
        if (!DEFAULT_FISCAL_NUMBER.equals(getFiscalNumber()) && find(getPerson(), getFiscalCountry(), getFiscalNumber()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.person.customer.duplicated");
        }

        setPerson(person);
        setCustomerType(getDefaultCustomerType(this));

        super.setAddressCountryCode(fiscalAddressCountryCode);
        super.setFiscalNumber(fiscalNumber);

        if (!FiscalCodeValidation.isValidFiscalNumber(getAddressCountryCode(), getFiscalNumber())) {
            throw new AcademicTreasuryDomainException("error.Customer.fiscal.information.invalid");
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
        
        if (isActive() && getPerson().getFiscalAddress() == null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.person.fiscalAddress.required");
        }
        
        if(isActive() && getPerson().getFiscalAddress().getCountryOfResidence() == null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.person.countryOfResidence.required");
        }
        
        if(isActive() && !TreasuryConstants.isSameCountryCode(getPerson().getFiscalAddress().getCountryOfResidence().getCode(), getAddressCountryCode())) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.countryOfResidence.is.not.equal.of.customer");
        }

        if (DEFAULT_FISCAL_NUMBER.equals(getFiscalNumber()) && !TreasuryConstants.isDefaultCountry(getFiscalCountry())) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.default.fiscal.number.applied.only.to.default.country");
        }

        final Person person = isActive() ? getPerson() : getPersonForInactivePersonCustomer();

        if (!DEFAULT_FISCAL_NUMBER.equals(getFiscalNumber())
                && find(person, getFiscalCountry(), getFiscalNumber()).filter(pc -> !pc.isFromPersonMerge()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.person.customer.duplicated");
        }
    }

    @Override
    public String getCode() {
        return this.getExternalId();
    }

    public Person getAssociatedPerson() {
        return isActive() ? getPerson() : getPersonForInactivePersonCustomer();
    }

    public static String fiscalNumber(final Person person) {
        final IAcademicTreasuryPlatformDependentServices services = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        
        return services.fiscalNumber(person);
    }

    @Override
    public String getName() {
        return getAssociatedPerson().getName();
    }

    @Override
    public String getFirstNames() {
        return getAssociatedPerson().getProfile().getGivenNames();
    }

    @Override
    public String getLastNames() {
        return getAssociatedPerson().getProfile().getFamilyNames();
    }

    @Override
    public String getEmail() {
        return getAssociatedPerson().getDefaultEmailAddressValue();
    }

    @Override
    public String getPhoneNumber() {
        final Person person = getAssociatedPerson();

        if (!Strings.isNullOrEmpty(person.getDefaultPhoneNumber())) {
            return person.getDefaultPhoneNumber();
        }

        return person.getDefaultMobilePhoneNumber();
    }

    public String getMobileNumber() {
        return getAssociatedPerson().getDefaultMobilePhoneNumber();
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
        if (person.getDefaultPhysicalAddress() != null) {
            return person.getDefaultPhysicalAddress();
        }

        if (person.getPendingOrValidPhysicalAddresses().size() == 1) {
            return person.getPendingOrValidPhysicalAddresses().get(0);
        }

        return null;
    }
    
    public PhysicalAddress getFiscalAddress() {
        return fiscalAddress(getAssociatedPerson());
    }
    
    public static PhysicalAddress fiscalAddress(final Person person) {
        return person.getFiscalAddress();
    }

    @Override
    public String getAddress() {
        if(!isActive()) {
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
        if(!isActive()) {
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
        if(!isActive()) {
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
        if(!isActive()) {
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
        final PhysicalAddress fiscalAddress = person.getFiscalAddress();
        
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

    public static boolean isValidFiscalNumber(final Person person) {
        return person.getFiscalAddress() != null && FiscalCodeValidation.isValidFiscalNumber(addressCountryCode(person), fiscalNumber(person));
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
    public String getFiscalCountry() {
        return getAddressCountryCode();
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
        BigDecimal globalBalance = BigDecimal.ZERO;

        final Person person = isActive() ? getPerson() : getPersonForInactivePersonCustomer();

        if (person.getPersonCustomer() != null) {
            for (final DebtAccount debtAccount : person.getPersonCustomer().getDebtAccountsSet()) {
                globalBalance = globalBalance.add(debtAccount.getTotalInDebt());
            }
        }

        for (final PersonCustomer personCustomer : person.getInactivePersonCustomersSet()) {
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

    public void mergeWithPerson(final Person person) {

        if (getPerson() == person) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.merging.not.happening");
        }

        if (getPersonForInactivePersonCustomer() == person) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.merged.already.with.person");
        }

        if (person.getPersonCustomer() != null) {
            final PersonCustomer personCustomer = person.getPersonCustomer();
            personCustomer.saveFiscalAddressFieldsFromPersonInCustomer();
            personCustomer.setPersonForInactivePersonCustomer(getPerson());
            personCustomer.setPerson(null);
            personCustomer.setFromPersonMerge(true);
            personCustomer.checkRules();
        }

        for (final PersonCustomer personCustomer : person.getInactivePersonCustomersSet()) {
            personCustomer.setPersonForInactivePersonCustomer(getPerson());
            personCustomer.setFromPersonMerge(true);
            personCustomer.checkRules();
        }

        final Person thisPerson = isActive() ? getPerson() : getPersonForInactivePersonCustomer();
        for (final AcademicTreasuryEvent e : Sets.newHashSet(person.getAcademicTreasuryEventSet())) {
            e.setPerson(thisPerson);
        }
        
        checkRules();
    }

    @Atomic
    public void changeFiscalNumber(final AdhocCustomerBean bean, final PhysicalAddress fiscalAddress) {
        if (!Strings.isNullOrEmpty(getErpCustomerId())) {
            throw new TreasuryDomainException("warning.Customer.changeFiscalNumber.maybe.integrated.in.erp");
        }

        final String oldAddressFiscalCountry = getAddressCountryCode();
        final String oldFiscalNumber = getFiscalNumber();
        
        final boolean changeFiscalNumberConfirmed = bean.isChangeFiscalNumberConfirmed();
        final boolean withFinantialDocumentsIntegratedInERP = isWithFinantialDocumentsIntegratedInERP();
        final boolean customerInformationMaybeIntegratedWithSuccess = isCustomerInformationMaybeIntegratedWithSuccess();
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

        if (customerInformationMaybeIntegratedWithSuccess) {
            throw new TreasuryDomainException("warning.Customer.changeFiscalNumber.maybe.integrated.in.erp");
        }

        if (withFinantialDocumentsIntegratedInERP) {
            throw new TreasuryDomainException("error.Customer.changeFiscalNumber.documents.integrated.erp");
        }

        if (!FiscalCodeValidation.isValidFiscalNumber(addressCountryCode, fiscalNumber)) {
            throw new TreasuryDomainException("error.Customer.fiscal.information.invalid");
        }

        final Optional<? extends PersonCustomer> customerOptional = findUnique(getAssociatedPerson(), addressCountryCode, fiscalNumber);
        if (isActive()) {
            // Check if this customer has customer with same fiscal information
            if (customerOptional.isPresent()) {
                throw new TreasuryDomainException("error.Customer.changeFiscalNumber.customer.exists.for.fiscal.number");
            }

            setAddressCountryCode(addressCountryCode);
            setFiscalNumber(fiscalNumber);
            if(getPerson().getFiscalAddress() != null) {
                getPerson().getFiscalAddress().setFiscalAddress(false);
            }
            
            getPerson().editSocialSecurityNumber(fiscalNumber, fiscalAddress);
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

        return result;
    }

    @Override
    public boolean isUiOtherRelatedCustomerActive() {
        return !isActive() && getPersonForInactivePersonCustomer().getPersonCustomer() != null;
    }

    @Override
    public String uiRedirectToActiveCustomer(final String url) {
        if (isActive() || !isUiOtherRelatedCustomerActive()) {
            return url + "/" + getExternalId();
        }

        return url + "/" + getPersonForInactivePersonCustomer().getPersonCustomer().getExternalId();
    }

    public static String uiPersonFiscalNumber(final Person person) {
        final String fiscalCountry = !Strings.isNullOrEmpty(addressCountryCode(person)) ? addressCountryCode(person) : "";
        final String fiscalNumber = !Strings.isNullOrEmpty(fiscalNumber(person)) ? fiscalNumber(person) : "";
        return fiscalCountry + " " + fiscalNumber;
    }
    
    @Override
    public LocalizedString getIdentificationTypeDesignation() {
        final Person person = getAssociatedPerson();
        
        if(person.getIdDocumentType() != null) {
            return person.getIdDocumentType().getLocalizedNameI18N();
        }
        
        return null;
    }
    
    @Override
    public String getIdentificationTypeCode() {
        final Person person = getAssociatedPerson();
        
        if(person.getIdDocumentType() != null) {
            return person.getIdDocumentType().getName();
        }
        
        return null;
    }
    
    @Override
    public String getIban() {
        return getAssociatedPerson().getIban();
    }
    
    public void saveFiscalAddressFieldsFromPersonInCustomer() {
        PhysicalAddress fiscalAddress = fiscalAddress(getAssociatedPerson());
        if(fiscalAddress == null) {
            // No fiscal address assigned, return
            return;
        }
        
        saveFiscalAddressFieldsInCustomer(fiscalAddress);
    }
    
    public void saveFiscalAddressFieldsInCustomer(final PhysicalAddress fiscalAddress) {
        if(fiscalAddress == null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.updateFiscalAddress.fiscalAddress.required");
        }
        
        if(fiscalAddress.getCountryOfResidence() == null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.updateFiscalAddress.countryOfResidence.required");
        }
        
        if(!TreasuryConstants.isSameCountryCode(getAddressCountryCode(), fiscalAddress.getCountryOfResidence().getCode())) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.countryOfResidence.is.not.equal.of.customer");
        }
        
        super.setAddress(address(fiscalAddress));
        super.setDistrictSubdivision(districtSubdivision(fiscalAddress));
        super.setRegion(region(fiscalAddress));
        super.setZipCode(zipCode(fiscalAddress));
    }
    
    protected void inactivateCustomer() {
        if(!isActive()) {
            throw new AcademicTreasuryDomainException("error.Person.inactivateCustomer.customer.not.active");
        }
        
        final Person person = this.getAssociatedPerson();
        setPerson(null);
        setPersonForInactivePersonCustomer(person);
        
        this.checkRules();
    }

    protected void activateCustomer() {
        if(isActive()) {
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
        final Set<PersonCustomer> result = Sets.newHashSet();

        if (person != null) {

            if (person.getPersonCustomer() != null) {
                result.add(person.getPersonCustomer());
            }

            result.addAll(person.getInactivePersonCustomersSet());
        }

        return result.stream();
    }

    public static Stream<? extends PersonCustomer> find(final Person person, final String fiscalCountryCode,
            final String fiscalNumber) {
        return find(person).filter(pc -> !Strings.isNullOrEmpty(pc.getFiscalCountry())
                && TreasuryConstants.isSameCountryCode(pc.getFiscalCountry(), fiscalCountryCode)
                && !Strings.isNullOrEmpty(pc.getFiscalNumber()) && pc.getFiscalNumber().equals(fiscalNumber));
    }

    private static final Comparator<PersonCustomer> SORT_BY_PERSON_MERGE = new Comparator<PersonCustomer>() {

        @Override
        public int compare(final PersonCustomer o1, final PersonCustomer o2) {
            if (!o1.isFromPersonMerge() && o2.isFromPersonMerge()) {
                return -1;
            } else if (o1.isFromPersonMerge() && !o2.isFromPersonMerge()) {
                return 1;
            }

            return o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    public static Optional<? extends PersonCustomer> findUnique(final Person person, final String fiscalCountryCode,
            final String fiscalNumber) {
        return find(person, fiscalCountryCode, fiscalNumber).sorted(SORT_BY_PERSON_MERGE).findFirst();
    }

    public static PersonCustomer createWithCurrentFiscalInformation(final Person person) {
        if (person.getFiscalAddress() == null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalAddress.required");
        }

        if(person.getFiscalAddress().getCountryOfResidence() == null) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalAddress.countryOfResidence.required");
        }
        
        if (!Strings.isNullOrEmpty(person.getSocialSecurityNumber())) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalNumber.required");
        }
        
        final Country countryOfResidence = person.getFiscalAddress().getCountryOfResidence();

        return create(person, countryOfResidence.getCode(), person.getSocialSecurityNumber());
    }

    @Atomic
    public static PersonCustomer create(final Person person, final String fiscalAddressCountryCode, final String fiscalNumber) {
        return new PersonCustomer(person, fiscalAddressCountryCode, fiscalNumber);
    }

    public static boolean switchCustomer(final Person person, final String fiscalAddressCountryCode, final String fiscalNumber) {
        final PersonCustomer personCustomer = person.getPersonCustomer();
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

        if (personCustomer != null) {
            personCustomer.checkRules();
        }

        newCustomer.get().checkRules();

        return true;
    }

    public static CustomerType getDefaultCustomerType(final PersonCustomer person) {
        if (person.getPerson().getStudent() != null) {
            return CustomerType.findByCode(STUDENT_CODE).findFirst().orElse(null);
        } else {
            return CustomerType.findByCode(CANDIDACY_CODE).findFirst().orElse(null);
        }
    }

}
