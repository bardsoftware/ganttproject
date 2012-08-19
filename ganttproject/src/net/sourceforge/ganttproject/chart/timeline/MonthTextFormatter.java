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

import java.text.SimpleDateFormat;
import java.util.Date;

import net.sourceforge.ganttproject.chart.TimeUnitText;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;

public class MonthTextFormatter extends CachingTextFormatter implements TimeFormatter {
  private String myLongPattern;

  private String myMediumPattern;

  private String myShortPattern;

  public MonthTextFormatter(String longPattern, String mediumPattern, String shortPattern) {
    myLongPattern = longPattern;
    myMediumPattern = mediumPattern;
    myShortPattern = shortPattern;
    initFormats();
  }

  @Override
  protected TimeUnitText[] createTimeUnitText(Date adjustedLeft) {
    TimeUnitText result;
    String longText = myLongFormat.format(adjustedLeft);
    String mediumText = myMediumFormat.format(adjustedLeft);
    String shortText = myShortFormat.format(adjustedLeft);
    result = new TimeUnitText(longText, mediumText, shortText);
    return new TimeUnitText[] { result };
  }

  private void initFormats() {
    myLongFormat = GanttLanguage.getInstance().createDateFormat(myLongPattern);
    myMediumFormat = GanttLanguage.getInstance().createDateFormat(myMediumPattern);
    myShortFormat = GanttLanguage.getInstance().createDateFormat(myShortPattern);
  }

  @Override
  public void languageChanged(Event event) {
    super.languageChanged(event);
    initFormats();
  }

  private SimpleDateFormat myLongFormat;

  private SimpleDateFormat myMediumFormat;

  private SimpleDateFormat myShortFormat;
}
