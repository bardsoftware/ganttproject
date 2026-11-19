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

import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.model.task.CapacitySchedule
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.math.BigDecimal
import java.time.LocalDate

/** Why a task gets no effort from its duration. */
enum class BackfillSkip {
  /** A supertask gets its duration from its children. */
  SUPERTASK,
  /** A milestone has no duration. */
  MILESTONE,
  /** An effort which is already set is never overwritten. */
  HAS_EFFORT,
  /** Without assignments no hours are available, so there is nothing to multiply the duration with. */
  NO_ASSIGNMENT,
  /** The assigned resources have no hours on the days of the task. */
  NO_HOURS,
  /** The effort would give a different duration back, e.g. when the last days of the task have no hours. */
  DURATION_WOULD_CHANGE
}

data class BackfillProposal(val effort: Map<Task, BigDecimal>, val skipped: Map<Task, BackfillSkip>)

/**
 * Effort of [task] which is its duration multiplied by the hours its assignments make available on each of its
 * working days, as [effortDrivenDurationDays] computes them. Returns null if no hours are available.
 */
fun backfillEffort(task: Task, calendar: GPCalendar?): BigDecimal? {
  val schedule = task.capacitySchedule()
  val days = task.duration.length
  val start = task.start.toLocalDate()
  val effort = if (schedule.isConstant) {
    schedule.base.max(BigDecimal.ZERO).multiply(BigDecimal(days))
  } else {
    sumOfHours(schedule, start, days) { calendar?.isWorkingDay(it) ?: true }
  }
  // Without the scale fix 20 would become 2E+1, also in the project file.
  return effort.takeIf { it.signum() > 0 }?.stripTrailingZeros()?.let { if (it.scale() < 0) it.setScale(0) else it }
}

private fun sumOfHours(schedule: CapacitySchedule, start: LocalDate, days: Int,
                       isWorkingDay: (LocalDate) -> Boolean): BigDecimal {
  var sum = BigDecimal.ZERO
  var day = start
  var counted = 0
  while (counted < days) {
    if (isWorkingDay(day)) {
      sum = sum.add(schedule.hoursOn(day).max(BigDecimal.ZERO))
      counted++
    }
    day = day.plusDays(1)
  }
  return sum
}

/**
 * Proposes an effort for each of [tasks] which has none, such that the effort driven duration is exactly the current
 * duration: setting the effort does not move anything.
 */
fun proposeEffortBackfill(tasks: Collection<Task>, calendar: GPCalendar?): BackfillProposal {
  val effort = linkedMapOf<Task, BigDecimal>()
  val skipped = linkedMapOf<Task, BackfillSkip>()
  for (task in tasks) {
    val reason = when {
      task.isSupertask -> BackfillSkip.SUPERTASK
      task.isMilestone -> BackfillSkip.MILESTONE
      task.effort != null -> BackfillSkip.HAS_EFFORT
      task.assignments.isEmpty() -> BackfillSkip.NO_ASSIGNMENT
      else -> null
    }
    if (reason != null) {
      skipped[task] = reason
      continue
    }
    val hours = backfillEffort(task, calendar)
    when {
      hours == null -> skipped[task] = BackfillSkip.NO_HOURS
      effortDrivenDurationDays(hours, task.assignments, task.start.toLocalDate(), calendar) != task.duration.length.toLong() ->
        skipped[task] = BackfillSkip.DURATION_WOULD_CHANGE
      else -> effort[task] = hours
    }
  }
  return BackfillProposal(effort, skipped)
}

/**
 * Sets the proposed effort and makes the tasks effort driven. The scheduler waits until all tasks are written.
 * Callers wrap this into one undoable edit.
 */
fun applyEffortBackfill(proposal: BackfillProposal, taskManager: TaskManager) {
  val scheduler = taskManager.algorithmCollection.scheduler
  val wasEnabled = scheduler.isEnabled
  scheduler.isEnabled = false
  try {
    proposal.effort.forEach { (task, hours) ->
      task.createMutator().also {
        it.setEffort(hours)
        it.setEffortDriven(true)
        it.commit()
      }
    }
  } finally {
    scheduler.isEnabled = wasEnabled
  }
  scheduler.run()
}

/** Writes the proposal as one undoable edit. */
fun applyEffortBackfill(proposal: BackfillProposal, taskManager: TaskManager, undoManager: GPUndoManager, editName: String) =
  undoManager.undoableEdit(editName) { applyEffortBackfill(proposal, taskManager) }

/** The tasks to fill: the selected ones with all their subtasks, or all tasks if nothing is selected. */
fun tasksForBackfill(selection: Collection<Task>, taskManager: TaskManager): List<Task> =
  if (selection.isEmpty()) {
    taskManager.tasks.toList()
  } else {
    selection.flatMap { listOf(it) + taskManager.taskHierarchy.getDeepNestedTasks(it) }.distinct()
  }

sealed class BackfillPreparation {
  /** Some resources have values from which no effort is derived. Nothing is proposed. */
  data class Problems(val problems: Map<HumanResource, List<CapacityProblem>>) : BackfillPreparation()
  data class Ready(val proposal: BackfillProposal) : BackfillPreparation()
}

/** Checks the resources assigned to the tasks which may get an effort, and proposes the effort if they are fine. */
fun prepareEffortBackfill(tasks: Collection<Task>, calendar: GPCalendar?): BackfillPreparation {
  val resources = tasks.filter { !it.isSupertask && !it.isMilestone && it.effort == null }
    .flatMap { task -> task.assignments.mapNotNull { it.resource } }.distinct()
  val problems = capacityProblems(resources)
  return if (problems.isNotEmpty()) BackfillPreparation.Problems(problems)
    else BackfillPreparation.Ready(proposeEffortBackfill(tasks, calendar))
}

/** A value of a resource which the resource table does not accept, typically read from a file. */
enum class CapacityProblem { HOURS_PER_DAY, UTILIZATION, HOURS_CHANGE }

/**
 * Values of [resources] which would make the derived effort plausibly wrong: hours per day not above 0 or above
 * [CapacitySchedule.MAX_HOURS_PER_DAY], utilization outside 1..100, a change of hours outside 0..24.
 * The duration algorithm computes with such values as they are; deriving an effort from them is refused instead.
 */
fun capacityProblems(resources: Collection<HumanResource>): Map<HumanResource, List<CapacityProblem>> =
  resources.associateWith { resource ->
    buildList {
      resource.hoursPerDay?.let {
        if (it.signum() <= 0 || it > CapacitySchedule.MAX_HOURS_PER_DAY) add(CapacityProblem.HOURS_PER_DAY)
      }
      resource.utilization?.let {
        if (it !in 1..100) add(CapacityProblem.UTILIZATION)
      }
      if (resource.hoursSchedule.any { it.hoursPerDay.signum() < 0 || it.hoursPerDay > CapacitySchedule.MAX_HOURS_PER_DAY }) {
        add(CapacityProblem.HOURS_CHANGE)
      }
    }
  }.filterValues { it.isNotEmpty() }
