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
package biz.ganttproject.core.time.impl;

import java.util.Calendar;
import java.util.Date;

import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.DateFrameable;


public class WeekFramerImpl implements DateFrameable {
  private final FramerImpl myDayFramer = new FramerImpl(Calendar.DATE);
  private final ICalendarFactory myCalendarFactory;

  public static interface ICalendarFactory {
    Calendar newCalendar();
  }

  private static class DefaultCalendarFactory implements ICalendarFactory {
    @Override
    public Calendar newCalendar() {
      return CalendarFactory.newCalendar();
    }
  }

  public WeekFramerImpl() {
    this(new DefaultCalendarFactory());
  }

  public WeekFramerImpl(ICalendarFactory calendarFactory) {
    myCalendarFactory = calendarFactory;
  }

  @Override
  public Date adjustRight(Date baseDate) {
    Calendar c = myCalendarFactory.newCalendar();
    do {
      baseDate = myDayFramer.adjustRight(baseDate);
      c.setTime(baseDate);
    } while (c.get(Calendar.DAY_OF_WEEK) != c.getFirstDayOfWeek());
    return c.getTime();
  }

  @Override
  public Date adjustLeft(Date baseDate) {
    Calendar c = myCalendarFactory.newCalendar();
    c.setTime(myDayFramer.adjustLeft(baseDate));
    while (c.get(Calendar.DAY_OF_WEEK) != c.getFirstDayOfWeek()) {
      c.setTime(myDayFramer.adjustLeft(myDayFramer.jumpLeft(c.getTime())));
    }
    return c.getTime();
  }

  @Override
  public Date jumpLeft(Date baseDate) {
    Calendar c = myCalendarFactory.newCalendar();
    c.setTime(myDayFramer.adjustLeft(baseDate));
    int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
    do {
      baseDate = myDayFramer.jumpLeft(baseDate);
      c.setTime(baseDate);
    } while (c.get(Calendar.DAY_OF_WEEK) != dayOfWeek);
    return c.getTime();
  }
}