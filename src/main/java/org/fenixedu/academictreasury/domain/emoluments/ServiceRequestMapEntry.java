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

import java.util.stream.Stream;

import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequestSituationType;
import org.fenixedu.academic.domain.serviceRequests.ServiceRequestType;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.serviceRequests.ITreasuryServiceRequest;
import org.fenixedu.treasury.domain.Product;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class ServiceRequestMapEntry extends ServiceRequestMapEntry_Base {

    public ServiceRequestMapEntry() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected ServiceRequestMapEntry(Product product, ServiceRequestType requestType,
            AcademicServiceRequestSituationType createEventOnSituation, boolean generatePaymentCode,
            String debitEntryDescriptionExtensionFormat) {
        this();

        setProduct(product);
        setServiceRequestType(requestType);
        setCreateEventOnSituation(createEventOnSituation);
        setGeneratePaymentCode(generatePaymentCode);
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
        return new ServiceRequestMapEntry(product, requestType, situationType, false, debitEntryDescriptionExtensionFormat);
    }

    @Atomic
    public static ServiceRequestMapEntry create(final Product product, final ServiceRequestType requestType,
            AcademicServiceRequestSituationType situationType, final boolean generatePaymentCode,
            final String debitEntryDescriptionExtensionFormat) {
        return new ServiceRequestMapEntry(product, requestType, situationType, generatePaymentCode,
                debitEntryDescriptionExtensionFormat);
    }

}
