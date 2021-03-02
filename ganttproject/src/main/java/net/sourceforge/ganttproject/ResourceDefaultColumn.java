/*
Copyright 2014 BarD Software s.r.o

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
package net.sourceforge.ganttproject;

import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.table.ColumnList.Column;
import com.google.common.base.MoreObjects;
import net.sourceforge.ganttproject.language.GanttLanguage;

import java.util.ArrayList;
import java.util.List;

/**
 * Default columns in the resource table
 *
 * @author dbarashev (Dmitry Barashev)
 */
public enum ResourceDefaultColumn {
  ID(new ColumnList.ColumnStub("", null, false, 0, 75), Integer.class, "tableColID", true),
  NAME(new ColumnList.ColumnStub("0", null, true, 0, 200), String.class, "tableColResourceName", true),
  ROLE(new ColumnList.ColumnStub("1", null, true, 1, 75), String.class, "tableColResourceRole", true),
  EMAIL(new ColumnList.ColumnStub("2", null, false, -1, 75), String.class, "tableColResourceEMail", true),
  PHONE(new ColumnList.ColumnStub("3", null, false, -1, 50), String.class, "tableColResourcePhone", true),
  ROLE_IN_TASK(new ColumnList.ColumnStub("4", null, false, -1, 75), String.class, "tableColResourceRoleForTask", true),
  STANDARD_RATE(new ColumnList.ColumnStub("5", null, false, -1, 75), Double.class, "tableColResourceRate", true),
  TOTAL_COST(new ColumnList.ColumnStub("6", null, false, -1, 50), Double.class, "tableColResourceCost", false),
  TOTAL_LOAD(new ColumnList.ColumnStub("7", null, false, -1, 50), Double.class, "tableColResourceLoad", false);

  private final Column myDelegate;
  private final boolean isEditable;
  private final String myNameKey;
  private final Class<?> myValueClass;

  private ResourceDefaultColumn(ColumnList.Column delegate, Class<?> valueClass, String nameKey, boolean editable) {
    myDelegate = delegate;
    myNameKey = nameKey;
    isEditable = editable;
    myValueClass = valueClass;
  }

  public Column getStub() {
    return myDelegate;
  }

  static List<Column> getColumnStubs() {
    List<Column> result = new ArrayList<Column>();
    for (ResourceDefaultColumn dc : values()) {
      result.add(dc.myDelegate);
    }
    return result;
  }

  public boolean isEditable() {
    return isEditable;
  }

  public String getName() {
    return MoreObjects.firstNonNull(GanttLanguage.getInstance().getText(myNameKey), myNameKey);
  }

  @Override
  public String toString() {
    return getName();
  }

  public Class<?> getValueClass() {
    return myValueClass;
  }

  public static ResourceDefaultColumn find(String id) {
    for (ResourceDefaultColumn column : values()) {
      if (column.getStub().getID().equals(id)) {
        return column;
      }
    }
    return null;
  }
}
