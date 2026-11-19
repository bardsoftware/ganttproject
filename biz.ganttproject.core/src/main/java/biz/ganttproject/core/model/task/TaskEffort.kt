/*
Copyright 2026 BarD Software s.r.o., GanttProject Cloud OU

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
package biz.ganttproject.core.model.task

import java.math.BigDecimal

/**
 * The unit in which task effort is stored and written to the project file is decided here and nowhere else:
 * the effort value is a decimal number of hours, and this is the name of its attribute in the .gan file.
 */
object TaskEffort {
  const val XML_ATTRIBUTE = "effort-hours"

  /** Attribute of the effort actually spent on a task, in the same unit as [XML_ATTRIBUTE]. */
  const val ACTUAL_XML_ATTRIBUTE = "actual-effort-hours"

  /** Hours per day assumed for a resource which has none set. */
  const val DEFAULT_HOURS_PER_DAY = 8.0

  /** Converts a stored effort value to hours. The only place which knows the unit of the stored value. */
  fun toHours(effort: BigDecimal): BigDecimal = effort

  /** Converts hours to the stored unit. The counterpart of [toHours]. */
  fun fromHours(hours: BigDecimal): BigDecimal = hours
}
