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
package biz.ganttproject.core.calendar;

import java.util.Calendar;
import java.util.Date;

import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;


/**
 * @author nbohn
 */
public class GanttDaysOff {
  private final GanttCalendar myStart, myFinish;

  public GanttDaysOff(Date start, Date finish) {
    myStart = CalendarFactory.createGanttCalendar(start);
    myFinish = CalendarFactory.createGanttCalendar(finish);
  }

  public GanttDaysOff(GanttCalendar start, GanttCalendar finish) {
    myStart = CalendarFactory.createGanttCalendar(start.getYear(), start.getMonth(), start.getDate());
    myFinish = finish;
  }

  @Override
  public String toString() {
    return (myStart + " -> " + myFinish);
  }

  public boolean equals(GanttDaysOff dayOffs) {
    return ((dayOffs.getStart().equals(myStart)) && (dayOffs.getFinish().equals(myFinish)));
  }

  public GanttCalendar getStart() {
    return myStart;
  }

  public GanttCalendar getFinish() {
    return myFinish;
  }

  public boolean isADayOff(GanttCalendar date) {
    return (date.equals(myStart) || date.equals(myFinish) || (date.before(myFinish) && date.after(myStart)));
  }

  public boolean isADayOff(Date date) {
    return (date.equals(myStart.getTime()) || date.equals(myFinish.getTime()) || (date.before(myFinish.getTime()) && date.after(myStart.getTime())));
  }

  public int isADayOffInWeek(Date date) {
    GanttCalendar start = myStart.clone();
    GanttCalendar finish = myFinish.clone();
    for (int i = 0; i < 7; i++) {
      start.add(Calendar.DATE, -1);
      finish.add(Calendar.DATE, -1);
      if (date.equals(start.getTime()) || date.equals(finish.getTime())
          || (date.before(finish.getTime()) && date.after(start.getTime())))
        return i + 1;
    }
    return -1;
  }

  public static GanttDaysOff create(GanttDaysOff from) {
    return new GanttDaysOff(from.myStart.clone(), from.myFinish.clone());
  }

}
