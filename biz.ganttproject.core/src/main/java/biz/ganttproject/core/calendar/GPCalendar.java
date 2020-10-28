/*
Copyright 2013 BarD Software s.r.o

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

import java.util.Collection;
import java.util.Date;


/**
 * Represents a project calendar in GanttProject. Allows for managing weekend days
 * and public holidays.
 *  
 * @author dbarashev (Dmitry Barashev)
 */
public interface GPCalendar {
  /**
   * Flags corresponding to particular features of a calendar day. 
   * A day can be working, in the sense that tasks can run at this day,
   * or not working. At the same time, it may or may not be a weekend.
   * Weekend is normally a non-working day, however, it can be made working
   * if project owner decides to. 
   */
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

  CalendarEvent getEvent(Date date);
  public int getDayMask(Date date);
  
  //public boolean isNonWorkingDay(Date curDayStart);

  public void setPublicHolidays(Collection<CalendarEvent> holidays);


  /** @return an unmodifiable collection of (public) holidays */
  public Collection<CalendarEvent> getPublicHolidays();

  void importCalendar(GPCalendar calendar, ImportCalendarOption importOption);

  public String getBaseCalendarID();

  public void setBaseCalendarID(String id);

  public void addListener(GPCalendarListener listener);

  public String getID();
  public String getName();
  public void setName(String name);
  public void setID(String id);
}
