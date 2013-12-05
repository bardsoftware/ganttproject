/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.gui;

import java.awt.Component;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import biz.ganttproject.core.calendar.CalendarEvent;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.DateIntervalListEditor.DateInterval;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * @author nbohn
 */
public class GanttDialogPublicHoliday {

  private final DateIntervalListEditor myIntervalEditor;

  private final DateIntervalListEditor.DateIntervalModel myIntervalModel;

  private final Set<DateInterval> myRecurringHolidays = Sets.newHashSet();

  public GanttDialogPublicHoliday(IGanttProject project) {
    myIntervalModel = new DateIntervalListEditor.DefaultDateIntervalModel() {
      @Override
      public boolean canRemove(DateInterval interval) {
        return super.canRemove(interval) && !myRecurringHolidays.contains(interval);
      }

      @Override
      public String format(DateInterval interval) {
        StringBuffer buffer = new StringBuffer();
        FieldPosition posYear = new FieldPosition(DateFormat.YEAR_FIELD);
        GanttLanguage.getInstance().getDateFormat().format(interval.start, buffer, posYear);
        if (myRecurringHolidays.contains(interval)) {
          buffer.replace(posYear.getBeginIndex(), posYear.getEndIndex(), "--");
          return GanttLanguage.getInstance().formatText("holiday.list.item.recurring", buffer.toString());
        }
        return buffer.toString();
      }

    };
    for (CalendarEvent h : project.getActiveCalendar().getPublicHolidays()) {
      DateInterval interval = DateIntervalListEditor.DateInterval.createFromVisibleDates(h.myDate, h.myDate);
      myIntervalModel.add(interval);
      if (h.isRecurring) {
        myRecurringHolidays.add(interval);
      }
    }

    myIntervalEditor = new DateIntervalListEditor(myIntervalModel);
  }

  public Component getContentPane() {
    return myIntervalEditor;
  }

  public List<CalendarEvent> getHolidays() {
    List<CalendarEvent> result = Lists.newArrayList();
    for (DateInterval interval : myIntervalModel.getIntervals()) {
      boolean isRecurring = myRecurringHolidays.contains(interval);
      result.add(CalendarEvent.newEvent(interval.start, isRecurring, CalendarEvent.Type.HOLIDAY, null));
    }
    return result;
  }
}
