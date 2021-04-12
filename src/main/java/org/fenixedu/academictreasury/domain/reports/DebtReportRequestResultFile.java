package org.fenixedu.academictreasury.domain.reports;



import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class DebtReportRequestResultFile extends DebtReportRequestResultFile_Base implements IGenericFile {

    public static final String CONTENT_TYPE = "application/octet-stream";
    
    
    protected DebtReportRequestResultFile() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
    }
    
    protected DebtReportRequestResultFile(final DebtReportRequest request, final byte[] content) {
        this();

        setDebtReportRequest(request);

        final String filename = academicTreasuryBundle("label.DebtReportRequestResultFile.ZIP.filename", 
                new DateTime().toString("YYYYMMddHHmmss"));

        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        
        services.createFile(this, filename, CONTENT_TYPE, content);

        checkRules();
    }

    private void checkRules() {
        if(getDebtReportRequest() == null) {
            throw new AcademicTreasuryDomainException("error.DebtReportRequestResultFile.debtReportRequest.required");
        }
    }
    
    public String getFileSizeWithUnitDescription() {
        BigDecimal filesizeMb = getFilesizeMb();
        if(TreasuryConstants.isGreaterOrEqualThan(filesizeMb, BigDecimal.ONE)) {
            return filesizeMb.toString() + " Mb";
        }
        
        return getFilesizeKb() + " Kb";
    }
    
    public BigDecimal getFilesizeKb() {
        return (new BigDecimal(getSize()).setScale(4, RoundingMode.DOWN)
                .divide(new BigDecimal(1024)))
                .setScale(1, RoundingMode.DOWN);
    }

    public BigDecimal getFilesizeMb() {
        return (new BigDecimal(getSize()).setScale(4, RoundingMode.DOWN)
                .divide(new BigDecimal(1024)))
                .divide(new BigDecimal(1024))
                .setScale(1, RoundingMode.DOWN);
    }

    @Override
    public boolean isAccessible(final String username) {
        return TreasuryAccessControlAPI.isBackOfficeMember(username);
    }
    
    @Override
    public void delete() {
        
        setDomainRoot(null);
        
        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        services.deleteFile(this);
        
        super.deleteDomainObject();
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on
    
    
    public static DebtReportRequestResultFile create(final DebtReportRequest request, final byte[] content) {
        return new DebtReportRequestResultFile(request, content);
    }

    public static Stream<DebtReportRequestResultFile> findAll() {
        return FenixFramework.getDomainRoot().getDebtReportRequestResultFilesSet().stream();
    }
    
}
