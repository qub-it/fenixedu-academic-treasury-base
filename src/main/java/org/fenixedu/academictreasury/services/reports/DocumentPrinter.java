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
package org.fenixedu.academictreasury.services.reports;

import java.io.InputStream;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.services.reports.dataproviders.CustomerDataProvider;
import org.fenixedu.treasury.services.reports.dataproviders.DebtAccountDataProvider;
import org.fenixedu.treasury.services.reports.dataproviders.FinantialInstitutionDataProvider;
import org.fenixedu.treasury.services.reports.helpers.DateHelper;
import org.fenixedu.treasury.services.reports.helpers.EnumerationHelper;
import org.fenixedu.treasury.services.reports.helpers.LanguageHelper;
import org.fenixedu.treasury.services.reports.helpers.MoneyHelper;
import org.fenixedu.treasury.services.reports.helpers.NumbersHelper;
import org.fenixedu.treasury.services.reports.helpers.StringsHelper;

import com.google.common.base.Strings;
import com.qubit.terra.docs.core.DocumentGenerator;
import com.qubit.terra.docs.core.DocumentTemplateEngine;
import com.qubit.terra.docs.core.IDocumentTemplateService;

public class DocumentPrinter {
    private static final String TEMPLATES_TUITIONS_PAYMENT_PLAN = "templates/tuitionsPaymentPlan.odt";

    static {
        registerService();
    }

    public static synchronized void registerService() {
        IDocumentTemplateService service = new DocumentPrinterConfiguration();
        DocumentTemplateEngine.registerServiceImplementations(service);
    }

    public static final String PDF = DocumentGenerator.PDF;
    public static final String ODT = DocumentGenerator.ODT;

    private static void registerHelpers(DocumentGenerator generator) {
        generator.registerHelper("dates", new DateHelper());
        generator.registerHelper("lang", new LanguageHelper());
        generator.registerHelper("numbers", new NumbersHelper());
        generator.registerHelper("enumeration", new EnumerationHelper());
        generator.registerHelper("strings", new StringsHelper());
        generator.registerHelper("money", new MoneyHelper());
    }

    //https://github.com/qub-it/fenixedu-qubdocs-reports/blob/master/src/main/java/org/fenixedu/academic/util/report/DocumentPrinter.java
    public static byte[] printRegistrationTuititionPaymentPlan(Registration registration, String outputMimeType) {
        final Person p = registration.getStudent().getPerson();
        final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(p);
        final String fiscalNumber = PersonCustomer.fiscalNumber(p);
        if (Strings.isNullOrEmpty(addressFiscalCountryCode) || Strings.isNullOrEmpty(fiscalNumber)) {
            throw new AcademicTreasuryDomainException("error.PersonCustomer.fiscalInformation.required");
        }

        final PersonCustomer customer = PersonCustomer.findUnique(p, addressFiscalCountryCode, fiscalNumber).orElse(null);
        final FinantialInstitution finst =
                registration.getDegree().getAdministrativeOffice().getFinantialEntity().getFinantialInstitution();
        final DebtAccount account = DebtAccount.findUnique(finst, customer).orElse(null);

        return printRegistrationTuititionPaymentPlan(account, outputMimeType);
    }

    public static byte[] printRegistrationTuititionPaymentPlan(final DebtAccount debtAccount, String outputMimeType) {

        DocumentGenerator generator = null;

        //TODO refactor: there should be an application runtime configuration to enable those templates
        //Gets file templates/tuitionsPaymentPlan-NIF.odt
        InputStream resourceAsStream =
                DocumentGenerator.class.getClassLoader().getResourceAsStream(TEMPLATES_TUITIONS_PAYMENT_PLAN);

        generator = DocumentGenerator.create(resourceAsStream, outputMimeType);
//          throw new TreasuryDomainException("error.ReportExecutor.document.template.not.available");
//      }

        registerHelpers(generator);
        generator.registerDataProvider(new DebtAccountDataProvider(debtAccount, null));
        generator.registerDataProvider(new CustomerDataProvider(debtAccount.getCustomer(), "customer"));
        generator.registerDataProvider(new FinantialInstitutionDataProvider(debtAccount.getFinantialInstitution()));

        //... add more providers...

        byte[] outputReport = generator.generateReport();

        return outputReport;
    }

}
