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
import java.util.Iterator;
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
 * Action which adds a weekend exception from the chart UI
 *
 * @author dbarashev (Dmitry Barashev)
 */
public abstract class WeekendExceptionAction extends GPAction {
  protected final GPCalendar myCalendar;
  protected final Date myDate;
  public WeekendExceptionAction(GPCalendar calendar, Date date, String id) {
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


  public static WeekendExceptionAction addException(GPCalendar calendar, Date date) {
    return new WeekendExceptionAction(calendar, date, "calendar.action.weekendException.add") {
      public void actionPerformed(ActionEvent e) {
        Set<CalendarEvent> events = Sets.newLinkedHashSet(myCalendar.getPublicHolidays());
        events.add(CalendarEvent.newEvent(
            myDate, false, Type.WORKING_DAY, GanttLanguage.getInstance().getText("calendar.action.weekendException.add.description")));
        myCalendar.setPublicHolidays(events);
      }
    };
  }

  public static WeekendExceptionAction removeException(GPCalendar calendar, Date date) {
    return new WeekendExceptionAction(calendar, date, "calendar.action.weekendException.remove") {
      public void actionPerformed(ActionEvent e) {
        Set<CalendarEvent> events = Sets.newLinkedHashSet(myCalendar.getPublicHolidays());
        events.remove(CalendarEvent.newEvent(myDate, false, Type.WORKING_DAY, null));
        myCalendar.setPublicHolidays(events);
      }
    };
  }
}
