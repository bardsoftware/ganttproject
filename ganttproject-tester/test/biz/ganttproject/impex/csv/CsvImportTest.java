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

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;

import junit.framework.TestCase;

import static biz.ganttproject.impex.csv.SpreadsheetFormat.CSV;
import static biz.ganttproject.impex.csv.SpreadsheetFormat.XLS;
import static com.google.common.base.Charsets.UTF_8;

/**
 * Tests for spreadsheet (CSV and XLS) import.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class CsvImportTest extends TestCase {

  private Supplier<InputStream> createSupplier(String data) {
    return createSupplier(data.getBytes(UTF_8));
  }

  private Supplier<InputStream> createSupplier(final byte[] data) {
    return () -> new ByteArrayInputStream(data);
  }

  public void testBasic() throws Exception {
    String header = "A, B";
    String data = "a1, b1";

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    RecordGroup recordGroup = createBasicRecordGroup(wasCalled);
    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header, data)), CSV, recordGroup);
    importer.load();
    assertTrue(wasCalled.get());

    wasCalled = new AtomicBoolean(false);
    recordGroup = createBasicRecordGroup(wasCalled);
    importer = new GanttCSVOpen(createSupplier(createXls(header, data)), XLS, recordGroup);
    importer.load();
    assertTrue(wasCalled.get());
  }

  public void testSkipEmptyLine() throws Exception {
    String header = "A, B";
    String data = "a1, b1";

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    RecordGroup recordGroup = createBasicRecordGroup(wasCalled);
    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header, "", data)), CSV, recordGroup);
    importer.load();
    assertTrue(wasCalled.get());

    wasCalled = new AtomicBoolean(false);
    recordGroup = createBasicRecordGroup(wasCalled);
    importer = new GanttCSVOpen(createSupplier(createXls(header, "", data)), XLS, recordGroup);
    importer.load();
    assertTrue(wasCalled.get());
  }

  public void testTwoGroups() throws Exception {
    String header1 = "A, B";
    String data1 = "a1, b1";
    AtomicBoolean wasCalled1 = new AtomicBoolean(false);
    RecordGroup recordGroup1 = createTwoGroupsFirstRecordGroup(wasCalled1);

    String header2 = "C, D, E";
    String data2 = "c1, d1, e1";
    AtomicBoolean wasCalled2 = new AtomicBoolean(false);
    RecordGroup recordGroup2 = createTwoGroupsSecondRecordGroup(wasCalled2);

    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header1, data1, "", header2, data2)),
        CSV, recordGroup1, recordGroup2);
    importer.load();
    assertTrue(wasCalled1.get() && wasCalled2.get());

    wasCalled1 = new AtomicBoolean(false);
    recordGroup1 = createTwoGroupsFirstRecordGroup(wasCalled1);
    wasCalled2 = new AtomicBoolean(false);
    recordGroup2 = createTwoGroupsSecondRecordGroup(wasCalled2);
    importer = new GanttCSVOpen(createSupplier(createXls(header1, data1, "", header2, data2)),
        XLS, recordGroup1, recordGroup2);
    importer.load();
    assertTrue(wasCalled1.get() && wasCalled2.get());
  }

  public void testIncompleteHeader() throws Exception {
    String header = "A, B";
    String data = "a1, b1";

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    RecordGroup recordGroup = createIncompleteHeaderRecordGroup(wasCalled);
    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header, data)), CSV, recordGroup);
    importer.load();
    assertTrue(wasCalled.get());

    wasCalled = new AtomicBoolean(false);
    recordGroup = createIncompleteHeaderRecordGroup(wasCalled);
    importer = new GanttCSVOpen(createSupplier(createXls(header, data)), XLS, recordGroup);
    importer.load();
    assertTrue(wasCalled.get());
  }

  public void testSkipUntilFirstHeader() throws Exception {
    String notHeader = "FOO, BAR, A";
    String header = "A, B";
    String data = "a1, b1";

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    RecordGroup recordGroup = createSkipUntilFirstHeaderRecordGroup(wasCalled);
    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(notHeader, header, data)), CSV, recordGroup);
    importer.load();
    assertTrue(wasCalled.get());
    assertEquals(1, importer.getSkippedLineCount());

    wasCalled = new AtomicBoolean(false);
    recordGroup = createSkipUntilFirstHeaderRecordGroup(wasCalled);
    importer = new GanttCSVOpen(createSupplier(createXls(notHeader, header, data)), XLS, recordGroup);
    importer.load();
    assertTrue(wasCalled.get());
    assertEquals(1, importer.getSkippedLineCount());
  }

  public void testSkipLinesWithEmptyMandatoryFields() throws Exception {
    String header = "A, B, C";
    String data1 = "a1,,c1";
    String data2 = "a2,b2,c2";
    String data3 = ",b3,c3";

    AtomicBoolean wasCalled = new AtomicBoolean(false);
    RecordGroup recordGroup = createSkipLinesWithEmptyMandatoryFieldsRecordGroup(wasCalled);
    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header, data1, data2, data3)), CSV, recordGroup);
    importer.load();
    assertTrue(wasCalled.get());
    assertEquals(2, importer.getSkippedLineCount());

    wasCalled = new AtomicBoolean(false);
    recordGroup = createSkipLinesWithEmptyMandatoryFieldsRecordGroup(wasCalled);
    importer = new GanttCSVOpen(createSupplier(createXls(header, data1, data2, data3)), XLS, recordGroup);
    importer.load();
    assertTrue(wasCalled.get());
    assertEquals(2, importer.getSkippedLineCount());
  }

  private RecordGroup createBasicRecordGroup(final AtomicBoolean wasCalled) {
    return new RecordGroup("AB", ImmutableSet.of("A", "B")) {
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
  }

  private RecordGroup createTwoGroupsFirstRecordGroup(final AtomicBoolean wasCalled) {
    return new RecordGroup("AB", ImmutableSet.of("A", "B")) {
      @Override
      protected boolean doProcess(SpreadsheetRecord record) {
        if (!super.doProcess(record)) {
          return false;
        }
        assertEquals("a1", record.get("A"));
        assertEquals("b1", record.get("B"));
        wasCalled.set(true);
        return true;
      }
    };
  }

  private RecordGroup createTwoGroupsSecondRecordGroup(final AtomicBoolean wasCalled) {
    return new RecordGroup("CDE", ImmutableSet.of("C", "D", "E")) {
      @Override
      protected boolean doProcess(SpreadsheetRecord record) {
        if (!super.doProcess(record)) {
          return false;
        }
        assertEquals("c1", record.get("C"));
        assertEquals("d1", record.get("D"));
        assertEquals("e1", record.get("E"));
        wasCalled.set(true);
        return true;
      }
    };
  }

  private RecordGroup createIncompleteHeaderRecordGroup(final AtomicBoolean wasCalled) {
    return new RecordGroup("ABC",
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
  }

  private RecordGroup createSkipUntilFirstHeaderRecordGroup(final AtomicBoolean wasCalled) {
    return new RecordGroup("ABC", ImmutableSet.of("A", "B")) {
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
  }

  private RecordGroup createSkipLinesWithEmptyMandatoryFieldsRecordGroup(final AtomicBoolean wasCalled) {
    return new RecordGroup("ABC", ImmutableSet.of("A", "B", "C"), ImmutableSet.of("A", "B")) {
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
  }

  private byte[] createXls(String... rows) throws Exception {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try (SpreadsheetWriter writer = new XlsWriterImpl(stream)) {
      for (String row : rows) {
        for (String cel : row.split(",")) {
          writer.print(cel.trim());
        }
        writer.println();
      }
    }
    return stream.toByteArray();
  }
}
