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
package org.fenixedu.academictreasury.domain.treasury;

import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.bennu.signals.BennuSignalsHandler;

import org.fenixedu.academic.domain.treasury.IAcademicTreasuryTarget;
import org.fenixedu.academictreasury.domain.event.AcademicTreasuryEvent;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;

import pt.ist.fenixframework.FenixFramework;

public class HandleSettlementNotePayment implements BennuSignalsHandler<SettlementNote> {

    public void handleObject(SettlementNote settlementNote) {

      // @formatter:off
      /* 
       * Check if settlementNote was deleted to avoid process of deleted objects in erp integration.
       * Unfortunately FenixFramework.isDomainObjectValid is throwing the same ClassCastException
       * over settlementNote so is wrapped in try-catch
       */
      // @formatter:on
        boolean toReturn = true;
        try {
            toReturn = !FenixFramework.isDomainObjectValid(settlementNote);
        } catch (Throwable t) {
            toReturn = true;
        }

        if (toReturn) {
            return;
        }

        for (final SettlementEntry s : settlementNote.getSettlemetEntries().collect(Collectors.toSet())) {
            final InvoiceEntry invoiceEntry = s.getInvoiceEntry();

            if (!(invoiceEntry instanceof DebitEntry)) {
                continue;
            }

            final DebitEntry d = (DebitEntry) invoiceEntry;

            if (d.getTreasuryEvent() == null) {
                continue;
            }

            if (!(d.getTreasuryEvent() instanceof AcademicTreasuryEvent)) {
                continue;
            }

            AcademicTreasuryEvent academicTreasuryEvent = (AcademicTreasuryEvent) d.getTreasuryEvent();
            if (!academicTreasuryEvent.isForTreasuryEventTarget()) {
                continue;
            }

            ((IAcademicTreasuryTarget) academicTreasuryEvent.getTreasuryEventTarget()).handleSettlement(academicTreasuryEvent);
        }
    }
}