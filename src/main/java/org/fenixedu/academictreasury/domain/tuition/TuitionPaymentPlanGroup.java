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
package org.fenixedu.academictreasury.domain.tuition;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tuition.calculators.TuitionPaymentPlanCalculator;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.academictreasury.util.LocalizedStringUtil;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class TuitionPaymentPlanGroup extends TuitionPaymentPlanGroup_Base {

    public static final Comparator<TuitionPaymentPlanGroup> COMPARE_BY_NAME = (o1, o2) -> {
        int c = o1.getName().getContent().compareTo(o2.getName().getContent());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    protected TuitionPaymentPlanGroup() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected TuitionPaymentPlanGroup(final String code, final LocalizedString name, boolean forRegistration,
            boolean forStandalone, boolean forExtracurricular, final Product currentProduct) {
        this();
        setCode(code);
        setName(name);

        setForRegistration(forRegistration);
        setForStandalone(forStandalone);
        setForExtracurricular(forExtracurricular);
        setCurrentProduct(currentProduct);
        setBypassInstallmentNameIfSingleInstallmentApplied(false);

        checkRules();
    }

    private void checkRules() {
        if (Strings.isNullOrEmpty(getCode())) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.name.required");
        }

        if (!(isForRegistration() ^ isForStandalone() ^ isForExtracurricular())) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.only.one.type.supported");
        }

        if (findDefaultGroupForRegistration().count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.for.registration.already.exists");
        }

        if (findDefaultGroupForStandalone().count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.for.standalone.already.exists");
        }

        if (findDefaultGroupForExtracurricular().count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.for.extracurricular.already.exists");
        }

        if (findByCode(getCode()).count() > 1) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.code.already.exists");
        }
    }

    @Atomic
    public void edit(String code, LocalizedString name, boolean forRegistration, boolean forStandalone,
            boolean forExtracurricular, Product currentProduct, boolean bypassInstallmentNameIfSingleInstallmentApplied,
            boolean useCustomDebitEntryDescriptionFormat, LocalizedString installmentDebitEntryDescriptionFormat,
            LocalizedString oneInstallmentDebitEntryDescriptionFormat) {

        super.setCode(code);
        super.setName(name);
        super.setForRegistration(forRegistration);
        super.setForStandalone(forStandalone);
        super.setForExtracurricular(forExtracurricular);
        super.setCurrentProduct(currentProduct);
        super.setBypassInstallmentNameIfSingleInstallmentApplied(bypassInstallmentNameIfSingleInstallmentApplied);
        super.setUseCustomDebitEntryDescriptionFormat(useCustomDebitEntryDescriptionFormat);
        super.setInstallmentDebitEntryDescriptionFormat(installmentDebitEntryDescriptionFormat);
        super.setOneInstallmentDebitEntryDescriptionFormat(oneInstallmentDebitEntryDescriptionFormat);

        checkRules();
    }

    public boolean isForRegistration() {
        return getForRegistration();
    }

    public boolean isForStandalone() {
        return getForStandalone();
    }

    public boolean isForExtracurricular() {
        return getForExtracurricular();
    }

    public boolean isForImprovement() {
        return getForImprovement();
    }

    public boolean isBypassInstallmentNameIfSingleInstallmentApplied() {
        return getBypassInstallmentNameIfSingleInstallmentApplied();
    }

    public boolean isDeletable() {
        // ACFSILVA
        return getAcademicTreasuryEventSet().isEmpty() && getTuitionPaymentPlansSet().isEmpty();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new AcademicTreasuryDomainException("error.TuitionPaymentPlanGroup.delete.impossible");
        }

        setDomainRoot(null);

        super.deleteDomainObject();
    }

    public static Stream<TuitionPaymentPlanGroup> findAll() {
        return FenixFramework.getDomainRoot().getTuitionPaymentPlanGroupsSet().stream();
    }

    protected static Stream<TuitionPaymentPlanGroup> findDefaultGroupForRegistration() {
        return findAll().filter(t -> t.isForRegistration());
    }

    protected static Stream<TuitionPaymentPlanGroup> findDefaultGroupForStandalone() {
        return findAll().filter(t -> t.isForStandalone());
    }

    protected static Stream<TuitionPaymentPlanGroup> findDefaultGroupForImprovement() {
        return findAll().filter(t -> t.isForImprovement());
    }

    protected static Stream<TuitionPaymentPlanGroup> findDefaultGroupForExtracurricular() {
        return findAll().filter(t -> t.isForExtracurricular());
    }

    protected static Stream<TuitionPaymentPlanGroup> findByCode(final String code) {
        return findAll().filter(l -> StringNormalizer.normalize(l.getCode().toLowerCase())
                .equals(StringNormalizer.normalize(code).toLowerCase()));
    }

    public static Optional<TuitionPaymentPlanGroup> findUniqueDefaultGroupForRegistration() {
        return findDefaultGroupForRegistration().findFirst();
    }

    public static Optional<TuitionPaymentPlanGroup> findUniqueDefaultGroupForStandalone() {
        return findDefaultGroupForStandalone().findFirst();
    }

    public static Optional<TuitionPaymentPlanGroup> findUniqueDefaultGroupForExtracurricular() {
        return findDefaultGroupForExtracurricular().findFirst();
    }

    public static Optional<TuitionPaymentPlanGroup> findUniqueDefaultGroupForImprovement() {
        return findDefaultGroupForImprovement().findFirst();
    }

    @Atomic
    public static TuitionPaymentPlanGroup create(final String code, final LocalizedString name, boolean forRegistration,
            boolean forStandalone, boolean forExtracurricular, final Product currentProduct) {
        return new TuitionPaymentPlanGroup(code, name, forRegistration, forStandalone, forExtracurricular, currentProduct);
    }

    public Set<Class<? extends TuitionConditionRule>> getAllowedConditionRules() {
        if (getAllowedConditionRulesSerialized() == null) {
            return new HashSet<>();
        }
        String[] allowedConditionRulesSerialized = getAllowedConditionRulesSerialized().split(",");
        Set<Class<? extends TuitionConditionRule>> result = new HashSet<>();
        for (String allowedConditionRule : allowedConditionRulesSerialized) {
            try {
                result.add((Class<? extends TuitionConditionRule>) Class.forName(allowedConditionRule));
            } catch (ClassNotFoundException e) {
                continue;
            }
        }
        return result;
    }

    public void addAllowedConditionRules(Class<? extends TuitionConditionRule> allowedConditionRules) {
        Set<Class<? extends TuitionConditionRule>> classes = getAllowedConditionRules();
        classes.add(allowedConditionRules);
        setAllowedConditionRulesSerialized(classes.stream().map(clazz -> clazz.getName()).collect(Collectors.joining(",")));
    }

    public void removeAllowedConditionRules(Class<? extends TuitionConditionRule> allowedConditionRules) {
        Set<Class<? extends TuitionConditionRule>> classes = getAllowedConditionRules();
        classes.remove(allowedConditionRules);
        setAllowedConditionRulesSerialized(classes.stream().map(t -> t.getName()).collect(Collectors.joining(",")));
    }

    public Set<Class<? extends TuitionPaymentPlanCalculator>> getAllowedTuitionPaymentPlanCalculators() {
        if (getAllowedTuitionPaymentPlanCalculatorsSerialized() == null) {
            return new HashSet<>();
        }

        String[] allowedTuitionPaymentPlanCalculatorsSerialized = getAllowedTuitionPaymentPlanCalculatorsSerialized().split(",");
        Set<Class<? extends TuitionPaymentPlanCalculator>> result = new HashSet<>();
        for (String allowedCalculatedAmountCalculators : allowedTuitionPaymentPlanCalculatorsSerialized) {
            try {
                result.add((Class<? extends TuitionPaymentPlanCalculator>) Class.forName(allowedCalculatedAmountCalculators));
            } catch (ClassNotFoundException e) {
                continue;
            }
        }
        return result;
    }

    public void addAllowedTuitionPaymentPlanCalculators(
            Class<? extends TuitionPaymentPlanCalculator> allowedTuitionPaymentPlanCalculators) {
        Set<Class<? extends TuitionPaymentPlanCalculator>> classes = getAllowedTuitionPaymentPlanCalculators();
        classes.add(allowedTuitionPaymentPlanCalculators);
        setAllowedTuitionPaymentPlanCalculatorsSerialized(
                classes.stream().map(clazz -> clazz.getName()).collect(Collectors.joining(",")));
    }

    public void removeAllowedTuitionPaymentPlanCalculators(
            Class<? extends TuitionPaymentPlanCalculator> allowedTuitionPaymentPlanCalculators) {
        Set<Class<? extends TuitionPaymentPlanCalculator>> classes = getAllowedTuitionPaymentPlanCalculators();
        classes.remove(allowedTuitionPaymentPlanCalculators);
        setAllowedTuitionPaymentPlanCalculatorsSerialized(
                classes.stream().map(t -> t.getName()).collect(Collectors.joining(",")));
    }

    public Set<Class<? extends TuitionTariffCustomCalculator>> getAllowedCalculatedAmountCalculators() {
        if (getAllowedCalculatedAmountCalculatorsSerialized() == null) {
            return new HashSet<>();
        }
        String[] allowedCalculatedAmountCalculatorsSerialized = getAllowedCalculatedAmountCalculatorsSerialized().split(",");
        Set<Class<? extends TuitionTariffCustomCalculator>> result = new HashSet<>();
        for (String allowedCalculatedAmountCalculators : allowedCalculatedAmountCalculatorsSerialized) {
            try {
                result.add((Class<? extends TuitionTariffCustomCalculator>) Class.forName(allowedCalculatedAmountCalculators));
            } catch (ClassNotFoundException e) {
                continue;
            }
        }
        return result;
    }

    public void addAllowedCalculatedAmountCalculators(
            Class<? extends TuitionTariffCustomCalculator> allowedCalculatedAmountCalculators) {
        Set<Class<? extends TuitionTariffCustomCalculator>> classes = getAllowedCalculatedAmountCalculators();
        classes.add(allowedCalculatedAmountCalculators);
        setAllowedCalculatedAmountCalculatorsSerialized(
                classes.stream().map(clazz -> clazz.getName()).collect(Collectors.joining(",")));
    }

    public void removeAllowedCalculatedAmountCalculators(
            Class<? extends TuitionTariffCustomCalculator> allowedCalculatedAmountCalculators) {
        Set<Class<? extends TuitionTariffCustomCalculator>> classes = getAllowedCalculatedAmountCalculators();
        classes.remove(allowedCalculatedAmountCalculators);
        setAllowedCalculatedAmountCalculatorsSerialized(classes.stream().map(t -> t.getName()).collect(Collectors.joining(",")));
    }

    public LocalizedString buildDebitEntryDescription(TuitionInstallmentTariff installmentTariff, Registration registration,
            ExecutionYear executionYear) {
        if (getUseCustomDebitEntryDescriptionFormat()) {
            return formatInstallmentName(installmentTariff, registration, executionYear);
        } else {
            return defaultInstallmentName(installmentTariff, registration, executionYear);
        }
    }

    private LocalizedString formatInstallmentName(TuitionInstallmentTariff installmentTariff, Registration registration,
            ExecutionYear executionYear) {
        LocalizedString formatToUse = installmentFormatToUse(installmentTariff);

        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

        DegreeCurricularPlan degreeCurricularPlan =
                registration.getStudentCurricularPlan(executionYear).getDegreeCurricularPlan();

        LocalizedString productName = installmentTariff.getProduct().getName();
        String degreeCode = registration.getDegree().getCode();

        String executionYearQualifiedName = executionYear.getQualifiedName();

        LocalizedString result = treasuryServices.availableLocales().stream().map(locale -> {
            Map<String, String> valueMap = new HashMap<String, String>();

            // ANIL 2024-09-25 (#qubIT-Fenix-5846) 
            //
            // There is a mismatch between the names of the degree built in debit entry 
            // importer and created by tuition payment plan
            //
            // Now it is used in production, the solution is to have a dynamic property
            // to control the degree designation

            String degreePresentationName = degreeCurricularPlan.getDegree().getPresentationName(executionYear, locale);
            String degreeName = degreeCurricularPlan.getDegree().getNameI18N(executionYear).getContent(locale);

            valueMap.put("productName", StringUtils.isNotEmpty(productName.getContent(locale)) ? productName
                    .getContent(locale) : productName.getContent());
            valueMap.put("degreeCode", degreeCode);

            valueMap.put("degreePresentationName", degreePresentationName);
            valueMap.put("degreeName", degreeName);

            valueMap.put("executionYearName", executionYearQualifiedName);

            return new LocalizedString(locale, StrSubstitutor.replace(formatToUse.getContent(locale), valueMap));
        }).reduce((a, c) -> a.append(c)).get();

        return result;
    }

    private LocalizedString installmentFormatToUse(TuitionInstallmentTariff installmentTariff) {
        LocalizedString formatToUse = getInstallmentDebitEntryDescriptionFormat();

        if (getBypassInstallmentNameIfSingleInstallmentApplied()
                && installmentTariff.getTuitionPaymentPlan().getTuitionInstallmentTariffsSet().size() == 1) {
            formatToUse = getOneInstallmentDebitEntryDescriptionFormat();
        }
        return formatToUse;
    }

    private LocalizedString defaultInstallmentName(TuitionInstallmentTariff installmentTariff, Registration registration,
            ExecutionYear executionYear) {
        TuitionPaymentPlan tuitionPaymentPlan = installmentTariff.getTuitionPaymentPlan();

        final ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();
        String label = "label.TuitionInstallmentTariff.debitEntry.name.";

        if (isForRegistration()) {
            if (tuitionPaymentPlan.getTuitionInstallmentTariffsSet().size() == 1
                    && isBypassInstallmentNameIfSingleInstallmentApplied()) {
                label += "registration.one.installment";
            } else {
                label += "registration";
            }
        } else if (isForStandalone()) {
            label += "standalone";
        } else if (isForExtracurricular()) {
            label += "extracurricular";
        }
        DegreeCurricularPlan degreeCurricularPlan =
                registration.getStudentCurricularPlan(executionYear).getDegreeCurricularPlan();

        LocalizedString result = new LocalizedString();
        for (final Locale locale : treasuryServices.availableLocales()) {
            final String installmentName = AcademicTreasuryConstants.academicTreasuryBundle(locale, label,
                    String.valueOf(installmentTariff.getInstallmentOrder()),
                    degreeCurricularPlan.getDegree().getPresentationName(executionYear, locale),
                    executionYear.getQualifiedName());

            result = result.with(locale, installmentName);
        }

        return result;
    }

}
