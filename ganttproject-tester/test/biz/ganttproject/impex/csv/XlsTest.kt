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

import biz.ganttproject.core.time.GanttCalendar
import junit.framework.TestCase
import net.sourceforge.ganttproject.CustomPropertyClass
import org.w3c.util.DateParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal

/**
 * @author dbarashev@bardsoftware.com
 */
class XlsTest : TestCase() {
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
        assertEquals(CustomPropertyClass.DOUBLE, record.getType("D"))

        assertEquals(true, record.getBoolean("E"))
        assertEquals("true", record["E"])
        assertEquals(CustomPropertyClass.BOOLEAN, record.getType("E"))

        assertEquals(DateParser.parse("2021-04-05"), record.getDate("F"))
        assertEquals("4/5/21", record["F"])
        assertEquals(CustomPropertyClass.DATE, record.getType("F"))

        assertEquals(null, record["G"])
        assertEquals(null, record.getInt("G"))
      }
    }

  }
}
