package org.fenixedu.academictreasury.domain.reservationtax;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import pt.ist.fenixframework.FenixFramework;

public class ReservationTax extends ReservationTax_Base {

    public ReservationTax() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected ReservationTax(LocalizedString name, FinantialEntity finantialEntity, Product product, boolean discountInTuitionFee,
            boolean active, boolean createPaymentReferenceCode, LocalizedString taxReservationDescription) {
        this();

        super.setName(name);
        super.setFinantialEntity(finantialEntity);
        super.setProduct(product);
        super.setDiscountInTuitionFee(discountInTuitionFee);
        super.setActive(active);
        super.setCreatePaymentReferenceCode(createPaymentReferenceCode);
        super.setTaxReservationDescription(taxReservationDescription);

        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTax.domainRoot.required");
        }

        if (getFinantialEntity() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTax.finantialEntity.required");
        }

        if (getName() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTax.name.required");
        }

        if (getProduct() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTax.product.required");
        }

        if (getDiscountInTuitionFee() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTax.discountInTuitionFee.required");
        }

        if (getActive() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTax.active.required");
        }

        if (getCreatePaymentReferenceCode() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTax.active.required");
        }

        if (getTaxReservationDescription() == null) {
            throw new AcademicTreasuryDomainException("error.ReservationTax.taxReservationDescription.required");
        }
    }

    public void edit(LocalizedString name, FinantialEntity finantialEntity, Product product, boolean discountInTuitionFee,
            boolean active, boolean createPaymentReferenceCode, LocalizedString taxReservationDescription) {

        super.setName(name);
        super.setFinantialEntity(finantialEntity);
        super.setProduct(product);
        super.setDiscountInTuitionFee(discountInTuitionFee);
        super.setActive(active);
        super.setCreatePaymentReferenceCode(createPaymentReferenceCode);
        super.setTaxReservationDescription(taxReservationDescription);

        checkRules();
    }

    public LocalizedString buildEmolumentDescription(ExecutionYear executionYear) {
        LocalizedString result = new LocalizedString();
        for (Locale locale : TreasuryPlataformDependentServicesFactory.implementation().availableLocales()) {
            Map<String, String> valueMap = new HashMap<String, String>();
            valueMap.put("executionYear", executionYear.getQualifiedName());
            valueMap.put("productName", getProduct().getName().getContent(locale));

            result = result.with(locale, StrSubstitutor.replace(getTaxReservationDescription().getContent(locale), valueMap));
        }

        return result;
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<ReservationTax> findAll() {
        return FenixFramework.getDomainRoot().getReservationTaxesSet().stream();
    }

    public static ReservationTax create(LocalizedString name, FinantialEntity finantialEntity, Product product,
            boolean discountInTuitionFee, boolean active, boolean createPaymentReferenceCode,
            LocalizedString taxReservationDescription) {
        return new ReservationTax(name, finantialEntity, product, discountInTuitionFee, active, createPaymentReferenceCode,
                taxReservationDescription);
    }

    public static Stream<ReservationTax> findActiveByProduct(Product product) {
        return product.getReservationTaxesSet().stream().filter(r -> Boolean.TRUE.equals(r.getActive()));
    }

    public static Optional<ReservationTax> findUniqueActiveByProduct(Product product) {
        return findActiveByProduct(product).findFirst();
    }

}
