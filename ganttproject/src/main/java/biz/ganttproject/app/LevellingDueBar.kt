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
package biz.ganttproject.app

import biz.ganttproject.FXUtil
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import net.sourceforge.ganttproject.task.algorithm.LevellingStaleness

/**
 * The status bar message which says that the levelling is due, with a button that runs it.
 *
 * The message is a signal: a short sentence, and the reason for it is in the tooltips, because the status bar is
 * narrow on small screens.
 *
 * The node is neither visible nor managed while the levelling is not due, so it takes no room in the status bar.
 * [runLevelling] is called on the JavaFX thread, and it has to move itself to the thread where the levelling action
 * runs.
 */
class LevellingDueBar(
  private val staleness: LevellingStaleness,
  private val runLevelling: () -> Unit
) {
  val label: Label = Label(RootLocalizer.formatText("levelling.due.message")).also {
    it.tooltip = Tooltip(RootLocalizer.formatText("levelling.due.message.tooltip"))
  }

  val button: Button = Button(RootLocalizer.formatText("levelling.due.button")).also {
    it.setOnAction { runLevelling() }
    // The button explains what pressing it does, which is not what the label explains.
    it.tooltip = Tooltip(RootLocalizer.formatText("levelling.due.button.tooltip"))
  }

  val node: HBox = HBox(8.0, label, button).also {
    it.alignment = Pos.CENTER_LEFT
    it.padding = Insets(0.0, 8.0, 0.0, 8.0)
  }

  private val onChange: (Boolean) -> Unit = { FXUtil.runLater { show(it) } }

  init {
    show(staleness.isStale)
    staleness.addListener(onChange)
  }

  fun detach() {
    staleness.removeListener(onChange)
  }

  private fun show(visible: Boolean) {
    node.isVisible = visible
    node.isManaged = visible
  }
}
