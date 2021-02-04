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
import org.apache.commons.csv.CSVFormat
import com.google.common.base.Charsets
import org.apache.commons.csv.CSVPrinter
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.math.BigDecimal

/**
 * @author akurutin on 04.04.2017.
 */
class CsvWriterImpl @JvmOverloads internal constructor(
  stream: OutputStream, format: CSVFormat, addBom: Boolean = false) : SpreadsheetWriter {
  private val myCsvPrinter: CSVPrinter
  @Throws(IOException::class)
  override fun print(value: String?) {
    myCsvPrinter.print(value)
  }

  override fun print(value: Int?) {
    print(value?.toString())
  }

  override fun print(value: Double?) {
    print(value?.toString())
  }

  override fun print(value: BigDecimal?) {
    print(value?.toString())
  }

  override fun print(value: GanttCalendar?) {
    print(value?.toString())
  }

  override fun print(value: Boolean?) {
    print(value?.toString())
  }

  @Throws(IOException::class)
  override fun println() {
    myCsvPrinter.println()
  }

  @Throws(IOException::class)
  override fun close() {
    myCsvPrinter.flush()
    myCsvPrinter.close()
  }

  init {
    val writer = OutputStreamWriter(stream, Charsets.UTF_8)
    if (addBom) {
      writer.write('\ufeff'.toInt())
    }
    myCsvPrinter = CSVPrinter(writer, format)
  }
}
