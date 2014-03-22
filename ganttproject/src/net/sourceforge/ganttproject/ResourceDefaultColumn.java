package net.sourceforge.ganttproject;

import java.util.ArrayList;
import java.util.List;

import biz.ganttproject.core.table.ColumnList;
import biz.ganttproject.core.table.ColumnList.Column;

public enum ResourceDefaultColumn {
  NAME(new ColumnList.ColumnStub("0", null, true, 0, 200), true),
  ROLE(new ColumnList.ColumnStub("1", null, true, 1, 75), true),
  EMAIL(new ColumnList.ColumnStub("2", null, false, -1, 75), true),
  PHONE(new ColumnList.ColumnStub("3", null, false, -1, 50), true),
  ROLE_IN_TASK(new ColumnList.ColumnStub("4", null, false, -1, 75), true),
  STANDARD_RATE(new ColumnList.ColumnStub("5", null, false, -1, 75), true);

  private final Column myDelegate;
  private final boolean isEditable;

  private ResourceDefaultColumn(ColumnList.Column delegate, boolean editable) {
    myDelegate = delegate;
    isEditable = editable;
  }

  Column getStub() {
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
}