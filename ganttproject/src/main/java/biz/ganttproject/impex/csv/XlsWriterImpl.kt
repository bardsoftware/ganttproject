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
package biz.ganttproject.impex.csv

import biz.ganttproject.core.time.GanttCalendar
import org.apache.poi.ss.usermodel.Workbook
import java.io.IOException
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import java.io.OutputStream
import java.math.BigDecimal

/**
 * @author akurutin on 04.04.2017.
 */
class XlsWriterImpl internal constructor(private val myStream: OutputStream) : SpreadsheetWriter {
  private val myWorkbook: Workbook
  private val mySheet: Sheet
  private var myCurrentRow: Row
  private var myNextRowInd = 0
  private var myNextCellInd = 0

  init {
    myWorkbook = HSSFWorkbook()
    mySheet = myWorkbook.createSheet()
    myCurrentRow = createNewRow()
  }

  @Throws(IOException::class)
  override fun print(value: String?) {
    createCell().let {
      if (value != null) {
        it.setCellValue(value)
      }
    }
  }

  override fun print(value: Int?) {
    createCell().let {
      if (value != null) {
        it.setCellValue(value.toDouble())
      }
    }
  }

  override fun print(value: Double?) {
    createCell().let {
      if (value != null) {
        it.setCellValue(value)
      }
    }
  }

  override fun print(value: BigDecimal?) {
    createCell().let {
      if (value != null) {
        it.setCellValue(value.toDouble())
      }
    }
  }

  override fun print(value: GanttCalendar?) {
    createCell().let {
      if (value != null) {
        it.setCellValue(value.time)
      }
    }
  }

  @Throws(IOException::class)
  override fun println() {
    createNewRow()
    myNextCellInd = 0
  }

  @Throws(IOException::class)
  override fun close() {
    myWorkbook.write(myStream)
    myWorkbook.close()
    myStream.close()
  }

  private fun createNewRow() =
    mySheet.createRow(myNextRowInd++).also { myCurrentRow = it }

  private fun createCell() = myCurrentRow.createCell(myNextCellInd++)

}