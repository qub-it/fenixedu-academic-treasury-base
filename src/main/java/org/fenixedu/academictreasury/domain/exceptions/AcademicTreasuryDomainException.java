package org.fenixedu.academictreasury.domain.exceptions;

import javax.ws.rs.core.Response.Status;

import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.bennu.core.domain.exceptions.DomainException;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import com.google.gson.JsonObject;

public class AcademicTreasuryDomainException extends RuntimeException {

    // Bennu DomainException
    private final String key;

    private final String[] args;

    private final String bundle;

    private final Status status;

    private AcademicTreasuryDomainException(String bundle, String key, String... args) {
        this(Status.PRECONDITION_FAILED, bundle, key, args);
    }

    private AcademicTreasuryDomainException(Status status, String bundle, String key, String... args) {
        super(key);
        this.status = status;
        this.bundle = bundle;
        this.key = key;
        this.args = args;
    }

    private AcademicTreasuryDomainException(Throwable cause, String bundle, String key, String... args) {
        this(cause, Status.INTERNAL_SERVER_ERROR, bundle, key, args);
    }

    private AcademicTreasuryDomainException(Throwable cause, Status status, String bundle, String key, String... args) {
        super(key, cause);
        this.status = status;
        this.bundle = bundle;
        this.key = key;
        this.args = args;
    }

    public String getLocalizedMessage() {
        return TreasuryPlataformDependentServicesFactory.implementation().bundle(this.bundle, this.key, this.args);
    }

    public Status getResponseStatus() {
        return this.status;
    }

    public JsonObject asJson() {
        JsonObject json = new JsonObject();
        json.addProperty("message", getLocalizedMessage());
        return json;
    }

    public String getKey() {
        return this.key;
    }

    public String[] getArgs() {
        return this.args;
    }
    
    // AcademicTreasuryDomainException

    private static final long serialVersionUID = 1L;

    public AcademicTreasuryDomainException(String key, String... args) {
        this(AcademicTreasuryConstants.BUNDLE.replace('.', '/'), key, args);
    }

    public AcademicTreasuryDomainException(Status status, String key, String... args) {
        this(status, AcademicTreasuryConstants.BUNDLE.replace('.', '/'), key, args);
    }

    public AcademicTreasuryDomainException(Throwable cause, String key, String... args) {
        this(cause, AcademicTreasuryConstants.BUNDLE.replace('.', '/'), key, args);
    }

    public AcademicTreasuryDomainException(Throwable cause, Status status, String key, String... args) {
        this(cause, status, AcademicTreasuryConstants.BUNDLE.replace('.', '/'), key, args);
    }
    
}
