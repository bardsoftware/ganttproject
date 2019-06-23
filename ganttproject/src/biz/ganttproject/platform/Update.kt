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

import biz.ganttproject.app.DefaultLocalizer
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.lib.fx.VBoxBuilder
import com.bardsoftware.eclipsito.update.UpdateMetadata
import com.sandec.mdfx.MDFXNode
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.*
import javafx.scene.effect.BoxBlur
import javafx.scene.input.KeyCombination
import javafx.scene.layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.gui.UIFacade
import java.awt.event.WindowEvent
import java.io.File
import java.util.concurrent.CompletableFuture
import javax.swing.SwingUtilities
import org.eclipse.core.runtime.Platform as Eclipsito


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

/**
 * @author dbarashev@bardsoftware.com
 */
private class UpdateDialog(private val updates: List<UpdateMetadata>, private val uiFacade: UIFacade) {
  private lateinit var dialogPane: DialogPane
  private val version2ui = mutableMapOf<String, UpdateComponentUi>()
  private val stackPane = StackPane()
  private var buttonBar: Parent? = null

  fun createPane(): Node {
    val vboxBuilder = VBoxBuilder("content-pane")
    vboxBuilder.addTitle(i18n.formatText("title"))
    vboxBuilder.add(Label().apply {
      this.text = i18n.formatText("titleHelp", this@UpdateDialog.updates.first().version)
      this.styleClass.add("help")
    })


    val bodyBuilder = VBoxBuilder("updates")
    this.updates.map {
      UpdateComponentUi(it).also { ui ->
        version2ui[it.version] = ui
      }
    }.forEach {
      bodyBuilder.add(it.title)
      bodyBuilder.add(it.subtitle)
      bodyBuilder.add(it.text)
      bodyBuilder.add(it.progress)
    }
    vboxBuilder.add(ScrollPane(bodyBuilder.vbox).also {
      it.styleClass.add("body")
      it.isFitToWidth = true
      it.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
      it.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
    }, Pos.CENTER, Priority.ALWAYS)

    return vboxBuilder.vbox

  }

  fun addContent(dialogPane: DialogPane) {
    this.dialogPane = dialogPane
    dialogPane.buttonTypes.addAll(ButtonType.APPLY, ButtonType.CLOSE)
    dialogPane.lookupButton(ButtonType.APPLY).apply {
      if (this is Button) {
        val btn = this
        buttonBar = btn.parent.also {
          println("button bar=$it")
        }
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

    stackPane.children.add(this.createPane())
//    dialogPane.content = NotificationPane(this.createPane()).also {
//      it.isShowFromTop = true
//      this.notificationPane = it
//    }
    dialogPane.content = stackPane
  }

  private fun onRestart() {
    SwingUtilities.invokeLater {
      uiFacade.mainFrame.dispatchEvent(WindowEvent(uiFacade.mainFrame, WindowEvent.WINDOW_CLOSING))
    }
  }

  private fun onDownload(btn: Button) {
    btn.disableProperty().set(true)
    var installFuture: CompletableFuture<File>? = null
    for (update in updates.reversed()) {
      val progressMonitor: (Int) -> Unit = { percents: Int ->
        this.version2ui[update.version]?.updateProgress(percents)
      }
      installFuture =
          if (installFuture == null) update.install(progressMonitor)
          else installFuture.thenCompose { update.install(progressMonitor) }
    }
    installFuture?.thenAccept {
      GlobalScope.launch(Dispatchers.Main) {
        btn.disableProperty().set(false)
        btn.text = i18n.formatText("platform.update.restart")
        btn.properties["restart"] = true
      }
    }?.exceptionally { ex ->
      GPLogger.logToLogger(ex)
      Platform.runLater {
        val notificationPane = BorderPane().also { pane ->
          val contentPane = this.stackPane.children[0]
          contentPane.apply {
            val bb = BoxBlur()
            bb.width = 5.0
            bb.height = 5.0
            bb.iterations = 3

            this.effect = bb
          }
          pane.styleClass.add("alert-glasspane")
          val vboxBuilder = VBoxBuilder("alert-box")
          vboxBuilder.addTitle(i18n.create("alert.title")).also {hbox ->
            hbox.alignment = Pos.CENTER_LEFT
            hbox.isFillHeight = true
            hbox.children.add(Region().also { node -> HBox.setHgrow(node, Priority.ALWAYS)})
            val btnClose = Button("x")
            hbox.children.add(btnClose)
            btnClose.addEventHandler(ActionEvent.ACTION) {
              this.stackPane.children.remove(pane)
              contentPane.effect = null
            }

          }
          vboxBuilder.add(ScrollPane(MDFXNode(ex.message)).also { scroll ->
            scroll.isFitToWidth = true
            scroll.isFitToHeight = true
          }, Pos.CENTER, Priority.ALWAYS)
          pane.center = vboxBuilder.vbox
        }
        this.stackPane.children.add(notificationPane)

      }
      null
    }
  }

}

private fun (UpdateMetadata).install(monitor: (Int) -> Unit): CompletableFuture<File> {
  return Eclipsito.getUpdater().installUpdate(this) { percents ->
    monitor(percents)
  }
}

private fun (UpdateMetadata).sizeAsString(): String {
  return when {
    this.sizeBytes < (1 shl 10) -> """${this.sizeBytes}b"""
    this.sizeBytes >= (1 shl 10) && this.sizeBytes < (1 shl 20) -> """${this.sizeBytes / (1 shl 10)}KiB"""
    else -> "%.2fMiB".format(this.sizeBytes.toFloat() / (1 shl 20))
  }
}

private class UpdateComponentUi(val update: UpdateMetadata) {
  val title: Label
  val subtitle: Label
  val text: MDFXNode
  val progressText = i18n.create("bodyItem.progress")
  val progress: Label
  var progressValue: Int = -1

  init {
    title = Label(i18n.formatText("bodyItem.title", update.version)).also { l ->
      l.styleClass.add("title")
    }
    subtitle = Label(i18n.formatText("bodyItem.subtitle", update.date, update.sizeAsString())).also { l ->
      l.styleClass.add("subtitle")
    }
    text = MDFXNode(i18n.formatText("bodyItem.description", update.description)).also { l ->
      l.styleClass.add("par")
    }
    progress = Label().also {
      it.textProperty().bind(progressText)
      it.styleClass.add("progress")
      it.isVisible = false
    }
  }

  fun updateProgress(percents: Int) {
    Platform.runLater {
      if (progressValue == -1) {
        this.progress.isVisible = true
      }
      if (progressValue != percents) {
        progressValue = percents
        progressText.update(percents.toString())
        if (progressValue == 100) {
          listOf(title, subtitle, text, progress).forEach { it.opacity = 0.5 }
        }
      }
    }
  }
}

private val i18n = DefaultLocalizer("platform.update", RootLocalizer)
