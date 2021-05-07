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

import biz.ganttproject.app.DialogController
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.createAlertBody
import biz.ganttproject.app.dialog
import biz.ganttproject.core.option.DefaultBooleanOption
import biz.ganttproject.core.option.DefaultStringOption
import biz.ganttproject.core.option.GPOptionGroup
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.createToggleSwitch
import biz.ganttproject.lib.fx.openInBrowser
import biz.ganttproject.platform.PgpUtil.verifyFile
import com.bardsoftware.eclipsito.update.UpdateMetadata
import com.bardsoftware.eclipsito.update.Updater
import com.google.common.base.Strings
import com.sandec.mdfx.MDFXNode
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.GridPane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.gui.UIFacade
import org.controlsfx.control.HyperlinkLabel
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.swing.SwingUtilities
import org.eclipse.core.runtime.Platform as Eclipsito

const val PRIVACY_URL = "https://www.ganttproject.biz/about/privacy"

fun checkAvailableUpdates(updater: Updater, uiFacade: UIFacade) {
  updater.getUpdateMetadata(UpdateOptions.updateUrl.value).thenAccept { updateMetadata ->
    if (updateMetadata.isNotEmpty()) {
      showUpdateDialog(updateMetadata, uiFacade, false)
    }
  }.exceptionally { ex ->
    LOG.error(msg = "Failed to fetch updates from {}", UpdateOptions.updateUrl.value, exception = ex)
    null
  }
}

private fun showUpdateDialog(updates: List<UpdateMetadata>, uiFacade: UIFacade, showSkipped: Boolean = false) {
  val runningVersion = Eclipsito.getUpdater().installedUpdateVersions.maxOrNull() ?: "2900"
  val cutoffVersion = listOf(UpdateOptions.latestShownVersion.value ?: runningVersion, runningVersion).maxOrNull()
  val latestShownUpdateMetadata = UpdateMetadata(
      cutoffVersion,
      null, null, null, 0, "")
  val visibleUpdates = updates
      .filter { showSkipped || Strings.nullToEmpty(latestShownUpdateMetadata.version).isEmpty() || it > latestShownUpdateMetadata }
  if (visibleUpdates.isNotEmpty()) {
    val runningUpdateMetadata = UpdateMetadata(
      runningVersion,
      null, null, null, 0, "")
    val applyUpdates = updates.filter { it > runningUpdateMetadata }
    val dlg = UpdateDialog(applyUpdates, visibleUpdates) {
      SwingUtilities.invokeLater {
        uiFacade.quitApplication(false)
        org.eclipse.core.runtime.Platform.restart()
      }
    }
    dialog(
        title = RootLocalizer.create("platform.update.hasUpdates.title"),
        contentBuilder = dlg::addContent
    )
  }
}

typealias AppRestarter = () -> Unit
data class PlatformBean(var checkUpdates: Boolean = true, val version: String)
/**
 * @author dbarashev@bardsoftware.com
 */
internal class UpdateDialog(
    private val updates: List<UpdateMetadata>,
    private val visibleUpdates: List<UpdateMetadata>,
    private val restarter: AppRestarter) {
  private lateinit var dialogApi: DialogController
  private val version2ui = mutableMapOf<String, UpdateComponentUi>()
  private val hasUpdates: Boolean get() = this.visibleUpdates.isNotEmpty()

  fun createPane(bean: PlatformBean): Node {
    val bodyBuilder = VBoxBuilder("content-pane")

    val props = GridPane().also { it.styleClass.add("props") }
    props.add(Label(ourLocalizer.formatText("installedVersion")).also {
      GridPane.setMargin(it, Insets(5.0, 10.0, 3.0, 0.0))
    }, 0, 0)
    props.add(Label(bean.version).also {
      GridPane.setMargin(it, Insets(5.0, 0.0, 3.0, 0.0))
    }, 1, 0)
    props.add(Label(ourLocalizer.formatText("checkUpdates")).also {
      GridPane.setMargin(it, Insets(5.0, 10.0, 3.0, 0.0))
    }, 0, 1)
    val toggleSwitch = createToggleSwitch().also {
      it.selectedProperty().value = UpdateOptions.isCheckEnabled.value
      it.selectedProperty().addListener { _, _, newValue -> UpdateOptions.isCheckEnabled.value = newValue }
    }
    props.add(toggleSwitch, 1, 1)
    props.add(HyperlinkLabel(ourLocalizer.formatText("checkUpdates.helpline")).also {
      it.styleClass.add("helpline")
      it.onAction = EventHandler { openInBrowser(PRIVACY_URL) }
    }, 1, 2)
    props.add(
      Label(if (this.hasUpdates) ourLocalizer.formatText("availableUpdates") else ourLocalizer.formatText("noUpdates.title")).also {
        GridPane.setMargin(it, Insets(30.0, 0.0, 5.0, 0.0))
      },
      0, 3)
    bodyBuilder.add(props)

    if (this.hasUpdates) {
      val updateBox = VBoxBuilder()
      this.visibleUpdates
          .map {
            UpdateComponentUi(it).also { ui ->
              version2ui[it.version] = ui
            }
          }.forEach {
            updateBox.add(it.title)
            updateBox.add(it.subtitle)
            updateBox.add(it.text)
            updateBox.add(it.progress)
          }

      bodyBuilder.add(ScrollPane(updateBox.vbox.also {
        it.styleClass.add("body")
      }).also {
        it.isFitToWidth = true
        it.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        it.vbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
      })
    }
    return bodyBuilder.vbox
  }

  fun addContent(dialogApi: DialogController) {
    val installedVersion = Eclipsito.getUpdater().installedUpdateVersions.maxOrNull() ?: "2900"
    val bean = PlatformBean(true, installedVersion)
    this.dialogApi = dialogApi
    dialogApi.addStyleClass("dlg-platform-update")
    dialogApi.addStyleSheet(
        "/biz/ganttproject/platform/Update.css",
        "/biz/ganttproject/storage/StorageDialog.css",
        "/biz/ganttproject/storage/cloud/GPCloudStorage.css")

    dialogApi.setHeader(VBoxBuilder("header").apply {
      addTitle(ourLocalizer.formatText(
          if (this@UpdateDialog.hasUpdates) "hasUpdates.title" else "noUpdates.title")
      )
      add(Label().apply {
        this.styleClass.add("help")
        if (this@UpdateDialog.hasUpdates) {
          this.text = ourLocalizer.formatText("hasUpdates.titleHelp",
              installedVersion,
              this@UpdateDialog.updates.first().version
          )
        }
      })
    }.vbox)

    val downloadCompleted = SimpleBooleanProperty(false)
    if (this.hasUpdates) {
      dialogApi.setupButton(ButtonType.APPLY) { btn ->
        ButtonBar.setButtonUniformSize(btn, false)
        btn.styleClass.add("btn-attention")
        btn.text = ourLocalizer.formatText("button.ok")
        btn.maxWidth = Double.MAX_VALUE
        btn.addEventFilter(ActionEvent.ACTION) { event ->
          if (btn.properties["restart"] == true) {
            onRestart()
          } else {
            event.consume()
            btn.disableProperty().set(true)
            onDownload(downloadCompleted)
          }
        }
        downloadCompleted.addListener { _, _, newValue ->
          if (newValue) {
            btn.disableProperty().set(false)
            btn.text = ourLocalizer.formatText("restart")
            btn.properties["restart"] = true
          }
        }
      }
    }
    dialogApi.setupButton(ButtonType.CLOSE) { btn ->
      btn.styleClass.add("btn")
      btn.text = ourLocalizer.formatText("button.close_skip")
      btn.addEventFilter(ActionEvent.ACTION) {
        UpdateOptions.latestShownVersion.value = this.updates.first().version
      }
      downloadCompleted.addListener { _, _, newValue ->
        if (newValue) {
          btn.text = ourLocalizer.formatText("close")
        }
      }
    }
    dialogApi.setContent(this.createPane(bean))
  }

  private fun onRestart() {
    this.restarter()
  }

  private fun onDownload(completed: SimpleBooleanProperty) {
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
        completed.value = true
      }
    }?.exceptionally { ex ->
      GPLogger.logToLogger(ex)
      this.dialogApi.showAlert(ourLocalizer.create("alert.title"), createAlertBody(ex.message ?: ""))
      null
    }
  }
}

private fun (UpdateMetadata).install(monitor: (Int) -> Unit): CompletableFuture<File> {
  return Eclipsito.getUpdater().installUpdate(this, monitor) { dataFile ->
    if (this.signatureBody.isNullOrBlank()) {
      true
    } else {
      verifyFile(dataFile, Base64.getDecoder().wrap(this.signatureBody.byteInputStream()))
      true
    }
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
  val progressText = ourLocalizer.create("bodyItem.progress")
  val progress: Label
  var progressValue: Int = -1

  init {
    title = Label(ourLocalizer.formatText("bodyItem.title", update.version)).also { l ->
      l.styleClass.add("title")
    }
    subtitle = Label(ourLocalizer.formatText("bodyItem.subtitle", update.date, update.sizeAsString())).also { l ->
      l.styleClass.add("subtitle")
    }
    text = MDFXNode(ourLocalizer.formatText("bodyItem.description", update.description)).also { l ->
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

private val LOG = GPLogger.create("App.Update")
private val ourLocalizer = RootLocalizer.createWithRootKey("platform.update")

object UpdateOptions {
  val isCheckEnabled = DefaultBooleanOption("checkEnabled", true)
  val latestShownVersion = DefaultStringOption("latestShownVersion")
  val updateUrl = DefaultStringOption("url", System.getProperty("platform.update.url", "https://www.ganttproject.biz/dl/updates/ganttproject-3.0.json"))
  val optionGroup: GPOptionGroup = GPOptionGroup("platform.update", isCheckEnabled, latestShownVersion, updateUrl)

}
