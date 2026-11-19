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
package net.sourceforge.ganttproject.action.resource

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.calendar.GanttDaysOff
import biz.ganttproject.core.model.task.LevelResult
import biz.ganttproject.core.model.task.LevelTask
import biz.ganttproject.core.model.task.workingDays
import biz.ganttproject.task.recurrence.toModelLocalDate
import net.sourceforge.ganttproject.action.task.LevellingConfirmation
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.algorithm.WorkWeekWorkingDays
import net.sourceforge.ganttproject.task.algorithm.collectLevelTasks
import net.sourceforge.ganttproject.task.algorithm.daysOffRanges
import net.sourceforge.ganttproject.task.algorithm.levelProject
import net.sourceforge.ganttproject.task.algorithm.workingDaysOf
import java.time.LocalDate

/** A task which ends on another day when the days off of a person change. */
data class EndShift(val task: Task, val oldEnd: LocalDate, val newEnd: LocalDate)

/**
 * The tasks which end on another day if [person] has [daysOff] instead of the days off in the model. The project is
 * levelled twice, with the days off in the model and with the new ones, and the last days are compared: a single run
 * against the dates in the model would report the moves of levelling itself as well. The model is not written.
 */
fun endShifts(taskManager: TaskManager, person: HumanResource, daysOff: List<Pair<LocalDate, LocalDate>>,
              today: LocalDate): List<EndShift> {
  val tasks = collectLevelTasks(taskManager, today)
  val workingDays = WorkWeekWorkingDays(taskManager.calendar)
  val worksOn = workingDaysOf(taskManager, workingDays)
  val before = levelProject(taskManager, tasks, today, workingDays)
  val after = levelProject(taskManager, tasks, today, workingDays) { if (it === person) daysOff else it.daysOffRanges() }

  fun lastDay(result: LevelResult, task: LevelTask): LocalDate? {
    val start = result.starts[task.id] ?: return null
    return workingDays(start, result.durations[task.id] ?: task.durationDays, worksOn(task)).last()
  }
  return tasks.mapNotNull { levelTask ->
    val oldEnd = lastDay(before, levelTask) ?: return@mapNotNull null
    val newEnd = lastDay(after, levelTask) ?: return@mapNotNull null
    val task = taskManager.getTask(levelTask.id.toInt())
    if (task == null || oldEnd == newEnd) null else EndShift(task, oldEnd, newEnd)
  }
}

/**
 * Shows what new days off of a person move before they are saved. Nothing is shown when nothing moves, and nothing is
 * saved when the answer is no.
 */
class DaysOffPreview(
  private val taskManager: TaskManager,
  private val confirmation: LevellingConfirmation,
  private val today: () -> LocalDate = { LocalDate.now() }
) {
  /** Runs [save] at once if the days off do not move any task, and after the answer yes if they do. */
  fun confirmThen(person: HumanResource, daysOff: Collection<GanttDaysOff>, save: Runnable) {
    val ranges = daysOff.map { it.start.time.toModelLocalDate() to it.finish.time.toModelLocalDate() }
    val shifts = if (ranges == person.daysOffRanges()) emptyList() else endShifts(taskManager, person, ranges, today())
    if (shifts.isEmpty()) {
      save.run()
    } else {
      confirmation.ask(preview(shifts)) { confirmed ->
        if (confirmed) {
          save.run()
        }
      }
    }
  }

  internal fun preview(shifts: List<EndShift>): String {
    val text = StringBuilder(i18n("resource.daysOff.preview", shifts.size))
    shifts.take(MAX_ROWS).forEach { text.append("\n  • ${it.task.name}: ${it.oldEnd} → ${it.newEnd}") }
    if (shifts.size > MAX_ROWS) {
      text.append("\n  … ").append(i18n("resource.daysOff.preview.more", shifts.size - MAX_ROWS))
    }
    return text.toString()
  }
}

private const val MAX_ROWS = 10

// Shows the key when there is no text for it.
private fun i18n(key: String, vararg args: Any): String = RootLocalizer.formatText(key, *args)
