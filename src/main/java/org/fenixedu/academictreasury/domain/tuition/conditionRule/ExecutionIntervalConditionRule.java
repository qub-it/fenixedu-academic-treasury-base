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

import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionInterval;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionAnnotation;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionRule;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlan;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;

@TuitionConditionAnnotation(ExecutionIntervalConditionRule.BUNDLE_NAME)
public class ExecutionIntervalConditionRule extends ExecutionIntervalConditionRule_Base {

    public static final String BUNDLE_NAME = AcademicTreasuryConstants.BUNDLE;

    public ExecutionIntervalConditionRule() {
        super();
    }

    @Override
    public boolean containsRule(TuitionConditionRule tuitionConditionRule) {
        if (!(tuitionConditionRule instanceof ExecutionIntervalConditionRule)) {
            return false;
        }
        ExecutionIntervalConditionRule rule = (ExecutionIntervalConditionRule) tuitionConditionRule;
        return getExecutionIntervalSet().containsAll(rule.getExecutionIntervalSet());
    }

    @Override
    public boolean isValidTo(Registration registration, ExecutionYear executionYear, Enrolment enrolment) {
        Set<ExecutionInterval> collect =
                registration.getEnrolments(executionYear).stream().map(e -> e.getExecutionPeriod()).collect(Collectors.toSet());
        if (collect.size() != 1) {
            return false;
        }
        return getExecutionIntervalSet().contains(collect.iterator().next());
    }

    @Override
    public boolean checkRules() {
        if (getExecutionIntervalSet() == null || getExecutionIntervalSet().isEmpty()) {
            throw new DomainException(i18n(
                    "org.fenixedu.academictreasury.domain.tuition.conditionRule.executionIntervalConditionRule.executionIntervalSet.cannotBeEmpty"));
        }
        return true;
    }

    @Override
    public String getDescription() {
        return getExecutionIntervalSet().stream().sorted(ExecutionInterval.COMPARATOR_BY_BEGIN_DATE).map(c -> c.getName())
                .collect(Collectors.joining(", "));
    }

    @Override
    protected String getBundle() {
        return BUNDLE_NAME;
    }

    @Override
    public void delete() {
        getExecutionIntervalSet().clear();
        setTuitionPaymentPlan(null);
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Override
    public TuitionConditionRule copyToPlan(TuitionPaymentPlan tuitionPaymentPlan) {
        IAcademicTreasuryPlatformDependentServices implementation =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();

        ExecutionIntervalConditionRule result = new ExecutionIntervalConditionRule();
        result.setTuitionPaymentPlan(tuitionPaymentPlan);
        Set<ExecutionInterval> executionIntervalOfTuitionPaymentPlanExecutionYear = getExecutionIntervalSet()
                .stream().map(c -> tuitionPaymentPlan.getExecutionYear()
                        .getExecutionSemesterFor(c.getAcademicInterval().getAcademicSemesterOfAcademicYear()))
                .collect(Collectors.toSet());

        result.getExecutionIntervalSet().addAll(executionIntervalOfTuitionPaymentPlanExecutionYear);
        return result;
    }

    @Override
    public TuitionConditionRule duplicate() {
        ExecutionIntervalConditionRule result = new ExecutionIntervalConditionRule();
        getExecutionIntervalSet().forEach(c -> result.addExecutionInterval(c));
        return result;
    }

    @Override
    public void fillRuleFromImporter(String string) {
        String[] split = string.split("\\|");
        for (String s : split) {
            // TODO: For ISCTE
            ExecutionInterval value = null; // ExecutionInterval.getExecutionInterval(s);
            if (value == null) {
                throw new IllegalArgumentException();
            }
            addExecutionInterval(value);
        }
    }

}
