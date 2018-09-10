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

public interface ColumnList {
  int getSize();

  Column getField(int index);

  void clear();

  void add(String name, int order, int width);

  void importData(ColumnList source);

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
    private final String myName;
    private boolean isVisible;
    private SortOrder mySortOrder = SortOrder.UNSORTED;

    public ColumnStub(String id, String name, boolean visible, int order, int width) {
      myName = name;
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

    @Override
    public void setVisible(boolean visible) {
      isVisible = visible;
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
      return myID;
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
        public void importData(ColumnList source) {
          throw new UnsupportedOperationException();
        }
      };
    }
  }
}
