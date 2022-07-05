/*
 * Copyright (c) 2021 Dmitry Barashev, BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.impex.csv

import biz.ganttproject.app.DefaultLocalizer
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.customproperty.CustomPropertyClass
import junit.framework.TestCase
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.task.CustomColumnsManager
import biz.ganttproject.customproperty.CustomColumnsValues
import org.apache.commons.csv.CSVFormat
import org.w3c.util.DateParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.math.BigDecimal
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author dbarashev@bardsoftware.com
 */
class CustomPropertyExportImportTest : TestCase() {
  @Throws(Exception::class)
  override fun setUp() {
    TaskDefaultColumn.setLocaleApi { key -> GanttLanguage.getInstance().getText(key) }
    RootLocalizer = object : DefaultLocalizer() {
      override fun formatTextOrNull(key: String, vararg args: Any): String? {
        return key
      }
    }
    GanttLanguage.getInstance().shortDateFormat = SimpleDateFormat("dd/MM/yyyy")
    initLocale()
  }

  fun testXlsCustomPropertyTypesImport() {
    val standardFields = listOf(
      TaskRecords.TaskFields.NAME,
      TaskRecords.TaskFields.BEGIN_DATE,
      TaskRecords.TaskFields.DURATION
    ).map { it.toString() }
    doTestCustomPropertyTypesImport(
      writerFactory = {XlsWriterImpl(it)},
      readerFactory = {XlsReaderImpl(it, standardFields)},
      expectedTypes = mapOf(
        "F1" to CustomPropertyClass.INTEGER,
        "F2" to CustomPropertyClass.DOUBLE,
        "F3" to CustomPropertyClass.BOOLEAN,
        "F4" to CustomPropertyClass.DATE
      ),
      expectedValues = mapOf(
        "F1" to 42,
        "F2" to Int.MAX_VALUE.toDouble(),
        "F3" to true,
        "F4" to GanttCalendar.parseXMLDate("2021-04-05")
      )
    )
  }



  fun testCsvCustomPropertyTypesImport() {
    doTestCustomPropertyTypesImport(
      writerFactory = {CsvWriterImpl(it, CSVFormat.DEFAULT)},
      readerFactory = {CsvReaderImpl(it, CSVFormat.DEFAULT)},
      expectedTypes = mapOf(
        "F1" to CustomPropertyClass.INTEGER,
        "F2" to CustomPropertyClass.TEXT,
        "F3" to CustomPropertyClass.TEXT,
        "F4" to CustomPropertyClass.TEXT
      ),
      expectedValues = mapOf(
        "F1" to 42,
        "F2" to Int.MAX_VALUE.toString(),
        "F3" to "true",
        "F4" to GanttLanguage.getInstance().formatShortDate(DateParser.parse("2021-04-05"))
      )
    )
  }

  /**
   * Here we write some typed values with "custom" property names using a writer and then
   * check that after reading we create appropriately typed custom properties and their values.
   */
  private fun doTestCustomPropertyTypesImport(
    writerFactory: (ByteArrayOutputStream) -> SpreadsheetWriter,
    readerFactory: (ByteArrayInputStream) -> SpreadsheetReader,
    expectedTypes: Map<String, CustomPropertyClass>,
    expectedValues: Map<String, Any>
  ) {
    val standardFields = listOf(
      TaskRecords.TaskFields.NAME,
      TaskRecords.TaskFields.BEGIN_DATE,
      TaskRecords.TaskFields.DURATION
    ).map { it.toString() }
    val customFields = listOf("F1", "F2", "F3", "F4")
    val header = standardFields + customFields

    GanttLanguage.getInstance().shortDateFormat = SimpleDateFormat("dd/MM/yy")
    val out = ByteArrayOutputStream()
    writerFactory(out).let {
      // -- Header row
      header.forEach(it::print)
      it.println()
      // -- Data row
      // -- Standard fields
      it.print("task1")
      it.print(GanttCalendar.parseXMLDate("2021-04-05"))
      it.print(1)
      // -- Custom fields
      it.print(42)
      it.print(BigDecimal.valueOf(Int.MAX_VALUE.toLong()))
      it.print(true)
      it.print(GanttCalendar.parseXMLDate("2021-04-05"))
      it.println()
      it.close()
    }
    println(out.toString(Charsets.UTF_8))
    val inputStream = ByteArrayInputStream(out.toByteArray())
    readerFactory(inputStream).iterator().let {
      // Read the header row
      val headerRecord = it.next().let { first ->
        assertEquals(header, first.iterator().asSequence().toList())
        assertTrue(it.hasNext())
        first
      }
      // Create a custom property with name F1 and Int type. When reading custom
      // property values, we're supposed to find this definition and properly
      // interpret numeric value as integer.
      val customPropertyMgr = CustomColumnsManager().also { mgr ->
        mgr.createDefinition("F1", "int", "F1", "0")
      }
      val customValues = CustomColumnsValues(customPropertyMgr, eventDispatcher = {})
      it.next().let{ record ->
        readCustomProperties(headerRecord, customFields, record, customPropertyMgr) { def, value ->
          customValues.addCustomProperty(def, value)
        }
      }
      for ((id, expectedClass) in expectedTypes) {
        assertEquals(expectedClass, customPropertyMgr.getCustomPropertyDefinition(id).propertyClass)
      }
      GanttLanguage.getInstance().shortDateFormat = SimpleDateFormat("dd/MM/yy")
      for ((id, expectedValue) in expectedValues) {
        assertEquals(expectedValue, customValues.getValue(customPropertyMgr.getCustomPropertyDefinition(id)))
      }
    }
  }
}

fun initLocale() {
  object : CalendarFactory() {
    init {
      setLocaleApi(object : LocaleApi {
        override fun getLocale(): Locale {
          return Locale.UK
        }

        override fun getShortDateFormat(): DateFormat {
          return DateFormat.getDateInstance(DateFormat.SHORT, Locale.UK)
        }
      })
    }
  }
}
