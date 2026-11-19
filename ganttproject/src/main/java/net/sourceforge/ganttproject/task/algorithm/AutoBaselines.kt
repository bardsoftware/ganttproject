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

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.option.DefaultBooleanOption
import biz.ganttproject.core.option.GPOptionGroup
import net.sourceforge.ganttproject.GanttPreviousState
import net.sourceforge.ganttproject.task.TaskManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** At most this many automatic baselines are kept; the oldest ones go. */
const val MAX_AUTO_BASELINES = 15

/**
 * The option which makes the levelling save a baseline before it moves the tasks. It is off by default: a baseline is
 * project data, and the levelling does not touch project data other than the dates of the tasks unless asked.
 */
object LevellingOptions {
  val autoBaseline = DefaultBooleanOption("autoBaseline", false)
  val optionGroup = GPOptionGroup("levelling", autoBaseline)
}

/**
 * Saves a baseline of the plan before the levelling moves the tasks, and keeps their number bounded.
 *
 * With the option switched off nothing here touches the baselines of the project.
 *
 * Only the baselines whose name matches the generated name are ever removed, so a baseline named by hand stays. The
 * match is against the whole name and in the current language only: a baseline made in another language is not
 * recognised, which means it stays.
 *
 * [baselines] is asked every time because the list behind it is replaced when another project is opened.
 * [formatName] builds the name from a time stamp.
 */
class AutoBaselines @JvmOverloads constructor(
  private val taskManager: TaskManager,
  private val baselines: () -> MutableList<GanttPreviousState>,
  private val isEnabled: () -> Boolean = { LevellingOptions.autoBaseline.isChecked },
  private val now: () -> LocalDateTime = { LocalDateTime.now() },
  private val formatName: (String) -> String = { RootLocalizer.formatText("task.level.baseline.name", it) },
  private val limit: Int = MAX_AUTO_BASELINES
) {
  /** The text for the confirmation question, or null when no baseline will be saved. */
  fun previewNote(): String? {
    if (!isEnabled()) return null
    val note = StringBuilder(RootLocalizer.formatText("task.level.baseline", newName()))
    val doomed = surplus(incoming = 1)
    if (doomed.isNotEmpty()) {
      note.append(' ').append(RootLocalizer.formatText("task.level.baseline.prune", doomed.size))
    }
    return note.toString()
  }

  /** Saves the baseline and removes the surplus ones. Call it before the tasks are moved. */
  fun saveBeforeLevelling() {
    if (!isEnabled()) return
    val list = baselines()
    list.add(GanttPreviousState(newName(), GanttPreviousState.createTasks(taskManager)))
    surplus(incoming = 0).forEach { list.remove(it) }
  }

  fun isAutomatic(name: String): Boolean {
    val marker = "@@STAMP@@"
    val parts = formatName(marker).split(marker)
    return name.isNotEmpty() && Regex(parts.joinToString(".+") { Regex.escape(it) }).matches(name)
  }

  // The oldest first: a baseline has no time stamp of its own, so the position in the list is the age.
  private fun surplus(incoming: Int): List<GanttPreviousState> {
    val automatic = baselines().filter { isAutomatic(it.name) }
    return automatic.take(maxOf(0, automatic.size + incoming - limit))
  }

  private fun newName() = formatName(now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
}
