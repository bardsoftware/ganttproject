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
package biz.ganttproject.core.calendar;

import java.util.Collection;
import java.util.Date;


/**
 * @author bard
 */
public interface GPCalendar {
  public static interface DayMask {
    int WORKING = 1;
    int WEEKEND = 2;
    int HOLIDAY = 4;
  }

  public enum DayType {
    WORKING, NON_WORKING, WEEKEND, HOLIDAY
  }

  void setWeekDayType(int day, DayType type);

  DayType getWeekDayType(int day);

  public int getDayMask(Date date);
  
  //public boolean isNonWorkingDay(Date curDayStart);

  public void setPublicHolidays(Collection<CalendarEvent> holidays);


  /** @return an unmodifiable collection of (public) holidays */
  public Collection<CalendarEvent> getPublicHolidays();

  void importCalendar(GPCalendar calendar, ImportCalendarOption importOption);

  public String getBaseCalendarID();

  public void setBaseCalendarID(String id);


}
