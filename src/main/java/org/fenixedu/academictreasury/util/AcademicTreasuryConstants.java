/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/> All rights reserved.
 *
 * Redistribution and use in source and binary forms, without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution. * Neither the name of Quorum Born IT nor
 * the names of its contributors may be used to endorse or promote products derived from this software without specific prior
 * written permission. * Universidade de Lisboa and its respective subsidiary Serviços Centrais da Universidade de Lisboa
 * (Departamento de Informática), hereby referred to as the Beneficiary, is the sole demonstrated end-user and ultimately the only
 * beneficiary of the redistributed binary form and/or source code. * The Beneficiary is entrusted with either the binary form,
 * the source code, or both, and by accepting it, accepts the terms of this License. * Redistribution of any binary form and/or
 * source code is only allowed in the scope of the Universidade de Lisboa FenixEdu(™)’s implementation projects. * This license
 * and conditions of redistribution of source code/binary can only be reviewed by the Steering Comittee of FenixEdu(™)
 * <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING,
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL “Quorum Born IT” BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.academictreasury.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.fenixedu.academic.domain.*;
import org.fenixedu.academic.domain.contacts.PartyContactType;
import org.fenixedu.academic.domain.contacts.PhysicalAddress;
import org.fenixedu.academic.domain.contacts.PhysicalAddressData;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.StatuteType;
import org.fenixedu.academictreasury.domain.serviceRequests.ITreasuryServiceRequest;
import org.fenixedu.treasury.domain.FinantialEntity;
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

    public static final TreasuryTupleDataSourceBean SELECT_OPTION;

    static {
        SELECT_OPTION = new TreasuryTupleDataSourceBean();
        SELECT_OPTION.setId("");

        try {
            SELECT_OPTION.setText(academicTreasuryBundle("label.TreasuryTupleDataSourceBean.select.description"));
        } catch (Throwable t) {
            t.printStackTrace();

            SELECT_OPTION.setText("Select Option");
        }
    }

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
        return TreasuryPlataformDependentServicesFactory.implementation()
                .bundle(locale, AcademicTreasuryConstants.BUNDLE, key, args);
    }

    public static LocalizedString academicTreasuryBundleI18N(final String key, final String... args) {
        return TreasuryPlataformDependentServicesFactory.implementation().bundleI18N(AcademicTreasuryConstants.BUNDLE, key, args);
    }

    // @formatter: off

    /**************
     * DATE UTILS *
     **************/
    // @formatter: on
    public static boolean isDateBetween(final LocalDate beginDate, final LocalDate endDate, final LocalDate when) {
        return new Interval(beginDate.toDateTimeAtStartOfDay(),
                endDate != null ? endDate.toDateTimeAtStartOfDay().plusDays(1).minusSeconds(1) : INFINITY_DATE).contains(
                when.toDateTimeAtStartOfDay());
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

            return numberOfApprovedExtraCurriculum + numberOfApprovedStandaloneCurriculum + numberOfApprovedEnrolments + numberOfCurriculum + numberOfEnrolments;
        }
    }

    /*
     * This method returns the Finantial Entity responsible for the degree.
     *
     * This method is used to get the academic tariffs associated with the Finantial Entity.
     * Also it is used to get the finantial institution, in order to know the debt account associated
     * with the customer.
     *
     * For tuitions it is different, in the sense that the TuitionPaymentPlan is obtained with
     * DegreeCurricularPlan. With the TuitionPaymentPlan we get the FinantialEntity.
     *
     */
    public static FinantialEntity getFinantialEntityOfDegree(Degree degree, LocalDate when) {
        return getFinantialEntityOfUnit(degree.getUnit(), when);
    }

    public static FinantialEntity getFinantialEntityOfUnit(Unit unit, LocalDate when) {
        // Look at the organizational structure

        if (unit == null) {
            return null;
        }

        List<FinantialEntity> candidateFinantialEntities =
                unit.getAllParentUnits().stream().map(parent -> parent.getFinantialEntity()).filter(Objects::nonNull)
                        .sorted(FinantialEntity.COMPARE_BY_NAME).collect(Collectors.toList());

        if (candidateFinantialEntities.size() == 1) {
            // There is no ambiguity. The degree descendent of one unit
            // associated with the finantial entity

            return candidateFinantialEntities.iterator().next();
        } else if (candidateFinantialEntities.size() > 1) {
            // The degree is descendent of more than one unit
            // associated with the finantial entity
            //
            // We need to untie and find which finantial entity
            // is responsible for finantial entity

            // TODO: Find with a specific accountability type?
            // It is not desirable to return the first ordered
            // alphabetically

            return candidateFinantialEntities.iterator().next();
        }

        return null;
    }

    public static Set<Degree> readDegrees(FinantialEntity finantialEntity) {
        if (finantialEntity.getUnit() != null) {
            return finantialEntity.getUnit().getAllSubUnits().stream() //
                    .filter(u -> u.isDegreeUnit()) //
                    .filter(u -> u.getDegree() != null) //
                    .map(u -> u.getDegree()) //
                    .collect(Collectors.toSet()); //
        }

        return Collections.emptySet();
    }

    public static Set<StatuteType> statutesTypesValidOnAnyExecutionSemesterFor(Registration registration,
            ExecutionInterval executionInterval) {
        return Sets.newHashSet(findStatuteTypes(registration, executionInterval));
    }

    static public Collection<StatuteType> findStatuteTypes(final Registration registration,
            final ExecutionInterval executionInterval) {

        if (executionInterval instanceof ExecutionYear) {
            return findStatuteTypesByYear(registration, (ExecutionYear) executionInterval);
        }

        return findStatuteTypesByChildInterval(registration, executionInterval);
    }

    static private Collection<StatuteType> findStatuteTypesByYear(final Registration registration,
            final ExecutionYear executionYear) {

        final Set<StatuteType> result = Sets.newHashSet();
        for (final ExecutionInterval executionInterval : executionYear.getExecutionPeriodsSet()) {
            result.addAll(findStatuteTypesByChildInterval(registration, executionInterval));
        }

        return result;
    }

    static private Collection<StatuteType> findStatuteTypesByChildInterval(final Registration registration,
            final ExecutionInterval executionInterval) {

        return registration.getStudent().getStudentStatutesSet().stream().filter(s -> s.isValidInExecutionInterval(
                        executionInterval) && (s.getRegistration() == null || s.getRegistration() == registration)).map(s -> s.getType())
                .collect(Collectors.toSet());
    }

    static public String getVisibleStatuteTypesDescription(final Registration registration,
            final ExecutionInterval executionInterval) {
        return findVisibleStatuteTypes(registration, executionInterval).stream().map(s -> s.getName().getContent()).distinct()
                .collect(Collectors.joining(", "));
    }

    static public Collection<StatuteType> findVisibleStatuteTypes(final Registration registration,
            final ExecutionInterval executionInterval) {
        return findStatuteTypes(registration, executionInterval).stream().filter(s -> s.getVisible()).collect(Collectors.toSet());
    }

    public static PhysicalAddress createPhysicalAddress(Person person, Country countryOfResidence, String districtOfResidence,
            String districtSubdivisionOfResidence, String areaCode, String address) {
        PhysicalAddressData data = new PhysicalAddressData();

        data.setAddress(address);
        data.setCountryOfResidence(countryOfResidence);
        data.setDistrictOfResidence(districtOfResidence);
        data.setDistrictSubdivisionOfResidence(districtSubdivisionOfResidence);
        data.setAreaCode(areaCode);

        final PhysicalAddress physicalAddress =
                PhysicalAddress.createPhysicalAddress(person, data, PartyContactType.PERSONAL, false);

        physicalAddress.setValid();

        return physicalAddress;
    }
}
