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
package org.fenixedu.academictreasury.domain.coursefunctioncost;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.dto.coursefunctioncost.CourseFunctionCostBean;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import pt.ist.fenixframework.FenixFramework;

import pt.ist.fenixframework.Atomic;

public class CourseFunctionCost extends CourseFunctionCost_Base {

    protected CourseFunctionCost(final DegreeCurricularPlan degreeCurricularPlan, final CompetenceCourse competenceCourse,
            final ExecutionYear executionYear, final BigDecimal functionCost) {
        super();

        setDomainRoot(FenixFramework.getDomainRoot());

        setDegreeCurricularPlan(degreeCurricularPlan);
        setCompetenceCourses(competenceCourse);
        setExecutionYear(executionYear);
        setFunctionCost(functionCost);

        checkRules();
    }

    private void checkRules() {
        if (getDegreeCurricularPlan() == null) {
            throw new AcademicTreasuryDomainException("error.CourseFunctionCost.degreeCurricularPlan.required");
        }

        if (getCompetenceCourses() == null) {
            throw new AcademicTreasuryDomainException("error.CourseFunctionCost.competenceCourses.required");
        }

        if (getExecutionYear() == null) {
            throw new AcademicTreasuryDomainException("error.CourseFunctionCost.executionYear.required");
        }

        if (getFunctionCost() == null) {
            throw new AcademicTreasuryDomainException("error.CourseFunctionCost.functionCost.required");
        }

        if (AcademicTreasuryConstants.isNegative(getFunctionCost())) {
            throw new AcademicTreasuryDomainException("error.CourseFunctionCost.functionCost.must.be.positive.or.zero");
        }
    }

    @Atomic
    public void delete() {

        setDomainRoot(null);
        setExecutionYear(null);
        setDegreeCurricularPlan(null);
        setCompetenceCourses(null);

        deleteDomainObject();
    }

    public static Stream<CourseFunctionCost> findAll() {
        return FenixFramework.getDomainRoot().getCourseFunctionCostsSet().stream();
    }

    public static Stream<CourseFunctionCost> find(final ExecutionYear executionYear, final CurricularCourse curricularCourse) {
        return find(curricularCourse.getDegreeCurricularPlan(), curricularCourse.getCompetenceCourse(), executionYear);
    }

    public static Stream<CourseFunctionCost> find(final DegreeCurricularPlan degreeCurricularPlan,
            final CompetenceCourse competenceCourse, final ExecutionYear executionYear) {
        return findAll().filter(c -> c.getCompetenceCourses() == competenceCourse)
                .filter(l -> l.getExecutionYear() == executionYear && l.getDegreeCurricularPlan() == degreeCurricularPlan);
    }

    public static Optional<CourseFunctionCost> findUnique(final ExecutionYear executionYear,
            final CurricularCourse curricularCourse) {
        return find(executionYear, curricularCourse).findFirst();
    }

    public static Optional<CourseFunctionCost> findUnique(final DegreeCurricularPlan degreeCurricularPlan,
            final CompetenceCourse competenceCourse, final ExecutionYear executionYear) {
        return find(degreeCurricularPlan, competenceCourse, executionYear).findFirst();
    }

    @Atomic
    public static CourseFunctionCost create(final ExecutionYear executionYear, final CurricularCourse curricularCourse,
            final BigDecimal functionCost) {
        return new CourseFunctionCost(curricularCourse.getDegreeCurricularPlan(), curricularCourse.getCompetenceCourse(),
                executionYear, functionCost);
    }

    @Atomic
    public static CourseFunctionCost create(CourseFunctionCostBean bean) {
        return new CourseFunctionCost(bean.getDegreeCurricularPlan(), bean.getCompetenceCourses(), bean.getExecutionYear(),
                bean.getFunctionCost());
    }

}
