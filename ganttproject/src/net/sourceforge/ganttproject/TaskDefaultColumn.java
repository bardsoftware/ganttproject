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
package net.sourceforge.ganttproject;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import javax.swing.Icon;

import com.google.common.base.Function;

import net.sourceforge.ganttproject.gui.TableHeaderUIFacade;
import net.sourceforge.ganttproject.gui.TableHeaderUIFacade.Column;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;

/**
 * Enumeration of built-in task properties.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public enum TaskDefaultColumn {
  TYPE(new TableHeaderUIFacade.ColumnStub("tpd0", null, false, -1, -1), Icon.class, "tableColType"),
  PRIORITY(new TableHeaderUIFacade.ColumnStub("tpd1", null, false, -1, 50), Icon.class, "tableColPriority"),
  INFO(new TableHeaderUIFacade.ColumnStub("tpd2", null, false, -1, -1), Icon.class, "tableColInfo", Functions.NOT_EDITABLE),
  NAME(new TableHeaderUIFacade.ColumnStub("tpd3", null, true, 0, 200), String.class, "tableColName"),
  BEGIN_DATE(new TableHeaderUIFacade.ColumnStub("tpd4", null, true, 1, 75), GregorianCalendar.class, "tableColBegDate"),
  END_DATE(new TableHeaderUIFacade.ColumnStub("tpd5", null, true, 2, 75), GregorianCalendar.class, "tableColEndDate", Functions.NOT_MILESTONE),
  DURATION(new TableHeaderUIFacade.ColumnStub("tpd6", null, false, -1, 50), Integer.class, "tableColDuration", Functions.NOT_MILESTONE),
  COMPLETION(new TableHeaderUIFacade.ColumnStub("tpd7", null, false, -1, 50), Integer.class, "tableColCompletion"),
  COORDINATOR(new TableHeaderUIFacade.ColumnStub("tpd8", null, false, -1, 200), String.class, "tableColCoordinator", Functions.NOT_EDITABLE),
  PREDECESSORS(new TableHeaderUIFacade.ColumnStub("tpd9", null, false, -1, 200), String.class, "tableColPredecessors", Functions.NOT_EDITABLE),
  ID(new TableHeaderUIFacade.ColumnStub("tpd10", null, false, -1, 20), Integer.class, "tableColID", Functions.NOT_EDITABLE)/*,
  OUTLINE_NUMBER(new TableHeaderUIFacade.ColumnStub("tpd11", null, false, 4, 20), String.class, "tableColOutline", Functions.NOT_EDITABLE)*/;

  private final Column myDelegate;
  private final Class<?> myValueClass;
  private final Function<Task, Boolean> myIsEditableFunction;
  private final String myNameKey;

  private TaskDefaultColumn(TableHeaderUIFacade.Column delegate, Class<?> valueClass, String nameKey) {
    this(delegate, valueClass, nameKey, Functions.ALWAYS_EDITABLE);
  }

  private TaskDefaultColumn(TableHeaderUIFacade.Column delegate, Class<?> valueClass, String nameKey, Function<Task, Boolean> isEditable) {
    myDelegate = delegate;
    myValueClass = valueClass;
    myIsEditableFunction = isEditable;
    myNameKey = nameKey;
  }

  public Column getStub() {
    return myDelegate;
  }

  static List<Column> getColumnStubs() {
    List<Column> result = new ArrayList<Column>();
    for (TaskDefaultColumn dc : values()) {
      result.add(dc.myDelegate);
    }
    return result;
  }

  public Class<?> getValueClass() {
    return myValueClass;
  }

  public boolean isEditable(Task task) {
    return myIsEditableFunction.apply(task);
  }

  public String getNameKey() {
    return myNameKey;
  }

  public String getName() {
    return GanttLanguage.getInstance().getText(getNameKey());
  }

  static class Functions {
    static Function<Task, Boolean> NOT_EDITABLE = new Function<Task, Boolean>() {
      @Override
      public Boolean apply(Task input) {
        return Boolean.FALSE;
      }
    };

    static Function<Task, Boolean> ALWAYS_EDITABLE = new Function<Task, Boolean>() {
      @Override
      public Boolean apply(Task input) {
        return Boolean.TRUE;
      }
    };

    static Function<Task, Boolean> NOT_MILESTONE = new Function<Task, Boolean>() {
      @Override
      public Boolean apply(Task input) {
        return !input.isMilestone();
      }
    };
  }
}