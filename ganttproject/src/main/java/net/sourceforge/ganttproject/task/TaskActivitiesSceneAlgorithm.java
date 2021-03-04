/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.task;

import biz.ganttproject.core.calendar.GPCalendarActivity;
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.time.TimeDuration;
import kotlin.jvm.functions.Function2;
import net.sourceforge.ganttproject.chart.gantt.ITaskActivity;
import net.sourceforge.ganttproject.chart.gantt.ITaskSceneTask;
import net.sourceforge.ganttproject.chart.gantt.TaskSceneTaskActivityImpl;

import java.util.Date;
import java.util.List;


public class TaskActivitiesSceneAlgorithm {
  private final GPCalendarCalc myCalendar;
  private final Function2<Date, Date, TimeDuration> durationCalc;


  public TaskActivitiesSceneAlgorithm(GPCalendarCalc calendar, Function2<Date, Date, TimeDuration> durationCalc) {
    myCalendar = calendar;
    this.durationCalc = durationCalc;
  }

  public void recalculateActivities(ITaskSceneTask task, List<ITaskActivity<ITaskSceneTask>> output, Date startDate, Date endDate) {
    output.clear();
    List<GPCalendarActivity> activities = myCalendar.getActivities(startDate, endDate);
    for (int i = 0; i < activities.size(); i++) {
      GPCalendarActivity activity = activities.get(i);
      ITaskActivity<ITaskSceneTask> nextTaskActivity;
      TimeDuration duration = durationCalc.invoke(activity.getStart(), activity.getEnd());
      if (activity.isWorkingTime()) {
        nextTaskActivity = new TaskSceneTaskActivityImpl(task, activity.getStart(), activity.getEnd(), duration);
      } else if (i > 0 && i + 1 < activities.size()) {
        nextTaskActivity = new TaskSceneTaskActivityImpl(task, activity.getStart(), activity.getEnd(), duration, 0f);
      } else {
        continue;
      }
      output.add(nextTaskActivity);
    }
  }
}
