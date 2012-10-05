/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 Dmitry Barashev

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

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;
import net.sourceforge.ganttproject.task.CustomColumnsException;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskNode;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;

import biz.ganttproject.core.option.ValidationException;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * This class is the model for GanttTreeTable to display tasks.
 *
 * @author bbaranne (Benoit Baranne)
 */
public class GanttTreeTableModel extends DefaultTreeTableModel implements TableColumnModelListener, GanttLanguage.Listener {

  private static GanttLanguage language = GanttLanguage.getInstance();

  private final CustomPropertyManager myCustomColumnsManager;

  private final UIFacade myUiFacade;

  private static final int STANDARD_COLUMN_COUNT = TaskDefaultColumn.values().length;
  /**
   * Creates an instance of GanttTreeTableModel with a root.
   *
   * @param root
   *          The root.
   * @param customColumnsManager
   */
  public GanttTreeTableModel(TaskManager taskManager, CustomPropertyManager customColumnsManager, UIFacade uiFacade) {
    super(new TaskNode(taskManager.getRootTask()));
    myUiFacade = uiFacade;
    GanttLanguage.getInstance().addListener(this);
    changeLanguage(language);
    myCustomColumnsManager = customColumnsManager;
  }

  @Override
  public int getColumnCount() {
    return STANDARD_COLUMN_COUNT + myCustomColumnsManager.getDefinitions().size();
  }

  /**
   * Changes the language.
   *
   * @param ganttLanguage
   *          New language to use.
   */
  public void changeLanguage(GanttLanguage ganttLanguage) {
  }

  /**
   * Invoked this to insert newChild at location index in parents children. This
   * will then message nodesWereInserted to create the appropriate event. This
   * is the preferred way to add children as it will create the appropriate
   * event.
   */
  // @Override
  // public void insertNodeInto(MutableTreeTableNode newChild,
  // MutableTreeTableNode parent, int index) {
  // parent.insert(newChild, index);
  //
  // int[] newIndexs = new int[1];
  //
  // newIndexs[0] = index;
  // modelSupport.fireChildAdded(TreeUtil.createPath(parent), index, child);
  // }

  /**
   * Message this to remove node from its parent. This will message
   * nodesWereRemoved to create the appropriate event. This is the preferred way
   * to remove a node as it handles the event creation for you.
   */
  // @Override
  // public void removeNodeFromParent(MutableTreeTableNode node) {
  // MutableTreeTableNode parent = (MutableTreeTableNode) node.getParent();
  //
  // if (parent == null)
  // throw new IllegalArgumentException("node does not have a parent.");
  //
  // int[] childIndex = new int[1];
  // Object[] removedArray = new Object[1];
  //
  // childIndex[0] = parent.getIndex(node);
  // parent.remove(childIndex[0]);
  // removedArray[0] = node;
  // nodesWereRemoved(parent, childIndex, removedArray);
  // }

  @Override
  public String getColumnName(int column) {
    if (column >=0 && column < STANDARD_COLUMN_COUNT) {
      return GanttLanguage.getInstance().getText(TaskDefaultColumn.values()[column].getNameKey());
    }
    CustomPropertyDefinition customColumn = getCustomProperty(column);
    return customColumn.getName();
  }

  @Override
  public int getHierarchicalColumn() {
    return TaskDefaultColumn.NAME.ordinal();
  }

  @Override
  public Class<?> getColumnClass(int column) {
    if (column < 0) {
      return null;
    }
    if (column >= 0 && column < STANDARD_COLUMN_COUNT) {
      return TaskDefaultColumn.values()[column].getValueClass();
    }
    CustomPropertyDefinition customColumn = getCustomProperty(column);
    Class<?> result = customColumn == null ? String.class : customColumn.getType();
    return result;
  }

  private CustomPropertyDefinition getCustomProperty(int columnIndex) {
    assert columnIndex >= STANDARD_COLUMN_COUNT : "We have " + STANDARD_COLUMN_COUNT + " default properties, and custom property index starts at " + STANDARD_COLUMN_COUNT + ". I've got index #"
        + columnIndex + ". Something must be wrong here";
    List<CustomPropertyDefinition> definitions = myCustomColumnsManager.getDefinitions();
    columnIndex -= STANDARD_COLUMN_COUNT;
    return columnIndex < definitions.size() ? definitions.get(columnIndex) : null;
  }

  @Override
  public boolean isCellEditable(Object node, int column) {
    if (node instanceof TaskNode) {
      Task task = (Task) ((TaskNode) node).getUserObject();
      if (column >=0 && column < STANDARD_COLUMN_COUNT) {
        return TaskDefaultColumn.values()[column].isEditable(task);
      }
      return true;
    }
    return false;
  }

  @Override
  public Object getValueAt(Object node, int column) {
    if (column < 0) {
      return "";
    }
    if (!(node instanceof TaskNode)) {
      return null;
    }
    Object res = null;
    TaskNode tn = (TaskNode) node;
    Task t = (Task) tn.getUserObject();
    if (column < STANDARD_COLUMN_COUNT) {
      TaskDefaultColumn defaultColumn = TaskDefaultColumn.values()[column];
      switch (defaultColumn) {
      case TYPE:
        if (((Task) tn.getUserObject()).isProjectTask()) {
          res = new ImageIcon(getClass().getResource("/icons/mproject.gif"));
        } else if (!tn.isLeaf())
          res = new ImageIcon(getClass().getResource("/icons/mtask.gif"));
        else if (t.isMilestone()) {
          res = new ImageIcon(getClass().getResource("/icons/meeting.gif"));
        } else {
          res = new ImageIcon(getClass().getResource("/icons/tasks2.png"));
        }
        break;
      case PRIORITY:
        GanttTask task = (GanttTask) tn.getUserObject();
        res = new ImageIcon(getClass().getResource(task.getPriority().getIconPath()));
        break;
      case INFO:
        // TODO(dbarashev): implement alerts some other way
        /**
              res = new ImageIcon(getClass().getResource("/icons/alert1_16.gif"));
              res = new ImageIcon(getClass().getResource("/icons/alert2_16.gif"));
              */
        break;
      case NAME:
        res = tn.getName();
        break;
      case BEGIN_DATE:
        res = tn.getStart();
        break;
      case END_DATE:
        res = t.isMilestone() ? tn.getEnd() : tn.getEnd().getDisplayValue();
        break;
      case DURATION:
        res = new Integer(tn.getDuration());
        break;
      case COMPLETION:
        res = new Integer(tn.getCompletionPercentage());
        break;
      case COORDINATOR:
        ResourceAssignment[] tAssign = t.getAssignments();
        StringBuffer sb = new StringBuffer();
        int nb = 0;
        for (int i = 0; i < tAssign.length; i++) {
          ResourceAssignment resAss = tAssign[i];
          if (resAss.isCoordinator()) {
            sb.append(nb++ == 0 ? "" : ", ").append(resAss.getResource().getName());
          }
        }
        res = sb.toString();
        break;
      case PREDECESSORS:
        List<Integer> depids = Lists.newArrayList(Lists.transform(Arrays.asList(t.getDependenciesAsDependant().toArray()), new Function<TaskDependency, Integer>() {
          @Override
          public Integer apply(TaskDependency value) {
            return value.getDependee().getTaskID();
          }
        }));
        Collections.sort(depids);
        res = Joiner.on(',').join(depids);
        break;
      case ID:
        res = new Integer(t.getTaskID());
        break;
      case OUTLINE_NUMBER:
        List<Integer> outlinePath = t.getManager().getTaskHierarchy().getOutlinePath(t);
        res = Joiner.on('.').join(outlinePath);
        break;
      default:
        break;
      }

    } else {
      CustomPropertyDefinition customColumn = getCustomProperty(column);
      res = t.getCustomValues().getValue(customColumn);

    }
    // if(tn.getParent()!=null){
    return res;
  }

  @Override
  public void setValueAt(final Object value, final Object node, final int column) {
    if (value == null) {
      return;
    }
    if (isCellEditable(node, column) && !Objects.equal(value, getValueAt(node, column))) {
      // System.out.println("undoable column: " + column);
      myUiFacade.getUndoManager().undoableEdit("Change properties column", new Runnable() {
        @Override
        public void run() {
          setValue(value, node, column);
        }
      });
    } else {
      // System.out.println("NOT undoable column: " + column);
      setValue(value, node, column);
    }
    myUiFacade.getActiveChart().reset();
  }

  /**
   * Set value in left pane cell
   *
   * @param value
   * @param node
   * @param column
   */
  private void setValue(final Object value, final Object node, final int column) {
    if (column >= STANDARD_COLUMN_COUNT) {
      setCustomPropertyValue(value, node, column);
      return;
    }
    assert node instanceof TaskNode : "Tree node=" + node + " is not a task node";

    Task task = (Task) ((TaskNode)node).getUserObject();
    TaskDefaultColumn property = TaskDefaultColumn.values()[column];
    switch (property) {
    case NAME:
      ((TaskNode) node).setName(value.toString());
      break;
    case BEGIN_DATE:
      ((TaskNode) node).setStart((GanttCalendar) value);
      ((TaskNode) node).applyThirdDateConstraint();
      break;
    case END_DATE:
      ((TaskNode) node).setEnd(CalendarFactory.createGanttCalendar(
          GPTimeUnitStack.DAY.adjustRight(((GanttCalendar)value).getTime())));
      break;
    case DURATION:
      TimeDuration tl = task.getDuration();
      ((TaskNode) node).setDuration(task.getManager().createLength(tl.getTimeUnit(), ((Integer) value).intValue()));
      break;
    case COMPLETION:
      ((TaskNode) node).setCompletionPercentage(((Integer) value).intValue());
      break;
    case PREDECESSORS:
      List<Integer> newIds = Lists.transform(Arrays.asList(String.valueOf(value).split(",")), new Function<String, Integer>() {
          @Override
          public Integer apply(String value) {
            try {
              return Integer.parseInt(value);
            } catch (NumberFormatException e) {
              throw new ValidationException(MessageFormat.format("{0} is not a number", value));
            }
          }
        });
      List<Integer> oldIds = Lists.transform(Arrays.asList(task.getDependenciesAsDependant().toArray()), new Function<TaskDependency, Integer>() {
        @Override
        public Integer apply(TaskDependency value) {
          return value.getDependee().getTaskID();
        }
      });
      Set<Integer> removedIds = Sets.difference(Sets.newHashSet(oldIds), Sets.newHashSet(newIds));
      Set<Integer> addedIds = Sets.difference(Sets.newHashSet(newIds), Sets.newHashSet(oldIds));

      for (Integer id : removedIds) {
        Task dependee = task.getManager().getTask(id);
        TaskDependency dep = task.getDependenciesAsDependant().getDependency(dependee);
        if (dep != null) {
          dep.delete();
        }
      }
      for (Integer id : addedIds) {
        Task predecessorCandidate = null;
        try {
          predecessorCandidate = task.getManager().getTask(id);
          if (predecessorCandidate == null) {
            continue;
          }
          if (task.getManager().getDependencyCollection().canCreateDependency(task, predecessorCandidate)) {
            task.getManager().getDependencyCollection().createDependency(task, predecessorCandidate);
          } else {
            throw new ValidationException(MessageFormat.format("Can't create dependency between task {0} and {1}", task.getName(), predecessorCandidate.getName()));
          }
        } catch (TaskDependencyException e) {
          throw new ValidationException(MessageFormat.format("Can't create dependency between task {0} and {1}", task.getName(), predecessorCandidate.getName()));
        }
      }
      break;
    default:
      break;
    }

  }

  private void setCustomPropertyValue(Object value, Object node, int column) {
    try {
      ((Task) ((TaskNode) node).getUserObject()).getCustomValues().setValue(getCustomProperty(column), value);
    } catch (CustomColumnsException e) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err);
      }
    }
  }

  @Override
  public void columnAdded(TableColumnModelEvent arg0) {
  }

  @Override
  public void columnRemoved(TableColumnModelEvent arg0) {
  }

  @Override
  public void columnMoved(TableColumnModelEvent arg0) {
    // TODO Auto-generated method stub
  }

  @Override
  public void columnMarginChanged(ChangeEvent arg0) {
    // TODO Auto-generated method stub
  }

  @Override
  public void columnSelectionChanged(ListSelectionEvent arg0) {
    // TODO Auto-generated method stub
  }

  // public Task[] getNestedTasks(Task container) {
  // return null;
  // }
  //
  // public Task[] getDeepNestedTasks(Task container) {
  // // TODO Auto-generated method stub
  // return null;
  // }
  //
  // /**
  // * @return true if this Tasks has any nested subtasks.
  // */
  // public boolean hasNestedTasks(Task container) {
  // TaskNode r = (TaskNode) root;
  // if (r.getChildCount() > 0) {
  // return true;
  // } else {
  // return false;
  // }
  // }
  //
  // public Task getRootTask() {
  // return (Task) ((TaskNode) this.getRoot()).getUserObject();
  // }
  //
  // /**
  // * @return the corresponding task node according to the given task.
  // *
  // * @param task
  // * The task whose TaskNode we want to get.
  // * @return The corresponding TaskNode according to the given task.
  // */
  // public TaskNode getTaskNodeForTask(Task task) {
  // for (MutableTreeTableNode tn : TreeUtil.collectSubtree(getRootNode())) {
  // Task t = (Task) tn.getUserObject();
  // if (t.equals(task)) {
  // return tn;
  // }
  // }
  // return null;
  // }
  //
  // public Task getContainer(Task nestedTask) {
  // // TODO Auto-generated method stub
  // return null;
  // }
  //
  // public void move(Task whatMove, Task whereMove) {
  // // TODO Auto-generated method stub
  // }
  //
  // public boolean areUnrelated(Task dependant, Task dependee) {
  // // TODO Auto-generated method stub
  // return false;
  // }
  //
  // public int getDepth(Task task) {
  // // TODO Auto-generated method stub
  // return 0;
  // }

  @Override
  public void languageChanged(Event event) {
    changeLanguage(event.getLanguage());
  }

  public int compareDocumentOrder(Task next, Task dependeeTask) {
    throw new UnsupportedOperationException();
  }

  public boolean contains(Task task) {
    throw new UnsupportedOperationException();
  }

  public DefaultMutableTreeTableNode getRootNode() {
    return (DefaultMutableTreeTableNode) getRoot();
  }
}