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
package org.fenixedu.academictreasury.domain.integration.tuitioninfo.exporter;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.integration.ERPTuitionInfoExportOperation;
import org.fenixedu.academictreasury.domain.integration.tuitioninfo.ERPTuitionInfo;
import org.fenixedu.academictreasury.domain.integration.tuitioninfo.ERPTuitionInfoType;
import org.fenixedu.academictreasury.domain.integration.tuitioninfo.IERPTuitionInfoExporter;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.document.ERPCustomerFieldsBean;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.domain.integration.IntegrationOperationLogBean;
import org.fenixedu.treasury.domain.integration.OperationFile;
import org.fenixedu.treasury.generated.sources.saft.sap.AddressStructurePT;
import org.fenixedu.treasury.generated.sources.saft.sap.AuditFile;
import org.fenixedu.treasury.generated.sources.saft.sap.Header;
import org.fenixedu.treasury.generated.sources.saft.sap.OrderReferences;
import org.fenixedu.treasury.generated.sources.saft.sap.PaymentMethod;
import org.fenixedu.treasury.generated.sources.saft.sap.SAFTPTPaymentType;
import org.fenixedu.treasury.generated.sources.saft.sap.SAFTPTSettlementType;
import org.fenixedu.treasury.generated.sources.saft.sap.SAFTPTSourceBilling;
import org.fenixedu.treasury.generated.sources.saft.sap.SAFTPTSourcePayment;
import org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments;
import org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.Payments;
import org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.Payments.Payment;
import org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.Payments.Payment.Line.SourceDocumentID;
import org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.WorkingDocuments.WorkDocument;
import org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.WorkingDocuments.WorkDocument.Line.Metadata;
import org.fenixedu.treasury.generated.sources.saft.sap.Tax;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.IERPExternalService;
import org.fenixedu.treasury.services.integration.erp.SaftConfig;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentStatusWS;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationInput;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationOutput;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class ERPTuitionInfoExporterForSAP implements IERPTuitionInfoExporter {

    private static Logger logger = LoggerFactory.getLogger(SAPExporter.class);

    @Atomic(mode = TxMode.WRITE)
    public ERPTuitionInfoExportOperation export(final ERPTuitionInfo erpTuitionInfo) {
        final FinantialInstitution institution = erpTuitionInfo.getDocumentNumberSeries().getSeries().getFinantialInstitution();

        if (!institution.getErpIntegrationConfiguration().isIntegratedDocumentsExportationEnabled()
                && !erpTuitionInfo.isPendingToExport()) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoExporterForSAP.export.not.pending");
        }

        final IntegrationOperationLogBean logBean = new IntegrationOperationLogBean();

        final ERPTuitionInfoExportOperation operation =
                createSaftExportOperation(erpTuitionInfo, null, institution, new DateTime());
        try {
            logBean.appendIntegrationLog(
                    academicTreasuryBundle("label.ERPTuitionInfoExporterForSAP.starting.finantialdocuments.integration"));

            final String xml = generateERPFile(erpTuitionInfo);

            logBean.appendIntegrationLog(academicTreasuryBundle("label.ERPTuitionInfoExporterForSAP.erp.xml.content.generated"));

            writeContentToExportOperation(xml, operation);

            boolean success = sendDocumentsInformationToIntegration(operation);

            operation.setSuccess(success);
            logBean.appendIntegrationLog(
                    academicTreasuryBundle("label.ERPTuitionInfoExporterForSAP.finished.finantialdocuments.integration"));

        } catch (Exception ex) {
            writeError(operation, logBean, ex);
        } finally {
            operation.appendLog(logBean.getErrorLog(), logBean.getIntegrationLog(), logBean.getSoapInboundMessage(),
                    logBean.getSoapOutboundMessage());
        }

        return operation;
    }

    private String generateERPFile(final ERPTuitionInfo erpTuitionInfo) {
        final FinantialInstitution institution = erpTuitionInfo.getDocumentNumberSeries().getSeries().getFinantialInstitution();

        // Build SAFT-AuditFile
        AuditFile auditFile = new AuditFile();
        // ThreadInformation information = 
        // SaftThreadRegister.retrieveCurrentThreadInformation();

        // Build SAFT-HEADER (Chapter 1 in AuditFile)
        Header header = this.createSAFTHeader(erpTuitionInfo.getBeginDate().toDateTimeAtStartOfDay(),
                erpTuitionInfo.getEndDate().toDateTimeAtStartOfDay(), institution, SAPExporter.ERP_HEADER_VERSION_1_00_00);

        // SetHeader
        auditFile.setHeader(header);

        // Build Master-Files
        org.fenixedu.treasury.generated.sources.saft.sap.AuditFile.MasterFiles masterFiles =
                new org.fenixedu.treasury.generated.sources.saft.sap.AuditFile.MasterFiles();

        // SetMasterFiles
        auditFile.setMasterFiles(masterFiles);

        // Build SAFT-MovementOfGoods (Customer and Products are built inside)
        // ProductsTable (Chapter 2.4 in AuditFile)
        List<org.fenixedu.treasury.generated.sources.saft.sap.Product> productList = masterFiles.getProduct();
        Map<String, org.fenixedu.treasury.generated.sources.saft.sap.Product> productMap =
                new HashMap<String, org.fenixedu.treasury.generated.sources.saft.sap.Product>();
        Set<String> productCodes = new HashSet<String>();

        // ClientsTable (Chapter 2.2 in AuditFile)
        List<org.fenixedu.treasury.generated.sources.saft.sap.Customer> customerList = masterFiles.getCustomer();
        final Map<String, ERPCustomerFieldsBean> customerMap = new HashMap<String, ERPCustomerFieldsBean>();

        // TaxTable (Chapter 2.5 in AuditFile)
        org.fenixedu.treasury.generated.sources.saft.sap.TaxTable taxTable =
                new org.fenixedu.treasury.generated.sources.saft.sap.TaxTable();
        masterFiles.setTaxTable(taxTable);

        for (Vat vat : institution.getVatsSet()) {
            if (vat.isActiveNow()) {
                taxTable.getTaxTableEntry().add(SAPExporter.convertVATtoTaxTableEntry(vat, institution));
            }
        }

        // Set MovementOfGoods in SourceDocuments(AuditFile)
        org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments sourceDocuments =
                new org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments();
        auditFile.setSourceDocuments(sourceDocuments);

        SourceDocuments.SalesInvoices invoices = new SourceDocuments.SalesInvoices();
        SourceDocuments.WorkingDocuments workingDocuments = new SourceDocuments.WorkingDocuments();
        Payments paymentsDocuments = new Payments();

        BigInteger numberOfWorkingDocuments = BigInteger.ZERO;
        BigDecimal totalDebitOfWorkingDocuments = BigDecimal.ZERO;
        BigDecimal totalCreditOfWorkingDocuments = BigDecimal.ZERO;

        invoices.setNumberOfEntries(BigInteger.ZERO);
        invoices.setTotalCredit(BigDecimal.ZERO);
        invoices.setTotalDebit(BigDecimal.ZERO);

        final WorkDocument workDocument = convertToSAFTWorkDocument(erpTuitionInfo, customerMap, productMap);
        workingDocuments.getWorkDocument().add(workDocument);

        if (erpTuitionInfo.isDebit()) {
            totalDebitOfWorkingDocuments = totalDebitOfWorkingDocuments.add(workDocument.getDocumentTotals().getNetTotal());
        } else if (erpTuitionInfo.isCredit()) {
            totalCreditOfWorkingDocuments =
                    totalCreditOfWorkingDocuments.add(workDocument.getDocumentTotals().getNetTotal()).abs();
        }

        // AcumulateValues
        numberOfWorkingDocuments = numberOfWorkingDocuments.add(BigInteger.ONE);

        // Update Totals of Workingdocuments
        workingDocuments.setNumberOfEntries(numberOfWorkingDocuments);
        workingDocuments.setTotalCredit(totalCreditOfWorkingDocuments.setScale(2, RoundingMode.HALF_EVEN));
        workingDocuments.setTotalDebit(totalDebitOfWorkingDocuments.setScale(2, RoundingMode.HALF_EVEN));

        sourceDocuments.setWorkingDocuments(workingDocuments);

        //PROCESSING PAYMENTS TABLE

        paymentsDocuments.setNumberOfEntries(BigInteger.ZERO);
        paymentsDocuments.setTotalCredit(BigDecimal.ZERO);
        paymentsDocuments.setTotalDebit(BigDecimal.ZERO);

        // Update Totals of Payment Documents
        paymentsDocuments.setNumberOfEntries(BigInteger.ZERO);
        sourceDocuments.setPayments(paymentsDocuments);

        // Update the Customer Table in SAFT
        for (final ERPCustomerFieldsBean customerBean : customerMap.values()) {
            final org.fenixedu.treasury.generated.sources.saft.sap.Customer customer =
                    SAPExporter.convertCustomerToSAFTCustomer(customerBean);
            customerList.add(customer);
        }

        // Update the Product Table in SAFT
        for (org.fenixedu.treasury.generated.sources.saft.sap.Product product : productMap.values()) {
            productList.add(product);
        }

        String xml = SAPExporter.exportAuditFileToXML(auditFile);

        return xml;
    }

    private Header createSAFTHeader(final DateTime startDate, final DateTime endDate,
            final FinantialInstitution finantialInstitution, final String auditVersion) {

        Header header = new Header();
        DatatypeFactory dataTypeFactory;
        try {

            dataTypeFactory = DatatypeFactory.newInstance();

            // AuditFileVersion
            header.setAuditFileVersion(auditVersion);
            header.setIdProcesso(finantialInstitution.getErpIntegrationConfiguration().getErpIdProcess());

            // BusinessName - Nome da Empresa
            header.setBusinessName(finantialInstitution.getCompanyName());
            header.setCompanyName(finantialInstitution.getName());

            // CompanyAddress
            AddressStructurePT companyAddress = null;
            //TODOJN Locale por resolver
            companyAddress = SAPExporter.convertFinantialInstitutionAddressToAddressPT(finantialInstitution.getAddress(),
                    finantialInstitution.getZipCode(), finantialInstitution.getMunicipality() != null ? finantialInstitution
                            .getMunicipality().getLocalizedName(new Locale("pt")) : "---",
                    finantialInstitution.getAddress());
            header.setCompanyAddress(companyAddress);

            // CompanyID
            /*
             * Obtem -se pela concatena??o da conservat?ria do registo comercial
             * com o n?mero do registo comercial, separados pelo car?cter
             * espa?o. Nos casos em que n?o existe o registo comercial, deve ser
             * indicado o NIF.
             */
            header.setCompanyID(finantialInstitution.getFiscalNumber());

            // CurrencyCode
            /*
             * 1.11 * C?digo de moeda (CurrencyCode) . . . . . . . Preencher com
             * ?EUR?
             */
            header.setCurrencyCode(finantialInstitution.getCurrency().getCode());

            // DateCreated
            DateTime now = new DateTime();

            /* ANIL: 2015/10/20 converted from dateTime to Date */
            header.setDateCreated(SAPExporter.convertToXMLDate(dataTypeFactory, now));

            // Email
            // header.setEmail(StringUtils.EMPTY);

            // EndDate

            /* ANIL: 2015/10/20 converted from dateTime to Date */
            header.setEndDate(SAPExporter.convertToXMLDate(dataTypeFactory, endDate));

            // Fax
            // header.setFax(StringUtils.EMPTY);

            // FiscalYear
            /*
             * Utilizar as regras do c?digo do IRC, no caso de per?odos
             * contabil?sticos n?o coincidentes com o ano civil. (Ex: per?odo de
             * tributa??o de 01 -10 -2008 a 30 -09 -2009 corresponde FiscalYear
             * 2008). Inteiro 4
             */
            header.setFiscalYear(endDate.getYear());

            // Ir obter a data do ?ltimo
            // documento(por causa de submeter em janeiro, documentos de
            // dezembro)

            // HeaderComment
            // header.setHeaderComment(org.apache.commons.lang.StringUtils.EMPTY);

            // ProductCompanyTaxID
            // Preencher com o NIF da entidade produtora do software
            header.setProductCompanyTaxID(SaftConfig.PRODUCT_COMPANY_TAX_ID());

            // ProductID
            /*
             * 1.16 * Nome do produto (ProductID). . . . . . . . . . . Nome do
             * produto que gera o SAF -T (PT) . . . . . . . . . . . Deve ser
             * indicado o nome comercial do software e o da empresa produtora no
             * formato ?Nome produto/nome empresa?.
             */
            header.setProductID(SaftConfig.PRODUCT_ID());

            // Product Version
            header.setProductVersion(SaftConfig.PRODUCT_VERSION());

            // SoftwareCertificateNumber
            /* Changed to 0 instead of -1 decribed in SaftConfig.SOFTWARE_CERTIFICATE_NUMBER() */
            header.setSoftwareCertificateNumber(BigInteger.valueOf(0));

            // StartDate
            header.setStartDate(SAPExporter.convertToXMLDate(dataTypeFactory, startDate));

            // TaxAccountingBasis
            /*
             * Deve ser preenchido com: contabilidade; facturao; ?I? ? dados
             * integrados de factura??o e contabilidade; ?S? ? autofactura??o;
             * ?P? ? dados parciais de factura??o
             */
            header.setTaxAccountingBasis("P");

            // TaxEntity
            /*
             * Identifica??o do estabelecimento (TaxEntity) No caso do ficheiro
             * de factura??o dever? ser especificado a que estabelecimento diz
             * respeito o ficheiro produzido, se aplic?vel, caso contr?rio,
             * dever? ser preenchido com a especifica??o ?Global?. No caso do
             * ficheiro de contabilidade ou integrado, este campo dever? ser
             * preenchido com a especifica??o ?Sede?. Texto 20
             */
            header.setTaxEntity("Global");

            // TaxRegistrationNumber
            /*
             * N?mero de identifica??o fiscal da empresa
             * (TaxRegistrationNumber). Preencher com o NIF portugu?s sem
             * espa?os e sem qualquer prefixo do pa?s. Inteiro 9
             */
            try {
                header.setTaxRegistrationNumber(Integer.parseInt(finantialInstitution.getFiscalNumber()));
            } catch (Exception ex) {
                throw new RuntimeException("Invalid Fiscal Number.");
            }

            // header.setTelephone(finantialInstitution.get);

            // header.setWebsite(finantialInstitution.getEmailContact());

            return header;
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
            return null;
        }
    }

    private WorkDocument convertToSAFTWorkDocument(final ERPTuitionInfo erpTuitionInfo,
            final Map<String, ERPCustomerFieldsBean> baseCustomers,
            final Map<String, org.fenixedu.treasury.generated.sources.saft.sap.Product> baseProducts) {
        final ERPCustomerFieldsBean customerBean = ERPCustomerFieldsBean.fillFromCustomer(erpTuitionInfo.getCustomer());

        WorkDocument workDocument = new WorkDocument();

        // Find the Customer in BaseCustomers
        if (baseCustomers.containsKey(customerBean.getCustomerId())) {
            ERPCustomerFieldsBean customer = baseCustomers.get(customerBean.getCustomerId());

            if (!customer.getCustomerFiscalNumber().equals(customerBean.getCustomerFiscalNumber())) {
                throw new TreasuryDomainException("error.SAPExporter.customer.registered.with.different.fiscalNumber");
            }
        } else {
            // If not found, create a new one and add it to baseCustomers
            baseCustomers.put(customerBean.getCustomerId(), customerBean);
        }

        // MovementDate
        DatatypeFactory dataTypeFactory;
        try {
            dataTypeFactory = DatatypeFactory.newInstance();
            final DateTime documentDate = erpTuitionInfo.getCreationDate();

            /* Anil: 14/06/2016: Fill with 0's the Hash element */
            workDocument.setDueDate(SAPExporter.convertToXMLDate(dataTypeFactory, documentDate));

            /* Anil: 14/06/2016: Fill with 0's the Hash element */
            workDocument.setHash(Strings.repeat("0", 172));

            // SystemEntryDate
            workDocument.setSystemEntryDate(SAPExporter.convertToXMLDateTime(dataTypeFactory, documentDate));

            /* ANIL: 2015/10/20 converted from dateTime to Date */
            workDocument.setWorkDate(SAPExporter.convertToXMLDate(dataTypeFactory, documentDate));
            workDocument.setCertificationDate(SAPExporter.convertToXMLDate(dataTypeFactory, erpTuitionInfo.getCreationDate()));

            // DocumentNumber
            workDocument.setDocumentNumber(erpTuitionInfo.getUiDocumentNumber());

            // CustomerID
            workDocument.setCustomerID(erpTuitionInfo.getCustomer().getCode());

            // DocumentStatus
            /*
             * Deve ser preenchido com: ?N? ? Normal; Texto 1 ?T? ? Por conta de
             * terceiros; ?A? ? Documento anulado.
             */
            SourceDocuments.WorkingDocuments.WorkDocument.DocumentStatus status =
                    new SourceDocuments.WorkingDocuments.WorkDocument.DocumentStatus();
            status.setWorkStatus("N");

            status.setWorkStatusDate(SAPExporter.convertToXMLDateTime(dataTypeFactory, documentDate));
            // Utilizador responsável pelo estado atual do docu-mento.
            status.setSourceID(TreasuryPlataformDependentServicesFactory.implementation().versioningCreatorUsername(erpTuitionInfo));

            status.setSourceBilling(SAFTPTSourceBilling.P);

            workDocument.setDocumentStatus(status);

            // DocumentTotals
            SourceDocuments.WorkingDocuments.WorkDocument.DocumentTotals docTotals =
                    new SourceDocuments.WorkingDocuments.WorkDocument.DocumentTotals();
            docTotals.setGrossTotal(erpTuitionInfo.getTuitionDeltaAmount().setScale(2, RoundingMode.HALF_EVEN).abs());
            docTotals.setNetTotal(erpTuitionInfo.getTuitionDeltaAmount().setScale(2, RoundingMode.HALF_EVEN).abs());
            docTotals.setTaxPayable(erpTuitionInfo.getTuitionDeltaAmount().setScale(2, RoundingMode.HALF_EVEN).abs());
            workDocument.setDocumentTotals(docTotals);

            // WorkType
            /*
             * Deve ser preenchido com: Texto 2 "DC" — Documentos emitidos que
             * sejam suscetiveis de apresentacao ao cliente para conferencia de
             * entrega de mercadorias ou da prestacao de servicos. "FC" — Fatura
             * de consignacao nos termos do artigo 38º do codigo do IVA.
             */
            workDocument.setWorkType("DC");

            // Period
            /*
             * Per?odo contabil?stico (Period) . . . . . . . . . . Deve ser
             * indicado o n?mero do m?s do per?odo de tributa??o, de ?1? a ?12?,
             * contado desde a data do in?cio. Pode ainda ser preenchido com
             * ?13?, ?14?, ?15? ou ?16? para movimentos efectuados no ?ltimo m?s
             * do per?odo de tributa??o, relacionados com o apuramento do
             * resultado. Ex.: movimentos de apuramentos de invent?rios,
             * deprecia??es, ajustamentos ou apuramentos de resultados.
             */
            workDocument.setPeriod(documentDate.getMonthOfYear());

            // SourceID
            String creator = TreasuryPlataformDependentServicesFactory.implementation().versioningCreatorUsername(erpTuitionInfo);
            workDocument.setSourceID(
                    !Strings.isNullOrEmpty(creator) ? creator : "");

        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        List<org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.WorkingDocuments.WorkDocument.Line> productLines =
                workDocument.getLine();

        // Process individual
        org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.WorkingDocuments.WorkDocument.Line line =
                convertToSAFTWorkDocumentLine(erpTuitionInfo, baseProducts);
        line.setLineNumber(BigInteger.ONE);
        productLines.add(line);

        return workDocument;
    }

    private org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.WorkingDocuments.WorkDocument.Line convertToSAFTWorkDocumentLine(
            final ERPTuitionInfo erpTuitionInfo,
            Map<String, org.fenixedu.treasury.generated.sources.saft.sap.Product> baseProducts) {

        final org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.WorkingDocuments.WorkDocument.Line line =
                new org.fenixedu.treasury.generated.sources.saft.sap.SourceDocuments.WorkingDocuments.WorkDocument.Line();
        final DateTime documentDate = erpTuitionInfo.getCreationDate();

        try {
            final DatatypeFactory dataTypeFactory = DatatypeFactory.newInstance();

            org.fenixedu.treasury.generated.sources.saft.sap.Product currentProduct = null;

            final ERPTuitionInfoType tuitionInfoType = erpTuitionInfo.getErpTuitionInfoType();

            if (tuitionInfoType.getErpTuitionInfoProduct().getCode() != null && baseProducts.containsKey(tuitionInfoType.getErpTuitionInfoProduct().getCode())) {
                currentProduct = baseProducts.get(tuitionInfoType.getErpTuitionInfoProduct().getCode());
            } else {
                currentProduct = convertERPTuitionInfoTypeToSAFTProduct(tuitionInfoType);
                baseProducts.put(currentProduct.getProductCode(), currentProduct);
            }
            XMLGregorianCalendar documentDateCalendar = null;
            documentDateCalendar = SAPExporter.convertToXMLDate(dataTypeFactory, documentDate);

            if (erpTuitionInfo.isCredit()) {
                line.setCreditAmount(erpTuitionInfo.getTuitionDeltaAmount().setScale(2, RoundingMode.HALF_EVEN).abs());
            } else if (erpTuitionInfo.isDebit()) {
                line.setDebitAmount(erpTuitionInfo.getTuitionDeltaAmount().setScale(2, RoundingMode.HALF_EVEN));
            }

            // Description
            line.setDescription(tuitionInfoType.getErpTuitionInfoProduct().getName());
            List<OrderReferences> orderReferences = line.getOrderReferences();

            line.setMetadata(fillMetadata(erpTuitionInfo, dataTypeFactory));

            if (erpTuitionInfo.getFirstERPTuitionInfo() != null) {
                final ERPTuitionInfo firstERPTuitionInfo = erpTuitionInfo.getFirstERPTuitionInfo();

                OrderReferences reference = new OrderReferences();
                reference.setOriginatingON(firstERPTuitionInfo.getUiDocumentNumber());
                reference.setOrderDate(SAPExporter.convertToXMLDate(dataTypeFactory,
                        erpTuitionInfo.getFirstERPTuitionInfo().getCreationDate()));
                reference.setLineNumber(BigInteger.ONE);

                orderReferences.add(reference);
            }

            // ProductCode
            line.setProductCode(currentProduct.getProductCode());

            // ProductDescription
            line.setProductDescription(currentProduct.getProductDescription());

            // Quantity
            line.setQuantity(BigDecimal.ONE);

            // SettlementAmount
            line.setSettlementAmount(BigDecimal.ZERO);

            // Tax
            line.setTax(getSAFTWorkingDocumentsTax(erpTuitionInfo));

            line.setTaxPointDate(documentDateCalendar);

            line.setTaxExemptionReason("M07-Isento Artigo 9.º do CIVA (Ou similar)");

            // UnitOfMeasure
            line.setUnitOfMeasure("Unidade");

            // UnitPrice
            line.setUnitPrice(erpTuitionInfo.getTuitionDeltaAmount().setScale(2, RoundingMode.HALF_EVEN).abs());

        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        return line;
    }

    private org.fenixedu.treasury.generated.sources.saft.sap.Product convertERPTuitionInfoTypeToSAFTProduct(
            ERPTuitionInfoType tuitionInfoType) {
        org.fenixedu.treasury.generated.sources.saft.sap.Product p =
                new org.fenixedu.treasury.generated.sources.saft.sap.Product();

        // ProductCode
        p.setProductCode(tuitionInfoType.getErpTuitionInfoProduct().getCode());

        // ProductDescription
        p.setProductDescription(tuitionInfoType.getErpTuitionInfoProduct().getName());

        // ProductGroup
        p.setProductGroup(AcademicTreasurySettings.getInstance().getTuitionProductGroup().getName().getContent());

        // ProductNumberCode
        p.setProductNumberCode(p.getProductCode());

        // ProductType
        /*
         * Deve ser preenchido com: ?P? ? produtos; ?S? ? servi?os; ?O? ? outros
         * (ex: portes debitados); ?I? ? impostos, taxas e encargos parafiscais
         * (excepto IVA e IS que dever?o ser reflectidos na tabela de impostos ?
         * TaxTable). Texto 1
         */
        p.setProductType("S");

        return p;
    }

    private Metadata fillMetadata(final ERPTuitionInfo erpTuitionInfo, final DatatypeFactory dataTypeFactory) {
        final Map<String, String> metadataPropertiesMap = Maps.newHashMap();

        metadataPropertiesMap.put("TOTAL_TUITION_AMOUNT", erpTuitionInfo.getTuitionTotalAmount().toString());

        if (erpTuitionInfo.getLastSuccessfulSentERPTuitionInfo() != null) {
            metadataPropertiesMap.put("LAST_SUCCESSFUL_EXPORTATION",
                    erpTuitionInfo.getLastSuccessfulSentERPTuitionInfo().getUiDocumentNumber());
        } else {
            metadataPropertiesMap.put("LAST_SUCCESSFUL_EXPORTATION", "");
        }

        metadataPropertiesMap.put("START_DATE", erpTuitionInfo.getBeginDate()
                .toString(org.fenixedu.academictreasury.util.AcademicTreasuryConstants.STANDARD_DATE_FORMAT_YYYY_MM_DD));
        metadataPropertiesMap.put("END_DATE", erpTuitionInfo.getEndDate()
                .toString(org.fenixedu.academictreasury.util.AcademicTreasuryConstants.STANDARD_DATE_FORMAT_YYYY_MM_DD));

        final GsonBuilder builder = new GsonBuilder();

        final Gson gson = builder.create();
        final Type stringStringMapType = new TypeToken<Map<String, String>>() {
        }.getType();

        final String json = gson.toJson(metadataPropertiesMap, stringStringMapType);

        final Metadata metadata = new Metadata();
        metadata.setDescription(json);

        return metadata;
    }

    private Tax getSAFTWorkingDocumentsTax(final ERPTuitionInfo erpTuitionInfo) {
        Tax tax = new Tax();

        // VatType vat = product.getVatType();
        // Tax-TaxCode
        tax.setTaxCode("ISE");

        tax.setTaxCountryRegion("PT");

        // Tax-TaxPercentage
        tax.setTaxPercentage(BigDecimal.ZERO);

        // Tax-TaxType
        tax.setTaxType("IVA");

        // TODO: Fill with vat amount
        //tax.setTaxAmount(entry.getVatAmount());

        return tax;
    }

    // SERVICE
    @Atomic(mode = TxMode.WRITE)
    private ERPTuitionInfoExportOperation createSaftExportOperation(final ERPTuitionInfo erpTuitionInfo, byte[] data,
            final FinantialInstitution institution, final DateTime when) {
        String filename = institution.getFiscalNumber() + "_" + when.toString() + ".xml";
        ERPTuitionInfoExportOperation operation =
                ERPTuitionInfoExportOperation.create(erpTuitionInfo, data, filename, institution, null, when);

        return operation;
    }

    private boolean sendDocumentsInformationToIntegration(final ERPTuitionInfoExportOperation operation)
            throws MalformedURLException {
        final FinantialInstitution institution = operation.getFinantialInstitution();
        final IntegrationOperationLogBean logBean = new IntegrationOperationLogBean();

        try {

            boolean success = true;
            ERPConfiguration erpIntegrationConfiguration = institution.getErpIntegrationConfiguration();
            if (erpIntegrationConfiguration == null) {
                throw new TreasuryDomainException("error.ERPExporter.invalid.erp.configuration");
            }

            if (erpIntegrationConfiguration.getActive() == false) {
                logBean.appendErrorLog(treasuryBundle("info.ERPExporter.configuration.inactive"));
                return false;
            }

            logBean.appendIntegrationLog(treasuryBundle("info.ERPExporter.sending.inforation"));

            final IERPExternalService service = erpIntegrationConfiguration.getERPExternalServiceImplementation();
            final DocumentsInformationInput input = new DocumentsInformationInput();
            if (operation.getFile().getSize() <= erpIntegrationConfiguration.getMaxSizeBytesToExportOnline()) {
                input.setData(operation.getFile().getContent());
                DocumentsInformationOutput sendInfoOnlineResult = service.sendInfoOnline(institution, input);

                operation.setErpOperationId(sendInfoOnlineResult.getRequestId());
                logBean.appendIntegrationLog(treasuryBundle("info.ERPExporter.sucess.sending.inforation.online",
                        sendInfoOnlineResult.getRequestId()));

                //if we have result in online situation, then check the information of integration STATUS
                for (DocumentStatusWS status : sendInfoOnlineResult.getDocumentStatus()) {
                    if (status.isIntegratedWithSuccess()) {

                        ERPTuitionInfo tuitionInfo = 
                                ERPTuitionInfo.findUniqueByDocumentNumber(status.getDocumentNumber()).orElse(null);
                        
                        if (tuitionInfo != null) {
                            final String message = treasuryBundle("info.ERPExporter.sucess.integrating.document", tuitionInfo.getUiDocumentNumber());
                            logBean.appendIntegrationLog(message);
                            tuitionInfo.markIntegratedWithSuccess(message);
                        } else {
                            success = false;
                            logBean.appendIntegrationLog(treasuryBundle("info.ERPExporter.error.integrating.document",
                                    status.getDocumentNumber(), status.getErrorDescription()));
                            logBean.appendErrorLog(treasuryBundle("info.ERPExporter.error.integrating.document",
                                    status.getDocumentNumber(), status.getErrorDescription()));
                        }
                    } else {
                        success = false;
                        logBean.appendIntegrationLog(treasuryBundle("info.ERPExporter.error.integrating.document",
                                status.getDocumentNumber(), status.getErrorDescription()));
                        logBean.appendErrorLog(treasuryBundle("info.ERPExporter.error.integrating.document",
                                status.getDocumentNumber(), status.getErrorDescription()));

                    }
                }

                for (final String m : sendInfoOnlineResult.getOtherMessages()) {
                    logBean.appendIntegrationLog(m);
                }

                logBean.defineSoapInboundMessage(sendInfoOnlineResult.getSoapInboundMessage());
                logBean.defineSoapOutboundMessage(sendInfoOnlineResult.getSoapOutboundMessage());

            } else {
                throw new TreasuryDomainException(
                        "error.ERPExporter.sendDocumentsInformationToIntegration.maxSizeBytesToExportOnline.exceeded");
            }

            return success;
        } finally {
            operation.appendLog(logBean.getErrorLog(), logBean.getIntegrationLog(), logBean.getSoapInboundMessage(),
                    logBean.getSoapOutboundMessage());
        }
    }

    private void writeError(final ERPTuitionInfoExportOperation operation, final IntegrationOperationLogBean logBean,
            final Throwable t) {
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        t.printStackTrace(writer);

        logBean.appendErrorLog(out.toString());

        operation.setProcessed(true);
    }

    // SERVICE
    @Atomic
    private void writeContentToExportOperation(final String content, final ERPTuitionInfoExportOperation operation) {
        byte[] bytes = null;
        try {
            bytes = content.getBytes(SAPExporter.SAFT_PT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String fileName = operation.getFinantialInstitution().getFiscalNumber() + "_"
                + operation.getExecutionDate().toString("ddMMyyyy_hhmm") + ".xml";
        OperationFile binaryStream = new OperationFile(fileName, bytes);
        if (operation.getFile() != null) {
            operation.getFile().delete();
        }

        operation.setFile(binaryStream);
    }

    private Payment convertToSAFTPaymentDocument(final ERPTuitionInfo erpTuitionInfo) {

        final Payment payment = new Payment();

        // MovementDate
        DatatypeFactory dataTypeFactory;
        try {
            dataTypeFactory = DatatypeFactory.newInstance();
            final DateTime documentDate = erpTuitionInfo.getCreationDate();

            // SystemEntryDate
            payment.setSystemEntryDate(SAPExporter.convertToXMLDateTime(dataTypeFactory, documentDate));

            /* ANIL: 2015/10/20 converted from dateTime to Date */
            payment.setTransactionDate(SAPExporter.convertToXMLDate(dataTypeFactory, documentDate));

            /* SAP: 2016/09/19 This element is required */
            payment.setPaymentType(SAFTPTPaymentType.RG);

            // DocumentNumber
            payment.setPaymentRefNo(erpTuitionInfo.getUiSettlementDocumentNumberForERP());

            // Finantial Transaction Reference
            payment.setFinantialTransactionReference("");

            //OriginDocumentNumber
            payment.setSourceID(" ");

            // CustomerID
            payment.setCustomerID(erpTuitionInfo.getCustomer().getCode());

            // DocumentStatus
            /*
             * Deve ser preenchido com: ?N? ? Normal; Texto 1 ?T? ? Por conta de
             * terceiros; ?A? ? Documento anulado.
             */
            SourceDocuments.Payments.Payment.DocumentStatus status = new SourceDocuments.Payments.Payment.DocumentStatus();
            status.setPaymentStatus("N");

            status.setPaymentStatusDate(SAPExporter.convertToXMLDate(dataTypeFactory, erpTuitionInfo.getCreationDate()));

            // Utilizador responsável pelo estado atual do docu-mento.
            String creator = TreasuryPlataformDependentServicesFactory.implementation().versioningCreatorUsername(erpTuitionInfo);
            status.setSourceID(
                    !Strings.isNullOrEmpty(creator) ? creator : " ");
            status.setReason("");

            // Deve ser preenchido com:
            // 'P' - Documento produzido na aplicacao;
            status.setSourcePayment(SAFTPTSourcePayment.P);

            payment.setDocumentStatus(status);

            final PaymentMethod voidMethod = new PaymentMethod();
            voidMethod.setPaymentAmount(BigDecimal.ZERO);

            /* ANIL: 2015/10/20 converted from dateTime to Date */
            voidMethod.setPaymentDate(SAPExporter.convertToXMLDate(dataTypeFactory, erpTuitionInfo.getCreationDate()));

            voidMethod.setPaymentMechanism("OU");
            voidMethod.setPaymentMethodReference("");

            payment.getPaymentMethod().add(voidMethod);
            payment.setSettlementType(SAFTPTSettlementType.NN);

            // DocumentTotals
            SourceDocuments.Payments.Payment.DocumentTotals docTotals = new SourceDocuments.Payments.Payment.DocumentTotals();

            //Lines
            {
                final SourceDocuments.Payments.Payment.Line line = new SourceDocuments.Payments.Payment.Line();
                line.setLineNumber(BigInteger.ONE);
                //SourceDocument

                final SourceDocumentID sourceDocument = new SourceDocumentID();
                sourceDocument.setLineNumber(BigInteger.ONE);
                sourceDocument.setOriginatingON(erpTuitionInfo.getFirstERPTuitionInfo().getUiDocumentNumber());

                /* ANIL: 2015/10/20 converted from dateTime to Date */
                sourceDocument.setInvoiceDate(SAPExporter.convertToXMLDate(dataTypeFactory, erpTuitionInfo.getCreationDate()));

                sourceDocument.setDescription(erpTuitionInfo.getErpTuitionInfoType().getErpTuitionInfoProduct().getName());
                line.getSourceDocumentID().add(sourceDocument);

                //SettlementAmount
                line.setSettlementAmount(BigDecimal.ZERO);
                line.setDebitAmount(erpTuitionInfo.getTuitionDeltaAmount().abs());
                payment.getLine().add(line);
            }

            {
                final SourceDocuments.Payments.Payment.Line line = new SourceDocuments.Payments.Payment.Line();
                line.setLineNumber(new BigInteger("2"));
                //SourceDocument

                final SourceDocumentID sourceDocument = new SourceDocumentID();
                sourceDocument.setLineNumber(BigInteger.ONE);
                sourceDocument.setOriginatingON(erpTuitionInfo.getUiDocumentNumber());

                /* ANIL: 2015/10/20 converted from dateTime to Date */
                sourceDocument.setInvoiceDate(SAPExporter.convertToXMLDate(dataTypeFactory, erpTuitionInfo.getCreationDate()));

                sourceDocument.setDescription(erpTuitionInfo.getErpTuitionInfoType().getErpTuitionInfoProduct().getName());
                line.getSourceDocumentID().add(sourceDocument);

                //SettlementAmount
                line.setSettlementAmount(BigDecimal.ZERO);
                line.setCreditAmount(erpTuitionInfo.getTuitionDeltaAmount().abs());
                payment.getLine().add(line);
            }

            docTotals.setGrossTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
            docTotals.setNetTotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
            docTotals.setTaxPayable(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
            payment.setDocumentTotals(docTotals);

            payment.setPeriod(erpTuitionInfo.getCreationDate().getMonthOfYear());

        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }

        return payment;
    }
}
