package org.fenixedu.academictreasury.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import org.fenixedu.bennu.TupleDataSourceBean;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

public class Constants {

    private static final int SCALE = 20;

    public static final int EXTENDED_CURRENCY_DECIMAL_DIGITS = 4;

    public static final String BUNDLE = "resources.AcademicTreasuryResources";

    public static final BigDecimal HUNDRED_PERCENT = new BigDecimal("100.00");

    public static final BigDecimal DEFAULT_QUANTITY = new BigDecimal(1);

    public static final String DATE_FORMAT = "dd/MM/yyyy";

    public static final String DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

    public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy/MM/dd";

    public static final String DATE_TIME_FORMAT_YYYY_MM_DD = "yyyy/MM/dd HH:mm:ss";
    
    // HACK: org.joda.time.Interval does not allow open end dates so use this date in the future
    public static final DateTime INFINITY_DATE = new DateTime().plusYears(500);

    public static final TupleDataSourceBean SELECT_OPTION = new TupleDataSourceBean("", BundleUtil.getString(Constants.BUNDLE,
            "label.TupleDataSourceBean.select.description"));

    public static final Locale DEFAULT_LANGUAGE = new Locale("PT");

    public static boolean isForeignLanguage(final Locale language) {
        return !language.getLanguage().equals(DEFAULT_LANGUAGE.getLanguage());
    }

    // @formatter: off
    /**************
     * MATH UTILS *
     **************/
    // @formatter: on

    public static boolean isNegative(final BigDecimal value) {
        return !isZero(value) && !isPositive(value);
    }

    public static boolean isZero(final BigDecimal value) {
        return BigDecimal.ZERO.compareTo(value) == 0;
    }

    public static boolean isPositive(final BigDecimal value) {
        return BigDecimal.ZERO.compareTo(value) < 0;
    }

    public static boolean isGreaterThan(final BigDecimal v1, final BigDecimal v2) {
        return v1.compareTo(v2) > 0;
    }

    public static BigDecimal defaultScale(final BigDecimal v) {
        return v.setScale(20, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal divide(final BigDecimal a, BigDecimal b) {
        return a.divide(b, SCALE, RoundingMode.HALF_EVEN);
    }

    // @formatter: off
    /**********
     * BUNDLE *
     **********/
    // @formatter: on

    public static String bundle(final String key, final String... args) {
        return BundleUtil.getString(Constants.BUNDLE, key, args);
    }

    public static LocalizedString bundleI18N(final String key, final String... args) {
        return BundleUtil.getLocalizedString(Constants.BUNDLE, key, args);
    }

    // @formatter: off
    /**************
     * DATE UTILS *
     **************/
    // @formatter: on

    public static boolean isDateBetween(final LocalDate beginDate, final LocalDate endDate, final LocalDate when) {
        return new Interval(beginDate.toDateTimeAtStartOfDay(), endDate != null ? endDate.toDateTimeAtStartOfDay()
                .minusSeconds(1) : INFINITY_DATE).contains(when.toDateTimeAtStartOfDay());
    }

    public static boolean isDateBetween(final LocalDate beginDate, final LocalDate endDate, final DateTime when) {
        return new Interval(beginDate.toDateTimeAtStartOfDay(), endDate != null ? endDate.toDateTimeAtStartOfDay()
                .minusSeconds(1) : INFINITY_DATE).contains(when);
    }
}
