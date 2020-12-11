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
package org.fenixedu.academictreasury.dto.academicservicerequest;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.serviceRequests.AcademicServiceRequest;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.treasury.TreasuryBridgeAPIFactory;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.serviceRequests.ITreasuryServiceRequest;
import org.fenixedu.academictreasury.services.EmolumentServices;
import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;

import pt.ist.fenixframework.Atomic;

public class AcademicServiceRequestDebtCreationBean implements Serializable, ITreasuryBean {

    private static final long serialVersionUID = 1L;

    private LocalDate debtDate;
    private Registration registration;

    private DebtAccount debtAccount;

    private List<TreasuryTupleDataSourceBean> registrationDataSource;
    private List<TreasuryTupleDataSourceBean> academicServiceRequestesDataSource;

    private AcademicServiceRequest academicServiceRequest;

    public AcademicServiceRequestDebtCreationBean(final DebtAccount debtAccount) {
        this.debtAccount = debtAccount;

        updateData();
    }

    @Atomic
    public void updateData() {
        if (registration == null) {
            academicServiceRequest = null;
        }

        getRegistrationDataSource();
        getAcademicServiceRequestesDataSource();

        if (registration != null && academicServiceRequest != null) {
            debtDate = EmolumentServices.possibleDebtDateOnAcademicService((ITreasuryServiceRequest) academicServiceRequest);

            if (debtDate == null) {
                debtDate = new LocalDate();
            }
        } else {
            debtDate = new LocalDate();
        }
    }

    public List<TreasuryTupleDataSourceBean> getRegistrationDataSource() {
        if (!isStudent()) {
            registrationDataSource = Lists.newArrayList();
            return registrationDataSource;
        }

        registrationDataSource = ((PersonCustomer) debtAccount.getCustomer()).getPerson().getStudent().getRegistrationsSet()
                .stream()
                .map(r -> { 
                  
                    final String degreeCode = r.getDegree().getCode();
                    final String degreePresentationName = r.getDegree().getPresentationNameI18N().getContent();
                    final String registrationDate = r.getStartDate() != null ? r.getStartDate().toString("yyyy-MM-dd") : "";
                    final String agreement = r.getRegistrationProtocol() != null ? r.getRegistrationProtocol().getDescription().getContent() : "";

                    final TreasuryTupleDataSourceBean t = new TreasuryTupleDataSourceBean(r.getExternalId(),
                        String.format("[%s] %s (%s %s)", degreeCode, degreePresentationName, registrationDate, agreement));
                    
                    return t; 

                }).collect(Collectors.toList());

        return registrationDataSource;
    }

    public List<TreasuryTupleDataSourceBean> getAcademicServiceRequestesDataSource() {
        if (registration == null) {
            academicServiceRequestesDataSource = Lists.newArrayList();
            return academicServiceRequestesDataSource;
        }
        
        academicServiceRequestesDataSource = AcademicTreasuryEvent
                .find(((PersonCustomer) this.debtAccount.getCustomer()).getAssociatedPerson())
            .filter(e -> e.isAcademicServiceRequestEvent())
            .map(e -> e.getITreasuryServiceRequest())
            .map(r -> new TreasuryTupleDataSourceBean(r.getExternalId(), String.format("[%s] %s", r.getServiceRequestNumberYear(), r.getDescription())))
            .sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT).collect(Collectors.toList());

        return academicServiceRequestesDataSource;
    }

    public boolean isStudent() {
        return debtAccount.getCustomer().isPersonCustomer()
                && ((PersonCustomer) debtAccount.getCustomer()).getPerson().getStudent() != null;
    }

    /* -----------------
     * GETTERS & SETTERS
     * -----------------
     */
    public LocalDate getDebtDate() {
        return debtDate;
    }

    public void setDebtDate(LocalDate debtDate) {
        this.debtDate = debtDate;
    }

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    public AcademicServiceRequest getAcademicServiceRequest() {
        return academicServiceRequest;
    }

    public void setAcademicServiceRequest(AcademicServiceRequest academicServiceRequest) {
        this.academicServiceRequest = academicServiceRequest;
    }

}
