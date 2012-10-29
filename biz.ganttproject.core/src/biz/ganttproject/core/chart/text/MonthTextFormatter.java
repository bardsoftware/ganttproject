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
package biz.ganttproject.core.chart.text;

import java.text.DateFormat;
import java.util.Date;

import biz.ganttproject.core.chart.text.TimeFormatters.LocaleApi;

public class MonthTextFormatter extends CachingTextFormatter implements TimeFormatter {
  private String myLongPattern;

  private String myMediumPattern;

  private String myShortPattern;

  public MonthTextFormatter(LocaleApi localeApi, String longPattern, String mediumPattern, String shortPattern) {
    myLongPattern = longPattern;
    myMediumPattern = mediumPattern;
    myShortPattern = shortPattern;
    initFormats(localeApi);
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

  private void initFormats(LocaleApi localeApi) {
    myLongFormat = localeApi.createDateFormat(myLongPattern);
    myMediumFormat = localeApi.createDateFormat(myMediumPattern);
    myShortFormat = localeApi.createDateFormat(myShortPattern);
  }

  @Override
  public void setLocale(LocaleApi localeApi) {
    super.setLocale(localeApi);
    initFormats(localeApi);
  }

  private DateFormat myLongFormat;

  private DateFormat myMediumFormat;

  private DateFormat myShortFormat;
}
