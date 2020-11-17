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
package net.sourceforge.ganttproject.task.event;

import java.util.EventObject;

import biz.ganttproject.core.time.GanttCalendar;

import net.sourceforge.ganttproject.task.Task;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class TaskScheduleEvent extends EventObject {
  private final GanttCalendar myOldStartDate;

  private final GanttCalendar myOldFinishDate;

  private final GanttCalendar myNewStartDate;

  private final GanttCalendar myNewFinishDate;

  public TaskScheduleEvent(Task source, GanttCalendar oldStartDate, GanttCalendar oldFinishDate,
      GanttCalendar newStartDate, GanttCalendar newFinishDate) {
    super(source);
    myOldStartDate = oldStartDate;
    myOldFinishDate = oldFinishDate;
    myNewStartDate = newStartDate;
    myNewFinishDate = newFinishDate;
  }

  public Task getTask() {
    return (Task) getSource();
  }

  public GanttCalendar getOldStartDate() {
    return myOldStartDate;
  }

  public GanttCalendar getOldFinishDate() {
    return myOldFinishDate;
  }

  public GanttCalendar getNewStartDate() {
    return myNewStartDate;
  }

  public GanttCalendar getNewFinishDate() {
    return myNewFinishDate;
  }
}
