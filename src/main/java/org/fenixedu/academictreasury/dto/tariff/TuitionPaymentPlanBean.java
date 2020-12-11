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
package org.fenixedu.academictreasury.dto.tariff;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.domain.tuition.EctsCalculationType;
import org.fenixedu.academictreasury.domain.tuition.TuitionCalculationType;
import org.fenixedu.academictreasury.domain.tuition.TuitionInstallmentTariff;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.domain.tariff.InterestType;
import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class TuitionPaymentPlanBean implements Serializable, ITreasuryBean {

    private static final long serialVersionUID = 1L;

    private FinantialEntity finantialEntity;

    private Product product;
    private TuitionPaymentPlanGroup tuitionPaymentPlanGroup;
    private ExecutionYear executionYear;

    private DegreeType degreeType;
    private Set<DegreeCurricularPlan> degreeCurricularPlans = Sets.newHashSet();

    private boolean showAllDcps;
    private boolean defaultPaymentPlan;
    private RegistrationRegimeType registrationRegimeType;
    private RegistrationProtocol registrationProtocol;
    private IngressionType ingression;
    private CurricularYear curricularYear;
    private ExecutionInterval executionSemester;
    private boolean firstTimeStudent;
    private boolean customized;
    private StatuteType statuteType;
    private DebtAccount payorDebtAccount;

    // TODO: Anil Use LocalizedString when web component is compatible with AngularJS
    private String name;
    private boolean withLaboratorialClasses;

    private List<TreasuryTupleDataSourceBean> executionYearDataSource = null;
    private List<TreasuryTupleDataSourceBean> degreeTypeDataSource = null;
    private List<TreasuryTupleDataSourceBean> degreeCurricularPlanDataSource = null;
    private List<TreasuryTupleDataSourceBean> registrationRegimeTypeDataSource = null;
    private List<TreasuryTupleDataSourceBean> registrationProtocolDataSource = null;
    private List<TreasuryTupleDataSourceBean> ingressionDataSource = null;
    private List<TreasuryTupleDataSourceBean> curricularYearDataSource = null;
    private List<TreasuryTupleDataSourceBean> semesterDataSource = null;
    private List<TreasuryTupleDataSourceBean> statuteTypeDataSource = null;
    private List<TreasuryTupleDataSourceBean> payorDebtAccountDataSource = null;

    private List<TreasuryTupleDataSourceBean> tuitionCalculationTypeDataSource = null;
    private List<TreasuryTupleDataSourceBean> ectsCalculationTypeDataSource = null;
    private List<TreasuryTupleDataSourceBean> interestTypeDataSource = null;
    private List<TreasuryTupleDataSourceBean> dueDateCalculationTypeDataSource = null;
    private List<TreasuryTupleDataSourceBean> tuitionInstallmentProductDataSource = null;

    public List<AcademicTariffBean> tuitionInstallmentBeans = Lists.newArrayList();

    // @formatter:off
    /*--------------
     * TARIFF FIELDS
     * -------------
     */
    // @formatter:on

    /* Tariff */
    private LocalDate beginDate = new LocalDate();
    private LocalDate endDate = AcademicTreasuryConstants.INFINITY_DATE.toLocalDate();
    private DueDateCalculationType dueDateCalculationType;
    private LocalDate fixedDueDate = new LocalDate();
    private int numberOfDaysAfterCreationForDueDate;

    /* InterestRate */
    private boolean applyInterests;
    private InterestType interestType;
    private int numberOfDaysAfterDueDate;
    private boolean applyInFirstWorkday;
    private int maximumDaysToApplyPenalty;
    private BigDecimal interestFixedAmount;
    private BigDecimal rate;

    /* TuitionInstallment */
    private Product tuitionInstallmentProduct;
    private int installmentOrder;
    private TuitionCalculationType tuitionCalculationType;
    private BigDecimal fixedAmount;
    private EctsCalculationType ectsCalculationType;
    private BigDecimal factor;
    private BigDecimal totalEctsOrUnits;
    private boolean applyMaximumAmount;
    private BigDecimal maximumAmount;
    private boolean academicalActBlockingOn;
    private boolean blockAcademicActsOnDebt;

    // @formatter:off
    /*---------------------
     * END OF TARIFF FIELDS
     * --------------------
     */
    // @formatter:on

    // To be used on copy tuition payment plan
    private ExecutionYear copiedExecutionYear;

    // Named in tuition importation
    private String sheetName;

    public TuitionPaymentPlanBean() {
    }

    public TuitionPaymentPlanBean(final Product product, final TuitionPaymentPlanGroup tuitionPaymentPlanGroup,
            final FinantialEntity finantialEntity, final ExecutionYear executionYear) {
        this.showAllDcps = false;
        this.product = product;
        this.tuitionPaymentPlanGroup = tuitionPaymentPlanGroup;
        this.finantialEntity = finantialEntity;
        this.executionYear = executionYear;

        updateData();
        resetInstallmentFields();
    }

    public TuitionPaymentPlanBean(TuitionPaymentPlan tuitionPaymentPlan) {
        this(tuitionPaymentPlan.getProduct(), tuitionPaymentPlan.getTuitionPaymentPlanGroup(),
                tuitionPaymentPlan.getFinantialEntity(), tuitionPaymentPlan.getExecutionYear());

        this.degreeType = tuitionPaymentPlan.getDegreeCurricularPlan().getDegreeType();

        this.copiedExecutionYear = tuitionPaymentPlan.getExecutionYear();

        this.curricularYear = tuitionPaymentPlan.getCurricularYear();
        this.customized = tuitionPaymentPlan.isCustomized();
        this.defaultPaymentPlan = tuitionPaymentPlan.isDefaultPaymentPlan();

        if (tuitionPaymentPlan.getSemester() != null) {
            this.executionSemester = getExecutionYear().getExecutionSemesterFor(tuitionPaymentPlan.getSemester());
        }

        this.firstTimeStudent = tuitionPaymentPlan.isFirstTimeStudent();
        this.ingression = tuitionPaymentPlan.getIngression();
        this.name = tuitionPaymentPlan.getCustomizedName().getContent();
        this.registrationProtocol = tuitionPaymentPlan.getRegistrationProtocol();
        this.registrationRegimeType = tuitionPaymentPlan.getRegistrationRegimeType();
        this.statuteType = tuitionPaymentPlan.getStatuteType();
        this.withLaboratorialClasses = tuitionPaymentPlan.isWithLaboratorialClasses();
        this.payorDebtAccount = tuitionPaymentPlan.getPayorDebtAccount();

        fillWithInstallments(tuitionPaymentPlan);
    }

    private void fillWithInstallments(final TuitionPaymentPlan tuitionPaymentPlan) {
        for (final TuitionInstallmentTariff tuitionInstallmentTariff : tuitionPaymentPlan.getOrderedTuitionInstallmentTariffs()) {

            this.tuitionInstallmentProduct = tuitionInstallmentTariff.getProduct();
            this.beginDate = tuitionInstallmentTariff.getBeginDate().toLocalDate();
            this.dueDateCalculationType = tuitionInstallmentTariff.getDueDateCalculationType();
            this.fixedDueDate = tuitionInstallmentTariff.getFixedDueDate();
            this.numberOfDaysAfterCreationForDueDate = tuitionInstallmentTariff.getNumberOfDaysAfterCreationForDueDate();

            this.applyInterests = tuitionInstallmentTariff.getApplyInterests();
            if (this.applyInterests) {
                this.interestType = tuitionInstallmentTariff.getInterestRate().getInterestType();
                this.numberOfDaysAfterDueDate = tuitionInstallmentTariff.getNumberOfDaysAfterCreationForDueDate();
                this.applyInFirstWorkday = tuitionInstallmentTariff.getInterestRate().isApplyInFirstWorkday();
                this.maximumDaysToApplyPenalty = tuitionInstallmentTariff.getInterestRate().getMaximumDaysToApplyPenalty();
                this.interestFixedAmount = tuitionInstallmentTariff.getInterestRate().getInterestFixedAmount();
                this.rate = tuitionInstallmentTariff.getInterestRate().getRate();
            }

            this.tuitionCalculationType = tuitionInstallmentTariff.getTuitionCalculationType();
            this.fixedAmount = tuitionInstallmentTariff.getFixedAmount();
            this.ectsCalculationType = tuitionInstallmentTariff.getEctsCalculationType();
            this.factor = tuitionInstallmentTariff.getFactor();
            this.totalEctsOrUnits = tuitionInstallmentTariff.getTotalEctsOrUnits();
            this.applyMaximumAmount = tuitionInstallmentTariff.isApplyMaximumAmount();
            this.maximumAmount = tuitionInstallmentTariff.getMaximumAmount();
            this.academicalActBlockingOn = !tuitionInstallmentTariff.getAcademicalActBlockingOff();
            this.blockAcademicActsOnDebt = tuitionInstallmentTariff.getBlockAcademicActsOnDebt();

            addInstallment();
        }
    }

    public void updateData() {
        this.degreeTypeDataSource = degreeTypeDataSource();
        this.degreeCurricularPlanDataSource = degreeCurricularPlanDataSource();

        this.degreeCurricularPlans.clear();

        this.registrationRegimeTypeDataSource = registrationRegimeTypeDataSource();
        this.registrationProtocolDataSource = registrationProtocolDataSource();
        this.ingressionDataSource = ingressionDataSource();
        this.curricularYearDataSource = curricularYearDataSource();
        this.semesterDataSource = semesterDataSource();

        this.tuitionCalculationTypeDataSource = tuitionCalculationTypeDataSource();

        this.ectsCalculationTypeDataSource = ectsCalculationTypeDataSource();

        this.interestTypeDataSource = interestTypeDataSource();

        this.dueDateCalculationTypeDataSource = dueDateCalculationTypeDataSource();

        this.tuitionInstallmentProductDataSource =
                tuitionInstallmentProductDataSource(getTuitionPaymentPlanGroup(), this.tuitionInstallmentBeans.size() + 1);

        this.statuteTypeDataSource = statuteTypeDataSource();

        this.payorDebtAccountDataSource = payorDebtAccountDataSource();

        this.executionYearDataSource = executionYearDataSource();

    }

    public void updateDatesBasedOnSelectedExecutionYear() {
        int executionYearInterval = executionYear.getAcademicInterval().getStart().getYear()
                - copiedExecutionYear.getAcademicInterval().getStart().getYear();

        for (final AcademicTariffBean academicTariffBean : tuitionInstallmentBeans) {
            academicTariffBean.setBeginDate(academicTariffBean.getBeginDate().plusYears(executionYearInterval));

            if (academicTariffBean.getFixedDueDate() != null) {
                academicTariffBean.setFixedDueDate(academicTariffBean.getFixedDueDate().plusYears(executionYearInterval));
            }
        }
    }

    public static List<TreasuryTupleDataSourceBean> dueDateCalculationTypeDataSource() {
        return ((List<DueDateCalculationType>) Arrays.asList(DueDateCalculationType.values())).stream()
                .filter(t -> !t.isNoDueDate() && !t.isFixedDate())
                .map(t -> new TreasuryTupleDataSourceBean(t.name(), t.getDescriptionI18N().getContent()))
                .collect(Collectors.toList());
    }

    public List<String> addInstallment() {

        List<String> errorMessages = Lists.newArrayList();

        final AcademicTariffBean installmentBean = new AcademicTariffBean(tuitionInstallmentBeans.size() + 1);

        if (this.tuitionInstallmentProduct == null) {
            errorMessages.add("error.TuitionPaymentPlan.tuitionInstallmentProduct.required");
        }

        if (this.tuitionCalculationType == null) {
            errorMessages.add("error.TuitionPaymentPlan.tuitionCalculationType.required");
        }

        if (this.tuitionCalculationType != null && this.tuitionCalculationType.isFixedAmount() && this.fixedAmount == null) {
            errorMessages.add("error.TuitionPaymentPlan.fixedAmount.required");
        }

        if (this.tuitionCalculationType != null && (this.tuitionCalculationType.isEcts() || this.tuitionCalculationType.isUnits())
                && this.ectsCalculationType == null) {
            errorMessages.add("error.TuitionPaymentPlan.ectsCalculationType.required");
        }

        if (this.tuitionCalculationType != null && (this.tuitionCalculationType.isEcts() || this.tuitionCalculationType.isUnits())
                && this.ectsCalculationType != null && this.ectsCalculationType.isFixedAmount() && this.fixedAmount == null) {
            errorMessages.add("error.TuitionPaymentPlan.fixedAmount.required");
        }

        if (this.tuitionCalculationType != null && (this.tuitionCalculationType.isEcts() || this.tuitionCalculationType.isUnits())
                && this.ectsCalculationType != null && this.ectsCalculationType.isDependentOnDefaultPaymentPlan()
                && this.factor == null) {
            errorMessages.add("error.TuitionPaymentPlan.factor.required");
        }

        if (this.tuitionCalculationType != null && (this.tuitionCalculationType.isEcts() || this.tuitionCalculationType.isUnits())
                && this.ectsCalculationType != null && this.ectsCalculationType.isDependentOnDefaultPaymentPlan()
                && this.totalEctsOrUnits == null) {
            errorMessages.add("error.TuitionPaymentPlan.totalEctsOrUnits.required");
        }

        if (this.applyMaximumAmount
                && (this.maximumAmount == null || !AcademicTreasuryConstants.isPositive(this.maximumAmount))) {
            errorMessages.add("error.TuitionPaymentPlan.maximumAmount.required");
        }

        if (this.beginDate == null) {
            errorMessages.add("error.TuitionPaymentPlan.beginDate.required");
        }

        if (this.dueDateCalculationType == null) {
            errorMessages.add("error.TuitionPaymentPlan.dueDateCalculationType.required");
        }

        if (this.dueDateCalculationType != null && this.dueDateCalculationType.isFixedDate() && this.fixedDueDate == null) {
            errorMessages.add("error.TuitionPaymentPlan.fixedDueDate.required");
        }

        if (this.applyInterests && this.interestType == null) {
            errorMessages.add("error.TuitionPaymentPlan.interestType.required");
        }

        if (this.applyInterests && this.interestType != null && this.interestType.isFixedAmount()
                && this.interestFixedAmount == null) {
            errorMessages.add("error.TuitionPaymentPlan.interestFixedAmount.required");
        }

        if (this.applyInterests && this.interestType != null && this.interestType.isDaily() && this.rate == null) {
            errorMessages.add("error.TuitionPaymentPlan.interestRate.required");
        }

        if (getTuitionInstallmentBeans().stream().filter(l -> l.getTuitionInstallmentProduct() == getTuitionInstallmentProduct())
                .count() > 0) {
            errorMessages.add("error.TuitionPaymentPlan.installment.already.with.product");
        }

        if (getTuitionPaymentPlanGroup().isForRegistration()
                && (getTuitionCalculationType().isEcts() || getTuitionCalculationType().isUnits())
                && getEctsCalculationType().isDefaultPaymentPlanCourseFunctionCostIndexed()) {
            errorMessages.add(
                    "error.TuitionInstallmentTariff.defaultPaymentPlanCourseFunctionCostIndexed.not.supported.for.registrationTuition");
        }

        if (!isAcademicalActBlockingOn() && isBlockAcademicActsOnDebt()) {
            errorMessages.add("error.TuitionPaymentPlanBean.cannot.suspend.and.also.block.academical.acts.on.debt");
        }

        if (!errorMessages.isEmpty()) {
            return errorMessages;
        }

        installmentBean.setBeginDate(this.beginDate);
        installmentBean.setEndDate(this.endDate);
        installmentBean.setDueDateCalculationType(dueDateCalculationType);
        installmentBean.setFixedDueDate(this.fixedDueDate);
        installmentBean.setNumberOfDaysAfterCreationForDueDate(this.numberOfDaysAfterCreationForDueDate);

        installmentBean.setApplyInterests(this.applyInterests);
        installmentBean.setInterestType(this.interestType);
        installmentBean.setNumberOfDaysAfterDueDate(this.numberOfDaysAfterDueDate);
        installmentBean.setApplyInFirstWorkday(this.applyInFirstWorkday);
        installmentBean.setMaximumDaysToApplyPenalty(this.maximumDaysToApplyPenalty);
        installmentBean.setInterestFixedAmount(this.interestFixedAmount);
        installmentBean.setRate(this.rate);

        installmentBean.setTuitionInstallmentProduct(getTuitionInstallmentProduct());
        installmentBean.setTuitionCalculationType(this.tuitionCalculationType);
        installmentBean.setFixedAmount(this.fixedAmount);
        installmentBean.setEctsCalculationType(this.ectsCalculationType);
        installmentBean.setFactor(this.factor);
        installmentBean.setTotalEctsOrUnits(this.totalEctsOrUnits);
        installmentBean.setApplyMaximumAmount(this.applyMaximumAmount);
        installmentBean.setMaximumAmount(this.maximumAmount);
        installmentBean.setAcademicalActBlockingOn(this.academicalActBlockingOn);
        installmentBean.setBlockAcademicActsOnDebt(this.blockAcademicActsOnDebt);

        this.tuitionInstallmentBeans.add(installmentBean);

        this.tuitionInstallmentProductDataSource =
                tuitionInstallmentProductDataSource(getTuitionPaymentPlanGroup(), this.tuitionInstallmentBeans.size() + 1);

        return errorMessages;
    }

    public void removeInstallment(final int installmentNumber) {
        if (findTariffBeanByInstallmentNumber(installmentNumber + 1) != null) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlan.delete.after.first");
        }

        AcademicTariffBean removeBean = findTariffBeanByInstallmentNumber(installmentNumber);

        if (removeBean != null) {
            getTuitionInstallmentBeans().remove(removeBean);

            int i = 1;
            for (AcademicTariffBean academicTariffBean : getTuitionInstallmentBeans()) {
                academicTariffBean.setInstallmentOrder(i++);
            }
        }

        this.tuitionInstallmentProductDataSource =
                tuitionInstallmentProductDataSource(getTuitionPaymentPlanGroup(), this.tuitionInstallmentBeans.size() + 1);
    }

    private AcademicTariffBean findTariffBeanByInstallmentNumber(int installmentNumber) {
        for (final AcademicTariffBean academicTariffBean : getTuitionInstallmentBeans()) {
            if (academicTariffBean.getInstallmentOrder() == installmentNumber) {
                return academicTariffBean;
            }
        }

        return null;
    }

    public void resetInstallmentFields() {
        this.beginDate = this.executionYear != null ? this.executionYear.getBeginLocalDate() : null;
        this.endDate = AcademicTreasuryConstants.INFINITY_DATE.toLocalDate();
        this.dueDateCalculationType = DueDateCalculationType.DAYS_AFTER_CREATION;
        this.fixedDueDate = this.executionYear != null ? this.executionYear.getBeginLocalDate() : null;
        this.numberOfDaysAfterCreationForDueDate = 0;

        this.applyInterests = true;
        this.interestType = InterestType.GLOBAL_RATE;
        this.numberOfDaysAfterDueDate = 1;
        this.applyInFirstWorkday = false;
        this.maximumDaysToApplyPenalty = 0;
        this.interestFixedAmount = null;
        this.rate = null;

        this.tuitionInstallmentProduct = null;
        this.tuitionCalculationType = TuitionCalculationType.FIXED_AMOUNT;
        this.fixedAmount = null;
        this.ectsCalculationType = EctsCalculationType.FIXED_AMOUNT;
        this.factor = null;
        this.totalEctsOrUnits = null;
        this.applyMaximumAmount = false;
        this.maximumAmount = null;
        this.academicalActBlockingOn = true;
        this.blockAcademicActsOnDebt = false;

        if (tuitionPaymentPlanGroup.isForExtracurricular() || tuitionPaymentPlanGroup.isForStandalone()) {
            setTuitionInstallmentProduct(tuitionPaymentPlanGroup.getCurrentProduct());
        }
    }

    public FinantialEntity getFinantialEntity() {
        return finantialEntity;
    }

    public void setFinantialEntity(FinantialEntity finantialEntity) {
        this.finantialEntity = finantialEntity;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public TuitionPaymentPlanGroup getTuitionPaymentPlanGroup() {
        return tuitionPaymentPlanGroup;
    }

    public void setTuitionPaymentPlanGroup(TuitionPaymentPlanGroup tuitionPaymentPlanGroup) {
        this.tuitionPaymentPlanGroup = tuitionPaymentPlanGroup;
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

    public Set<DegreeCurricularPlan> getDegreeCurricularPlans() {
        return degreeCurricularPlans;
    }

    public void setDegreeCurricularPlans(Set<DegreeCurricularPlan> degreeCurricularPlans) {
        this.degreeCurricularPlans = degreeCurricularPlans;
    }

    public boolean isDefaultPaymentPlan() {
        return defaultPaymentPlan;
    }

    public void setDefaultPaymentPlan(boolean defaultPaymentPlan) {
        this.defaultPaymentPlan = defaultPaymentPlan;
    }

    public RegistrationRegimeType getRegistrationRegimeType() {
        return registrationRegimeType;
    }

    public void setRegistrationRegimeType(RegistrationRegimeType registrationRegimeType) {
        this.registrationRegimeType = registrationRegimeType;
    }

    public RegistrationProtocol getRegistrationProtocol() {
        return registrationProtocol;
    }

    public void setRegistrationProtocol(RegistrationProtocol registrationProtocol) {
        this.registrationProtocol = registrationProtocol;
    }

    public IngressionType getIngression() {
        return ingression;
    }

    public void setIngression(IngressionType ingression) {
        this.ingression = ingression;
    }

    public CurricularYear getCurricularYear() {
        return curricularYear;
    }

    public void setCurricularYear(CurricularYear curricularYear) {
        this.curricularYear = curricularYear;
    }

    public ExecutionInterval getExecutionSemester() {
        return executionSemester;
    }

    public void setExecutionSemester(ExecutionInterval executionSemester) {
        this.executionSemester = executionSemester;
    }

    public boolean isFirstTimeStudent() {
        return firstTimeStudent;
    }

    public void setFirstTimeStudent(boolean firstTimeStudent) {
        this.firstTimeStudent = firstTimeStudent;
    }

    public boolean isCustomized() {
        return customized;
    }

    public void setCustomized(boolean customized) {
        this.customized = customized;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isWithLaboratorialClasses() {
        return withLaboratorialClasses;
    }

    public void setWithLaboratorialClasses(boolean withLaboratorialClasses) {
        this.withLaboratorialClasses = withLaboratorialClasses;
    }

    public List<AcademicTariffBean> getTuitionInstallmentBeans() {
        return tuitionInstallmentBeans;
    }

    public void setTuitionInstallmentBeans(final List<AcademicTariffBean> tuitionInstallmentBeans) {
        this.tuitionInstallmentBeans = tuitionInstallmentBeans;
    }

    public List<TreasuryTupleDataSourceBean> getDegreeTypeDataSource() {
        return degreeTypeDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getDegreeCurricularPlanDataSource() {
        return degreeCurricularPlanDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getRegistrationRegimeTypeDataSource() {
        return registrationRegimeTypeDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getRegistrationProtocolDataSource() {
        return registrationProtocolDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getIngressionDataSource() {
        return ingressionDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getCurricularYearDataSource() {
        return curricularYearDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getSemesterDataSource() {
        return semesterDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getTuitionCalculationTypeDataSource() {
        return tuitionCalculationTypeDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getEctsCalculationTypeDataSource() {
        return ectsCalculationTypeDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getInterestTypeDataSource() {
        return interestTypeDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getTuitionInstallmentProductDataSource() {
        return tuitionInstallmentProductDataSource;
    }

    public DebtAccount getPayorDebtAccount() {
        return payorDebtAccount;
    }

    public void setPayorDebtAccount(DebtAccount payorDebtAccount) {
        this.payorDebtAccount = payorDebtAccount;
    }

    /*
     * GETTERS & SETTERS
     */

    public LocalDate getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(LocalDate beginDate) {
        this.beginDate = beginDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public DueDateCalculationType getDueDateCalculationType() {
        return dueDateCalculationType;
    }

    public void setDueDateCalculationType(DueDateCalculationType dueDateCalculationType) {
        this.dueDateCalculationType = dueDateCalculationType;
    }

    public LocalDate getFixedDueDate() {
        return fixedDueDate;
    }

    public void setFixedDueDate(LocalDate fixedDueDate) {
        this.fixedDueDate = fixedDueDate;
    }

    public int getNumberOfDaysAfterCreationForDueDate() {
        return numberOfDaysAfterCreationForDueDate;
    }

    public void setNumberOfDaysAfterCreationForDueDate(int numberOfDaysAfterCreationForDueDate) {
        this.numberOfDaysAfterCreationForDueDate = numberOfDaysAfterCreationForDueDate;
    }

    public boolean isApplyInterests() {
        return applyInterests;
    }

    public void setApplyInterests(boolean applyInterests) {
        this.applyInterests = applyInterests;
    }

    public boolean isApplyInFirstWorkday() {
        return applyInFirstWorkday;
    }

    public void setApplyInFirstWorkday(boolean applyInFirstWorkday) {
        this.applyInFirstWorkday = applyInFirstWorkday;
    }

    /* InterestRate */

    public InterestType getInterestType() {
        return interestType;
    }

    public void setInterestType(InterestType interestType) {
        this.interestType = interestType;
    }

    public int getNumberOfDaysAfterDueDate() {
        return numberOfDaysAfterDueDate;
    }

    public void setNumberOfDaysAfterDueDate(int numberOfDaysAfterDueDate) {
        this.numberOfDaysAfterDueDate = numberOfDaysAfterDueDate;
    }

    public int getMaximumDaysToApplyPenalty() {
        return maximumDaysToApplyPenalty;
    }

    public void setMaximumDaysToApplyPenalty(int maximumDaysToApplyPenalty) {
        this.maximumDaysToApplyPenalty = maximumDaysToApplyPenalty;
    }

    public BigDecimal getInterestFixedAmount() {
        return interestFixedAmount;
    }

    public void setInterestFixedAmount(BigDecimal interestFixedAmount) {
        this.interestFixedAmount = interestFixedAmount;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    /* TuitionInstallmentTariff */

    public Product getTuitionInstallmentProduct() {
        return tuitionInstallmentProduct;
    }

    public void setTuitionInstallmentProduct(Product tuitionInstallmentProduct) {
        this.tuitionInstallmentProduct = tuitionInstallmentProduct;
    }

    public int getInstallmentOrder() {
        return installmentOrder;
    }

    public void setInstallmentOrder(int installmentOrder) {
        this.installmentOrder = installmentOrder;
    }

    public TuitionCalculationType getTuitionCalculationType() {
        return tuitionCalculationType;
    }

    public void setTuitionCalculationType(TuitionCalculationType tuitionCalculationType) {
        this.tuitionCalculationType = tuitionCalculationType;
    }

    public BigDecimal getFixedAmount() {
        return fixedAmount;
    }

    public void setFixedAmount(BigDecimal fixedAmount) {
        this.fixedAmount = fixedAmount;
    }

    public EctsCalculationType getEctsCalculationType() {
        return ectsCalculationType;
    }

    public void setEctsCalculationType(EctsCalculationType ectsCalculationType) {
        this.ectsCalculationType = ectsCalculationType;
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public void setFactor(BigDecimal factor) {
        this.factor = factor;
    }

    public BigDecimal getTotalEctsOrUnits() {
        return totalEctsOrUnits;
    }

    public void setTotalEctsOrUnits(BigDecimal totalEctsOrUnits) {
        this.totalEctsOrUnits = totalEctsOrUnits;
    }

    public boolean isAcademicalActBlockingOn() {
        return academicalActBlockingOn;
    }

    public void setAcademicalActBlockingOn(boolean academicalActBlockingOn) {
        this.academicalActBlockingOn = academicalActBlockingOn;
    }

    public boolean isBlockAcademicActsOnDebt() {
        return blockAcademicActsOnDebt;
    }

    public void setBlockAcademicActsOnDebt(boolean blockAcademicActsOnDebt) {
        this.blockAcademicActsOnDebt = blockAcademicActsOnDebt;
    }

    public StatuteType getStatuteType() {
        return statuteType;
    }

    public void setStatuteType(StatuteType statuteType) {
        this.statuteType = statuteType;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public boolean isApplyMaximumAmount() {
        return applyMaximumAmount;
    }

    public void setApplyMaximumAmount(boolean applyMaximumAmount) {
        this.applyMaximumAmount = applyMaximumAmount;
    }

    public BigDecimal getMaximumAmount() {
        return maximumAmount;
    }

    public void setMaximumAmount(BigDecimal maximumAmount) {
        this.maximumAmount = maximumAmount;
    }

    public boolean isShowAllDcps() {
        return showAllDcps;
    }

    public void setShowAllDcps(final boolean degreeCurricularPlansShownFilteredByExecutions) {
        this.showAllDcps = degreeCurricularPlansShownFilteredByExecutions;
    }

    /*
     * -------------
     * Other Methods
     * -------------
     */

    public static final Comparator<TreasuryTupleDataSourceBean> COMPARE_BY_ID_AND_TEXT =
            new Comparator<TreasuryTupleDataSourceBean>() {

                @Override
                public int compare(final TreasuryTupleDataSourceBean o1, final TreasuryTupleDataSourceBean o2) {
                    if (o1.getId() == "") {
                        return -1;
                    } else if (o2.getId() == "") {
                        return 1;
                    }

                    return TreasuryTupleDataSourceBean.COMPARE_BY_TEXT.compare(o1, o2);
                }
            };

    private List<TreasuryTupleDataSourceBean> degreeTypeDataSource() {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        final List<TreasuryTupleDataSourceBean> result =
                Lists.newArrayList(DegreeType.all().map((dt) -> new TreasuryTupleDataSourceBean(dt.getExternalId(),
                        academicTreasuryServices.localizedNameOfDegreeType(dt))).collect(Collectors.toList()));

        result.add(AcademicTreasuryConstants.SELECT_OPTION);

        return result.stream().sorted(COMPARE_BY_ID_AND_TEXT).collect(Collectors.toList());
    }

    private List<TreasuryTupleDataSourceBean> degreeCurricularPlanDataSource() {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        if (getExecutionYear() == null) {
            return Collections.<TreasuryTupleDataSourceBean> emptyList();
        }

        if (getDegreeType() == null) {
            return Collections.<TreasuryTupleDataSourceBean> emptyList();
        }

        final List<TreasuryTupleDataSourceBean> result = Lists.newArrayList();

        if (isShowAllDcps()) {
            result.addAll(academicTreasuryServices.readAllDegreeCurricularPlansSet().stream()
                    .filter(dcp -> dcp.getDegreeType() == getDegreeType())
                    .map((dcp) -> new TreasuryTupleDataSourceBean(dcp.getExternalId(),
                            "[" + dcp.getDegree().getCode() + "] " + dcp.getPresentationName(getExecutionYear())))
                    .collect(Collectors.toList()));
        } else {
            result.addAll(academicTreasuryServices
                    .readDegreeCurricularPlansWithExecutionDegree(getExecutionYear(), getDegreeType()).stream()
                    .map((dcp) -> new TreasuryTupleDataSourceBean(dcp.getExternalId(),
                            "[" + dcp.getDegree().getCode() + "] " + dcp.getPresentationName(getExecutionYear())))
                    .collect(Collectors.toList()));
        }

        return result.stream().sorted(COMPARE_BY_ID_AND_TEXT).collect(Collectors.toList());
    }

    private List<TreasuryTupleDataSourceBean> semesterDataSource() {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        if (getExecutionYear() == null) {
            return Lists.newArrayList();
        }

        final List<TreasuryTupleDataSourceBean> result = getExecutionYear().getExecutionPeriodsSet().stream()
                .map((cs) -> new TreasuryTupleDataSourceBean(cs.getExternalId(), cs.getQualifiedName()))
                .collect(Collectors.toList());

        result.add(AcademicTreasuryConstants.SELECT_OPTION);

        return result.stream().sorted(COMPARE_BY_ID_AND_TEXT).collect(Collectors.toList());
    }

    private List<TreasuryTupleDataSourceBean> curricularYearDataSource() {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        final List<TreasuryTupleDataSourceBean> result = academicTreasuryServices.readAllCurricularYearsSet().stream()
                .map((cy) -> new TreasuryTupleDataSourceBean(cy.getExternalId(), cy.getYear().toString()))
                .collect(Collectors.toList());

        result.add(AcademicTreasuryConstants.SELECT_OPTION);

        return result.stream().sorted(COMPARE_BY_ID_AND_TEXT).collect(Collectors.toList());
    }

    private List<TreasuryTupleDataSourceBean> ingressionDataSource() {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        final List<TreasuryTupleDataSourceBean> result = academicTreasuryServices.readAllIngressionTypesSet().stream()
                .map((i) -> new TreasuryTupleDataSourceBean(i.getExternalId(), i.getDescription().getContent()))
                .collect(Collectors.toList());

        result.add(AcademicTreasuryConstants.SELECT_OPTION);

        return result.stream().sorted(COMPARE_BY_ID_AND_TEXT).collect(Collectors.toList());
    }

    private List<TreasuryTupleDataSourceBean> registrationProtocolDataSource() {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        final List<TreasuryTupleDataSourceBean> result = academicTreasuryServices.readAllRegistrationProtocol().stream()
                .map((rp) -> new TreasuryTupleDataSourceBean(rp.getExternalId(), rp.getDescription().getContent()))
                .collect(Collectors.toList());

        result.add(AcademicTreasuryConstants.SELECT_OPTION);

        return result.stream().sorted(COMPARE_BY_ID_AND_TEXT).collect(Collectors.toList());
    }

    private List<TreasuryTupleDataSourceBean> registrationRegimeTypeDataSource() {
        List<TreasuryTupleDataSourceBean> result =
                ((List<RegistrationRegimeType>) Arrays.asList(RegistrationRegimeType.values())).stream()
                        .map((t) -> new TreasuryTupleDataSourceBean(t.name(), t.getLocalizedName())).collect(Collectors.toList());

        result.add(AcademicTreasuryConstants.SELECT_OPTION);

        return result.stream().sorted(COMPARE_BY_ID_AND_TEXT).collect(Collectors.toList());
    }

    public static List<TreasuryTupleDataSourceBean> interestTypeDataSource() {
        List<TreasuryTupleDataSourceBean> result = InterestType.findAll().stream()
                .map((it) -> new TreasuryTupleDataSourceBean(it.name(), it.getDescriptionI18N().getContent()))
                .collect(Collectors.toList());

        result.add(AcademicTreasuryConstants.SELECT_OPTION);

        return result.stream().sorted(COMPARE_BY_ID_AND_TEXT).collect(Collectors.toList());
    }

    public static List<TreasuryTupleDataSourceBean> ectsCalculationTypeDataSource() {
        List<TreasuryTupleDataSourceBean> result = ((List<EctsCalculationType>) Arrays.asList(EctsCalculationType.values()))
                .stream().map((ct) -> new TreasuryTupleDataSourceBean(ct.name(), ct.getDescriptionI18N().getContent()))
                .collect(Collectors.toList());

        result.add(AcademicTreasuryConstants.SELECT_OPTION);

        return result.stream().sorted(COMPARE_BY_ID_AND_TEXT).collect(Collectors.toList());
    }

    public static List<TreasuryTupleDataSourceBean> tuitionCalculationTypeDataSource() {
        List<TreasuryTupleDataSourceBean> result = ((List<TuitionCalculationType>) Arrays.asList(TuitionCalculationType.values()))
                .stream().map((ct) -> new TreasuryTupleDataSourceBean(ct.name(), ct.getDescriptionI18N().getContent()))
                .collect(Collectors.toList());

        result.add(AcademicTreasuryConstants.SELECT_OPTION);

        return result.stream().sorted(COMPARE_BY_ID_AND_TEXT).collect(Collectors.toList());
    }

    public static List<TreasuryTupleDataSourceBean> tuitionInstallmentProductDataSource(
            final TuitionPaymentPlanGroup tuitionPaymentPlanGroup, int desiredTuitionInstallmentOrder) {
        List<TreasuryTupleDataSourceBean> result = null;

        if (tuitionPaymentPlanGroup != null && tuitionPaymentPlanGroup.isForRegistration()) {
            result = AcademicTreasurySettings.getInstance().getTuitionProductGroup().getProductsSet().stream()
                    .filter(p -> p.isActive() && p.getTuitionInstallmentOrder() == desiredTuitionInstallmentOrder)
                    .map(p -> new TreasuryTupleDataSourceBean(p.getExternalId(), p.getName().getContent()))
                    .collect(Collectors.toList());
        } else {
            result = AcademicTreasurySettings.getInstance().getTuitionProductGroup().getProductsSet().stream()
                    .filter(p -> p.isActive())
                    .map(p -> new TreasuryTupleDataSourceBean(p.getExternalId(), p.getName().getContent()))
                    .collect(Collectors.toList());
        }

        result.add(AcademicTreasuryConstants.SELECT_OPTION);

        return result.stream().sorted(COMPARE_BY_ID_AND_TEXT).collect(Collectors.toList());
    }

    private List<TreasuryTupleDataSourceBean> statuteTypeDataSource() {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        final List<TreasuryTupleDataSourceBean> result = academicTreasuryServices.readAllStatuteTypesSet().stream().map(
                l -> new TreasuryTupleDataSourceBean(l.getExternalId(), academicTreasuryServices.localizedNameOfStatuteType(l)))
                .collect(Collectors.toList());

        result.add(AcademicTreasuryConstants.SELECT_OPTION);

        return result.stream().sorted(COMPARE_BY_ID_AND_TEXT).collect(Collectors.toList());
    }

    private List<TreasuryTupleDataSourceBean> payorDebtAccountDataSource() {
        if (finantialEntity == null) {
            return Lists.newArrayList();
        }

        final SortedSet<DebtAccount> payorDebtAccountsSet =
                DebtAccount.findActiveAdhocDebtAccountsSortedByCustomerName(finantialEntity.getFinantialInstitution());

        final List<TreasuryTupleDataSourceBean> result = payorDebtAccountsSet.stream()
                .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(),
                        String.format("%s - %s", l.getCustomer().getUiFiscalNumber(), l.getCustomer().getName())))
                .collect(Collectors.toList());

        result.add(AcademicTreasuryConstants.SELECT_OPTION);

        return result.stream().sorted(COMPARE_BY_ID_AND_TEXT).collect(Collectors.toList());
    }

    private List<TreasuryTupleDataSourceBean> executionYearDataSource() {
        final List<TreasuryTupleDataSourceBean> result = ExecutionYear.readNotClosedExecutionYears().stream()
                .sorted(ExecutionYear.REVERSE_COMPARATOR_BY_YEAR).collect(Collectors.toList()).stream()
                .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getQualifiedName())).collect(Collectors.toList());

        result.add(0, AcademicTreasuryConstants.SELECT_OPTION);

        return result;
    }

    public List<String> validateStudentConditions() {
        List<String> result = Lists.newArrayList();
        if (!hasAtLeastOneConditionSpecified()) {
            result.add("error.TuitionPaymentPlan.specify.at.least.one.condition");
        }

        if (isCustomized() && (isDefaultPaymentPlan() || hasStudentConditionSelected())) {
            result.add("error.TuitionPaymentPlan.customized.plan.cannot.have.other.options");
        }

        if (isDefaultPaymentPlan() && hasStudentConditionSelected()) {
            result.add("error.TuitionPaymentPlan.default.payment.plan.cannot.have.other.options");
        }

        return result;
    }

    private boolean hasStudentConditionSelected() {
        return getRegistrationRegimeType() != null || getRegistrationProtocol() != null || getIngression() != null
                || getCurricularYear() != null || getExecutionSemester() != null || isFirstTimeStudent()
                || getStatuteType() != null;
    }

    private boolean hasAtLeastOneConditionSpecified() {
        boolean hasAtLeastOneCondition = false;

        hasAtLeastOneCondition |= isDefaultPaymentPlan();
        hasAtLeastOneCondition |= getRegistrationRegimeType() != null;
        hasAtLeastOneCondition |= getRegistrationProtocol() != null;
        hasAtLeastOneCondition |= getIngression() != null;
        hasAtLeastOneCondition |= getCurricularYear() != null;
        hasAtLeastOneCondition |= getStatuteType() != null;
        hasAtLeastOneCondition |= getExecutionSemester() != null;
        hasAtLeastOneCondition |= isFirstTimeStudent();
        hasAtLeastOneCondition |= isCustomized();
        hasAtLeastOneCondition |= isWithLaboratorialClasses();

        return hasAtLeastOneCondition;
    }

}
