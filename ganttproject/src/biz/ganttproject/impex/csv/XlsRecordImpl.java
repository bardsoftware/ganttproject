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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author torkhov
 */
class XlsRecordImpl implements SpreadsheetRecord {

  private final List<String> myValues;
  private final Map<String, Integer> myMapping;

  XlsRecordImpl(List<String> values, Map<String, Integer> mapping) {
    myValues = values;
    myMapping = mapping;
  }

  @Override
  public String get(String name) {
    if (myMapping == null) {
      throw new IllegalStateException("No header mapping was specified, the record values can\'t be accessed by name");
    }
    Integer index = myMapping.get(name);
    if (index == null) {
      throw new IllegalArgumentException(String.format("Mapping for %s not found, expected one of %s", name, myMapping.keySet()));
    }
    return (myValues.size() <= index) ? new String() : myValues.get(index);
  }

  @Override
  public String get(int i) {
    return myValues.get(i);
  }

  @Override
  public boolean isMapped(String name) {
    return myMapping != null && myMapping.containsKey(name);
  }

  @Override
  public boolean isSet(String name) {
    return isMapped(name) && myMapping.get(name) >= 0 && myMapping.get(name) < myValues.size();
  }

  @Override
  public Iterator<String> iterator() {
    return myValues.iterator();
  }

  @Override
  public int size() {
    return myValues.size();
  }
}