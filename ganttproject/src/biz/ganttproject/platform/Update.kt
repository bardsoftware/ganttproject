package biz.ganttproject.platform

import biz.ganttproject.app.DefaultLocalizer
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.lib.fx.VBoxBuilder
import com.bardsoftware.eclipsito.update.UpdateMetadata
import com.sandec.mdfx.MDFXNode
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.input.KeyCombination
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.gui.UIFacade
import org.eclipse.core.runtime.Platform as Eclipsito

/**
 * @author dbarashev@bardsoftware.com
 */
class UpdateDialog(private val updates: List<UpdateMetadata>) {
  private val i18n = DefaultLocalizer("platform.update", RootLocalizer)

  fun createPane(): Pane {
    val vboxBuilder = VBoxBuilder("dlg-lock")
    vboxBuilder.addTitle(i18n.formatText("title"))
    vboxBuilder.add(Label().apply {
      this.text = i18n.formatText("titleHelp", this@UpdateDialog.updates.last().version)
      this.styleClass.add("help")
    })
    val body = this.updates.map {
      i18n.formatText("bodyItem", it.version, it.description).trimIndent()
    }.joinToString("\n")
    val mdfx = MDFXNode(body).also {
      it.styleClass.add("signup-body")
    }
    vboxBuilder.add(mdfx, Pos.CENTER, Priority.ALWAYS)
    return vboxBuilder.vbox
  }

  fun addContent(dialogPane: DialogPane) {
    dialogPane.buttonTypes.addAll(ButtonType.OK, ButtonType.CANCEL)
    dialogPane.lookupButton(ButtonType.OK).apply {
      if (this is Button) {
        styleClass.add("btn-attention")
        text = i18n.formatText("button.ok")
        addEventHandler(ActionEvent.ACTION) {
          Eclipsito.getUpdater().installUpdate(this@UpdateDialog.updates.last()) { percents -> println(String.format("Downloading... %d%% done", percents)) }
              .thenAccept { file -> println("Installed into $file") }
              .exceptionally { ex ->
                GPLogger.log(ex)
                null
              }
        }
      }
    }
    dialogPane.content = this.createPane()
  }
}

fun showUpdateDialog(uiFacade: UIFacade, updates: List<UpdateMetadata>) {
  val dlg = UpdateDialog(updates)
  Platform.runLater {
    Dialog<Unit>().also {
      it.isResizable = true
      it.dialogPane.apply {
        styleClass.addAll("dlg-lock")
        stylesheets.addAll("/biz/ganttproject/storage/cloud/GPCloudStorage.css", "/biz/ganttproject/storage/StorageDialog.css")

        dlg.addContent(this)
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
