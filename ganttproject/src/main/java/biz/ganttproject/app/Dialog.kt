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

import biz.ganttproject.FXUtil
import biz.ganttproject.centerOnOwner
import biz.ganttproject.colorFromUiManager
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.printCss
import biz.ganttproject.walkTree
import com.sandec.mdfx.MDFXNode
import javafx.animation.FadeTransition
import javafx.animation.ParallelTransition
import javafx.animation.Transition
import javafx.application.Platform
import javafx.embed.swing.SwingNode
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.effect.BoxBlur
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.layout.*
import javafx.scene.shape.Rectangle
import javafx.stage.*
import javafx.util.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.action.CancelAction
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.action.OkAction
import net.sourceforge.ganttproject.gui.UIFacade
import java.util.Stack
import java.util.concurrent.CountDownLatch
import javax.swing.SwingUtilities
import kotlin.math.max

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
fun dialogFx(title: String? = null, owner: Window? = DialogPlacement.applicationWindow, id: String? = null, contentBuilder: (DialogController) -> Unit) {
  dialogFxBuild(owner, id,  contentBuilder).also {dlg ->
    title?.let { dlg.title = it }
    dlg.show()
  }
}

fun dialogFxBuild(owner: Window? = null, id: String? = null, contentBuilder: (DialogController) -> Unit): Dialog<Unit> =
  Dialog<Unit>().apply {
    val dialogPaneExt = DialogPaneExt()
    dialogPane = dialogPaneExt
    owner?.let(::initOwner)
    initModality(Modality.APPLICATION_MODAL)

    DialogControllerFx(dialogPaneExt, this).let { dialogBuildApi ->
      dialogPane.styleClass.addAll("dlg")
      dialogPane.stylesheets.addAll(DIALOG_STYLESHEET)
      dialogBuildApi.setEscCloseEnabled(true)
      contentBuilder(dialogBuildApi)

      when (dialogBuildApi.frameStyle) {
        FrameStyle.NATIVE_FRAME -> initStyle(StageStyle.DECORATED)
        FrameStyle.NO_FRAME -> initStyle(StageStyle.UNDECORATED)
      }
      dialogPane.scene.window.let { window ->
        dialogBuildApi.onShown = {
          id?.let(DialogPlacement::getBounds)?.let {
            window.x = it.x
            window.y = it.y
            window.width = it.width
            window.height = it.height
            dialogPane.prefWidth = it.width
            dialogPane.prefHeight = it.height
          } ?: run {
            owner?.let {
              centerOnOwner(window, it)
            }
          }
          id?.let {
            window.onHiding = EventHandler {
              DialogPlacement.setBounds(id, Rectangle(window.x, window.y, window.width, window.height))
            }
          }
          window.onCloseRequest = EventHandler {
            window.hide()
          }
          isResizable = true
        }
      }
    }

    dialogPane.apply {
      styleClass.addAll("dlg")
      stylesheets.addAll(DIALOG_STYLESHEET)
    }

  }

class DialogPaneExt : DialogPane() {
  lateinit var buttonBar: HBox
  private lateinit var errorPaneWrapper: StackPane

  fun setButtonBarNode(node: Node) {
    if (!errorPaneWrapper.children.contains(node)) {
      errorPaneWrapper.children.add(node)
    }
    errorPaneWrapper.styleClass.remove("hide")
    errorPaneWrapper.isManaged = true
  }
  override fun createButtonBar(): Node {
    val buttonBar = StackPane(super.createButtonBar()).also {
      it.minWidth = Region.USE_PREF_SIZE
    }
    errorPaneWrapper = StackPane().also {
      it.styleClass.addAll("hide")
      it.isManaged = false
    }
    return HBox().apply {
      styleClass.addAll("button-pane")
      minWidth = Region.USE_PREF_SIZE
      HBox.setHgrow(buttonBar, Priority.ALWAYS)
      HBox.setHgrow(errorPaneWrapper, Priority.SOMETIMES)
      children.addAll(errorPaneWrapper, buttonBar)
    }.also {
      this.buttonBar = it
    }
  }
}
fun dialog(title: String? = null,  id: String? = null, contentBuilder: (DialogController) -> Unit) {
  Platform.runLater {
    try {
      dialogFx(title = title, id = id, contentBuilder = contentBuilder)
    } catch (ex: Exception) {
      ex.printStackTrace()
    }
  }
}


enum class FrameStyle {
  NO_FRAME, NATIVE_FRAME
}
interface DialogController {
  fun setContent(content: Node)
  fun setupButton(type: ButtonType, code: (Button) -> Unit = {}): Button?
  fun setupButton(action: GPAction, code: (Button) -> Unit = {}): Button?
  fun showAlert(title: LocalizedString, content: Region)
  fun showAlert(title: String, content: Region)
  fun addStyleClass(vararg styleClass: String)
  fun addStyleSheet(vararg stylesheets: String)
  fun setHeader(header: Node)
  fun hide()
  fun setButtonPaneNode(content: Node)
  fun removeButtonBar()
  fun toggleProgress(shown: Boolean): () -> Unit
  fun resize()

  fun setEscCloseEnabled(value: Boolean)
  fun walkTree(walker: (Node)->Unit)
  var frameStyle: FrameStyle
  var beforeShow: () -> Unit
  var onShown: () -> Unit
  var onClosed: () -> Unit
}

/**
 * This is an implementation of a DialogController on top of a Swing dialog with embedded
 * JavaFX components.
 */
class DialogControllerSwing : DialogController {
  private var buttonPaneNode: Node? = null
  override var beforeShow: () -> Unit = {}
  override var onShown: () -> Unit = {}
  override var onClosed: () -> Unit = {}
  set(value) {
    if (this::dialogFrame.isInitialized) {
      this.dialogFrame.onClosed(value)
    }
  }

  private lateinit var dialogFrame: UIFacade.Dialog
  private val paneBuilder = VBoxBuilder().also {
    it.vbox.styleClass.add("dlg")
    it.vbox.stylesheets.addAll(DIALOG_STYLESHEET)
  }

  private val contentStack = StackPane()
  private lateinit var content: Node
  private var buttonBar: ButtonBar = ButtonBar().also {
    it.maxWidth = Double.MAX_VALUE
  }
  private var buttonBarDisabled = false
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
      this.paneBuilder.add(
        this.buttonPaneNode?.let { buttonPaneNode ->
          BorderPane().also {
            it.styleClass.add("button-pane")
            //HBox.setHgrow(this.buttonBar, Priority.ALWAYS)
            it.center = this.buttonBar
            it.left = buttonPaneNode
          }
        } ?: run {
          BorderPane().also {
            it.styleClass.add("button-pane")
            //HBox.setHgrow(this.buttonBar, Priority.ALWAYS)
            it.center = this.buttonBar
          }
        }
      )
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
//      if (it.code == KeyCode.ESCAPE) {
//        hide()
//        it.consume()
//      }
    }
    isBuilt = true
    return this.paneBuilder.vbox
  }

  internal fun setDialogFrame(dlgFrame: UIFacade.Dialog) {
    this.dialogFrame = dlgFrame
    dlgFrame.onShown(this.onShown)
    dlgFrame.onClosed(this.onClosed)
    this.beforeShow()
    this.dialogFrame.show()
  }

  override fun setContent(content: Node) {
    this.content = content
  }

  override fun setupButton(type: ButtonType, code: (Button) -> Unit): Button? {
//    buttons.add(type)
//    this.buttonNodes[type]?.let {
//      code(it)
//    }
    return createButton(buttonType = type).also {
      code(it)
      buttonBar.buttons.add(it)
    }
//    if (isBuilt) {
//      updateButtons(buttonBar)
//    }
  }

  override fun setupButton(action: GPAction, code: (Button)->Unit): Button? {
    TODO("Not yet implemented")
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
    Platform.runLater {
      buttonBar.layout()
    }
  }

  override fun setEscCloseEnabled(value: Boolean) {
    this.dialogFrame.isEscCloseEnabled = value
  }

  override fun walkTree(walker: (Node) -> Unit) {
    TODO("Not yet implemented")
  }

  override var frameStyle: FrameStyle
    get() = FrameStyle.NATIVE_FRAME
    set(value) {}

  override fun showAlert(title: LocalizedString, content: Region) {
    Platform.runLater {
      createAlertPane(this.content, this.contentStack, title, content)
    }
  }

  override fun showAlert(title: String, content: Region) {
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

  override fun setButtonPaneNode(content: Node) {
    this.buttonPaneNode = content
  }

  override fun removeButtonBar() {
//    this.buttons.clear()
//    this.buttonNodes.clear()
    this.buttonBarDisabled = true
    //this.buttonBar = null
  }

//  private fun updateButtons(buttonBar: ButtonBar) {
//    buttonBar.buttons.clear()
//    // show details button if expandable content is present
//    var hasDefault = false
//    for (cmd in buttons) {
//      val button = buttonNodes.computeIfAbsent(cmd, ::createButton)
//      // keep only first default button
//      val buttonType = cmd.buttonData
//      button.isDefaultButton = !hasDefault && buttonType != null && buttonType.isDefaultButton
//      button.isCancelButton = buttonType != null && buttonType.isCancelButton
//      hasDefault = hasDefault || buttonType != null && buttonType.isDefaultButton
//      buttonBar.buttons.add(button)
//
//      button.addEventHandler(ActionEvent.ACTION) { ae: ActionEvent ->
//        if (ae.isConsumed) return@addEventHandler
//        hide()
//      }
//    }
//  }

  private fun createButton(buttonType: ButtonType): Button {
    val button = Button(buttonType.text)
    val buttonData = buttonType.buttonData
    ButtonBar.setButtonData(button, buttonData)
    button.isDefaultButton = buttonData.isDefaultButton
    button.isCancelButton = buttonData.isCancelButton
    ButtonBar.setButtonUniformSize(button, false)
    return button
  }

}

/**
 * This is an implementation of a DialogController on top of JavaFX dialog.
 */
class DialogControllerFx(private val dialogPane: DialogPaneExt, private val dialog: Dialog<Unit>) : DialogController {
  override var frameStyle: FrameStyle = FrameStyle.NATIVE_FRAME

  override var beforeShow: () -> Unit = {}
  override var onShown: () -> Unit = {}
    set(value) {
      val prevValue = field
      field = {
        prevValue()
        value()
      }
    }
  override var onClosed: () -> Unit = {}
  private val stackPane = StackPane().also {
    it.styleClass.add("layers")
  }
  private var content: Node = Region()
  private var cancelAction: CancelAction? = null

  init {
    dialog.onShowing = EventHandler{ beforeShow() }
    dialog.onShown = EventHandler { onShown() }
    dialog.onHidden = EventHandler { onClosed() }
  }
  override fun setContent(content: Node) {
    this.content = content
    content.styleClass.add("content-pane")
    this.dialogPane.content = this.stackPane
    this.onShown = {
      this.stackPane.children.add(content)
      this.resize()
    }
  }

  override fun setupButton(type: ButtonType, code: (Button) -> Unit): Button? {
    this.dialogPane.buttonTypes.add(type)
    val btn = this.dialogPane.lookupButton(type)
    if (btn is Button) {
      btn.font = applicationFont.value
      code(btn)
      return btn
    }
    return null
  }

  override fun setupButton(action: GPAction, code: (Button) -> Unit): Button? {
    val buttonType = when (action) {
      is OkAction -> ButtonType.OK
      is CancelAction -> ButtonType.CANCEL
      else -> ButtonType(action.name)
    }
    if (action is CancelAction) {
      this.cancelAction = action
    }
    return setupButton(buttonType) { btn ->
      btn.text = action.name
      btn.onAction = EventHandler {
        action.actionPerformed(null)
      }
      btn.styleClass.add("btn")
      when (buttonType) {
        ButtonType.OK -> btn.styleClass.add("btn-attention")
        ButtonType.CANCEL -> btn.styleClass.addAll("btn-regular", "secondary")
      }
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
    dialogPane.minWidth = dialogPane.width
    dialogPane.buttonBar.minWidth = max(dialogPane.buttonBar.minWidth, dialogPane.width)
    dialogPane.layout()
    dialogPane.scene?.window?.sizeToScene()
  }

  override fun setEscCloseEnabled(value: Boolean) {
    this.dialog.dialogPane.scene.addEventFilter(KeyEvent.KEY_PRESSED) { evt ->
      if (evt.code == KeyCode.ESCAPE) {
        evt.consume()
        hide()
      }
    }
    this.dialog.dialogPane.scene.accelerators[KeyCombination.keyCombination("ESC")] = Runnable { hide() }
    this.cancelAction?.actionPerformed(null)
  }

  override fun walkTree(walker: (Node) -> Unit) {
    this.dialogPane.walkTree(walker)
  }

  override fun showAlert(title: LocalizedString, content: Region) {
    Platform.runLater {
      createAlertPane(this.content, this.stackPane, title, content)
    }
  }

  override fun showAlert(title: String, content: Region) {
    FXUtil.runLater {
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
    FXUtil.runLater {
      header.styleClass.add("header")
      this.dialogPane.header = header
    }
  }

  override fun removeButtonBar() {
    this.dialogPane.children.firstOrNull { it.styleClass.contains("button-bar") }?.let(this.dialogPane.children::remove)
  }

  override fun hide() {
    Platform.runLater {
      this.dialog.hide()
      this.dialogPane.scene?.window?.hide()
    }
  }

  override fun setButtonPaneNode(content: Node) {
    dialogPane.setButtonBarNode(content)
  }
}

/**
 * This implementation provides DialogController interface on top of BorderPane.
 * It is useful when the same component is used both as a standalone dialog and as
 * a part of a more complex window. Examples are "Cloud Document Properties" and "App Update"
 * panes.
 */
class DialogControllerPane(private val root: BorderPane) : DialogController {
  private var buttonPaneNodeWrapper = StackPane()
  override var beforeShow: () -> Unit = {}
  override var onShown: () -> Unit = {}
  override var onClosed: () -> Unit = {}

  private lateinit var contentNode: Node
  private val buttonBar = ButtonBar().also {
    it.maxWidth = Double.MAX_VALUE
  }
  private val stackPane = StackPane().also {
    it.styleClass.add("layers")
    root.center = it
    root.bottom = HBox().also {
        it.styleClass.add("button-pane")
        HBox.setHgrow(this.buttonBar, Priority.ALWAYS)
        it.children.addAll(this.buttonPaneNodeWrapper, this.buttonBar)
      }
    }

  override fun setContent(content: Node) {
    this.contentNode = content
    content.styleClass.add("content-pane")
    this.stackPane.children.add(content)
  }

  override fun setupButton(type: ButtonType, code: (Button) -> Unit): Button? {
//    if (type == ButtonType.APPLY) {
      val btn = createButton(type)
      code(btn)
      this.buttonBar.buttons.add(btn)
      return btn
//    } else {
//      return null
//    }
  }

  override fun setupButton(action: GPAction, code: (Button)->Unit): Button? {
    TODO("Not yet implemented")
  }

  override fun showAlert(title: LocalizedString, content: Region) {
    Platform.runLater {
      createAlertPane(this.contentNode, this.stackPane, title, content)
    }
  }

  override fun showAlert(title: String, content: Region) {
    Platform.runLater {
      createAlertPane(this.contentNode, this.stackPane, title, content)
    }
  }

  override fun addStyleClass(vararg styleClass: String) {
    root.styleClass.addAll(styleClass)
  }

  override fun addStyleSheet(vararg stylesheets: String) {
    root.stylesheets.addAll(stylesheets)
  }

  override fun setHeader(header: Node) {
    root.top = header.also { it.styleClass.add("header") }
  }

  override fun hide() {
  }

  override fun setButtonPaneNode(node: Node) {
    this.buttonPaneNodeWrapper.children.clear()
    this.buttonPaneNodeWrapper.children.add(node)
  }

  override fun removeButtonBar() {
    root.bottom = null
  }

  override fun toggleProgress(shown: Boolean): () -> Unit {
    return {}
  }

  override fun resize() {
    this.buttonBar.layout()
    this.root.layout()
  }

  override fun setEscCloseEnabled(value: Boolean) {
    TODO("Not yet implemented")
  }

  override fun walkTree(walker: (Node) -> Unit) {
    TODO("Not yet implemented")
  }

  override var frameStyle: FrameStyle
    get() = FrameStyle.NATIVE_FRAME
    set(value) {}

  private fun createButton(buttonType: ButtonType): Button {
    val button = Button(buttonType.text)
    val buttonData = buttonType.buttonData
    ButtonBar.setButtonData(button, buttonData)
    button.isDefaultButton = buttonData.isDefaultButton
    button.isCancelButton = buttonData.isCancelButton

    return button
  }
}

// ----------------------------------------------------------------------------
// Overlay pane and alert panes

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
  doCreateAlertPane(underlayPane, stackPane, { it.addTitle(title) }, body)
}

fun createAlertPane(underlayPane: Node, stackPane: StackPane, title: String, body: Node) {
  doCreateAlertPane(underlayPane, stackPane, { it.addTitleString(title) }, body)
}

private fun doCreateAlertPane(underlayPane: Node, stackPane: StackPane, title: (VBoxBuilder)->HBox, body: Node) {
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

fun createAlertBody(message: String): Region =
    ScrollPane(MDFXNode(message)).also { scroll ->
      scroll.isFitToWidth = true
      scroll.isFitToHeight = true
    }

fun createAlertBody(ex: Exception) = createAlertBody(ex.message ?: "")

// ----------------------------------------------------------------------------
// This object keeps the dialog window dimensions and position on the screen.
object DialogPlacement {
  private val id2bounds = mutableMapOf<String, Rectangle>()
  fun getBounds(dialogId: String): Rectangle? = id2bounds[dialogId]
  fun setBounds(dialogId: String, bounds: Rectangle) {
    id2bounds[dialogId] = bounds
  }

  var applicationWindow: Window? = null
}

const val DIALOG_STYLESHEET = "/biz/ganttproject/app/Dialog.css"
private val SWING_BACKGROUND_STYLES = setOf("tab-header-background", "tab-contents", "swing-background")
fun DialogController.setSwingBackground() {
  val background = Background(
    BackgroundFill(
      "Panel.background".colorFromUiManager(), CornerRadii.EMPTY, Insets.EMPTY
    )
  )

  walkTree {
    if (it is Region && it.styleClass.intersect(SWING_BACKGROUND_STYLES).isNotEmpty()) {
      it.background = background
    }
  }
}

fun main() {
  class PureFxTestApp : javafx.application.Application() {
    override fun start(primaryStage: javafx.stage.Stage) {
      val btnOpen = Button("Open Pure JavaFX Dialog")
      btnOpen.onAction = EventHandler {
        val dialog = Dialog<Unit>()
        dialog.title = "Pure JavaFX Layout Test"

        val pane = object : DialogPane() {
          override fun createButtonBar(): Node {
            // This mimics the structure in DialogPaneExt
            val buttonBar = super.createButtonBar()
            val errorPaneWrapper = StackPane().apply {
              isManaged = false
              isVisible = false
            }
            return HBox(10.0).apply {
              styleClass.add("button-pane")
              HBox.setHgrow(buttonBar, Priority.ALWAYS)
              children.addAll(errorPaneWrapper, buttonBar)
            }
          }
        }

//        pane.content = Label("This dialog uses pure JavaFX components.\nCheck if the buttons below are clipped.").apply {
//          padding = Insets(20.0)
////          minWidth = 200.0
//        }
        pane.content = StackPane()//SwingNode()

        pane.buttonTypes.addAll(
          ButtonType.OK,
          ButtonType.APPLY,
          ButtonType.CANCEL,
          ButtonType("Very Long Button Label")
        )

        dialog.dialogPane = pane
        dialog.showAndWait()
      }

      primaryStage.scene = javafx.scene.Scene(StackPane(btnOpen), 300.0, 200.0)
      primaryStage.show()
    }
  }
  javafx.application.Application.launch(PureFxTestApp::class.java)
}