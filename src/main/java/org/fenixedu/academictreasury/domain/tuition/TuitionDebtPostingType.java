package org.fenixedu.academictreasury.domain.tuition;

import java.util.stream.Stream;

import org.fenixedu.treasury.domain.FinantialEntity;

import pt.ist.fenixframework.FenixFramework;

public class TuitionDebtPostingType extends TuitionDebtPostingType_Base {

    public TuitionDebtPostingType() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public static Stream<TuitionDebtPostingType> findAll() {
        return FenixFramework.getDomainRoot().getTuitionDebtPostingTypesSet().stream();
    }

    public static Stream<TuitionDebtPostingType> find(FinantialEntity finantialEntity) {
        return finantialEntity.getTuitionDebtPostingTypesSet().stream();
    }

}
