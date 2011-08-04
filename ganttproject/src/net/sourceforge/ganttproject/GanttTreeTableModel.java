/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

import net.sourceforge.ganttproject.delay.Delay;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.task.CustomColumn;
import net.sourceforge.ganttproject.task.CustomColumnsException;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskInfo;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskNode;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;

import org.jdesktop.swing.treetable.DefaultTreeTableModel;

/**
 * This class is the model for GanttTreeTable to display tasks.
 *
 * @author bbaranne (Benoit Baranne)
 */
public class GanttTreeTableModel extends DefaultTreeTableModel implements
        TableColumnModelListener, /*TaskContainmentHierarchyFacade,*/ GanttLanguage.Listener {

    private static GanttLanguage language = GanttLanguage.getInstance();

    public static String strColType = null;

    public static String strColPriority = null;

    public static String strColInfo = null;

    public static String strColName = null;

    public static String strColBegDate = null;

    public static String strColEndDate = null;

    public static String strColDuration = null;

    public static String strColCompletion = null;

    public static String strColCoordinator = null;

    public static String strColPredecessors = null;

    public static String strColID = null;

    /** The columns titles */
    public List<String> titles = null;

    /**
     * Custom columns list.
     */
    private Vector<String> customColumns = null;

    /**
     * Number of columns (presently in the model)
     */
    private int nbCol = 11;

    /**
     * Number of columns (at all, even hidden)
     */
    private int nbColTot = nbCol;

    private final CustomPropertyManager myCustomColumnsManager;

    /**
     * Creates an instance of GanttTreeTableModel with a root.
     *
     * @param root
     *            The root.
     * @param customColumnsManager
     */
    public GanttTreeTableModel(TreeNode root, CustomPropertyManager customColumnsManager) {
        super(root);
        titles = new ArrayList<String>();
        customColumns = new Vector<String>();
        changeLanguage(language);
        myCustomColumnsManager = customColumnsManager;
    }

    /**
     * Changes the language.
     *
     * @param ganttLanguage
     *            New language to use.
     */
    public void changeLanguage(GanttLanguage ganttLanguage) {
        strColType = language.getText("tableColType");
        strColPriority = language.getText("tableColPriority");
        strColInfo = language.getText("tableColInfo");
        strColName = language.getText("tableColName");
        strColBegDate = language.getText("tableColBegDate");
        strColEndDate = language.getText("tableColEndDate");
        strColDuration = language.getText("tableColDuration");
        strColCompletion = language.getText("tableColCompletion");
        strColCoordinator = language.getText("tableColCoordinator");
        strColPredecessors = language.getText("tableColPredecessors");
        strColID = language.getText("tableColID");

        titles.clear();
        String[] cols = new String[] { strColType, strColPriority, strColInfo,
                strColName, strColBegDate, strColEndDate, strColDuration,
                strColCompletion, strColCoordinator, strColPredecessors,
                strColID };
        for (int i = 0; i < cols.length; i++)
            titles.add(new String(cols[i]));
    }

    /**
     * Invoked this to insert newChild at location index in parents children.
     * This will then message nodesWereInserted to create the appropriate event.
     * This is the preferred way to add children as it will create the
     * appropriate event.
     */
    public void insertNodeInto(MutableTreeNode newChild,
            MutableTreeNode parent, int index) {
        parent.insert(newChild, index);

        int[] newIndexs = new int[1];

        newIndexs[0] = index;
        nodesWereInserted(parent, newIndexs);
    }

    /**
     * Message this to remove node from its parent. This will message
     * nodesWereRemoved to create the appropriate event. This is the preferred
     * way to remove a node as it handles the event creation for you.
     */
    public void removeNodeFromParent(MutableTreeNode node) {
        MutableTreeNode parent = (MutableTreeNode) node.getParent();

        if (parent == null) {
            throw new IllegalArgumentException("node does not have a parent.");
        }

        int[] childIndex = new int[1];
        Object[] removedArray = new Object[1];

        childIndex[0] = parent.getIndex(node);
        parent.remove(childIndex[0]);
        removedArray[0] = node;
        nodesWereRemoved(parent, childIndex, removedArray);
    }

    /**
     * Add a custom column to the model.
     *
     * @param title
     */
    public void addCustomColumn(String title) {
        customColumns.add(title);
        nbColTot++;
    }

    /**
     * Delete a custom column.
     *
     * @param title
     */
    public void deleteCustomColumn(String title) {
        customColumns.remove(title);
        this.columnRemoved(null);
        nbColTot--;
    }

    public void renameCustomColumn(String oldName, String newName) {
        customColumns.set(customColumns.indexOf(oldName), newName);
    }

    public int getColumnCountTotal() {
        return nbColTot;
    }

    public Class<?> getColumnClass(int column) {
        switch (column) {
        case 0:
        case 1:
        case 2:
            return Icon.class;
        case 3:
            return hierarchicalColumnClass;
        case 4:
        case 5:
            return GregorianCalendar.class;
        case 6:
        case 7:
            return Integer.class;
        case 8:
            return String.class;
        case 9:
            return String.class;
        case 10:
            return Integer.class;
        default:
            CustomColumn customColumn = (CustomColumn) myCustomColumnsManager.getCustomPropertyDefinition(getColumnName(column));
            return customColumn == null ? String.class : customColumn.getType();
        }
    }

    public String getColumnName(int column) {
        if (column < titles.size()) {
            return titles.get(column);
        }

        try {
            return customColumns.get(column - titles.size());
        } catch (IndexOutOfBoundsException e) {
            return customColumns.get(column - titles.size() - 1);
        }
    }

    public boolean isCellEditable(Object node, int column) {
        if (node instanceof TaskNode) {
            Task task = (Task) ((TaskNode) node).getUserObject();
            switch (column) {
            case 5:
            case 6:
                return !task.isMilestone();
            case 2:
            case 8:
            case 9:
            case 10:
                return false;
                default:
                    return true;
            }
        }
        return false;
    }

    public Object getValueAt(Object node, int column) {
        Object res = null;
        if (!(node instanceof TaskNode))
            return null;
        TaskNode tn = (TaskNode) node;
        Task t = (Task) tn.getUserObject();
        // if(tn.getParent()!=null){
        switch (column) {
        case 0:
            if (((Task) tn.getUserObject()).isProjectTask()) {
                res = new ImageIcon(getClass().getResource(
                        "/icons/mproject.gif"));
            } else if (!tn.isLeaf())
                res = new ImageIcon(getClass().getResource("/icons/mtask.gif"));
            else if (t.isMilestone())
                res = new ImageIcon(getClass()
                        .getResource("/icons/meeting.gif"));
            else
                res = new ImageIcon(getClass().getResource("/icons/tasks2.png"));
            break;
        case 1: // Priority
            GanttTask task = (GanttTask) tn.getUserObject();
            res = new ImageIcon(getClass().getResource(task.getPriority().getIconPath()));
            break;
        case 2: // info
            TaskInfo info = t.getTaskInfo();
            if (info != null) {
                if (info instanceof Delay) {
                    int type = ((Delay) info).getType();
                    if (type == Delay.NORMAL)
                        res = new ImageIcon(getClass().getResource(
                                "/icons/alert1_16.gif"));
                    else if (type == Delay.CRITICAL)
                        res = new ImageIcon(getClass().getResource(
                                "/icons/alert2_16.gif"));
                }
            }
            break;
        case 3:
            res = tn.getName();
            break;
        case 4:
            res = tn.getStart();
            break;
        case 5:
            res = tn.getEnd().newAdd(Calendar.DATE, -1);
            break;
        case 6:
            res = new Integer(tn.getDuration());
            break;
        case 7:
            res = new Integer(tn.getCompletionPercentage());
            break;
        case 8: {
            ResourceAssignment[] tAssign = t.getAssignments();
            StringBuffer sb = new StringBuffer();
            int nb = 0;
            for (int i = 0; i < tAssign.length; i++) {
                ResourceAssignment resAss = tAssign[i];
                if (resAss.isCoordinator()) {
                    sb.append(nb++ == 0 ? "" : ", ").append(
                            resAss.getResource().getName());
                }
            }
            res = sb.toString();
            break;
        }
        case 9: {
            String resStr = "";
            TaskDependency[] dep = t.getDependenciesAsDependant().toArray();
            int i = 0;
            if (dep != null && dep.length > 0) {
                for (i = 0; i < dep.length - 1; i++)
                    resStr += dep[i].getDependee().getTaskID() + ", ";
                resStr += dep[i].getDependee().getTaskID() + "";
            }
            res = resStr;
            break;
        }
        case 10:
            res = new Integer(t.getTaskID());
            break;
        default:
            String colName = this.getColumnName(column);
            // System.out.println(" -> "+colName);
            // System.out.println(t+" : "+t.getCustomValues());
            res = t.getCustomValues().getValue(colName);
            break;
        }
        // }
        // else
        // res ="";
        return res;
    }

    public void setValueAt(final Object value, final Object node,
            final int column) {
        if (value == null) {
            return;
        }
        if (isCellEditable(node, column)) {
//            System.out.println("undoable column: " + column);
            Mediator.getGanttProjectSingleton().getUndoManager().undoableEdit(
                    "Change properties column", new Runnable() {
                        public void run() {
                            setValue(value, node, column);
                        }
                    });
        } else {
//            System.out.println("NOT undoable column: " + column);
            setValue(value, node, column);
        }
        // System.out.println("node : " + node);
        // System.out.println("value : " + value);
        Mediator.getGanttProjectSingleton().repaint();
        Mediator.getGanttProjectSingleton().setAskForSave(true);
    }

    /**
     * Set value in left pane cell
     * 
     * @param value
     * @param node
     * @param column
     */
    private void setValue(final Object value, final Object node,
            final int column) {
        switch (column) {
        case 0:
        case 1:
        case 2: // info
            ((TaskNode) node).setTaskInfo((TaskInfo) value);
        case 8:
            break;
        case 3:
            ((TaskNode) node).setName(value.toString());
            break;
        case 4:
            ((TaskNode) node).setStart((GanttCalendar) value);
            ((TaskNode) node).applyThirdDateConstraint();
            break;
        case 5:
            ((TaskNode) node).setEnd(((GanttCalendar) value).newAdd(Calendar.DATE, 1));
            break;
        case 6:
            Task task = (Task) ((TaskNode) node).getUserObject();
            TaskLength tl = task.getDuration();
            ((TaskNode) node).setDuration(task.getManager().createLength(tl.getTimeUnit(),
                    ((Integer) value).intValue()));
            break;
        case 7:
            ((TaskNode) node).setCompletionPercentage(((Integer) value)
                    .intValue());
            break;
        default: // custom colums
            try {
                ((Task) ((TaskNode) node).getUserObject()).getCustomValues()
                        .setValue(this.getColumnName(column), value);
            } catch (CustomColumnsException e) {
                if (!GPLogger.log(e)) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    public void columnAdded(TableColumnModelEvent arg0) {
        nbCol++;
    }

    public void columnRemoved(TableColumnModelEvent arg0) {
        nbCol--;
    }

    public void columnMoved(TableColumnModelEvent arg0) {
        // TODO Auto-generated method stub
    }

    public void columnMarginChanged(ChangeEvent arg0) {
        // TODO Auto-generated method stub
    }

    public void columnSelectionChanged(ListSelectionEvent arg0) {
        // TODO Auto-generated method stub
    }

    /**
     * @return true if this Tasks has any nested subtasks.
     */
    public boolean hasNestedTasks(Task container) {
        TaskNode r = (TaskNode) root;
        return (r.getChildCount() > 0);
    }

    /**
     * @param task
     *            The task whose TaskNode we want to get.
     * @return The corresponding TaskNode according to the given task.
     */
    public TaskNode getTaskNodeForTask(Task task) {
        Enumeration enumeration = ((TaskNode) getRoot()).preorderEnumeration();
        while (enumeration.hasMoreElements()) {
            Object next = enumeration.nextElement();
            if (!(next instanceof TaskNode)) {
                continue;
            }
            TaskNode tn = (TaskNode) next;
            Task t = (Task) tn.getUserObject();
            if (t.equals(task)) {
                return tn;
            }
        }
        return null;
    }

    public void languageChanged(Event event) {
        changeLanguage(event.getLanguage());
    }
}