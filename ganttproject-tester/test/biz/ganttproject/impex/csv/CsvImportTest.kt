/*
 * Copyright (c) 2012-2021 Dmitry Barashev, BarD Software s.r.o.
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

import biz.ganttproject.customproperty.CustomPropertyClass
import com.google.common.base.Charsets
import com.google.common.base.Joiner
import com.google.common.base.Supplier
import com.google.common.collect.ImmutableSet
import junit.framework.TestCase
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.util.collect.Pair
import org.w3c.util.DateParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tests for spreadsheet (CSV and XLS) import.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class CsvImportTest : TestCase() {
  private fun createSupplier(data: ByteArray): Supplier<InputStream> {
    return Supplier { ByteArrayInputStream(data) }
  }

  @Throws(Exception::class)
  fun testBasic() {
    val header = "A, B"
    val data = "a1, b1"
    for (pair in createPairs(header, data)) {
      val wasCalled = AtomicBoolean(false)
      val recordGroup = RecordGroup("AB", ImmutableSet.of("A", "B")) { record, _ ->
          wasCalled.set(true)
          assertEquals("a1", record["A"])
          assertEquals("b1", record["B"])
          true
        }
      val importer = GanttCSVOpen(pair.second(), pair.first(), recordGroup)
      importer.load()
      assertTrue(wasCalled.get())
    }
  }

  @Throws(Exception::class)
  fun testSkipEmptyLine() {
    val header = "A, B"
    val data = "a1, b1"
    for (pair in createPairs(header, "", data)) {
      val wasCalled = AtomicBoolean(false)
      val recordGroup = RecordGroup("AB", ImmutableSet.of("A", "B")) { record, _ ->
          wasCalled.set(true)
          assertEquals("a1", record["A"])
          assertEquals("b1", record["B"])
          true
      }
      val importer = GanttCSVOpen(pair.second(), pair.first(), recordGroup)
      importer.load()
      assertTrue(wasCalled.get())
    }
  }

  @Throws(Exception::class)
  private fun doTestTwoGroups(groupSeparator: String) {
    val header1 = "A, B"
    val data1 = "a1, b1"
    val header2 = "C, D, E"
    val data2 = "c1, d1, e1"
    for (pair in createPairs(header1, data1, "", header2, data2)) {
      val wasCalled1 = AtomicBoolean(false)
      val recordGroup1 = RecordGroup("AB", ImmutableSet.of("A", "B")) { record, _ ->
          assertEquals("a1", record["A"])
          assertEquals("b1", record["B"])
          wasCalled1.set(true)
          true
        }
      val wasCalled2 = AtomicBoolean(false)
      val recordGroup2 = RecordGroup("CDE", ImmutableSet.of("C", "D", "E")) { record, _ ->
          assertEquals("c1", record["C"])
          assertEquals("d1", record["D"])
          assertEquals("e1", record["E"])
          wasCalled2.set(true)
          true
        }
      val importer = GanttCSVOpen(
        createSupplier(
          Joiner.on('\n').join(header1, data1, groupSeparator, header2, data2).toByteArray(
            Charsets.UTF_8
          )
        ), SpreadsheetFormat.CSV,
        recordGroup1, recordGroup2
      )
      importer.load()
      assertTrue(wasCalled1.get() && wasCalled2.get())
    }
  }

  @Throws(Exception::class)
  fun testTwoGroups() {
    doTestTwoGroups("")
    doTestTwoGroups(",,,,,,,,")
    doTestTwoGroups("           ")
  }

  @Throws(Exception::class)
  fun testIncompleteHeader() {
    val header = "A, B"
    val data = "a1, b1"
    for (pair in createPairs(header, data)) {
      val wasCalled = AtomicBoolean(false)
      val recordGroup = RecordGroup(
        "ABC",
        ImmutableSet.of("A", "B", "C"),  // all fields
        ImmutableSet.of("A", "B")
      ) { record, _ ->
          wasCalled.set(true)
          assertEquals("a1", record["A"])
          assertEquals("b1", record["B"])
          true
        }
      val importer = GanttCSVOpen(pair.second(), pair.first(), recordGroup)
      importer.load()
      assertTrue(wasCalled.get())
    }
  }

  @Throws(Exception::class)
  fun testSkipUntilFirstHeader() {
    val notHeader = "FOO, BAR, A"
    val header = "A, B"
    val data = "a1, b1"
    for (pair in createPairs(notHeader, header, data)) {
      val wasCalled = AtomicBoolean(false)
      val recordGroup = RecordGroup("ABC", ImmutableSet.of("A", "B")) { record, group ->
        if (group.header == null) {
          false
        } else {
          wasCalled.set(true)
          assertEquals("a1", record["A"])
          assertEquals("b1", record["B"])
          true
        }
      }
      val importer = GanttCSVOpen(pair.second(), pair.first(), recordGroup)
      importer.load()
      assertTrue(wasCalled.get())
      assertEquals(1, importer.skippedLineCount)
    }
  }

  @Throws(Exception::class)
  fun testSkipLinesWithEmptyMandatoryFields() {
    val header = "A, B, C"
    val data1 = "a1,,c1"
    val data2 = "a2,b2,c2"
    val data3 = ",b3,c3"
    for (pair in createPairs(header, data1, data2, data3)) {
      val wasCalled = AtomicBoolean(false)
      val recordGroup =
        RecordGroup("ABC", ImmutableSet.of("A", "B", "C"), ImmutableSet.of("A", "B")) { record, group ->
            if (!group.hasMandatoryFields(record)) {
              false
            } else {
              wasCalled.set(true)
              assertEquals("a2", record["A"])
              assertEquals("b2", record["B"])
              true
            }
          }
      val importer = GanttCSVOpen(pair.second(), pair.first(), recordGroup)
      importer.load()
      assertTrue(wasCalled.get())
      assertEquals(2, importer.skippedLineCount)
    }
  }

  @Throws(Exception::class)
  private fun createPairs(vararg data: String): List<Pair<SpreadsheetFormat?, Supplier<InputStream>?>> {
    val pairs: MutableList<Pair<SpreadsheetFormat?, Supplier<InputStream>?>> = ArrayList()
    pairs.add(
      Pair.create(
        SpreadsheetFormat.CSV,
        createSupplier(Joiner.on('\n').join(data).toByteArray(Charsets.UTF_8))
      )
    )
    pairs.add(Pair.create(SpreadsheetFormat.XLS, createSupplier(createXls(*data))))
    return pairs
  }

  @Throws(Exception::class)
  private fun createXls(vararg rows: String): ByteArray {
    val stream = ByteArrayOutputStream()
    XlsWriterImpl(stream).use { writer ->
      for (row in rows) {
        for (cell in row.split(",").dropLastWhile { it.isEmpty() }.toTypedArray()) {
          writer.print(cell.trim { it <= ' ' })
        }
        writer.println()
      }
    }
    return stream.toByteArray()
  }

  @Throws(IOException::class)
  fun testTrailingEmptyCells() {
    val header1 = "A, B, C, D"
    val data1 = "a1, b1, c1, d1"
    val wasCalled1 = AtomicBoolean(false)
    val recordGroup1 = RecordGroup("ABCD", ImmutableSet.of("A", "B", "C", "D")) { record, _ ->
        assertEquals("a1", record["A"])
        assertEquals("b1", record["B"])
        assertEquals("c1", record["C"])
        assertEquals("d1", record["D"])
        wasCalled1.set(true)
        true
      }
    val header2 = "E,,,"
    val data2 = "e1,,,"
    val wasCalled2 = AtomicBoolean(false)
    val recordGroup2 = RecordGroup("E", ImmutableSet.of("E")) { record, _ ->
        assertEquals("e1", record["E"])
        assertFalse(record.isMapped("B"))
        wasCalled2.set(true)
        true
      }
    val importer = GanttCSVOpen(
      createSupplier(
        Joiner.on('\n').join(header1, data1, ",,,", header2, data2).toByteArray(
          Charsets.UTF_8
        )
      ), SpreadsheetFormat.CSV,
      recordGroup1, recordGroup2
    )
    importer.load()
    assertTrue(wasCalled1.get() && wasCalled2.get())
  }

  fun testDataTypesFromStrings() {
    val today = DateParser.parse("2021-02-05")
    val header = "A,B,C,D,E"
    val data = "1, 3.14, true, ${GanttLanguage.getInstance().shortDateFormat.format(today)}, 6.15"
    for (pair in createPairs(header, data)) {
      val recordGroup = RecordGroup("AB", header.split(",").toSet()) { record, _ ->
        assertEquals(1, record.getInt("A"))
        assertEquals(3.14, record.getDouble("B"))
        assertEquals(true, record.getBoolean("C"))
        assertEquals(today, record.getDate("D"))
        assertEquals(BigDecimal.valueOf(6.15), record.getBigDecimal("E"))

        // Although we can read typed values, they are actually strings in the input
        header.split(",").forEach { assertEquals(CustomPropertyClass.TEXT, record.getType(it)) }
        true
      }
      val importer = GanttCSVOpen(pair.second(), pair.first(), recordGroup)
      importer.load()
    }
  }
}
