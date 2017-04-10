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
package biz.ganttproject.impex.csv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;

import junit.framework.TestCase;
import net.sourceforge.ganttproject.util.collect.Pair;

import static biz.ganttproject.impex.csv.SpreadsheetFormat.CSV;
import static biz.ganttproject.impex.csv.SpreadsheetFormat.XLS;

/**
 * Tests for spreadsheet (CSV and XLS) import.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class CsvImportTest extends TestCase {

  private Supplier<InputStream> createSupplier(final byte[] data) {
    return () -> new ByteArrayInputStream(data);
  }

  public void testBasic() throws Exception {
    String header = "A, B";
    String data = "a1, b1";

    doTestBasic(header, data);
  }

  public void testSkipEmptyLine() throws Exception {
    String header = "A, B";
    String data = "a1, b1";

    doTestSkipEmptyLine(header, "", data);
  }

  public void testTwoGroups() throws Exception {
    String header1 = "A, B";
    String data1 = "a1, b1";

    String header2 = "C, D, E";
    String data2 = "c1, d1, e1";

    doTestTwoGroups(header1, data1, "", header2, data2);
  }

  public void testIncompleteHeader() throws Exception {
    String header = "A, B";
    String data = "a1, b1";

    doTestIncompleteHeader(header, data);
  }

  public void testSkipUntilFirstHeader() throws Exception {
    String notHeader = "FOO, BAR, A";
    String header = "A, B";
    String data = "a1, b1";

    doTestSkipUntilFirstHeader(notHeader, header, data);
  }

  public void testSkipLinesWithEmptyMandatoryFields() throws Exception {
    String header = "A, B, C";
    String data1 = "a1,,c1";
    String data2 = "a2,b2,c2";
    String data3 = ",b3,c3";

    doTestSkipLinesWithEmptyMandatoryFields(header, data1, data2, data3);
  }

  private void doTestBasic(String... data) throws Exception {
    for (Pair<SpreadsheetFormat, Supplier<InputStream>> pair : createPairs(data)) {
      final AtomicBoolean wasCalled = new AtomicBoolean(false);
      RecordGroup recordGroup = new RecordGroup("AB", ImmutableSet.of("A", "B")) {
        @Override
        protected boolean doProcess(SpreadsheetRecord record) {
          if (!super.doProcess(record)) {
            return false;
          }
          wasCalled.set(true);
          assertEquals("a1", record.get("A"));
          assertEquals("b1", record.get("B"));
          return true;
        }
      };
      GanttCSVOpen importer = new GanttCSVOpen(pair.second(), pair.first(), recordGroup);
      importer.load();
      assertTrue(wasCalled.get());
    }
  }

  private void doTestSkipEmptyLine(String... data) throws Exception {
    for (Pair<SpreadsheetFormat, Supplier<InputStream>> pair : createPairs(data)) {
      final AtomicBoolean wasCalled = new AtomicBoolean(false);
      RecordGroup recordGroup = new RecordGroup("AB", ImmutableSet.of("A", "B")) {
        @Override
        protected boolean doProcess(SpreadsheetRecord record) {
          if (!super.doProcess(record)) {
            return false;
          }
          wasCalled.set(true);
          assertEquals("a1", record.get("A"));
          assertEquals("b1", record.get("B"));
          return true;
        }
      };
      GanttCSVOpen importer = new GanttCSVOpen(pair.second(), pair.first(), recordGroup);
      importer.load();
      assertTrue(wasCalled.get());
    }
  }

  private void doTestTwoGroups(String... data) throws Exception {
    for (Pair<SpreadsheetFormat, Supplier<InputStream>> pair : createPairs(data)) {
      final AtomicBoolean wasCalled1 = new AtomicBoolean(false);
      RecordGroup recordGroup1 = new RecordGroup("AB", ImmutableSet.of("A", "B")) {
        @Override
        protected boolean doProcess(SpreadsheetRecord record) {
          if (!super.doProcess(record)) {
            return false;
          }
          assertEquals("a1", record.get("A"));
          assertEquals("b1", record.get("B"));
          wasCalled1.set(true);
          return true;
        }
      };

      final AtomicBoolean wasCalled2 = new AtomicBoolean(false);
      RecordGroup recordGroup2 = new RecordGroup("CDE", ImmutableSet.of("C", "D", "E")) {
        @Override
        protected boolean doProcess(SpreadsheetRecord record) {
          if (!super.doProcess(record)) {
            return false;
          }
          assertEquals("c1", record.get("C"));
          assertEquals("d1", record.get("D"));
          assertEquals("e1", record.get("E"));
          wasCalled2.set(true);
          return true;
        }
      };

      GanttCSVOpen importer = new GanttCSVOpen(pair.second(), pair.first(), recordGroup1, recordGroup2);
      importer.load();
      assertTrue(wasCalled1.get() && wasCalled2.get());
    }
  }

  private void doTestIncompleteHeader(String... data) throws Exception {
    for (Pair<SpreadsheetFormat, Supplier<InputStream>> pair : createPairs(data)) {
      final AtomicBoolean wasCalled = new AtomicBoolean(false);
      RecordGroup recordGroup = new RecordGroup("ABC",
          ImmutableSet.of("A", "B", "C"), // all fields
          ImmutableSet.of("A", "B")) { // mandatory fields
        @Override
        protected boolean doProcess(SpreadsheetRecord record) {
          if (!super.doProcess(record)) {
            return false;
          }
          wasCalled.set(true);
          assertEquals("a1", record.get("A"));
          assertEquals("b1", record.get("B"));
          return true;
        }
      };
      GanttCSVOpen importer = new GanttCSVOpen(pair.second(), pair.first(), recordGroup);
      importer.load();
      assertTrue(wasCalled.get());
    }
  }

  private void doTestSkipUntilFirstHeader(String... data) throws Exception {
    for (Pair<SpreadsheetFormat, Supplier<InputStream>> pair : createPairs(data)) {
      final AtomicBoolean wasCalled = new AtomicBoolean(false);
      RecordGroup recordGroup = new RecordGroup("ABC", ImmutableSet.of("A", "B")) {
        @Override
        protected boolean doProcess(SpreadsheetRecord record) {
          if (!super.doProcess(record)) {
            return false;
          }
          wasCalled.set(true);
          assertEquals("a1", record.get("A"));
          assertEquals("b1", record.get("B"));
          return true;
        }
      };
      GanttCSVOpen importer = new GanttCSVOpen(pair.second(), pair.first(), recordGroup);
      importer.load();
      assertTrue(wasCalled.get());
      assertEquals(1, importer.getSkippedLineCount());
    }
  }

  private void doTestSkipLinesWithEmptyMandatoryFields(String... data) throws Exception {
    for (Pair<SpreadsheetFormat, Supplier<InputStream>> pair : createPairs(data)) {
      final AtomicBoolean wasCalled = new AtomicBoolean(false);
      RecordGroup recordGroup = new RecordGroup("ABC", ImmutableSet.of("A", "B", "C"), ImmutableSet.of("A", "B")) {
        @Override
        protected boolean doProcess(SpreadsheetRecord record) {
          if (!super.doProcess(record)) {
            return false;
          }
          if (!hasMandatoryFields(record)) {
            return false;
          }
          wasCalled.set(true);
          assertEquals("a2", record.get("A"));
          assertEquals("b2", record.get("B"));
          return true;
        }
      };
      GanttCSVOpen importer = new GanttCSVOpen(pair.second(), pair.first(), recordGroup);
      importer.load();
      assertTrue(wasCalled.get());
      assertEquals(2, importer.getSkippedLineCount());
    }
  }

  private List<Pair<SpreadsheetFormat, Supplier<InputStream>>> createPairs(String... data) throws Exception {
    List<Pair<SpreadsheetFormat, Supplier<InputStream>>> pairs = new ArrayList<>();
    pairs.add(Pair.create(CSV, createSupplier(Joiner.on('\n').join(data).getBytes(Charsets.UTF_8))));
    pairs.add(Pair.create(XLS, createSupplier(createXls(data))));
    return pairs;
  }

  private byte[] createXls(String... rows) throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try (SpreadsheetWriter writer = new XlsWriterImpl(stream)) {
      for (String row : rows) {
        for (String cell : row.split(",")) {
          writer.print(cell.trim());
        }
        writer.println();
      }
    }
    return stream.toByteArray();
  }
}
