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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import biz.ganttproject.core.calendar.GPCalendar;
import biz.ganttproject.core.time.CalendarFactory;
import biz.ganttproject.core.time.GanttCalendar;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.DateIntervalListEditor.DateInterval;

/**
 * @author nbohn
 */
public class GanttDialogPublicHoliday {

  private DateIntervalListEditor publicHolidayBean;

  private DateIntervalListEditor.DateIntervalModel publicHolidays;

  public GanttDialogPublicHoliday(IGanttProject project) {
    final Set<DateInterval> recurringHolidays = Sets.newHashSet();
    publicHolidays = new DateIntervalListEditor.DefaultDateIntervalModel() {
      @Override
      public boolean canRemove(DateInterval interval) {
        return super.canRemove(interval) && !recurringHolidays.contains(interval);
      }
    };
    for (GPCalendar.Holiday h : project.getActiveCalendar().getPublicHolidays()) {
      DateInterval interval = DateIntervalListEditor.DateInterval.createFromVisibleDates(h.date, h.date);
      publicHolidays.add(interval);
      if (h.isRepeating) {
        recurringHolidays.add(interval);
      }
    }

    publicHolidayBean = new DateIntervalListEditor(publicHolidays);
  }

  public Component getContentPane() {
    return publicHolidayBean;
  }

  public List<GanttCalendar> getHolidays() {
    List<GanttCalendar> result = new ArrayList<GanttCalendar>();
    for (DateInterval interval : publicHolidays.getIntervals()) {
      result.add(CalendarFactory.createGanttCalendar(interval.start));
    }
    return result;
  }
}
