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

import biz.ganttproject.core.chart.text.TimeFormatters.LocaleApi;
import kotlin.Unit;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;
import java.util.function.Function;


public class WeekTextFormatter extends CachingTextFormatter implements TimeFormatter {
  private DateFormat myDateFormat;
  private String myWeekText;
  private Function<Date, Integer> myWeekNumbering;

  WeekTextFormatter() {
  }

  @Override
  protected TimeUnitText[] createTimeUnitText(Date startDate) {
    return new TimeUnitText[] { createTopText(startDate), createBottomText(startDate) };
  }

  private TimeUnitText createTopText(Date date) {
    Integer weekNo = myWeekNumbering.apply(date);
    String shortText = weekNo.toString();
    String middleText = MessageFormat.format("{0} {1}", myWeekText, weekNo);
    String longText = middleText;
    return new TimeUnitText(longText, middleText, shortText);
  }

  private TimeUnitText createBottomText(Date date) {
    return new TimeUnitText(myDateFormat.format(date));
  }

  @Override
  public void setLocale(LocaleApi localeApi) {
    super.setLocale(localeApi);
    myDateFormat = localeApi.getShortDateFormat();
    myWeekText = localeApi.i18n("week");
    myWeekNumbering = localeApi.getWeekNumbering().getValue();
    localeApi.getWeekNumbering().addWatcher(evt ->  {
      clearCache();
      myWeekNumbering = evt.getNewValue();
      return Unit.INSTANCE;
    });
  }

  @Override
  public int getTextCount() {
    return 2;
  }
}
