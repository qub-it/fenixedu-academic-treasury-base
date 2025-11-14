package org.fenixedu.academictreasury.dto.reports;

import java.util.Comparator;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryTarget;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent.AcademicTreasuryEventKeys;
import org.fenixedu.academictreasury.domain.serviceRequests.ITreasuryServiceRequest;
import org.fenixedu.academictreasury.services.TuitionServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.settings.TreasurySettings;

import com.google.common.base.Strings;

// ANIL 2024-07-19 
//
// Use this interface to not repeat code between methods used for report
public interface IFinantialReportEntryCommonMethods {

    default void fillAcademicInformation(final InvoiceEntry invoiceEntry) {
        DebitEntry debitEntry =
                invoiceEntry.isDebitNoteEntry() ? (DebitEntry) invoiceEntry : ((CreditEntry) invoiceEntry).getDebitEntry();

        TreasuryEvent treasuryEvent = null;
        if (debitEntry != null) {
            treasuryEvent = debitEntry.getTreasuryEvent();
        } else if (invoiceEntry.isCreditNoteEntry()) {
            treasuryEvent = ((CreditEntry) invoiceEntry).getTreasuryEvent();
        }

        if (treasuryEvent == null) {
            // In some cases it is not possible to have the treasury event available,
            // for example in the excess payment

            // In the case of excess payment, try to retrieve information
            // with treasury event of some debitEntry associated with
            // a payment request

            // Usually excessPayments are created with paymentRequests

            boolean isTreasuryCertificationMode =
                    invoiceEntry.getDebtAccount().getFinantialInstitution().isInvoiceRegistrationByTreasuryCertification();
            boolean isExcessPayment = invoiceEntry.getProduct() == TreasurySettings.getInstance().getAdvancePaymentProduct();

            if (isTreasuryCertificationMode && isExcessPayment) {
                final DebitEntry originDebitEntry = getExcessPaymentOriginDebitEntry(invoiceEntry);

                if (originDebitEntry == null || originDebitEntry.getFinantialDocument() == null) {
                    return;
                }

                SettlementNote originSettlementNote = originDebitEntry.getDebitNote().getExcessPaymentSettlementNote();

                if (originSettlementNote == null) {
                    return;
                }

                if (originSettlementNote.getPaymentTransaction() != null && originSettlementNote.getPaymentTransaction() != null) {
                    PaymentRequest paymentRequest = originSettlementNote.getPaymentTransaction().getPaymentRequest();

                    TreasuryEvent relatedTreasuryEvent = paymentRequest.getDebitEntriesSet().stream() //
                            .sorted(Comparator.comparing(DebitEntry::getExternalId)) //
                            .filter(de -> de.getTreasuryEvent() != null).map(de -> de.getTreasuryEvent()) //
                            .findFirst() //
                            .orElse(null);

                    treasuryEvent = relatedTreasuryEvent;
                }

                if (treasuryEvent == null) {
                    // Try to get treasuryEvent by some debit that was
                    // paid in the original settlement note

                    TreasuryEvent relatedTreasuryEvent = originSettlementNote.getSettlemetEntriesSet().stream() //
                            .sorted(Comparator.comparing(SettlementEntry::getExternalId)) //
                            .filter(se -> se.getInvoiceEntry() != originDebitEntry) //
                            .filter(se -> se.getInvoiceEntry().getTreasuryEvent() != null) //
                            .map(se -> se.getInvoiceEntry().getTreasuryEvent()) //
                            .findFirst() //
                            .orElse(null);

                    treasuryEvent = relatedTreasuryEvent;
                }
            }
        }

        if (treasuryEvent != null) {

            // Degree && ExecutionYear && ExecutionInterval
            if (treasuryEvent instanceof AcademicTreasuryEvent) {
                final AcademicTreasuryEvent academicTreasuryEvent = (AcademicTreasuryEvent) treasuryEvent;

                if (academicTreasuryEvent.getDegree() != null) {
                    this.setDegreeType(academicTreasuryEvent.getDegree().getDegreeType().getName().getContent());
                    this.setDegreeCode(academicTreasuryEvent.getDegree().getCode());
                    this.setDegreeName(academicTreasuryEvent.getDegree().getPresentationName());
                    this.setDegreeDomainObject(academicTreasuryEvent.getDegree());
                }

                if (academicTreasuryEvent.getExecutionYear() != null) {
                    this.setExecutionYear(academicTreasuryEvent.getExecutionYear().getQualifiedName());
                    this.setExecutionYearDomainObject(academicTreasuryEvent.getExecutionYear());
                }

                if (academicTreasuryEvent.isForRegistrationTuition()) {
                    Registration registration = academicTreasuryEvent.getRegistration();
                    ExecutionYear executionYear = academicTreasuryEvent.getExecutionYear();

                    this.setRegistrationNumber(registration.getNumber());
                    this.setDegreeType(
                            academicTreasuryEvent.getRegistration().getDegree().getDegreeType().getName().getContent());
                    this.setDegreeCode(academicTreasuryEvent.getRegistration().getDegree().getCode());
                    this.setDegreeName(academicTreasuryEvent.getRegistration().getDegree().getPresentationName());
                    this.setDegreeDomainObject(academicTreasuryEvent.getRegistration().getDegree());
                    this.setExecutionYear(executionYear.getQualifiedName());
                    this.setExecutionYearDomainObject(executionYear);

                    if (debitEntry != null) {
                        this.setTuitionPaymentPlan(
                                AcademicTreasuryEventKeys.valueFor(debitEntry, AcademicTreasuryEventKeys.TUITION_PAYMENT_PLAN));
                        this.setTuitionPaymentPlanConditions(AcademicTreasuryEventKeys.valueFor(debitEntry,
                                AcademicTreasuryEventKeys.TUITION_PAYMENT_PLAN_CONDITIONS));
                    }

                    fillStudentConditionsInformation(registration, executionYear);

                    this.setActiveStudentCurricularPlanOfExecutionYear(registration.getStudentCurricularPlan(executionYear));

                    if (this.getActiveStudentCurricularPlanOfExecutionYear() == null) {
                        this.setActiveStudentCurricularPlanOfExecutionYear(registration.getActiveStudentCurricularPlan());
                    }
                } else if (academicTreasuryEvent.isForStandaloneTuition() || academicTreasuryEvent.isForExtracurricularTuition()) {
                    if (debitEntry != null) {
                        CurricularCourse curricularCourse = getCurricularCourse(debitEntry);

                        if (curricularCourse != null) {
                            this.setDegreeType(curricularCourse.getDegree().getDegreeType().getName().getContent());
                            this.setDegreeCode(curricularCourse.getDegree().getCode());
                            this.setDegreeName(curricularCourse.getDegree().getPresentationName());
                            this.setDegreeDomainObject(curricularCourse.getDegree());
                        }

                        if (debitEntry.getExecutionSemester() != null) {
                            this.setExecutionYear(debitEntry.getExecutionSemester().getExecutionYear().getQualifiedName());
                            this.setExecutionYearDomainObject(debitEntry.getExecutionSemester().getExecutionYear());
                            this.setExecutionSemester(debitEntry.getExecutionSemester().getQualifiedName());
                        }

                        this.setTuitionPaymentPlan(
                                AcademicTreasuryEventKeys.valueFor(debitEntry, AcademicTreasuryEventKeys.TUITION_PAYMENT_PLAN));
                        this.setTuitionPaymentPlanConditions(AcademicTreasuryEventKeys.valueFor(debitEntry,
                                AcademicTreasuryEventKeys.TUITION_PAYMENT_PLAN_CONDITIONS));
                    }

                    Registration registration = academicTreasuryEvent.getRegistration();
                    ExecutionYear executionYear = academicTreasuryEvent.getExecutionYear();
                    fillStudentConditionsInformation(registration, executionYear);

                    this.setActiveStudentCurricularPlanOfExecutionYear(registration.getStudentCurricularPlan(executionYear));

                    if (this.getActiveStudentCurricularPlanOfExecutionYear() == null) {
                        this.setActiveStudentCurricularPlanOfExecutionYear(registration.getActiveStudentCurricularPlan());
                    }
                } else if (academicTreasuryEvent.isForImprovementTax()) {
                    if (debitEntry != null) {
                        CurricularCourse curricularCourse = getCurricularCourse(debitEntry);

                        if (curricularCourse != null) {
                            this.setDegreeType(curricularCourse.getDegree().getDegreeType().getName().getContent());
                            this.setDegreeCode(curricularCourse.getDegree().getCode());
                            this.setDegreeName(curricularCourse.getDegree().getPresentationName());
                            this.setDegreeDomainObject(curricularCourse.getDegree());
                        }

                        if (debitEntry.getExecutionSemester() != null) {
                            this.setExecutionYear(debitEntry.getExecutionSemester().getExecutionYear().getQualifiedName());
                            this.setExecutionSemester(debitEntry.getExecutionSemester().getQualifiedName());
                            this.setExecutionYearDomainObject(debitEntry.getExecutionSemester().getExecutionYear());
                        }
                    }

                    Registration registration = academicTreasuryEvent.getRegistration();
                    ExecutionYear executionYear = academicTreasuryEvent.getExecutionYear();
                    fillStudentConditionsInformation(registration, executionYear);

                    this.setActiveStudentCurricularPlanOfExecutionYear(registration.getStudentCurricularPlan(executionYear));

                    if (this.getActiveStudentCurricularPlanOfExecutionYear() == null) {
                        this.setActiveStudentCurricularPlanOfExecutionYear(registration.getActiveStudentCurricularPlan());
                    }
                } else if (academicTreasuryEvent.isForAcademicTax()) {
                    Registration registration = academicTreasuryEvent.getRegistration();
                    ExecutionYear executionYear = academicTreasuryEvent.getExecutionYear();

                    this.setRegistrationNumber(registration.getNumber());
                    this.setDegreeType(
                            academicTreasuryEvent.getRegistration().getDegree().getDegreeType().getName().getContent());
                    this.setDegreeCode(academicTreasuryEvent.getRegistration().getDegree().getCode());
                    this.setDegreeName(academicTreasuryEvent.getRegistration().getDegree().getPresentationName());
                    this.setDegreeDomainObject(academicTreasuryEvent.getRegistration().getDegree());
                    this.setExecutionYear(executionYear.getQualifiedName());
                    this.setExecutionYearDomainObject(executionYear);

                    fillStudentConditionsInformation(academicTreasuryEvent.getRegistration(), executionYear);

                    this.setActiveStudentCurricularPlanOfExecutionYear(registration.getStudentCurricularPlan(executionYear));

                    if (this.getActiveStudentCurricularPlanOfExecutionYear() == null) {
                        this.setActiveStudentCurricularPlanOfExecutionYear(registration.getActiveStudentCurricularPlan());
                    }
                } else if (academicTreasuryEvent.isForAcademicServiceRequest()) {
                    final ITreasuryServiceRequest iTreasuryServiceRequest = academicTreasuryEvent.getITreasuryServiceRequest();

                    Registration registration = iTreasuryServiceRequest.getRegistration();
                    this.setRegistrationNumber(registration.getNumber());
                    this.setDegreeType(registration.getDegree().getDegreeType().getName().getContent());
                    this.setDegreeCode(registration.getDegree().getCode());
                    this.setDegreeName(registration.getDegree().getPresentationName());
                    this.setDegreeDomainObject(registration.getDegree());

                    if (iTreasuryServiceRequest.hasExecutionYear()) {
                        ExecutionYear executionYear = iTreasuryServiceRequest.getExecutionYear();

                        this.setExecutionYear(executionYear.getQualifiedName());
                        this.setExecutionYearDomainObject(executionYear);
                        fillStudentConditionsInformation(iTreasuryServiceRequest.getRegistration(), executionYear);

                        this.setActiveStudentCurricularPlanOfExecutionYear(registration.getStudentCurricularPlan(executionYear));
                    }

                    if (this.getActiveStudentCurricularPlanOfExecutionYear() == null) {
                        this.setActiveStudentCurricularPlanOfExecutionYear(registration.getActiveStudentCurricularPlan());
                    }
                } else if (academicTreasuryEvent.isForTreasuryEventTarget()) {
                    IAcademicTreasuryTarget treasuryEventTarget =
                            (IAcademicTreasuryTarget) academicTreasuryEvent.getTreasuryEventTarget();

                    Registration registration = treasuryEventTarget.getAcademicTreasuryTargetRegistration();
                    if (registration != null) {
                        this.setRegistrationNumber(registration.getNumber());
                        this.setDegreeType(registration.getDegree().getDegreeType().getName().getContent());
                        this.setDegreeCode(registration.getDegree().getCode());
                        this.setDegreeName(registration.getDegree().getPresentationName());
                        this.setDegreeDomainObject(registration.getDegree());
                    }

                    ExecutionYear executionYear = treasuryEventTarget.getAcademicTreasuryTargetExecutionYear();
                    if (executionYear != null) {
                        this.setExecutionYear(executionYear.getQualifiedName());
                        this.setExecutionYearDomainObject(executionYear);
                    }

                    if (treasuryEventTarget.getAcademicTreasuryTargetExecutionSemester() != null) {
                        this.setExecutionSemester(
                                treasuryEventTarget.getAcademicTreasuryTargetExecutionSemester().getQualifiedName());
                    }

                    if (registration != null && executionYear != null) {
                        this.setActiveStudentCurricularPlanOfExecutionYear(registration.getStudentCurricularPlan(executionYear));
                    }

                    if (registration != null && this.getActiveStudentCurricularPlanOfExecutionYear() == null) {
                        this.setActiveStudentCurricularPlanOfExecutionYear(registration.getActiveStudentCurricularPlan());
                    }
                } else if (academicTreasuryEvent.isForCustomAcademicDebt()) {
                    Registration registration = academicTreasuryEvent.getRegistration();
                    ExecutionYear executionYear = academicTreasuryEvent.getExecutionYear();

                    this.setRegistrationNumber(registration.getNumber());
                    this.setDegreeType(registration.getDegree().getDegreeType().getName().getContent());
                    this.setDegreeCode(registration.getDegree().getCode());
                    this.setDegreeName(registration.getDegree().getPresentationName());
                    this.setDegreeDomainObject(registration.getDegree());
                    this.setExecutionYear(executionYear.getQualifiedName());
                    this.setExecutionYearDomainObject(executionYear);

                    fillStudentConditionsInformation(registration, executionYear);

                    this.setActiveStudentCurricularPlanOfExecutionYear(registration.getStudentCurricularPlan(executionYear));
                    if (this.getActiveStudentCurricularPlanOfExecutionYear() == null) {
                        this.setActiveStudentCurricularPlanOfExecutionYear(registration.getActiveStudentCurricularPlan());
                    }
                }
            } else {
                if (!Strings.isNullOrEmpty(treasuryEvent.getDegreeCode())) {
                    this.setDegreeCode(treasuryEvent.getDegreeCode());
                }

                if (!Strings.isNullOrEmpty(treasuryEvent.getDegreeName())) {
                    this.setDegreeName(treasuryEvent.getDegreeName());
                }

                if (!Strings.isNullOrEmpty(treasuryEvent.getExecutionYearName())) {
                    this.setExecutionYear(treasuryEvent.getExecutionYearName());
                }
            }

            if (debitEntry != null && Strings.isNullOrEmpty(this.getDegreeCode())) {
                this.setDegreeCode(debitEntry.getDegreeCode());
            }

            if (debitEntry != null && Strings.isNullOrEmpty(this.getExecutionYear())) {
                this.setExecutionYear(debitEntry.getExecutionYearName());
            }
        }
    }

    private static CurricularCourse getCurricularCourse(DebitEntry debitEntry) {
        if (debitEntry.getCurricularCourse() != null) {
            return debitEntry.getCurricularCourse();
        }

        if (debitEntry.getDebitEntry() != null) {
            return debitEntry.getDebitEntry().getCurricularCourse();
        }

        return null;
    }

    default DebitEntry getExcessPaymentOriginDebitEntry(final InvoiceEntry invoiceEntry) {
        DebitEntry originDebitEntry = null;

        if (invoiceEntry.isDebitNoteEntry()) {
            originDebitEntry = (DebitEntry) invoiceEntry;
        } else if (invoiceEntry.isCreditNoteEntry()) {
            originDebitEntry = ((CreditEntry) invoiceEntry).getDebitEntry();
        }
        return originDebitEntry;
    }

    private void fillStudentConditionsInformation(final Registration registration, final ExecutionYear executionYear) {
        this.setFirstTimeStudent(registration.isFirstTime(executionYear));
        this.setPartialRegime(registration.getRegimeType(executionYear) == RegistrationRegimeType.PARTIAL_TIME);
        this.setStatutes(statutes(registration, executionYear));
        this.setAgreement(registration.getRegistrationProtocol().getDescription());
        IngressionType ingressionType = registration.getIngressionType();
        this.setIngression(ingressionType != null ? ingressionType.getDescription() : null);

        this.setNumberOfNormalEnrolments(TuitionServices.normalEnrolmentsIncludingAnnuled(registration, executionYear).size());
        this.setNumberOfStandaloneEnrolments(
                TuitionServices.standaloneEnrolmentsIncludingAnnuled(registration, executionYear).size());
        this.setNumberOfExtracurricularEnrolments(
                TuitionServices.extracurricularEnrolmentsIncludingAnnuled(registration, executionYear).size());
    }

    private String statutes(final Registration registration, final ExecutionYear executionYear) {
        return AcademicTreasuryConstants.statutesTypesValidOnAnyExecutionSemesterFor(registration, executionYear).stream()
                .map(s -> s != null ? s.getName().getContent() : "").reduce((a, c) -> c + ", " + a).orElse(null);
    }

    public Integer getRegistrationNumber();

    public void setRegistrationNumber(Integer registrationNumber);

    public String getDegreeType();

    public void setDegreeType(String degreeType);

    public String getDegreeCode();

    public void setDegreeCode(String degreeCode);

    public String getDegreeName();

    public void setDegreeName(String degreeName);

    public String getExecutionYear();

    public void setExecutionYear(String executionYear);

    public String getExecutionSemester();

    public void setExecutionSemester(String executionSemester);

    public LocalizedString getAgreement();

    public void setAgreement(LocalizedString agreement);

    public LocalizedString getIngression();

    public void setIngression(LocalizedString ingression);

    public Boolean getFirstTimeStudent();

    public void setFirstTimeStudent(Boolean firstTimeStudent);

    public Boolean getPartialRegime();

    public void setPartialRegime(Boolean partialRegime);

    public String getStatutes();

    public void setStatutes(String statutes);

    public Integer getNumberOfNormalEnrolments();

    public void setNumberOfNormalEnrolments(Integer numberOfNormalEnrolments);

    public Integer getNumberOfStandaloneEnrolments();

    public void setNumberOfStandaloneEnrolments(Integer numberOfStandaloneEnrolments);

    public Integer getNumberOfExtracurricularEnrolments();

    public void setNumberOfExtracurricularEnrolments(Integer numberOfExtracurricularEnrolments);

    public String getTuitionPaymentPlan();

    public void setTuitionPaymentPlan(String tuitionPaymentPlan);

    public String getTuitionPaymentPlanConditions();

    public void setTuitionPaymentPlanConditions(String tuitionPaymentPlanConditions);

    public abstract Degree getDegreeDomainObject();

    public abstract void setDegreeDomainObject(Degree degree);

    public abstract ExecutionYear getExecutionYearDomainObject();

    public abstract void setExecutionYearDomainObject(ExecutionYear executionYear);

    public abstract StudentCurricularPlan getActiveStudentCurricularPlanOfExecutionYear();

    public abstract void setActiveStudentCurricularPlanOfExecutionYear(
            StudentCurricularPlan activeStudentCurricularPlanOfExecutionYear);

}
