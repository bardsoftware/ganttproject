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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import biz.ganttproject.core.calendar.CalendarEvent.Type;
import biz.ganttproject.core.calendar.GPCalendar.DayMask;
import biz.ganttproject.core.calendar.GPCalendar.DayType;
import biz.ganttproject.core.time.CalendarFactory;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import junit.framework.TestCase;

/**
 * Tests for {@link WeekendsCalendarImpl} class.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class WeekendCalendarImplTest extends TestCase {
  static {
    new CalendarFactory() {
      {
        setLocaleApi(new LocaleApi() {
          @Override
          public Locale getLocale() {
            return Locale.US;
          }
          @Override
          public DateFormat getShortDateFormat() {
            return DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
          }
        });
      }
    };
  }
  private static List<CalendarEvent> TEST_EVENTS = ImmutableList.of(
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 0, 1).getTime(), true, CalendarEvent.Type.HOLIDAY, "Jan 1", null),
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 1, 14).getTime(), false, CalendarEvent.Type.NEUTRAL, "Feb 14", null),
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 2, 8).getTime(), true, CalendarEvent.Type.HOLIDAY, "Mar 8", null),
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 2, 8).getTime(), false, CalendarEvent.Type.WORKING_DAY, "Mar 8, 2014", null),
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 2, 9).getTime(), false, CalendarEvent.Type.HOLIDAY, "Mar 9, 2014", null),
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 3, 12).getTime(), true, CalendarEvent.Type.WORKING_DAY, "Apr 12", null),
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 3, 12).getTime(), false, CalendarEvent.Type.HOLIDAY, "Apr 12, 2014", null)
  );
  private static List<CalendarEvent> TEST_EVENTS_RECURRING_FIRST = ImmutableList.of(
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 0, 1).getTime(), true, CalendarEvent.Type.HOLIDAY, "Jan 1", null),
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 2, 8).getTime(), true, CalendarEvent.Type.HOLIDAY, "Mar 8", null),
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 3, 12).getTime(), true, CalendarEvent.Type.WORKING_DAY, "Apr 12", null),
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 1, 14).getTime(), false, CalendarEvent.Type.NEUTRAL, "Feb 14", null),
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 2, 8).getTime(), false, CalendarEvent.Type.WORKING_DAY, "Mar 8, 2014", null),
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 2, 9).getTime(), false, CalendarEvent.Type.HOLIDAY, "Mar 9, 2014", null),
      CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 3, 12).getTime(), false, CalendarEvent.Type.HOLIDAY, "Apr 12, 2014", null)
  );


  private static Function<CalendarEvent, String> GET_TITLE = new Function<CalendarEvent, String>() {
    @Override
    public String apply(CalendarEvent e) {
      return e.getTitle();
    }
  };

  public void testSetEvents() {
    WeekendCalendarImpl calendar = new WeekendCalendarImpl();
    calendar.setPublicHolidays(TEST_EVENTS);
    assertEquals(TEST_EVENTS_RECURRING_FIRST, calendar.getPublicHolidays());
    assertEquals(ImmutableList.of("Jan 1", "Mar 8", "Apr 12", "Feb 14", "Mar 8, 2014", "Mar 9, 2014", "Apr 12, 2014"),
        Lists.newArrayList(Collections2.transform(calendar.getPublicHolidays(), GET_TITLE)));
  }

  public void testRecurringHoliday() {
    WeekendCalendarImpl calendar = new WeekendCalendarImpl();
    calendar.setPublicHolidays(TEST_EVENTS);
    assertEquals(DayMask.HOLIDAY, calendar.getDayMask(CalendarFactory.createGanttCalendar(2013, 0, 1).getTime()) & DayMask.HOLIDAY);
    assertEquals(DayMask.HOLIDAY, calendar.getDayMask(CalendarFactory.createGanttCalendar(2014, 0, 1).getTime()) & DayMask.HOLIDAY);
    assertEquals(DayMask.HOLIDAY, calendar.getDayMask(CalendarFactory.createGanttCalendar(2015, 0, 1).getTime()) & DayMask.HOLIDAY);
    assertEquals(CalendarEvent.Type.HOLIDAY, calendar.getEvent(CalendarFactory.createGanttCalendar(2015, 0, 1).getTime()).getType());
  }

  public void testOneOffHoliday() {
    WeekendCalendarImpl calendar = new WeekendCalendarImpl();
    calendar.setWeekDayType(Calendar.SATURDAY, DayType.WORKING);
    calendar.setWeekDayType(Calendar.SUNDAY, DayType.WORKING);
    calendar.setPublicHolidays(TEST_EVENTS);
    assertEquals(DayMask.WORKING, calendar.getDayMask(CalendarFactory.createGanttCalendar(2013, 3, 12).getTime()) & DayMask.WORKING);
    assertEquals(DayMask.HOLIDAY, calendar.getDayMask(CalendarFactory.createGanttCalendar(2014, 3, 12).getTime()) & DayMask.HOLIDAY);
    assertEquals(DayMask.WORKING, calendar.getDayMask(CalendarFactory.createGanttCalendar(2015, 3, 12).getTime()) & DayMask.WORKING);
  }

  public void testOneOffWorking() {
    WeekendCalendarImpl calendar = new WeekendCalendarImpl();
    calendar.setWeekDayType(Calendar.SATURDAY, DayType.WORKING);
    calendar.setWeekDayType(Calendar.SUNDAY, DayType.WORKING);
    calendar.setPublicHolidays(TEST_EVENTS);
    assertEquals(DayMask.HOLIDAY, calendar.getDayMask(CalendarFactory.createGanttCalendar(2013, 2, 8).getTime()) & DayMask.HOLIDAY);
    assertEquals(DayMask.WORKING, calendar.getDayMask(CalendarFactory.createGanttCalendar(2014, 2, 8).getTime()) & DayMask.WORKING);
    assertEquals(DayMask.HOLIDAY, calendar.getDayMask(CalendarFactory.createGanttCalendar(2015, 2, 8).getTime()) & DayMask.HOLIDAY);
  }

  public void testOneOffWorkingWeekend() {
    WeekendCalendarImpl calendar = new WeekendCalendarImpl();
    calendar.setPublicHolidays(ImmutableList.of(
        CalendarEvent.newEvent(CalendarFactory.createGanttCalendar(2014, 0, 4).getTime(), false, CalendarEvent.Type.WORKING_DAY, "Jan 4, Saturday", null)
    ));
    assertEquals(DayMask.WORKING, calendar.getDayMask(CalendarFactory.createGanttCalendar(2014, 0, 4).getTime()) & DayMask.WORKING);
    assertEquals(0, calendar.getDayMask(CalendarFactory.createGanttCalendar(2014, 0, 11).getTime()) & DayMask.WORKING);
    assertEquals(DayMask.WEEKEND, calendar.getDayMask(CalendarFactory.createGanttCalendar(2014, 0, 11).getTime()) & DayMask.WEEKEND);
  }
}
