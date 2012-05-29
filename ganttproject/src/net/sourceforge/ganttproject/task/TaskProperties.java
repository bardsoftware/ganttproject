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

import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.time.TimeUnitStack;

/**
 * Class with which one can get any properties (even custom) from any task.
 *
 * @author bbaranne
 *
 */
public class TaskProperties {

  public static final String ID_TASK_DATES = "taskDates";

  public static final String ID_TASK_NAME = "name";

  public static final String ID_TASK_LENGTH = "length";

  public static final String ID_TASK_ADVANCEMENT = "advancement";

  public static final String ID_TASK_COORDINATOR = "coordinator";

  public static final String ID_TASK_RESOURCES = "resources";

  public static final String ID_TASK_ID = "id";

  public static final String ID_TASK_PREDECESSORS = "predecessors";

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
        formatDate(task.getEnd(), sb);
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
        ResourceAssignment[] assignments = task.getAssignments();
        if (assignments.length > 0) {
          boolean first = true;
          boolean close = false;
          int j = 0;
          for (int i = 0; i < assignments.length; i++) {
            if (assignments[i].isCoordinator()) {
              j++;
              if (first) {
                close = true;
                first = false;
                sb.append("{");
              }
              if (j > 1) {
                sb.append(", ");
              }
              sb.append(assignments[i].getResource().getName());
            }
          }
          if (close)
            sb.append("}");
        }
        res = sb.toString();
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
        TaskDependency[] dep = task.getDependenciesAsDependant().toArray();
        int i = 0;
        if (dep != null && dep.length > 0) {
          for (i = 0; i < dep.length - 1; i++)
            sb.append(dep[i].getDependee().getTaskID() + ", ");
          sb.append(dep[i].getDependee().getTaskID());
        }
        res = sb.toString();
      }
    }
    return res;

  }
}
