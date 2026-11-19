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
import net.sourceforge.ganttproject.resource.HumanResource
import java.math.BigDecimal

/** Whether the actual effort of the task is recorded per assignment. */
val Task.hasAssignmentActualEffort: Boolean get() = assignments.any { it.actualEffort != null }

/**
 * The effort actually spent on the task, in the unit defined by [TaskEffort], or null if none was recorded.
 * Once an assignment has a value, it is the sum over the assignments and the task's own [Task.getActualEffort] is
 * kept but not used. Without values per assignment, it is the task's own value.
 */
val Task.totalActualEffort: BigDecimal?
  get() =
    if (hasAssignmentActualEffort) assignments.mapNotNull { it.actualEffort }.fold(BigDecimal.ZERO, BigDecimal::add)
    else actualEffort

/** The effort actually spent on the task, in hours, or null if none was recorded. See [totalActualEffort]. */
val Task.actualEffortHours: BigDecimal? get() = totalActualEffort?.let(TaskEffort::toHours)

/** The effort actually spent by the resource on the task, in hours, or null if none was recorded. */
val ResourceAssignment.actualEffortHours: BigDecimal? get() = actualEffort?.let(TaskEffort::toHours)

/**
 * Adds [hours] to the effort actually spent on the task and commits it. The task which has none yet starts from zero.
 * The actual effort is only a record: it does not change the dates or the duration of the task.
 * A task which records its actual effort per assignment does not take hours without a resource.
 */
fun Task.addActualEffortHours(hours: BigDecimal) {
  require(hours.signum() > 0) { "Hours to add must be positive: $hours" }
  check(!hasAssignmentActualEffort) { "Actual effort of task $taskID is recorded per assignment, add hours with a resource" }
  val spent = actualEffort ?: BigDecimal.ZERO
  createMutator().also {
    it.setActualEffort(spent.add(TaskEffort.fromHours(hours)))
    it.commit()
  }
}

/**
 * Adds [hours] to the effort which [resource] actually spent on the task. The resource has to be assigned to the task.
 * Like the other properties of an assignment, the value is changed at once.
 */
fun Task.addActualEffortHours(hours: BigDecimal, resource: HumanResource) {
  require(hours.signum() > 0) { "Hours to add must be positive: $hours" }
  val assignment = requireNotNull(assignmentCollection.getAssignment(resource)) {
    "Resource ${resource.id} is not assigned to task $taskID"
  }
  val spent = assignment.actualEffort ?: BigDecimal.ZERO
  assignment.actualEffort = spent.add(TaskEffort.fromHours(hours))
}
