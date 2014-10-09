/*
Copyright 2014 BarD Software s.r.o

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
package net.sourceforge.ganttproject;

import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.calendar.CalendarEvent.Type;
import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.time.CalendarFactory;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Action which adds or removes calendar events, such as weekend exceptions or holidays, from the chart UI
 *
 * @author dbarashev (Dmitry Barashev)
 */
public abstract class CalendarEventAction extends GPAction {
  protected final GPCalendar myCalendar;
  protected final Date myDate;
  public CalendarEventAction(GPCalendar calendar, Date date, String id) {
    super(id);
    myCalendar = Preconditions.checkNotNull(calendar);
    myDate = Preconditions.checkNotNull(date);
    updateName();
  }

  @Override
  protected String getLocalizedName() {
    GanttLanguage i18n = GanttLanguage.getInstance();
    return myDate == null ? super.getLocalizedName()
        : i18n.formatText(getID(), i18n.formatShortDate(CalendarFactory.createGanttCalendar(myDate)));
  }


  public static CalendarEventAction addException(GPCalendar calendar, Date date) {
    return new CalendarEventAction(calendar, date, "calendar.action.weekendException.add") {
      @Override
      public void actionPerformed(ActionEvent e) {
        Set<CalendarEvent> events = Sets.newLinkedHashSet(myCalendar.getPublicHolidays());
        events.add(CalendarEvent.newEvent(
            myDate, false, Type.WORKING_DAY, GanttLanguage.getInstance().getText("calendar.action.weekendException.add.description"), null));
        myCalendar.setPublicHolidays(events);
      }
    };
  }

  public static CalendarEventAction removeException(GPCalendar calendar, Date date) {
    return new CalendarEventAction(calendar, date, "calendar.action.weekendException.remove") {
      @Override
      public void actionPerformed(ActionEvent e) {
        Set<CalendarEvent> events = Sets.newLinkedHashSet(myCalendar.getPublicHolidays());
        events.remove(CalendarEvent.newEvent(myDate, false, Type.WORKING_DAY, null, null));
        myCalendar.setPublicHolidays(events);
      }
    };
  }

  public static CalendarEventAction addHoliday(GPCalendar calendar, final Date date) {
    return new CalendarEventAction(calendar, date, "calendar.action.holiday.add") {
      @Override
      public void actionPerformed(ActionEvent e) {
        Set<CalendarEvent> events = Sets.newLinkedHashSet(myCalendar.getPublicHolidays());
        events.add(CalendarEvent.newEvent(myDate, false, Type.HOLIDAY,
            GanttLanguage.getInstance().formatText("calendar.action.holiday.add.description", date), null));
        myCalendar.setPublicHolidays(events);
      }
    };
  }

  public static CalendarEventAction removeHoliday(GPCalendar calendar, Date date) {
    return new CalendarEventAction(calendar, date, "calendar.action.holiday.remove") {
      @Override
      public void actionPerformed(ActionEvent e) {
        Set<CalendarEvent> events = Sets.newLinkedHashSet(myCalendar.getPublicHolidays());
        events.remove(CalendarEvent.newEvent(myDate, false, Type.HOLIDAY, null, null));
        myCalendar.setPublicHolidays(events);
      }
    };
  }
}
