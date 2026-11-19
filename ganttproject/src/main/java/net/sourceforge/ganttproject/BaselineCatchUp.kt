/*
Copyright 2026

This file is part of GanttProject, an open-source project management tool.

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
package net.sourceforge.ganttproject

import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.undo.GPUndoManager

/** What the status bar has to say about the tasks which are missing in the baseline. */
sealed class BaselineGap {
  /** Nothing to say: the baseline has an entry for every task, or the project has no tasks. */
  object Covered : BaselineGap()

  /** The project has tasks but no baseline. There is nothing to add tasks to in this case. */
  object NoBaseline : BaselineGap()

  /** [tasks] have no entry in [baseline]. */
  data class Missing(val baseline: GanttPreviousState, val tasks: List<Task>) : BaselineGap()
}

/**
 * The baseline which the missing tasks are counted against and added to: the one shown in the chart, or the last
 * one in the list if the chart shows none. The shown baseline is not saved, so after opening a file it is the last.
 */
fun catchUpTarget(baselines: List<GanttPreviousState>, shown: GanttPreviousState?): GanttPreviousState? =
  shown?.takeIf { baselines.contains(it) } ?: baselines.lastOrNull()

/**
 * The tasks of [taskManager] which have no entry in [baseline]. Deleted tasks are not in [TaskManager.getTasks],
 * and entries of deleted tasks are ignored.
 */
fun tasksMissingIn(baseline: GanttPreviousState, taskManager: TaskManager): List<Task> {
  val ids = baseline.tasks.mapTo(HashSet()) { it.id }
  return taskManager.tasks.filter { it.taskID !in ids }
}

fun baselineGap(
  taskManager: TaskManager, baselines: List<GanttPreviousState>, shown: GanttPreviousState?
): BaselineGap {
  if (taskManager.tasks.isEmpty()) {
    return BaselineGap.Covered
  }
  val target = catchUpTarget(baselines, shown) ?: return BaselineGap.NoBaseline
  val missing = tasksMissingIn(target, taskManager)
  return if (missing.isEmpty()) BaselineGap.Covered else BaselineGap.Missing(target, missing)
}

/**
 * Adds an entry for every task which is missing in [target] and replaces [target] in [baselines] with the result,
 * as one undoable edit. Existing entries are kept as they are. Returns the new baseline, or null if nothing was
 * missing. The caller has to point the chart to the new baseline if [target] was shown there.
 */
fun catchUpBaseline(
  baselines: MutableList<GanttPreviousState>, target: GanttPreviousState, taskManager: TaskManager,
  undoManager: GPUndoManager, localizedName: String
): GanttPreviousState? {
  val index = baselines.indexOf(target)
  require(index >= 0) { "The baseline '${target.name}' is not in the list" }
  val result = tasksMissingIn(target, taskManager).fold(target) { baseline, task ->
    when (val appended = appendToBaseline(baseline, task, taskManager)) {
      is BaselineAppendResult.Appended -> appended.baseline
      is BaselineAppendResult.AlreadyPresent -> baseline
    }
  }
  if (result === target) {
    return null
  }
  undoManager.undoableEdit(localizedName) { baselines[index] = result }
  return result
}
