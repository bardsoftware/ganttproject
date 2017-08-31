/*
Copyright 2017 Roman Torkhov, BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

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
package biz.ganttproject.impex.csv;

import org.apache.commons.csv.CSVRecord;

import java.util.Iterator;

/**
 * @author torkhov
 */
class CsvRecordImpl implements SpreadsheetRecord {

  private final CSVRecord myRecord;

  CsvRecordImpl(CSVRecord record) {
    myRecord = record;
  }

  @Override
  public String get(String name) {
    return (isSet(name)) ? myRecord.get(name) : new String();
  }

  @Override
  public String get(int i) {
    return myRecord.get(i);
  }

  @Override
  public boolean isMapped(String name) {
    return myRecord.isMapped(name);
  }

  @Override
  public boolean isSet(String name) {
    return myRecord.isSet(name);
  }

  @Override
  public Iterator<String> iterator() {
    return myRecord.iterator();
  }

  @Override
  public int size() {
    return myRecord.size();
  }
}