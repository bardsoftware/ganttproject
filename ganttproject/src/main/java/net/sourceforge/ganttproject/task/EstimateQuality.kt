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
package net.sourceforge.ganttproject.task

import biz.ganttproject.core.model.task.TaskEffort
import java.math.BigDecimal
import java.math.MathContext

/** The effort the task was first estimated with, in hours, or null if none was kept. */
val Task.originalEffortHours: BigDecimal? get() = originalEffort?.let(TaskEffort::toHours)

/**
 * How the first estimate of a task compares with the effort it takes: the original effort, the current planned effort
 * and the effort actually spent, all in hours.
 */
data class EstimateQuality(
  val originalHours: BigDecimal,
  val currentHours: BigDecimal?,
  val actualHours: BigDecimal?
) {
  /** Actual effort divided by the original one: 1 means the estimate was right. Null if it cannot be told. */
  val factor: BigDecimal?
    get() = if (actualHours == null || originalHours.signum() == 0) null
      else actualHours.divide(originalHours, MathContext.DECIMAL32).stripTrailingZeros()

  /** Hours spent beyond the original estimate, negative if less was spent. Null if no effort was recorded. */
  val extraHours: BigDecimal? get() = actualHours?.subtract(originalHours)

  /** Whether the planned effort was changed after the first estimate. */
  val isReplanned: Boolean get() = currentHours != null && currentHours.compareTo(originalHours) != 0
}

/**
 * The estimate quality of the task, or null if no original effort was kept or the task is a summary task: the effort
 * of a summary task is not its own and would be counted twice.
 */
val Task.estimateQuality: EstimateQuality?
  get() {
    val original = originalEffortHours ?: return null
    if (manager.taskHierarchy.hasNestedTasks(this)) return null
    return EstimateQuality(original, effort?.let(TaskEffort::toHours), actualEffortHours)
  }
