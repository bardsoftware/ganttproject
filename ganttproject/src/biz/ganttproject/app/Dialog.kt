/*
Copyright 2019 BarD Software s.r.o

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
package biz.ganttproject.app

import biz.ganttproject.lib.fx.VBoxBuilder
import com.sandec.mdfx.MDFXNode
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.animation.FadeTransition
import javafx.animation.ParallelTransition
import javafx.animation.Transition
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.effect.BoxBlur
import javafx.scene.input.KeyCombination
import javafx.scene.layout.*
import javafx.util.Duration

/**
 * Some utility code for building nice dialogs. Provides the following features:
 * - convenient single-line access to buttons via DialogBuilderApi::setupButton
 * - overlay alert message via DialogBuilderApi::showAlert
 * - binds Escape key to closing dialog
 *
 * Typical usage:
 * dialog() {
 *   this.dialogApi = it
 *   it.setContent(...)
 *   it.setupButton(...)
 * }
 *
 * onError() {
 *   this.dialogApi.showAlert(...)
 * }
 */
fun dialog(contentBuilder: (DialogControllerDialogPane) -> Unit) {
  Platform.runLater {
    Dialog<Unit>().also {
      it.isResizable = true
      val dialogBuildApi = DialogControllerDialogPane(it.dialogPane)
      it.dialogPane.apply {
        styleClass.addAll("dlg")
        stylesheets.addAll("/biz/ganttproject/app/Theme.css", "/biz/ganttproject/app/Dialog.css")

        contentBuilder(dialogBuildApi)
        val window = scene.window
        window.onCloseRequest = EventHandler {
          window.hide()
        }
        scene.accelerators[KeyCombination.keyCombination("ESC")] = Runnable { window.hide() }
      }
      it.onShown = EventHandler { _ ->
        it.dialogPane.layout()
        it.dialogPane.scene.window.sizeToScene()
      }
      it.show()
    }
  }
}

interface DialogController {
  fun setContent(content: Node)
  fun setupButton(type: ButtonType, code: (Button) -> Unit = {})
  fun showAlert(title: LocalizedString, content: Node)
  fun addStyleClass(styleClass: String)
  fun addStyleSheet(vararg stylesheets: String)
  fun setHeader(header: Node)
  fun hide()
  fun removeButtonBar()
}

class DialogControllerDialogPane(private val dialogPane: DialogPane) : DialogController {
  private val stackPane = StackPane().also { it.styleClass.add("layers") }
  private var content: Node = Region()

  override fun setContent(content: Node) {
    this.content = content
    content.styleClass.add("content-pane")
    this.stackPane.children.add(content)
    this.dialogPane.content = stackPane
  }

  override fun setupButton(type: ButtonType, code: (Button) -> Unit) {
    this.dialogPane.buttonTypes.add(type)
    val btn = this.dialogPane.lookupButton(type)
    if (btn is Button) {
      code(btn)
    }
  }

  override fun showAlert(title: LocalizedString, content: Node) {
    Platform.runLater {
      createAlertPane(this.content, this.stackPane, title, content)
    }
  }

  override fun addStyleClass(styleClass: String) {
    this.dialogPane.styleClass.add(styleClass)
  }

  override fun addStyleSheet(vararg stylesheets: String) {
    this.dialogPane.stylesheets.addAll(stylesheets)
  }

  override fun setHeader(header: Node) {
    header.styleClass.add("header")
    this.dialogPane.header = header
  }

  override fun hide() {
    this.dialogPane.scene.window.hide()
  }

  override fun removeButtonBar() {
    this.dialogPane.children.remove(this.dialogPane.children.first { it.styleClass.contains("button-bar") })
  }

}

fun createAlertPane(underlayPane: Node, stackPane: StackPane, title: LocalizedString, body: Node) {
  val notificationPane = BorderPane().also { pane ->
    pane.styleClass.add("alert-glasspane")
    val vboxBuilder = VBoxBuilder("alert-box")
    vboxBuilder.addTitle(title).also { hbox ->
      hbox.alignment = Pos.CENTER_LEFT
      hbox.isFillHeight = true
      hbox.children.add(Region().also { node -> HBox.setHgrow(node, Priority.ALWAYS) })
      val btnClose = Button(null, FontAwesomeIconView(FontAwesomeIcon.TIMES)).also { btn -> btn.styleClass.add("alert-dismiss") }
      hbox.children.add(btnClose)
      btnClose.addEventHandler(ActionEvent.ACTION) {
        stackPane.children.remove(pane)
        underlayPane.effect = null
      }

    }
    vboxBuilder.add(body, Pos.CENTER, Priority.ALWAYS)
    pane.center = vboxBuilder.vbox
    pane.opacity = 0.0
  }
  stackPane.children.add(notificationPane)
  val fadeIn = FadeTransition(Duration.seconds(1.0), notificationPane)
  fadeIn.fromValue = 0.0
  fadeIn.toValue = 1.0

  val washOut = object : Transition() {
    init {
      cycleDuration = Duration.seconds(1.0)
      cycleCount = 1
    }
    override fun interpolate(frac: Double) {
      val bb = BoxBlur()
      bb.width = 5.0 * frac
      bb.height = 5.0 * frac
      bb.iterations = 2

      underlayPane.effect = bb
    }
  }
  ParallelTransition(fadeIn, washOut).play()
}

fun createAlertBody(message: String): Node =
    ScrollPane(MDFXNode(message)).also { scroll ->
      scroll.isFitToWidth = true
      scroll.isFitToHeight = true
    }
