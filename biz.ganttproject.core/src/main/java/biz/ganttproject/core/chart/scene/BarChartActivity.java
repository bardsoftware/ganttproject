/*
Copyright 2012 GanttProject Team

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
package biz.ganttproject.core.chart.scene;

import java.util.Date;

import biz.ganttproject.core.time.TimeDuration;

/**
 * Represents an abstract activity on a bar chart defined by a start date and duration. 
 * Activity belongs to an owner object, possibly with other activities.
 * 
 * @param <T> model object type 
 */
public interface BarChartActivity<T> {
  Date getStart();

  Date getEnd();

  TimeDuration getDuration();

  T getOwner();
}