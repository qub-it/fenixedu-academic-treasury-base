package org.fenixedu.academictreasury.domain.integration.tuitioninfo;

import static java.lang.String.format;
import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundleI18N;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import pt.ist.fenixframework.FenixFramework;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.settings.TreasurySettings;

import com.google.common.collect.Sets;

public class ERPTuitionInfoTypeAcademicEntry extends ERPTuitionInfoTypeAcademicEntry_Base {

    public ERPTuitionInfoTypeAcademicEntry() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    private ERPTuitionInfoTypeAcademicEntry(final ERPTuitionInfoType type, final DegreeType degreeType) {
        this();

        setErpTuitionInfoType(type);
        setDegreeType(degreeType);

        setForRegistration(true);
        setForStandalone(false);
        setForExtracurricular(false);

        checkRules();
    }

    private ERPTuitionInfoTypeAcademicEntry(final ERPTuitionInfoType type, final Degree degree) {
        this();

        setErpTuitionInfoType(type);
        setDegreeType(degree.getDegreeType());
        setDegree(degree);

        setForRegistration(true);
        setForStandalone(false);
        setForExtracurricular(false);

        checkRules();
    }

    private ERPTuitionInfoTypeAcademicEntry(final ERPTuitionInfoType type, final DegreeCurricularPlan degreeCurricularPlan) {
        this();

        setErpTuitionInfoType(type);
        setDegreeType(degreeCurricularPlan.getDegree().getDegreeType());
        setDegree(degreeCurricularPlan.getDegree());
        setDegreeCurricularPlan(degreeCurricularPlan);

        setForRegistration(true);
        setForStandalone(false);
        setForExtracurricular(false);

        checkRules();

    }

    private ERPTuitionInfoTypeAcademicEntry(final ERPTuitionInfoType type, final boolean forStandalone,
            final boolean forExtracurricular) {
        this();

        setErpTuitionInfoType(type);
        setForRegistration(false);
        setForStandalone(forStandalone);
        setForExtracurricular(forExtracurricular);

        checkRules();
    }

    void checkRules() {

        if (getDomainRoot() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.bennu.required");
        }

        if (getErpTuitionInfoType() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoType.erpTuitionInfoType.required");
        }

        if (!(isForRegistration() ^ isForStandalone() ^ isForExtracurricular())) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoTypeEntry.for.one.tuition.type.only");
        }

        if (isForRegistration() && getDegreeType() == null) {
            throw new AcademicTreasuryDomainException("error.ERPTuitionInfoTypeEntry.degreeType.required");
        }

        if (getDegree() != null && getDegree().getDegreeType() != getDegreeType()) {
            throw new AcademicTreasuryDomainException(
                    "error.ERPTuitionInfoTypeEntry.degreeType.of.degree.not.match.assigned.degree.type");
        }

        if (getDegreeCurricularPlan() != null && getDegreeCurricularPlan().getDegree() != getDegree()) {
            throw new AcademicTreasuryDomainException(
                    "error.ERPTuitionInfoTypeEntry.degree.of.degreeCurricularPlan.not.match.assigned.degree");
        }

        if ((isForStandalone() || isForExtracurricular()) && getDegreeType() != null) {
            throw new AcademicTreasuryDomainException(
                    "error.ERPTuitionInfoTypeEntry.degreeType.not.supported.for.standalone.or.extracurricular");
        }

        checkAcademicEntriesColisions();

    }

    private void checkAcademicEntriesColisions() {
        for (ERPTuitionInfoTypeAcademicEntry academicEntry : getErpTuitionInfoType().getErpTuitionInfoTypeAcademicEntriesSet()) {
            checkColision(academicEntry);
        }
    }

    private void checkColision(final ERPTuitionInfoTypeAcademicEntry academicEntry) {
        final ExecutionYear executionYear = academicEntry.getErpTuitionInfoType().getExecutionYear();

        final Set<ERPTuitionInfoTypeAcademicEntry> allAcademicEntries =
                executionYear.getErpTuitionInfoTypesSet().stream()
                        .filter(t -> !Sets.intersection(academicEntry.getErpTuitionInfoType().getTuitionProductsSet(), t.getTuitionProductsSet()).isEmpty())
                        .flatMap(t -> t.getErpTuitionInfoTypeAcademicEntriesSet().stream())
                        .collect(Collectors.toSet());

        if (academicEntry.isDefinedForDegreeType()) {
            // Ensure degree type is not repeated
            for (final ERPTuitionInfoTypeAcademicEntry otherEntry : allAcademicEntries) {
                if (academicEntry == otherEntry) {
                    continue;
                }

//                if (!otherEntry.isDefinedForDegreeType()) {
//                    continue;
//                }

                if (academicEntry.getDegreeType() == otherEntry.getDegreeType()) {
                    throw new AcademicTreasuryDomainException(
                            "error.ERPTuitionInfoTypeAcademicEntry.entry.duplicated.for.degreeType",
                            otherEntry.getDegreeType().getName().getContent());
                }
            }

        } else if (academicEntry.isDefinedForDegree()) {
            // Ensure degree is not duplicated and there is not entry for degree type
            for (final ERPTuitionInfoTypeAcademicEntry otherEntry : allAcademicEntries) {
                if (academicEntry == otherEntry) {
                    continue;
                }

                
                if(otherEntry.isDefinedForDegree() || otherEntry.isDefinedForDegreeCurricularPlan()) {

                    if (academicEntry.getDegree() == otherEntry.getDegree()) {
                        throw new AcademicTreasuryDomainException("error.ERPTuitionInfoTypeAcademicEntry.entry.duplicated.for.degree",
                                otherEntry.getDegree().getPresentationNameI18N().getContent());
                    }

                }

                if (otherEntry.isDefinedForDegreeType() && academicEntry.getDegreeType() == otherEntry.getDegreeType()) {
                    throw new AcademicTreasuryDomainException(
                            "error.ERPTuitionInfoTypeAcademicEntry.entry.degreeType.of.degree.defined",
                            academicEntry.getDegree().getPresentationNameI18N().getContent(),
                            otherEntry.getDegreeType().getName().getContent());
                }
            }

        } else if (academicEntry.isDefinedForDegreeCurricularPlan()) {
            // Ensure degree curricular plan is not duplicated, there is no entry for degree or degree type
            for (final ERPTuitionInfoTypeAcademicEntry otherEntry : allAcademicEntries) {
                if (academicEntry == otherEntry) {
                    continue;
                }

                if (otherEntry.isDefinedForDegreeCurricularPlan() && academicEntry.getDegreeCurricularPlan() == otherEntry.getDegreeCurricularPlan()) {
                    throw new AcademicTreasuryDomainException(
                            "error.ERPTuitionInfoTypeAcademicEntry.entry.duplicated.for.degreeCurricularPlan",
                            otherEntry.getDegreeCurricularPlan().getName());
                }

                if (otherEntry.isDefinedForDegree() && academicEntry.getDegree() == otherEntry.getDegree()) {
                    throw new AcademicTreasuryDomainException(
                            "error.ERPTuitionInfoTypeAcademicEntry.entry.degree.of.degreeCurricularPlan.defined",
                            academicEntry.getDegreeCurricularPlan().getName(),
                            otherEntry.getDegree().getPresentationNameI18N().getContent());
                }

                if (otherEntry.isDefinedForDegreeType() && academicEntry.getDegreeType() == otherEntry.getDegreeType()) {
                    throw new AcademicTreasuryDomainException(
                            "error.ERPTuitionInfoTypeAcademicEntry.entry.degreeType.of.degreeCurricularPlan.defined",
                            academicEntry.getDegreeCurricularPlan().getPresentationName(executionYear),
                            otherEntry.getDegreeType().getName().getContent());
                }
            }
        } else if (academicEntry.isForStandalone()) {
            for (final ERPTuitionInfoTypeAcademicEntry otherEntry : allAcademicEntries) {
                if (academicEntry == otherEntry) {
                    continue;
                }

                if (!otherEntry.isForStandalone()) {
                    continue;
                }

                throw new AcademicTreasuryDomainException(
                        "error.ERPTuitionInfoTypeAcademicEntry.entry.duplicated.for.standalone");
            }
        } else if (academicEntry.isForExtracurricular()) {
            for (final ERPTuitionInfoTypeAcademicEntry otherEntry : allAcademicEntries) {
                if (academicEntry == otherEntry) {
                    continue;
                }

                if (!otherEntry.isForExtracurricular()) {
                    continue;
                }

                throw new AcademicTreasuryDomainException(
                        "error.ERPTuitionInfoTypeAcademicEntry.entry.duplicated.for.extracurricular");
            }
        }
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

    public boolean isDefinedForDegreeCurricularPlan() {
        return isForRegistration() && getDegreeCurricularPlan() != null;
    }

    public boolean isDefinedForDegree() {
        return isForRegistration() && getDegree() != null && getDegreeCurricularPlan() == null;
    }

    public boolean isDefinedForDegreeType() {
        return isForRegistration() && getDegreeType() != null && getDegree() == null && getDegreeCurricularPlan() == null;
    }

    public boolean isAppliedForRegistration(final Registration registration, final ExecutionYear executionYear) {
        if (executionYear != getErpTuitionInfoType().getExecutionYear()) {
            return false;
        }

        if (isDefinedForDegreeCurricularPlan()
                && getDegreeCurricularPlan() == registration.getStudentCurricularPlan(executionYear).getDegreeCurricularPlan()) {
            return true;
        } else if (isDefinedForDegree() && getDegree() == registration.getDegree()) {
            return true;
        } else if (isDefinedForDegreeType() && getDegreeType() == registration.getDegreeType()) {
            return true;
        }

        return false;
    }

    public boolean isAppliedOnAcademicTreasuryEvent(final AcademicTreasuryEvent event, final ExecutionYear executionYear) {

        if (executionYear != getErpTuitionInfoType().getExecutionYear()) {
            return false;
        }

        final Set<Product> debtProducts = DebitEntry.findActive(event).map(d -> d.getProduct())
                .filter(p -> p != TreasurySettings.getInstance().getInterestProduct()).collect(Collectors.toSet());

        if (!getErpTuitionInfoType().getTuitionProductsSet().containsAll(debtProducts)) {

            if(!Sets.intersection(getErpTuitionInfoType().getTuitionProductsSet(), debtProducts).isEmpty()) {
                // There are some products which match, throw an error to analyse
                throw new AcademicTreasuryDomainException("error.ERPTuitionInfo.event.define.some.of.the.products.of.type.but.not.all",
                        event.getPerson().getStudent().getNumber().toString(),
                        event.getPerson().getStudent().getName(),
                        getErpTuitionInfoType().getErpTuitionInfoProduct().getName());
            }
            
            return false;
        }

        final Registration registration = event.getRegistration();

        if (isForRegistration() && event.isForRegistrationTuition() && isAppliedForRegistration(registration, executionYear)) {
            return true;
        } else if (isForStandalone() && event.isForStandaloneTuition()) {
            return true;
        } else if (isForExtracurricular() && event.isForExtracurricularTuition()) {
            return true;
        }

        return false;
    }

    public LocalizedString getDescription() {
        if (isDefinedForDegreeType()) {
            return getDegreeType().getName();
        }

        if (isDefinedForDegree()) {
            LocalizedString result = new LocalizedString();
            for (Locale locale : AcademicTreasuryConstants.supportedLocales()) {
                result = result.with(locale, format("[%s] %s", getDegree().getCode(), getDegree().getPresentationNameI18N().getContent(locale)));
            }

            return result;
        }

        if (isDefinedForDegreeCurricularPlan()) {
            LocalizedString result = new LocalizedString();
            for (Locale locale : AcademicTreasuryConstants.supportedLocales()) {
                result = result.with(locale, String.format("[%s] %s - %s", getDegree().getCode(),
                        getDegree().getPresentationNameI18N().getContent(locale), getDegreeCurricularPlan().getName()));

            }
            return result;
        }

        if (isForStandalone()) {
            return academicTreasuryBundleI18N("label.ERPTuitionInfoTypeAcademicEntry.standalone.description");
        } else if (isForExtracurricular()) {
            return academicTreasuryBundleI18N("label.ERPTuitionInfoTypeAcademicEntry.extracurricular.description");
        }

        throw new RuntimeException("error");
    }

    public void delete() {
        setDomainRoot(null);
        setErpTuitionInfoType(null);

        setDegreeCurricularPlan(null);
        setDegree(null);
        setDegreeType(null);

        deleteDomainObject();
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static ERPTuitionInfoTypeAcademicEntry createForRegistrationTuition(final ERPTuitionInfoType type,
            final DegreeType degreeType) {
        return new ERPTuitionInfoTypeAcademicEntry(type, degreeType);
    }

    public static ERPTuitionInfoTypeAcademicEntry createForRegistrationTuition(final ERPTuitionInfoType type,
            final Degree degree) {
        return new ERPTuitionInfoTypeAcademicEntry(type, degree);
    }

    public static ERPTuitionInfoTypeAcademicEntry createForRegistrationTuition(final ERPTuitionInfoType type,
            final DegreeCurricularPlan degreeCurricularPlan) {
        return new ERPTuitionInfoTypeAcademicEntry(type, degreeCurricularPlan);
    }

    public static ERPTuitionInfoTypeAcademicEntry createForStandaloneTuition(final ERPTuitionInfoType type) {
        return new ERPTuitionInfoTypeAcademicEntry(type, true, false);
    }

    public static ERPTuitionInfoTypeAcademicEntry createForExtracurricularTuition(final ERPTuitionInfoType type) {
        return new ERPTuitionInfoTypeAcademicEntry(type, false, true);
    }

}
