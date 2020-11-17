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
package net.sourceforge.ganttproject.gui.taskproperties;

import com.google.common.base.Objects;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependency.Hardness;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyCollectionMutator;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.dependency.constraint.ConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public class DependencyTableModel extends AbstractTableModel {
  private static final boolean EDITABLE = true;
  private static final boolean NOT_EDITABLE = false;

  public static enum MyColumn {
    ID(GanttLanguage.getInstance().getText("id"), DependencyTableModel.NOT_EDITABLE), TASK_NAME(
        GanttLanguage.getInstance().getText("taskname"), DependencyTableModel.EDITABLE), CONSTRAINT_TYPE(
        GanttLanguage.getInstance().getText("type"), DependencyTableModel.EDITABLE), LAG(
        GanttLanguage.getInstance().getText("delay"), DependencyTableModel.EDITABLE), HARDNESS(
        GanttLanguage.getInstance().getText("hardness"), DependencyTableModel.EDITABLE);

    private final String myCaption;
    private final boolean isEditable;

    MyColumn(String caption, boolean isEditable) {
      myCaption = caption;
      this.isEditable = isEditable;
    }

    public String getCaption() {
      return myCaption;
    }

    public boolean isEditable() {
      return isEditable;
    }

    TableColumn getTableColumn(JTable table) {
      return table.getColumnModel().getColumn(this.ordinal());
    }
  }

  private final List<TaskDependency> myDependencies;

  private final TaskDependencyCollectionMutator myMutator;

  private final Task myTask;

  public DependencyTableModel(Task task) {
    myDependencies = new ArrayList<TaskDependency>(Arrays.asList(task.getDependenciesAsDependant().toArray()));
    myMutator = task.getManager().getDependencyCollection().createMutator();
    myTask = task;
  }

  List<TaskDependency> getDependencies() {
    return Collections.unmodifiableList(myDependencies);
  }

  public void commit() {
    myMutator.commit();
  }

  @Override
  public int getColumnCount() {
    return MyColumn.values().length;
  }

  @Override
  public int getRowCount() {
    return myDependencies.size() + 1;
  }

  @Override
  public String getColumnName(int col) {
    return MyColumn.values()[col].getCaption();
  }

  @Override
  public Object getValueAt(int row, int col) {
    assert row >= 0 && row < getRowCount() && col >= 0 && col < getColumnCount();
    if (row == myDependencies.size()) {
      return "";
    }

    TaskDependency dep = myDependencies.get(row);
    MyColumn column = MyColumn.values()[col];
    switch (column) {
    case ID: {
      return dep.getDependee().getTaskID();
    }
    case TASK_NAME: {
      return new TaskComboItem(dep.getDependee());
    }
    case CONSTRAINT_TYPE: {
      return dep.getConstraint().getName();
    }
    case LAG: {
      return dep.getDifference();
    }
    case HARDNESS: {
      return dep.getHardness();
    }
    default:
      throw new IllegalArgumentException("Illegal row number=" + row);
    }
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    MyColumn column = MyColumn.values()[col];
    return row == getRowCount() ? column == MyColumn.TASK_NAME : column.isEditable();
  }

  @Override
  public void setValueAt(Object value, int row, int col) {
    assert row >= 0;
    if (Objects.equal(value, getValueAt(row, col))) {
      return;
    }
    try {
      if (row == myDependencies.size()) {
        createDependency(value);
      } else {
        updateDependency(value, row, col);
      }
    } catch (TaskDependencyException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
    }
    fireTableCellUpdated(row, col);
  }

  private void updateDependency(Object value, int row, int col) throws TaskDependencyException {
    TaskDependency dep = myDependencies.get(row);
    switch (col) {
    case 4:
      dep.setHardness((Hardness) value);
      break;
    case 3: {
      int loadAsInt = Integer.parseInt(String.valueOf(value));
      dep.setDifference(loadAsInt);
      break;
    }
    case 2: {
      TaskDependencyConstraint clone;
      try {
        clone = (TaskDependencyConstraint) ((ConstraintImpl) value).clone();
      } catch (CloneNotSupportedException e) {
        throw new RuntimeException(e);
      }
      dep.setConstraint(clone);
      break;
    }
    case 1: {
      myMutator.deleteDependency(dep);
      dep.delete();
      myDependencies.remove(row);
      if (value == null) {
        fireTableRowsDeleted(row, row);
      } else {
        Task selectedTask = ((TaskComboItem) value).myTask;
        TaskDependency newDependency = myMutator.createDependency(myTask, selectedTask, new FinishStartConstraintImpl());
        myDependencies.add(newDependency);
      }
    }
    }
  }

  public void delete(int[] selectedRows) {
    List<TaskDependency> selected = new ArrayList<TaskDependency>();
    for (int row : selectedRows) {
      selected.add(myDependencies.get(row));
    }
    for (TaskDependency d : selected) {
      d.delete();
    }
    myDependencies.removeAll(selected);
    fireTableDataChanged();
  }

  private void createDependency(Object value) throws TaskDependencyException {
    if (value instanceof TaskComboItem) {
      Task selectedTask = ((TaskComboItem) value).myTask;
      TaskDependency dep = myMutator.createDependency(myTask, selectedTask, new FinishStartConstraintImpl());
      dep.setHardness(TaskDependency.Hardness.parse(myTask.getManager().getDependencyHardnessOption().getValue()));
      myDependencies.add(dep);
      fireTableRowsInserted(myDependencies.size(), myDependencies.size());
    }
  }

  static class TaskComboItem {
    final String myText;

    final Task myTask;

    TaskComboItem(Task task) {
      myTask = task;
      myText = "[#" + task.getTaskID() + "] " + task.getName();
    }

    @Override
    public String toString() {
      return myTask.getName();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof TaskComboItem == false) {
        return false;
      }
      TaskComboItem value = (TaskComboItem) obj;
      return myTask.getTaskID() == value.myTask.getTaskID();
    }

    @Override
    public int hashCode() {
      return myTask.getTaskID();
    }

  }
}
