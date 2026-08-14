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
package org.fenixedu.academictreasury.domain.debtGeneration.requests;

import com.qubit.terra.framework.services.ServiceProvider;
import com.qubit.terra.framework.services.fileSupport.FileDescriptor;
import com.qubit.terra.framework.services.fileSupport.FileManager;
import org.apache.commons.lang3.StringUtils;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academictreasury.domain.emoluments.AcademicTax;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.domain.TreasuryFile;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.io.InputStream;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * This class applies not only for debt generation but also for other
 * operations with debts
 *
 * @author anilmamede
 *
 */
public class MassiveDebtGenerationRequestFile extends MassiveDebtGenerationRequestFile_Base implements IGenericFile {

    public static final String CONTENT_TYPE = "application/octet-stream";

    public static final Comparator<MassiveDebtGenerationRequestFile> COMPARE_BY_CREATION_DATE = (o1, o2) -> {

        int c = o1.getCreationDate().compareTo(o2.getCreationDate());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    protected MassiveDebtGenerationRequestFile() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
    }

    protected MassiveDebtGenerationRequestFile(final MassiveDebtGenerationRequestFileBean bean, final String filename,
            final byte[] content) {
        this();

        final MassiveDebtGenerationType type = bean.getMassiveDebtGenerationType();
        final TuitionPaymentPlanGroup tuitionPaymentPlanGroup = bean.getTuitionPaymentPlanGroup();
        final AcademicTax academicTax = bean.getAcademicTax();
        final ExecutionYear executionYear = bean.getExecutionYear();
        final LocalDate debtDate = bean.getDebtDate();
        final String reason = bean.getReason();

        FileManager fileManager = ServiceProvider.getService(FileManager.class);

        FileDescriptor fileDescriptor = fileManager.createFile(filename, content.length, CONTENT_TYPE, content);
        setFileDescriptorId(fileDescriptor.getId());

        setMassiveDebtGenerationType(type);
        setTuitionPaymentPlanGroup(tuitionPaymentPlanGroup);
        setAcademicTax(academicTax);
        setExecutionYear(executionYear);
        setDebtDate(debtDate);
        setReason(reason);
        setFinantialInstitution(bean.getFinantialInstitution());

        checkRules();

    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequestFile.bennu.required");
        }

        if (getMassiveDebtGenerationType() == null) {
            throw new AcademicTreasuryDomainException(
                    "error.MassiveDebtGenerationRequestFile.massiveDebtGenerationType.required");
        }

        getMassiveDebtGenerationType().implementation().checkRules(this);
    }

    public String getDataDescription() {
        return getMassiveDebtGenerationType().implementation().dataDescription(this);
    }

    @Atomic
    public void process() {
        getMassiveDebtGenerationType().implementation().process(this);
    }

    @Override
    public boolean isAccessible(final String username) {
        return TreasuryAccessControlAPI.isBackOfficeMember(username);
    }

    @Override
    public void delete() {
        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        FileManager fileManager = ServiceProvider.getService(FileManager.class);

        setDomainRoot(null);
        setMassiveDebtGenerationType(null);
        setTuitionPaymentPlanGroup(null);
        setAcademicTax(null);
        setExecutionYear(null);
        setFinantialInstitution(null);

        if (StringUtils.isNotEmpty(getFileDescriptorId())) {
            fileManager.delete(getFileDescriptorId());
        }

        if (getTreasuryFile() != null) {
            services.deleteFile(this);
        }

        super.deleteDomainObject();
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<MassiveDebtGenerationRequestFile> findAll() {
        return FenixFramework.getDomainRoot().getMassiveDebtGenerationRequestFilesSet().stream();
    }

    public static Stream<MassiveDebtGenerationRequestFile> findAllActive() {
        return findAll().filter(m -> m.getMassiveDebtGenerationType().isActive());
    }

    @Atomic
    public static MassiveDebtGenerationRequestFile create(final MassiveDebtGenerationRequestFileBean bean, final String filename,
            final byte[] content) {
        return new MassiveDebtGenerationRequestFile(bean, filename, content);
    }

    // 2026-08-14 (#qubIT-Fenix-8024)
    //
    // The property fileDescriptorId is used to get the file id
    @Override
    @Deprecated
    public String getFileId() {
        return super.getFileId();
    }

    // 2026-08-14 (#qubIT-Fenix-8024)
    //
    // The property fileDescriptorId is used to get the file id
    @Override
    @Deprecated
    public void setFileId(String fileId) {
        super.setFileId(fileId);
    }

    // 2026-08-14 (#qubIT-Fenix-8024)
    //
    // The property fileDescriptorId is used to get the file id
    @Override
    @Deprecated
    public TreasuryFile getTreasuryFile() {
        return super.getTreasuryFile();
    }

    // 2026-08-14 (#qubIT-Fenix-8024)
    //
    // The property fileDescriptorId is used to get the file id
    @Override
    @Deprecated
    public void setTreasuryFile(TreasuryFile treasuryFile) {
        super.setTreasuryFile(treasuryFile);
    }

    @Override
    public byte[] getContent() {
        return getFileDescriptor().getContent();
    }

    @Override
    public long getSize() {
        return getFileDescriptor().getSize();
    }

    @Override
    public String getFilename() {
        return getFileDescriptor().getName();
    }

    @Override
    public String getContentType() {
        return getFileDescriptor().getContentType();
    }

    @Override
    public InputStream getStream() {
        return getFileDescriptor().getReadStream();
    }

    private FileDescriptor getFileDescriptor() {
        return ServiceProvider.getService(FileManager.class).getFileDescriptor(getFileDescriptorId());
    }

}
