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

import java.util.Arrays;
import java.util.Collection;

import biz.ganttproject.core.time.GanttCalendar;
import biz.ganttproject.core.time.TimeUnitStack;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;

import static biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder.*;
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
        sb.append(" [ ");
        formatDate(task.getStart(), sb);
        sb.append(" - ");
        formatDate(task.getDisplayEnd(), sb);
        sb.append(" ] ");
        res = sb.toString();
      } else if (propertyID.equals(ID_TASK_NAME)) {
        sb.append(" " + task.getName() + " ");
        res = sb.toString();
      } else if (propertyID.equals(ID_TASK_LENGTH)) {
        sb.append(" [ ");
        sb.append((int) task.getDuration().getLength() + " " + GanttLanguage.getInstance().getText("days"));
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
        return formatPredecessors(task);
      }
    }
    return res;

  }

  public static String formatPredecessors(Task task) {
    TaskDependency[] dep = task.getDependenciesAsDependant().toArray();
    if (dep != null && dep.length > 0) {
      return Joiner.on(", ").join(Lists.transform(Arrays.asList(dep), new Function<TaskDependency, String>() {
        @Override
        public String apply(TaskDependency input) {
          return String.valueOf(input.getDependee().getTaskID());
        }
      }));
    }
    return "";
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
    return Joiner.on(", ").join(Collections2.transform(coordinators, new Function<ResourceAssignment, String>() {
      @Override
      public String apply(ResourceAssignment input) {
        return input.getResource().getName();
      }
    }));
  }
}
