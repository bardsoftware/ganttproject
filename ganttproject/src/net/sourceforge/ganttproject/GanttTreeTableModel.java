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

import biz.ganttproject.core.model.task.TaskDefaultColumn;
import biz.ganttproject.core.option.DefaultBooleanOption;
import biz.ganttproject.core.option.ValidationException;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;
import biz.ganttproject.core.time.TimeDuration;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.CustomColumnsException;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskNode;
import net.sourceforge.ganttproject.task.TaskProperties;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;

/**
 * This class is the model for GanttTreeTable to display tasks.
 *
 * @author bbaranne (Benoit Baranne)
 */
public class GanttTreeTableModel extends DefaultTreeTableModel implements TableColumnModelListener {
  private static class Icons {
    static ImageIcon ALERT_TASK_INPROGRESS = new ImageIcon(GanttTreeTableModel.class.getResource("/icons/alert1_16.gif"));
    static ImageIcon ALERT_TASK_OUTDATED = new ImageIcon(GanttTreeTableModel.class.getResource("/icons/alert2_16.gif"));
  }
  static Predicate<Task> NOT_SUPERTASK = new Predicate<Task>() {
    @Override
    public boolean apply(Task task) {
      return !task.isSupertask();
    }
  };
  static Predicate<Task> NOT_MILESTONE = new Predicate<Task>() {
    @Override
    public boolean apply(Task input) {
      return !input.isMilestone();
    }
  };
  static {
    new DefaultBooleanOption("");
    TaskDefaultColumn.setLocaleApi(new TaskDefaultColumn.LocaleApi() {
      @Override
      public String i18n(String key) {
        return GanttLanguage.getInstance().getText(key);
      }
    });
  }
  private static GanttLanguage language = GanttLanguage.getInstance();

  private final CustomPropertyManager myCustomColumnsManager;

  private final UIFacade myUiFacade;

  private final Runnable myDirtyfier;

  private static final int STANDARD_COLUMN_COUNT = TaskDefaultColumn.values().length;
  /**
   * Creates an instance of GanttTreeTableModel with a root.
   *
   * @param root
   *          The root.
   * @param customColumnsManager
   * @param dirtyfier
   */
  public GanttTreeTableModel(
      TaskManager taskManager, CustomPropertyManager customColumnsManager, UIFacade uiFacade, Runnable dirtyfier) {
    super(new TaskNode(taskManager.getRootTask()));
    TaskDefaultColumn.BEGIN_DATE.setIsEditablePredicate(NOT_SUPERTASK);
    TaskDefaultColumn.BEGIN_DATE.setSortComparator(new BeginDateComparator());
    TaskDefaultColumn.END_DATE.setIsEditablePredicate(Predicates.and(NOT_SUPERTASK, NOT_MILESTONE));
    TaskDefaultColumn.END_DATE.setSortComparator(new EndDateComparator());
    TaskDefaultColumn.DURATION.setIsEditablePredicate(Predicates.and(NOT_SUPERTASK, NOT_MILESTONE));
    myUiFacade = uiFacade;
    myDirtyfier = dirtyfier;
    myCustomColumnsManager = customColumnsManager;
  }

  private static class BeginDateComparator implements Comparator<Task> {
    @Override
    public int compare(Task t1, Task t2) {
      return t1.getStart().compareTo(t2.getStart());
    }
  }


  private static class EndDateComparator implements Comparator<Task> {
    @Override
    public int compare(Task t1, Task t2) {
      return t1.getEnd().compareTo(t2.getEnd());
    }
  }

  @Override
  public int getColumnCount() {
    return STANDARD_COLUMN_COUNT + myCustomColumnsManager.getDefinitions().size();
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
        if (t.getCompletionPercentage() < 100) {
          Calendar c = GanttCalendar.getInstance();
          if (t.getStart().before(c)) {
            res = Icons.ALERT_TASK_INPROGRESS;
          }
          if (t.getEnd().before(GanttCalendar.getInstance())) {
            res = Icons.ALERT_TASK_OUTDATED;
          }
        }
        break;
      case NAME:
        res = tn.getName();
        break;
      case BEGIN_DATE:
        res = tn.getStart();
        break;
      case END_DATE:
        res = t.getDisplayEnd();
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
        res = TaskProperties.formatPredecessors(t, ",", true);
        break;
      case ID:
        res = t.getTaskID();
        break;
      case OUTLINE_NUMBER:
        List<Integer> outlinePath = t.getManager().getTaskHierarchy().getOutlinePath(t);
        res = Joiner.on('.').join(outlinePath);
        break;
      case COST:
        res = t.getCost().getValue();
        break;
      case COLOR:
        res = t.getColor();
        break;
      case RESOURCES:
    	List<String> resources = Lists.transform(Arrays.asList(t.getAssignments()), new Function<ResourceAssignment, String>() {
			@Override
			public String apply(ResourceAssignment ra) {
				return ra.getResource().getName();
			}
    	});
        res = Joiner.on(',').join(resources);
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
    myDirtyfier.run();
    if (column >= STANDARD_COLUMN_COUNT) {
      setCustomPropertyValue(value, node, column);
      return;
    }
    assert node instanceof TaskNode : "Tree node=" + node + " is not a task node";

    final Task task = (Task) ((TaskNode)node).getUserObject();
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
      //List<Integer> newIds = Lists.newArrayList();
      List<String> specs = Lists.newArrayList();
      for (String s : String.valueOf(value).split(",")) {
        if (!s.trim().isEmpty()) {
          specs.add(s.trim());
        }
      }
      Map<Integer, Supplier<TaskDependency>> promises;
      try {
         promises = TaskProperties.parseDependencies(
            specs, task, new Function<Integer, Task>() {
              @Override
              public Task apply(@Nullable Integer id) {
                return task.getManager().getTask(id);
              }
            });
        TaskManager taskManager = task.getManager();
        taskManager.getAlgorithmCollection().getScheduler().setEnabled(false);
        task.getDependenciesAsDependant().clear();
        for (Supplier<TaskDependency> promise : promises.values()) {
          promise.get();
        }
        taskManager.getAlgorithmCollection().getScheduler().setEnabled(true);
      } catch (IllegalArgumentException | TaskDependencyException e) {
        throw new ValidationException(e);
      }
      break;
    case COST:
      try {
        BigDecimal cost = new BigDecimal(String.valueOf(value));
        task.getCost().setCalculated(false);
        task.getCost().setValue(cost);
      } catch (NumberFormatException e) {
        throw new ValidationException(MessageFormat.format("Can't parse {0} as number", value));
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