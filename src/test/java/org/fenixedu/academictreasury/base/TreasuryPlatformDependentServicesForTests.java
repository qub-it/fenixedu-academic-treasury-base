package org.fenixedu.academictreasury.base;

import java.util.Collections;
import java.util.Set;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.erp.ISaftExporterConfiguration;
import org.joda.time.LocalDate;
import org.joda.time.Partial;

public class TreasuryPlatformDependentServicesForTests implements ITreasuryPlatformDependentServices {

    @Override
    public boolean isDynamicApplicationMessageDefined(String key) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public LocalizedString getDynamicApplicationMessage(String key) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ISaftExporterConfiguration getSaftExporterConfiguration(ERPConfiguration erpConfiguration) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Partial> getHolidays() {
        return Collections.emptySet();
    }

    /* Tax Authority Certification */

    @Override
    public void certifyDocument(FinantialDocument finantialDocument) {
    }

    @Override
    public void annulCertifiedDocument(FinantialDocument finantialDocument) {
    }

    @Override
    public boolean hasCertifiedDocument(FinantialDocument finantialDocument) {
        return false;
    }

    @Override
    public boolean isProductCertified(Product product) {
        return false;
    }

    @Override
    public String getCertifiedDocumentNumber(FinantialDocument finantialDocument) {
        return null;
    }

    @Override
    public LocalDate getCertifiedDocumentDate(FinantialDocument finantialDocument) {
        return null;
    }

}