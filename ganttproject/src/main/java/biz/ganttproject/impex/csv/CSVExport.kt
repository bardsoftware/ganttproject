/*
Copyright 2017-2021 Alexander Kurutin, Dmitry Barashev, BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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
import biz.ganttproject.customproperty.CustomProperty
import biz.ganttproject.customproperty.CustomPropertyClass
import biz.ganttproject.customproperty.CustomPropertyDefinition
import com.google.common.base.Charsets
import com.google.common.base.Strings
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.math.BigDecimal

/**
 * This is an implementation of SpreadsheetWriter which writes to CSV file
 *
 * @author Dmitry Barashev
 * ------
 * 2021: translated to Kotlin and significantly updated to allow for data types other than strings
 *       by Dmitry Barashev
 * 2017: initially written in Java by Alexander Kurutin.
 */
class CsvWriterImpl @JvmOverloads constructor(
  stream: OutputStream, format: CSVFormat, addBom: Boolean = false) : SpreadsheetWriter {
  private val myCsvPrinter: CSVPrinter

  init {
    val writer = OutputStreamWriter(stream, Charsets.UTF_8)
    if (addBom) {
      writer.write('\ufeff'.toInt())
    }
    myCsvPrinter = CSVPrinter(writer, format)
  }

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
}

/**
 * Prints the values of the specified custom property definitions using the following rules:
 * - the order of printing is the same as the order of definitions in the defs list
 * - if some value is missing, it is replaced with null as String
 * - the type of the printed value is taken from the definition
 * - when all definitions are processed, it ends the current record
 */
@Throws(IOException::class)
fun writeCustomPropertyValues(
  writer: SpreadsheetWriter,
  defs: List<CustomPropertyDefinition>,
  values: List<CustomProperty>) {
  val definedProps = values.map { it.definition.id to it}.toMap()
  for (def in defs) {
    val value = definedProps[def.id]
    if (value == null) {
      writer.print(null as String?)
    } else {
      when (value.definition.propertyClass) {
        CustomPropertyClass.TEXT -> {
          writer.print(Strings.nullToEmpty(value.valueAsString))
        }
        CustomPropertyClass.DOUBLE -> {
          writer.print(value.valueAsString.toDoubleOrNull())
        }
        CustomPropertyClass.INTEGER -> {
          writer.print(value.valueAsString.toIntOrNull())
        }
        CustomPropertyClass.DATE -> {
          writer.print(value.value as GanttCalendar)
        }
        CustomPropertyClass.BOOLEAN -> {
          writer.print(value.valueAsString.toBoolean())
        }
      }
    }
  }
  writer.println()
}
