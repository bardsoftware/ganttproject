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
package biz.ganttproject.platform

import biz.ganttproject.FXUtil.launchFx
import biz.ganttproject.app.Localizer
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.option.DefaultBooleanOption
import biz.ganttproject.core.option.DefaultStringOption
import biz.ganttproject.core.option.GPOptionGroup
import biz.ganttproject.createLogger
import biz.ganttproject.lib.fx.openInBrowser
import biz.ganttproject.platform.PgpUtil.verifyFile
import com.bardsoftware.eclipsito.update.UpdateMetadata
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapEither
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.google.common.base.Strings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.scene.control.Button
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import java.io.File
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executors
import javax.swing.SwingUtilities
import org.eclipse.core.runtime.Platform as Eclipsito
import biz.ganttproject.platform.UpdateDialogLocalizationKeys as Keys

internal enum class ApplyAction {
  INSTALL_FROM_CHANNEL, INSTALL_FROM_ZIP, DOWNLOAD_MAJOR, RESTART
}

typealias InstallProgressMonitor = (Int)->Unit

internal fun createModel(allUpdates: List<UpdateMetadata>, showSkipped: Boolean, quitFunction: ()->Unit): UpdateDialogModel? {
  val runningVersion = Eclipsito.getUpdater().installedUpdateVersions.maxOrNull() ?: "2900"
  val cutoffVersion = listOf(UpdateOptions.latestShownVersion.value ?: runningVersion, runningVersion).maxOrNull()
  val latestShownUpdateMetadata = UpdateMetadata(
    cutoffVersion,
    null, null, null, 0, "", false
  )
  val visibleUpdates = allUpdates
    .filter {
      showSkipped || Strings.nullToEmpty(latestShownUpdateMetadata.version).isEmpty() || it > latestShownUpdateMetadata
    }
  return if (visibleUpdates.isNotEmpty()) {
    val runningUpdateMetadata = UpdateMetadata(
      runningVersion,
      null, null, null, 0, "", false
    )
    val applyUpdates = allUpdates.filter { it > runningUpdateMetadata }
    UpdateDialogModel(applyUpdates, visibleUpdates) {
      SwingUtilities.invokeLater {
        quitFunction()
        Executors.newSingleThreadExecutor().run {  Eclipsito.restart() }
      }
    }
  } else null
}

class UpdateDialogModel(
  internal val updates: List<UpdateMetadata>,
  private val visibleUpdates: List<UpdateMetadata>,
  internal val installedVersion: String = Eclipsito.getUpdater().installedUpdateVersions.maxOrNull() ?: "3390",
  private val restarter: AppRestarter
  ) {

  internal var localizer: Localizer = RootLocalizer
  internal var startProgressMonitor: (version: String)->InstallProgressMonitor? = {null}

  internal val hasUpdates: Boolean get() = this.visibleUpdates.isNotEmpty()
  internal val hasMajorUpdates get() = this.majorUpdate != null
  internal val hasMinorUpdates get() = this.minorUpdates.isNotEmpty()

  // Major update metadata, if found in the JSON
  internal val majorUpdate: UpdateMetadata? get() = this.updates.firstOrNull { it.isMajorUpdate }
  // The list of visible updates with major updates filtered out
  internal val minorUpdates get() = this.visibleUpdates.filter { !it.isMajorUpdate }


  val btnApplyText = SimpleStringProperty("")
  val btnApplyDisabled = SimpleBooleanProperty(false)

  val btnCloseText = SimpleStringProperty("")
  val btnToggleSourceText = SimpleStringProperty("")

  internal var state: ApplyAction = ApplyAction.INSTALL_FROM_CHANNEL
    set(value) {
      when (value) {
        ApplyAction.INSTALL_FROM_CHANNEL -> {
          btnApplyText.value = localizer.formatText(Keys.BUTTON_OK)
          btnCloseText.value = localizer.formatText("button.close_skip")
          stateTitleProperty.value = "Maintenance Updates"
          btnToggleSourceText.value = localizer.formatText(Keys.INSTALL_FROM_ZIP)
        }
        ApplyAction.INSTALL_FROM_ZIP -> {
          btnApplyText.value = localizer.formatText(Keys.BUTTON_OK)
          btnToggleSourceText.value = localizer.formatText(Keys.INSTALL_FROM_CHANNEL)
        }
        ApplyAction.RESTART -> {
          btnApplyText.value = localizer.formatText("restart")
          btnCloseText.value = localizer.formatText("close")
        }
        ApplyAction.DOWNLOAD_MAJOR -> {
          btnApplyText.value = localizer.formatText(Keys.MAJOR_UPDATE_DOWNLOAD)
          btnToggleSourceText.value = localizer.formatText(Keys.INSTALL_FROM_ZIP)
          stateTitleProperty.value = ""
        }
      }
      field = value
      stateProperty.value = value
    }
  internal val stateProperty = SimpleObjectProperty<ApplyAction>(state)
  internal val stateTitleProperty = SimpleStringProperty("")

  var hideDialog: ()->Unit = {}
  var installFromZip: ()->Unit = {}

  internal fun initState() {
    if (hasMajorUpdates) {
      state = ApplyAction.DOWNLOAD_MAJOR
    } else if (hasMinorUpdates) {
      state = ApplyAction.INSTALL_FROM_CHANNEL
    }
  }

  // Starts installing the available updates one-by-one.
  internal fun onDownload(): Result<Boolean, Throwable> {
    var installFuture: CompletableFuture<File>? = null
    for (update in updates.filter { !it.isMajorUpdate }.reversed()) {
      startProgressMonitor(update.version)?.let { progressMonitor ->
        installFuture =
          if (installFuture == null) update.install(progressMonitor)
          else installFuture.thenCompose { update.install(progressMonitor) }
      }
    }
    return try {
      installFuture?.join()
      Ok(true)
    } catch (ex: CompletionException) {
      ourLogger.error("Failed to install updates", ex)
      Err(ex.cause ?: ex)
    }
  }

  internal fun setupApplyButton(applyButton: Button) {
    applyButton.textProperty().bind(btnApplyText)
    applyButton.disableProperty().bind(btnApplyDisabled)
    applyButton.addEventFilter(ActionEvent.ACTION) {
      it.consume()
      launchFx {
        onApplyPressed()
      }
    }
  }

  internal fun setupCloseButton(closeButton: Button) {
    closeButton.textProperty().bind(btnCloseText)
    closeButton.addEventFilter(ActionEvent.ACTION) {
      it.consume()
      onClosePressed()
    }
  }

  fun setupToggleSourceButton(button: Button) {
    button.textProperty().bind(btnToggleSourceText)
    button.addEventFilter(ActionEvent.ACTION) {
      it.consume()
      if (state == ApplyAction.INSTALL_FROM_CHANNEL || state == ApplyAction.DOWNLOAD_MAJOR) {
        state = ApplyAction.INSTALL_FROM_ZIP
      } else if (state == ApplyAction.INSTALL_FROM_ZIP) {
        state = ApplyAction.INSTALL_FROM_CHANNEL
      }
    }
  }

  private suspend fun onApplyPressed() {
    when (state) {
      ApplyAction.INSTALL_FROM_CHANNEL -> {
        applyMinorUpdates()
      }
      ApplyAction.INSTALL_FROM_ZIP -> {
        installFromZip()
      }
      ApplyAction.RESTART -> {
        this.restarter()
      }
      ApplyAction.DOWNLOAD_MAJOR -> {
        openInBrowser(UPGRADE_URL)
      }
    }
  }

  private fun onClosePressed() {
    UpdateOptions.latestShownVersion.value = updates.first().version
    hideDialog()
  }

  private suspend fun applyMinorUpdates() {
    btnApplyDisabled.set(true)
    runCatching {
      ourBackgroundScope.async {
        onDownload()
      }.await()
    }.anyway { btnApplyDisabled.set(false) }.onSuccess {
      state = ApplyAction.RESTART
    }
  }

  private fun (UpdateMetadata).install(monitor: InstallProgressMonitor): CompletableFuture<File> {
    return Eclipsito.getUpdater().installUpdate(this, monitor) { dataFile ->
      if (this.signatureBody.isNullOrBlank()) {
        true
      } else {
        verifyFile(dataFile, Base64.getDecoder().wrap(this.signatureBody.byteInputStream()))
        true
      }
    }
  }
}

private fun <V,E> Result<V, E>.anyway(code: ()->Unit): Result<V,E> =
  this.mapEither(
    success = {
      code()
      it
    },
    failure = {
      code()
      it
    }
  )

object UpdateOptions {
  val isCheckEnabled = DefaultBooleanOption("checkEnabled", true)
  val latestShownVersion = DefaultStringOption("latestShownVersion")
  val updateUrl = DefaultStringOption("url", System.getProperty("platform.update.url", "https://www.ganttproject.biz/dl/updates/ganttproject-3.0.json"))
  val optionGroup: GPOptionGroup = GPOptionGroup("platform.update", isCheckEnabled, latestShownVersion, updateUrl)

}

private val ourBackgroundScope = CoroutineScope(Executors.newFixedThreadPool(2).asCoroutineDispatcher())
private val ourLogger = createLogger("Update.Download")
const val UPGRADE_URL = "https://www.ganttproject.biz/download/upgrade"

object UpdateDialogLocalizationKeys {
  internal const val MAJOR_UPDATE_DOWNLOAD = "majorUpdate.download"
  internal const val BUTTON_OK = "button.ok"
  internal const val INSTALL_FROM_ZIP = "Install from ZIP"
  internal const val INSTALL_FROM_CHANNEL = "Install from Update Channel"
}
