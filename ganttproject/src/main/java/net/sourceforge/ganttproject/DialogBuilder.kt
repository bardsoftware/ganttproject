/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
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
package net.sourceforge.ganttproject

import biz.ganttproject.app.DialogController
import biz.ganttproject.app.dialogFxBuild
import biz.ganttproject.colorFromUiManager
import biz.ganttproject.lib.fx.vbox
import biz.ganttproject.walkTree
import javafx.application.Platform
import javafx.embed.swing.SwingNode
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.input.KeyCombination
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.CornerRadii
import javafx.scene.layout.Priority
import javafx.stage.Screen
import javafx.stage.Stage
import net.sourceforge.ganttproject.action.CancelAction
import net.sourceforge.ganttproject.action.OkAction
import net.sourceforge.ganttproject.gui.UIFacade
import java.awt.BorderLayout
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities


class DialogImplSwingInFx(content: JComponent, private val buttonActions: Array<Action>, private val title: String): UIFacade.Dialog {
  private lateinit var controller: DialogController
  lateinit var dialog: Dialog<Unit>
  private var isCommitted = false
  private var contentPane = JPanel(BorderLayout()).also { it.add(content, BorderLayout.CENTER) }
  private val commandsOnShown = mutableListOf<Runnable>()
  private val commandsOnHidden = mutableListOf<Runnable>()

  private fun addButtons(buttonActions: Array<Action>) {
    var cancelAction: Action? = null
    for (action in buttonActions) {
      when {
        action is OkAction -> {
          controller.setupButton(ButtonType.OK) {
            it.text = "${action.getValue(Action.NAME)}"
            it.onAction = EventHandler { SwingUtilities.invokeLater {
              action.actionPerformed(null)
            }}
          }
        }
        action is CancelAction -> {
          controller.setupButton(ButtonType.CANCEL) {
            it.text = "${action.getValue(Action.NAME)}"
          }
        }
        else -> {
          controller.setupButton(ButtonType("${action.getValue(Action.NAME)}", ButtonBar.ButtonData.OTHER)) {
            it.addEventFilter(javafx.event.ActionEvent.ACTION) {
              it.consume()
              SwingUtilities.invokeLater {
                action.actionPerformed(null)
              }
            }
          }
        }
      }
    }

//    for (action in buttonActions) {
//      val nextButton = when {
//        action is OkAction -> JButton().also { _btn ->
//          val _delegate = action as AbstractAction
//          val proxy: OkAction = object : OkAction() {
//            // These two steps handel the case when focus is somewhere in text input
//            // and user hits Ctrl+Enter
//            // First we want to move focus to OK button to allow focus listeners, if any,
//            // to catch focusLost event
//            // Second, we want it to happen before original OkAction runs
//            // So we wrap original OkAction into proxy which moves focus and schedules "later" command
//            // which call the original action. Between them EDT sends out focusLost events.
//            val myStep2: Runnable = Runnable {
//              Platform.runLater { controller.hide() }
//              isCommitted = true
//              action.actionPerformed(null)
//              _delegate.removePropertyChangeListener(myDelegateListener)
//            }
//            val myStep1: Runnable = Runnable {
//              _btn.requestFocus()
//              SwingUtilities.invokeLater(myStep2)
//            }
//
//            override fun actionPerformed(e: ActionEvent) {
//              SwingUtilities.invokeLater(myStep1)
//            }
//
//            private fun copyValues() {
//              for (key in _delegate.keys) {
//                putValue(key.toString(), _delegate.getValue(key.toString()))
//              }
//              isEnabled = _delegate.isEnabled
//            }
//
//            private val myDelegateListener = PropertyChangeListener { copyValues() }
//
//            init {
//              _delegate.addPropertyChangeListener(myDelegateListener)
//              copyValues()
//            }
//          }
//          _btn.action = proxy
//          //++
////          if (action.isDefault) {
////            dlg.rootPane.defaultButton = nextButton
////          }
//
//        }
//
//        action is CancelAction -> {
//          cancelAction = action
//          (action.getValue(GPAction.HAS_DIALOG_BUTTON) as? Boolean)?.let {
//            JButton(action).also {
//              it.addActionListener(ActionListener { e: ActionEvent? ->
//                controller.hide()
//                isCommitted = true
//              })
//            }
//          }
//          //++
////          dlg.rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
////            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), action.getValue(Action.NAME)
////          )
////          dlg.rootPane.actionMap.put(action.getValue(Action.NAME), object : AbstractAction() {
////            override fun actionPerformed(e: ActionEvent) {
////              action.actionPerformed(e)
////              if (result.isEscCloseEnabled()) {
////                result.hide()
////              }
////            }
////          })
//        }
//
//        else -> JButton(action)
//      }
//      if (nextButton != null) {
//        buttonBox.add(nextButton)
//        buttonCount += 1
//      }
//    }

    cancelAction?.let {cancelAction ->
      controller.onClosed = {
        if (!isCommitted) {
          cancelAction.actionPerformed(null)
        }
      }
    }

//    if (buttonCount > 0) {
//      val buttonPanel = JPanel(BorderLayout())
//      buttonPanel.border = BorderFactory.createEmptyBorder(0, 0, 5, 5)
//      //buttonPanel.add(buttonBox, BorderLayout.EAST)
//      contentPane.add(buttonPanel, BorderLayout.SOUTH)
//    }

  }

  override fun show() {
    Platform.runLater {
      dialog = dialogFxBuild {
        controller = it
        val swingNode = SwingNode()

        it.setContent(vbox {
          add(swingNode, null, Priority.ALWAYS)
        })

        if (buttonActions.isNotEmpty()) {
          addButtons(buttonActions)
        }
        SwingUtilities.invokeLater {
          swingNode.content = contentPane
          Platform.runLater {
            dialog.dialogPane.let {
              it.walkTree { node ->
                if (node is ButtonBar) {
                  node.background = Background(BackgroundFill("Panel.background".colorFromUiManager(), CornerRadii.EMPTY, Insets.EMPTY))
                }
              }
              it.layout()
              it.scene.window.sizeToScene()
            }
          }
        }
      }
      dialog.title = title
      onShown {
        commandsOnShown.forEach { it.run() }
      }
      onClosed { commandsOnHidden.forEach { it.run() } }
      dialog.show()
    }
  }

  override fun hide() = Platform.runLater(controller::hide)

  override fun layout() {
    if (this::dialog.isInitialized) {
      dialog.dialogPane.layout()
    }
  }

  override fun center(centering: UIFacade.Centering) {
    if (this::dialog.isInitialized) {
      val stage = dialog.dialogPane.scene.window as Stage
      centerStage(stage, 600.0, 600.0)
    }
  }

  override fun onShown(onShown: Runnable) {
    if (this::dialog.isInitialized) {
      val prevOnShown = dialog.onShown
      dialog.onShown = EventHandler { evt ->
        prevOnShown?.handle(evt)
        onShown.run()
      }
    } else {
      commandsOnShown += onShown
    }
  }

  override fun onClosed(onClosed: Runnable) {
    if (this::dialog.isInitialized) {
      val prevOnClosed = dialog.onHidden
      dialog.onHidden = EventHandler { evt ->
        prevOnClosed?.handle(evt)
        onClosed.run()
      }
    } else {
      commandsOnHidden += onClosed
    }
  }

  override fun isEscCloseEnabled(): Boolean = dialog.dialogPane.scene.accelerators[KeyCombination.keyCombination("ESC")] != null

  override fun setEscCloseEnabled(value: Boolean) {
    dialog.dialogPane.scene.accelerators[KeyCombination.keyCombination("ESC")] =
      if (value) Runnable { controller.hide() }
      else null
  }

}
fun createDialogFx(content: JComponent, buttonActions: Array<Action>, title: String): UIFacade.Dialog =
  DialogImplSwingInFx(content, buttonActions, title)

private fun centerStage(stage: Stage, width: Double, height: Double) {
  val screenBounds = Screen.getPrimary().getVisualBounds()
  stage.x = (screenBounds.getWidth() - width) / 2
  stage.y = (screenBounds.getHeight() - height) / 2
}