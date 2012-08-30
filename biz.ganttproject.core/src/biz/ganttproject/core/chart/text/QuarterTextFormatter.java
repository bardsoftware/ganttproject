/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2011 Alexandre Thomas, Dmitry Barashev, GanttProject team

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
package biz.ganttproject.core.chart.text;

import java.util.Calendar;
import java.util.Date;

import biz.ganttproject.core.time.CalendarFactory;


public class QuarterTextFormatter extends CachingTextFormatter implements TimeFormatter {
  private final Calendar myCalendar;

  QuarterTextFormatter() {
    myCalendar = CalendarFactory.newCalendar();
  }

  @Override
  protected TimeUnitText[] createTimeUnitText(Date startDate) {
    myCalendar.setTime(startDate);
    int month = myCalendar.get(Calendar.MONTH);
    int quarter = month / 4 + 1;
    String shortText = "Q" + quarter;
    return new TimeUnitText[] { new TimeUnitText(shortText) };
  }
}
