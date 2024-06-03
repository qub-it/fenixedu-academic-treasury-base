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
package org.fenixedu.academictreasury.services.accesscontrol.spi;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicAccessRule;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.accesscontrol.TreasuryAccessControlConfiguration;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.services.accesscontrol.spi.ITreasuryAccessControlExtension;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

// TODO: Until all users are fully registered in permission based access control, this class must be registered
public class AcademicTreasuryAccessControlExtension implements ITreasuryAccessControlExtension<Object> {

    @Override
    public boolean isFrontOfficeMember(final String username) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        return FinantialInstitution.findAll().map(l -> isFrontOfficeMember(username, l)).reduce((a, b) -> a || b).orElse(false);
    }

    @Override
    public boolean isFrontOfficeMember(final String username, final FinantialInstitution finantialInstitution) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        return FinantialEntity.find(finantialInstitution).map(l -> isFrontOfficeMember(username, l)).reduce((a, b) -> a || b)
                .orElse(false);
    }

    @Override
    public boolean isFrontOfficeMember(final String username, final FinantialEntity finantialEntity) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        final User user = User.findByUsername(username);

        return AcademicAccessRule.isMember(user, AcademicOperationType.MANAGE_STUDENT_PAYMENTS, emptySet(),
                singleton(finantialEntity.getAdministrativeOffice()));
    }

    @Override
    public boolean isFrontOfficeMemberWithinContext(final String username, final Object context) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        final IAcademicTreasuryPlatformDependentServices services =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        if (context instanceof Degree) {
            final Degree degree = (Degree) context;
            final FinantialEntity finantialEntity = services.finantialEntityOfDegree(degree, new LocalDate());

            return isFrontOfficeMember(username, finantialEntity);
        }

        return false;
    }

    @Override
    public boolean isBackOfficeMember(final String username) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        return FinantialInstitution.findAll().map(l -> isBackOfficeMember(username, l)).reduce((a, b) -> a || b).orElse(false);
    }

    @Override
    public boolean isBackOfficeMember(final String username, final FinantialInstitution finantialInstitution) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        return FinantialEntity.find(finantialInstitution).map(l -> isBackOfficeMember(username, l)).reduce((a, b) -> a || b)
                .orElse(false);
    }

    @Override
    public boolean isBackOfficeMember(final String username, final FinantialEntity finantialEntity) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        final User user = User.findByUsername(username);

        return AcademicAccessRule.isMember(user, AcademicOperationType.MANAGE_STUDENT_PAYMENTS_ADV, emptySet(),
                singleton(finantialEntity.getAdministrativeOffice()));
    }

    @Override
    public boolean isBackOfficeMemberWithinContext(String username, Object context) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        final IAcademicTreasuryPlatformDependentServices services =
                AcademicTreasuryPlataformDependentServicesFactory.implementation();
        if (context instanceof Degree) {
            final Degree degree = (Degree) context;
            final FinantialEntity finantialEntity = services.finantialEntityOfDegree(degree, new LocalDate());

            if (finantialEntity == null) {
                return false;
            }

            return isBackOfficeMember(username, finantialEntity);
        }

        return false;
    }

    @Override
    public boolean isContextObjectApplied(Object context) {
        if (context instanceof Degree) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isManager(final String username) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        return false;
    }

    @Override
    public boolean isAllowToModifySettlements(final String username, final FinantialInstitution finantialInstitution) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        return FinantialEntity.find(finantialInstitution).map(l -> isAllowToModifySettlements(username, l))
                .reduce((a, b) -> a || b).orElse(false);
    }

    private boolean isAllowToModifySettlements(final String username, final FinantialEntity finantialEntity) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        final User user = User.findByUsername(username);

        return AcademicAccessRule.isMember(user, AcademicOperationType.PAYMENTS_MODIFY_SETTLEMENTS, emptySet(),
                singleton(finantialEntity.getAdministrativeOffice()));
    }

    @Override
    @Deprecated
    public boolean isAllowToModifyInvoices(final String username, final FinantialInstitution finantialInstitution) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        return FinantialEntity.find(finantialInstitution).map(l -> isAllowToModifyInvoices(username, l)).reduce((a, b) -> a || b)
                .orElse(false);
    }

    @Deprecated
    private boolean isAllowToModifyInvoices(final String username, final FinantialEntity finantialEntity) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        final User user = User.findByUsername(username);

        return AcademicAccessRule.isMember(user, AcademicOperationType.PAYMENTS_MODIFY_INVOICES, emptySet(),
                singleton(finantialEntity.getAdministrativeOffice()));
    }

    @Override
    public boolean isAllowToConditionallyAnnulSettlementNote(final String username, final SettlementNote settlementNote) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        return false;
    }

    @Override
    public boolean isAllowToAnnulSettlementNoteWithoutAnyRestriction(final String username, final SettlementNote settlementNote) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return false;
        }

        return false;
    }

    @Override
    public Set<String> getFrontOfficeMemberUsernames() {
        final Set<String> result = Sets.newHashSet();

        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return result;
        }

        FinantialEntity.findAll().forEach(entity -> {
            result.addAll(AcademicAccessRule
                    .getMembers(AcademicOperationType.MANAGE_STUDENT_PAYMENTS, emptySet(),
                            singleton(entity.getAdministrativeOffice()))
                    .filter(u -> !isNullOrEmpty(u.getUsername())).map(u -> u.getUsername()).collect(Collectors.toSet()));
        });

        return result;
    }

    @Override
    public Set<String> getBackOfficeMemberUsernames() {
        final Set<String> result = Sets.newHashSet();

        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return result;
        }

        FinantialEntity.findAll().forEach(entity -> {
            result.addAll(AcademicAccessRule
                    .getMembers(AcademicOperationType.MANAGE_STUDENT_PAYMENTS_ADV, emptySet(),
                            singleton(entity.getAdministrativeOffice()))
                    .filter(u -> !isNullOrEmpty(u.getUsername())).map(u -> u.getUsername()).collect(Collectors.toSet()));
        });

        return result;
    }

    @Override
    public Set<String> getTreasuryManagerMemberUsernames() {
        if (!TreasuryAccessControlConfiguration.isAccessControlByAcademicAuthorizations()) {
            return Sets.newHashSet();
        }

        return Sets.newHashSet();
    }

}
