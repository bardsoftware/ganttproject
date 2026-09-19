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
import java.util.Objects;

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

  /**
   * Two day off intervals are the same when they start on the same day and end on the same day.
   * Those two calendars are the only state this class has, so there is nothing else equality could
   * be built on.
   *
   * This used to be an overload, {@code equals(GanttDaysOff)}, which {@code Object.equals} callers
   * never reach: {@link java.util.List#remove} and friends call {@code equals(Object)}, so removing
   * an interval by value quietly did nothing and an interval could only be removed by identity.
   * Now that HumanResource delegates removal to its list, this has to be the real override.
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof GanttDaysOff)) {
      return false;
    }
    GanttDaysOff dayOffs = (GanttDaysOff) other;
    return myStart.equals(dayOffs.myStart) && myFinish.equals(dayOffs.myFinish);
  }

  /**
   * Built from the year, month and day of both ends rather than from the two calendars' own
   * hashCodes, because GanttCalendar overrides equals(Object) -- comparing exactly those three
   * fields -- without overriding hashCode(). It therefore inherits GregorianCalendar.hashCode(),
   * which takes the time of day into account. Two GanttCalendars that are equal can have different
   * hashCodes, and a GanttDaysOff delegating to them would inherit that broken contract: a day off
   * built from a Date keeps whatever time of day that Date carried.
   */
  @Override
  public int hashCode() {
    return Objects.hash(
        myStart.getYear(), myStart.getMonth(), myStart.getDay(),
        myFinish.getYear(), myFinish.getMonth(), myFinish.getDay());
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
