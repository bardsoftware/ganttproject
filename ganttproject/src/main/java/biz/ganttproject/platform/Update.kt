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

import biz.ganttproject.FXUtil
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
import biz.ganttproject.lib.fx.vbox
import biz.ganttproject.platform.PgpUtil.verifyFile
import com.bardsoftware.eclipsito.update.UpdateIntegrityChecker
import com.bardsoftware.eclipsito.update.UpdateMetadata
import com.bardsoftware.eclipsito.update.UpdateProgressMonitor
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
import javafx.scene.layout.*
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.gui.UIFacade
import org.controlsfx.control.HyperlinkLabel
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import javax.swing.SwingUtilities
import org.eclipse.core.runtime.Platform as Eclipsito

const val PRIVACY_URL = "https://www.ganttproject.biz/about/privacy"
const val UPGRADE_URL = "https://www.ganttproject.biz/download/upgrade"

fun checkAvailableUpdates(updater: Updater, uiFacade: UIFacade) {
  if (UpdateOptions.isCheckEnabled.value) {
    LOG.debug("Fetching updates from {}", UpdateOptions.updateUrl.value)
    updater.getUpdateMetadata(UpdateOptions.updateUrl.value).thenAccept { updateMetadata ->
      if (updateMetadata.isNotEmpty()) {
        showUpdateDialog(updateMetadata, uiFacade, false)
      }
    }.exceptionally { ex ->
      LOG.error(msg = "Failed to fetch updates from {}", UpdateOptions.updateUrl.value, exception = ex)
      null
    }
  } else {
    LOG.debug("Updates are disabled")
  }
}

private fun showUpdateDialog(updates: List<UpdateMetadata>, uiFacade: UIFacade, showSkipped: Boolean = false) {
  val runningVersion = Eclipsito.getUpdater().installedUpdateVersions.maxOrNull() ?: "2900"
  val cutoffVersion = listOf(UpdateOptions.latestShownVersion.value ?: runningVersion, runningVersion).maxOrNull()
  val latestShownUpdateMetadata = UpdateMetadata(
      cutoffVersion,
      null, null, null, 0, "", false)
  val visibleUpdates = updates
      .filter { showSkipped || Strings.nullToEmpty(latestShownUpdateMetadata.version).isEmpty() || it > latestShownUpdateMetadata }
  if (visibleUpdates.isNotEmpty()) {
    val runningUpdateMetadata = UpdateMetadata(
      runningVersion,
      null, null, null, 0, "", false)
    val applyUpdates = updates.filter { it > runningUpdateMetadata }
    val dlg = UpdateDialog(applyUpdates, visibleUpdates) {
      SwingUtilities.invokeLater {
        uiFacade.quitApplication(false)
        Executors.newSingleThreadExecutor().run {  org.eclipse.core.runtime.Platform.restart() }
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
 * UI with the information about the running GanttProject version, the list of available updates amd
 * controls to install updates or disable update checking.
 */
internal class UpdateDialog(
    private val updates: List<UpdateMetadata>,
    private val visibleUpdates: List<UpdateMetadata>,
    private val restarter: AppRestarter) {
  private lateinit var dialogApi: DialogController
  private val version2ui = mutableMapOf<String, UpdateComponentUi>()
  private val hasUpdates: Boolean get() = this.visibleUpdates.isNotEmpty()
  // Major update metadata, if found in the JSON
  private val majorUpdate: UpdateMetadata? get() = this.updates.firstOrNull { it.isMajorUpdate }
  // The list of visible updates with major updates filtered out
  private val minorUpdates get() = this.visibleUpdates.filter { !it.isMajorUpdate }
  // The action to install minor updates shall be disabled if we show a major update UI,
  // but if we switch to "show minor updates", it shall become enabled.
  private val minorUpdatesDisabled = SimpleBooleanProperty(false)
  // Progress indicator
  private val progressText = ourLocalizer.create("installProgress")
  private val progressLabel: Label = Label().also {
    it.textProperty().bind(progressText)
    it.styleClass.add("progress")
    it.isVisible = false
  }

  private fun createPane(bean: PlatformBean): Node {
    val bodyBuilder = VBoxBuilder("content-pane")

    // The upper part of the dialog which shows the running version number and a toggle to switch
    // the update checking on and off.
    val props = GridPane().apply {
      styleClass.add("props")
      add(Label(ourLocalizer.formatText("installedVersion")).also {
        GridPane.setMargin(it, Insets(5.0, 10.0, 3.0, 0.0))
      }, 0, 0)
      add(Label(bean.version).also {
        GridPane.setMargin(it, Insets(5.0, 0.0, 3.0, 0.0))
      }, 1, 0)
      add(Label(ourLocalizer.formatText("checkUpdates")).also {
        GridPane.setMargin(it, Insets(5.0, 10.0, 3.0, 0.0))
      }, 0, 1)
      val toggleSwitch = createToggleSwitch().also {
        it.selectedProperty().value = UpdateOptions.isCheckEnabled.value
        it.selectedProperty().addListener { _, _, newValue -> UpdateOptions.isCheckEnabled.value = newValue }
      }
      add(toggleSwitch, 1, 1)
      add(HyperlinkLabel(ourLocalizer.formatText("checkUpdates.helpline")).also {
        it.styleClass.add("helpline")
        it.onAction = EventHandler { openInBrowser(PRIVACY_URL) }
      }, 1, 2)
      if (this@UpdateDialog.majorUpdate == null) {
        add(
          Label(
            if (this@UpdateDialog.hasUpdates) ourLocalizer.formatText("availableUpdates")
            else ourLocalizer.formatText("noUpdates.title")
          ).also {
            GridPane.setMargin(it, Insets(30.0, 0.0, 5.0, 0.0))
          },
          0, 3)
      }
    }
    bodyBuilder.add(props)

    if (this.hasUpdates) {

      val wrapperPane = BorderPane()
      val minorUpdates = this.minorUpdates.map {
        UpdateComponentUi(it).also { ui ->
          version2ui[it.version] = ui
        }
      }

      val minorUpdatesUi = vbox {
        addClasses("minor-update")
        minorUpdates.forEach {
            add(it.title)
            add(it.subtitle)
            add(it.text)
          }
        vbox
      }

      wrapperPane.center = this.majorUpdate?.let { majorUpdate ->
        // If we have a "major update" record, we will show only that one.
        // The action to install minor updates shall be disabled.
        minorUpdatesDisabled.value = true
        MajorUpdateUi(majorUpdate, minorUpdates.isNotEmpty()) {
          FXUtil.transitionCenterPane(wrapperPane, minorUpdatesUi) {
            // If we transition to the minor updates, we enable the install action.
            dialogApi.resize()
            minorUpdatesDisabled.value = false
          }
        }.component
      } ?: minorUpdatesUi

      bodyBuilder.add(ScrollPane(wrapperPane.also {
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
      "/biz/ganttproject/app/Dialog.css",
      "/biz/ganttproject/platform/Update.css"
    )

    dialogApi.setHeader(VBoxBuilder("header").apply {
      addTitle(ourLocalizer.formatText(
          if (this@UpdateDialog.hasUpdates) "hasUpdates.title" else "noUpdates.title")
      )
    }.vbox)

    val downloadCompleted = SimpleBooleanProperty(false)
    if (this.minorUpdates.isNotEmpty()) {
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
            Platform.runLater {
              btn.disableProperty().set(false)
              btn.text = ourLocalizer.formatText("restart")
              btn.properties["restart"] = true
            }
          }
        }
        minorUpdatesDisabled.addListener { _, _, newValue ->
          btn.isDisable = newValue
        }
      }
    }
    dialogApi.setupButton(ButtonType.CLOSE) { btn ->
      ButtonBar.setButtonUniformSize(btn, false)
      btn.maxWidth = Double.MAX_VALUE
      btn.styleClass.add("btn")
      btn.text = ourLocalizer.formatText("button.close_skip")
      btn.addEventFilter(ActionEvent.ACTION) {
        UpdateOptions.latestShownVersion.value = this.updates.first().version
        dialogApi.hide()
      }
      downloadCompleted.addListener { _, _, newValue ->
        if (newValue) {
          Platform.runLater {
            btn.text = RootLocalizer.formatText("close")
            dialogApi.resize()
          }
        }
      }
    }
    dialogApi.setContent(this.createPane(bean))
    dialogApi.setButtonPaneNode(this.progressLabel)
  }

  private fun onRestart() {
    this.restarter()
  }

  private fun onDownload(completed: SimpleBooleanProperty) {
    var installFuture: CompletableFuture<File>? = null
    for (update in updates.filter { !it.isMajorUpdate }.reversed()) {
      val minorUpdateUi = this.version2ui[update.version]
      val progressMonitor: (Int) -> Unit = { percents: Int ->
        if (minorUpdateUi != null) {
          Platform.runLater {
            if (minorUpdateUi.progressValue == -1) {
              this.progressLabel.isVisible = true
            }
            if (minorUpdateUi.progressValue != percents) {
              minorUpdateUi.progressValue = percents
              progressText.update(update.version, percents.toString())
              if (percents == 100) {
                this.progressLabel.isVisible = false
                this.progressText.clear()
              }
            }
          }
        }
      }
      installFuture =
          if (installFuture == null) update.install(progressMonitor)
          else installFuture.thenCompose { update.install(progressMonitor) }
    }
    installFuture?.thenAccept {
      completed.value = true
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

/**
 * Encapsulates UI controls which show information and status of individual updates.
 */
private class UpdateComponentUi(val update: UpdateMetadata) {
  val title: Label = Label(ourLocalizer.formatText("bodyItem.title", update.version)).also { l ->
    l.styleClass.add("title")
  }
  val subtitle: Label = Label(ourLocalizer.formatText("bodyItem.subtitle", update.date, update.sizeAsString())).also { l ->
    l.styleClass.add("subtitle")
  }
  val text: MDFXNode = MDFXNode(ourLocalizer.formatText("bodyItem.description", update.description)).also { l ->
    l.styleClass.add("par")
  }
  var progressValue: Int = -1
  set(value) {
    field = value
    if (value == 100) {
      listOf(title, subtitle, text).forEach { it.opacity = 0.5 }
    }
  }
}

/**
 * User interface for the major update record.
 */
private class MajorUpdateUi(update: UpdateMetadata, hasMinorUpdates: Boolean, private val onShowMinorUpdates: ()->Unit) {
  val title: Label = Label(ourLocalizer.formatText("majorUpdate.title", update.version)).also { l ->
    l.styleClass.add("title",)
  }
  val subtitle = Label(ourLocalizer.formatText("majorUpdate.subtitle", update.version)).apply {
    styleClass.add("subtitle")
  }
  val text: MDFXNode = MDFXNode(ourLocalizer.formatText("majorUpdate.description", update.description)).also { l ->
    l.styleClass.add("par")
  }
  val btnMinorUpdates = Button(ourLocalizer.formatText("majorUpdate.showBugfixUpdates")).also {
    it.styleClass.addAll("btn", "btn-attention", "secondary")
    it.onAction = EventHandler {
      this.onShowMinorUpdates()
    }
  }
  val btnDownload = Button(ourLocalizer.formatText("majorUpdate.download")).apply {
    styleClass.addAll("btn", "btn-attention")
    onAction = EventHandler { openInBrowser(UPGRADE_URL) }
  }
  val component = vbox {
    addClasses("major-update")
    addStylesheets("/biz/ganttproject/app/Dialog.css")
    add(title)
    add(subtitle)
    add(text)
    add(GridPane().apply {
      add(btnDownload.also {
        GridPane.setMargin(it, Insets(00.0, 5.0, 0.0, 0.0))
      }, 0, 0)
      if (hasMinorUpdates) {
        add(btnMinorUpdates, 1, 0)
      }
      VBox.setMargin(this, Insets(15.0, 0.0, 0.0, 0.0))
    })
    vbox
  }
}
private val LOG = GPLogger.create("App.Update")
private val ourLocalizer = RootLocalizer.createWithRootKey("platform.update")

object UpdateOptions {
  val isCheckEnabled = DefaultBooleanOption("checkEnabled", true)
  val latestShownVersion = DefaultStringOption("latestShownVersion")
  val updateUrl = DefaultStringOption("url", System.getProperty("platform.update.url", "https://www.ganttproject.biz/dl/updates/ganttproject-3.3.json"))
  val optionGroup: GPOptionGroup = GPOptionGroup("platform.update", isCheckEnabled, latestShownVersion, updateUrl)

}

object DummyUpdater : Updater {
  override fun getUpdateMetadata(p0: String?) = CompletableFuture.completedFuture(listOf<UpdateMetadata>())
  override fun installUpdate(p0: UpdateMetadata?, p1: UpdateProgressMonitor?, p2: UpdateIntegrityChecker?): CompletableFuture<File> {
    TODO("Not yet implemented")
  }
}
