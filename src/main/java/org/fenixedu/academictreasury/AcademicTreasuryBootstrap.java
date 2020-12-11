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
package org.fenixedu.academictreasury;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.organizationalStructure.Party;
import org.fenixedu.academictreasury.domain.customer.PersonCustomer;
import org.fenixedu.academictreasury.services.AcademicTreasuryPlataformDependentServicesFactory;
import org.fenixedu.academictreasury.services.IAcademicTreasuryPlatformDependentServices;
import org.fenixedu.bennu.core.domain.Bennu;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class AcademicTreasuryBootstrap {
    static final private String LOG_CONTEXT = AcademicTreasuryBootstrap.class.getSimpleName();

    public static void process() {

    }

    private static final class CustomersPersonThread extends Thread {

        @Override
        public void run() {
            createMissingPersonCustomersForStudents();
        }

        @Atomic(mode = TxMode.READ)
        private void createMissingPersonCustomersForStudents() {
            final IAcademicTreasuryPlatformDependentServices academicTreasuryServices = AcademicTreasuryPlataformDependentServicesFactory.implementation();
            
            int count = 0;
            int totalCount = academicTreasuryServices.readAllPersonsSet().size();
            for (Party party : academicTreasuryServices.readAllPersonsSet()) {
                if (count % 1000 == 0) {
                    System.out.println("TreasuryAcademicBoot - Processing " + count + "/" + totalCount + " parties.");
                }
                count++;

                if (!party.isPerson()) {
                    continue;
                }

                final Person person = (Person) party;

                if (person.getStudent() == null) {
                    continue;
                }

                if (person.getPersonCustomer() != null) {
                    continue;
                }

                final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(person);
                final String fiscalNumber = PersonCustomer.fiscalNumber(person);
                if (Strings.isNullOrEmpty(addressFiscalCountryCode) || Strings.isNullOrEmpty(fiscalNumber)) {
                    return;
                }
                
                try {
                    createMissingPersonCustomer(person);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
            System.out.println("TreasuryAcademicBoot - Finished Validating Students and Customers DebtAccount");

        }

        @Atomic(mode = TxMode.WRITE)
        private void createMissingPersonCustomer(final Person person) {
            final String addressFiscalCountryCode = PersonCustomer.addressCountryCode(person);
            final String fiscalNumber = PersonCustomer.fiscalNumber(person);

            PersonCustomer.create(person, addressFiscalCountryCode, fiscalNumber);
        }

    }

}
