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
package net.sourceforge.ganttproject.task;

import java.awt.Color;

import biz.ganttproject.core.chart.render.ShapePaint;
import biz.ganttproject.core.time.GanttCalendar;
import biz.ganttproject.core.time.TimeDuration;


/**
 * @author bard
 */
public interface MutableTask {
  void setName(String name);

  void setMilestone(boolean isMilestone);

  void setPriority(Task.Priority priority);

  void setStart(GanttCalendar start);

  void setEnd(GanttCalendar end);

  void setDuration(TimeDuration length);

  void shift(TimeDuration shift);

  void setCompletionPercentage(int percentage);

  // void setStartFixed(boolean isFixed);

  // void setFinishFixed(boolean isFixed);

  void setShape(ShapePaint shape);

  void setColor(Color color);

  /** Sets the weblink for the task */
  void setWebLink(String webLink);

  void setNotes(String notes);

  void addNotes(String notes);

  void setExpand(boolean expand);

  /**
   * Sets the task as critical or not. The method is used be TaskManager after
   * having run a CriticalPathAlgorithm to set the critical tasks. When painted,
   * the tasks are rendered as critical using Task.isCritical(). So, a task is
   * set as critical only if critical path is displayed.
   * 
   * @param critical
   *          <code>true</code> if this is critical, <code>false</code>
   *          otherwise.
   */
  void setCritical(boolean critical);

  void setTaskInfo(TaskInfo taskInfo);

  void setProjectTask(boolean projectTask);
}
