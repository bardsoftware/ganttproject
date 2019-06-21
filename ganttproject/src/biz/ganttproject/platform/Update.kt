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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.gui.UIFacade
import java.awt.event.WindowEvent
import javax.swing.SwingUtilities
import org.eclipse.core.runtime.Platform as Eclipsito

/**
 * @author dbarashev@bardsoftware.com
 */
class UpdateDialog(private val updates: List<UpdateMetadata>, private val uiFacade: UIFacade) {
  private val i18n = DefaultLocalizer("platform.update", RootLocalizer)

  fun createPane(): Pane {
    val vboxBuilder = VBoxBuilder("content-pane")
    vboxBuilder.addTitle(i18n.formatText("title"))
    vboxBuilder.add(Label().apply {
      this.text = i18n.formatText("titleHelp", this@UpdateDialog.updates.first().version)
      this.styleClass.add("help")
    })


    val bodyBuilder = VBoxBuilder("body")
    this.updates.forEach {
      bodyBuilder.add(Label(i18n.formatText("bodyItem.title", it.version)).also { l ->
        l.styleClass.add("title")
      })
      bodyBuilder.add(Label(i18n.formatText("bodyItem.subtitle", it.date, it.sizeAsString())).also { l ->
        l.styleClass.add("subtitle")
      })
      bodyBuilder.add(MDFXNode(i18n.formatText("bodyItem.description", it.description)).also {l ->
        l.styleClass.add("par")
      })
    }
    vboxBuilder.add(bodyBuilder.vbox, Pos.CENTER, Priority.ALWAYS)
    return vboxBuilder.vbox
  }

  fun addContent(dialogPane: DialogPane) {
    dialogPane.buttonTypes.addAll(ButtonType.APPLY, ButtonType.CLOSE)
    dialogPane.lookupButton(ButtonType.APPLY).apply {
      if (this is Button) {
        val btn = this
        ButtonBar.setButtonUniformSize(btn, false)
        styleClass.add("btn-attention")
        text = i18n.formatText("button.ok")
        maxWidth = Double.MAX_VALUE
        addEventFilter(ActionEvent.ACTION) { event ->
          if (btn.properties["restart"] == true) {
            onRestart()
          } else {
            event.consume()
            onDownload(btn)
          }
        }
      }
    }
    dialogPane.content = this.createPane()
  }

  private fun onRestart() {
    SwingUtilities.invokeLater {
      uiFacade.mainFrame.dispatchEvent(WindowEvent(uiFacade.mainFrame, WindowEvent.WINDOW_CLOSING))
    }
  }

  private fun onDownload(btn: Button) {
    Eclipsito.getUpdater().installUpdate(this@UpdateDialog.updates.first()) { percents ->
      println("""Downloaded $percents%""")
      GlobalScope.launch(Dispatchers.Main) {
        btn.text = String.format("Downloaded %d%%", percents)
        btn.disableProperty().set(true)
      }
    }.thenAccept { file ->
      GlobalScope.launch(Dispatchers.Main) {
        btn.disableProperty().set(false)
        btn.text = "Restart GanttProject"
        btn.properties["restart"] = true
      }
    }.exceptionally { ex ->
      GPLogger.log(ex)
      null
    }
  }
}

fun (UpdateMetadata).sizeAsString(): String {
  return when {
    this.sizeBytes < (1 shl 10) -> """${this.sizeBytes}b"""
    this.sizeBytes >= (1 shl 10) && this.sizeBytes < (1 shl 20) -> """${this.sizeBytes / (1 shl 10)}KiB"""
    else -> "%.2fMiB".format(this.sizeBytes.toFloat() / (1 shl 20))
  }
}

fun showUpdateDialog(updates: List<UpdateMetadata>, uiFacade: UIFacade) {
  val dlg = UpdateDialog(updates, uiFacade)
  Platform.runLater {
    Dialog<Unit>().also {
      it.isResizable = true
      it.dialogPane.apply {
        styleClass.addAll("dlg-lock", "dlg-information", "dlg-platform-update")
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
