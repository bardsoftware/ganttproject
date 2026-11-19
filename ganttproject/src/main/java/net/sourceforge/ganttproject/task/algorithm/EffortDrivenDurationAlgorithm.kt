/*
Copyright 2026 BarD Software s.r.o., GanttProject Team

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
package net.sourceforge.ganttproject.task.algorithm

import biz.ganttproject.core.model.task.TaskEffort
import net.sourceforge.ganttproject.task.ResourceAssignment
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Hours per day which the assignment makes available to its task: working hours of the resource
 * scaled by the load of the assignment. This is the only place where the availability of one
 * assignment is computed.
 */
fun availableHoursPerDay(assignment: ResourceAssignment): BigDecimal {
  val hoursPerDay = assignment.resource?.hoursPerDay ?: BigDecimal.valueOf(TaskEffort.DEFAULT_HOURS_PER_DAY)
  // Float.toString gives the load as the user typed it, while BigDecimal(float) would add noise
  // which could push the quotient over a whole number.
  return hoursPerDay.multiply(BigDecimal(assignment.load.toString())).divide(BigDecimal(100))
}

/**
 * Duration in working days of a task with the given effort and assignments, at least one day.
 * Returns null if nothing can be derived: no effort, or no hours available.
 */
fun effortDrivenDurationDays(effort: BigDecimal?, assignments: Array<ResourceAssignment>): Long? {
  if (effort == null || effort.signum() <= 0) {
    return null
  }
  val available = assignments.fold(BigDecimal.ZERO) { sum, assignment -> sum.add(availableHoursPerDay(assignment)) }
  if (available.signum() <= 0) {
    return null
  }
  val days = TaskEffort.toHours(effort).divide(available, 6, RoundingMode.HALF_UP).setScale(0, RoundingMode.CEILING)
  return maxOf(1L, days.toLong())
}

/**
 * Sets the duration of every effort driven task from its effort and assignments.
 * Milestones and supertasks are left alone: the former have no duration, the latter get it from their children.
 */
class EffortDrivenDurationAlgorithm(private val taskManager: TaskManager) : AlgorithmBase() {
  override fun run() {
    if (!isEnabled) {
      return
    }
    for (task in taskManager.tasks) {
      if (!task.isEffortDriven || task.isMilestone || task.isSupertask) {
        continue
      }
      val days = effortDrivenDurationDays(task.effort, task.assignments) ?: continue
      val newDuration = taskManager.createLength(days)
      val current = task.duration
      if (current.timeUnit == newDuration.timeUnit && current.length == newDuration.length) {
        continue
      }
      task.createMutator().also {
        it.setDuration(newDuration)
        it.commit()
      }
    }
  }
}
