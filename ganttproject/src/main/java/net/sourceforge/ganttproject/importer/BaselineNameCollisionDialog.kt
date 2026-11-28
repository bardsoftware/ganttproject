/*
Copyright 2026 BarD Software s.r.o

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
package net.sourceforge.ganttproject.importer

import biz.ganttproject.app.OPTION_PANE_STYLESHEET
import biz.ganttproject.app.OptionElementData
import biz.ganttproject.app.OptionPaneBuilder
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.dialog
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.scene.control.ButtonType
import javafx.stage.WindowEvent
import java.util.concurrent.CompletableFuture

/**
 * Shows one dialog which lists all [collidingNames] of an import and asks what to do with them.
 * Calls [handleAnswer] exactly once: with the chosen value if OK was clicked, or with null if the
 * dialog was closed in any other way (Escape, the window close button).
 *
 * OptionPaneBuilder.showDialog is not used because it calls its handler only on OK.
 * onClosed alone is not enough: the window close button hides the window without closing the
 * dialog (Dialog.kt sets onCloseRequest to window.hide()), and then onClosed is never called.
 */
fun showBaselineNameCollisionDialog(collidingNames: List<String>, handleAnswer: (BaselineNameCollision?) -> Unit) {
  var answered = false
  val answerOnce = { answer: BaselineNameCollision? ->
    if (!answered) {
      answered = true
      handleAnswer(answer)
    }
  }
  val optionPane = OptionPaneBuilder<BaselineNameCollision>().apply {
    i18n = RootLocalizer.createWithRootKey(rootKey = "importer.baselineNameCollision")
    titleHelpString?.update(collidingNames.joinToString(", "))
    elements = listOf(
      OptionElementData("rename", BaselineNameCollision.RENAME, true),
      OptionElementData("replace", BaselineNameCollision.REPLACE),
      OptionElementData("keepBoth", BaselineNameCollision.KEEP_BOTH)
    )
  }
  dialog(title = optionPane.titleString.value) { controller ->
    controller.setHeader(optionPane.createHeader())
    val pane = optionPane.buildPane().apply { styleClass.add("option-pane") }
    controller.setContent(pane)
    controller.addStyleSheet(OPTION_PANE_STYLESHEET)
    controller.addStyleClass("option-pane-padding")
    controller.setEscCloseEnabled(true)
    controller.setupButton(ButtonType.OK) { btn ->
      btn.styleClass.add("btn-attention")
      // A filter, because the dialog pane's own handler of this button closes the dialog and so
      // calls onClosed before any handler added here would run.
      btn.addEventFilter(ActionEvent.ACTION) {
        answerOnce(optionPane.toggleGroup.selectedToggle?.userData as? BaselineNameCollision)
      }
    }
    // onClosed is also called after OK, and twice after Escape
    controller.onClosed = { answerOnce(null) }
    controller.onShown = {
      pane.scene?.window?.addEventHandler(WindowEvent.WINDOW_HIDDEN) { answerOnce(null) }
    }
  }
}

/**
 * Shows the dialog and waits until it is answered or closed. Returns null if it was closed.
 * Must not be called on the JavaFX application thread, because the dialog is shown there.
 */
fun askBaselineNameCollision(collidingNames: List<String>): BaselineNameCollision? {
  check(!Platform.isFxApplicationThread()) { "This would block the JavaFX application thread" }
  val answer = CompletableFuture<BaselineNameCollision?>()
  showBaselineNameCollisionDialog(collidingNames) { answer.complete(it) }
  return answer.get()
}
