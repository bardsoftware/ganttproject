/*
Copyright (C) 2004-2012 GanttProject Team

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

import biz.ganttproject.core.chart.grid.Offset;
import biz.ganttproject.core.time.TimeUnit;

import java.util.Date;

public interface TimeFormatter {
  TimeUnitText[] format(Offset curOffset);
  TimeUnitText[] format(TimeUnit timeUnit, Date baseDate);

  int getTextCount();
}
