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
package org.fenixedu.academictreasury.domain.integration;

import java.util.Comparator;
import java.util.stream.Stream;

import org.fenixedu.academictreasury.domain.integration.tuitioninfo.ERPTuitionInfo;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.integration.OperationFile;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;

public class ERPTuitionInfoExportOperation extends ERPTuitionInfoExportOperation_Base {

    public static final Comparator<ERPTuitionInfoExportOperation> COMPARE_BY_EXECUTION_DATE =
            new Comparator<ERPTuitionInfoExportOperation>() {

                @Override
                public int compare(final ERPTuitionInfoExportOperation o1, final ERPTuitionInfoExportOperation o2) {
                    int c = o1.getExecutionDate().compareTo(o2.getExecutionDate());

                    return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
                }
            };
            
    public static final Comparator<ERPTuitionInfoExportOperation> COMPARE_BY_VERSIONING_CREATION_DATE = 
            new Comparator<ERPTuitionInfoExportOperation>() {

                @Override
                public int compare(final ERPTuitionInfoExportOperation o1, final ERPTuitionInfoExportOperation o2) {
                    int c = TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(o1).compareTo(TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(o2));
                    
                    return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
                }
        
    };

    public ERPTuitionInfoExportOperation() {
        super();
    }

    public ERPTuitionInfoExportOperation(final ERPTuitionInfo erpTuitionInfo) {
        this();

        setErpTuitionInfo(erpTuitionInfo);
    }

    protected void init(final OperationFile file, final FinantialInstitution finantialInstitution, final String erpOperationId,
            final DateTime executionDate) {
        setFile(file);
        setFinantialInstitution(finantialInstitution);
        setExecutionDate(executionDate);

        checkRules();
    }

    private void checkRules() {
        if (getFile() == null) {
            throw new TreasuryDomainException("error.ERPTuitionInfoExportOperation.file.required");
        }

        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.ERPTuitionInfoExportOperation.finantialInstitution.required");
        }
    }
    
    public boolean isSuccess() {
        return getSuccess();
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<ERPTuitionInfoExportOperation> findAll() {
        return ERPTuitionInfo.findAll().flatMap(e -> e.getErpTuitionInfoExportOperationsSet().stream());
    }

    @Atomic
    public static ERPTuitionInfoExportOperation create(final ERPTuitionInfo erpTuitionInfo, final byte[] data,
            final String filename, final FinantialInstitution finantialInstitution, final String erpOperationId,
            final DateTime executionDate) {
        ERPTuitionInfoExportOperation erpTuitionInfoExportOperation = new ERPTuitionInfoExportOperation(erpTuitionInfo);
        OperationFile file;

        if (data == null) {
            file = OperationFile.create(filename, new byte[0], erpTuitionInfoExportOperation);
        } else {
            file = OperationFile.create(filename, data, erpTuitionInfoExportOperation);
        }

        erpTuitionInfoExportOperation.init(file, finantialInstitution, erpOperationId, executionDate);

        return erpTuitionInfoExportOperation;
    }

}
