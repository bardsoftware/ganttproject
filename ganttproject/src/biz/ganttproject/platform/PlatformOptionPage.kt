package biz.ganttproject.platform

import biz.ganttproject.app.DialogController
import biz.ganttproject.core.option.GPOptionGroup
import com.bardsoftware.eclipsito.update.UpdateMetadata
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.control.DialogPane
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
    val dialogPane = DialogPane().also {
      //it.styleClass.addAll("dlg-information", "dlg")
      it.stylesheets.addAll("/biz/ganttproject/app/Theme.css", "/biz/ganttproject/app/Dialog.css")
    }
    val dialogBuildApi = DialogController(dialogPane)
    val updateUi = UpdateDialog(updateMetadata.subList(0,1)) {}
    updateUi.addContent(dialogBuildApi)
    return Scene(dialogPane)
  }

}
