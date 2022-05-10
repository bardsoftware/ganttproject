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
import biz.ganttproject.customproperty.CustomPropertyClass
import junit.framework.TestCase
import net.sourceforge.ganttproject.language.GanttLanguage
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
}
