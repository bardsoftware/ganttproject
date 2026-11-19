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
package net.sourceforge.ganttproject.action.task

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.model.task.LevelConflict
import biz.ganttproject.core.model.task.LevelResult
import biz.ganttproject.core.model.task.SHARED_POOL
import biz.ganttproject.core.model.task.levelTasks
import biz.ganttproject.task.recurrence.toModelLocalDate
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.algorithm.applyLevelling
import net.sourceforge.ganttproject.task.algorithm.capacityProblems
import net.sourceforge.ganttproject.task.algorithm.collectLevelTasks
import net.sourceforge.ganttproject.task.algorithm.durationAtStart
import net.sourceforge.ganttproject.task.algorithm.presenceTest
import net.sourceforge.ganttproject.task.algorithm.resourcePresence
import net.sourceforge.ganttproject.task.algorithm.startAllowed
import net.sourceforge.ganttproject.task.algorithm.WorkWeekWorkingDays
import net.sourceforge.ganttproject.task.algorithm.movedTasks
import net.sourceforge.ganttproject.task.algorithm.workingDaysOf
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.awt.event.ActionEvent
import java.time.LocalDate

/** Shows [message] and asks whether to go on; the answer may come later, after the dialog closes. */
fun interface LevellingConfirmation {
  fun ask(message: String, answer: (Boolean) -> Unit)
}

/** Shows [message]; [isProblem] tells a warning from a plain notice. */
fun interface LevellingReport {
  fun show(isProblem: Boolean, message: String)
}

/**
 * Moves tasks later until no resource is booked over its capacity.
 *
 * Nothing is changed before the preview is confirmed, and the whole run is one undoable edit.
 */
class LevellingAction(
  private val taskManager: TaskManager,
  private val undoManager: GPUndoManager,
  private val confirmation: LevellingConfirmation,
  private val report: LevellingReport,
  private val today: () -> LocalDate = { LocalDate.now() }
) : GPAction("task.level") {

  override fun getLocalizedName(): String = i18n("task.level")

  override fun actionPerformed(event: ActionEvent?) {
    val today = today()
    val workingDays = WorkWeekWorkingDays(taskManager.calendar)
    val tasks = collectLevelTasks(taskManager, today, workingDays = workingDays)
    if (tasks.isEmpty()) {
      report.show(false, i18n("task.level.nothing"))
      return
    }
    // Unfinished work is not laid into the past; started work stays where it is anyway.
    val from = maxOf(taskManager.projectStart.toModelLocalDate(), today)
    val worksOn = workingDaysOf(taskManager, workingDays)
    val result = levelTasks(tasks, from, workingDays.projectOnly, durationAtStart(taskManager, worksOn),
      canStartOn = startAllowed(taskManager), isPresent = resourcePresence(taskManager), workingDaysOf = worksOn,
      isAtWorkplace = presenceTest(taskManager))
    if (result.conflicts.any { it is LevelConflict.Cycle }) {
      report.show(true, i18n("task.level.cycle"))
      return
    }
    val moved = movedTasks(result)
    if (moved.isEmpty() && result.conflicts.isEmpty()) {
      report.show(false, i18n("task.level.nothing"))
      return
    }
    confirmation.ask(levellingPreview(result, moved.size, today)) { confirmed ->
      if (confirmed) {
        applyLevelling(result.starts, result.durations, taskManager, undoManager, i18n("task.level.undo"))
      }
    }
  }

  private fun movedTasks(result: LevelResult): List<Task> = movedTasks(result.starts, result.durations, taskManager)

  /** The text of the preview: what is moved, and what levelling cannot resolve. */
  internal fun levellingPreview(result: LevelResult, movedCount: Int, today: LocalDate): String {
    val text = StringBuilder(i18n("task.level.preview", movedCount))
    val past = unstartedInThePast(today)
    if (past.isNotEmpty()) {
      section(text, i18n("task.level.past", past.size), past.map { it.name })
    }
    val unreachable = result.conflicts.filterIsInstance<LevelConflict.FixedDateNotReachable>()
    if (unreachable.isNotEmpty()) {
      section(text, i18n("task.level.unreachable", unreachable.size), unreachable.map {
        "${nameOf(it.id)}: ${it.fixedStart} / ${it.earliestPossible}"
      })
    }
    val overloads = result.conflicts.filterIsInstance<LevelConflict.Overload>()
    if (overloads.isNotEmpty()) {
      section(text, i18n("task.level.overload", overloads.size), overloads.map {
        "${it.day}: ${it.percent}% (${it.ids.joinToString { id -> nameOf(id) }})"
      })
    }
    val deadlines = result.conflicts.filterIsInstance<LevelConflict.DeadlineMissed>()
    if (deadlines.isNotEmpty()) {
      section(text, i18n("task.level.deadline", deadlines.size), deadlines.map {
        "${nameOf(it.id)}: ${it.deadline} / ${it.actualEnd} (+${it.missingDays})"
      })
    }
    val noDate = result.conflicts.filterIsInstance<LevelConflict.NoPossibleDate>()
    if (noDate.isNotEmpty()) {
      section(text, i18n("task.level.noDate", noDate.size), noDate.map { conflict ->
        val reasons = mutableListOf<String>()
        if (conflict.blocking.isNotEmpty()) {
          reasons.add(i18n("task.level.noDate.absence", conflict.blocking.joinToString { resourceName(it) }))
        }
        if (conflict.atHome.isNotEmpty()) {
          reasons.add(i18n("task.level.noDate.home", conflict.atHome.joinToString { resourceName(it) }))
        }
        if (conflict.fullFor.isNotEmpty()) {
          reasons.add(i18n("task.level.noDate.capacity", conflict.fullFor.joinToString { resourceName(it) }))
        }
        "${nameOf(conflict.id)}: ${reasons.joinToString("; ")}"
      })
    }
    val capacity = capacityProblems(taskManager)
    if (capacity.isNotEmpty()) {
      section(text, i18n("task.level.capacity", capacity.size), capacity.map { it.name })
    }
    return text.toString()
  }

  // Leaf tasks which have not started although their start is before today. Levelling moves them.
  private fun unstartedInThePast(today: LocalDate): List<Task> = taskManager.tasks.filter {
    !taskManager.taskHierarchy.hasNestedTasks(it) && !it.isMilestone && !it.isDateFixed &&
      it.completionPercentage == 0 && it.start.time.toModelLocalDate() < today
  }

  private fun nameOf(id: String): String = taskManager.getTask(id.toIntOrNull() ?: -1)?.name ?: id

  // The pool of the tasks without resources has no name of its own.
  private fun resourceName(id: String): String =
    if (id == SHARED_POOL) i18n("task.level.noDate.sharedPool")
    else taskManager.tasks.asSequence().flatMap { it.assignments.asSequence() }
      .mapNotNull { it.resource }.firstOrNull { it.id.toString() == id }?.name ?: id

  private fun section(text: StringBuilder, title: String, rows: List<String>) {
    text.append("\n\n").append(title)
    rows.take(MAX_ROWS).forEach { text.append("\n  • ").append(it) }
    if (rows.size > MAX_ROWS) {
      text.append("\n  … ").append(i18n("task.level.more", rows.size - MAX_ROWS))
    }
  }
}

private const val MAX_ROWS = 5

// Shows the key when there is no text for it.
private fun i18n(key: String, vararg args: Any): String = RootLocalizer.formatText(key, *args)
