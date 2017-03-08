/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject Team

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
package net.sourceforge.ganttproject.task.dependency;

import biz.ganttproject.core.chart.scene.gantt.Connector;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;
import biz.ganttproject.core.chart.scene.BarChartActivity;

import java.awt.*;

/**
 * @author bard
 */
public class TaskDependencyImpl implements TaskDependency {
  private TaskDependencyConstraint myConstraint;

  private int myDifference;

  private final Task myDependant;

  private final Task myDependee;

  private Hardness myHardness;

  private TaskDependencyCollectionImpl myCollection;

  private BarChartActivity<Task> myStartActivity;

  private TaskActivity myEndActivity;

  public TaskDependencyImpl(Task dependant, Task dependee, TaskDependencyCollectionImpl collection) {
    this(dependant, dependee, collection, null);
  }

  TaskDependencyImpl(Task dependant, Task dependee, TaskDependencyCollectionImpl collection, TaskDependencyConstraint constraint) {
    this(dependant, dependee, collection, constraint, Hardness.STRONG, 0);
  }

  public TaskDependencyImpl(Task dependant, Task dependee, TaskDependencyCollectionImpl collection, TaskDependencyConstraint constraint, Hardness hardness, int lag) {
    myDependant = dependant;
    myDependee = dependee;
    myCollection = collection;
    if (dependee == null || dependant == null) {
      throw new IllegalArgumentException("invalid participants of dependency: dependee=" + dependee + " dependant="
          + dependant);
    }
    myDifference = lag;
    myHardness = hardness;
    myConstraint = constraint;
    if (constraint != null) {
      constraint.setTaskDependency(this);
    }
  }

  @Override
  public Task getDependant() {
    return myDependant;
  }

  @Override
  public Task getDependee() {
    return myDependee;
  }

  @Override
  public void setConstraint(TaskDependencyConstraint constraint) {
    myStartActivity = null;
    myEndActivity = null;
    myConstraint = constraint;
    constraint.setTaskDependency(this);
    myCollection.fireChanged(this);
  }

  @Override
  public TaskDependencyConstraint getConstraint() {
    return myConstraint;
  }

  @Override
  public ActivityBinding getActivityBinding() {
    return getConstraint().getActivityBinding();
  }

  @Override
  public void delete() {
    myCollection.delete(this);
  }

  @Override
  public boolean equals(Object obj) {
    boolean result = obj instanceof TaskDependency;
    if (result) {
      TaskDependency rvalue = (TaskDependency) obj;
      result = myDependant.equals(rvalue.getDependant()) && myDependee.equals(rvalue.getDependee());
    }
    return result;
  }

  @Override
  public int hashCode() {
    return 7 * myDependant.hashCode() + 9 * myDependee.hashCode();
  }

  @Override
  public void setDifference(int difference) {
    myDifference = difference;
    myCollection.fireChanged(this);
  }

  @Override
  public int getDifference() {
    return myDifference;
  }

  @Override
  public Hardness getHardness() {
    return myHardness;
  }

  @Override
  public void setHardness(Hardness hardness) {
    myHardness = hardness;
  }

  @Override
  public String toString() {
    return myDependee + "->" + myDependant;
  }

  public BarChartActivity<Task> getStart() {
//    if (myStartActivity == null) {
      ActivityBinding activityBinding = getConstraint().getActivityBinding();
      return activityBinding == null ? null : activityBinding.getDependeeActivity();
      //myStartActivity = dependeeActivity.getOwner().isMilestone() ? new MilestoneTaskFakeActivity(dependeeActivity.getOwner()) : dependeeActivity;
//    }
//    return myStartActivity;
  }

  @Override
  public Dimension getStartVector() {
    TaskDependencyConstraint.Type type = getConstraint().getType();
    if (type == TaskDependencyConstraint.Type.finishfinish || type == TaskDependencyConstraint.Type.finishstart) {
      return Connector.Vector.EAST;
    }
    return Connector.Vector.WEST;
  }

  public BarChartActivity<Task> getEnd() {
//    if (myEndActivity == null) {
      ActivityBinding activityBinding = getConstraint().getActivityBinding();
      return activityBinding == null ? null : activityBinding.getDependantActivity();
      //myEndActivity = dependantActivity.getOwner().isMilestone() ? new MilestoneTaskFakeActivity(dependantActivity.getOwner()) : dependantActivity;
//    }
//    return myEndActivity;
  }

  @Override
  public Dimension getEndVector() {
    TaskDependencyConstraint.Type type = getConstraint().getType();
    if (type == TaskDependencyConstraint.Type.finishfinish || type == TaskDependencyConstraint.Type.startfinish) {
      return Connector.Vector.EAST;
    }
    return Connector.Vector.WEST;
  }

  public TaskDependency getImpl() {
    return this;
  }

}
