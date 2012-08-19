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
import java.util.Date;

import biz.ganttproject.core.time.TimeUnitText;


public class DayTextFormatter extends CachingTextFormatter implements TimeFormatter {
  // /** cache for holding formatted day names * */
  // private final HashMap<Date, String> textCache = new HashMap<Date,
  // String>();

  @Override
  protected TimeUnitText[] createTimeUnitText(Date adjustedLeft) {
    return new TimeUnitText[] { new TimeUnitText(MessageFormat.format("{0}",
        new Object[] { "" + adjustedLeft.getDate() })) };
  }

  // public TimeUnitText format(TimeUnit timeUnit, Date baseDate) {
  // String result = null;
  // if (timeUnit instanceof DateFrameable) {
  // Date adjustedLeft = ((DateFrameable) timeUnit).adjustLeft(baseDate);
  // result = (String) textCache.get(adjustedLeft);
  // if (result == null) {
  // result = MessageFormat.format("{0}", new Object[] { ""
  // + adjustedLeft.getDate() });
  // textCache.put(adjustedLeft, result);
  // }
  // }
  // return new TimeUnitText(result);
  // }

}
