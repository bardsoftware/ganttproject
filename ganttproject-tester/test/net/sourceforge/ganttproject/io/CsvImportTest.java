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
package net.sourceforge.ganttproject.io;

import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.csv.CSVRecord;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;

import junit.framework.TestCase;

/**
 * Tests for CSV import.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class CsvImportTest extends TestCase {
  private Supplier<Reader> createSupplier(String data) {
    return Suppliers.<Reader> ofInstance(new StringReader(data));
  }

  public void testBasic() throws Exception {
    String header = "A, B";
    String data = "a1, b1";
    final AtomicBoolean wasCalled = new AtomicBoolean(false);
    GanttCSVOpen.RecordGroup recordGroup = new GanttCSVOpen.RecordGroup(ImmutableSet.<String> of("A", "B")) {
      @Override
      protected void process(CSVRecord record) {
        wasCalled.set(true);
        assertEquals("a1", record.get("A"));
        assertEquals("b1", record.get("B"));
      }
    };
    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header, data)), recordGroup);
    importer.load();
    assertTrue(wasCalled.get());

    // Now test with one empty line between header and data
    wasCalled.set(false);
    importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header, "", data)), recordGroup);
    importer.load();
    assertTrue(wasCalled.get());
  }

  public void testTwoGroups() throws Exception {
    String header1 = "A, B";
    String data1 = "a1, b1";
    final AtomicBoolean wasCalled1 = new AtomicBoolean(false);
    GanttCSVOpen.RecordGroup recordGroup1 = new GanttCSVOpen.RecordGroup(ImmutableSet.<String> of("A", "B")) {
      @Override
      protected void process(CSVRecord record) {
        assertEquals("a1", record.get("A"));
        assertEquals("b1", record.get("B"));
        wasCalled1.set(true);
      }
    };

    String header2 = "C, D, E";
    String data2 = "c1, d1, e1";
    final AtomicBoolean wasCalled2 = new AtomicBoolean(false);
    GanttCSVOpen.RecordGroup recordGroup2 = new GanttCSVOpen.RecordGroup(ImmutableSet.<String> of("C", "D", "E")) {
      @Override
      protected void process(CSVRecord record) {
        assertEquals("c1", record.get("C"));
        assertEquals("d1", record.get("D"));
        assertEquals("e1", record.get("E"));
        wasCalled2.set(true);
      }
    };
    GanttCSVOpen importer = new GanttCSVOpen(createSupplier(Joiner.on('\n').join(header1, data1, "", header2, data2)),
        recordGroup1, recordGroup2);
    importer.load();
    assertTrue(wasCalled1.get() && wasCalled2.get());
  }
}
