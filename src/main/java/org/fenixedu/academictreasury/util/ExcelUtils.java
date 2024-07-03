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
package org.fenixedu.academictreasury.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.qubit.terra.framework.tools.excel.ExcelUtil;
import com.qubit.terra.framework.tools.excel.SheetProcessor;
import com.qubit.terra.framework.tools.excel.XlsType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.google.common.collect.Lists;

public class ExcelUtils {
    private static class TreasuryDefaultExcelSheetProcessor extends SheetProcessor {

        private final boolean readAllSheets;
        List<List<String>> spreadsheetContent;

        final List<ExcelSheet> result = Lists.newArrayList();

        public TreasuryDefaultExcelSheetProcessor(int maxCols, boolean readAllSheets) {
            super();
            this.readAllSheets = readAllSheets;
            setRowProcessor(row -> {
                final ArrayList<String> rowContent = new ArrayList<String>();
                spreadsheetContent.add(rowContent);

                if(row == null) {
                    for(int j = 0; j < maxCols; j++) {
                        rowContent.add("");
                    }

                    return;
                }

                Cell cell;
                for(int j = 0; j < maxCols; j++) {
                    cell = row.getCell(j);

                    if(cell == null) {
                        rowContent.add("");
                        continue;
                    }

                    if (Cell.CELL_TYPE_NUMERIC == cell.getCellType() && DateUtil.isCellDateFormatted(cell)) {
                        rowContent.add(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(cell.getDateCellValue()));
                    } else {
                        cell.setCellType(Cell.CELL_TYPE_STRING);
                        String value = cell.getStringCellValue();
                        rowContent.add(value);
                    }

                }
            });
        }

        @Override
        protected void beforeSheetProcess(Sheet sheet) {
            super.beforeSheetProcess(sheet);
            this.spreadsheetContent = new ArrayList<>();
        }

        public List<List<String>> getSpreadsheetContent() {
            return spreadsheetContent;
        }

        @Override
        protected void afterSheetProcess(Sheet sheet) {
            super.afterSheetProcess(sheet);
            this.result.add(new ExcelSheet(sheet.getSheetName(), getSpreadsheetContent()));
        }

        public List<ExcelSheet> getResult() {
            return this.result;
        }

        @Override
        protected Function<Workbook, List<Sheet>> getSheetsToProcessSupplier() {
            return readAllSheets ? workbook -> {
                int numberOfSheets = workbook.getNumberOfSheets();
                List<Sheet> sheets = new ArrayList<>(numberOfSheets);
                for (int i = 0; i < numberOfSheets; i++) {
                    sheets.add(workbook.getSheetAt(i));
                }
                return sheets;
            } : super.getSheetsToProcessSupplier();
        }
    }
	
    public static List<List<String>> readExcel(final InputStream stream, int maxCols) throws IOException {
        TreasuryDefaultExcelSheetProcessor sheetProcessor = new TreasuryDefaultExcelSheetProcessor(maxCols, false);
        sheetProcessor.setIncludeHeader(true);
        ExcelUtil.importExcel(XlsType.XLSX, stream, sheetProcessor, maxCols);
        return sheetProcessor.getSpreadsheetContent();
    }
    
	public static List<ExcelSheet> readExcelSheets(final InputStream stream, int maxCols) throws IOException {
        TreasuryDefaultExcelSheetProcessor sheetProcessor = new TreasuryDefaultExcelSheetProcessor(maxCols, true);
        sheetProcessor.setIncludeHeader(true);
        ExcelUtil.importExcel(XlsType.XLSX, stream, sheetProcessor, maxCols);
        return sheetProcessor.getResult();
	}
}
