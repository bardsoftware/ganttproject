/*
Copyright 2017-2021 Alexandr Kurutin, Dmitry Barashev, BarD Software s.r.o

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
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import java.io.IOException
import java.io.OutputStream
import java.math.BigDecimal


/**
 * This is an implementation of SpreadsheetWriter which writes to Excel workbook.
 *
 * @author Dmitry Barashev
 * ------
 * 2021: translated to Kotlin and significantly updated to allow for data types other than strings
 *       by Dmitry Barashev
 * 2017: initially written in Java by Alexander Kurutin.
 */
class XlsWriterImpl(private val myStream: OutputStream) : SpreadsheetWriter {
  private val myWorkbook: Workbook = HSSFWorkbook()
  private val mySheet: Sheet = myWorkbook.createSheet()
  private val dateFormat = myWorkbook.creationHelper.createDataFormat().getFormat("m/d/yy")
  private var myCurrentRow: Row
  private var myNextRowInd = 0
  private var myNextCellInd = 0

  init {
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
        it.cellStyle = myWorkbook.createCellStyle().also { it.dataFormat = dateFormat }
      }
    }
  }

  override fun print(value: Boolean?) {
    createCell().let {
      if (value != null) {
        it.setCellValue(value)
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
