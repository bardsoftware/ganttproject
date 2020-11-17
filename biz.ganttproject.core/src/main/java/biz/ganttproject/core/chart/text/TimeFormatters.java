/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

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

import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.time.TimeUnit;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import com.google.common.collect.Iterables;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author dbarashev (Dmitry Barashev)
 */
public class TimeFormatters {
  private final Map<String, TimeFormatter> ourUpperFormatters = new HashMap<String, TimeFormatter>();
  private final Map<String, TimeFormatter> ourLowerFormatters = new HashMap<String, TimeFormatter>();
  public static final TimeUnitText[] EMPTY_TEXT = new TimeUnitText[] { new TimeUnitText("") };
  private static final TimeFormatter DEFAULT_TIME_FORMATTER = new TimeFormatter() {
    @Override
    public TimeUnitText[] format(Offset curOffset) {
      return EMPTY_TEXT;
    }

    @Override
    public TimeUnitText[] format(TimeUnit timeUnit, Date baseDate) {
      return EMPTY_TEXT;
    }

    @Override
    public int getTextCount() {
      return 1;
    }
  };

  public TimeFormatters(LocaleApi localeApi) {
    Map<String, TimeFormatter> commonFormatters = new HashMap<String, TimeFormatter>();

    commonFormatters.put(GPTimeUnitStack.DAY.getName(), new DayTextFormatter());
    commonFormatters.put(GPTimeUnitStack.QUARTER.getName(), new QuarterTextFormatter());
    commonFormatters.put(GPTimeUnitStack.YEAR.getName(), new YearTextFormatter());

    ourUpperFormatters.putAll(commonFormatters);
    ourUpperFormatters.put(GPTimeUnitStack.MONTH.getName(), new MonthTextFormatter(localeApi, "MMMM yyyy", "MMM''yyyy", "MM''yy"));
    ourUpperFormatters.put(GPTimeUnitStack.WEEK.getName(), new WeekTextFormatter());

    ourLowerFormatters.putAll(commonFormatters);
    ourLowerFormatters.put(GPTimeUnitStack.MONTH.getName(), new MonthTextFormatter(localeApi, "MMMM", "MMM", "MM"));
    ourLowerFormatters.put(GPTimeUnitStack.WEEK.getName(), new WeekTextFormatter());
    setLocaleApi(localeApi);
  }

  public TimeFormatter getFormatter(TimeUnit timeUnit, TimeUnitText.Position position) {
    TimeFormatter result = DEFAULT_TIME_FORMATTER;
    switch (position) {
    case UPPER_LINE:
      result = ourUpperFormatters.get(timeUnit.getName());
      break;
    case LOWER_LINE:
      result = ourLowerFormatters.get(timeUnit.getName());
      break;
    }
    return result;
  }

  public static interface LocaleApi {
    DateFormat getShortDateFormat();
    DateFormat createDateFormat(String pattern);
    Locale getLocale();
    String i18n(String key);
  }

  public void setLocaleApi(LocaleApi localeApi) {
    for (TimeFormatter tf : Iterables.concat(ourUpperFormatters.values(), ourLowerFormatters.values())) {
      if (tf instanceof CachingTextFormatter) {
        ((CachingTextFormatter)tf).setLocale(localeApi);
      }
    }
  }
}
