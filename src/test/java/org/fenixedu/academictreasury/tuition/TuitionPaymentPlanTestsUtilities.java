package org.fenixedu.academictreasury.tuition;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.organizationalStructure.*;
import org.fenixedu.academictreasury.base.BasicAcademicTreasuryUtils;
import org.fenixedu.treasury.domain.FinantialEntity;

public class TuitionPaymentPlanTestsUtilities {

    public static void startUp() {
        BasicAcademicTreasuryUtils.startup(() -> {
            Unit degreesUnit = Unit.readAllUnits().stream().filter(u -> "Degrees".equals(u.getName())).findFirst().get();

            FinantialEntity.findAll().iterator().next().setUnit(degreesUnit);
            Degree.findAll().filter(d -> d.getUnit() == null).forEach(d -> {
                Unit subUnit =
                        Unit.createNewUnit(PartyType.of(PartyTypeEnum.DEGREE_UNIT), d.getPresentationNameI18N(), d.getCode(),
                                degreesUnit, AccountabilityType.readByType(AccountabilityTypeEnum.ORGANIZATIONAL_STRUCTURE));
                d.setUnit(subUnit);
            });

            return null;
        });
    }

}