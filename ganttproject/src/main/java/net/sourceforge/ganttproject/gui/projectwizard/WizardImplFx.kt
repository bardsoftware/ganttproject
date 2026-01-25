/*
Copyright 2026 Dmitry Barashev,  BarD Software s.r.o

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
package net.sourceforge.ganttproject.gui.projectwizard

import biz.ganttproject.FXUtil
import biz.ganttproject.app.DialogController
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.dialog
import biz.ganttproject.app.setSwingBackground
import biz.ganttproject.core.option.ObservableBoolean
import biz.ganttproject.lib.fx.vbox
import javafx.embed.swing.SwingNode
import javafx.event.ActionEvent
import javafx.scene.control.Button
import javafx.scene.layout.StackPane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import net.sourceforge.ganttproject.action.CancelAction
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.action.OkAction
import javax.swing.JComponent
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Collects input for building a wizard UI.
 */
class WizardBuilder {
  val i18n = RootLocalizer
  val coroutineScope = CoroutineScope(EmptyCoroutineContext)
  var title: String = ""
  var dialogId: String = ""
  var onOk: () -> Unit = {}
  var canFinish: () -> Boolean = { true }

  val pages = mutableListOf<WizardPage>()
  val needsRefresh = ObservableBoolean("", false)

  fun addPage(page: WizardPage) {
    pages.add(page)
  }
}

/**
 * Shows a wizard dialog using the provided builder.
 */
fun showWizard(builder: WizardBuilder) {
  dialog(builder.title, builder.dialogId) { ctrl ->
    val ui = WizardUiFx(ctrl, builder)
    ui.show(ctrl)
  }
}

/**
 * Implements a wizard dialog UI using Java FX.
 */
private class WizardUiFx(private val ctrl: DialogController, private val builder: WizardBuilder) {
  private val coroutineScope = builder.coroutineScope
  private val pages = builder.pages
  private val i18n = RootLocalizer
  private var currentPageIndex = 0
  private var nextButton: Button = Button()
  private var backButton: Button = Button()
  private var finishButton: Button = Button()
  private val stackPane = StackPane().also {
    it.styleClass.add("page-container")
    it.styleClass.add("swing-background")
  }
  private val titleString = i18n.create("exportWizard.page.header")

  init {
    backButton = ctrl.setupButton(GPAction.create("back") {
      backPage()
    }) { btn ->
      btn.styleClass.addAll("btn-regular")
      btn.addEventFilter(ActionEvent.ACTION) {
        it.consume()
        backPage()
      }
    }!!

    nextButton = ctrl.setupButton(GPAction.create("next") {
      nextPage()
    }) { btn ->
      btn.styleClass.addAll("btn-regular", "secondary")
      btn.addEventFilter(ActionEvent.ACTION) {
        it.consume()
        nextPage()
      }
    }!!

    finishButton = ctrl.setupButton(OkAction.create("ok") {
      onOkPressed()
    })!!

    // Cancel Button
    ctrl.setupButton(CancelAction.create("cancel") {
      onCancelPressed()
    })

    builder.needsRefresh.addWatcher { evt ->
      if (evt.newValue && evt.trigger != this) {
        adjustButtonState()
        builder.needsRefresh.set(false, this)
      }
    }

    ctrl.resize()
  }
  fun show(ctrl: DialogController) {
      ctrl.addStyleSheet("/biz/ganttproject/app/Dialog.css")
      ctrl.addStyleSheet("/biz/ganttproject/impex/Exporter.css")
      ctrl.addStyleClass("dlg", "dlg-export-wizard", "swing-background")
      ctrl.setHeader(vbox {
        addClasses("header")
        addTitle(titleString)
      })
      ctrl.setContent(stackPane)
      ctrl.onShown = {
        updatePage()
        ctrl.setSwingBackground()
        ctrl.resize()
      }
  }

  private fun nextPage() {
    if (currentPageIndex < pages.size - 1) {
      currentPage.setActive(false)
      currentPageIndex++
      updatePage()
    }
  }

  private fun backPage() {
    if (currentPageIndex > 0) {
      currentPage.setActive(false)
      currentPageIndex--
      updatePage()
    }
  }

  private fun updatePage() {
    val page = currentPage
    page.setActive(true)

    titleString.update(page.title, i18n.formatText("step"), currentPageIndex + 1, i18n.formatText("of"), pages.size)

    val swingNode = SwingNode()
    coroutineScope.launch {
      withContext(Dispatchers.Swing) {
        swingNode.content = page.component as JComponent
      }
      withContext(Dispatchers.JavaFx) {
        //borderPane.center = swingNode
        FXUtil.transitionNode(stackPane, {
          stackPane.children.clear()
          stackPane.children.add(swingNode)
        }, {
          ctrl.resize()
          adjustButtonState()
        })
      }
    }
  }

  private fun adjustButtonState() {
    backButton.isDisable = currentPageIndex == 0
    nextButton.isDisable = currentPageIndex >= pages.size - 1
    finishButton.isDisable = !canFinish()
  }

  private fun canFinish(): Boolean = builder.canFinish()

  private fun onOkPressed() {
    currentPage.setActive(false)
    builder.onOk()
  }

  private fun onCancelPressed() {
    currentPage.setActive(false)
  }

  private val currentPage: WizardPage
    get() = pages[currentPageIndex]
}
