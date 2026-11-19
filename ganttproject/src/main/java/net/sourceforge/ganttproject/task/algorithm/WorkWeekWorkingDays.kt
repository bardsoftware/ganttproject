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
import biz.ganttproject.core.model.task.WorkWeekSchedule
import biz.ganttproject.core.model.task.nextWorkingDay
import biz.ganttproject.core.model.task.taskWorksOn
import biz.ganttproject.core.model.task.workingDays
import biz.ganttproject.core.model.task.workingDaysBetween
import biz.ganttproject.task.recurrence.toModelDate
import net.sourceforge.ganttproject.task.ResourceAssignment
import net.sourceforge.ganttproject.task.Task
import java.time.LocalDate

/**
 * Whether the working week of the resource of this assignment decides on the days of its task: the resource brings
 * hours to the effort or has to be present, and the assignment has a load. An assignment which is neither, or has no
 * load, is not involved in the task.
 */
val ResourceAssignment.isInvolvedInWorkWeek: Boolean
  get() = load > 0 && resource != null && (contributesEffort || isBlocking)

/**
 * The working days of each task, from the project calendar and the working weeks of the resources involved in the
 * task, see [taskWorksOn]. Only effort driven tasks follow the working weeks; any other task has a duration in working
 * days of the project calendar and keeps it. A task whose involved resources have no working week of their own gets
 * [projectOnly] itself, so nothing changes for a plan without working weeks.
 *
 * The working weeks are read when a task is asked for the first time, so one instance serves one run of a calculation.
 */
class WorkWeekWorkingDays(private val calendar: GPCalendar) {
  /** The working days of the project calendar. */
  val projectOnly: (LocalDate) -> Boolean = workingDayTest(calendar)

  private val perTask = mutableMapOf<Int, (LocalDate) -> Boolean>()

  fun forTask(task: Task): (LocalDate) -> Boolean =
    if (!task.isEffortDriven || task.isMilestone) projectOnly else perTask.getOrPut(task.taskID) { forAssignments(task.assignments) }

  /** The working days of an effort driven task with the given assignments. */
  fun forAssignments(assignments: Array<ResourceAssignment>): (LocalDate) -> Boolean {
    val involved = assignments.filter { it.isInvolvedInWorkWeek }.map { WorkWeekSchedule(it.resource.workWeek) }
    if (involved.all { it.isEmpty }) {
      return projectOnly
    }
    return { day ->
      val mask = calendar.getDayMask(day.toModelDate())
      taskWorksOn(day, mask and GPCalendar.DayMask.WORKING != 0, involved)
    }
  }

  /**
   * The duration in working days of the project calendar of a task which starts on [start] and needs [days] of its
   * own working days, as given by [isWorkingDay]. The task model counts durations with the project calendar, so a
   * task whose resources do not work on every working day of the project is longer there, at least one day.
   */
  fun projectDays(start: LocalDate, days: Long, isWorkingDay: (LocalDate) -> Boolean): Long {
    if (isWorkingDay === projectOnly) {
      return days
    }
    val end = workingDays(start, days.toInt(), isWorkingDay).last().plusDays(1)
    return maxOf(1, workingDaysBetween(start, end, projectOnly)).toLong()
  }

  /**
   * The first working day of the project after the [days] working days of a task from [start] on, which is where a
   * successor of the task can start.
   */
  fun endAfter(start: LocalDate, days: Int, isWorkingDay: (LocalDate) -> Boolean): LocalDate =
    nextWorkingDay(workingDays(start, days, isWorkingDay).last().plusDays(1), projectOnly)

  /**
   * The end date of the task model for a task which needs [days] of its own working days from [start] on: the day
   * after its last working day, as for a new task with the same days. A task which ends on a Friday ends on Saturday
   * there, not on the next Monday, which would show Sunday as its last day.
   */
  fun modelEnd(start: LocalDate, days: Int, isWorkingDay: (LocalDate) -> Boolean): LocalDate =
    workingDays(start, days, isWorkingDay).last().plusDays(1)

  /** The inverse of [projectDays]: the working days of the task between [start] and [end], at least one. */
  fun taskDays(start: LocalDate, end: LocalDate, isWorkingDay: (LocalDate) -> Boolean): Int =
    maxOf(1, workingDaysBetween(start, end, isWorkingDay))
}

/**
 * Duration in working days of the project calendar of an effort driven task with the given effort and assignments
 * which starts on [start]. The days are counted with the working days of the task, see [WorkWeekWorkingDays].
 */
fun effortDrivenProjectDays(effort: java.math.BigDecimal?, assignments: Array<ResourceAssignment>, start: LocalDate,
                            calendar: GPCalendar, workingDays: WorkWeekWorkingDays = WorkWeekWorkingDays(calendar),
                            requiresPresence: Boolean = false): Long? {
  val isWorkingDay = workingDays.forAssignments(assignments)
  val days = effortDrivenDurationDays(effort, effortInputs(assignments, requiresPresence), start, isWorkingDay) ?: return null
  return workingDays.projectDays(start, days, isWorkingDay)
}
