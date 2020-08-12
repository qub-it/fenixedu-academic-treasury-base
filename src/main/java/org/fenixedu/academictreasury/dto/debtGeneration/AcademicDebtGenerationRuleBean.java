package org.fenixedu.academictreasury.dto.debtGeneration;


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtGenerationRule;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicDebtGenerationRuleType;
import org.fenixedu.academictreasury.domain.debtGeneration.AcademicTaxDueDateAlignmentType;
import org.fenixedu.academictreasury.domain.debtGeneration.DebtGenerationRuleRestriction;
import org.fenixedu.academictreasury.domain.debtGeneration.IAcademicDebtGenerationRuleStrategy;
import org.fenixedu.academictreasury.domain.emoluments.AcademicTax;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;

import com.google.common.collect.Lists;

public class AcademicDebtGenerationRuleBean implements Serializable, ITreasuryBean {

    private static final long serialVersionUID = 1L;

    public static class ProductEntry implements ITreasuryBean, Serializable {

        private static final long serialVersionUID = 1L;

        private Product product;
        private boolean createDebt;
        private boolean toCreateAfterLastRegistrationStateDate;
        private boolean forceCreation;
        private boolean limitToRegisteredOnExecutionYear;

        public ProductEntry(Product product, boolean createDebt, boolean toCreateAfterLastRegistrationStateDate,
                boolean forceCreation, boolean limitToRegisteredOnExecutionYear) {
            this.product = product;
            this.createDebt = createDebt;
            this.toCreateAfterLastRegistrationStateDate = toCreateAfterLastRegistrationStateDate;
            this.forceCreation = forceCreation;
            this.limitToRegisteredOnExecutionYear = forceCreation && limitToRegisteredOnExecutionYear;
        }

        public Product getProduct() {
            return product;
        }

        public boolean isCreateDebt() {
            return createDebt;
        }

        public boolean isToCreateAfterLastRegistrationStateDate() {
            return toCreateAfterLastRegistrationStateDate;
        }

        public boolean isForceCreation() {
            return forceCreation;
        }

        public boolean isLimitToRegisteredOnExecutionYear() {
            return limitToRegisteredOnExecutionYear;
        }

    }

    private AcademicDebtGenerationRuleType type;

    private ExecutionYear executionYear;
    private boolean aggregateOnDebitNote;
    private boolean aggregateAllOrNothing;
    private boolean eventDebitEntriesMustEqualRuleProducts;
    private AcademicTaxDueDateAlignmentType academicTaxDueDateAlignmentType;
    private DebtGenerationRuleRestriction debtGenerationRuleRestriction;

    private List<ProductEntry> entries = new ArrayList<>();

    private DegreeType degreeType;

    private List<DegreeCurricularPlan> degreeCurricularPlans = new ArrayList<>();
    private List<DegreeCurricularPlan> degreeCurricularPlansToAdd = new ArrayList<>();

    private Product product;
    private boolean createDebt;
    private boolean toCreateAfterLastRegistrationStateDate;
    private boolean forceCreation;
    private boolean limitToRegisteredOnExecutionYear;
    private DigitalPaymentPlatform digitalPaymentPlatform;

    private int numberOfDaysToDueDate = 0;

    private List<TreasuryTupleDataSourceBean> executionYearDataSource = new ArrayList<>();
    private List<TreasuryTupleDataSourceBean> productDataSource = new ArrayList<>();
    private List<TreasuryTupleDataSourceBean> digitalPaymentPlatformDataSource = new ArrayList<>();

    private List<TreasuryTupleDataSourceBean> degreeTypeDataSource = new ArrayList<>();
    private List<TreasuryTupleDataSourceBean> degreeCurricularPlanDataSource = new ArrayList<>();
    private List<TreasuryTupleDataSourceBean> academicTaxDueDateAlignmentTypeDataSource = new ArrayList<>();
    private List<TreasuryTupleDataSourceBean> debtGenerationRuleRestrictionDataSource = new ArrayList<>();

    private boolean toAggregateDebitEntries;
    private boolean toCloseDebitNote;
    private boolean toCreatePaymentReferenceCodes;
    private boolean toCreateDebitEntries;
    private boolean toAlignAcademicTaxesDueDate;

    private boolean appliedMinimumAmountForPaymentCode;
    private BigDecimal minimumAmountForPaymentCode;
    
    public AcademicDebtGenerationRuleBean(final AcademicDebtGenerationRuleType type, final ExecutionYear executionYear) {
        this.type = type;
        this.executionYear = executionYear;

        executionYearDataSource = ExecutionYear.readNotClosedExecutionYears().stream()
                .sorted(Collections.reverseOrder(ExecutionYear.COMPARATOR_BY_BEGIN_DATE))
                .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getQualifiedName())).collect(Collectors.toList());

        final List<Product> availableProducts = new ArrayList<>();

        final IAcademicDebtGenerationRuleStrategy strategyImplementation = getType().strategyImplementation();
        if (strategyImplementation.isAppliedOnAcademicTaxDebitEntries()) {
            availableProducts.addAll(AcademicTax.findAll().filter(AcademicTax::isAppliedAutomatically).map(l -> l.getProduct())
                    .collect(Collectors.toList()));
        }

        if (strategyImplementation.isAppliedOnTuitionDebitEntries()) {
            availableProducts.addAll(AcademicTreasurySettings.getInstance().getTuitionProductGroup().getProductsSet());
        }

        if (strategyImplementation.isAppliedOnOtherDebitEntries()) {
            availableProducts.add(TreasurySettings.getInstance().getInterestProduct());
        }

        IAcademicTreasuryPlatformDependentServices academicTreasuryServices = AcademicTreasuryPlataformDependentServicesFactory.implementation();
        
        this.productDataSource = availableProducts.stream().sorted(Product.COMPARE_BY_NAME)
                .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), String.format("%s [%s]", l.getName().getContent(), l.getCode()))).collect(Collectors.toList());

        this.digitalPaymentPlatformDataSource = DigitalPaymentPlatform.findAll().filter(DigitalPaymentPlatform::isActive)
                    .filter(DigitalPaymentPlatform::isSibsPaymentCodeServiceSupported)
                    .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getName()))
                    .collect(Collectors.toList());

        this.degreeTypeDataSource = DegreeType.all().map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), academicTreasuryServices.localizedNameOfDegreeType(l)))
                .sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT).collect(Collectors.toList());

        this.academicTaxDueDateAlignmentTypeDataSource = Lists.newArrayList(AcademicTaxDueDateAlignmentType.values()).stream()
                .map(l -> new TreasuryTupleDataSourceBean(l.name(), l.getDescriptionI18N().getContent()))
                .sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT).collect(Collectors.toList());

        this.academicTaxDueDateAlignmentTypeDataSource.add(0, AcademicTreasuryConstants.SELECT_OPTION);

        this.aggregateOnDebitNote = true;
        this.aggregateAllOrNothing = true;
        this.eventDebitEntriesMustEqualRuleProducts = false;

        this.toAggregateDebitEntries = type.strategyImplementation().isToAggregateDebitEntries();
        this.toCloseDebitNote = type.strategyImplementation().isToCloseDebitNote();
        this.toCreatePaymentReferenceCodes = type.strategyImplementation().isToCreatePaymentReferenceCodes();
        this.toCreateDebitEntries = type.strategyImplementation().isToCreateDebitEntries();
        this.toAlignAcademicTaxesDueDate = type.strategyImplementation().isToAlignAcademicTaxesDueDate();

        this.debtGenerationRuleRestrictionDataSource =
                DebtGenerationRuleRestriction.findAll().map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getName()))
                        .sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT).collect(Collectors.toList());

        this.debtGenerationRuleRestrictionDataSource.add(0, AcademicTreasuryConstants.SELECT_OPTION);

        this.appliedMinimumAmountForPaymentCode = false;
        this.minimumAmountForPaymentCode = null;
        
        this.forceCreation = false;
        this.limitToRegisteredOnExecutionYear = true;
    }

    public AcademicDebtGenerationRuleBean(final AcademicDebtGenerationRule rule) {
        this.type = rule.getAcademicDebtGenerationRuleType();
        this.executionYear = rule.getExecutionYear();

        this.executionYearDataSource = ExecutionYear.readNotClosedExecutionYears().stream()
                .sorted(Collections.reverseOrder(ExecutionYear.COMPARATOR_BY_BEGIN_DATE))
                .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getQualifiedName())).collect(Collectors.toList());

        this.degreeTypeDataSource = DegreeType.all().map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getName().getContent()))
                .sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT).collect(Collectors.toList());

        this.aggregateOnDebitNote = rule.isAggregateOnDebitNote();
        this.aggregateAllOrNothing = rule.isAggregateAllOrNothing();
        this.eventDebitEntriesMustEqualRuleProducts = rule.isEventDebitEntriesMustEqualRuleProducts();

        this.degreeCurricularPlans.addAll(rule.getDegreeCurricularPlansSet());

        Collections.sort(this.degreeCurricularPlans,
                DegreeCurricularPlan.DEGREE_CURRICULAR_PLAN_COMPARATOR_BY_DEGREE_TYPE_AND_EXECUTION_DEGREE_AND_DEGREE_CODE);

        this.appliedMinimumAmountForPaymentCode = rule.isAppliedMinimumAmountForPaymentCode();
        this.minimumAmountForPaymentCode = rule.getMinimumAmountForPaymentCode();
    }

    public void chooseDegreeType() {
        if (getExecutionYear() == null) {
            degreeCurricularPlanDataSource = Collections.<TreasuryTupleDataSourceBean> emptyList();
            return;
        }

        if (getDegreeType() == null) {
            degreeCurricularPlanDataSource = Collections.<TreasuryTupleDataSourceBean> emptyList();
            return;
        }

        final List<TreasuryTupleDataSourceBean> result =
                ExecutionDegree.getAllByExecutionYearAndDegreeType(getExecutionYear(), getDegreeType()).stream()
                        .map(e -> e.getDegreeCurricularPlan())
                        .map((dcp) -> new TreasuryTupleDataSourceBean(dcp.getExternalId(),
                                "[" + dcp.getDegree().getCode() + "] " + dcp.getPresentationName(getExecutionYear())))
                .collect(Collectors.toList());

        degreeCurricularPlanDataSource = result.stream().sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT).collect(Collectors.toList());
    }

    public void addEntry() {
        if (product == null) {
            return;
        }

        if (entries.stream().filter(l -> l.getProduct() == product).count() > 0) {
            return;
        }

        entries.add(new ProductEntry(this.product, isToCreateDebitEntries() && this.createDebt,
                isToCreateDebitEntries() && this.toCreateAfterLastRegistrationStateDate,
                isToCreateDebitEntries() && this.forceCreation,
                isToCreateDebitEntries() && this.limitToRegisteredOnExecutionYear));

        this.product = null;
        this.createDebt = false;
        this.forceCreation = false;
        this.limitToRegisteredOnExecutionYear = true;
    }

    public void removEntry(final int index) {
        entries.remove(index);
    }

    public void addDegreeCurricularPlans() {
        degreeCurricularPlans.addAll(degreeCurricularPlansToAdd);

        degreeCurricularPlansToAdd = new ArrayList<>();
    }

    public void removeDegreeCurricularPlan(int entryIndex) {
        degreeCurricularPlans.remove(entryIndex);
    }

    public AcademicDebtGenerationRuleType getType() {
        return type;
    }

    public void setType(AcademicDebtGenerationRuleType type) {
        this.type = type;
    }

    public boolean isToAggregateDebitEntries() {
        return toAggregateDebitEntries;
    }

    public boolean isToCloseDebitNote() {
        return toCloseDebitNote;
    }

    public boolean isToCreatePaymentReferenceCodes() {
        return toCreatePaymentReferenceCodes;
    }

    public boolean isToCreateDebitEntries() {
        return toCreateDebitEntries;
    }

    public boolean isToAlignAcademicTaxesDueDate() {
        return toAlignAcademicTaxesDueDate;
    }

    public boolean isAggregateOnDebitNote() {
        return isToAggregateDebitEntries() && aggregateOnDebitNote;
    }

    public void setAggregateOnDebitNote(boolean aggregateOnDebitNote) {
        this.aggregateOnDebitNote = aggregateOnDebitNote;
    }

    public AcademicTaxDueDateAlignmentType getAcademicTaxDueDateAlignmentType() {
        return academicTaxDueDateAlignmentType;
    }

    public void setAcademicTaxDueDateAlignmentType(AcademicTaxDueDateAlignmentType academicTaxDueDateAlignmentType) {
        this.academicTaxDueDateAlignmentType = academicTaxDueDateAlignmentType;
    }

    public List<ProductEntry> getEntries() {
        return entries;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public boolean isCreateDebt() {
        return isToCreateDebitEntries() && createDebt;
    }

    public void setCreateDebt(boolean createDebt) {
        this.createDebt = createDebt;
    }

    public boolean isToCreateAfterLastRegistrationStateDate() {
        return isToCreateDebitEntries() && toCreateAfterLastRegistrationStateDate;
    }

    public void setToCreateAfterLastRegistrationStateDate(boolean toCreateAfterLastRegistrationStateDate) {
        this.toCreateAfterLastRegistrationStateDate = toCreateAfterLastRegistrationStateDate;
    }

    public ExecutionYear getExecutionYear() {
        return executionYear;
    }

    public void setExecutionYear(ExecutionYear executionYear) {
        this.executionYear = executionYear;
    }

    public DegreeType getDegreeType() {
        return degreeType;
    }

    public void setDegreeType(DegreeType degreeType) {
        this.degreeType = degreeType;
    }

    public boolean isAggregateAllOrNothing() {
        return isToAggregateDebitEntries() && aggregateAllOrNothing;
    }

    public void setAggregateAllOrNothing(boolean aggregateAllOrNothing) {
        this.aggregateAllOrNothing = aggregateAllOrNothing;
    }
    
    public boolean isEventDebitEntriesMustEqualRuleProducts() {
        return eventDebitEntriesMustEqualRuleProducts;
    }
    
    public void setEventDebitEntriesMustEqualRuleProducts(boolean eventDebitEntriesMustEqualRuleProducts) {
        this.eventDebitEntriesMustEqualRuleProducts = eventDebitEntriesMustEqualRuleProducts;
    }

    public DigitalPaymentPlatform getDigitalPaymentPlatform() {
        return digitalPaymentPlatform;
    }

    public void setDigitalPaymentPlatform(DigitalPaymentPlatform digitalPaymentPlatform) {
        this.digitalPaymentPlatform = digitalPaymentPlatform;
    }

    public List<DegreeCurricularPlan> getDegreeCurricularPlans() {
        return degreeCurricularPlans;
    }

    public List<TreasuryTupleDataSourceBean> getExecutionYearDataSource() {
        return executionYearDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getProductDataSource() {
        return productDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getDigitalPaymentPlatformDataSource() {
        return digitalPaymentPlatformDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getDegreeTypeDataSource() {
        return degreeTypeDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getDegreeCurricularPlanDataSource() {
        return degreeCurricularPlanDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getAcademicTaxDueDateAlignmentTypeDataSource() {
        return academicTaxDueDateAlignmentTypeDataSource;
    }
    
    public List<TreasuryTupleDataSourceBean> getDebtGenerationRuleRestrictionDataSource() {
        return debtGenerationRuleRestrictionDataSource;
    }
    
    public void setDebtGenerationRuleRestrictionDataSource(List<TreasuryTupleDataSourceBean> debtGenerationRuleRestrictionDataSource) {
        this.debtGenerationRuleRestrictionDataSource = debtGenerationRuleRestrictionDataSource;
    }

    public boolean isForceCreation() {
        return isToCreateDebitEntries() && forceCreation;
    }

    public void setForceCreation(boolean forceCreation) {
        this.forceCreation = forceCreation;
    }

    public boolean isLimitToRegisteredOnExecutionYear() {
        return isToCreateDebitEntries() && limitToRegisteredOnExecutionYear;
    }

    public void setLimitToRegisteredOnExecutionYear(boolean limitToRegisteredOnExecutionYear) {
        this.limitToRegisteredOnExecutionYear = limitToRegisteredOnExecutionYear;
    }

    public int getNumberOfDaysToDueDate() {
        return numberOfDaysToDueDate;
    }

    public void setNumberOfDaysToDueDate(int numberOfDaysToDueDate) {
        this.numberOfDaysToDueDate = numberOfDaysToDueDate;
    }

    public DebtGenerationRuleRestriction getDebtGenerationRuleRestriction() {
        return debtGenerationRuleRestriction;
    }

    public void setDebtGenerationRuleRestriction(DebtGenerationRuleRestriction debtGenerationRuleRestriction) {
        this.debtGenerationRuleRestriction = debtGenerationRuleRestriction;
    }

    public boolean isAppliedMinimumAmountForPaymentCode() {
        return appliedMinimumAmountForPaymentCode;
    }
    
    public void setAppliedMinimumAmountForPaymentCode(boolean appliedMinimumAmountForPaymentCode) {
        this.appliedMinimumAmountForPaymentCode = appliedMinimumAmountForPaymentCode;
    }
    
    public BigDecimal getMinimumAmountForPaymentCode() {
        return minimumAmountForPaymentCode;
    }
    
    public void setMinimumAmountForPaymentCode(BigDecimal minimumAmountForPaymentCode) {
        this.minimumAmountForPaymentCode = minimumAmountForPaymentCode;
    }
    
}
