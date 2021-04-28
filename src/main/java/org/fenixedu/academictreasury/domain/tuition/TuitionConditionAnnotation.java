package org.fenixedu.academictreasury.domain.tuition;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface TuitionConditionAnnotation {
    public String value();
}
