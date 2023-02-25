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
package org.fenixedu.academictreasury.dto.debtGeneration;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionInterval;
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

    private ExecutionInterval executionYear;
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
    private int numberOfDaysToDueDate = 0;

    private List<TreasuryTupleDataSourceBean> executionYearDataSource = new ArrayList<>();
    private List<TreasuryTupleDataSourceBean> productDataSource = new ArrayList<>();

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

    public AcademicDebtGenerationRuleBean(final AcademicDebtGenerationRuleType type, final ExecutionInterval executionYear) {
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

        IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        this.productDataSource = availableProducts.stream().sorted(Product.COMPARE_BY_NAME)
                .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(),
                        String.format("%s [%s]", l.getName().getContent(), l.getCode())))
                .collect(Collectors.toList());

        this.degreeTypeDataSource = DegreeType.all().map(
                l -> new TreasuryTupleDataSourceBean(l.getExternalId(), academicTreasuryServices.localizedNameOfDegreeType(l)))
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

        this.degreeTypeDataSource =
                DegreeType.all().map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getName().getContent()))
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
                ExecutionDegree.getAllByExecutionYearAndDegreeType(getExecutionYear().getExecutionYear(), getDegreeType()).stream()
                        .map(e -> e.getDegreeCurricularPlan())
                        .map((dcp) -> new TreasuryTupleDataSourceBean(dcp.getExternalId(),
                                "[" + dcp.getDegree().getCode() + "] " + dcp.getPresentationName(getExecutionYear().getExecutionYear())))
                        .collect(Collectors.toList());

        degreeCurricularPlanDataSource =
                result.stream().sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT).collect(Collectors.toList());
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

    public ExecutionInterval getExecutionYear() {
        return executionYear;
    }

    public void setExecutionYear(ExecutionInterval executionYear) {
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

    public List<DegreeCurricularPlan> getDegreeCurricularPlans() {
        return degreeCurricularPlans;
    }

    public List<TreasuryTupleDataSourceBean> getExecutionYearDataSource() {
        return executionYearDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getProductDataSource() {
        return productDataSource;
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

    public void setDebtGenerationRuleRestrictionDataSource(
            List<TreasuryTupleDataSourceBean> debtGenerationRuleRestrictionDataSource) {
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
