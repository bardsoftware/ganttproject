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
package net.sourceforge.ganttproject.task;

import biz.ganttproject.core.time.GanttCalendar;
import biz.ganttproject.core.time.TimeUnitStack;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint.Type;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder.ID_TASK_ADVANCEMENT;
import static biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder.ID_TASK_COORDINATOR;
import static biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder.ID_TASK_DATES;
import static biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder.ID_TASK_ID;
import static biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder.ID_TASK_LENGTH;
import static biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder.ID_TASK_NAME;
import static biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder.ID_TASK_PREDECESSORS;
import static biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder.ID_TASK_RESOURCES;
/**
 * Class with which one can get any properties (even custom) from any task.
 *
 * @author bbaranne
 *
 */
public class TaskProperties {

  private final TimeUnitStack myTimeUnitStack;

  public TaskProperties(TimeUnitStack timeUnitStack) {
    myTimeUnitStack = timeUnitStack;
  }

  private void formatDate(GanttCalendar date, StringBuffer buf) {
    buf.append(GanttLanguage.getInstance().formatShortDate(date));
    if (myTimeUnitStack.getTimeFormat() != null) {
      buf.append(" ").append(GanttLanguage.getInstance().formatTime(date));
    }

  }

  /**
   * Returns the task property specified by <code>propertyID</code>.
   *
   * @param task
   *          The task from which we want the property.
   * @param propertyID
   *          The property ID which could be <code>ID_TASK_DATES</code>,
   *          <code>ID_TASK_NAME</code>, ... or a custom column name.
   * @return the task property specified by <code>propertyID</code>. The result
   *         may be <code>null</code>.
   */
  public Object getProperty(Task task, String propertyID) {
    Object res = null;
    StringBuffer sb = new StringBuffer();
    if (propertyID != null) {
      if (propertyID.equals(ID_TASK_DATES)) {
        if (task.isMilestone()) {
          formatDate(task.getStart(), sb);
        } else {
          sb.append(" [ ");
          formatDate(task.getStart(), sb);
          sb.append(" - ");
          formatDate(task.getDisplayEnd(), sb);
          sb.append(" ] ");
        }
        res = sb.toString();
      } else if (propertyID.equals(ID_TASK_NAME)) {
        sb.append(" " + task.getName() + " ");
        res = sb.toString();
      } else if (propertyID.equals(ID_TASK_LENGTH)) {
        if (task.isMilestone()) {
          return "";
        }
        sb.append(" [ ");
        sb.append(task.getDuration().getLength() + " " + GanttLanguage.getInstance().getText("days"));
        sb.append(" ] ");
        res = sb.toString();
      } else if (propertyID.equals(ID_TASK_ADVANCEMENT)) {
        sb.append(" [ ");
        sb.append(task.getCompletionPercentage() + "%");
        sb.append(" ] ");
        res = sb.toString();
      } else if (propertyID.equals(ID_TASK_COORDINATOR)) {
        String coordinators = formatCoordinators(task);
        return coordinators.isEmpty() ? "" : "{" + coordinators + "}";
      } else if (propertyID.equals(ID_TASK_RESOURCES)) {
        ResourceAssignment[] assignments = task.getAssignments();
        if (assignments.length > 0) {
          sb.append(" ");
          /* Keep coordinators and other resources separate */
          StringBuffer resources = new StringBuffer();
          StringBuffer coordinators = new StringBuffer();
          for (int i = 0; i < assignments.length; i++) {
            /* Creates list of resources in format {coordinators},resources */
            if (assignments[i].isCoordinator()) {
              if (coordinators.length() > 0) {
                coordinators.append(",");
              }
              coordinators.append(assignments[i].getResource().getName());
            } else {
              if (resources.length() > 0) {
                resources.append(",");
              }
              resources.append(assignments[i].getResource().getName());
            }
          }
          if (coordinators.length() > 0) {
            sb.append("{");
            sb.append(coordinators);
            sb.append("}");
            if (resources.length() > 0) {
              sb.append(",");
            }
          }
          if (resources.length() > 0) {
            sb.append(resources);
          }
          sb.append(" ");
        }
        res = sb.toString();
      } else if (propertyID.equals(ID_TASK_ID)) {
        sb.append("# ").append(task.getTaskID());
        res = sb.toString();
      } else if (propertyID.equals(ID_TASK_PREDECESSORS)) {
        return formatPredecessors(task, ", ", false);
      }
    }
    return res;

  }

  public static String formatPredecessors(Task task, String separator) {
    return formatPredecessors(task, separator, false);
  }

  public static String formatPredecessors(Task task, String separator, final boolean addDependencyType) {
    TaskDependency[] dep = task.getDependenciesAsDependant().toArray();
    if (dep != null && dep.length > 0) {
      return Joiner.on(separator).join(Lists.transform(Arrays.asList(dep), new Function<TaskDependency, String>() {
        @Override
        public String apply(TaskDependency input) {
          StringBuilder builder = new StringBuilder(String.valueOf(input.getDependee().getTaskID()));
          if (!addDependencyType) {
            return builder.toString();
          }
          TaskDependencyConstraint constraint = input.getConstraint();
          if (constraint.getType() == Type.finishstart && input.getDifference() == 0 && input.getHardness() == TaskDependency.Hardness.STRONG) {
            return builder.toString();
          }
          builder.append("-").append(constraint.getType().getReadablePersistentValue());
          if (input.getDifference() == 0 && input.getHardness() == TaskDependency.Hardness.STRONG) {
            return builder.toString();
          }
          if (input.getHardness() == TaskDependency.Hardness.RUBBER) {
            builder.append(">");
          } else {
            builder.append("=");
          }
          builder.append(String.format("P%dD", input.getDifference()));
          return builder.toString();
        }
      }));
    }
    return "";
  }

  public static Map<Integer, Supplier<TaskDependency>> parseDependencies(
      Iterable<String> deps, Task successor, Function<Integer, Task> taskIndex) {
    Map<Integer, Supplier<TaskDependency>> result = Maps.newLinkedHashMap();
    for (String spec : deps) {
      parseDependency(spec, successor, taskIndex, result);
    }
    return result;
  }

  public static void parseDependency(String depSpec, final Task successor, Function<Integer, Task> taskIndex,
                                     Map<Integer, Supplier<TaskDependency>> out) {
    final TaskManager taskMgr = successor.getManager();
    int posDash = depSpec.indexOf('-');
    String maybeId = posDash < 0 ? depSpec : depSpec.substring(0, posDash);
    final Integer predecessorId;
    try {
      predecessorId = Integer.parseInt(maybeId);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(String.format("%s is not a number", maybeId));
    }
    if (posDash < 0) {
      final Task predecessor = taskIndex.apply(predecessorId);
      if (predecessor == null) {
        throw new IllegalArgumentException(String.format("Can't find task with ID=%s", depSpec));
      }
      out.put(predecessorId, new Supplier<TaskDependency>() {
        @Override
        public TaskDependency get() {
          if (taskMgr.getDependencyCollection().canCreateDependency(successor, predecessor)) {
            return taskMgr.getDependencyCollection().createDependency(successor, predecessor);
          }
          throw new TaskDependencyException(MessageFormat.format(
              "Can't create dependency between task {0} and {1}", successor.getName(), predecessor.getName()));
        }
      });
      return;
    }


    if (depSpec.length() < posDash + 3) {
      throw new IllegalArgumentException(String.format("Invalid dependency spec '%s'. There must be a two-letter dependency type specification after dash", depSpec));
    }
    final Task predecessor = taskIndex.apply(predecessorId);
    if (predecessor == null) {
      throw new IllegalArgumentException(String.format("Can't find task with ID=%s", depSpec));
    }
    TaskDependencyConstraint.Type depType = TaskDependencyConstraint.Type.fromReadablePersistentValue(depSpec.substring(posDash + 1, posDash + 3));
    if (depSpec.length() == posDash + 3) {
      final TaskDependencyConstraint constraint = taskMgr.createConstraint(depType);
      out.put(predecessorId, new Supplier<TaskDependency>() {
        @Override
        public TaskDependency get() {
          if (taskMgr.getDependencyCollection().canCreateDependency(successor, predecessor)) {
            return taskMgr.getDependencyCollection().createDependency(successor, predecessor, constraint);
          }
          throw new TaskDependencyException(MessageFormat.format(
              "Can't create dependency between task {0} and {1}", successor.getName(), predecessor.getName()));
        }
      });
      return;
    }


    char hardnessSpec = depSpec.charAt(posDash + 3);
    if (hardnessSpec != '=' && hardnessSpec != '>') {
      throw new IllegalArgumentException(String.format("Invalid dependency spec '%s'. There must be either > or = char after dependency type", depSpec));
    }
    if (depSpec.charAt(posDash + 4) != 'P' || depSpec.charAt(depSpec.length() - 1) != 'D') {
      throw new IllegalArgumentException(String.format("Invalid dependency spec '%s'. Lag interval is expected to be P*D where * denotes integer value of lag days, e.g. P1D", depSpec));
    }
    final int lag = Integer.parseInt(depSpec.substring(posDash + 5, depSpec.length() - 1));
    final TaskDependency.Hardness hardness = hardnessSpec == '=' ? TaskDependency.Hardness.STRONG : TaskDependency.Hardness.RUBBER;
    final TaskDependencyConstraint constraint = taskMgr.createConstraint(depType);
    out.put(predecessorId, new Supplier<TaskDependency>() {
      @Override
      public TaskDependency get() {
        if (taskMgr.getDependencyCollection().canCreateDependency(successor, predecessor)) {
          TaskDependency dependency = taskMgr.getDependencyCollection().createDependency(successor, predecessor, constraint, hardness);
          if (lag != 0) {
            dependency.setDifference(lag);
          }
          return dependency;
        }
        throw new TaskDependencyException(MessageFormat.format(
            "Can't create dependency between task {0} and {1}", successor.getName(), predecessor.getName()));
      }
    });
  }

  public static String formatCoordinators(Task t) {
    ResourceAssignment[] assignments = t.getAssignments();
    Collection<ResourceAssignment> coordinators = Collections2.filter(Arrays.asList(assignments), new Predicate<ResourceAssignment>() {
      @Override
      public boolean apply(ResourceAssignment input) {
        return input.isCoordinator();
      }
    });
    if (coordinators.isEmpty()) {
      return "";
    }
    return formatResources(coordinators);
  }

  public static String formatResources(Collection<ResourceAssignment> resources) {
    return Joiner.on(", ").join(Collections2.transform(resources, new Function<ResourceAssignment, String>() {
        @Override
        public String apply(ResourceAssignment input) {
          return input.getResource().getName();
        }
      }));
  }
}
