package org.fenixedu.academictreasury.util;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.fenixedu.academictreasury.domain.settings.AcademicTreasurySettings;
import org.fenixedu.academictreasury.domain.tuition.TuitionPaymentPlanGroup;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.CustomerType;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.ProductGroup;
import org.fenixedu.treasury.domain.VatExemptionReason;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.util.TreasuryConstants;

public class AcademicTreasuryBootstrapper {

    // Should not called in the initialization of FenixEdu
    public static void bootstrap() {
        initializeCustomerType();
        initializeProductGroup();
        initializeTuitionsTypeProducts();
        initializeTuitionPaymentPlanGroups();
        initializeTuitionsProducts();
    }

    private static void initializeCustomerType() {
        CustomerType.findByCode("STUDENT").findFirst().ifPresentOrElse(t -> {
        }, () -> CustomerType.create("STUDENT", TreasuryConstants.treasuryBundleI18N("label.CustomerType.STUDENT")));

    }

    private static void initializeProductGroup() {
        Optional.ofNullable(ProductGroup.findByCode("TUITION")).ifPresentOrElse(p -> Function.identity().apply(p),
                () -> ProductGroup.create("TUITION", TreasuryConstants.treasuryBundleI18N("label.productGroup.tuition")));

        Optional.ofNullable(AcademicTreasurySettings.getInstance().getTuitionProductGroup()).ifPresentOrElse(
                p -> Function.identity().apply(p),
                () -> AcademicTreasurySettings.getInstance().setTuitionProductGroup(ProductGroup.findByCode("TUITION")));

        Optional.ofNullable(AcademicTreasurySettings.getInstance().getEmolumentsProductGroup()).ifPresentOrElse(
                p -> Function.identity().apply(p),
                () -> AcademicTreasurySettings.getInstance().setEmolumentsProductGroup(ProductGroup.findByCode("TUITION")));
    }

    public static void initializeTuitionsProducts() {

        for (int cycle = 1; cycle <= 3; cycle++) {
            for (int num = 1; num <= 12; num++) {

                String numlabel = num <= 3 ? "_" + num : "";

                final String code = String.format("PROP_%d_PREST_%d_CIC", num, cycle);

                final String label = String.format("PROP%s_PREST_%d_CIC", numlabel, cycle);
                final LocalizedString description =
                        AcademicTreasuryConstants.academicTreasuryBundleI18N("TreasuryBootstrap." + label, String.valueOf(num));

                final int installmentNumber = num;
                Product.findUniqueByCode(code).ifPresentOrElse(p -> Function.identity().apply(p), () -> {
                    Product.create(ProductGroup.findByCode("TUITION"), code, description,
                            TreasuryConstants.treasuryBundleI18N("label.unit"), true, false, installmentNumber,
                            VatType.findByCode("ISE"), FinantialInstitution.findAll().collect(Collectors.toList()),
                            VatExemptionReason.findByCode("M07"));

                });
            }
        }
    }

    public static void initializeTuitionsTypeProducts() {
        Product.findUniqueByCode("PROPINA_MATRICULA").ifPresentOrElse(p -> Function.identity().apply(p), () -> {
            Product.create(ProductGroup.findByCode("TUITION"), "PROPINA_MATRICULA",
                    TreasuryConstants.treasuryBundleI18N("label.registration.tuition"),
                    TreasuryConstants.treasuryBundleI18N("label.unit"), true, false, 0, VatType.findByCode("ISE"),
                    FinantialInstitution.findAll().collect(Collectors.toList()), VatExemptionReason.findByCode("M07"));
        });

        Product.findUniqueByCode("PROPINA_ISOLADAS").ifPresentOrElse(p -> Function.identity().apply(p), () -> {
            Product.create(ProductGroup.findByCode("TUITION"), "PROPINA_ISOLADAS",
                    TreasuryConstants.treasuryBundleI18N("label.standalone.tuition"),
                    TreasuryConstants.treasuryBundleI18N("label.unit"), true, false, 0, VatType.findByCode("ISE"),
                    FinantialInstitution.findAll().collect(Collectors.toList()), VatExemptionReason.findByCode("M07"));
        });

        Product.findUniqueByCode("PROPINA_EXTRACURRIC").ifPresentOrElse(p -> Function.identity().apply(p), () -> {
            Product.create(ProductGroup.findByCode("TUITION"), "PROPINA_EXTRACURRIC",
                    TreasuryConstants.treasuryBundleI18N("label.extracurricular.tuition"),
                    TreasuryConstants.treasuryBundleI18N("label.unit"), true, false, 0, VatType.findByCode("ISE"),
                    FinantialInstitution.findAll().collect(Collectors.toList()), VatExemptionReason.findByCode("M07"));
        });
    }

    public static void initializeTuitionPaymentPlanGroups() {

        final Product REGISTRATION_TUITION_product = Product.findUniqueByCode("PROPINA_MATRICULA").get();
        final Product STANDALONE_TUITION_product = Product.findUniqueByCode("PROPINA_ISOLADAS").get();
        final Product EXTRACURRICULAR_TUITION_product = Product.findUniqueByCode("PROPINA_EXTRACURRIC").get();

        if (!TuitionPaymentPlanGroup.findUniqueDefaultGroupForRegistration().isPresent()) {
            TuitionPaymentPlanGroup.create("REGISTRATION_TUITION", REGISTRATION_TUITION_product.getName(), true, false, false,
                    REGISTRATION_TUITION_product);
        }

        if (!TuitionPaymentPlanGroup.findUniqueDefaultGroupForStandalone().isPresent()) {
            TuitionPaymentPlanGroup.create("STANDALONE_TUITION", STANDALONE_TUITION_product.getName(), false, true, false,
                    STANDALONE_TUITION_product);
        }

        if (!TuitionPaymentPlanGroup.findUniqueDefaultGroupForExtracurricular().isPresent()) {
            TuitionPaymentPlanGroup.create("EXTRACURRICULAR_TUITION", EXTRACURRICULAR_TUITION_product.getName(), false, false,
                    true, EXTRACURRICULAR_TUITION_product);
        }
    }

}