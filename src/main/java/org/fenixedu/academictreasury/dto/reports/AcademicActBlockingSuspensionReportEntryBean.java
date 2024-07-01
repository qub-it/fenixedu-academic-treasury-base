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
package org.fenixedu.academictreasury.dto.reports;

import static com.qubit.terra.framework.tools.excel.ExcelUtil.createCellWithValue;
import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academictreasury.domain.academicalAct.AcademicActBlockingSuspension;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.reports.ErrorsLog;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class AcademicActBlockingSuspensionReportEntryBean extends AbstractReportEntryBean {

    public static String[] SPREADSHEET_HEADERS =
            { academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.identification"),
                    academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.versioningCreator"),
                    academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.creationDate"),
                    academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.name"),
                    academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.identificationType"),
                    academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.identificationNumber"),
                    academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.vatNumber"),
                    academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.email"),
                    academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.address"),
                    academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.addressCountryCode"),
                    academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.studentNumber"),
                    academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.beginDate"),
                    academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.endDate"),
                    academicTreasuryBundle("label.AcademicActBlockingSuspensionReportEntryBean.header.reason") };

    private String identification;
    private String versioningCreator;
    private DateTime creationDate;
    private String name;
    private String identificationType;
    private String identificationNumber;
    private String vatNumber;
    private String email;
    private String address;
    private String addressCountryCode;
    private Integer studentNumber;
    private LocalDate beginDate;
    private LocalDate endDate;
    private String reason;

    private AcademicActBlockingSuspension academicActBlockingSuspension;

    boolean completed = false;

    public AcademicActBlockingSuspensionReportEntryBean(final AcademicActBlockingSuspension academicActBlockingSuspension,
            final ErrorsLog errorsLog) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();
        
        this.academicActBlockingSuspension = academicActBlockingSuspension;

        try {
            this.identification = academicActBlockingSuspension.getExternalId();
            this.versioningCreator = treasuryServices.versioningCreatorUsername(academicActBlockingSuspension);
            this.creationDate = treasuryServices.versioningCreationDate(academicActBlockingSuspension);

            final Person person = academicActBlockingSuspension.getPerson();

            this.name = person.getName();

            if (academicActBlockingSuspension.getPerson().getIdDocumentType() != null) {
                this.identificationType = academicActBlockingSuspension.getPerson().getIdDocumentType().getLocalizedName();
            }

            this.identificationNumber = PersonCustomer.identificationNumber(person);
            this.vatNumber = PersonCustomer.uiPersonFiscalNumber(person);
            this.email = academicActBlockingSuspension.getPerson().getInstitutionalOrDefaultEmailAddressValue();
            this.address =
                    PersonCustomer.physicalAddress(person) != null ? PersonCustomer.physicalAddress(person).getAddress() : "";
            this.addressCountryCode = PersonCustomer.physicalAddress(person) != null
                    && PersonCustomer.physicalAddress(person).getCountryOfResidence() != null ? PersonCustomer
                            .physicalAddress(person).getCountryOfResidence().getCode() : "";

            if (academicActBlockingSuspension.getPerson().getStudent() != null) {
                this.studentNumber = academicActBlockingSuspension.getPerson().getStudent().getNumber();
            }

            this.beginDate = academicActBlockingSuspension.getBeginDate();
            this.endDate = academicActBlockingSuspension.getEndDate();
            this.reason = academicActBlockingSuspension.getReason();

            completed = true;
        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(academicActBlockingSuspension, e);
        }
    }

    @Override
    public void writeCellValues(final Row row, final IErrorsLog ierrorsLog) {
        final ErrorsLog errorsLog = (ErrorsLog) ierrorsLog;

        try {
            createCellWithValue(row, 0, identification);

            if (!completed) {
                createCellWithValue(row, 1, academicTreasuryBundle("error.DebtReportEntryBean.report.generation.verify.entry"));
                return;
            }

            int i = 1;

            createCellWithValue(row, i++, valueOrEmpty(versioningCreator));
            createCellWithValue(row, i++, valueOrEmpty(creationDate));
            createCellWithValue(row, i++, valueOrEmpty(name));
            createCellWithValue(row, i++, valueOrEmpty(identificationType));
            createCellWithValue(row, i++, valueOrEmpty(identificationNumber));
            createCellWithValue(row, i++, valueOrEmpty(vatNumber));
            createCellWithValue(row, i++, valueOrEmpty(email));
            createCellWithValue(row, i++, valueOrEmpty(address));
            createCellWithValue(row, i++, valueOrEmpty(addressCountryCode));
            createCellWithValue(row, i++, valueOrEmpty(studentNumber));
            createCellWithValue(row, i++, valueOrEmpty(beginDate));
            createCellWithValue(row, i++, valueOrEmpty(endDate));
            createCellWithValue(row, i++, valueOrEmpty(reason));

        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(academicActBlockingSuspension, e);
        }
    }

}
