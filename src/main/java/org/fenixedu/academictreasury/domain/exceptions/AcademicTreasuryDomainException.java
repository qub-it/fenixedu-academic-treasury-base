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
package org.fenixedu.academictreasury.domain.exceptions;


import javax.ws.rs.core.Response.Status;

import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
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
