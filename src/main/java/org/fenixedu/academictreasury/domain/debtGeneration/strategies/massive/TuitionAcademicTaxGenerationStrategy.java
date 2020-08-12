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
package org.fenixedu.academictreasury.domain.debtGeneration.strategies.massive;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.debtGeneration.requests.IMassiveDebtGenerationStrategy;
import org.fenixedu.academictreasury.domain.debtGeneration.requests.MassiveDebtGenerationRequestFile;
import org.fenixedu.academictreasury.domain.debtGeneration.requests.MassiveDebtGenerationRequestFileBean;
import org.fenixedu.academictreasury.domain.debtGeneration.requests.MassiveDebtGenerationRowResult;
import org.fenixedu.academictreasury.domain.emoluments.AcademicTax;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.academictreasury.services.AcademicTaxServices;
import org.fenixedu.academictreasury.services.TuitionServices;
import org.fenixedu.academictreasury.util.ExcelUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

public class TuitionAcademicTaxGenerationStrategy implements IMassiveDebtGenerationStrategy<MassiveDebtGenerationRowResult> {

    private static final int MAX_COLS = 5;

    private static final int STUDENT_NUMBER_IDX = 0;
    private static final int STUDENT_NAME_IDX = 1;
    private static final int DEGREE_IDX = 2;
    private static final int DCP_IDX = 3;
    private static final int TUITION_PLAN_IDX = 4;

    @Override
    public void checkRules(final MassiveDebtGenerationRequestFile file) {
        if (file.getExecutionYear() == null) {
            throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequestFile.executionYear.required");
        }

        if (file.getTuitionPaymentPlanGroup() == null && file.getAcademicTax() == null) {
            throw new AcademicTreasuryDomainException(
                    "error.MassiveDebtGenerationRequestFile.tuitionPaymentPlanGroup.or.academic.tax.required");
        }

        if (file.getTuitionPaymentPlanGroup() != null && file.getAcademicTax() != null) {
            throw new AcademicTreasuryDomainException(
                    "error.MassiveDebtGenerationRequestFile.tuitionPaymentPlanGroup.and.academic.tax.specified");
        }

        if (file.getDebtDate() == null) {
            throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequestFile.debtDate.required");
        }
    }

    @Override
    public void process(final MassiveDebtGenerationRequestFile file) {
        final MassiveDebtGenerationRequestFileBean bean = new MassiveDebtGenerationRequestFileBean(file);
        final List<MassiveDebtGenerationRowResult> rowResult = readExcel(file.getContent(), bean);

        for (final MassiveDebtGenerationRowResult row : rowResult) {
            try {
                if (file.getTuitionPaymentPlanGroup() != null) {
                    boolean createdTuition =
                            TuitionServices.createTuitionForRegistration(row.getStudentCurricularPlan().getRegistration(),
                                    file.getExecutionYear(), file.getDebtDate(), true, row.getTuitionPaymentPlan(), true);

                    if (!createdTuition) {
                        final Integer registrationNumber = row.getStudentCurricularPlan().getRegistration().getNumber();
                        final String studentName = row.getStudentCurricularPlan().getRegistration().getStudent().getName();
                        final String tuitionPaymentPlanName = row.getTuitionPaymentPlan().getConditionsDescription().getContent();

                        throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequestFile.tuition.not.created",
                                String.valueOf(registrationNumber), studentName, tuitionPaymentPlanName);
                    }
                } else if (file.getAcademicTax() != null) {
                    boolean createdAcademicTax =
                            AcademicTaxServices.createAcademicTaxForDefaultFinantialEntity(row.getStudentCurricularPlan().getRegistration(),
                                    file.getExecutionYear(), file.getAcademicTax(), file.getDebtDate(), true);

                    if (!createdAcademicTax) {
                        final Integer registrationNumber = row.getStudentCurricularPlan().getRegistration().getNumber();
                        final String studentName = row.getStudentCurricularPlan().getRegistration().getStudent().getName();

                        throw new AcademicTreasuryDomainException(
                                "error.MassiveDebtGenerationRequestFile.academicTax.not.created",
                                String.valueOf(registrationNumber), studentName);
                    }
                }
            } catch (Exception e) {
                throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequestFile.debt.not.created",
                        String.valueOf(row.getRowNum()),
                        String.valueOf(row.getStudentCurricularPlan().getRegistration().getNumber()),
                        row.getStudentCurricularPlan().getRegistration().getPerson().getName(), e.getLocalizedMessage());
            }
        }

        file.setWhenProcessed(new DateTime());
    }

    @Override
    public List<MassiveDebtGenerationRowResult> readExcel(byte[] content, final MassiveDebtGenerationRequestFileBean bean) {

        final TuitionPaymentPlanGroup tuitionPaymentPlanGroup = bean.getTuitionPaymentPlanGroup();
        final AcademicTax academicTax = bean.getAcademicTax();
        final ExecutionYear executionYear = bean.getExecutionYear();
        final LocalDate debtDate = bean.getDebtDate();

        try {

            if (executionYear == null) {
                throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequestFile.executionYear.required");
            }

            if (tuitionPaymentPlanGroup == null && academicTax == null) {
                throw new AcademicTreasuryDomainException(
                        "error.MassiveDebtGenerationRequestFile.tuitionPaymentPlanGroup.or.academic.tax.required");
            }

            if (tuitionPaymentPlanGroup != null && academicTax != null) {
                throw new AcademicTreasuryDomainException(
                        "error.MassiveDebtGenerationRequestFile.tuitionPaymentPlanGroup.and.academic.tax.specified");
            }

            if (debtDate == null) {
                throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequestFile.debtDate.required");
            }

            List<List<String>> data = ExcelUtils.readExcel(new ByteArrayInputStream(content), MAX_COLS);

            final List<MassiveDebtGenerationRowResult> result = Lists.newArrayList();

            int rowNum = 0;
            for (final List<String> row : data) {
                rowNum++;
                if (rowNum == 1) {
                    continue;
                }

                if (Strings.isNullOrEmpty(row.get(STUDENT_NUMBER_IDX))) {
                    throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequest.student.number.invalid",
                            String.valueOf(rowNum));
                }

                if (Strings.isNullOrEmpty(row.get(STUDENT_NAME_IDX))) {
                    throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequest.student.name.invalid",
                            String.valueOf(rowNum));
                }

                if (Strings.isNullOrEmpty(row.get(DEGREE_IDX))) {
                    throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequest.degree.code.invalid",
                            String.valueOf(rowNum));
                }

                int registrationNumber = -1;
                try {
                    registrationNumber = Integer.parseInt(row.get(STUDENT_NUMBER_IDX).trim());
                } catch (final NumberFormatException e) {
                    throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequest.student.number.invalid",
                            String.valueOf(rowNum));
                }

                final String studentName = row.get(STUDENT_NAME_IDX).trim();
                final String degreeCode = row.get(DEGREE_IDX).trim();
                final String dcpName = row.get(DCP_IDX).trim();
                final String tuitionPaymentPlanName = row.get(TUITION_PLAN_IDX) != null ? row.get(TUITION_PLAN_IDX).trim() : null;

                final Degree degree = Degree.find(degreeCode);

                if (degree == null) {
                    throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequest.degree.not.found",
                            String.valueOf(rowNum));
                }

                final Registration registration =
                        findActiveRegistration(executionYear, registrationNumber, degree, dcpName, rowNum);

                if (registration == null) {
                    throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequest.registration.not.found",
                            String.valueOf(rowNum));
                }

                if (!registration.getStudent().getName().trim().equals(studentName)) {
                    throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequest.student.name.not.equal",
                            String.valueOf(rowNum));
                }

                StudentCurricularPlan studentCurricularPlan = null;
                if (!Strings.isNullOrEmpty(dcpName)) {
                    studentCurricularPlan = registration.getStudentCurricularPlan(executionYear);
                } else {
                    studentCurricularPlan = registration.getLastStudentCurricularPlan();
                }

                if (!Strings.isNullOrEmpty(dcpName) && !studentCurricularPlan.getName().equals(dcpName)) {
                    throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequest.dcp.name.not.equal",
                            String.valueOf(rowNum));
                }

                if (tuitionPaymentPlanGroup != null) {
                    TuitionPaymentPlan tuitionPaymentPlan = TuitionPaymentPlan
                            .find(tuitionPaymentPlanGroup, studentCurricularPlan.getDegreeCurricularPlan(), executionYear)
                            .filter(t -> t.getConditionsDescription().getContent().equals(tuitionPaymentPlanName)).findFirst()
                            .orElse(null);

                    if (tuitionPaymentPlan == null) {
                        throw new AcademicTreasuryDomainException(
                                "error.MassiveDebtGenerationRequest.tuition.payment.plan.not.found", String.valueOf(rowNum),
                                studentCurricularPlan.getName(), tuitionPaymentPlanName);
                    }

                    result.add(new MassiveDebtGenerationRowResult(rowNum, executionYear, studentCurricularPlan,
                            tuitionPaymentPlan, debtDate));
                } else if (academicTax != null) {
                    result.add(new MassiveDebtGenerationRowResult(rowNum, executionYear, studentCurricularPlan, academicTax,
                            debtDate));
                } else {
                    throw new RuntimeException("error");
                }

            }

            return result;

        } catch (final IOException e) {
            throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequest.invalid.sheet");
        }
    }

    private Registration findActiveRegistration(final ExecutionYear executionYear, final int registrationNumber,
            final Degree degree, final String dcpName, final int rowNum) {
        Registration result = null;
        for (final Registration registration : Registration.readByNumber(registrationNumber)) {
            if (registration.getLastStateType() == null || !registration.getLastStateType().isActive()) {
                continue;
            }

            if (registration.getDegree() != degree) {
                continue;
            }

            if (!Strings.isNullOrEmpty(dcpName) && registration.getStudentCurricularPlan(executionYear) == null) {
                continue;
            } else if (Strings.isNullOrEmpty(dcpName) && registration.getLastStudentCurricularPlan() == null) {
                continue;
            }

            if (!Strings.isNullOrEmpty(dcpName)
                    && !registration.getStudentCurricularPlan(executionYear).getName().equals(dcpName)) {
                continue;
            }

            if (result != null) {
                throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequest.found.more.than.one.registration",
                        String.valueOf(rowNum));
            }

            result = registration;
        }

        return result;
    }

    @Override
    public boolean isDebtDateRequired() {
        return true;
    }

    @Override
    public boolean isExecutionYearRequired() {
        return true;
    }

    @Override
    public boolean isForAcademicTaxRequired() {
        return true;
    }

    @Override
    public boolean isFinantialInstitutionRequired() {
        return false;
    }

    @Override
    public boolean isReasonRequired() {
        return false;
    }

    public String viewUrl() {
        return "/WEB-INF/academicTreasury/debtGeneration/strategies/massive/TuitionAcademicTaxGenerationStrategy.jsp";
    }

    @Override
    public String dataDescription(final MassiveDebtGenerationRequestFile file) {
        if(file.getTuitionPaymentPlanGroup() != null) {
            return academicTreasuryBundle("label.TuitionAcademicTaxGenerationStrategy.dataDescription.tuitionPaymentPlanGroup", 
                        file.getTuitionPaymentPlanGroup().getName().getContent(), 
                        file.getExecutionYear().getQualifiedName(), 
                        file.getDebtDate().toString(org.fenixedu.academictreasury.util.AcademicTreasuryConstants.DATE_FORMAT));
            
        } else {
            return academicTreasuryBundle("label.TuitionAcademicTaxGenerationStrategy.dataDescription.academicTax", 
                    file.getAcademicTax().getProduct().getName().getContent(), 
                    file.getExecutionYear().getQualifiedName(), 
                    file.getDebtDate().toString(org.fenixedu.academictreasury.util.AcademicTreasuryConstants.DATE_FORMAT));
            
        }
    }

}
