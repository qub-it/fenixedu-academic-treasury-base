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
package org.fenixedu.academictreasury.domain.debtGeneration.strategies.massive;

import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundle;
import static org.fenixedu.academictreasury.util.AcademicTreasuryConstants.academicTreasuryBundleI18N;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.fenixedu.academictreasury.domain.debtGeneration.requests.IMassiveDebtGenerationStrategy;
import org.fenixedu.academictreasury.domain.debtGeneration.requests.MassiveDebtGenerationRequestFile;
import org.fenixedu.academictreasury.domain.debtGeneration.requests.MassiveDebtGenerationRequestFileBean;
import org.fenixedu.academictreasury.domain.debtGeneration.requests.MassiveDebtGenerationRowResult;
import org.fenixedu.academictreasury.domain.exceptions.AcademicTreasuryDomainException;
import org.fenixedu.academictreasury.util.AcademicTreasuryConstants;
import org.fenixedu.academictreasury.util.ExcelUtils;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import pt.ist.fenixframework.FenixFramework;

public class AnullDebitEntriesStrategy implements IMassiveDebtGenerationStrategy<MassiveDebtGenerationRowResult> {

    private static final int MAX_COLS = 6;

    private static final int DEBIT_ENTRY_ID_IDX = 0;
    private static final int DEBIT_ENTRY_DESC_IDX = 1;
    private static final int CUSTOMER_NAME_IDX = 2;
    private static final int ANNUL_OTHER_ENTRIES_IN_DEBIT_NOTE_IDX = 3;
    private static final int ANNUL_IN_CLOSED_DEBIT_NOTE_IDX = 4;
    private static final int ANNUL_WITH_SETTLEMENT_IDX = 5;

    public static final Locale LOCALE_PT = new Locale("PT");
    public static final Locale LOCALE_EN = new Locale("EN");

    @Override
    public void checkRules(final MassiveDebtGenerationRequestFile file) {
        if (Strings.isNullOrEmpty(file.getReason())) {
            throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequestFile.reason.required");
        }
    }

    @Override
    public String dataDescription(MassiveDebtGenerationRequestFile file) {
        return academicTreasuryBundle("label.AnullDebitEntriesStrategy.dataDescription", file.getReason());
    }

    @Override
    public void process(final MassiveDebtGenerationRequestFile file) {
        final List<MassiveDebtGenerationRowResult> rows =
                readExcel(file.getContent(), new MassiveDebtGenerationRequestFileBean(file));

        for (final MassiveDebtGenerationRowResult row : rows) {
            try {
                if(row.getDebitEntry().isAnnulled()) {
                    continue;
                }
                
                if (row.getDebitEntry().getFinantialDocument() != null) {
                    ((DebitNote) row.getDebitEntry().getFinantialDocument()).anullDebitNoteWithCreditNote(
                            academicTreasuryBundle("label.AnullDebitEntriesStrategy.anull.message", file.getReason()),
                            false);
                } else {
                    row.getDebitEntry().annulDebitEntry(
                            academicTreasuryBundle("label.AnullDebitEntriesStrategy.anull.message", file.getReason()));
                }
            } catch (final Exception e) {
                throw new AcademicTreasuryDomainException("error.AnullDebitEntriesStrategy.on.anull.debit.entry",
                        String.valueOf(row.getRowNum()), e.getLocalizedMessage());
            }
        }
    }

    @Override
    public List<MassiveDebtGenerationRowResult> readExcel(byte[] content, final MassiveDebtGenerationRequestFileBean bean) {

        if (Strings.isNullOrEmpty(bean.getReason())) {
            throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequestFile.reason.required");
        }

        try {

            List<List<String>> data = ExcelUtils.readExcel(new ByteArrayInputStream(content), MAX_COLS);

            final List<MassiveDebtGenerationRowResult> result = Lists.newArrayList();

            int rowNum = 0;
            for (final List<String> row : data) {
                rowNum++;
                if (rowNum == 1) {
                    continue;
                }

                final String debitEntryOidValue = trim(row.get(DEBIT_ENTRY_ID_IDX));
                final String debitEntryDescriptionValue = trim(row.get(DEBIT_ENTRY_DESC_IDX));
                final String customerNameValue = trim(row.get(CUSTOMER_NAME_IDX));
                final String annullOtherEntriesInDebitNoteValue = trim(row.get(ANNUL_OTHER_ENTRIES_IN_DEBIT_NOTE_IDX));
                final String annullInClosedDebitNoteValue = trim(row.get(ANNUL_IN_CLOSED_DEBIT_NOTE_IDX));
                final String annulWithSettlementsValue = trim(row.get(ANNUL_WITH_SETTLEMENT_IDX));

                if (Strings.isNullOrEmpty(debitEntryOidValue)) {
                    continue;
                }

                DebitEntry debitEntry = null;
                try {
                    debitEntry = FenixFramework.getDomainObject(debitEntryOidValue);
                } catch (final Exception e) {
                    throw new AcademicTreasuryDomainException("error.AnullDebitEntriesStrategy.debitEntry.invalid",
                            String.valueOf(rowNum), debitEntryOidValue);
                }

                if (debitEntry == null) {
                    throw new AcademicTreasuryDomainException("error.AnullDebitEntriesStrategy.debitEntry.invalid",
                            String.valueOf(rowNum), debitEntryOidValue);
                }

                if (!debitEntry.getDescription().trim().equals(debitEntryDescriptionValue)) {
                    throw new AcademicTreasuryDomainException("error.AnullDebitEntriesStrategy.debitEntryDescription.invalid",
                            String.valueOf(rowNum), debitEntryDescriptionValue, debitEntry.getDescription().trim());
                }

                if (!debitEntry.getDebtAccount().getCustomer().getName().trim().equals(customerNameValue)) {
                    throw new AcademicTreasuryDomainException("error.AnullDebitEntriesStrategy.customerName.invalid",
                            String.valueOf(rowNum), customerNameValue,
                            debitEntry.getDebtAccount().getCustomer().getName().trim());
                }

                if (debitEntry.isAnnulled()) {
                    throw new AcademicTreasuryDomainException("error.AnullDebitEntriesStrategy.debitEntry.already.annuled",
                            String.valueOf(rowNum));
                }
                
                if (!isYes(annullOtherEntriesInDebitNoteValue) && !isNo(annullOtherEntriesInDebitNoteValue)) {
                    throw new AcademicTreasuryDomainException("error.AnullDebitEntriesStrategy.annullOtherEntriesInDebitNoteValue.invalid",
                            String.valueOf(rowNum), annullOtherEntriesInDebitNoteValue);
                }

                final boolean annullOtherEntriesInDebitNote = isYes(annullOtherEntriesInDebitNoteValue) ? true : false;

                if (!annullOtherEntriesInDebitNote && debitEntry.isProcessedInDebitNote() && debitEntry.getFinantialDocument().getFinantialDocumentEntriesSet().size() > 1) {
                    throw new AcademicTreasuryDomainException("error.AnullDebitEntriesStrategy.debitEntry.in.debit.note.with.other.debit.entries",
                            String.valueOf(rowNum), debitEntry.getFinantialDocument().getUiDocumentNumber());
                }

                if (!isYes(annullInClosedDebitNoteValue) && !isNo(annullInClosedDebitNoteValue)) {
                    throw new AcademicTreasuryDomainException("error.AnullDebitEntriesStrategy.annullInClosedDebitNote.invalid",
                            String.valueOf(rowNum), annullInClosedDebitNoteValue);
                }

                final boolean annullInClosedDebitNote = isYes(annullInClosedDebitNoteValue) ? true : false;

                if (!annullInClosedDebitNote && debitEntry.isProcessedInClosedDebitNote()) {
                    throw new AcademicTreasuryDomainException("error.AnullDebitEntriesStrategy.debitEntry.closed.in.debit.note",
                            String.valueOf(rowNum), debitEntry.getFinantialDocument().getUiDocumentNumber());
                }

                if (!isYes(annulWithSettlementsValue) && !isNo(annulWithSettlementsValue)) {
                    throw new AcademicTreasuryDomainException("error.AnullDebitEntriesStrategy.annulWithSettlementsValue.invalid",
                            String.valueOf(rowNum), annulWithSettlementsValue);
                }

                final boolean annulWithSettlements = isYes(annulWithSettlementsValue) ? true : false;

                if (!annulWithSettlements
                        && debitEntry.getSettlementEntriesSet().stream().filter(s -> !s.isAnnulled()).count() > 0) {
                    throw new AcademicTreasuryDomainException("error.AnullDebitEntriesStrategy.debit.entry.with.settlements",
                            String.valueOf(rowNum), annulWithSettlementsValue);
                }

                result.add(new MassiveDebtGenerationRowResult(rowNum, debitEntry));
            }

            if (result.isEmpty()) {
                throw new AcademicTreasuryDomainException("error.AnullDebitEntriesStrategy.result.empty");

            }

            return result;
        } catch (final IOException e) {
            throw new AcademicTreasuryDomainException("error.MassiveDebtGenerationRequest.invalid.sheet");
        }
    }

    @Override
    public boolean isDebtDateRequired() {
        return false;
    }

    @Override
    public boolean isExecutionYearRequired() {
        return false;
    }

    @Override
    public boolean isForAcademicTaxRequired() {
        return false;
    }

    @Override
    public boolean isReasonRequired() {
        return true;
    }

    @Override
    public boolean isFinantialInstitutionRequired() {
        return false;
    }

    @Override
    public String viewUrl() {
        return "/WEB-INF/academicTreasury/debtGeneration/strategies/massive/AnullDebitEntriesStrategy.jsp";
    }

    private boolean isYes(final String value) {
        final LocalizedString ls = academicTreasuryBundleI18N("label.true", new String[0]);

        final String pt = ls.getContent(LOCALE_PT);
        final String en = ls.getContent(LOCALE_EN);

        return pt.equalsIgnoreCase(value) || en.equalsIgnoreCase(value);
    }

    private boolean isNo(final String value) {
        final LocalizedString ls = academicTreasuryBundleI18N("label.false", new String[0]);

        final String pt = ls.getContent(LOCALE_PT);
        final String en = ls.getContent(LOCALE_EN);

        return pt.equalsIgnoreCase(value) || en.equalsIgnoreCase(value);
    }

    private String trim(String string) {
        if (string == null) {
            return null;
        }

        return string.trim();
    }

}
