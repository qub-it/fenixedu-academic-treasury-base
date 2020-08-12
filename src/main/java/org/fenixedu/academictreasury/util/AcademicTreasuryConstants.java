package org.fenixedu.academictreasury.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

import org.fenixedu.academictreasury.domain.serviceRequests.ITreasuryServiceRequest;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;

public class AcademicTreasuryConstants {

    private static final int SCALE = 20;

    public static final int EXTENDED_CURRENCY_DECIMAL_DIGITS = 4;

    public static final String BUNDLE = "resources.AcademicTreasuryResources";

    public static final BigDecimal HUNDRED_PERCENT = new BigDecimal("100.00");

    public static final BigDecimal DEFAULT_QUANTITY = new BigDecimal(1);

    public static final String DATE_FORMAT = "dd/MM/yyyy";

    public static final String DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

    public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy/MM/dd";

    public static final String DATE_TIME_FORMAT_YYYY_MM_DD = "yyyy/MM/dd HH:mm:ss";

    public static final String STANDARD_DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";

    // HACK: org.joda.time.Interval does not allow open end dates so use this date in the future
    public static final DateTime INFINITY_DATE = new DateTime().plusYears(500);

    public static final TreasuryTupleDataSourceBean SELECT_OPTION =
            new TreasuryTupleDataSourceBean("", academicTreasuryBundle("label.TreasuryTupleDataSourceBean.select.description"));

    
    // @formatter:off
    /* *************
     * COUNTRY UTILS
     * *************
     * */
    // @formatter:on
    
    public static final Locale DEFAULT_LANGUAGE = new Locale("PT");
    public static final String DEFAULT_COUNTRY = "PT";

    public static final Locale ENGLISH_LANGUAGE = new Locale("EN");
   
    @Deprecated
    // Use TreasuryConstants
    public static boolean isForeignLanguage(final Locale language) {
        return !language.getLanguage().equals(DEFAULT_LANGUAGE.getLanguage());
    }
    
    @Deprecated
    // Use TreasuryConstants
    public static boolean isDefaultCountry(final String country) {
        if (Strings.isNullOrEmpty(country)) {
            return false;
        }

        return DEFAULT_COUNTRY.equals(country.toUpperCase());
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

    public static String academicTreasuryBundle(final String key, final String... args) {
        return TreasuryPlataformDependentServicesFactory.implementation().bundle(AcademicTreasuryConstants.BUNDLE, key, args);
    }
    
    public static String academicTreasuryBundle(final Locale locale, final String key, final String... args) {
        return TreasuryPlataformDependentServicesFactory.implementation().bundle(locale, AcademicTreasuryConstants.BUNDLE, key, args);
    }

    public static LocalizedString academicTreasuryBundleI18N(final String key, final String... args) {
        return TreasuryPlataformDependentServicesFactory.implementation().bundleI18N(AcademicTreasuryConstants.BUNDLE, key, args);
    }
    
    public static Set<Locale> supportedLocales() {
        return TreasuryPlataformDependentServicesFactory.implementation().availableLocales();
    }

    // @formatter: off
    /**************
     * DATE UTILS *
     **************/
    // @formatter: on

    public static boolean isDateBetween(final LocalDate beginDate, final LocalDate endDate, final LocalDate when) {
        return new Interval(beginDate.toDateTimeAtStartOfDay(),
                endDate != null ? endDate.toDateTimeAtStartOfDay().plusDays(1).minusSeconds(1) : INFINITY_DATE)
                        .contains(when.toDateTimeAtStartOfDay());
    }

    public static boolean isDateBetween(final LocalDate beginDate, final LocalDate endDate, final DateTime when) {
        return new Interval(beginDate.toDateTimeAtStartOfDay(),
                endDate != null ? endDate.toDateTimeAtStartOfDay().plusDays(1).minusSeconds(1) : INFINITY_DATE).contains(when);
    }

    public static Integer getNumberOfUnits(ITreasuryServiceRequest request) {
        if (request.hasNumberOfUnits()) {
            return request.getNumberOfUnits();
        } else if (request.hasNumberOfDays()) {
            return request.getNumberOfDays();
        } else {
            Integer numberOfApprovedExtraCurriculum =
                    request.hasApprovedExtraCurriculum() ? request.getApprovedExtraCurriculum().size() : 0;
            Integer numberOfApprovedStandaloneCurriculum =
                    request.hasApprovedStandaloneCurriculum() ? request.getApprovedStandaloneCurriculum().size() : 0;
            Integer numberOfApprovedEnrolments = request.hasApprovedEnrolments() ? request.getApprovedEnrolments().size() : 0;
            Integer numberOfCurriculum = request.hasCurriculum() ? request.getCurriculum().size() : 0;

            int numberOfEnrolments = request.hasEnrolmentsByYear() ? request.getEnrolmentsByYear().size() : 0;
            numberOfEnrolments += request.hasStandaloneEnrolmentsByYear() ? request.getStandaloneEnrolmentsByYear().size() : 0;
            numberOfEnrolments +=
                    request.hasExtracurricularEnrolmentsByYear() ? request.getExtracurricularEnrolmentsByYear().size() : 0;

            return numberOfApprovedExtraCurriculum + numberOfApprovedStandaloneCurriculum + numberOfApprovedEnrolments
                    + numberOfCurriculum + numberOfEnrolments;
        }
    }
}
