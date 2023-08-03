package org.fenixedu.academictreasury.domain.tuition.exemptions;

import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;

import pt.ist.fenixframework.FenixFramework;

public class StatuteExemptionByIntervalMapEntry extends StatuteExemptionByIntervalMapEntry_Base {

    public StatuteExemptionByIntervalMapEntry() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public StatuteExemptionByIntervalMapEntry(FinantialEntity finantialEntity, ExecutionInterval executionInterval,
            StatuteType statuteType, TreasuryExemptionType treasuryExemptionType) {
        this();

        setFinantialEntity(finantialEntity);
        setExecutionInterval(executionInterval);
        setStatuteType(statuteType);
        setTreasuryExemptionType(treasuryExemptionType);

        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new IllegalStateException("error.StatuteExemptionByIntervalMapEntry.domainRoot.required");
        }

        if (getFinantialEntity() == null) {
            throw new IllegalStateException("error.StatuteExemptionByIntervalMapEntry.finantialEntity.required");
        }

        if (getExecutionInterval() == null) {
            throw new IllegalStateException("error.StatuteExemptionByIntervalMapEntry.executionInterval.required");
        }

        if (getStatuteType() == null) {
            throw new IllegalStateException("error.StatuteExemptionByIntervalMapEntry.statuteType.required");
        }

        if (getTreasuryExemptionType() == null) {
            throw new IllegalStateException("error.StatuteExemptionByIntervalMapEntry.treasuryExemptionType.required");
        }

        if (find(getFinantialEntity(), getExecutionInterval(), getStatuteType(), getTreasuryExemptionType()).count() > 1) {
            throw new IllegalStateException(
                    AcademicTreasuryConstants.academicTreasuryBundle("error.StatuteExemptionByIntervalMapEntry.already.mapped"));
        }
    }

    public static Stream<StatuteExemptionByIntervalMapEntry> find(FinantialEntity finantialEntity,
            ExecutionInterval executionInterval) {
        return executionInterval.getStatuteExemptionByIntervalMapEntrySet().stream()
                .filter(e -> e.getFinantialEntity() == finantialEntity);
    }

    public static Stream<StatuteExemptionByIntervalMapEntry> find(FinantialEntity finantialEntity,
            ExecutionInterval executionInterval, StatuteType statuteType, TreasuryExemptionType treasuryExemptionType) {
        return find(finantialEntity, executionInterval).filter(s -> s.getStatuteType() == statuteType)
                .filter(s -> s.getTreasuryExemptionType() == treasuryExemptionType);
    }

}
