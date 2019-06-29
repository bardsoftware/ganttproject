package biz.ganttproject.platform

import biz.ganttproject.app.DialogController
import biz.ganttproject.app.LocalizedString
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
import net.sourceforge.ganttproject.client.RssFeedChecker.UPDATE_URL
import net.sourceforge.ganttproject.gui.options.OptionPageProviderBase
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel
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
    Eclipsito.getUpdater().getUpdateMetadata(UPDATE_URL).thenAccept { updateMetadata ->
      if (updateMetadata.isNotEmpty()) {
        Platform.runLater {
          jfxPanel.scene = createScene(updateMetadata)
        }
      }
    }.exceptionally { ex ->
      GPLogger.log(ex)
      null
    }


    return wrapper
  }


  private fun createScene(updateMetadata: List<UpdateMetadata>): Scene {
    val group = BorderPane().also {
      it.styleClass.addAll("dlg-information", "dlg")
      it.stylesheets.addAll("/biz/ganttproject/app/Theme.css", "/biz/ganttproject/app/Dialog.css")
    }
//    val dialogPane = DialogPane().also {
//      it.styleClass.addAll("dlg-information", "dlg")
//      it.stylesheets.addAll("/biz/ganttproject/app/Theme.css", "/biz/ganttproject/app/Dialog.css")
//    }
    val dialogBuildApi = DialogControllerImpl(group)
    val updateUi = UpdateDialog(updateMetadata) {}
    updateUi.addContent(dialogBuildApi)
    return Scene(group)
  }

}

class DialogControllerImpl(private val root: BorderPane) : DialogController {
  private val buttonBar = ButtonBar().also {
    it.maxWidth = Double.MAX_VALUE
  }
  private val stackPane = StackPane().also {
    it.styleClass.add("layers")
    root.center = it
//    it.prefWidthProperty().bind(root.widthProperty())
//    it.prefHeightProperty().bind(root.heightProperty())
    root.bottom = buttonBar
  }

  override fun setContent(content: Node) {
    this.stackPane.children.add(content)
  }

  override fun setupButton(type: ButtonType, code: (Button) -> Unit) {
    val btn = createButton(type)
    this.buttonBar.buttons.add(createButton(type))
    code(btn)
  }

  override fun showAlert(title: LocalizedString, content: Node) {
  }

  override fun addStyleClass(styleClass: String) {
    root.styleClass.add(styleClass)
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

  private fun createButton(buttonType: ButtonType): Button {
    val button = Button(buttonType.text)
    val buttonData = buttonType.buttonData
    ButtonBar.setButtonData(button, buttonData)
    button.isDefaultButton = buttonData.isDefaultButton
    button.isCancelButton = buttonData.isCancelButton

    return button
  }

}
