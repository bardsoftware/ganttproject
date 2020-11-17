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
package biz.ganttproject.core.time;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public abstract class CalendarFactory {
  public static interface LocaleApi {
    Locale getLocale();
    DateFormat getShortDateFormat();
  }

  private static LocaleApi ourLocaleApi;
  
  public static Calendar newCalendar() {
    return (Calendar) Calendar.getInstance(ourLocaleApi.getLocale()).clone();
  }
  
  protected static void setLocaleApi(LocaleApi localeApi) {
    ourLocaleApi = localeApi;
  }

  public static GanttCalendar createGanttCalendar(Date date) {
    return new GanttCalendar(date, ourLocaleApi);
  }

  public static GanttCalendar createGanttCalendar(int year, int month, int date) {
    return new GanttCalendar(year, month, date, ourLocaleApi);
  }

  public static GanttCalendar createGanttCalendar() {
    return new GanttCalendar(ourLocaleApi);
  }
}
