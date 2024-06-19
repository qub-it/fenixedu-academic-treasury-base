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
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryTarget;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.reports.DebtReportRequest;
import org.fenixedu.academictreasury.domain.reports.ErrorsLog;
import org.fenixedu.academictreasury.domain.serviceRequests.ITreasuryServiceRequest;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

public class TreasuryExemptionReportEntryBean extends AbstractReportEntryBean {

    public static String[] SPREADSHEET_HEADERS = {
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.identification"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.versioningCreator"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.creationDate"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.customerId"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.debtAccountId"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.customerName"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.identificationType"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.identificationNumber"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.vatNumber"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.studentNumber"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.registrationNumber"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.debitEntryId"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.documentNumber"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.debitEntryDescription"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.exemptedAmount"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.exemptionType.code"), 
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.exemptionType.description"), 
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.reason"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.degreeType"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.degreeCode"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.degreeName"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.executionYear"),
            academicTreasuryBundle("label.TreasuryExemptionReportEntryBean.header.executionSemester"),
            };

    private String identification;
    
    private String versioningCreator;
    private DateTime creationDate;
    
    private String customerId;
    private String debtAccountId;
    private String customerName;    
    private String identificationType;
    private String identificationNumber;
    private String vatNumber;
    private Integer studentNumber;
    private Integer registrationNumber;
    
    private String debitEntryId;
    private String documentNumber;
    private String debitEntryDescription;
    private String exemptedAmount;
    
    private String exemptionTypeCode;
    private String exemptionTypeDescription;
    private String reason;
    
    private String degreeType;
    private String degreeCode;
    private String degreeName;
    private String executionYear;
    private String executionSemester;

    private TreasuryExemption treasuryExemption;

    boolean completed = false;

    public TreasuryExemptionReportEntryBean(final TreasuryExemption treasuryExemption, final DebtReportRequest request, final ErrorsLog errorsLog) {
        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        final String decimalSeparator = request.getDecimalSeparator();
        
        try {
            this.treasuryExemption = treasuryExemption;

            this.identification = treasuryExemption.getExternalId();
            
            this.versioningCreator = treasuryServices.versioningCreatorUsername(treasuryExemption);
            this.creationDate = treasuryServices.versioningCreationDate(treasuryExemption);
            
            this.customerId = treasuryExemption.getDebitEntry().getDebtAccount().getCustomer().getExternalId();
            this.debtAccountId = treasuryExemption.getDebitEntry().getDebtAccount().getExternalId();
            this.customerName = treasuryExemption.getDebitEntry().getDebtAccount().getCustomer().getName();

            Customer customer = treasuryExemption.getDebitEntry().getDebtAccount().getCustomer();
            
            if (customer.isPersonCustomer() && ((PersonCustomer) customer).getAssociatedPerson() != null
                    && ((PersonCustomer) customer).getAssociatedPerson().getIdDocumentType() != null) {
                this.identificationType = ((PersonCustomer) customer).getAssociatedPerson().getIdDocumentType().getLocalizedName();
            }

            this.identificationNumber = customer.getIdentificationNumber();
            this.vatNumber = customer.getUiFiscalNumber();

            if (customer.isPersonCustomer() && ((PersonCustomer) customer).getAssociatedPerson() != null
                    && ((PersonCustomer) customer).getAssociatedPerson().getStudent() != null) {
                this.studentNumber = ((PersonCustomer) customer).getAssociatedPerson().getStudent().getNumber();
            }
            
            this.debitEntryId = treasuryExemption.getDebitEntry().getExternalId();
            this.debitEntryDescription = treasuryExemption.getDebitEntry().getDescription();

            if (treasuryExemption.getDebitEntry().getFinantialDocument() != null) {
                this.documentNumber = treasuryExemption.getDebitEntry().getFinantialDocument().getUiDocumentNumber();
            }
            
            this.exemptedAmount =
                    treasuryExemption.getDebitEntry().getCurrency().getValueFor(treasuryExemption.getNetAmountToExempt());
            
            if(DebtReportRequest.COMMA.equals(decimalSeparator)) {
                this.exemptedAmount = this.exemptedAmount.replace(DebtReportRequest.DOT, DebtReportRequest.COMMA);
            }
            
            this.exemptionTypeCode = treasuryExemption.getTreasuryExemptionType().getCode();
            this.exemptionTypeDescription = treasuryExemption.getTreasuryExemptionType().getName().getContent();
            
            this.reason = treasuryExemption.getReason();

            fillAcademicInformation(treasuryExemption.getDebitEntry());
            
            this.completed = true;
        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(treasuryExemption, e);
        }

    }

    private void fillAcademicInformation(final InvoiceEntry entry) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        final DebitEntry debitEntry = entry.isDebitNoteEntry() ? (DebitEntry) entry : ((CreditEntry) entry).getDebitEntry();

        if (debitEntry != null) {

            // Degree && ExecutionYear && ExecutionInterval
            if (debitEntry.getTreasuryEvent() != null && debitEntry.getTreasuryEvent() instanceof AcademicTreasuryEvent) {
                final AcademicTreasuryEvent academicTreasuryEvent = (AcademicTreasuryEvent) debitEntry.getTreasuryEvent();

                if (academicTreasuryEvent.isForRegistrationTuition()) {
                    this.registrationNumber = academicTreasuryEvent.getRegistration().getNumber();
                    this.degreeType = academicTreasuryServices
                            .localizedNameOfDegreeType(academicTreasuryEvent.getRegistration().getDegree().getDegreeType());
                    this.degreeCode = academicTreasuryEvent.getRegistration().getDegree().getCode();
                    this.degreeName = academicTreasuryEvent.getRegistration().getDegree().getPresentationName();
                    this.executionYear = academicTreasuryEvent.getExecutionYear().getQualifiedName();

                } else if (academicTreasuryEvent.isForStandaloneTuition()
                        || academicTreasuryEvent.isForExtracurricularTuition()) {
                    if (debitEntry.getCurricularCourse() != null) {
                        this.degreeType = academicTreasuryServices
                                .localizedNameOfDegreeType(debitEntry.getCurricularCourse().getDegree().getDegreeType());
                        this.degreeCode = debitEntry.getCurricularCourse().getDegree().getCode();
                        this.degreeName = debitEntry.getCurricularCourse().getDegree().getPresentationName();
                    }

                    if (debitEntry.getExecutionSemester() != null) {
                        this.executionYear = academicTreasuryServices().executionYearOfExecutionSemester(debitEntry.getExecutionSemester()).getQualifiedName();
                        this.executionSemester = debitEntry.getExecutionSemester().getQualifiedName();
                    }

                } else if (academicTreasuryEvent.isForImprovementTax()) {
                    if (debitEntry.getCurricularCourse() != null) {
                        this.degreeType = academicTreasuryServices
                                .localizedNameOfDegreeType(debitEntry.getCurricularCourse().getDegree().getDegreeType());
                        this.degreeCode = debitEntry.getCurricularCourse().getDegree().getCode();
                        this.degreeName = debitEntry.getCurricularCourse().getDegree().getPresentationName();
                    }

                    if (debitEntry.getExecutionSemester() != null) {
                        this.executionYear = academicTreasuryServices().executionYearOfExecutionSemester(debitEntry.getExecutionSemester()).getQualifiedName();
                        this.executionSemester = debitEntry.getExecutionSemester().getQualifiedName();
                    }

                } else if (academicTreasuryEvent.isForAcademicTax()) {

                    this.registrationNumber = academicTreasuryEvent.getRegistration().getNumber();
                    this.degreeType = academicTreasuryServices
                            .localizedNameOfDegreeType(academicTreasuryEvent.getRegistration().getDegree().getDegreeType());
                    this.degreeCode = academicTreasuryEvent.getRegistration().getDegree().getCode();
                    this.degreeName = academicTreasuryEvent.getRegistration().getDegree().getPresentationName();
                    this.executionYear = academicTreasuryEvent.getExecutionYear().getQualifiedName();


                } else if (academicTreasuryEvent.isForAcademicServiceRequest()) {

                    final ITreasuryServiceRequest iTreasuryServiceRequest = academicTreasuryEvent.getITreasuryServiceRequest();

                    this.registrationNumber = iTreasuryServiceRequest.getRegistration().getNumber();
                    this.degreeType = academicTreasuryServices
                            .localizedNameOfDegreeType(iTreasuryServiceRequest.getRegistration().getDegree().getDegreeType());
                    this.degreeCode = iTreasuryServiceRequest.getRegistration().getDegree().getCode();
                    this.degreeName = iTreasuryServiceRequest.getRegistration().getDegree().getPresentationName();

                    if (iTreasuryServiceRequest.hasExecutionYear()) {
                        this.executionYear = iTreasuryServiceRequest.getExecutionYear().getQualifiedName();
                    }
                } else if (academicTreasuryEvent.isForTreasuryEventTarget()) {
                    final IAcademicTreasuryTarget treasuryEventTarget =
                            (IAcademicTreasuryTarget) academicTreasuryEvent.getTreasuryEventTarget();

                    if (treasuryEventTarget.getAcademicTreasuryTargetRegistration() != null) {
                        this.registrationNumber = treasuryEventTarget.getAcademicTreasuryTargetRegistration().getNumber();
                        this.degreeType = treasuryEventTarget.getAcademicTreasuryTargetRegistration().getDegree().getDegreeType()
                                .getName().getContent();
                        this.degreeCode = treasuryEventTarget.getAcademicTreasuryTargetRegistration().getDegree().getCode();
                        this.degreeName =
                                treasuryEventTarget.getAcademicTreasuryTargetRegistration().getDegree().getPresentationName();
                    }

                    if (treasuryEventTarget.getAcademicTreasuryTargetExecutionYear() != null) {
                        this.executionYear = treasuryEventTarget.getAcademicTreasuryTargetExecutionYear().getQualifiedName();
                    }

                    if (treasuryEventTarget.getAcademicTreasuryTargetExecutionSemester() != null) {
                        this.executionSemester =
                                treasuryEventTarget.getAcademicTreasuryTargetExecutionSemester().getQualifiedName();
                    }
                }

            } else if (debitEntry.getTreasuryEvent() != null) {
                final TreasuryEvent treasuryEvent = debitEntry.getTreasuryEvent();

                if (!Strings.isNullOrEmpty(treasuryEvent.getDegreeCode())) {
                    this.degreeCode = treasuryEvent.getDegreeCode();
                }

                if (!Strings.isNullOrEmpty(treasuryEvent.getDegreeName())) {
                    this.degreeName = treasuryEvent.getDegreeName();
                }

                if (!Strings.isNullOrEmpty(treasuryEvent.getExecutionYearName())) {
                    this.executionYear = treasuryEvent.getExecutionYearName();
                }
            }

            if (Strings.isNullOrEmpty(this.degreeCode)) {
                this.degreeCode = debitEntry.getDegreeCode();
            }

            if (Strings.isNullOrEmpty(this.executionYear)) {
                this.executionYear = debitEntry.getExecutionYearName();
            }
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
            
            createCellWithValue(row, i++, valueOrEmpty(customerId));
            createCellWithValue(row, i++, valueOrEmpty(debtAccountId));
            createCellWithValue(row, i++, valueOrEmpty(customerName));
            createCellWithValue(row, i++, valueOrEmpty(this.identificationType));
            createCellWithValue(row, i++, valueOrEmpty(this.identificationNumber));
            createCellWithValue(row, i++, valueOrEmpty(this.vatNumber));
            createCellWithValue(row, i++, valueOrEmpty(this.studentNumber));
            createCellWithValue(row, i++, valueOrEmpty(this.registrationNumber));
            
            createCellWithValue(row, i++, valueOrEmpty(debitEntryId));
            createCellWithValue(row, i++, valueOrEmpty(this.documentNumber));
            createCellWithValue(row, i++, valueOrEmpty(debitEntryDescription));
            createCellWithValue(row, i++, valueOrEmpty(exemptedAmount));
            
            createCellWithValue(row, i++, valueOrEmpty(this.exemptionTypeCode));
            createCellWithValue(row, i++, valueOrEmpty(this.exemptionTypeDescription));
            createCellWithValue(row, i++, valueOrEmpty(reason));
            
            createCellWithValue(row, i++, valueOrEmpty(degreeType));
            createCellWithValue(row, i++, valueOrEmpty(degreeCode));
            createCellWithValue(row, i++, valueOrEmpty(degreeName));
            createCellWithValue(row, i++, valueOrEmpty(executionYear));
            createCellWithValue(row, i++, valueOrEmpty(executionSemester));
            
        } catch (final Exception e) {
            e.printStackTrace();
            errorsLog.addError(this.treasuryExemption, e);
        }
    }

    private IAcademicTreasuryPlatformDependentServices academicTreasuryServices() {
        return AcademicTreasuryPlataformDependentServicesFactory.implementation();
    }

}
