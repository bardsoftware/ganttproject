/*
Copyright 2018 BarD Software s.r.o

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

import biz.ganttproject.app.OptionElementData
import biz.ganttproject.app.OptionPaneBuilder
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.storage.*
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableObjectValue
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import javafx.scene.shape.Circle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.action.OkAction
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.ProxyDocument
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.language.GanttLanguage
import org.controlsfx.control.decoration.Decorator
import org.controlsfx.control.decoration.GraphicDecoration
import java.util.*
import javax.swing.JOptionPane

private fun createWarningDecoration(): Node {
  return Circle(4.0).also {
    it.styleClass.add("decoration-warning")
    it.strokeWidth = 2.0
  }
}

/**
 * This status bar appears in the bottom-lef corner of the app window and shows
 * document lock status and access mode. When clicked, it opens a dialog for changing
 * lock status and access mode.
 *
 * @author dbarashev@bardsoftware.com
 */
class GPCloudStatusBar(private val observableDocument: ObservableObjectValue<Document>, private val uiFacade: UIFacade) {
  private var onLatestVersionChange: ChangeListener<LatestVersion>? = null
  private val btnLock = Button().also {
    it.isVisible = false
  }
  private val btnOffline = Button().also {
    it.isVisible = false
  }
  val lockPanel = HBox().also {
    it.styleClass.add("statusbar")
    it.children.addAll(btnOffline, btnLock)
  }

  private lateinit var status: LockStatus


  init {
    observableDocument.addListener { _, oldDocument: Document?, newDocument: Document? ->
      onDocumentChange(oldDocument, newDocument)
    }
    btnOffline.addEventHandler(ActionEvent.ACTION) {
      showProperties()
    }
    btnLock.addEventHandler(ActionEvent.ACTION) {
      showProperties()
    }
  }

  private fun showProperties() {
    this.observableDocument.get().apply {
      val onlineDocument = this.asOnlineDocument()
      if (onlineDocument is GPCloudDocument) {
        DocPropertiesUi(errorUi = {}, busyUi = {}).showDialog(onlineDocument)
      }
    }
  }

  // This is called whenever open document changes and handles different cases.
  private fun onDocumentChange(oldDocument: Document?, newDocument: Document?) {
    Platform.runLater {

      // First we un-proxy old and new documents.
      val newDoc = if (newDocument is ProxyDocument) {
        newDocument.realDocument
      } else {
        newDocument
      }
      val oldDoc = if (oldDocument is ProxyDocument) {
        oldDocument.realDocument
      } else {
        oldDocument
      }

      // Then we remove listeners from the old document
      if (oldDoc is LockableDocument) {
        oldDoc.status.removeListener(this::onLockStatusChange)
      }
      if (oldDoc is OnlineDocument) {
        oldDoc.mode.removeListener(this::onOnlineModeChange)
        this.onLatestVersionChange?.let { oldDoc.latestVersionProperty.removeListener(it) }
        this.onLatestVersionChange = null
      }

      // If new document is lockable, we'll add listeners and show the icon.
      if (newDoc is LockableDocument) {
        newDoc.status.addListener(this::onLockStatusChange)
        this.btnLock.isVisible = true
        newDoc.reloadLockStatus()
      } else {
        this.btnLock.isVisible = false
      }

      // If new document is online, we'll add some listeners too.
      if (newDoc is OnlineDocument) {
        // Listen to online mode changes: online only/mirrored/offline only
        newDoc.mode.addListener(this::onOnlineModeChange)
        this.btnOffline.isVisible = true
        this.updateOnlineMode(newDoc.mode.value)

        // Listen to the version updates
        this.onLatestVersionChange = ChangeListener { _, _, newValue ->
          handleLatestVersionChange(newDoc, newValue)
        }
        newDoc.latestVersionProperty.addListener(this.onLatestVersionChange)
      } else {
        this.btnOffline.isVisible = false
      }
    }
  }

  private fun onLockStatusChange(observable: Any, oldStatus: LockStatus, newStatus: LockStatus) {
    Platform.runLater {
      this.updateLockStatus(newStatus)
    }
  }

  private fun updateLockStatus(status: LockStatus) {
    if (status.locked) {
      this.btnLock.graphic = FontAwesomeIconView(FontAwesomeIcon.LOCK)
      val lockOwner = STATUS_BAR_LOCALIZER.formatText("lockedBy", status.lockOwnerName ?: "")
      this.btnLock.text =
          if (GPCloudOptions.userId.value?.equals(status.lockOwnerId) == true) STATUS_BAR_LOCALIZER.formatText("locked")
          else lockOwner
      this.btnLock.tooltip = Tooltip(lockOwner)
    } else {
      this.btnLock.graphic = FontAwesomeIconView(FontAwesomeIcon.UNLOCK)
      this.btnLock.text = STATUS_BAR_LOCALIZER.formatText("notLocked")
      this.btnLock.tooltip = Tooltip(STATUS_BAR_LOCALIZER.formatText("notLocked.tooltip"))
    }

    this.status = status
  }

  private fun onOnlineModeChange(observable: Any, oldValue: OnlineDocumentMode, newValue: OnlineDocumentMode) {
    Platform.runLater {
      this.updateOnlineMode(newValue)
    }
  }

  private fun updateOnlineMode(mode: OnlineDocumentMode) {
    when (mode) {
      OnlineDocumentMode.ONLINE_ONLY -> {
        this.btnOffline.run {
          text = STATUS_BAR_LOCALIZER.formatText("mode.onlineOnly")
          graphic = FontAwesomeIconView(FontAwesomeIcon.CLOUD)
          tooltip = Tooltip(STATUS_BAR_LOCALIZER.formatText("mode.onlineOnly.tooltip"))
          Decorator.removeAllDecorations(this)
          isDisable = false
        }
        this.btnLock.isDisable = false
      }
      OnlineDocumentMode.MIRROR -> {
        this.btnOffline.run {
          text = STATUS_BAR_LOCALIZER.formatText("mode.mirror")
          graphic = FontAwesomeIconView(FontAwesomeIcon.CLOUD_DOWNLOAD)
          tooltip = Tooltip(STATUS_BAR_LOCALIZER.formatText("mode.mirror.tooltip"))
          Decorator.removeAllDecorations(this)
          isDisable = false
        }
        this.btnLock.isDisable = false
      }
      OnlineDocumentMode.OFFLINE_ONLY -> {
        this.btnOffline.run {
          text = STATUS_BAR_LOCALIZER.formatText("mode.offline")
          graphic = FontAwesomeIconView(FontAwesomeIcon.CLOUD_DOWNLOAD)
          tooltip = Tooltip(STATUS_BAR_LOCALIZER.formatText("mode.offline.tooltip"))
          Decorator.addDecoration(this, GraphicDecoration(createWarningDecoration(), Pos.BOTTOM_LEFT, 6.0, -4.0))
          isDisable = true
        }
        this.uiFacade.showOptionDialog(
            JOptionPane.WARNING_MESSAGE,
            STATUS_BAR_LOCALIZER.formatText("mode.offline.warning"),
            arrayOf(OkAction.createVoidAction("ok"))
        )
        this.btnLock.isDisable = true
      }
    }
  }

  // This is called when cloud document changes and we receive an update notification.
  // We want to show a dialog asking to reload document or ignore the update.
  private fun handleLatestVersionChange(doc: OnlineDocument, newValue: LatestVersion) {
    OptionPaneBuilder<Boolean>().run {
      i18n = RootLocalizer.createWithRootKey("cloud.loadLatestVersion")
      graphic = FontAwesomeIconView(FontAwesomeIcon.REFRESH)
      elements = listOf(
          OptionElementData("reload", true, true),
          OptionElementData("ignore", false)
      )
      titleHelpString?.update(newValue.author, GanttLanguage.getInstance().formatDate(GanttCalendar.getInstance().apply {
        time = Date(newValue.timestamp)
      }))

      showDialog { choice ->
        when (choice) {
          true -> {
            GlobalScope.launch(Dispatchers.IO) {
              doc.fetch().update()
            }
          }
          false -> {}
        }
      }
    }
  }
}

val STATUS_BAR_LOCALIZER = RootLocalizer.createWithRootKey("cloud.statusBar")
