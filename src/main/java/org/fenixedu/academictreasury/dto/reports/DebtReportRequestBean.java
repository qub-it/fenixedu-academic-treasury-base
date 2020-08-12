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

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academictreasury.domain.reports.DebtReportRequestType;
import org.fenixedu.treasury.dto.ITreasuryBean;
import org.joda.time.LocalDate;

public class DebtReportRequestBean implements ITreasuryBean {
	
	private DebtReportRequestType type;
	private org.joda.time.LocalDate beginDate;
	private org.joda.time.LocalDate endDate;
	private String decimalSeparator;
	private boolean includeAnnuledEntries;

    private boolean includeExtraAcademicInfo;
    private boolean includeErpIntegrationInfo;
    private boolean includeSibsInfo;
    private boolean includeProductsInfo;
    
    private DegreeType degreeType;
    private ExecutionYear executionYear;
	
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
