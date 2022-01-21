/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package biz.ganttproject.core.table;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

public interface ColumnList {
  int getSize();

  Column getField(int index);

  void clear();

  void add(String name, int order, int width);

  void importData(ColumnList source, boolean keepVisibleColumns);

  List<Column> exportData();

  public interface Column {
    SortOrder getSort();

    void setSort(SortOrder sort);

    String getID();

    String getName();

    int getOrder();

    int getWidth();

    boolean isVisible();

    void setVisible(boolean visible);

    void setOrder(int order);

    void setWidth(int width);
  }

  class ColumnStub implements ColumnList.Column {
    private final String myID;
    private int myOrder;
    private int myWidth;
    private String myName;
    private boolean isVisible;
    private SortOrder mySortOrder = SortOrder.UNSORTED;
    private Runnable onChange = () -> {};

    public ColumnStub(Column copy) {
      this(copy.getID(), copy.getName(), copy.isVisible(), copy.getOrder(), copy.getWidth());
    }
    public ColumnStub(String id, String name, boolean visible, int order, int width) {
      myName = name == null ? id : name;
      myID = id;
      myOrder = order;
      myWidth = width;
      isVisible = visible;
    }

    @Override
    public SortOrder getSort() {
      return mySortOrder;
    }

    @Override
    public void setSort(SortOrder sort) {
      mySortOrder = sort;
    }

    @Override
    public String getID() {
      return myID;
    }

    @Override
    public int getOrder() {
      return myOrder;
    }

    @Override
    public int getWidth() {
      return myWidth;
    }

    @Override
    public boolean isVisible() {
      return isVisible;
    }

    @Override
    public String getName() {
      return myName;
    }

    public void setName(String name) {
      this.myName = name;
    }

    @Override
    public void setVisible(boolean visible) {
      var wasVisible = isVisible;
      isVisible = visible;
      if (wasVisible != isVisible) {
        onChange.run();
      }
    }

    @Override
    public void setOrder(int order) {
      myOrder = order;
    }

    @Override
    public void setWidth(int width) {
      myWidth = width;
    }

    @Override
    public String toString() {
      return String.format("id=%s name=%s visible=%b width=%d", myID, myName, isVisible, myWidth);
    }

    public void setOnChange(Runnable listener) {
      onChange = listener;
    }
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ColumnStub that = (ColumnStub) o;

      if (myOrder != that.myOrder) return false;
      if (myWidth != that.myWidth) return false;
      if (isVisible != that.isVisible) return false;
      return Objects.equals(myID, that.myID);
    }

    @Override
    public int hashCode() {
      int result = myID != null ? myID.hashCode() : 0;
      result = 31 * result + myOrder;
      result = 31 * result + myWidth;
      result = 31 * result + (myName != null ? myName.hashCode() : 0);
      result = 31 * result + (isVisible ? 1 : 0);
      return result;
    }
  }

  class Immutable {
    public static ColumnList fromList(final List<Column> columns) {
      return new ColumnList() {
        @Override
        public int getSize() {
          return columns.size();
        }

        @Override
        public Column getField(int index) {
          return columns.get(index);
        }

        @Override
        public void clear() {
          throw new UnsupportedOperationException();
        }

        @Override
        public void add(String name, int order, int width) {
          throw new UnsupportedOperationException();
        }

        @Override
        public void importData(ColumnList source, boolean keepVisibleColumns) {
          throw new UnsupportedOperationException();
        }

        @Override
        public List<Column> exportData() {
          return columns;
        }
      };
    }
  }
}
