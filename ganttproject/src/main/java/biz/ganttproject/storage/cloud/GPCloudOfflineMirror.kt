/*
Copyright 2018-2020 Dmitry Barashev, BarD Software s.r.o

This file is part of GanttProject, an opensource project management tool.

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
package biz.ganttproject.storage.cloud

import biz.ganttproject.app.*
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.storage.*
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.document.Document
import java.time.Instant
import java.util.*
import java.util.function.Consumer

/**
 * Represents local offline mirror document in the document browser pane.
 */
class OfflineMirrorOptionsAsFolderItem(val options: GPCloudFileOptions) : CloudJsonAsFolderItem() {
  override val isLockable: Boolean = false
  override val name: String = options.name.ifBlank {
    options.offlineMirror ?: RootLocalizer.formatText("document.storage.untitledDocument", "")
  }
  override val basePath = options.teamName
  override val isDirectory: Boolean = false
  override val canChangeLock: Boolean = false
  override val isLocked: Boolean
    get() = Instant.ofEpochMilli(options.lockExpiration.toLongOrNull() ?: 0).isAfter(Instant.now())

}

/**
 * Builds offline notification pane and offline browser pane.
 */
class GPCloudOfflinePane(val mode: StorageDialogBuilder.Mode) : FlowPage() {
  private lateinit var controller: GPCloudUiFlow

  override fun createUi() = buildContentPane()

  override fun resetUi() {}

  override fun setController(controller: GPCloudUiFlow) {
    this.controller = controller;
  }

  enum class OfflineChoice {
    OPEN_MIRROR, TRY_AGAIN
  }

  private fun buildContentPane(): Pane {
    val offlineChoice = ToggleGroup()
    val btnContinue = Button(RootLocalizer.formatText("generic.continue")).also {
      it.styleClass.addAll("btn-attention")
    }
    val optionPaneBuilder = OptionPaneBuilder<OfflineChoice>().apply {
      i18n = RootLocalizer.createWithRootKey("storageService.cloudOffline.notification", BROWSE_PANE_LOCALIZER)

      toggleGroup = offlineChoice

      elements = listOf(
          OptionElementData("btnOpenMirror", OfflineChoice.OPEN_MIRROR, isSelected = true),
          OptionElementData("btnTryAgain", OfflineChoice.TRY_AGAIN),
      )

      titleHelpString?.update(RootLocalizer.formatText("cloud.officialTitle"))
    }

    btnContinue.addEventHandler(ActionEvent.ACTION) {
      offlineChoice.selectedToggle.userData.let {
        when (it) {
          OfflineChoice.TRY_AGAIN -> controller.start()
          OfflineChoice.OPEN_MIRROR -> controller.transition(SceneId.OFFLINE_BROWSER)
        }
      }
    }

    return VBoxBuilder("option-pane", "option-pane-padding").apply {
      i18n = ourLocalizer
      addStylesheets(THEME_STYLESHEET, OPTION_PANE_STYLESHEET, DIALOG_STYLESHEET)
      add(optionPaneBuilder.createHeader())
      add(optionPaneBuilder.buildPane(), alignment = Pos.CENTER, growth = Priority.ALWAYS)
      add(btnContinue, alignment = Pos.CENTER_RIGHT, growth = Priority.NEVER)
    }.vbox
  }
}

fun <T: CloudJsonAsFolderItem> loadOfflineMirrors(consumer: Consumer<ObservableList<T>>) {
  val mirrors = GPCloudOptions.cloudFiles.files.entries.mapNotNull { (_, options) ->
    options.offlineMirror?.let {
      OfflineMirrorOptionsAsFolderItem(options)
    }
  }
  consumer.accept(FXCollections.observableArrayList(mirrors) as ObservableList<T>)
}

private val ourLocalizer = RootLocalizer.createWithRootKey("storageService.cloudOffline", BROWSE_PANE_LOCALIZER)
