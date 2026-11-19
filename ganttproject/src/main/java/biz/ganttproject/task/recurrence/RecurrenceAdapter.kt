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
package biz.ganttproject.task.recurrence

import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.model.task.RecurrenceOrigin
import biz.ganttproject.core.model.task.RecurrenceProblem
import biz.ganttproject.core.model.task.RecurrenceRule
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.time.LocalDate

/**
 * Creates the repeated tasks of a task which has a recurrence rule.
 *
 * Running it again creates nothing twice: every created task carries the id of its source task and its date,
 * so the dates which exist already are left alone, including those which the user has moved or completed.
 * The repeated tasks are put next to the source task in a group, and not under it: a task with subtasks takes its
 * dates from them and would lose its own duration.
 */

/** Whether this task is the group which holds the tasks of a recurring series. */
fun Task.isRecurrenceGroup(): Boolean = recurrenceOrigin?.isGroup == true

/** The calendar of the project as a function, so that the rule can be computed without the project model. */
internal fun workingDayTest(calendar: GPCalendar): (LocalDate) -> Boolean = { day ->
  calendar.getDayMask(day.toModelDate()) and GPCalendar.DayMask.WORKING != 0
}

data class PlannedOccurrence(val sourceTaskId: Int, val date: LocalDate)

data class RecurrenceProblemReport(val taskName: String, val problem: RecurrenceProblem)

data class RecurrencePlan(
  val occurrences: List<PlannedOccurrence>,
  /** Series which cannot be created. If there is any, nothing is created at all. */
  val problems: List<RecurrenceProblemReport>,
  /** The number of series which have something to create. */
  val seriesCount: Int
) {
  val hasProblems: Boolean get() = problems.isNotEmpty()
}

/** Collects what would be created. Changes nothing. */
fun planRecurrences(taskManager: TaskManager): RecurrencePlan {
  val isWorkingDay = workingDayTest(taskManager.calendar)
  val existing = existingOccurrences(taskManager)
  val planned = mutableListOf<PlannedOccurrence>()
  val problems = mutableListOf<RecurrenceProblemReport>()
  var series = 0
  taskManager.tasks.forEach { task ->
    // A task created from a rule does not repeat itself, otherwise every run would repeat the repetitions.
    val rule = task.recurrenceRule?.takeIf { task.recurrenceOrigin == null } ?: return@forEach
    val plan = planSeries(task, rule, isWorkingDay, existing)
    problems.addAll(plan.problems)
    if (plan.occurrences.isNotEmpty()) {
      series++
      planned.addAll(plan.occurrences)
    }
  }
  return RecurrencePlan(planned, problems, series)
}

/** Collects what [rule] would create for [task], whatever rule the task has now. Changes nothing. */
fun planRecurrence(task: Task, rule: RecurrenceRule, taskManager: TaskManager): RecurrencePlan =
  planSeries(task, rule, workingDayTest(taskManager.calendar), existingOccurrences(taskManager))

private fun existingOccurrences(taskManager: TaskManager): Set<Pair<Int, LocalDate?>> =
  taskManager.tasks.mapNotNull { it.recurrenceOrigin }.map { it.sourceTaskId to it.date }.toSet()

private fun planSeries(
  task: Task, rule: RecurrenceRule, isWorkingDay: (LocalDate) -> Boolean, existing: Set<Pair<Int, LocalDate?>>
): RecurrencePlan {
  val first = task.start.time.toModelLocalDate()
  rule.problemFor(first, isWorkingDay)?.let {
    return RecurrencePlan(emptyList(), listOf(RecurrenceProblemReport(task.name.orEmpty(), it)), 0)
  }
  // The first date is the source task itself.
  val missing = rule.occurrences(first, isWorkingDay).drop(1)
    .filter { (task.taskID to it) !in existing }
    .map { PlannedOccurrence(task.taskID, it) }
  return RecurrencePlan(missing, emptyList(), if (missing.isEmpty()) 0 else 1)
}

/**
 * Creates the planned tasks as one undoable edit.
 *
 * @return the number of created tasks, the groups not counted.
 */
fun applyRecurrences(plan: RecurrencePlan, taskManager: TaskManager, undoManager: GPUndoManager, editName: String): Int {
  if (plan.occurrences.isEmpty() || plan.hasProblems) {
    return 0
  }
  var created = 0
  undoManager.undoableEdit(editName) {
    created = createPlanned(plan, taskManager)
  }
  return created
}

/**
 * Sets [rule] on [task] and creates its missing repeated tasks, all as one undoable edit. Repeated tasks which
 * exist already are kept, also those which the new rule does not produce any more.
 *
 * @return the number of created tasks, or null if the rule cannot be applied; then nothing changes.
 */
fun setRecurrence(task: Task, rule: RecurrenceRule, taskManager: TaskManager, undoManager: GPUndoManager, editName: String): Int? {
  val plan = planRecurrence(task, rule, taskManager)
  if (plan.hasProblems) {
    return null
  }
  var created = 0
  undoManager.undoableEdit(editName) {
    task.createMutator().also {
      it.setRecurrenceRule(rule)
      it.commit()
    }
    created = createPlanned(plan, taskManager)
  }
  return created
}

/**
 * Removes the rule of [task] as one undoable edit. The repeated tasks stay: they are ordinary tasks which may have
 * been moved, assigned or completed, and they keep their origin, so a rule set again later does not duplicate them.
 */
fun removeRecurrence(task: Task, undoManager: GPUndoManager, editName: String) {
  if (task.recurrenceRule == null) {
    return
  }
  undoManager.undoableEdit(editName) {
    task.createMutator().also {
      it.setRecurrenceRule(null)
      it.commit()
    }
  }
}

private fun createPlanned(plan: RecurrencePlan, taskManager: TaskManager): Int {
  if (plan.occurrences.isEmpty()) {
    return 0
  }
  var created = 0
  // The scheduler is paused while the tasks are written, every new task would run it over the whole graph.
  val scheduler = taskManager.algorithmCollection.scheduler
  val wasEnabled = scheduler.isEnabled
  scheduler.isEnabled = false
  try {
    val groups = mutableMapOf<Int, Task>()
    plan.occurrences.map { it.sourceTaskId }.distinct().forEach { sourceId ->
      val source = taskManager.getTask(sourceId) ?: return@forEach
      groups[sourceId] = findOrCreateGroup(taskManager, source)
    }
    plan.occurrences.forEach { occurrence ->
      val source = taskManager.getTask(occurrence.sourceTaskId) ?: return@forEach
      val task = taskManager.newTaskBuilder()
        .withPrototype(source)
        .withStartDate(occurrence.date.toModelDate())
        .withCompletion(0)
        .withParent(groups[occurrence.sourceTaskId] ?: taskManager.taskHierarchy.getContainer(source))
        .build()
      // The prototype brings the effort along, the assignments have to be copied.
      source.assignments.forEach { assignment ->
        assignment.resource?.let { task.assignmentCollection.addAssignment(it).load = assignment.load }
      }
      task.createMutator().also {
        it.setRecurrenceOrigin(RecurrenceOrigin(occurrence.sourceTaskId, occurrence.date))
        it.commit()
      }
      created++
    }
  } finally {
    scheduler.isEnabled = wasEnabled
  }
  scheduler.run()
  return created
}

private fun findOrCreateGroup(taskManager: TaskManager, source: Task): Task {
  val groupOrigin = RecurrenceOrigin(source.taskID)
  taskManager.tasks.firstOrNull { it.recurrenceOrigin == groupOrigin }?.let { return it }
  val hierarchy = taskManager.taskHierarchy
  val container = hierarchy.getContainer(source)
  val index = hierarchy.getTaskIndex(source)
  val group = taskManager.newTaskBuilder()
    .withName(source.name)
    .withStartDate(source.start.time)
    .withDuration(source.duration)
    .withParent(container)
    .build()
  group.createMutator().also {
    it.setRecurrenceOrigin(groupOrigin)
    it.commit()
  }
  // The group takes the place of the source task in the outline, a new task would be added at the end.
  hierarchy.move(group, container, index)
  // The group has to exist before the source moves into it.
  hierarchy.move(source, group)
  return group
}
