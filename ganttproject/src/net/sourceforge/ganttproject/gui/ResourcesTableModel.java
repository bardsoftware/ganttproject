package net.sourceforge.ganttproject.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ProjectResource;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.ResourceAssignmentCollection;
import net.sourceforge.ganttproject.task.ResourceAssignmentMutator;

public class ResourcesTableModel extends AbstractTableModel {

    final String[] columnNames = { GanttLanguage.getInstance().getText("id"),
            GanttLanguage.getInstance().getText("resourcename"),
            GanttLanguage.getInstance().getText("unit"),
            GanttLanguage.getInstance().getText("coordinator"),
            GanttLanguage.getInstance().getText("role") };

    private final ResourceAssignmentCollection myAssignmentCollection;

    private final List<ResourceAssignment> myAssignments;

    private static final int MAX_ROW_COUNT = 100;

    private final ResourceAssignmentMutator myMutator;

    private boolean isChanged = false;

    public ResourcesTableModel(ResourceAssignmentCollection assignmentCollection) {
        myAssignmentCollection = assignmentCollection;
        myAssignments = new ArrayList<ResourceAssignment>(Arrays.asList(assignmentCollection
                .getAssignments()));
        myMutator = assignmentCollection.createMutator();
    }

    /**
     * Return the number of colums
     */
    public int getColumnCount() {

        return columnNames.length;

    }

    /**
     * Return the number of rows
     */
    public int getRowCount() {
        return myAssignments.size() + 1;
    }

    /**
     * Return the name of the column at col index
     */
    public String getColumnName(int col) {

        return columnNames[col];

    }

    /**
     * Return the object a specify cell
     */
    public Object getValueAt(int row, int col) {
        Object result;
        if (row >= 0) {
            if (row < myAssignments.size()) {
                ResourceAssignment assignment = myAssignments
                        .get(row);
                switch (col) {
                case 0:
                    result = String.valueOf(assignment.getResource().getId());
                    break;
                case 1:
                    result = assignment.getResource();
                    break;
                case 2:
                    result = String.valueOf(assignment.getLoad());
                    break;
                case 3:
                    result = new Boolean(assignment.isCoordinator());
                    break;
                case 4:
                    result = assignment.getRoleForAssignment();
                    break;
                default:
                    result = "";
                }
            } else {
                result = "";
            }
        } else {
            throw new IllegalArgumentException("I can't return data in row="
                    + row);
        }
        return result;
    }

    /*
     * JTable uses this method to determine the default renderer/ editor for
     * each cell. If we didn't implement this method, then the last column would
     * contain text ("true"/"false"), rather than a check box.
     */

    public Class getColumnClass(int c) {
        // if (c == 0 || c == 2) {
        // return String.class;
        // } else {
        // return HumanResource.class;
        // }
        switch (c) {
        case 0:
        case 2:
            return String.class;
        case 1:
            return HumanResource.class;
        case 3:
            return Boolean.class;
        case 4:
            return Role.class;
        default:
            return String.class;
        }
    }

    public boolean isCellEditable(int row, int col) {
        boolean result = col > 0;
        if (result) {
            result = (col == 2 ? row < myAssignments.size()
                    : row <= myAssignments.size())
                    || col == 3 || col == 4;
        }
        return result;
    }

    /*
     * Don't need to implement this method unless your table's data can change.
     */

    public void setValueAt(Object value, int row, int col) {
        if (row >= 0) {
            if (row >= myAssignments.size()) {
                createAssignment(value);
            } else {
                updateAssignment(value, row, col);
            }
        } else {
            throw new IllegalArgumentException("I can't set data in row=" + row);
        }
        isChanged = true;
        // fireTableCellUpdated(row, col);

    }

    private void updateAssignment(Object value, int row, int col) {
        ResourceAssignment updateTarget = myAssignments
                .get(row);
        switch (col) {
        case 4: {
            updateTarget.setRoleForAssignment((Role) value);
            break;
        }
        case 3: {
            updateTarget.setCoordinator(((Boolean) value).booleanValue());
            break;
        }
        case 2: {
            float loadAsFloat = Float.parseFloat(String.valueOf(value));
            updateTarget.setLoad(loadAsFloat);
            break;
        }
        case 1: {
            if (value == null) {
                updateTarget.delete();
                myAssignments.remove(row);
                fireTableRowsDeleted(row, row);
            } else if (value instanceof ProjectResource) {
                float load = updateTarget.getLoad();
                boolean coord = updateTarget.isCoordinator();
                updateTarget.delete();
                ResourceAssignment newAssignment = myMutator
                        .addAssignment((ProjectResource) value);
                newAssignment.setLoad(load);
                newAssignment.setCoordinator(coord);
                myAssignments.set(row, newAssignment);
            }
            break;

        }
        default:
            break;
        }
    }

    private void createAssignment(Object value) {
        if (value instanceof ProjectResource) {
            ResourceAssignment newAssignment = myMutator
                    .addAssignment((ProjectResource) value);
            newAssignment.setLoad(100);

            boolean coord = false;
            if (myAssignments.isEmpty())
                coord = true;
            newAssignment.setCoordinator(coord);

            if (newAssignment.getResource() instanceof HumanResource)
                newAssignment
                        .setRoleForAssignment(((HumanResource) newAssignment
                                .getResource()).getRole());

            myAssignments.add(newAssignment);
            fireTableRowsInserted(myAssignments.size(), myAssignments.size());
        }
    }

    public List<ResourceAssignment> getResourcesAssignments() {
        return myAssignments;
    }

    void commit() {
        myMutator.commit();
    }

    public boolean isChanged() {
        return isChanged;
    }

}
