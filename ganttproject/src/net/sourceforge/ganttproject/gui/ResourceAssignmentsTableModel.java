package net.sourceforge.ganttproject.gui;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.ResourceAssignmentMutator;
import net.sourceforge.ganttproject.task.Task;

import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * @author Oleg Kushnikov
 */
public class ResourceAssignmentsTableModel extends AbstractTableModel {
  enum Column {
    ID("id", String.class),
    NAME("taskname", String.class),
    UNIT("unit", String.class);

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


  public ResourceAssignmentsTableModel(HumanResource person) {
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
      return true;
    }
    if (Column.UNIT.equals(Column.values()[col])) {
      return true;
    }
    return false;
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
    assert row >= 0 && row < getRowCount() && col >= 0 && col < getColumnCount();
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
        ra.setLoad(Float.parseFloat(String.valueOf(val)));
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

  public List<ResourceAssignment> getResourcesAssignments() {
    return Collections.unmodifiableList(myAssignments);
  }

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
