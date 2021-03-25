/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and ServiÃ§os Partilhados da
 * Universidade de Lisboa:
 *  - Copyright Â© 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright Â© 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: xpto@qub-it.com
 *
 * 
 * This file is part of FenixEdu Academictreasury.
 *
 * FenixEdu Academictreasury is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Academictreasury is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Academictreasury.  If not, see <http://www.gnu.org/licenses/>.
 */
		
package org.fenixedu.academictreasury.dto.reports;

import java.util.ArrayList;
import java.util.List;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academictreasury.domain.reports.DebtReportRequestType;
import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.joda.time.LocalDate;


public class DebtReportRequestBean implements ITreasuryBean {
	
	private DebtReportRequestType type;
	private LocalDate beginDate;
	private LocalDate endDate;
	private String decimalSeparator;
	private boolean includeAnnuledEntries;

    private boolean includeExtraAcademicInfo;
    private boolean includeErpIntegrationInfo;
    private boolean includeSibsInfo;
    private boolean includeProductsInfo;
    
    private DegreeType degreeType;
    private ExecutionYear executionYear;
	
    private List<TreasuryTupleDataSourceBean> debtReportRequestTypeDataSource;
    
	public DebtReportRequestBean(){
	    this.type = DebtReportRequestType.INVOICE_ENTRIES;
	    this.beginDate = new LocalDate();
	    this.endDate = new LocalDate();
	    this.decimalSeparator = ",";
	    this.includeAnnuledEntries = true;
        
        this.includeExtraAcademicInfo = true;
        this.includeErpIntegrationInfo = true;
        this.includeSibsInfo = true;
        this.includeProductsInfo = true;
        
        this.degreeType = null;
        this.executionYear = null;
        
        setDebtReportRequestTypeDataSource();
	}
	
	public List<TreasuryTupleDataSourceBean> getDebtReportRequestTypeDataSource() {
	    return this.debtReportRequestTypeDataSource;
	}

    public void setDebtReportRequestTypeDataSource() {
        this.debtReportRequestTypeDataSource = new ArrayList<>();
        for (DebtReportRequestType debtReportRequestType : DebtReportRequestType.values()) {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setId(debtReportRequestType.name());
            tuple.setText(debtReportRequestType.getDescriptionI18N().getContent());
            this.debtReportRequestTypeDataSource.add(tuple);
        }
    }

    /* GETTERS & SETTERS */
    
    public DebtReportRequestType getType() {
        return type;
    }

    public void setType(DebtReportRequestType type) {
        this.type = type;
    }

    public org.joda.time.LocalDate getBeginDate() {
        return beginDate;
    }

    public void setBeginDate(org.joda.time.LocalDate beginDate) {
        this.beginDate = beginDate;
    }

    public org.joda.time.LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(org.joda.time.LocalDate endDate) {
        this.endDate = endDate;
    }
    
    public String getDecimalSeparator() {
        return decimalSeparator;
    }
    
    public void setDecimalSeparator(String decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }
	
    public boolean isIncludeAnnuledEntries() {
        return includeAnnuledEntries;
    }
    
    public void setIncludeAnnuledEntries(boolean includeAnnuledEntries) {
        this.includeAnnuledEntries = includeAnnuledEntries;
    }

    public boolean isIncludeExtraAcademicInfo() {
        return includeExtraAcademicInfo;
    }
    
    public void setIncludeExtraAcademicInfo(boolean includeExtraAcademicInfo) {
        this.includeExtraAcademicInfo = includeExtraAcademicInfo;
    }

    public boolean isIncludeErpIntegrationInfo() {
        return includeErpIntegrationInfo;
    }
    
    public void setIncludeErpIntegrationInfo(boolean includeErpIntegrationInfo) {
        this.includeErpIntegrationInfo = includeErpIntegrationInfo;
    }

    public boolean isIncludeSibsInfo() {
        return includeSibsInfo;
    }
    
    public void setIncludeSibsInfo(boolean includeSibsInfo) {
        this.includeSibsInfo = includeSibsInfo;
    }

    public boolean isIncludeProductsInfo() {
        return includeProductsInfo;
    }
    
    public void setIncludeProductsInfo(boolean includeProductsInfo) {
        this.includeProductsInfo = includeProductsInfo;
    }
    
    public DegreeType getDegreeType() {
        return degreeType;
    }
    
    public void setDegreeType(DegreeType degreeType) {
        this.degreeType = degreeType;
    }
    
    public ExecutionYear getExecutionYear() {
        return executionYear;
    }
    
    public void setExecutionYear(ExecutionYear executionYear) {
        this.executionYear = executionYear;
    }
    
}
