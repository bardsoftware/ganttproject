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
package net.sourceforge.ganttproject.task.algorithm

import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.model.task.LevelTask
import biz.ganttproject.core.model.task.SHARED_POOL
import biz.ganttproject.core.model.task.SHARED_POOL_LOAD
import biz.ganttproject.core.model.task.endAfterWorkingDays
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.task.recurrence.toModelDate
import biz.ganttproject.task.recurrence.toModelLocalDate
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskImpl
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.time.LocalDate

/*
 * Connects the task model to the levelling calculation in biz.ganttproject.core.model.task.
 */

/** The working days of [calendar] as a function. */
fun workingDayTest(calendar: GPCalendar): (LocalDate) -> Boolean = { day ->
  calendar.getDayMask(day.toModelDate()) and GPCalendar.DayMask.WORKING != 0
}

/**
 * Leaf tasks of the project in the form levelling needs them, in the order of the plan.
 *
 * Supertasks get their dates from their children, so only leaves are levelled; a dependency on a supertask becomes a
 * dependency on all leaves under it. Work which has started stays where it is, and so does a task with a fixed date.
 * A task which started work depends on has happened already and stays as well, even if it is not marked as done;
 * otherwise the scheduler would push the started work after it.
 * A task which has not started and starts before [today] is moved if [moveUnstartedPast] is true and stays otherwise.
 */
fun collectLevelTasks(taskManager: TaskManager, today: LocalDate, moveUnstartedPast: Boolean = true): List<LevelTask> {
  val hierarchy = taskManager.taskHierarchy
  val leavesUnder = mutableMapOf<Int, List<Int>>()
  fun collectLeaves(task: Task): List<Int> {
    val nested = hierarchy.getNestedTasks(task)
    val leaves = if (nested.isEmpty()) listOf(task.taskID) else nested.flatMap { collectLeaves(it) }
    leavesUnder[task.taskID] = leaves
    return leaves
  }
  val leaves = hierarchy.getNestedTasks(hierarchy.rootTask).flatMap { collectLeaves(it) }
  val started = leaves.filter { taskManager.getTask(it).completionPercentage > 0 }.toMutableSet()
  do {
    val count = started.size
    leavesUnder.forEach { (id, under) ->
      val successorStarted = taskManager.getTask(id).dependenciesAsDependee.toArray().any { dependency ->
        (leavesUnder[dependency.dependant.taskID] ?: listOf(dependency.dependant.taskID)).any { it in started }
      }
      if (successorStarted) started.addAll(under)
    }
  } while (started.size > count)
  return leaves.mapIndexed { order, id ->
    taskManager.getTask(id).toLevelTask(order, leavesUnder, id in started, today, moveUnstartedPast)
  }
}

private fun Task.toLevelTask(
  order: Int, leavesUnder: Map<Int, List<Int>>, started: Boolean, today: LocalDate, moveUnstartedPast: Boolean
): LevelTask {
  // Every resource books its own load; two assignments of one resource add up. A negative load books nothing.
  val loadByResource = assignments
    .filter { it.resource != null }
    .groupBy({ it.resource.id.toString() }, { it.load.toDouble() })
    .mapValues { (_, loads) -> loads.sum().toInt().coerceAtLeast(0) }
  // A milestone or a waiting task takes no capacity. A task without resources takes the shared pool, while an
  // assignment with load zero takes nothing.
  val loads = if (isMilestone || isWaitOnly) {
    loadByResource.mapValues { 0 }.ifEmpty { mapOf(SHARED_POOL to 0) }
  } else {
    loadByResource.ifEmpty { mapOf(SHARED_POOL to SHARED_POOL_LOAD) }
  }
  val start = this.start.time.toModelLocalDate()
  val frozen = started || (start < today && !moveUnstartedPast)
  return LevelTask(
    id = taskID.toString(),
    orderInPlan = order,
    priority = priority.ordinal,
    durationDays = duration.length.coerceAtLeast(1),
    predecessors = dependenciesAsDependant.toArray()
      .flatMap { leavesUnder[it.dependee.taskID] ?: listOf(it.dependee.taskID) }
      .distinct().map { it.toString() },
    fixedStart = if (frozen || isDateFixed) start else null,
    earliestStart = if (thirdDateConstraint == TaskImpl.EARLIESTBEGIN) third?.time?.toModelLocalDate() else null,
    loads = loads,
    frozen = frozen,
    deadline = deadline?.time?.toModelLocalDate(),
    // A blocking resource has to be present whatever its load, also on a milestone.
    blocking = assignments.filter { it.resource != null && it.isBlocking }.map { it.resource.id.toString() }.toSet()
  )
}

/**
 * Whether a task may start on the given day. An effort driven task does not start on a day off of any of its
 * resources, it starts when all of them are back; a day off later in the task lengthens it instead. An assignment
 * without load or marked as no effort brings no hours, so its days off do not hold the task back. Every other task
 * may start on any day.
 */
fun startAllowed(taskManager: TaskManager): (LevelTask, LocalDate) -> Boolean = { levelTask, start ->
  val task = taskManager.getTask(levelTask.id.toInt())
  if (task == null || !task.isEffortDriven || task.isMilestone) {
    true
  } else {
    task.assignments.filter { it.load > 0 && it.contributesEffort }
      .none { it.resource?.daysOffRanges()?.covers(start) == true }
  }
}

/**
 * Whether a resource, by its id as in [LevelTask.blocking], is present on the given day, that is, not on a day off.
 * The days off are read once, when the function is made, so one function serves one levelling run.
 */
fun resourcePresence(taskManager: TaskManager): (String, LocalDate) -> Boolean {
  val daysOff = taskManager.tasks.flatMap { it.assignments.toList() }.mapNotNull { it.resource }.distinct()
    .associate { it.id.toString() to it.daysOffRanges() }
  return { resourceId, day -> daysOff[resourceId]?.covers(day) != true }
}

/**
 * The duration of a task if it starts on the given day. An effort driven task is derived again, because its duration
 * depends on the start when the working hours of its resources change; every other task keeps its duration.
 */
fun durationAtStart(taskManager: TaskManager): (LevelTask, LocalDate) -> Int = { levelTask, start ->
  val task = taskManager.getTask(levelTask.id.toInt())
  if (levelTask.frozen || task == null || !task.isEffortDriven || task.isMilestone) {
    levelTask.durationDays
  } else {
    effortDrivenDurationDays(task.effort, task.assignments, start, taskManager.calendar)?.toInt() ?: levelTask.durationDays
  }
}

/**
 * Effort driven tasks whose duration cannot be derived because their resources make no hours available, such as at a
 * utilization of zero. Levelling places them with their stored duration, so they are worth a warning.
 */
fun capacityProblems(taskManager: TaskManager): List<Task> = taskManager.tasks.filter { task ->
  task.isEffortDriven && !task.isMilestone && !task.isSupertask && (task.effort?.signum() ?: 0) > 0 &&
    effortDrivenDurationDays(task.effort, task.assignments, task.start.time.toModelLocalDate(), taskManager.calendar) == null
}

/**
 * Moves the tasks to their levelled [starts] with the levelled [durations], in one undoable edit.
 *
 * A moved task gets its start as the earliest begin, otherwise the scheduler would pull it back to the end of its
 * predecessors. The end is set as well, because a new start alone stretches the task. A milestone has no end of its
 * own; its stored end is dropped, otherwise it would stay at the old date before the new start.
 *
 * @return the number of moved tasks; nothing is added to the undo history when there are none. The run is reported
 * to [notifier] afterwards in either case.
 */
fun applyLevelling(
  starts: Map<String, LocalDate>, durations: Map<String, Int>, taskManager: TaskManager,
  undoManager: GPUndoManager, editName: String, notifier: LevellingRunNotifier = levellingRunNotifier
): Int = notifier.runAndReport { writeLevellingBack(starts, durations, taskManager, undoManager, editName) }

private fun writeLevellingBack(
  starts: Map<String, LocalDate>, durations: Map<String, Int>, taskManager: TaskManager,
  undoManager: GPUndoManager, editName: String
): Int {
  val isWorkingDay = workingDayTest(taskManager.calendar)
  val moves = starts.mapNotNull { (id, start) ->
    val task = taskManager.getTask(id.toInt()) ?: return@mapNotNull null
    val days = durations[id] ?: task.duration.length
    if (task.start.time.toModelLocalDate() == start && days == task.duration.length) null else Triple(task, start, days)
  }
  if (moves.isEmpty()) {
    return 0
  }
  undoManager.undoableEdit(editName) {
    // The scheduler would run on every commit; it runs once at the end instead.
    val scheduler = taskManager.algorithmCollection.scheduler
    val wasEnabled = scheduler.isEnabled
    scheduler.isEnabled = false
    try {
      moves.forEach { (task, start, days) ->
        val startDate = CalendarFactory.createGanttCalendar(start.toModelDate())
        (if (task.isMilestone) task.createMutatorFixingDuration() else task.createMutator()).also {
          it.setStart(startDate)
          if (!task.isMilestone) {
            it.setEnd(CalendarFactory.createGanttCalendar(endAfterWorkingDays(start, days, isWorkingDay).toModelDate()))
          }
          it.setThird(startDate, TaskImpl.EARLIESTBEGIN)
          it.commit()
        }
      }
    } finally {
      scheduler.isEnabled = wasEnabled
    }
    scheduler.run()
  }
  return moves.size
}
