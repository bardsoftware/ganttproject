/*
Copyright 2017 Alexandr Kurutin, BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/

package biz.ganttproject.impex.csv;

import com.google.common.base.Preconditions;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author akurutin on 04.04.2017.
 */
public class XlsWriterImpl implements SpreadsheetWriter {
  private final Workbook myWorkbook;
  private final Sheet mySheet;
  private final OutputStream myStream;


  private Row myCurrentRow = null;
  private int myNextRowInd = 0;
  private int myNextCellInd = 0;


  XlsWriterImpl(OutputStream stream) {
    myStream = Preconditions.checkNotNull(stream);
    myWorkbook = new HSSFWorkbook();
    mySheet = myWorkbook.createSheet();
  }

  @Override
  public void print(String value) throws IOException {
    if (myCurrentRow == null) {
      createNewRow();
    }

    Cell cell = myCurrentRow.createCell(myNextCellInd++);
    if (value != null) {
      cell.setCellValue(value);
    }
  }

  @Override
  public void println() throws IOException {
    createNewRow();
    myNextCellInd = 0;
  }

  @Override
  public void close() throws IOException {
    myWorkbook.write(myStream);
    myWorkbook.close();
    myStream.close();
  }

  private void createNewRow() {
    myCurrentRow = mySheet.createRow(myNextRowInd++);
  }
}
