package org.fenixedu.academictreasury.domain.emoluments;

import java.util.stream.Stream;

import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituationType;
import org.fenixedu.academic.domain.serviceRequests.ServiceRequestType;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.serviceRequests.ITreasuryServiceRequest;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class ServiceRequestMapEntry extends ServiceRequestMapEntry_Base {

    public ServiceRequestMapEntry() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected ServiceRequestMapEntry(Product product, ServiceRequestType requestType,
            AcademicServiceRequestSituationType createEventOnSituation, boolean generatePaymentCode,
            DigitalPaymentPlatform digitalPaymentPlatform, String debitEntryDescriptionExtensionFormat) {
        this();

        setProduct(product);
        setServiceRequestType(requestType);
        setCreateEventOnSituation(createEventOnSituation);
        setGeneratePaymentCode(generatePaymentCode);
        setDigitalPaymentPlatform(digitalPaymentPlatform);
        setDebitEntryDescriptionExtensionFormat(debitEntryDescriptionExtensionFormat);

        checkRules();
    }

    private void checkRules() {

        if (getProduct() == null) {
            throw new AcademicTreasuryDomainException("error.ServiceRequestMapEntry.product.required");
        }

        if (getServiceRequestType() == null) {
            throw new AcademicTreasuryDomainException("error.ServiceRequestMapEntry.serviceRequestType.required");
        }

        if (getCreateEventOnSituation() == null) {
            throw new AcademicTreasuryDomainException("error.ServiceRequestMapEntry.createEventOnSituation.required");
        }

        if (find(getServiceRequestType()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.ServiceRequestMapEntry.duplicatedForServiceRequestType");
        }

    }

    private boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new AcademicTreasuryDomainException("error.ServiceRequestMapEntry.delete.impossible");
        }

        setServiceRequestType(null);
        setProduct(null);
        setDigitalPaymentPlatform(null);
        setDomainRoot(null);
        super.deleteDomainObject();
    }

    /*---------
     * SERVICES
     * --------
     */

    public static Stream<ServiceRequestMapEntry> findAll() {
        return FenixFramework.getDomainRoot().getServiceRequestMapEntriesSet().stream();
    }

    public static Stream<ServiceRequestMapEntry> find(final Product product) {
        return product.getServiceRequestMapEntriesSet().stream();
    }

    public static Stream<ServiceRequestMapEntry> find(final ServiceRequestType requestType) {
        return requestType.getServiceRequestMapEntriesSet().stream();
    }

    public static Stream<ServiceRequestMapEntry> find(final Product product, final ServiceRequestType requestType) {
        return find(product).filter(e -> e.getServiceRequestType() == requestType);
    }

    public static ServiceRequestMapEntry findMatch(final ITreasuryServiceRequest iTreasuryServiceRequest) {
        final ServiceRequestType serviceRequestType = iTreasuryServiceRequest.getServiceRequestType();
        if (find(serviceRequestType).count() > 1) {
            throw new AcademicTreasuryDomainException("error.ServiceRequestMapEntry.duplicatedForServiceRequestType");
        }
        return find(serviceRequestType).findFirst().orElse(null);
    }

    public static Product findProduct(final ITreasuryServiceRequest iTreasuryServiceRequest) {
        if (findMatch(iTreasuryServiceRequest) == null) {
            throw new AcademicTreasuryDomainException("error.ServiceRequestMapEntry.cannot.find.serviceRequestMapEntry");
        }

        return findMatch(iTreasuryServiceRequest).getProduct();
    }

    @Atomic
    public static ServiceRequestMapEntry create(final Product product, final ServiceRequestType requestType,
            AcademicServiceRequestSituationType situationType, final String debitEntryDescriptionExtensionFormat) {
        return new ServiceRequestMapEntry(product, requestType, situationType, false, null, debitEntryDescriptionExtensionFormat);
    }

    @Atomic
    public static ServiceRequestMapEntry create(final Product product, final ServiceRequestType requestType,
            AcademicServiceRequestSituationType situationType, final boolean generatePaymentCode,
            final DigitalPaymentPlatform paymentCodePool, final String debitEntryDescriptionExtensionFormat) {
        return new ServiceRequestMapEntry(product, requestType, situationType, generatePaymentCode, paymentCodePool,
                debitEntryDescriptionExtensionFormat);
    }

}
