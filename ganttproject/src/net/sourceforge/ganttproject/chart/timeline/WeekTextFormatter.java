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
package net.sourceforge.ganttproject.chart.timeline;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;

import biz.ganttproject.core.time.CalendarFactory;

import net.sourceforge.ganttproject.chart.TimeUnitText;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;

public class WeekTextFormatter extends CachingTextFormatter implements TimeFormatter {
  private Calendar myCalendar;

  WeekTextFormatter() {
    myCalendar = CalendarFactory.newCalendar();
  }

  @Override
  protected TimeUnitText[] createTimeUnitText(Date startDate) {
    myCalendar.setTime(startDate);
    myCalendar.setMinimalDaysInFirstWeek(4);
    return new TimeUnitText[] { createTopText(), createBottomText() };
  }

  private TimeUnitText createTopText() {
    Integer weekNo = new Integer(myCalendar.get(Calendar.WEEK_OF_YEAR));
    String shortText = weekNo.toString();
    String middleText = MessageFormat.format("{0} {1}", GanttLanguage.getInstance().getText("week"), weekNo);
    String longText = middleText;
    return new TimeUnitText(longText, middleText, shortText);
  }

  private TimeUnitText createBottomText() {
    return new TimeUnitText(GanttLanguage.getInstance().getShortDateFormat().format(myCalendar.getTime()));
  }

  @Override
  public void languageChanged(Event event) {
    super.languageChanged(event);
    myCalendar = CalendarFactory.newCalendar();
  }

}
