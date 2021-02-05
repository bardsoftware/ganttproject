/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package biz.ganttproject.impex.csv

import com.google.common.base.Charsets
import com.google.common.base.Joiner
import com.google.common.base.Supplier
import com.google.common.collect.ImmutableSet
import junit.framework.TestCase
import net.sourceforge.ganttproject.util.collect.Pair
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
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
      val recordGroup: RecordGroup = object : RecordGroup("AB", ImmutableSet.of("A", "B")) {
        override fun doProcess(record: SpreadsheetRecord): Boolean {
          if (!super.doProcess(record)) {
            return false
          }
          wasCalled.set(true)
          assertEquals("a1", record["A"])
          assertEquals("b1", record["B"])
          return true
        }
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
      val recordGroup: RecordGroup = object : RecordGroup("AB", ImmutableSet.of("A", "B")) {
        override fun doProcess(record: SpreadsheetRecord): Boolean {
          if (!super.doProcess(record)) {
            return false
          }
          wasCalled.set(true)
          assertEquals("a1", record["A"])
          assertEquals("b1", record["B"])
          return true
        }
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
      val recordGroup1: RecordGroup = object : RecordGroup("AB", ImmutableSet.of("A", "B")) {
        override fun doProcess(record: SpreadsheetRecord): Boolean {
          if (!super.doProcess(record)) {
            return false
          }
          assertEquals("a1", record["A"])
          assertEquals("b1", record["B"])
          wasCalled1.set(true)
          return true
        }
      }
      val wasCalled2 = AtomicBoolean(false)
      val recordGroup2: RecordGroup = object : RecordGroup("CDE", ImmutableSet.of("C", "D", "E")) {
        override fun doProcess(record: SpreadsheetRecord): Boolean {
          if (!super.doProcess(record)) {
            return false
          }
          assertEquals("c1", record["C"])
          assertEquals("d1", record["D"])
          assertEquals("e1", record["E"])
          wasCalled2.set(true)
          return true
        }
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
      val recordGroup: RecordGroup = object : RecordGroup(
        "ABC",
        ImmutableSet.of("A", "B", "C"),  // all fields
        ImmutableSet.of("A", "B")
      ) {
        // mandatory fields
        override fun doProcess(record: SpreadsheetRecord): Boolean {
          if (!super.doProcess(record)) {
            return false
          }
          wasCalled.set(true)
          assertEquals("a1", record["A"])
          assertEquals("b1", record["B"])
          return true
        }
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
      val recordGroup: RecordGroup = object : RecordGroup("ABC", ImmutableSet.of("A", "B")) {
        override fun doProcess(record: SpreadsheetRecord): Boolean {
          if (!super.doProcess(record)) {
            return false
          }
          wasCalled.set(true)
          assertEquals("a1", record["A"])
          assertEquals("b1", record["B"])
          return true
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
      val recordGroup: RecordGroup =
        object : RecordGroup("ABC", ImmutableSet.of("A", "B", "C"), ImmutableSet.of("A", "B")) {
          override fun doProcess(record: SpreadsheetRecord): Boolean {
            if (!super.doProcess(record)) {
              return false
            }
            if (!hasMandatoryFields(record)) {
              return false
            }
            wasCalled.set(true)
            assertEquals("a2", record["A"])
            assertEquals("b2", record["B"])
            return true
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
    val recordGroup1: RecordGroup = object : RecordGroup("ABCD", ImmutableSet.of("A", "B", "C", "D")) {
      override fun doProcess(record: SpreadsheetRecord): Boolean {
        if (!super.doProcess(record)) {
          return false
        }
        assertEquals("a1", record["A"])
        assertEquals("b1", record["B"])
        assertEquals("c1", record["C"])
        assertEquals("d1", record["D"])
        wasCalled1.set(true)
        return true
      }
    }
    val header2 = "E,,,"
    val data2 = "e1,,,"
    val wasCalled2 = AtomicBoolean(false)
    val recordGroup2: RecordGroup = object : RecordGroup("E", ImmutableSet.of("E")) {
      override fun doProcess(record: SpreadsheetRecord): Boolean {
        if (!super.doProcess(record)) {
          return false
        }
        assertEquals("e1", record["E"])
        assertFalse(record.isMapped("B"))
        wasCalled2.set(true)
        return true
      }
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
}
