package org.fenixedu.academictreasury.domain.tuition;

import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;

import pt.ist.fenixframework.FenixFramework;

public class TuitionDebtPostingType extends TuitionDebtPostingType_Base {

    public TuitionDebtPostingType() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public void delete() {
        setDomainRoot(null);
        setFinantialEntity(null);
        
        super.deleteDomainObject();
    }

    public static Stream<TuitionDebtPostingType> findAll() {
        return FenixFramework.getDomainRoot().getTuitionDebtPostingTypesSet().stream();
    }

    public static Stream<TuitionDebtPostingType> find(FinantialEntity finantialEntity) {
        return finantialEntity.getTuitionDebtPostingTypesSet().stream();
    }

    public static TuitionDebtPostingType create(FinantialEntity finantialEntity, LocalizedString name) {
        TuitionDebtPostingType result = new TuitionDebtPostingType();

        result.setFinantialEntity(finantialEntity);
        result.setName(name);

        return result;
    }

}
