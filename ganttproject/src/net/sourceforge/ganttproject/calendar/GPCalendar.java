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
package net.sourceforge.ganttproject.calendar;

import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.time.TimeUnit;

/**
 * @author bard
 */
public interface GPCalendar {
  public enum MoveDirection {
    FORWARD, BACKWARD
  }

  List<GPCalendarActivity> getActivities(Date startDate, Date endDate);

  List<GPCalendarActivity> getActivities(Date startDate, TimeUnit timeUnit, long l);

  void setWeekDayType(int day, DayType type);

  DayType getWeekDayType(int day);

  /**
   * @return true when weekends are only shown and taken into account for the
   *         task scheduling.
   */
  public boolean getOnlyShowWeekends();

  /**
   * @param onlyShowWeekends
   *          must be set to true if weekends are only shown and not taken into
   *          account for the task scheduling
   */
  public void setOnlyShowWeekends(boolean onlyShowWeekends);

  void setPublicHoliDayType(int month, int date);

  public void setPublicHoliDayType(Date curDayStart);

  public boolean isPublicHoliDay(Date curDayStart);

  public boolean isNonWorkingDay(Date curDayStart);

  public DayType getDayTypeDate(Date curDayStart);

  public void setPublicHolidays(URL calendar);

  /** Clears all defined public holidays */
  public void clearPublicHolidays();

  /** @return an unmodifiable collection of (public) holidays */
  public Collection<Date> getPublicHolidays();

  public GPCalendar copy();

  public enum DayType {
    WORKING, NON_WORKING, WEEKEND, HOLIDAY
  }

  Date findClosestWorkingTime(Date time);

  /**
   * Adds <code>shift</code> period to <code>input</code> date taking into
   * account this calendar working/non-working time If input date corresponds to
   * Friday midnight and this calendar if configured to have a weekend on
   * Saturday and Sunday then adding a shift of "1 day" will result to the
   * midnight of the next Monday
   */
  Date shiftDate(Date input, TaskLength shift);

  Date findClosest(Date time, TimeUnit timeUnit, MoveDirection direction, DayType dayType);

  GPCalendar PLAIN = new AlwaysWorkingTimeCalendarImpl();
  String EXTENSION_POINT_ID = "net.sourceforge.ganttproject.calendar";

  URL getPublicHolidaysUrl();

}
