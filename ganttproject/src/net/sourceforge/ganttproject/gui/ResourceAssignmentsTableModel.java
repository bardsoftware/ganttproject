/*
Copyright 2017 Oleg Kushnikov, BarD Software s.r.o

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
package net.sourceforge.ganttproject.gui;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.ResourceAssignmentMutator;
import net.sourceforge.ganttproject.task.Task;

import java.util.*;

/**
 * @author Oleg Kushnikov
 */
public class ResourceAssignmentsTableModel extends TableModelExt<ResourceAssignment> {
  enum Column {
    ID("id", String.class),
    NAME("taskname", String.class),
    UNIT("unit", Float.class);

    private final String myCaption;
    private final Class<?> myClass;

    Column(String key, Class clazz) {
      myCaption = GanttLanguage.getInstance().getText(key);
      myClass = clazz;
    }

    String getCaption() {
      return myCaption;
    }

    public Class<?> getColumnClass() {
      return myClass;
    }
  }

  private final List<ResourceAssignment> myAssignments;
  private final List<ResourceAssignment> myAssignmentsToDelete = new ArrayList<>();
  private final HumanResource myResource;
  private final Map<Task, ResourceAssignmentMutator> myTask2MutatorMap = new HashMap<>();


  ResourceAssignmentsTableModel(HumanResource person) {
    myResource = person;
    myAssignments = new ArrayList<>(Arrays.asList(person.getAssignments()));
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return Column.values()[columnIndex].getColumnClass();
  }

  @Override
  public int getRowCount() {
    return myAssignments.size() + 1;
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    if (row == myAssignments.size()) {
      return Column.NAME.equals(Column.values()[col]);
    } else {
      return Column.UNIT.equals(Column.values()[col]);
    }
  }

  @Override
  public String getColumnName(int col) {

    return Column.values()[col].getCaption();
  }

  @Override
  public int getColumnCount() {

    return Column.values().length;
  }

  @Override
  public Object getValueAt(int row, int col) {
    assert row >= 0 && row < getRowCount() && col >= 0 && col < getColumnCount() :
        String.format("Row/column index is out of bounds: (%d,%d) [%d,%d]", row, col, getRowCount(), getColumnCount());
    if (row == myAssignments.size()) {
      return null;
    }
    ResourceAssignment ra = myAssignments.get(row);
    Column column = Column.values()[col];
    switch (column) {
      case ID: {
        return ra.getTask().getTaskID();
      }
      case NAME: {
        return ra.getTask();
      }
      case UNIT: {
        return ra.getLoad();
      }
      default:
        throw new IllegalArgumentException("Illegal row number=" + row);
    }
  }

  @Override
  public void setValueAt(Object val, int row, int col) {
    if (val == null) {
      return;
    }
    if (row >= 0) {
      if (row >= myAssignments.size()) {
        createAssignment(val);
      } else {
        updateAssignment(val, row, col);
      }
    } else {
      throw new IllegalArgumentException("I can't set data in row=" + row);
    }
  }

  private void updateAssignment(Object val, int row, int col) {
    Column column = Column.values()[col];
    ResourceAssignment ra = myAssignments.get(row);
    switch (column) {
      case UNIT:
        ra.setLoad((Float)val);
        break;
    }
    fireTableCellUpdated(row, col);
  }

  private void createAssignment(Object value) {
    Task task = ((Task) value);
    ResourceAssignmentMutator mutator = getMutator(task);
    ResourceAssignment ra = mutator.addAssignment(myResource);
    ra.setLoad(100);
    myAssignments.add(ra);
    fireTableRowsInserted(myAssignments.size(), myAssignments.size());
  }

  private ResourceAssignmentMutator getMutator(Task task) {
    ResourceAssignmentMutator mutator = myTask2MutatorMap.get(task);
    if (mutator == null) {
      mutator = task.getAssignmentCollection().createMutator();
      myTask2MutatorMap.put(task, mutator);
    }
    return mutator;
  }

  List<ResourceAssignment> getResourcesAssignments() {
    return Collections.unmodifiableList(myAssignments);
  }

  @Override
  public void delete(int[] selectedRows) {
    List<ResourceAssignment> selected = new ArrayList<>();
    for (int row : selectedRows) {
      if (row < myAssignments.size()) {
        ResourceAssignment ra = myAssignments.get(row);
        ResourceAssignmentMutator mutator = getMutator(ra.getTask());
        mutator.deleteAssignment(myResource);
        myAssignmentsToDelete.add(ra);
        selected.add(ra);
      }
    }
    myAssignments.removeAll(selected);
    fireTableDataChanged();
  }

  @Override
  public List<ResourceAssignment> getAllValues() {
    return myAssignments;
  }

  public void commit() {
    for (ResourceAssignmentMutator m : myTask2MutatorMap.values()) {
      m.commit();
    }
    for (ResourceAssignment ra : myAssignmentsToDelete) {
      if (!myAssignments.contains(ra)) {
        ra.delete();
      }
    }
  }
}
