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
    public void scheduleDocumentForExportation(FinantialDocument finantialDocument) {
        // TODO Auto-generated method stub

    }

    @Override
    public IERPExternalService getERPExternalServiceImplementation(ERPConfiguration erpConfiguration) {
        // TODO Auto-generated method stub
        return null;
    }

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
    public String getLoggedUsername() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCustomerEmail(Customer customer) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setCurrentApplicationUser(String username) {
        // Do nothing
    }

    @Override
    public void removeCurrentApplicationUser() {
        // Do nothing
    }

    @Override
    public Locale defaultLocale() {
        return TreasuryConstants.DEFAULT_LANGUAGE;
    }

    @Override
    public Locale currentLocale() {
        return TreasuryConstants.DEFAULT_LANGUAGE;
    }

    @Override
    public Set<Locale> availableLocales() {
        return Set.of(TreasuryConstants.DEFAULT_LANGUAGE);
    }

    @Override
    public String bundle(String bundleName, String key, String... args) {
        return key;
    }

    @Override
    public String bundle(Locale locale, String bundleName, String key, String... args) {
        return key;
    }

    @Override
    public LocalizedString bundleI18N(String bundleName, String key, String... args) {
        ResourceBundle bundlePt = ResourceBundle.getBundle(bundleName, Locale.getDefault());
        if (bundlePt.containsKey(key)) {
            final Locale enLocale = new Locale("en", "GB");
            ResourceBundle bundleEn = ResourceBundle.getBundle(bundleName, Locale.getDefault());
            return new LocalizedString(Locale.getDefault(), bundlePt.getString(key)).with(enLocale, bundleEn.getString(key));
        } else {
            return BasicAcademicTreasuryUtils.ls(key);
        }
    }

    @Override
    public LocalizedString getDynamicApplicationMessage(String key) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public <T> String versioningCreatorUsername(T obj) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> DateTime versioningCreationDate(T obj) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> String versioningUpdatorUsername(T obj) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <T> DateTime versioningUpdateDate(T obj) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PaylineWebServiceResponse paylineGetWebPaymentDetails(ForwardPaymentRequest forwardPaymentRequest) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PaylineWebServiceResponse paylineDoWebPayment(ForwardPaymentRequest forwardPaymentRequest, String returnUrl,
            String cancelUrl) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void paylineConfigureWebservice(PaylineConfiguration paylineConfiguration) {
        // TODO Auto-generated method stub

    }

    @Override
    public String calculateURLChecksum(String urlToChecksum, HttpSession session) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void signalsRegisterHandlerForKey(String signalKey, Object handler) {
        // TODO Auto-generated method stub

    }

    @Override
    public void signalsUnregisterHandlerForKey(String signalKey, Object handler) {
        // TODO Auto-generated method stub

    }

    @Override
    public void signalsEmitForObject(String signalKey, DomainObject obj) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getForwardPaymentURL(String contextPath, Class screenClass, boolean isSuccess, String forwardPaymentId,
            boolean isException) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream exportDocuments(String templateCode, FinantialInstitution finantialInstitution,
            FinantialEntity finantialEntity, LocalDate documentDateFrom, LocalDate documentDateTo, String username) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String exportDocumentFileExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream exportPaymentReceipt(String templateCode, SettlementNote settlementNote) {
        // TODO Auto-generated method stub
        return null;
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
    public void updateCertifiedDocument(FinantialDocument finantialDocument) {
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

    /* Development or quality mode */

    @Override
    public boolean isQualityOrDevelopmentMode() {
        return true;
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