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
package net.sourceforge.ganttproject.task.dependency.constraint;

import java.util.Date;
import java.util.List;

import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependency.ActivityBinding;

/**
 * Dependant task starts not earlier than dependee finishes Created by IntelliJ
 * IDEA. User: bard
 */
public class FinishStartConstraintImpl extends ConstraintImpl implements TaskDependencyConstraint {
  public FinishStartConstraintImpl() {
    super(TaskDependencyConstraint.Type.finishstart, GanttLanguage.getInstance().getText("finishstart"));
  }

  @Override
  public TaskDependencyConstraint.Collision getCollision() {
    TaskDependencyConstraint.Collision result = null;
    Task dependee = getDependency().getDependee();
    Task dependant = getDependency().getDependant();
    GanttCalendar dependeeEnd = dependee.getEnd().clone();
    // GanttCalendar dependeeEnd = dependee.getEnd();
    GanttCalendar dependantStart = dependant.getStart();

    addDelay(dependeeEnd);
    // int difference = getDependency().getDifference();
    GanttCalendar comparisonDate = dependantStart.clone();
    // comparisonDate.add(difference);

    boolean isActive = getDependency().getHardness() == TaskDependency.Hardness.RUBBER ? dependeeEnd.compareTo(comparisonDate) > 0
        : dependeeEnd.compareTo(comparisonDate) != 0;
    // new Exception("[FinishStartConstraint] isActive="+isActive+"
    // dependdee="+dependee+" end="+dependeeEnd+"
    // start="+dependantStart).printStackTrace();
    result = new TaskDependencyConstraint.DefaultCollision(dependeeEnd,
        TaskDependencyConstraint.Collision.START_LATER_VARIATION, isActive);
    return result;
  }

  @Override
  public Collision getBackwardCollision(Date dependantStart) {
    if (dependantStart == null) {
      System.err.println();
    }
    Task dependee = getDependency().getDependee();
    GanttCalendar dependeeEnd = dependee.getEnd().clone();

    Date barrier = shift(dependantStart, -getDependency().getDifference());
    boolean isActive = getDependency().getHardness() == TaskDependency.Hardness.RUBBER ? dependeeEnd.getTime().compareTo(
        barrier) > 0
        : dependeeEnd.getTime().compareTo(barrier) != 0;

    return new TaskDependencyConstraint.DefaultCollision(CalendarFactory.createGanttCalendar(barrier),
        TaskDependencyConstraint.Collision.START_EARLIER_VARIATION, isActive);
  }

  @Override
  public ActivityBinding getActivityBinding() {
    List<TaskActivity> dependantActivities = getDependency().getDependant().getActivities();
    List<TaskActivity> dependeeActivities = getDependency().getDependee().getActivities();
    if (dependantActivities.isEmpty()) {
      GPLogger.logToLogger("Task " + getDependency().getDependant() + " has no activities");
      return null;
    }
    if (dependeeActivities.isEmpty()) {
      GPLogger.logToLogger("Task " + getDependency().getDependee() + " has no activities");
      return null;
    }
    TaskActivity theDependant = dependantActivities.get(0);
    TaskActivity theDependee = dependeeActivities.get(dependeeActivities.size() - 1);
    return new DependencyActivityBindingImpl(theDependant, theDependee, new Date[] { theDependant.getStart(),
        theDependee.getEnd() });
  }

}
