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
import javafx.animation.FadeTransition
import javafx.animation.ParallelTransition
import javafx.animation.Transition
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.embed.swing.JFXPanel
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.effect.BoxBlur
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.layout.*
import javafx.util.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.DialogBuilder
import net.sourceforge.ganttproject.action.CancelAction
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.mainWindow
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

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
fun dialogFx(contentBuilder: (DialogController) -> Unit) {
  Platform.runLater {
    Dialog<Unit>().also {
      it.isResizable = true
      val dialogBuildApi = DialogControllerFx(it.dialogPane)
      it.dialogPane.apply {
        styleClass.addAll("dlg")
        stylesheets.addAll("/biz/ganttproject/app/Theme.css", DIALOG_STYLESHEET)

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

fun dialog(title: LocalizedString? = null,  contentBuilder: (DialogController) -> Unit) {
  val jfxPanel = JFXPanel()
  val swingDialogController = AtomicReference<UIFacade.Dialog?>(null)
  Platform.runLater {
    val dialogController = DialogControllerSwing()
    contentBuilder(dialogController)
    jfxPanel.scene = Scene(dialogController.build())
    SwingUtilities.invokeLater {
      val dialogBuilder = DialogBuilder(mainWindow.get())
      dialogBuilder.createDialog(
          jfxPanel,
          arrayOf(CancelAction("close").also { it.putValue(GPAction.HAS_DIALOG_BUTTON, false) }),
          title?.value ?: "", null).also {
        swingDialogController.set(it)
        dialogController.setDialogFrame(it)
      }
    }
  }
}

interface DialogController {
  fun setContent(content: Node)
  fun setupButton(type: ButtonType, code: (Button) -> Unit = {})
  fun showAlert(title: LocalizedString, content: Node)
  fun addStyleClass(vararg styleClass: String)
  fun addStyleSheet(vararg stylesheets: String)
  fun setHeader(header: Node)
  fun hide()
  fun removeButtonBar()
  fun toggleProgress(shown: Boolean): () -> Unit
  fun resize()
  var beforeShow: () -> Unit
  var onShown: () -> Unit
}

class DialogControllerSwing() : DialogController {
  override var beforeShow: () -> Unit = {}
  override var onShown: () -> Unit = {}

  private lateinit var dialogFrame: UIFacade.Dialog
  private val paneBuilder = VBoxBuilder().also {
    it.vbox.styleClass.add("dlg")
    it.vbox.stylesheets.addAll("/biz/ganttproject/app/Theme.css", DIALOG_STYLESHEET)
  }

  private val contentStack = StackPane()
  private lateinit var content: Node
  private var buttonBar: ButtonBar = ButtonBar().also { it.styleClass.add("button-pane") }
  private var buttonBarDisabled = false
  private val buttons = FXCollections.observableArrayList<ButtonType>().also {
    it.addListener(ListChangeListener { c ->
      while (c.next()) {
        if (c.wasRemoved()) {
          c.removed.forEach { buttonType -> buttonNodes.remove(buttonType) }
        }
        if (c.wasAdded()) {
          c.addedSubList.forEach { buttonType ->
            buttonNodes.getOrPut(buttonType) {
              createButton(buttonType)
            }
          }
        }
      }
    })
  }

  private val buttonNodes = WeakHashMap<ButtonType, Button>()

  private var header: Node? = null
  private var isBuilt = false

  internal fun build(): Parent {
    this.header?.let {
      this.paneBuilder.add(it)
    }
    this.content.let {
      this.contentStack.children.add(it)
      this.paneBuilder.add(this.contentStack, alignment = null, growth = Priority.ALWAYS)
    }
    if (!this.buttonBarDisabled) {
      this.buttonBar.let {
        updateButtons(it)
        this.paneBuilder.add(it)
      }
    }
    val defaultButton = this.buttonBar.buttons?.firstOrNull {
      if (it is Button) it.isDefaultButton else false
    }
    this.paneBuilder.vbox.addEventHandler(KeyEvent.KEY_PRESSED) {
      if (it.isControlDown && it.code == KeyCode.ENTER) {
        if (defaultButton is Button) {
          defaultButton.fire()
          it.consume()
        }
      }
      if (it.code == KeyCode.ESCAPE) {
        hide()
        it.consume()
      }
    }
    isBuilt = true
    return this.paneBuilder.vbox
  }

  internal fun setDialogFrame(dlgFrame: UIFacade.Dialog) {
    this.dialogFrame = dlgFrame
    dlgFrame.onShown(this.onShown)
    this.beforeShow()
    this.dialogFrame.show()
  }

  override fun setContent(content: Node) {
    this.content = content
  }

  override fun setupButton(type: ButtonType, code: (Button) -> Unit) {
    buttons.add(type)
    this.buttonNodes[type]?.let {
      code(it)
    }
    if (isBuilt) {
      updateButtons(buttonBar)
    }
  }

  override fun toggleProgress(shown: Boolean): () -> Unit {
    val countDown = CountDownLatch(2)
    GlobalScope.launch(Dispatchers.JavaFx) {
      val transition = createOverlayPane(this@DialogControllerSwing.content, this@DialogControllerSwing.contentStack) {pane ->
        pane.center = Spinner().let {
          it.state = Spinner.State.WAITING
          it.pane
        }
        pane.opacity = 0.5
        pane.styleClass.add("overlay")
      }
      transition.setOnFinished {
        countDown.countDown()
      }
    }
    GlobalScope.launch {
      countDown.await()
      Platform.runLater {
        this@DialogControllerSwing.contentStack.children.removeIf {
          it.styleClass.contains("overlay")
        }
        this@DialogControllerSwing.content.effect = null
      }
    }
    return {
      countDown.countDown()
    }
  }

  override fun resize() {
    SwingUtilities.invokeLater {
      this.dialogFrame.layout()
    }
  }

  override fun showAlert(title: LocalizedString, content: Node) {
    Platform.runLater {
      createAlertPane(this.content, this.contentStack, title, content)
    }
  }

  override fun addStyleClass(vararg styleClass: String) {
    this.paneBuilder.vbox.styleClass.addAll(styleClass)
  }

  override fun addStyleSheet(vararg stylesheets: String) {
    this.paneBuilder.vbox.stylesheets.addAll(stylesheets)
  }

  override fun setHeader(header: Node) {
    this.header = header
  }

  override fun hide() {
    SwingUtilities.invokeLater {
      this.dialogFrame.hide()
    }
  }

  override fun removeButtonBar() {
    this.buttons.clear()
    this.buttonNodes.clear()
    this.buttonBarDisabled = true
    //this.buttonBar = null
  }

  private fun updateButtons(buttonBar: ButtonBar) {
    buttonBar.buttons.clear()
    // show details button if expandable content is present
    var hasDefault = false
    for (cmd in buttons) {
      val button = buttonNodes.computeIfAbsent(cmd, ::createButton)
      // keep only first default button
      val buttonType = cmd.buttonData
      button.isDefaultButton = !hasDefault && buttonType != null && buttonType.isDefaultButton
      button.isCancelButton = buttonType != null && buttonType.isCancelButton
      hasDefault = hasDefault || buttonType != null && buttonType.isDefaultButton
      buttonBar.buttons.add(button)

      button.addEventHandler(ActionEvent.ACTION) { ae: ActionEvent ->
        if (ae.isConsumed) return@addEventHandler
        hide()
      }
    }
  }

  private fun createButton(buttonType: ButtonType): Button {
    val button = Button(buttonType.text)
    val buttonData = buttonType.buttonData
    ButtonBar.setButtonData(button, buttonData)
    button.isDefaultButton = buttonData.isDefaultButton
    button.isCancelButton = buttonData.isCancelButton
    return button
  }

}

class DialogControllerFx(private val dialogPane: DialogPane) : DialogController {
  override var beforeShow: () -> Unit = {}
  override var onShown: () -> Unit = {}
  private val stackPane = StackPane().also { it.styleClass.add("layers") }
  private var content: Node = Region()

  override fun setContent(content: Node) {
    this.content = content
    content.styleClass.add("content-pane")
    this.stackPane.children.add(content)
    this.dialogPane.content = this.stackPane
  }

  override fun setupButton(type: ButtonType, code: (Button) -> Unit) {
    this.dialogPane.buttonTypes.add(type)
    val btn = this.dialogPane.lookupButton(type)
    if (btn is Button) {
      code(btn)
    }
  }

  override fun toggleProgress(shown: Boolean): () -> Unit {
    Platform.runLater {
      createOverlayPane(this.content, this.stackPane) {pane ->
        pane.center = Label("")
        pane.opacity = 0.5
      }
    }
    return {
      this.stackPane.children.removeIf { it.styleClass.contains("overlay") }
    }
  }

  override fun resize() {
  }

  override fun showAlert(title: LocalizedString, content: Node) {
    Platform.runLater {
      createAlertPane(this.content, this.stackPane, title, content)
    }
  }

  override fun addStyleClass(vararg styleClass: String) {
    this.dialogPane.styleClass.addAll(styleClass)
  }

  override fun addStyleSheet(vararg stylesheets: String) {
    this.dialogPane.stylesheets.addAll(stylesheets)
  }

  override fun setHeader(header: Node) {
    header.styleClass.add("header")
    this.dialogPane.header = header
  }

  override fun removeButtonBar() {
    this.dialogPane.children.remove(this.dialogPane.children.first { it.styleClass.contains("button-bar") })
  }

  override fun hide() {
    this.dialogPane.scene.window.hide()
  }
}

fun createOverlayPane(underlayPane: Node, stackPane: StackPane, overlayBuilder: (BorderPane) -> Unit) : Transition {
  val notificationPane = BorderPane().also(overlayBuilder)
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
  return ParallelTransition(fadeIn, washOut).also {
    it.play()
  }
}

fun createAlertPane(underlayPane: Node, stackPane: StackPane, title: LocalizedString, body: Node) {
  createOverlayPane(underlayPane, stackPane) { pane ->
    pane.styleClass.add("alert-glasspane")
    buildAlertPane(title, body, true).let {
      pane.center = it.contents
      it.btnClose?.addEventHandler(ActionEvent.ACTION) {
        stackPane.children.remove(pane)
        underlayPane.effect = null
      }
    }
    pane.opacity = 0.0
  }
}

fun createAlertBody(message: String): Node =
    ScrollPane(MDFXNode(message)).also { scroll ->
      scroll.isFitToWidth = true
      scroll.isFitToHeight = true
    }

val THEME_STYLESHEET = "/biz/ganttproject/app/Theme.css"
val DIALOG_STYLESHEET = "/biz/ganttproject/app/Dialog.css"
