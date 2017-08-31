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

import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Calendar;

/**
 * @author akurutin on 04.04.2017.
 */
public class XlsWriterImpl implements SpreadsheetWriter {
  private final Workbook myWorkbook;
  private final OutputStream myStream;
  private final CellStyle myDateStyle;
  private final CellStyle myIntegerStyle;
  private final CellStyle myDoubleStyle;

  private Sheet myCurrentSheet = null;
  private Row myCurrentRow = null;
  private int myNextRowInd = 0;
  private int myNextCellInd = 0;


  XlsWriterImpl(OutputStream stream, String initialSheetName) {
    myStream = Preconditions.checkNotNull(stream);
    myWorkbook = new HSSFWorkbook();

    myDateStyle = myWorkbook.createCellStyle();
    short fmt = myWorkbook.createDataFormat().getFormat("yyyy-mm-dd");
    myDateStyle.setDataFormat(fmt);

    // https://poi.apache.org/apidocs/org/apache/poi/ss/usermodel/BuiltinFormats.html
    myIntegerStyle = myWorkbook.createCellStyle();
    myIntegerStyle.setDataFormat((short) 1);
    myDoubleStyle = myWorkbook.createCellStyle();
    myDoubleStyle.setDataFormat((short) 2);

    myCurrentSheet = myWorkbook.createSheet(initialSheetName);
  }

  @Override
  public void print(String value) throws IOException {
    if (value != null) {
      addCell().setCellValue(value);
    }
  }

  private Cell addCell() throws IOException {
    if (myCurrentRow == null) {
      createNewRow();
    }
    return myCurrentRow.createCell(myNextCellInd++);
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
    myCurrentRow = myCurrentSheet.createRow(myNextRowInd++);
  }

  @Override
  public void newSheet() throws IOException {
    resetForNewSheet();
    myCurrentSheet = myWorkbook.createSheet();
  }
  
  @Override
  public void newSheet(String name) throws IOException {
    resetForNewSheet();
    myCurrentSheet = myWorkbook.createSheet(name);
  }
  
  private void resetForNewSheet() {
    myNextRowInd = 0;
    myCurrentRow = null;
    myNextCellInd = 0;
  }

  @Override
  public void print(Double value) throws IOException {
    if (value != null) {
      Cell cell = addCell();
      cell.setCellStyle(myDoubleStyle);
      cell.setCellValue(value);
    }
  }

  @Override
  public void print(Integer value) throws IOException {
    if (value != null) {
      Cell cell = addCell();
      cell.setCellStyle(myIntegerStyle);
      cell.setCellValue(value);
    }
  }

  @Override
  public void print(BigDecimal value) throws IOException {
    print(value.doubleValue());
  }

  @Override
  public void print(Calendar value) throws IOException {
    if (value != null) {
      Cell cell = addCell();
      cell.setCellStyle(myDateStyle);
      cell.setCellValue(value);
    }
  }
}
