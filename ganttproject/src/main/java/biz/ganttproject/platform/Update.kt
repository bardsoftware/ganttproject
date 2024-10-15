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
import biz.ganttproject.app.dialog
import biz.ganttproject.createLogger
import biz.ganttproject.lib.fx.vbox
import com.bardsoftware.eclipsito.update.UpdateIntegrityChecker
import com.bardsoftware.eclipsito.update.UpdateMetadata
import com.bardsoftware.eclipsito.update.UpdateProgressMonitor
import com.bardsoftware.eclipsito.update.Updater
import javafx.scene.control.*
import javafx.scene.layout.*
import net.sourceforge.ganttproject.gui.UIFacade
import java.io.File
import java.net.ConnectException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

const val PRIVACY_URL = "https://www.ganttproject.biz/about/privacy"

fun checkAvailableUpdates(updater: Updater, uiFacade: UIFacade) {
  if (UpdateOptions.isCheckEnabled.value) {
    LOG.debug("Fetching updates from {}", UpdateOptions.updateUrl.value)
    updater.getUpdateMetadata(UpdateOptions.updateUrl.value).thenAccept { updateMetadata ->
      if (updateMetadata.isNotEmpty()) {
        createModel(updateMetadata, false, {uiFacade.quitApplication(false)})?.let {
          updatesAvailableDialog(it, null)
        }
      }
    }.exceptionally { ex ->
      LOG.error(msg = "Failed to fetch updates from {}", UpdateOptions.updateUrl.value, exception = ex)
      null
    }
  } else {
    LOG.debug("Updates are disabled")
  }
}

typealias AppRestarter = () -> Unit

fun updatesAvailableDialog(model: UpdateDialogModel,
                           dialogController: DialogController?) {
  val dlg = UpdateDialog(model)
  dialogController?.let {
    dlg.addContent(it, isFromSettings = true)
  } ?: dialog(
    title = RootLocalizer.formatText("platform.update.hasUpdates.title"),
    contentBuilder = {
      dlg.addContent(it, isFromSettings = false)
    }
  )
}

fun updatesFetchErrorDialog(ex: Throwable, dialogController: DialogController) {
  val dlg = UpdateDialog(UpdateDialogModel(listOf(), listOf(), restarter = {}))
  dlg.addContent(ex, dialogController)
}
/**
 * UI with the information about the running GanttProject version, the list of available updates and
 * controls to install updates or disable update checking.
 */
internal class UpdateDialog(private val model: UpdateDialogModel) {

  private lateinit var dialogApi: DialogController
  // Progress indicator
  private val installFromZipUi by lazy {
    UpdateFromZip(ourLocalizer).also {
      model.installFromZip = it::installUpdate
    }
  }
  private val installFromChannelUi by lazy {
    UpdateFromChannel(model, ourLocalizer).also {
      model.startProgressMonitor = {version ->
        it.startProgressMonitor(version)
      }
    }
  }
  private val dialogContent = BorderPane()

  init {
    model.localizer = ourLocalizer
    model.stateProperty.subscribe { oldValue, newValue ->
      if ((oldValue == ApplyAction.INSTALL_FROM_CHANNEL || oldValue == ApplyAction.DOWNLOAD_MAJOR) && newValue == ApplyAction.INSTALL_FROM_ZIP) {
        FXUtil.transitionCenterPane(dialogContent, installFromZipUi.node, dialogApi::resize)
      }
      if (oldValue == ApplyAction.INSTALL_FROM_ZIP && (newValue == ApplyAction.INSTALL_FROM_CHANNEL || newValue == ApplyAction.DOWNLOAD_MAJOR)) {
        FXUtil.transitionCenterPane(dialogContent, installFromChannelUi.node, dialogApi::resize)
      }
    }
    model.initState()
  }

  internal fun addContent(dialogApi: DialogController, isFromSettings: Boolean) {
    this.model.hideDialog = dialogApi::hide
    this.dialogApi = dialogApi
    dialogApi.addStyleClass("dlg-platform-update")
    dialogApi.addStyleSheet(
      "/biz/ganttproject/app/Dialog.css",
      "/biz/ganttproject/app/Util.css",
      "/biz/ganttproject/platform/Update.css"
    )

    dialogApi.setHeader(vbox {
      addClasses("header")
      addTitle(ourLocalizer.formatText(
          if (this@UpdateDialog.model.hasUpdates) "hasUpdates.title" else "noUpdates.title")
      )
    })

    dialogApi.setupButton(ButtonType.APPLY) { btn ->
      ButtonBar.setButtonUniformSize(btn, false)
      btn.styleClass.add("btn-attention")
      btn.maxWidth = Double.MAX_VALUE
      model.setupApplyButton(btn)
    }

    if (!isFromSettings) {
      // If we show this dialog on start-up, we allow for skipping the update and add the appropriate button.
      // This button will also behave like "close" button if we install the update.
      dialogApi.setupButton(ButtonType.CLOSE) { btn ->
        ButtonBar.setButtonUniformSize(btn, false)
        btn.maxWidth = Double.MAX_VALUE
        btn.styleClass.add("btn")
        model.setupCloseButton(btn)
      }
    } else {
      dialogApi.setupButton(ButtonType("ZIP")) { btn ->
        btn.maxWidth = Double.MAX_VALUE
        btn.styleClass.addAll("btn", "btn-regular")
        model.setupToggleSourceButton(btn)
      }
    }

    dialogContent.center = installFromChannelUi.node
    dialogApi.setContent(dialogContent)
    dialogApi.setButtonPaneNode(installFromChannelUi.progressLabel)
  }

  /**
   * This function builds a UI in case when we failed to connect to the update site and fetch the update metadata.
   * We will show the installed version, the error message and "install from ZIP" box.
   */
  internal fun addContent(ex: Throwable, dialogApi: DialogController) {
    this.dialogApi = dialogApi
    dialogApi.addStyleClass("dlg-platform-update")
    dialogApi.addStyleSheet(
      "/biz/ganttproject/app/Dialog.css",
      "/biz/ganttproject/platform/Update.css"
    )

    dialogApi.setHeader(vbox {
      addClasses("header")
      addTitle(ourLocalizer.formatText("alert.title"))
    })

    vbox {
      addClasses("content-pane")
      add(createGridPane(ourLocalizer, model))

      add(HBox().apply {
        styleClass.add("alert-embedded-box")
        children.add(Label(ex.getMeaningfulMessage()).also { it.styleClass.add("alert-error") })
      })
      add(installFromZipUi.node)
      dialogApi.setContent(vbox)
    }
  }
}

private fun Throwable.getMeaningfulMessage(): String {
  var cause: Throwable? = this
  while (cause != null && cause is CompletionException) {
    cause = cause.cause
  }
  return when (cause) {
    is ConnectException -> {
      ourLocalizer.formatText("error.cantConnect", UpdateOptions.updateUrl.value)
    }
    is com.grack.nanojson.JsonParserException -> {
      ourLocalizer.formatText("error.cantParse", this.message ?: "")
    }
    else -> cause?.message ?: this.message
  } ?: ""
}

private val LOG = createLogger("App.Update")
private val ourLocalizer = RootLocalizer.createWithRootKey("platform.update", baseLocalizer = RootLocalizer)

object DummyUpdater : Updater {
  override fun getUpdateMetadata(p0: String?) = CompletableFuture.completedFuture(listOf<UpdateMetadata>())
  override fun installUpdate(p0: UpdateMetadata?, p1: UpdateProgressMonitor?, p2: UpdateIntegrityChecker?): CompletableFuture<File> {
    TODO("Not yet implemented")
  }

  override fun installUpdate(p0: File?): CompletableFuture<File?>? {
    TODO("Not yet implemented")
  }
}
