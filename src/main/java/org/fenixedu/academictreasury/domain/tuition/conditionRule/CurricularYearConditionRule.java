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
package org.fenixedu.academictreasury.domain.tuition.conditionRule;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CurricularYear;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionAnnotation;
import org.fenixedu.academictreasury.domain.tuition.TuitionConditionRule;
import org.fenixedu.academictreasury.dto.tariff.TuitionPaymentPlanBean;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;

@TuitionConditionAnnotation(CurricularYearConditionRule.BUNDLE_NAME)
public class CurricularYearConditionRule extends CurricularYearConditionRule_Base {

    public static final String BUNDLE_NAME = AcademicTreasuryConstants.BUNDLE;

    public CurricularYearConditionRule() {
        super();
    }

    @Override
    public boolean containsRule(TuitionConditionRule tuitionConditionRule) {
        if (!(tuitionConditionRule instanceof CurricularYearConditionRule)) {
            return false;
        }
        CurricularYearConditionRule rule = (CurricularYearConditionRule) tuitionConditionRule;
        return getCurricularYearSet().containsAll(rule.getCurricularYearSet());
    }

    @Override
    public boolean isValidTo(Registration registration, ExecutionYear executionYear, Enrolment enrolment) {
        return getCurricularYearSet().contains(CurricularYear.readByYear(registration.getCurricularYear(executionYear)));
    }

    @Override
    public boolean checkRules() {
        if (getCurricularYearSet() == null || getCurricularYearSet().isEmpty()) {
            throw new DomainException(
                    i18n("org.fenixedu.academictreasury.domain.tuition.conditionRule.curricularYearConditionRule.curricularYearSet.cannotBeEmpty"));
        }
        return true;
    }

    @Override
    protected String getBundle() {
        return BUNDLE_NAME;
    }

    @Override
    public String getDescription() {
        return getCurricularYearSet().stream().sorted(CurricularYear.CURRICULAR_YEAR_COMPARATORY_BY_YEAR)
                .map(c -> c.getYear().toString()).collect(Collectors.joining(", "));
    }

    @Override
    public void delete() {
        getCurricularYearSet().clear();
        setTuitionPaymentPlan(null);
        setTuitionPaymentPlanCalculator(null);
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Override
    public TuitionConditionRule duplicate() {
        CurricularYearConditionRule result = new CurricularYearConditionRule();
        getCurricularYearSet().forEach(c -> result.addCurricularYear(c));
        return result;
    }

    @Override
    public void fillRuleFromImporter(TuitionPaymentPlanBean bean) {
        String string = bean.getImporterRules().get(this.getClass());
        String[] split = string.split("\\|");
        for (String s : split) {
            CurricularYear readByYear =
                    readAllCurricularYearsSet().stream().filter(c -> c.getYear().toString().equals(s)).findFirst()
                            .orElse(null); //CurricularYear.readByYear(Integer.getInteger(s));

            if (readByYear == null) {
                throw new AcademicTreasuryDomainException("error.CurricularYearConditionRule.curricularYear.invalid", s);
            }

            addCurricularYear(readByYear);
        }
    }

    private Set<CurricularYear> readAllCurricularYearsSet() {
        final Set<CurricularYear> result = new HashSet<>();

        for (int i = 1; i <= 10; i++) {
            if (CurricularYear.readByYear(i) == null) {
                return result;
            }

            result.add(CurricularYear.readByYear(i));
        }

        return result;
    }
}
