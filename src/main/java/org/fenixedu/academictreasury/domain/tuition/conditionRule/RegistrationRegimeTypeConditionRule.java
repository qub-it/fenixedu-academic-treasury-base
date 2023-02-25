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
package org.fenixedu.academictreasury.domain.tuition.conditionRule;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionAnnotation;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionRule;
import org.fenixedu.academictreasury.dto.tariff.TuitionPaymentPlanBean;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;

@TuitionConditionAnnotation(RegistrationRegimeTypeConditionRule.BUNDLE_NAME)
public class RegistrationRegimeTypeConditionRule extends RegistrationRegimeTypeConditionRule_Base {

    public static final String BUNDLE_NAME = AcademicTreasuryConstants.BUNDLE;

    public RegistrationRegimeTypeConditionRule() {
        super();
    }

    @Override
    public boolean containsRule(TuitionConditionRule tuitionConditionRule) {
        if (!(tuitionConditionRule instanceof RegistrationRegimeTypeConditionRule)) {
            return false;
        }
        RegistrationRegimeTypeConditionRule rule = (RegistrationRegimeTypeConditionRule) tuitionConditionRule;
        return getRegistrationRegimeTypes().containsAll(rule.getRegistrationRegimeTypes());
    }

    @Override
    public boolean isValidTo(Registration registration, ExecutionInterval executionYear, Enrolment enrolment) {
        final IAcademicTreasuryPlatformDependentServices academicTreasuryServices =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        RegistrationRegimeType registrationRegimeType =
                academicTreasuryServices.registrationRegimeType(registration, executionYear);
        return getRegistrationRegimeTypes().contains(registrationRegimeType);
    }

    public Set<RegistrationRegimeType> getRegistrationRegimeTypes() {
        Set<RegistrationRegimeType> result = new RegimeHashSet(this);
        getRegimeTypesConverted().forEach(r -> result.add(r));
        return result;
    }

    private Set<RegistrationRegimeType> getRegimeTypesConverted() {
        Set<RegistrationRegimeType> result = new HashSet<RegistrationRegimeType>();
        if (getRegistrationRegimeTypesSerialized() == null) {
            return result;
        }
        String[] types = getRegistrationRegimeTypesSerialized().split(",");
        for (String type : types) {
            try {
                result.add(RegistrationRegimeType.valueOf(type));
            } catch (IllegalArgumentException e) {
                continue;
            }
        }
        return result;
    }

    public void addRegistrationRegimeTypes(RegistrationRegimeType type) {
        Set<RegistrationRegimeType> registrationRegimeTypes = getRegimeTypesConverted();
        registrationRegimeTypes.add(type);
        setRegistrationRegimeTypesSerialized(
                registrationRegimeTypes.stream().map(t -> t.name()).collect(Collectors.joining(",")));
    }

    public void removeRegistrationRegimeTypes(RegistrationRegimeType type) {
        Set<RegistrationRegimeType> registrationRegimeTypes = getRegimeTypesConverted();
        registrationRegimeTypes.remove(type);
        setRegistrationRegimeTypesSerialized(
                registrationRegimeTypes.stream().map(t -> t.name()).collect(Collectors.joining(",")));
    }

    @Override
    public boolean checkRules() {
        if (getRegistrationRegimeTypesSerialized() == null || getRegistrationRegimeTypesSerialized().isEmpty()) {
            throw new DomainException(i18n(
                    "org.fenixedu.academictreasury.domain.tuition.conditionRule.RegistrationRegimeTypeConditionRule.RegistrationRegimeTypes.cannotBeEmpty"));
        }
        return true;
    }

    @Override
    public String getDescription() {
        return getRegistrationRegimeTypes().stream().map(c -> c.getLocalizedName()).collect(Collectors.joining(", "));
    }

    @Override
    protected String getBundle() {
        return BUNDLE_NAME;
    }

    @Override
    public void delete() {
        setTuitionPaymentPlan(null);
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Override
    public TuitionConditionRule duplicate() {
        RegistrationRegimeTypeConditionRule result = new RegistrationRegimeTypeConditionRule();
        getRegistrationRegimeTypes().forEach(c -> result.addRegistrationRegimeTypes(c));
        return result;
    }

    private static class RegimeHashSet extends HashSet<RegistrationRegimeType> {
        private RegistrationRegimeTypeConditionRule rule;

        public RegimeHashSet(RegistrationRegimeTypeConditionRule registrationRegimeTypeConditionRule) {
            this.rule = registrationRegimeTypeConditionRule;
        }

        @Override
        public boolean addAll(Collection<? extends RegistrationRegimeType> c) {
            for (RegistrationRegimeType regime : c) {
                rule.addRegistrationRegimeTypes(regime);
            }
            return super.addAll(c);
        }
    }

    @Override
    public void fillRuleFromImporter(TuitionPaymentPlanBean bean) {
        String string = bean.getImporterRules().get(this.getClass());
        String[] split = string.split("\\|");
        for (String s : split) {
            RegistrationRegimeType value = Arrays.asList(RegistrationRegimeType.values()).stream()
                    .filter(r -> r.getLocalizedName().equals(s)).findFirst().orElse(null);

            if (value == null) {
                throw new AcademicTreasuryDomainException("error.RegistrationRegimeTypeConditionRule.registrationRegime.invalid",
                        s);
            }

            addRegistrationRegimeTypes(value);
        }
    }

}
