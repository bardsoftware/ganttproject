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
package biz.ganttproject.platform

import biz.ganttproject.app.DIALOG_STYLESHEET
import biz.ganttproject.app.DialogController
import biz.ganttproject.app.LocalizedString
import biz.ganttproject.app.createAlertPane
import biz.ganttproject.core.option.GPOptionGroup
import com.bardsoftware.eclipsito.update.UpdateMetadata
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.scene.layout.BorderPane
import javafx.scene.layout.StackPane
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.gui.options.OptionPageProviderBase
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel
import javax.swing.SwingUtilities
import org.eclipse.core.runtime.Platform as Eclipsito

/**
 * @author dbarashev@bardsoftware.com
 */
class PlatformOptionPageProvider : OptionPageProviderBase("platform") {
  override fun getOptionGroups(): Array<GPOptionGroup> {
    return arrayOf()
  }

  override fun hasCustomComponent(): Boolean  = true

  override fun buildPageComponent(): Component {
    val jfxPanel = JFXPanel()
    val wrapper = JPanel(BorderLayout())
    wrapper.add(jfxPanel, BorderLayout.CENTER)
    Eclipsito.getUpdater().getUpdateMetadata(UpdateOptions.updateUrl.value).thenAccept { updateMetadata ->
        Platform.runLater {
          jfxPanel.scene = createScene(updateMetadata)
        }
    }.exceptionally { ex ->
      GPLogger.log(ex)
      null
    }


    return wrapper
  }


  private fun createScene(updateMetadata: List<UpdateMetadata>): Scene {
    val runningVersion = Eclipsito.getUpdater().installedUpdateVersions.maxOrNull() ?: "2900"
    val runningUpdateMetadata = UpdateMetadata(
      runningVersion,
      null, null, null, 0, "")
    val filteredUpdates = updateMetadata.filter { it > runningUpdateMetadata }

    val group = BorderPane().also {
      it.styleClass.addAll("dlg-information", "dlg", "dialog-pane", "border-etched")
      it.stylesheets.addAll("/biz/ganttproject/app/Theme.css", DIALOG_STYLESHEET)
    }
    val dialogBuildApi = DialogControllerImpl(group)
    val updateUi = UpdateDialog(filteredUpdates, filteredUpdates) {
      SwingUtilities.invokeLater {
        uiFacade.quitApplication(false)
        org.eclipse.core.runtime.Platform.restart()
      }
    }
    updateUi.addContent(dialogBuildApi)
    return Scene(group)
  }

}

class DialogControllerImpl(private val root: BorderPane) : DialogController {
  override var beforeShow: () -> Unit = {}
  override var onShown: () -> Unit = {}
  private lateinit var contentNode: Node
  private val buttonBar = ButtonBar().also {
    it.maxWidth = Double.MAX_VALUE
  }
  private val stackPane = StackPane().also {
    it.styleClass.add("layers")
    root.center = it
    root.bottom = buttonBar
  }

  override fun setContent(content: Node) {
    this.contentNode = content
    content.styleClass.add("content-pane")
    this.stackPane.children.add(content)
  }

  override fun setupButton(type: ButtonType, code: (Button) -> Unit) {
    if (type == ButtonType.APPLY) {
      val btn = createButton(type)
      code(btn)
      this.buttonBar.buttons.add(btn)
    }
  }

  override fun showAlert(title: LocalizedString, content: Node) {
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

  override fun removeButtonBar() {
  }

  override fun toggleProgress(shown: Boolean): () -> Unit {
    return {}
  }

  override fun resize() {

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
