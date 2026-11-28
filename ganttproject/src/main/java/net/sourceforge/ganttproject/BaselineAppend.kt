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

/**
 * Adding a task to an existing baseline.
 *
 * A task created after a baseline was taken has no entry in it, and the chart draws a baseline
 * band only for tasks that have one. [GanttPreviousState] has no way to add an entry, so the
 * baseline is copied: the entries of the source stay as they are and one more is added.
 * Entries of the source are never overwritten, because a baseline records what was planned at the
 * time it was taken.
 */
sealed class BaselineAppendResult {
  /** A new baseline with the entries of the source plus one for the task. Not in any list yet. */
  data class Appended(val baseline: GanttPreviousState) : BaselineAppendResult()

  /** The source already has an entry for the task, nothing was done. */
  data class AlreadyPresent(val taskId: Int) : BaselineAppendResult()
}

/** Creates the baseline entry of [task] in the same way as [GanttPreviousState.createTasks]. */
fun baselineEntryFor(task: Task, taskManager: TaskManager) = GanttPreviousStateTask(
  task.taskID,
  task.start.clone(),
  task.duration.length,
  task.isMilestone,
  taskManager.taskHierarchy.hasNestedTasks(task)
)

/**
 * Builds a baseline from [source] and [task]. Neither [source] nor any baseline list is changed.
 * The result is named [name], which is the name of the source by default.
 */
fun appendToBaseline(
  source: GanttPreviousState, task: Task, taskManager: TaskManager, name: String = source.name
): BaselineAppendResult {
  if (source.tasks.any { it.id == task.taskID }) {
    return BaselineAppendResult.AlreadyPresent(task.taskID)
  }
  return BaselineAppendResult.Appended(
    GanttPreviousState(name, source.tasks + baselineEntryFor(task, taskManager))
  )
}

/**
 * Replaces [source] in [baselines] with a copy which has an entry for [task], as one undoable edit.
 * The caller has to point the chart to the new baseline if [source] was shown there.
 */
fun appendToBaselineUndoable(
  baselines: MutableList<GanttPreviousState>, source: GanttPreviousState, task: Task,
  taskManager: TaskManager, undoManager: GPUndoManager, localizedName: String
): BaselineAppendResult {
  val index = baselines.indexOf(source)
  require(index >= 0) { "The baseline '${source.name}' is not in the list" }
  val result = appendToBaseline(source, task, taskManager)
  if (result is BaselineAppendResult.Appended) {
    undoManager.undoableEdit(localizedName) { baselines[index] = result.baseline }
  }
  return result
}
