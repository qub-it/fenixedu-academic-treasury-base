[![qubIT](http://www.qub-it.com/cms/images/qubIT_logo_transparent_medium.png)](http://www.qub-it.com)

# fenixedu-academic-treasury

This module provides the **integration between FenixEdu Treasury and FenixEdu Academic**, bridging academic processes with financial operations. It enables automatic debt generation, tuition calculation, and academic service request invoicing.

## Purpose

This module connects academic domain (students, registrations, enrollments, service requests) with treasury domain (debt accounts, payments, invoices). Key functions include:

- **Tuition Calculation**: Calculate tuition fees based on degree type, enrollment, and academic factors
- **Automatic Debt Generation**: Create debt entries for tuition, academic taxes, and service requests
- **Academic Service Request Invoicing**: Generate debit entries from academic service requests (e.g., diploma requests, certificate requests)
- **Academic Blocking**: Block students from academic acts due to unpaid debt
- **Payment Plans**: Manage tuition payment plans with installments
- **Reports**: Generate debt reports with academic information
- **ERP Integration**: Export tuition information to external systems

---

## YuML Domain Model

The following YuML format can be parsed programmatically:

```
[FinantialEntity]0..1--0..1>[Unit|unit]
[PersonCustomer]^-[Customer|extends]
[PersonCustomer]0..1--0..1>[Person|person]
[PersonCustomer]*--0..1>[Person|personForInactivePersonCustomer]
[AcademicTariff]^-[Tariff|extends]
[AcademicTariff]0..1--0..1>[DegreeType|degreeType]
[AcademicTariff]0..1--0..1>[Degree|degree]
[AcademicTariff]*--0..1>[Degree|associatedDegrees]
[AcademicTariff]*--*>[Unit|units]
[ServiceRequestMapEntry]1--1>[Product|product]
[ServiceRequestMapEntry]1--1>[ServiceRequestType|serviceRequestType]
[AcademicTax]1--0..1>[Product|product]
[TuitionPaymentPlanGroup]*--0..1>[Product|currentProduct]
[TuitionPaymentPlan]1--1>[TuitionPaymentPlanGroup|group]
[TuitionPaymentPlan]1--1>[FinantialEntity|finantialEntity]
[TuitionPaymentPlan]1--1>[ExecutionYear|executionYear]
[TuitionPaymentPlan]0..1--0..1>[DegreeCurricularPlan|degreeCurricularPlan]
[TuitionPaymentPlan]0..1--0..1>[RegistrationProtocol|registrationProtocol]
[TuitionPaymentPlan]0..1--0..1>[CurricularYear|curricularYear]
[TuitionPaymentPlan]0..1--0..1>[IngressionType|ingression]
[TuitionPaymentPlan]0..1--0..1>[StatuteType|statuteType]
[TuitionPaymentPlan]0..1--0..1>[DebtAccount|payorDebtAccount]
[TuitionPaymentPlan]0..1-->[TuitionPaymentPlanOrder|order]
[TuitionPaymentPlan]0..1-->[TuitionPaymentPlan|copyFromTuitionPaymentPlan]
[TuitionPaymentPlanCalculator]1--0..1>[TuitionPaymentPlan|paymentPlan]
[TuitionCalculatorAggregator]*--*>[TuitionPaymentPlanCalculator|child]
[TuitionInstallmentTariff]^-[Tariff|extends]
[TuitionInstallmentTariff]1--1>[TuitionPaymentPlan|paymentPlan]
[TuitionInstallmentTariff]0..1--0..1>[DebtAccount|payorDebtAccount]
[TuitionInstallmentTariff]0..1--0..1>[TuitionPaymentPlanCalculator|calculator]
[TuitionConditionRule]0..1--0..1>[TuitionPaymentPlan|paymentPlan]
[TuitionConditionRule]0..1--0..1>[TuitionPaymentPlanCalculator|calculator]
[RegistrationRegimeTypeConditionRule]*--*>[RegistrationProtocol|registrationProtocol]
[IngressionTypeConditionRule]*--*>[IngressionType|ingression]
[CurricularYearConditionRule]*--*>[CurricularYear|curricularYear]
[ExecutionIntervalConditionRule]*--*>[ExecutionInterval|executionInterval]
[StatuteTypeConditionRule]*--*>[StatuteType|statuteType]
[RegistrationYearConditionRule]*--*>[ExecutionInterval|executionIntervals]
[TuitionPaymentPlanRecalculation]1--1>[Product|product]
[TuitionPaymentPlanRecalculation]0..1--0..1>[TuitionPaymentPlan|paymentPlan]
[AcademicTreasuryEvent]^-[TreasuryEvent|extends]
[AcademicTreasuryEvent]1--1>[Person|person]
[AcademicTreasuryEvent]0..1--0..1>[AcademicServiceRequest|serviceRequest]
[AcademicTreasuryEvent]0..1--0..1>[Registration|registration]
[AcademicTreasuryEvent]0..1--0..1>[ExecutionYear|executionYear]
[AcademicTreasuryEvent]0..1--0..1>[TuitionPaymentPlanGroup|tuitionPaymentPlanGroup]
[AcademicTreasuryEvent]0..1--0..1>[AcademicTax|academicTax]
[AcademicTreasuryEvent]0..1--0..1>[Degree|degree]
[AcademicTreasuryEvent]0..1--0..1>[AbstractDomainObject|treasuryEventTarget]
[LegacyAcademicTreasuryEvent]^-[AcademicTreasuryEvent|extends]
[LegacyAcademicTreasuryEvent]0..1--0..1>[TreasuryExemptionType|exemptionToApplyInEventDiscountInTuitionFee]
[AcademicDebtGenerationRuleType]1--*>[AcademicDebtGenerationRule|rules]
[AcademicDebtGenerationRule]1--1>[AcademicDebtGenerationRuleType|type]
[AcademicDebtGenerationRule]1--1>[ExecutionYear|executionYear]
[AcademicDebtGenerationRule]1--1>[FinantialEntity|finantialEntity]
[AcademicDebtGenerationRule]*--*>[DegreeCurricularPlan|degreeCurricularPlans]
[AcademicDebtGenerationRule]0..1--0..1>[AcademicDebtGenerationRule|copyFromAcademicDebtGenerationRule]
[AcademicDebtGenerationRuleEntry]1--1>[AcademicDebtGenerationRule|rule]
[AcademicDebtGenerationRuleEntry]0..1--0..1>[Product|product]
[AcademicDebtGenerationRuleRestriction]1--1>[AcademicDebtGenerationRule|rule]
[EnrolmentRenewalRestriction]^-[AcademicDebtGenerationRuleRestriction|extends]
[DebtsWithNoPaymentCodeReferencesRestriction]^-[AcademicDebtGenerationRuleRestriction|extends]
[FirstTimeFirstYearRestriction]^-[AcademicDebtGenerationRuleRestriction|extends]
[AcademicActBlockingSuspension]1--1>[Person|person]
[CourseFunctionCost]1--1>[ExecutionYear|executionYear]
[CourseFunctionCost]*--*>[CompetenceCourse|competenceCourses]
[CourseFunctionCost]1--1>[DegreeCurricularPlan|degreeCurricularPlan]
[DebitEntry]*--0..1>[CurricularCourse|curricularCourse]
[DebitEntry]*--0..1>[ExecutionInterval|executionSemester]
[DebitEntry]*--0..1>[EvaluationSeason|evaluationSeason]
[PendingRegistrationsForDebtCreation]*--0..1>[Registration|pendingRegistrationsForDebtCreation]
[AcademicTreasurySettings]*--0..1>[ProductGroup|emolumentsProductGroup]
[AcademicTreasurySettings]*--0..1>[ProductGroup|tuitionProductGroup]
[AcademicTreasurySettings]*--*>[Product|academicalActBlockingProducts]
[AcademicTreasurySettings]*--0..1>[AcademicTax|improvementAcademicTax]
[DebtReportRequest]1--1>[DebtReportRequestResultFile|resultFile]
[DebtReportRequest]*--0..1>[DebtReportRequestResultErrorsFile|errorsFile]
[DebtReportRequest]*--0..1>[DegreeType|degreeType]
[DebtReportRequest]*--0..1>[ExecutionYear|executionYear]
[MassiveDebtGenerationType]1--*>[MassiveDebtGenerationRequestFile|requests]
[MassiveDebtGenerationRequestFile]1--1>[MassiveDebtGenerationType|type]
[MassiveDebtGenerationRequestFile]0..1--0..1>[ExecutionYear|executionYear]
[MassiveDebtGenerationRequestFile]0..1--0..1>[TuitionPaymentPlanGroup|tuitionPaymentPlanGroup]
[MassiveDebtGenerationRequestFile]0..1--0..1>[AcademicTax|academicTax]
[MassiveDebtGenerationRequestFile]0..1--0..1>[FinantialInstitution|finantialInstitution]
[ExemptionsGenerationRequestFile]1--1>[TreasuryExemptionType|exemptionType]
[TreasuryImportType]1--*>[TreasuryImportFile|importFiles]
[ERPTuitionInfoSettings]0..1--1>[Series|series]
[ERPTuitionInfoSettings]*--*>[ExecutionYear|activeExecutionYears]
[ERPTuitionInfoProduct]1--*>[ERPTuitionInfoType|types]
[ERPTuitionInfoType]1--1>[ERPTuitionInfoSettings|settings]
[ERPTuitionInfoType]*--*>[Product|tuitionProducts]
[ERPTuitionInfoType]1--1>[ExecutionYear|executionYear]
[ERPTuitionInfoTypeAcademicEntry]*--0..1>[DegreeType|degreeType]
[ERPTuitionInfoTypeAcademicEntry]*--0..1>[Degree|degree]
[ERPTuitionInfoTypeAcademicEntry]*--0..1>[DegreeCurricularPlan|degreeCurricularPlan]
[ERPTuitionInfo]1--1>[Customer|customer]
[ERPTuitionInfo]1--1>[DocumentNumberSeries|documentNumberSeries]
[ERPTuitionInfo]1--1>[ERPTuitionInfoType|type]
[ERPTuitionInfo]0..1--0..1>[ERPTuitionInfo|first]
[ERPTuitionInfo]0..1--0..1>[ERPTuitionInfo|last]
[ERPTuitionInfo]1--*>[ERPTuitionInfoExportOperation|exportOperations]
[PaymentPenaltySettings]*--0..1>[Product|penaltyProduct]
[PaymentPenaltySettings]*--*>[Product|targetProducts]
[ReservationTax]1--1>[FinantialEntity|finantialEntity]
[ReservationTax]1--1>[Product|product]
[ReservationTax]0..1--0..1>[TreasuryExemptionType|exemptionType]
[ReservationTaxTariff]1--1>[ReservationTax|reservationTax]
[ReservationTaxTariff]1--1>[ExecutionInterval|executionInterval]
[ReservationTaxTariff]*--*>[Degree|degrees]
[ReservationTaxTariff]0..1--0..1>[InterestRateType|interestRateType]
[ReservationTaxEventTarget]1--1>[ReservationTax|reservationTax]
[ReservationTaxEventTarget]1--1>[Person|person]
```

---

## Core Entities

### Customers

| Entity             | Description                        | Key Properties                |
| ------------------ | ---------------------------------- | ----------------------------- |
| **PersonCustomer** | Customer linked to academic Person | fiscalNumber, fromPersonMerge |

### Tariffs

| Entity                                        | Description                      | Key Properties                                                                                                                                                      |
| --------------------------------------------- | -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **AcademicTariff** (extends Tariff)           | Tariff for academic services     | baseAmount, unitsForBase, unitAmount, pageAmount, maximumAmount, urgencyRate, languageTranslationRate, cycleType, academicalActBlockingOff, blockAcademicActsOnDebt |
| **TuitionInstallmentTariff** (extends Tariff) | Installment-based tuition tariff | installmentOrder, tuitionCalculationType, tuitionTariffCalculatedAmountType, fixedAmount, ectsCalculationType, factor, maximumAmount                                |

### Tuition Payment Plans

| Entity                                   | Description                    | Key Properties                                                                                                                                                |
| ---------------------------------------- | ------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **TuitionPaymentPlanGroup**              | Group of payment plans         | code, name, forRegistration, forStandalone, forExtracurricular, forImprovement, allowedConditionRulesSerialized, allowedCalculatedAmountCalculatorsSerialized |
| **TuitionPaymentPlan**                   | Specific payment plan          | paymentPlanOrder, defaultPaymentPlan, registrationRegimeType, semester, firstTimeStudent, customized, customizedName, withLaboratorialClasses                 |
| **TuitionPaymentPlanCalculator**         | Calculator for payment amounts | name                                                                                                                                                          |
| **TuitionCalculatorAggregator**          | Aggregates calculators         | minimumAmount, maximumAmount                                                                                                                                  |
| **TuitionConditionRule**                 | Base condition rule            | -                                                                                                                                                             |
| **RegistrationRegimeTypeConditionRule**  | Condition by regime type       | registrationRegimeTypesSerialized                                                                                                                             |
| **RegistrationProtocolConditionRule**    | Condition by protocol          | -                                                                                                                                                             |
| **IngressionTypeConditionRule**          | Condition by ingression type   | -                                                                                                                                                             |
| **CurricularYearConditionRule**          | Condition by curricular year   | -                                                                                                                                                             |
| **ExecutionIntervalConditionRule**       | Condition by semester          | -                                                                                                                                                             |
| **StatuteTypeConditionRule**             | Condition by statute type      | -                                                                                                                                                             |
| **FirstTimeStudentConditionRule**        | First-time student condition   | firstTimeStudent                                                                                                                                              |
| **WithLaboratorialClassesConditionRule** | Lab classes condition          | withLaboratorialClasses                                                                                                                                       |
| **RegistrationYearConditionRule**        | Registration year condition    | -                                                                                                                                                             |
| **TuitionPaymentPlanRecalculation**      | Recalculation of tuition       | recalculationDueDate                                                                                                                                          |

### Emoluments & Academic Taxes

| Entity                     | Description                       | Key Properties                                                                                                    |
| -------------------------- | --------------------------------- | ----------------------------------------------------------------------------------------------------------------- |
| **ServiceRequestMapEntry** | Maps service requests to products | createEventOnSituation, generatePaymentCode, debitEntryDescriptionExtensionFormat                                 |
| **AcademicTax**            | Academic tax definition           | appliedOnRegistration, appliedOnRegistrationFirstYear, appliedOnRegistrationSubsequentYears, appliedAutomatically |

### Events

| Entity                                                          | Description              | Key Properties                                                                                                                                                                                                |
| --------------------------------------------------------------- | ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **AcademicTreasuryEvent** (extends TreasuryEvent)               | Academic financial event | baseAmount, amountForAdditionalUnits, amountForPages, maximumAmount, customAcademicDebt, customAcademicDebtEventDate, customAcademicDebtNumberOfUnits, customAcademicDebtNumberOfPages, academicProcessNumber |
| **LegacyAcademicTreasuryEvent** (extends AcademicTreasuryEvent) | Legacy academic event    | legacyEventAccountedAsTuition, legacyEventDiscountInTuitionFee                                                                                                                                                |

### Debt Generation

| Entity                                          | Description                                 | Key Properties                                                                                                                                     |
| ----------------------------------------------- | ------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| **AcademicDebtGenerationRuleType**              | Type of debt generation rule                | code, name, strategyImplementation, orderNumber                                                                                                    |
| **AcademicDebtGenerationRule**                  | Rule for automatic debt creation            | active, backgroundExecution, aggregateOnDebitNote, closeDebitNote, days, orderNumber, academicTaxDueDateAlignmentType, minimumAmountForPaymentCode |
| **AcademicDebtGenerationRuleEntry**             | Entry in debt generation rule               | createDebt, toCreateAfterLastRegistrationStateDate, forceCreation, limitToRegisteredOnExecutionYear                                                |
| **AcademicDebtGenerationRuleRestriction**       | Restriction for debt generation             | -                                                                                                                                                  |
| **EnrolmentRenewalRestriction**                 | Enrolment renewal restriction               | Inherits AcademicDebtGenerationRuleRestriction                                                                                                     |
| **DebtsWithNoPaymentCodeReferencesRestriction** | Restriction for debts without payment codes | Inherits AcademicDebtGenerationRuleRestriction                                                                                                     |
| **FirstTimeFirstYearRestriction**               | First-time first-year restriction           | Inherits AcademicDebtGenerationRuleRestriction                                                                                                     |

### Academic Acts

| Entity                            | Description                     | Key Properties             |
| --------------------------------- | ------------------------------- | -------------------------- |
| **AcademicActBlockingSuspension** | Suspension of academic blocking | reason, beginDate, endDate |

### Course Function Cost

| Entity                 | Description              | Key Properties |
| ---------------------- | ------------------------ | -------------- |
| **CourseFunctionCost** | Cost per course function | functionCost   |

### Settings

| Entity                       | Description                       | Key Properties                                                                                                                                                      |
| ---------------------------- | --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **AcademicTreasurySettings** | Global academic treasury settings | closeServiceRequestEmolumentsWithDebitNote, runAcademicDebtGenerationRuleOnNormalEnrolment, debtGenerationRulesPeriodicExecutionActive, useCustomAcademicDebtFormat |

### Reports

| Entity                                | Description              | Key Properties                                                                                                         |
| ------------------------------------- | ------------------------ | ---------------------------------------------------------------------------------------------------------------------- |
| **DebtReportRequest**                 | Request for debt reports | type, beginDate, endDate, decimalSeparator, includeAnnuledEntries, includeExtraAcademicInfo, includeErpIntegrationInfo |
| **DebtReportRequestResultFile**       | Result file of report    | fileId, fileDescriptorId, creationDate, creator                                                                        |
| **DebtReportRequestResultErrorsFile** | Errors file of report    | fileId, fileDescriptorId, creationDate, creator                                                                        |

### Debt Generation Requests

| Entity                               | Description                      | Key Properties                                                          |
| ------------------------------------ | -------------------------------- | ----------------------------------------------------------------------- |
| **MassiveDebtGenerationType**        | Type of massive debt generation  | name, implementationClass, active                                       |
| **MassiveDebtGenerationRequestFile** | File for massive debt generation | fileId, fileDescriptorId, debtDate, whenProcessed, reason, creationDate |

### Exemptions

| Entity                              | Description                    | Key Properties                                                  |
| ----------------------------------- | ------------------------------ | --------------------------------------------------------------- |
| **ExemptionsGenerationRequestFile** | File for exemptions generation | fileId, fileDescriptorId, debtDate, whenProcessed, creationDate |

### Import

| Entity                 | Description    | Key Properties                                        |
| ---------------------- | -------------- | ----------------------------------------------------- |
| **TreasuryImportType** | Type of import | name, clazz                                           |
| **TreasuryImportFile** | Import file    | fileId, fileDescriptorId, whenProcessed, creationDate |

### ERP Tuition Info

| Entity                               | Description                 | Key Properties                                                                                               |
| ------------------------------------ | --------------------------- | ------------------------------------------------------------------------------------------------------------ |
| **ERPTuitionInfoSettings**           | ERP tuition export settings | exporterClassName, exportationActive                                                                         |
| **ERPTuitionInfoProduct**            | Product for ERP export      | code, name                                                                                                   |
| **ERPTuitionInfoType**               | Type of ERP tuition info    | active                                                                                                       |
| **ERPTuitionInfoTypeAcademicEntry**  | Academic entry for ERP      | forRegistration, forStandalone, forExtracurricular                                                           |
| **ERPTuitionInfo**                   | ERP tuition info record     | creationDate, documentNumber, tuitionTotalAmount, tuitionDeltaAmount, beginDate, endDate, exportationSuccess |
| **ERPTuitionInfoExportOperation**    | Export operation            | Inherits IntegrationOperation                                                                                |
| **ERPTuitionInfoCreationReportFile** | Creation report file        | fileId, fileDescriptorId, creationDate                                                                       |

### Payment Penalty (Deprecated)

| Entity                     | Description                           | Key Properties                                  |
| -------------------------- | ------------------------------------- | ----------------------------------------------- |
| **PaymentPenaltySettings** | Payment penalty settings (deprecated) | active, emolumentDescription, createPaymentCode |

### Reservation Tax

| Entity                        | Description                      | Key Properties                                                                                                                           |
| ----------------------------- | -------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| **ReservationTax**            | Reservation tax definition       | code, name, discountInTuitionFee, active, createPaymentReferenceCode, taxReservationDescription                                          |
| **ReservationTaxTariff**      | Tariff for reservation tax       | baseAmount, dueDateCalculationType, numberOfDaysAfterCreationForDueDate, fixedDueDate, applyInterests, interestType, interestFixedAmount |
| **ReservationTaxEventTarget** | Event target for reservation tax | taxReservationDate, taxReservationDescription                                                                                            |

---

## Enumerations

| Enum                                              | Values                                                     |
| ------------------------------------------------- | ---------------------------------------------------------- |
| **AcademicServiceRequestSituationType**           | Situation types for service requests                       |
| **RegistrationRegimeType**                        | FULL_TIME, PART_TIME                                       |
| **TuitionCalculationType**                        | FIXED_AMOUNT, PER_ECTS, PER_UNIT                           |
| **EctsCalculationType**                           | PER_ECTS, TOTAL_ECTS                                       |
| **DebtReportRequestType**                         | Types of debt reports                                      |
| **AcademicTaxDueDateAlignmentType**               | Alignment types for tax due dates                          |
| **AcademicDebtEntriesAggregationInDebitNoteType** | Aggregation types                                          |
| **CycleType**                                     | Degree cycle types: FIRST_CYCLE, SECOND_CYCLE, THIRD_CYCLE |
| **TuitionTariffCalculatedAmountType**             | Calculated amount types                                    |

---

## Package Structure

```
org.fenixedu.academictreasury.domain/
├── customer/                 # PersonCustomer
├── tariff/                  # AcademicTariff
├── emoluments/              # ServiceRequestMapEntry, AcademicTax
├── tuition/
│   ├── calculators/         # TuitionPaymentPlanCalculator
│   ├── conditionRule/       # TuitionConditionRule subclasses
│   └── ...
├── event/                   # AcademicTreasuryEvent, LegacyAcademicTreasuryEvent
├── debtGeneration/
│   ├── requests/            # MassiveDebtGenerationRequestFile
│   └── ...
├── academicalAct/           # AcademicActBlockingSuspension
├── coursefunctioncost/     # CourseFunctionCost
├── settings/               # AcademicTreasurySettings
├── reports/                # DebtReportRequest
├── importation/            # TreasuryImportFile
├── integration/
│   └── tuitioninfo/         # ERPTuitionInfo
├── paymentpenalty/         # PaymentPenaltySettings (deprecated)
└── reservationtax/          # ReservationTax, ReservationTaxTariff
```

---

## Usage Examples

### Creating a Tuition Debt Entry

```java
AcademicTreasuryEvent event = AcademicTreasuryEvent.findOrCreate(person, executionYear, tuitionPaymentPlanGroup);
event.createDebitEntry(finantialEntity, product, new BigDecimal("500.00"), "Tuition Fee");
```

### Applying an Academic Exemption

```java
LegacyAcademicTreasuryEvent event = ...;
TreasuryExemptionType exemptionType = ...;
event.applyExemption(exemptionType, new BigDecimal("100.00"));
```

### Creating a Tuition Payment Plan

```java
TuitionPaymentPlan plan = TuitionPaymentPlan.create(
    finantialEntity, executionYear, degreeCurricularPlan, 
    product, registrationRegimeType, firstTimeStudent);
```

### Running Debt Generation Rule

```java
AcademicDebtGenerationRule rule = ...;
rule.executeForExecutionYear(executionYear);
```

### Generating a Debt Report

```java
DebtReportRequest request = DebtReportRequest.create(
    type, beginDate, endDate, executionYear, degreeType);
request.processReport();
```

---

## Key Concepts (from Portuguese Documentation)

- **Debt Account**: Links student to financial institution
- **Tuition Payment Plan**: Tuition payment plan with installments
- **Debit Note**: Debt document created from academic events
- **Emolument**: Fees for academic services (diplomas, certificates)
- **Academic Tax**: Annual academic tax for enrollment
- **Reservation Tax**: Reservation tax to guarantee enrollment
- **Interests**: Late payment interest
- **Academic Treasury Event**: Academic treasury event linking debt to academic processes
- **Blocking Academical Acts**: Blocking of academic acts due to unpaid debt

---

## Dependencies

This module depends on:

- **fenixedu-treasury-base**: Core treasury functionality
- **fenixedu-academic**: Academic domain (students, registrations, service requests)
- **fenixedu-bennu**: Framework core

## Integration

This module extends the base Treasury module with:

- Academic-specific debt generation rules
- Tuition calculation and payment plans
- Service request invoicing
- Academic act blocking management
- ERP tuition info export