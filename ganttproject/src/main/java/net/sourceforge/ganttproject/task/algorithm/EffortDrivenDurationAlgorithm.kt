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
import biz.ganttproject.core.model.task.CapacityChange
import biz.ganttproject.core.model.task.CapacitySchedule
import biz.ganttproject.core.model.task.TaskEffort
import biz.ganttproject.task.recurrence.toModelLocalDate
import biz.ganttproject.core.model.task.daysNeeded
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.ResourceAssignment
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

private val HUNDRED = BigDecimal(100)

/**
 * Hours per day which the assignment makes available to its task on [day], or without a [day] before the first change
 * of the working hours: working hours of the resource scaled by its utilization and by the load of the assignment.
 * This is the only place where the availability of one assignment is computed.
 */
fun availableHoursPerDay(assignment: ResourceAssignment, day: LocalDate? = null): BigDecimal {
  val resource = assignment.resource
  val hoursPerDay = resource?.hoursPerDay ?: BigDecimal.valueOf(TaskEffort.DEFAULT_HOURS_PER_DAY)
  val hours = if (day == null || resource == null) hoursPerDay else CapacitySchedule(hoursPerDay, resource.hoursSchedule).hoursOn(day)
  val utilization = BigDecimal((resource?.utilization ?: 100).coerceAtLeast(0))
  // Float.toString gives the load as the user typed it, while BigDecimal(float) would add noise
  // which could push the quotient over a whole number.
  return hours.multiply(utilization).divide(HUNDRED).multiply(BigDecimal(assignment.load.toString())).divide(HUNDRED)
}

/** Hours per day which the assignments of the task make available to it, as they change over time. */
fun Task.capacitySchedule(): CapacitySchedule = capacitySchedule(assignments)

fun capacitySchedule(assignments: Array<ResourceAssignment>): CapacitySchedule {
  val base = assignments.fold(BigDecimal.ZERO) { sum, assignment -> sum.add(availableHoursPerDay(assignment)) }
  val changeDays = assignments.flatMap { it.resource?.hoursSchedule.orEmpty() }.map { it.from }.distinct()
  return CapacitySchedule(base, changeDays.map { day ->
    CapacityChange(day, assignments.fold(BigDecimal.ZERO) { sum, assignment -> sum.add(availableHoursPerDay(assignment, day)) })
  })
}

/**
 * The days off of the resource as ranges which include the first day and exclude the last one. This is how the
 * interval of a day off is stored: the person dialog and the file import both write the day after the last day off as
 * the end of the interval.
 */
fun HumanResource.daysOffRanges(): List<Pair<LocalDate, LocalDate>> =
  daysOff.map { it.start.time.toModelLocalDate() to it.finish.time.toModelLocalDate() }

internal fun List<Pair<LocalDate, LocalDate>>.covers(day: LocalDate): Boolean =
  any { (from, toExclusive) -> !day.isBefore(from) && day.isBefore(toExclusive) }

private fun GPCalendar.isWorkingDay(day: LocalDate): Boolean =
  getDayMask(Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant())) and GPCalendar.DayMask.WORKING != 0

/**
 * Duration in working days of a task with the given effort and assignments, at least one day.
 * Returns null if nothing can be derived: no effort, or no hours available.
 * Without a start or when the working hours do not change, the hours of one day are divided; otherwise the days from
 * [start] on are counted one by one, with the working days of [calendar]. A day off of a resource still belongs to
 * the task, but the resource contributes no hours on it: five days of work with one day off in the middle make six
 * days. Days off are only taken into account when there is a [start] to count from.
 */
fun effortDrivenDurationDays(effort: BigDecimal?, assignments: Array<ResourceAssignment>,
                             start: LocalDate? = null, calendar: GPCalendar? = null): Long? {
  if (effort == null || effort.signum() <= 0) {
    return null
  }
  val schedule = capacitySchedule(assignments)
  val daysOff = assignments.map { it.resource?.daysOffRanges().orEmpty() }
  if (start != null && daysOff.any { it.isNotEmpty() }) {
    // No resource brings any hours on any day, so counting days would only run into the limit.
    if (schedule.base.signum() <= 0 && schedule.changes.all { it.hoursPerDay.signum() <= 0 }) {
      return null
    }
    val hoursOn = { day: LocalDate ->
      assignments.indices.fold(BigDecimal.ZERO) { sum, i ->
        if (daysOff[i].covers(day)) sum else sum.add(availableHoursPerDay(assignments[i], day))
      }
    }
    return daysNeeded(TaskEffort.toHours(effort), start, hoursOn) { calendar?.isWorkingDay(it) ?: true }?.toLong()
  }
  if (schedule.isConstant || start == null) {
    if (schedule.base.signum() <= 0) {
      return null
    }
    val days = TaskEffort.toHours(effort).divide(schedule.base, 6, RoundingMode.HALF_UP).setScale(0, RoundingMode.CEILING)
    return maxOf(1L, days.toLong())
  }
  return daysNeeded(TaskEffort.toHours(effort), start, schedule) { calendar?.isWorkingDay(it) ?: true }?.toLong()
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
      val days = effortDrivenDurationDays(task.effort, task.assignments, task.start.toLocalDate(), taskManager.calendar) ?: continue
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
