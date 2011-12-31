/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.chart.timeline;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.chart.timeline.TimeFormatters.Position;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitText;
import net.sourceforge.ganttproject.time.gregorian.GPTimeUnitStack;

/**
 *
 * @author dbarashev (Dmitry Barashev)
 *
 */
public class TimeFormatters {
    private static GanttLanguage i18n = GanttLanguage.getInstance();
    public static enum Position {
        UPPER_LINE, LOWER_LINE
    }
    private static final Map<String, TimeFormatter> ourUpperFormatters = new HashMap<String, TimeFormatter>();
    private static final Map<String, TimeFormatter> ourLowerFormatters = new HashMap<String, TimeFormatter>();
    protected static final TimeUnitText[] EMPTY_TEXT = new TimeUnitText[] {new TimeUnitText("")};
    private static final TimeFormatter DEFAULT_TIME_FORMATTER = new TimeFormatter() {
        @Override
        public TimeUnitText[] format(TimeUnit timeUnit, Date baseDate) {
            return EMPTY_TEXT;
        }
    };
    static {
        Map<String, TimeFormatter> commonFormatters = new HashMap<String, TimeFormatter>();

        commonFormatters.put(GPTimeUnitStack.DAY.getName(), new DayTextFormatter());
        commonFormatters.put(GPTimeUnitStack.QUARTER.getName(), new QuarterTextFormatter());
        commonFormatters.put(GPTimeUnitStack.YEAR.getName(), new YearTextFormatter());

        ourUpperFormatters.putAll(commonFormatters);
        ourUpperFormatters.put(GPTimeUnitStack.MONTH.getName(), new MonthTextFormatter("MMMM yyyy", "MMM''yyyy", "MM''yy"));
        ourUpperFormatters.put(GPTimeUnitStack.WEEK.getName(), new WeekTextFormatter());

        ourLowerFormatters.putAll(commonFormatters);
        ourLowerFormatters.put(GPTimeUnitStack.MONTH.getName(), new MonthTextFormatter("MMMM", "MMM", "MM"));
        ourLowerFormatters.put(GPTimeUnitStack.WEEK.getName(), new WeekTextFormatter());
    }

    public static TimeFormatter getFormatter(TimeUnit timeUnit, Position position) {
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
}
