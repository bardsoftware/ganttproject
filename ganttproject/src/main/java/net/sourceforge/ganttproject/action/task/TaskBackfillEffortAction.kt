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
package net.sourceforge.ganttproject.action.task

import biz.ganttproject.app.RootLocalizer
import net.sourceforge.ganttproject.action.CancelAction
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.action.OkAction
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.TaskSelectionManager
import net.sourceforge.ganttproject.task.algorithm.BackfillPreparation
import net.sourceforge.ganttproject.task.algorithm.BackfillProposal
import net.sourceforge.ganttproject.task.algorithm.BackfillSkip
import net.sourceforge.ganttproject.task.algorithm.CapacityProblem
import net.sourceforge.ganttproject.task.algorithm.applyEffortBackfill
import net.sourceforge.ganttproject.task.algorithm.prepareEffortBackfill
import net.sourceforge.ganttproject.task.algorithm.tasksForBackfill
import javax.swing.JOptionPane

/**
 * Sets the effort of the selected tasks, or of all tasks if none is selected, which have no effort yet, from their
 * duration and the hours of their assignments, and makes them effort driven. The dates do not change.
 */
class TaskBackfillEffortAction(taskManager: TaskManager, selectionManager: TaskSelectionManager, uiFacade: UIFacade)
  : TaskActionBase("task.backfillEffort", taskManager, selectionManager, uiFacade) {

  init {
    // The edit is undoable as a whole once the user has confirmed it.
    disableUndoableEdit()
  }

  override fun isEnabled(selection: List<Task>) = true

  override fun run(selection: List<Task>) {
    when (val preparation = prepareEffortBackfill(tasksForBackfill(selection, taskManager), taskManager.calendar)) {
      is BackfillPreparation.Problems -> uiFacade.showErrorDialog(problemsText(preparation.problems))
      is BackfillPreparation.Ready -> confirm(preparation.proposal)
    }
  }

  private fun confirm(proposal: BackfillProposal) {
    if (proposal.effort.isEmpty()) {
      uiFacade.showOptionDialog(JOptionPane.INFORMATION_MESSAGE,
        RootLocalizer.formatText("task.backfillEffort.nothing"), arrayOf(CancelAction.CLOSE))
      return
    }
    val skipped = proposal.skipped.values.groupingBy { it }.eachCount()
    val message = RootLocalizer.formatText("task.backfillEffort.preview",
      proposal.effort.size,
      skipped[BackfillSkip.HAS_EFFORT] ?: 0,
      skipped[BackfillSkip.NO_ASSIGNMENT] ?: 0,
      skipped[BackfillSkip.NO_HOURS] ?: 0,
      skipped[BackfillSkip.DURATION_WOULD_CHANGE] ?: 0,
      (skipped[BackfillSkip.SUPERTASK] ?: 0) + (skipped[BackfillSkip.MILESTONE] ?: 0))
    uiFacade.showOptionDialog(JOptionPane.QUESTION_MESSAGE, message, arrayOf(
      OkAction.create("ok") {
        applyEffortBackfill(proposal, taskManager, uiFacade.undoManager, localizedDescription)
      },
      CancelAction.EMPTY
    ))
  }

  private fun problemsText(problems: Map<HumanResource, List<CapacityProblem>>) = buildString {
    problems.forEach { (resource, list) ->
      appendLine(RootLocalizer.formatText("task.backfillEffort.capacityError.title", resource.name))
      list.forEach {
        append("  • ").appendLine(RootLocalizer.formatText(when (it) {
          CapacityProblem.HOURS_PER_DAY -> "task.backfillEffort.capacityError.hoursPerDay"
          CapacityProblem.UTILIZATION -> "task.backfillEffort.capacityError.utilization"
          CapacityProblem.HOURS_CHANGE -> "task.backfillEffort.capacityError.hoursChange"
        }))
      }
    }
    appendLine()
    append(RootLocalizer.formatText("task.backfillEffort.capacityError.consequence"))
  }

  override fun asToolbarAction(): GPAction = this
}
