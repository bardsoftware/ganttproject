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
package net.sourceforge.ganttproject.task.dependency;

import java.util.Date;

import net.sourceforge.ganttproject.GanttCalendar;

/**
 * Created by IntelliJ IDEA. User: bard Date: 14.02.2004 Time: 2:35:20 To change
 * this template use File | Settings | File Templates.
 */
public interface TaskDependencyConstraint extends Cloneable {
  enum Type {
    startstart, finishstart, finishfinish, startfinish;

    public static Type getType(TaskDependencyConstraint constraint) {
      return getType(constraint.getID());
    }

    public static Type getType(int constraintID) {
      for (Type t : Type.values()) {
        if (t.ordinal() + 1 == constraintID) {
          return t;
        }
      }
      return null;
    }
  }

  void setTaskDependency(TaskDependency dependency);

  // boolean isFulfilled();
  // void fulfil();
  Collision getCollision();

  Collision getBackwardCollision(Date depedantStart);

  String getName();

  int getID();

  TaskDependency.ActivityBinding getActivityBinding();

  interface Collision {
    GanttCalendar getAcceptableStart();

    int getVariation();

    int NO_VARIATION = 0;

    int START_EARLIER_VARIATION = -1;

    int START_LATER_VARIATION = 1;

    boolean isActive();
  }

  class DefaultCollision implements Collision {
    private final GanttCalendar myAcceptableStart;

    private final int myVariation;

    private final boolean isActive;

    public DefaultCollision(GanttCalendar myAcceptableStart, int myVariation, boolean isActive) {
      this.myAcceptableStart = myAcceptableStart;
      this.myVariation = myVariation;
      this.isActive = isActive;
    }

    @Override
    public GanttCalendar getAcceptableStart() {
      return myAcceptableStart;
    }

    @Override
    public int getVariation() {
      return myVariation;
    }

    @Override
    public boolean isActive() {
      return isActive;
    }

  }

  Object clone() throws CloneNotSupportedException;
}
