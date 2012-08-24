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
package net.sourceforge.ganttproject.chart;

import java.util.Date;

import biz.ganttproject.core.time.TimeDuration;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskActivity;

public class MilestoneTaskFakeActivity implements TaskActivity {
  private final Task myTask;
  private final Date myStartTime;
  private final Date myEndTime;

  public MilestoneTaskFakeActivity(Task task) {
    this(task, task.getStart().getTime(), task.getEnd().getTime());
  }

  public MilestoneTaskFakeActivity(Task task, Date startTime, Date endTime) {
    myTask = task;
    myStartTime = startTime;
    myEndTime = endTime;
  }

  @Override
  public TimeDuration getDuration() {
    return myTask.getManager().createLength(1);
  }

  @Override
  public Date getEnd() {
    return myEndTime;
  }

  @Override
  public float getIntensity() {
    return 1;
  }

  @Override
  public Date getStart() {
    return myStartTime;
  }

  @Override
  public Task getOwner() {
    return myTask;
  }

  @Override
  public boolean isFirst() {
    return true;
  }

  @Override
  public boolean isLast() {
    return true;
  }

  @Override
  public String toString() {
    return "Milestone activity [" + getStart() + "-" + getEnd() + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof MilestoneTaskFakeActivity) {
      return ((MilestoneTaskFakeActivity) obj).myTask.equals(myTask);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return myTask.hashCode();
  }

}
