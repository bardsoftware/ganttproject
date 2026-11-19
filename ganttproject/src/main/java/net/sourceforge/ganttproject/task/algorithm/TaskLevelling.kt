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
import biz.ganttproject.core.model.task.LevelResult
import biz.ganttproject.core.model.task.SHARED_POOL_LOAD
import biz.ganttproject.core.model.task.levelTasks
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.task.recurrence.toModelDate
import biz.ganttproject.task.recurrence.toModelLocalDate
import net.sourceforge.ganttproject.resource.HumanResource
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
fun collectLevelTasks(taskManager: TaskManager, today: LocalDate, moveUnstartedPast: Boolean = true,
                      workingDays: WorkWeekWorkingDays = WorkWeekWorkingDays(taskManager.calendar)): List<LevelTask> {
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
    taskManager.getTask(id).toLevelTask(order, leavesUnder, id in started, today, moveUnstartedPast, workingDays)
  }
}

private fun Task.toLevelTask(
  order: Int, leavesUnder: Map<Int, List<Int>>, started: Boolean, today: LocalDate, moveUnstartedPast: Boolean,
  workingDays: WorkWeekWorkingDays
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
  // Levelling counts the days of a task with its own working days, the task model with those of the project.
  val worksOn = workingDays.forTask(this)
  val durationDays = if (worksOn === workingDays.projectOnly) duration.length.coerceAtLeast(1)
    else workingDays.taskDays(start, end.time.toModelLocalDate(), worksOn)
  return LevelTask(
    id = taskID.toString(),
    orderInPlan = order,
    priority = priority.ordinal,
    durationDays = durationDays,
    predecessors = dependenciesAsDependant.toArray()
      .flatMap { leavesUnder[it.dependee.taskID] ?: listOf(it.dependee.taskID) }
      .distinct().map { it.toString() },
    fixedStart = if (frozen || isDateFixed) start else null,
    earliestStart = if (thirdDateConstraint == TaskImpl.EARLIESTBEGIN) third?.time?.toModelLocalDate() else null,
    loads = loads,
    frozen = frozen,
    deadline = deadline?.time?.toModelLocalDate(),
    // A blocking resource has to be present whatever its load, also on a milestone.
    blocking = assignments.filter { it.resource != null && it.isBlocking }.map { it.resource.id.toString() }.toSet(),
    requiresPresence = isOnSiteOnly
  )
}

/**
 * Whether a task may start on the given day. An effort driven task does not start on a day off of any of its
 * blocking resources, it starts when all of them are back; a day off later in the task lengthens it instead. An
 * assignment which is not blocking, or has no load, does not hold the task back. Every other task may start on any
 * day.
 */
fun startAllowed(taskManager: TaskManager,
                 daysOffOf: (HumanResource) -> List<Pair<LocalDate, LocalDate>> = { it.daysOffRanges() }): (LevelTask, LocalDate) -> Boolean = { levelTask, start ->
  val task = taskManager.getTask(levelTask.id.toInt())
  if (task == null || !task.isEffortDriven || task.isMilestone) {
    true
  } else {
    task.assignments.filter { it.load > 0 && it.isBlocking }
      .none { it.resource?.let(daysOffOf)?.covers(start) == true }
  }
}

/**
 * Whether a resource, by its id as in [LevelTask.blocking], is present on the given day, that is, not on a day off.
 * The days off are read once, when the function is made, so one function serves one levelling run.
 */
fun resourcePresence(taskManager: TaskManager,
                     daysOffOf: (HumanResource) -> List<Pair<LocalDate, LocalDate>> = { it.daysOffRanges() }): (String, LocalDate) -> Boolean {
  val daysOff = taskManager.tasks.flatMap { it.assignments.toList() }.mapNotNull { it.resource }.distinct()
    .associate { it.id.toString() to daysOffOf(it) }
  return { resourceId, day -> daysOff[resourceId]?.covers(day) != true }
}

/**
 * Whether a resource, by its id as in [LevelTask.blocking], is on site on the given day, that is, not at home. A day at
 * home is not a day off: the resource works, but not on a task which has to be done on site. The home offices are read
 * once, when the function is made, so one function serves one levelling run.
 */
fun presenceTest(taskManager: TaskManager): (String, LocalDate) -> Boolean {
  val homeOffice = taskManager.tasks.flatMap { it.assignments.toList() }.mapNotNull { it.resource }.distinct()
    .associate { it.id.toString() to it.homeOffice }.filterValues { !it.isEmpty }
  return { resourceId, day -> homeOffice[resourceId]?.worksFromHome(day) != true }
}

/**
 * Levels [tasks] as the levelling action does, from [today] on. [daysOffOf] tells the days off of a person, which lets
 * a caller ask what the result would be with other days off than the ones in the model.
 */
fun levelProject(taskManager: TaskManager, tasks: List<LevelTask>, today: LocalDate,
                 workingDays: WorkWeekWorkingDays = WorkWeekWorkingDays(taskManager.calendar),
                 daysOffOf: (HumanResource) -> List<Pair<LocalDate, LocalDate>> = { it.daysOffRanges() }): LevelResult {
  // Unfinished work is not laid into the past; started work stays where it is anyway.
  val from = maxOf(taskManager.projectStart.toModelLocalDate(), today)
  val worksOn = workingDaysOf(taskManager, workingDays)
  return levelTasks(tasks, from, workingDays.projectOnly, durationAtStart(taskManager, worksOn, daysOffOf),
    canStartOn = startAllowed(taskManager, daysOffOf), isPresent = resourcePresence(taskManager, daysOffOf),
    workingDaysOf = worksOn, isAtWorkplace = presenceTest(taskManager))
}

/**
 * The working days of each levelled task as [WorkWeekWorkingDays.forTask] gives them. Levelling asks for the same days
 * again and again, so the answers are remembered; one function serves one levelling run.
 */
fun workingDaysOf(taskManager: TaskManager,
                  workingDays: WorkWeekWorkingDays = WorkWeekWorkingDays(taskManager.calendar)): (LevelTask) -> (LocalDate) -> Boolean {
  val perTask = mutableMapOf<String, (LocalDate) -> Boolean>()
  return { levelTask ->
    perTask.getOrPut(levelTask.id) {
      val worksOn = taskManager.getTask(levelTask.id.toInt())?.let { workingDays.forTask(it) } ?: workingDays.projectOnly
      val answers = HashMap<LocalDate, Boolean>()
      val remembering: (LocalDate) -> Boolean = { day -> answers.getOrPut(day) { worksOn(day) } }
      remembering
    }
  }
}

/**
 * The duration of a task in its own working days if it starts on the given day. An effort driven task is derived
 * again, because its duration depends on the start when the working hours of its resources change; every other task
 * keeps its duration. What the duration is derived from is read once per task.
 */
fun durationAtStart(taskManager: TaskManager,
                    worksOn: (LevelTask) -> (LocalDate) -> Boolean = workingDaysOf(taskManager),
                    daysOffOf: (HumanResource) -> List<Pair<LocalDate, LocalDate>> = { it.daysOffRanges() }): (LevelTask, LocalDate) -> Int {
  val inputs = mutableMapOf<String, EffortInputs>()
  return { levelTask, start ->
    val task = taskManager.getTask(levelTask.id.toInt())
    if (levelTask.frozen || task == null || !task.isEffortDriven || task.isMilestone) {
      levelTask.durationDays
    } else {
      val effortInputs = inputs.getOrPut(levelTask.id) { effortInputs(task.assignments, task.isOnSiteOnly, daysOffOf) }
      effortDrivenDurationDays(task.effort, effortInputs, start, worksOn(levelTask))?.toInt() ?: levelTask.durationDays
    }
  }
}

/**
 * Effort driven tasks whose duration cannot be derived because their resources make no hours available, such as at a
 * utilization of zero. Levelling places them with their stored duration, so they are worth a warning.
 */
fun capacityProblems(taskManager: TaskManager): List<Task> = taskManager.tasks.filter { task ->
  task.isEffortDriven && !task.isMilestone && !task.isSupertask && (task.effort?.signum() ?: 0) > 0 &&
    effortDrivenProjectDays(task.effort, task.assignments, task.start.time.toModelLocalDate(), taskManager.calendar,
      requiresPresence = task.isOnSiteOnly) == null
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

/** The tasks which levelling to [starts] and [durations] moves or makes longer or shorter. */
fun movedTasks(starts: Map<String, LocalDate>, durations: Map<String, Int>, taskManager: TaskManager): List<Task> =
  levellingMoves(starts, durations, taskManager).map { it.task }

private data class Move(val task: Task, val start: LocalDate, val end: LocalDate)

private fun levellingMoves(starts: Map<String, LocalDate>, durations: Map<String, Int>, taskManager: TaskManager): List<Move> {
  val workingDays = WorkWeekWorkingDays(taskManager.calendar)
  return starts.mapNotNull { (id, start) ->
    val task = taskManager.getTask(id.toInt()) ?: return@mapNotNull null
    val isWorkingDay = workingDays.forTask(task)
    // Levelling counts in the working days of the task, the task model in those of the project.
    val taskDays = durations[id] ?: if (isWorkingDay === workingDays.projectOnly) task.duration.length
      else workingDays.taskDays(task.start.time.toModelLocalDate(), task.end.time.toModelLocalDate(), isWorkingDay)
    val days = workingDays.projectDays(start, taskDays.toLong(), isWorkingDay).toInt()
    if (task.start.time.toModelLocalDate() == start && days == task.duration.length) null
    else Move(task, start, workingDays.modelEnd(start, taskDays, isWorkingDay))
  }
}

private fun writeLevellingBack(
  starts: Map<String, LocalDate>, durations: Map<String, Int>, taskManager: TaskManager,
  undoManager: GPUndoManager, editName: String
): Int {
  val moves = levellingMoves(starts, durations, taskManager)
  if (moves.isEmpty()) {
    return 0
  }
  undoManager.undoableEdit(editName) {
    // The scheduler would run on every commit; it runs once at the end instead.
    val scheduler = taskManager.algorithmCollection.scheduler
    val wasEnabled = scheduler.isEnabled
    scheduler.isEnabled = false
    try {
      moves.forEach { (task, start, end) ->
        val startDate = CalendarFactory.createGanttCalendar(start.toModelDate())
        (if (task.isMilestone) task.createMutatorFixingDuration() else task.createMutator()).also {
          it.setStart(startDate)
          if (!task.isMilestone) {
            it.setEnd(CalendarFactory.createGanttCalendar(end.toModelDate()))
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
