package org.fenixedu.academictreasury.dto.reports;

import java.util.Comparator;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.treasury.IAcademicTreasuryTarget;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent.AcademicTreasuryEventKeys;
import org.fenixedu.academictreasury.domain.serviceRequests.ITreasuryServiceRequest;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.services.TuitionServices;
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
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

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

                if (originSettlementNote.getPaymentTransaction() != null
                        && originSettlementNote.getPaymentTransaction() != null) {
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

                if (academicTreasuryEvent.isForRegistrationTuition()) {
                    Registration registration = academicTreasuryEvent.getRegistration();

                    this.setRegistrationNumber(registration.getNumber());
                    this.setDegreeType(academicTreasuryServices
                            .localizedNameOfDegreeType(academicTreasuryEvent.getRegistration().getDegree().getDegreeType()));
                    this.setDegreeCode(academicTreasuryEvent.getRegistration().getDegree().getCode());
                    this.setDegreeName(academicTreasuryEvent.getRegistration().getDegree().getPresentationName());
                    this.setExecutionYear(academicTreasuryEvent.getExecutionYear().getQualifiedName());

                    if (debitEntry != null) {
                        this.setTuitionPaymentPlan(
                                AcademicTreasuryEventKeys.valueFor(debitEntry, AcademicTreasuryEventKeys.TUITION_PAYMENT_PLAN));
                        this.setTuitionPaymentPlanConditions(AcademicTreasuryEventKeys.valueFor(debitEntry,
                                AcademicTreasuryEventKeys.TUITION_PAYMENT_PLAN_CONDITIONS));
                    }

                    fillStudentConditionsInformation(registration, academicTreasuryEvent.getExecutionYear());

                } else if (academicTreasuryEvent.isForStandaloneTuition()
                        || academicTreasuryEvent.isForExtracurricularTuition()) {
                    if (debitEntry != null && debitEntry.getCurricularCourse() != null) {
                        this.setDegreeType(academicTreasuryServices
                                .localizedNameOfDegreeType(debitEntry.getCurricularCourse().getDegree().getDegreeType()));
                        this.setDegreeCode(debitEntry.getCurricularCourse().getDegree().getCode());
                        this.setDegreeName(debitEntry.getCurricularCourse().getDegree().getPresentationName());
                    }

                    if (debitEntry != null && debitEntry.getExecutionSemester() != null) {
                        this.setExecutionYear(academicTreasuryServices()
                                .executionYearOfExecutionSemester(debitEntry.getExecutionSemester()).getQualifiedName());
                        this.setExecutionSemester(debitEntry.getExecutionSemester().getQualifiedName());
                    }

                    if (debitEntry != null) {
                        this.setTuitionPaymentPlan(
                                AcademicTreasuryEventKeys.valueFor(debitEntry, AcademicTreasuryEventKeys.TUITION_PAYMENT_PLAN));
                        this.setTuitionPaymentPlanConditions(AcademicTreasuryEventKeys.valueFor(debitEntry,
                                AcademicTreasuryEventKeys.TUITION_PAYMENT_PLAN_CONDITIONS));
                    }

                    if (academicTreasuryEvent.getRegistration() != null && academicTreasuryEvent.getExecutionYear() != null) {
                        fillStudentConditionsInformation(academicTreasuryEvent.getRegistration(),
                                academicTreasuryEvent.getExecutionYear());

                    }

                } else if (academicTreasuryEvent.isForImprovementTax()) {
                    if (debitEntry != null && debitEntry.getCurricularCourse() != null) {
                        this.setDegreeType(academicTreasuryServices
                                .localizedNameOfDegreeType(debitEntry.getCurricularCourse().getDegree().getDegreeType()));
                        this.setDegreeCode(debitEntry.getCurricularCourse().getDegree().getCode());
                        this.setDegreeName(debitEntry.getCurricularCourse().getDegree().getPresentationName());
                    }

                    if (debitEntry != null && debitEntry.getExecutionSemester() != null) {
                        this.setExecutionYear(academicTreasuryServices()
                                .executionYearOfExecutionSemester(debitEntry.getExecutionSemester()).getQualifiedName());
                        this.setExecutionSemester(debitEntry.getExecutionSemester().getQualifiedName());
                    }

                    if (academicTreasuryEvent.getRegistration() != null && academicTreasuryEvent.getExecutionYear() != null) {
                        fillStudentConditionsInformation(academicTreasuryEvent.getRegistration(),
                                academicTreasuryEvent.getExecutionYear());
                    }
                } else if (academicTreasuryEvent.isForAcademicTax()) {
                    Registration registration = academicTreasuryEvent.getRegistration();

                    this.setRegistrationNumber(registration.getNumber());
                    this.setDegreeType(academicTreasuryServices
                            .localizedNameOfDegreeType(academicTreasuryEvent.getRegistration().getDegree().getDegreeType()));
                    this.setDegreeCode(academicTreasuryEvent.getRegistration().getDegree().getCode());
                    this.setDegreeName(academicTreasuryEvent.getRegistration().getDegree().getPresentationName());
                    this.setExecutionYear(academicTreasuryEvent.getExecutionYear().getQualifiedName());

                    fillStudentConditionsInformation(academicTreasuryEvent.getRegistration(),
                            academicTreasuryEvent.getExecutionYear());

                } else if (academicTreasuryEvent.isForAcademicServiceRequest()) {

                    final ITreasuryServiceRequest iTreasuryServiceRequest = academicTreasuryEvent.getITreasuryServiceRequest();

                    Registration registration = iTreasuryServiceRequest.getRegistration();
                    this.setRegistrationNumber(registration.getNumber());
                    this.setDegreeType(
                            academicTreasuryServices.localizedNameOfDegreeType(registration.getDegree().getDegreeType()));
                    this.setDegreeCode(registration.getDegree().getCode());
                    this.setDegreeName(registration.getDegree().getPresentationName());

                    if (iTreasuryServiceRequest.hasExecutionYear()) {
                        this.setExecutionYear(iTreasuryServiceRequest.getExecutionYear().getQualifiedName());
                        fillStudentConditionsInformation(iTreasuryServiceRequest.getRegistration(),
                                iTreasuryServiceRequest.getExecutionYear());
                    }
                } else if (academicTreasuryEvent.isForTreasuryEventTarget()) {
                    IAcademicTreasuryTarget treasuryEventTarget =
                            (IAcademicTreasuryTarget) academicTreasuryEvent.getTreasuryEventTarget();

                    if (treasuryEventTarget.getAcademicTreasuryTargetRegistration() != null) {
                        this.setRegistrationNumber(treasuryEventTarget.getAcademicTreasuryTargetRegistration().getNumber());
                        this.setDegreeType(treasuryEventTarget.getAcademicTreasuryTargetRegistration().getDegree().getDegreeType()
                                .getName().getContent());
                        this.setDegreeCode(treasuryEventTarget.getAcademicTreasuryTargetRegistration().getDegree().getCode());
                        this.setDegreeName(
                                treasuryEventTarget.getAcademicTreasuryTargetRegistration().getDegree().getPresentationName());
                    }

                    if (treasuryEventTarget.getAcademicTreasuryTargetExecutionYear() != null) {
                        this.setExecutionYear(treasuryEventTarget.getAcademicTreasuryTargetExecutionYear().getQualifiedName());
                    }

                    if (treasuryEventTarget.getAcademicTreasuryTargetExecutionSemester() != null) {
                        this.setExecutionSemester(
                                treasuryEventTarget.getAcademicTreasuryTargetExecutionSemester().getQualifiedName());
                    }
                } else if (academicTreasuryEvent.isForCustomAcademicDebt()) {
                    Registration registration = academicTreasuryEvent.getRegistration();

                    this.setRegistrationNumber(registration.getNumber());
                    this.setDegreeType(
                            academicTreasuryServices.localizedNameOfDegreeType(registration.getDegree().getDegreeType()));
                    this.setDegreeCode(registration.getDegree().getCode());
                    this.setDegreeName(registration.getDegree().getPresentationName());
                    this.setExecutionYear(academicTreasuryEvent.getExecutionYear().getQualifiedName());

                    fillStudentConditionsInformation(registration, academicTreasuryEvent.getExecutionYear());
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
        final IAcademicTreasuryPlatformDependentServices academicServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        this.setFirstTimeStudent(registration.isFirstTime(executionYear));
        this.setPartialRegime(
                academicServices.registrationRegimeType(registration, executionYear) == RegistrationRegimeType.PARTIAL_TIME);
        this.setStatutes(statutes(registration, executionYear));
        this.setAgreement(academicServices.registrationProtocol(registration).getDescription());
        IngressionType ingressionType = academicServices.ingression(registration);
        this.setIngression(ingressionType != null ? ingressionType.getDescription() : null);

        this.setNumberOfNormalEnrolments(TuitionServices.normalEnrolmentsIncludingAnnuled(registration, executionYear).size());
        this.setNumberOfStandaloneEnrolments(
                TuitionServices.standaloneEnrolmentsIncludingAnnuled(registration, executionYear).size());
        this.setNumberOfExtracurricularEnrolments(
                TuitionServices.extracurricularEnrolmentsIncludingAnnuled(registration, executionYear).size());
    }

    private String statutes(final Registration registration, final ExecutionYear executionYear) {
        final IAcademicTreasuryPlatformDependentServices services =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        return services.statutesTypesValidOnAnyExecutionSemesterFor(registration, executionYear).stream()
                .map(s -> s != null ? services.localizedNameOfStatuteType(s) : "").reduce((a, c) -> c + ", " + a).orElse(null);
    }

    private IAcademicTreasuryPlatformDependentServices academicTreasuryServices() {
        return AcademicTreasuryPlataformDependentServicesFactory.implementation();
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

}
