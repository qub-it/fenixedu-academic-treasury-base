package org.fenixedu.academictreasury.base;

import java.io.InputStream;
import java.util.Collections;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.payline.PaylineConfiguration;
import org.fenixedu.treasury.domain.forwardpayments.payline.PaylineWebServiceResponse;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.erp.IERPExternalService;
import org.fenixedu.treasury.services.integration.erp.ISaftExporterConfiguration;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Partial;

import pt.ist.fenixframework.DomainObject;

public class TreasuryPlatformDependentServicesForTests implements ITreasuryPlatformDependentServices {

    @Override
    public byte[] getFileContent(IGenericFile genericFile) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getFileSize(IGenericFile genericFile) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getFilename(IGenericFile genericFile) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getFileStream(IGenericFile genericFile) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DateTime getFileCreationDate(IGenericFile genericFile) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFileContentType(IGenericFile iGenericFile) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createFile(IGenericFile genericFile, String fileName, String contentType, byte[] content) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteFile(IGenericFile genericFile) {
        // TODO Auto-generated method stub

    }

    @Override
    public byte[] getFileContent(String fileId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getFileSize(String fileId) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getFilename(String fileId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getFileStream(String fileId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DateTime getFileCreationDate(String fileId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getFileContentType(String fileId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String createFile(String fileName, String contentType, byte[] content) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteFile(String fileId) {
        // TODO Auto-generated method stub

    }

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