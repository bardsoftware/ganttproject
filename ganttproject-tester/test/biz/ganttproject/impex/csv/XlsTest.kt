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
import biz.ganttproject.core.time.GanttCalendar
import junit.framework.TestCase
import net.sourceforge.ganttproject.CustomPropertyClass
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.task.CustomColumnsManager
import net.sourceforge.ganttproject.task.CustomColumnsValues
import org.w3c.util.DateParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal

/**
 * Some tests which are specific to XLS import/export
 *
 * @author Dmitry Barashev
 */
class XlsTest : TestCase() {
  @Throws(Exception::class)
  override fun setUp() {
    TaskDefaultColumn.setLocaleApi { key -> GanttLanguage.getInstance().getText(key) }
    RootLocalizer = object : DefaultLocalizer() {
      override fun formatTextOrNull(key: String, vararg args: Any): String? {
        return key
      }
    }
  }

  /**
   * Here we write some typed values using a writer and then
   * check that we read the same values with (nearly) the same types.
   *
   * We get different types for Integer and BigDecimal values because in
   * Excel numeric values are all doubles.
   */
  fun testRawTypedValuesExportImport() {
    val out = ByteArrayOutputStream()
    XlsWriterImpl(out).let {
      "A,B,C,D,E,F,G".split(",").forEach(it::print)
      it.println()
      it.print("foo")
      it.print(42)
      it.print(3.14)
      it.print(BigDecimal.valueOf(Int.MAX_VALUE.toLong()))
      it.print(true)
      it.print(GanttCalendar.parseXMLDate("2021-04-05"))
      it.print(null as String?)
      it.println()
      it.close()
    }
    val inputStream = ByteArrayInputStream(out.toByteArray())
    XlsReaderImpl(inputStream, "A,B,C,D,E,F,G".split(",")).iterator().let {
      it.next().let { header ->
        assertEquals("A,B,C,D,E,F,G", header.iterator().asSequence().joinToString(","))
        assertTrue(it.hasNext())
      }
      it.next().let { record ->
        assertEquals("foo", record["A"])
        assertEquals(CustomPropertyClass.TEXT, record.getType("A"))

        assertEquals(42, record.getInt("B"))
        assertEquals("42.0", record["B"])
        assertEquals(CustomPropertyClass.DOUBLE, record.getType("B"))

        assertEquals(3.14, record.getDouble("C"))
        assertEquals("3.14", record["C"])
        assertEquals(CustomPropertyClass.DOUBLE, record.getType("C"))

        assertEquals(BigDecimal.valueOf(Int.MAX_VALUE.toLong()), record.getBigDecimal("D"))
        assertEquals("${Int.MAX_VALUE.toDouble()}", record["D"])
        assertEquals(CustomPropertyClass.DOUBLE, record.getType("D"))

        assertEquals(true, record.getBoolean("E"))
        assertEquals("true", record["E"])
        assertEquals(CustomPropertyClass.BOOLEAN, record.getType("E"))

        assertEquals(DateParser.parse("2021-04-05"), record.getDate("F"))
        // Text value record["F"] is locale-dependent so we just check that it is not null
        assertNotNull(record["F"])
        assertEquals(CustomPropertyClass.DATE, record.getType("F"))

        assertEquals(null, record["G"])
        assertEquals(null, record.getInt("G"))
        assertEquals(null, record.getType("G"))
      }
    }
  }

  /**
   * Here we write some typed values with "custom" property names using a writer and then
   * check that after reading we create appropriately typed custom properties and their values.
   */
  fun testCustomPropertyTypesImport() {
    val standardFields = listOf(
      TaskRecords.TaskFields.NAME,
      TaskRecords.TaskFields.BEGIN_DATE,
      TaskRecords.TaskFields.DURATION
    ).map { it.toString() }
    val customFields = listOf("F1", "F2", "F3", "F4")
    val header = standardFields + customFields

    val out = ByteArrayOutputStream()
    XlsWriterImpl(out).let {
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
    val inputStream = ByteArrayInputStream(out.toByteArray())
    XlsReaderImpl(inputStream, standardFields).iterator().let {
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
      val customValues = CustomColumnsValues(customPropertyMgr)
      it.next().let{ record ->
        readCustomProperties(headerRecord, customFields, record, customPropertyMgr) { def, value ->
          customValues.addCustomProperty(def, value)
        }
      }
      customPropertyMgr.getCustomPropertyDefinition("F1").let {
        assertEquals(CustomPropertyClass.INTEGER, it.propertyClass)
        assertEquals("42", customValues.getValue(it))
      }

      assertEquals(CustomPropertyClass.DOUBLE, customPropertyMgr.getCustomPropertyDefinition("F2").propertyClass)
      assertEquals(CustomPropertyClass.BOOLEAN, customPropertyMgr.getCustomPropertyDefinition("F3").propertyClass)
      assertEquals(CustomPropertyClass.DATE, customPropertyMgr.getCustomPropertyDefinition("F4").propertyClass)


    }
  }
}
