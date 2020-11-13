package org.fenixedu.academictreasury.domain.integration.tuitioninfo;

import static java.lang.String.format;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;

import com.google.common.collect.Lists;

public class ERPTuitionInfoTypeBean implements ITreasuryBean, Serializable {
    
    public static final String DEGREE_TYPE_OPTION = "DEGREE_TYPE_OPTION";
    public static final String DEGREES_OPTION = "DEGREES_OPTION";
    public static final String DEGREE_CURRICULAR_PLANS_OPTION = "DEGREE_CURRICULAR_PLANS_OPTIONS";
    
    private static final long serialVersionUID = 1L;
    
    private ExecutionYear executionYear;
    
    private ERPTuitionInfoProduct erpTuitionInfoProduct;
    
    private List<Product> tuitionProducts = Lists.newArrayList();
    private List<DegreeType> degreeTypes = Lists.newArrayList();
    private List<Degree> degrees = Lists.newArrayList();
    private List<DegreeCurricularPlan> degreeCurricularPlans = Lists.newArrayList();
    
    private List<TreasuryTupleDataSourceBean> tuitionPaymentPlanGroupDataSource = Lists.newArrayList();
    private List<TreasuryTupleDataSourceBean> erpTuitionInfoProductDataSource = Lists.newArrayList();
    private List<TreasuryTupleDataSourceBean> productDataSource = Lists.newArrayList();
    private List<TreasuryTupleDataSourceBean> degreeTypeDataSource = Lists.newArrayList();
    private List<TreasuryTupleDataSourceBean> degreeDataSource = Lists.newArrayList();
    private List<TreasuryTupleDataSourceBean> degreeCurricularPlanDataSource = Lists.newArrayList();
    
    private DegreeType selectedDegreeType;
    private List<Degree> selectedDegrees = Lists.newArrayList();
    private List<DegreeCurricularPlan> selectedDegreeCurricularPlans = Lists.newArrayList();
    
    private Product selectedTuitionProduct;
    
    private String degreeInfoSelectOption;
    
    private TuitionPaymentPlanGroup tuitionPaymentPlanGroup;

    private ERPTuitionInfoType erpTuitionInfoType;
    
    public ERPTuitionInfoTypeBean(final ExecutionYear executionYear) {
        this.executionYear = executionYear;
        this.degreeInfoSelectOption = DEGREE_TYPE_OPTION;
    }
    
    public ERPTuitionInfoTypeBean(final ERPTuitionInfoType erpTuitionInfoType) {
        this.erpTuitionInfoType = erpTuitionInfoType;
        
        this.erpTuitionInfoProduct = erpTuitionInfoType.getErpTuitionInfoProduct();
        
        this.executionYear = erpTuitionInfoType.getExecutionYear();

        this.tuitionProducts.addAll(erpTuitionInfoType.getTuitionProductsSet());
        this.degreeTypes.addAll(erpTuitionInfoType.getErpTuitionInfoTypeAcademicEntriesSet().stream().filter(e -> e.isDefinedForDegreeType()).map(e -> e.getDegreeType()).collect(Collectors.toSet()));
        this.degrees.addAll(erpTuitionInfoType.getErpTuitionInfoTypeAcademicEntriesSet().stream().filter(e -> e.isDefinedForDegree()).map(e -> e.getDegree()).collect(Collectors.toSet()));
        this.degreeCurricularPlans.addAll(erpTuitionInfoType.getErpTuitionInfoTypeAcademicEntriesSet().stream().filter(e -> e.isDefinedForDegreeCurricularPlan()).map(e -> e.getDegreeCurricularPlan()).collect(Collectors.toSet()));
        
        if(erpTuitionInfoType.getErpTuitionInfoTypeAcademicEntriesSet().iterator().next().isForRegistration()) {
            this.tuitionPaymentPlanGroup = TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get();
        } else if(erpTuitionInfoType.getErpTuitionInfoTypeAcademicEntriesSet().iterator().next().isForStandalone()) {
            this.tuitionPaymentPlanGroup = TuitionPaymentPlanGroup.findUniqueDefaultGroupForStandalone().get();
        } else if(erpTuitionInfoType.getErpTuitionInfoTypeAcademicEntriesSet().iterator().next().isForExtracurricular()) {
            this.tuitionPaymentPlanGroup = TuitionPaymentPlanGroup.findUniqueDefaultGroupForExtracurricular().get();
        }
        
        update();
    }
    
    public void addTuitionProduct() {
        if(this.selectedTuitionProduct == null) {
            return;
        }
        
        if(!this.tuitionProducts.contains(this.selectedTuitionProduct) ) {
            this.tuitionProducts.add(this.selectedTuitionProduct);
        }
        
        this.selectedTuitionProduct = null;
    }
    
    public void removeTuitionProduct(final Product product) {
        this.tuitionProducts.remove(product);
    }
    
    public void removeDegreeType(final DegreeType degreeType) {
        this.degreeTypes.remove(degreeType);
    }
    
    public void removeDegree(final Degree degree) {
        this.degrees.remove(degree);
    }
    
    public void removeDegreeCurricularPlan(final DegreeCurricularPlan degreeCurricularPlan) {
        this.degreeCurricularPlans.remove(degreeCurricularPlan);
    }
    
    public void addDegreeType() {
        
        if(this.selectedDegreeType == null) {
            return;
        }
        
        if(degreeCurricularPlans.stream().map(DegreeCurricularPlan::getDegree).map(Degree::getDegreeType).anyMatch(d -> d == selectedDegreeType)) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.degreeType.refering.in.degree.curricular.plans", 
                    format("%s",  selectedDegreeType.getName().getContent()));
        }
        
        if(degrees.stream().map(Degree::getDegreeType).anyMatch(d -> d == selectedDegreeType)) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.degreeType.refering.in.degrees", 
                    format("%s",  selectedDegreeType.getName().getContent()));
        }
        
        if(!this.degreeTypes.contains(this.selectedDegreeType)) {
            this.degreeTypes.add(this.selectedDegreeType);
        }
        
        this.selectedDegreeType = null;
    }

    public void addDegrees() {
        if(this.selectedDegrees.isEmpty()) {
            return;
        }
        
        for (final Degree degree : this.selectedDegrees) {
            
            if(degreeCurricularPlans.stream().map(DegreeCurricularPlan::getDegree).anyMatch(d -> d == degree)) {
                throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.degree.refering.in.degree.curricular.plans", 
                        format("%s > %s",  degree.getDegreeType().getName().getContent(), degree.getPresentationNameI18N().getContent()));
            }

            if(degrees.contains(degree)) {
                continue;
            }
            
            if(degreeTypes.contains(degree.getDegreeType())) {
                continue;
            }
            
            this.degrees.add(degree);
            
        }
        
        this.selectedDegrees = Lists.newArrayList();
    }
    
    public void addDegreeCurricularPlans() {
        if(this.selectedDegreeCurricularPlans.isEmpty()) {
            return;
        }
        
        for (final DegreeCurricularPlan dcp : this.selectedDegreeCurricularPlans) {
            
            if(degreeTypes.contains(dcp.getDegreeType())) {
                continue;
            }
            
            if(degrees.contains(dcp.getDegree())) {
                continue;
            }
            
            if(degreeCurricularPlans.contains(dcp)) {
                continue;
            }
            
            this.degreeCurricularPlans.add(dcp);
        }
        
        this.selectedDegreeCurricularPlans = Lists.newArrayList();
    }

    public void update() {
        {
            final TuitionPaymentPlanGroup r = TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().get();
            final TuitionPaymentPlanGroup s = TuitionPaymentPlanGroup.findUniqueDefaultGroupForStandalone().get();
            final TuitionPaymentPlanGroup e = TuitionPaymentPlanGroup.findUniqueDefaultGroupForExtracurricular().get();
            
            this.tuitionPaymentPlanGroupDataSource = Lists.newArrayList(
                    new TreasuryTupleDataSourceBean(r.getExternalId(), r.getName().getContent()),
                    new TreasuryTupleDataSourceBean(s.getExternalId(), s.getName().getContent()),
                    new TreasuryTupleDataSourceBean(e.getExternalId(), e.getName().getContent()));
        }
        
        this.erpTuitionInfoProductDataSource = ERPTuitionInfoProduct.findAll()
                .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), format("[%s] %s", l.getCode(), l.getName())))
                .sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT)
                .collect(Collectors.toList());
        
        this.productDataSource = AcademicTreasurySettings.getInstance().getTuitionProductGroup().getProductsSet().stream()
                .filter(p -> !this.tuitionProducts.contains(p))
                .sorted(Product.COMPARE_BY_NAME)
                .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), format("%s [%s]", l.getName().getContent(), l.getCode()))).collect(Collectors.toList());

        this.degreeTypeDataSource = DegreeType.all()
                .filter(dt -> dt.getDegreeSet().stream().flatMap(d -> d.getExecutionDegreesForExecutionYear(executionYear).stream()).count() > 0)
                .filter(d -> !degreeTypes.contains(d))
                .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getName().getContent()))
                .sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT).collect(Collectors.toList());
        
        if(DEGREES_OPTION.equals(this.degreeInfoSelectOption)) {
            if(this.selectedDegreeType != null) {
                this.degreeDataSource = this.selectedDegreeType.getDegreeSet().stream()
                        .filter(d -> !this.degrees.contains(d))
                        .filter(d -> !this.degreeTypes.contains(d.getDegreeType()))
                        .filter(d -> !d.getExecutionDegreesForExecutionYear(executionYear).isEmpty())
                        .map(d -> new TreasuryTupleDataSourceBean(d.getExternalId(), degreeDescription(d)))
                        .sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT)
                        .collect(Collectors.toList());
            } else {
                this.degreeDataSource = Lists.newArrayList();
            }
            
        } else if(DEGREE_CURRICULAR_PLANS_OPTION.equals(this.degreeInfoSelectOption)) {
            if(this.selectedDegreeType != null) {
                
                this.degreeCurricularPlanDataSource = Bennu.getInstance().getDegreeCurricularPlansSet().stream()
                    .filter(d -> d.getDegreeType() == this.selectedDegreeType)
                    .filter(d -> d.getExecutionDegreeByAcademicInterval(executionYear.getAcademicInterval()) != null)
                    .filter(d -> !this.degrees.contains(d.getDegree()))
                    .filter(d -> !this.degreeTypes.contains(d.getDegree().getDegreeType()))
                    .map((dcp) -> new TreasuryTupleDataSourceBean(dcp.getExternalId(), dcpDescription(dcp)))
                    .sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT)
                    .collect(Collectors.toList());

            
            } else {
                this.degreeCurricularPlanDataSource = Lists.newArrayList();
            }
            
        }
        
    }

    private String degreeDescription(final Degree d) {
        return format("[%s] %s", d.getCode(), d.getPresentationNameI18N().getContent());
    }

    public static String dcpDescription(final DegreeCurricularPlan dcp) {
        return format("[%s] %s - %s",
                dcp.getDegree().getCode(), dcp.getDegree().getPresentationNameI18N().getContent(), dcp.getName());
    }
    
    public boolean isToUpdate() {
        return getErpTuitionInfoType() != null;
    }
    
    public boolean isDegreeInformationDefined() {
        return !degreeTypes.isEmpty() || !degrees.isEmpty() || !degreeCurricularPlans.isEmpty();
    }
    
    public boolean isDegreeInfoSelectOptionDegreeType() {
        return DEGREE_TYPE_OPTION.equals(this.degreeInfoSelectOption);
    }
    
    public boolean isDegreeInfoSelectOptionDegrees() {
        return DEGREES_OPTION.equals(this.degreeInfoSelectOption);
    }
    
    public boolean isDegreeInfoSelectOptionDegreeCurricularPlans() {
        return DEGREE_CURRICULAR_PLANS_OPTION.equals(this.degreeInfoSelectOption);
    }
    

    // @formatter:off
    /* *****************
     * GETTERS & SETTERS
     * *****************
     */
    // @formatter:on

    public ExecutionYear getExecutionYear() {
        return executionYear;
    }
    
    public void setExecutionYear(ExecutionYear executionYear) {
        this.executionYear = executionYear;
    }
    
    public ERPTuitionInfoProduct getErpTuitionInfoProduct() {
        return erpTuitionInfoProduct;
    }

    public void setErpTuitionInfoProduct(ERPTuitionInfoProduct erpTuitionInfoProduct) {
        this.erpTuitionInfoProduct = erpTuitionInfoProduct;
    }

    public List<Product> getTuitionProducts() {
        return tuitionProducts;
    }

    public void setTuitionProducts(List<Product> tuitionProducts) {
        this.tuitionProducts = tuitionProducts;
    }

    public List<DegreeType> getDegreeTypes() {
        return degreeTypes;
    }

    public void setDegreeTypes(List<DegreeType> degreeTypes) {
        this.degreeTypes = degreeTypes;
    }

    public List<Degree> getDegrees() {
        return degrees;
    }

    public void setDegrees(List<Degree> degrees) {
        this.degrees = degrees;
    }

    public List<DegreeCurricularPlan> getDegreeCurricularPlans() {
        return degreeCurricularPlans;
    }

    public void setDegreeCurricularPlans(List<DegreeCurricularPlan> degreeCurricularPlans) {
        this.degreeCurricularPlans = degreeCurricularPlans;
    }

    public List<TreasuryTupleDataSourceBean> getErpTuitionInfoProductDataSource() {
        return erpTuitionInfoProductDataSource;
    }

    public void setErpTuitionInfoProductDataSource(List<TreasuryTupleDataSourceBean> erpTuitionInfoProductDataSource) {
        this.erpTuitionInfoProductDataSource = erpTuitionInfoProductDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getProductDataSource() {
        return productDataSource;
    }

    public void setProductDataSource(List<TreasuryTupleDataSourceBean> productDataSource) {
        this.productDataSource = productDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getDegreeTypeDataSource() {
        return degreeTypeDataSource;
    }

    public void setDegreeTypeDataSource(List<TreasuryTupleDataSourceBean> degreeTypeDataSource) {
        this.degreeTypeDataSource = degreeTypeDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getDegreeDataSource() {
        return degreeDataSource;
    }

    public void setDegreeDataSource(List<TreasuryTupleDataSourceBean> degreeDataSource) {
        this.degreeDataSource = degreeDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getDegreeCurricularPlanDataSource() {
        return degreeCurricularPlanDataSource;
    }

    public void setDegreeCurricularPlanDataSource(List<TreasuryTupleDataSourceBean> degreeCurricularPlanDataSource) {
        this.degreeCurricularPlanDataSource = degreeCurricularPlanDataSource;
    }

    public DegreeType getSelectedDegreeType() {
        return selectedDegreeType;
    }

    public void setSelectedDegreeType(DegreeType selectedDegreeType) {
        this.selectedDegreeType = selectedDegreeType;
    }

    public List<Degree> getSelectedDegrees() {
        return selectedDegrees;
    }

    public void setSelectedDegrees(List<Degree> selectedDegrees) {
        this.selectedDegrees = selectedDegrees;
    }

    public List<DegreeCurricularPlan> getSelectedDegreeCurricularPlans() {
        return selectedDegreeCurricularPlans;
    }

    public void setSelectedDegreeCurricularPlans(List<DegreeCurricularPlan> selectedDegreeCurricularPlans) {
        this.selectedDegreeCurricularPlans = selectedDegreeCurricularPlans;
    }

    public Product getSelectedTuitionProduct() {
        return selectedTuitionProduct;
    }

    public void setSelectedTuitionProduct(Product selectedTuitionProduct) {
        this.selectedTuitionProduct = selectedTuitionProduct;
    }

    public String getDegreeInfoSelectOption() {
        return degreeInfoSelectOption;
    }

    public void setDegreeInfoSelectOption(String degreeInfoSelectOption) {
        this.degreeInfoSelectOption = degreeInfoSelectOption;
    }

    public TuitionPaymentPlanGroup getTuitionPaymentPlanGroup() {
        return tuitionPaymentPlanGroup;
    }
    
    public void setTuitionPaymentPlanGroup(TuitionPaymentPlanGroup tuitionPaymentPlanGroup) {
        this.tuitionPaymentPlanGroup = tuitionPaymentPlanGroup;
    }
    
    public List<TreasuryTupleDataSourceBean> getTuitionPaymentPlanGroupDataSource() {
        return tuitionPaymentPlanGroupDataSource;
    }
    
    public void setTuitionPaymentPlanGroupDataSource(List<TreasuryTupleDataSourceBean> tuitionPaymentPlanGroupDataSource) {
        this.tuitionPaymentPlanGroupDataSource = tuitionPaymentPlanGroupDataSource;
    }
    
    public ERPTuitionInfoType getErpTuitionInfoType() {
        return erpTuitionInfoType;
    }
    
    public void setErpTuitionInfoType(ERPTuitionInfoType erpTuitionInfoType) {
        this.erpTuitionInfoType = erpTuitionInfoType;
    }
    
}
