/*
 * Copyright (c) 2026 Dmitry Barashev, BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.gui.projectwizard

import biz.ganttproject.FXUtil
import biz.ganttproject.app.DialogController
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.dialog
import biz.ganttproject.app.setSwingBackground
import biz.ganttproject.core.option.ObservableBoolean
import biz.ganttproject.core.option.ObservableString
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
 * Import/Export wizard model.
 */
open class WizardModel(val id: String, val title: String) {
  val i18n = RootLocalizer
  val coroutineScope = CoroutineScope(EmptyCoroutineContext)

  // This is executed when user clicks "OK" button
  var onOk: () -> Unit = {}

  // Returns `true` if it is okay to finish the wizard.
  var canFinish: () -> Boolean = { errorMessage.value.isNullOrBlank() }

  // Returns `true` if it is okay to go to the next page.
  var hasNext: () -> Boolean = { currentPage < pages.size - 1 }

  // Current page index.
  internal var currentPage = 0

  internal val pages = mutableListOf<WizardPage>()

  // Indicates that the wizard buttons, such as Next or OK, need to be refreshed.
  val needsRefresh = ObservableBoolean("needsRefresh", false)

  // In case of errors, this string will contain a localized error message.
  val errorMessage = ObservableString("", "").also {
    it.addWatcher { needsRefresh.set(true, this) }
  }

  fun addPage(page: WizardPage) {
    pages.add(page)
  }

  fun removePage(page: WizardPage) {
    pages.remove(page)
  }

  fun hasPrev(): Boolean {
    return currentPage > 0
  }

  fun start() {
    needsRefresh.set(false, this)
  }


}

/**
 * Shows a wizard dialog using the provided builder.
 */
fun showWizard(model: WizardModel) {
  dialog(model.title, model.id) { ctrl ->
    val ui = WizardUiFx(ctrl, model)
    ui.show(ctrl)
  }
}

/**
 * Implements a wizard dialog UI using Java FX.
 */
private class WizardUiFx(private val ctrl: DialogController, private val model: WizardModel) {
  private val coroutineScope = model.coroutineScope
  private val pages = model.pages
  private val i18n = RootLocalizer
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

    model.needsRefresh.addWatcher { evt ->
      if (evt.trigger != this) {
        if (evt.newValue) {
          adjustButtonState()
        }
        model.needsRefresh.set(false, this)
      }
    }
    model.start()
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
    if (model.hasNext()) {
      currentPage.setActive(false)
      model.currentPage++
      updatePage()
    }
  }

  private fun backPage() {
    if (model.hasPrev()) {
      currentPage.setActive(false)
      model.currentPage--
      updatePage()
    }
  }

  private fun updatePage() {
    val page = currentPage
    page.setActive(true)

    titleString.update(page.title)
    //, i18n.formatText("step"), model.currentPage + 1, i18n.formatText("of"), pages.size
    val fxNode = page.fxComponent
    if (fxNode == null) {
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
    } else {
      FXUtil.transitionNode(stackPane, {
        stackPane.children.clear()
        stackPane.children.add(fxNode)
      }, {
        ctrl.resize()
        adjustButtonState()
      })
    }
  }

  private fun adjustButtonState() {
    backButton.isDisable = !model.hasPrev()
    nextButton.isDisable = !model.hasNext()
    finishButton.isDisable = !canFinish()
  }

  private fun canFinish(): Boolean = model.canFinish()

  private fun onOkPressed() {
    currentPage.setActive(false)
    model.onOk()
  }

  private fun onCancelPressed() {
    currentPage.setActive(false)
  }

  private val currentPage: WizardPage
    get() = pages[model.currentPage]
}
