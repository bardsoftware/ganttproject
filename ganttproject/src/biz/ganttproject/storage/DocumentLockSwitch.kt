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
package biz.ganttproject.storage

import biz.ganttproject.storage.cloud.GPCloudOptions
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.beans.value.ObservableObjectValue
import javafx.event.ActionEvent
import javafx.scene.control.Button
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.ProxyDocument

/**
 * @author dbarashev@bardsoftware.com
 */
class DocumentLockSwitch(private val observableDocument: ObservableObjectValue<Document>) {
  private val btnLock = Button().also {
    it.tooltip = Tooltip("Currently not available offline")
  }
  private val btnOffline = Button().also {
    it.graphic = FontAwesomeIconView(FontAwesomeIcon.CLOUD)
  }
  private val btnManage = Button("Cloud Options...")
  val lockPanel = HBox().also {
    it.styleClass.add("statusbar")
    it.children.addAll(btnOffline, btnLock, btnManage)
  }

  private var isChangingSelected: Boolean = false
  private lateinit var status: LockStatus


  init {
    observableDocument.addListener(this::onDocumentChange)
    btnOffline.addEventHandler(ActionEvent.ACTION) {
      onOfflineAction()
    }
    btnLock.addEventHandler(ActionEvent.ACTION) {
      onSwitchAction()
    }
  }

  private fun onDocumentChange(observable: Any, oldDocument: Document?, newDocument: Document?) {
    Platform.runLater {

      val newDoc = if (newDocument is ProxyDocument) { newDocument.realDocument } else { newDocument }
      val oldDoc = if (oldDocument is ProxyDocument) { oldDocument.realDocument } else { oldDocument }

      if (oldDoc is LockableDocument) {
        oldDoc.status.removeListener(this::onStatusChange)
      }

      if (newDoc is LockableDocument) {
        newDoc.status.addListener(this::onStatusChange)
        this.updateStatus(newDoc.status.value)
      } else if (newDoc != null) {
        this.lockPanel.isDisable = true
      }

      if (newDoc is OnlineDocument) {
        newDoc.isAvailableOffline.addListener(this::onOfflineChange)
      }
    }
  }

  private fun onStatusChange(observable: Any, oldStatus: LockStatus, newStatus: LockStatus) {
    Platform.runLater {
      this.updateStatus(newStatus)
    }
  }

  private fun updateStatus(status: LockStatus) {
    this.isChangingSelected = true
    this.isChangingSelected = false
    if (status.locked) {
      this.btnLock.graphic = FontAwesomeIconView(FontAwesomeIcon.LOCK)
      this.btnLock.tooltip = Tooltip("Locked by ${status.lockOwnerName}")
    } else {
      this.btnLock.graphic = FontAwesomeIconView(FontAwesomeIcon.UNLOCK)
      this.btnLock.tooltip = Tooltip("Currently not locked")
    }

    this.lockPanel.isDisable = status.locked && status.lockOwnerId != GPCloudOptions.userId.value
    this.status = status
  }

  private fun onSwitchAction() {
    if (this.isChangingSelected) {
      return
    }
    val doc = this.observableDocument.get()
    val realDoc = if (doc is ProxyDocument) { doc.realDocument } else { doc }
    if (realDoc is LockableDocument) {
      val progress = ProgressIndicator()
      lockPanel.children.add(progress)
      val isNowLocked = this.status.locked
      val savedStatus = this.status
      val future = realDoc.toggleLocked()
      future.thenAccept(this::updateStatus).handle { ok, ex ->
        lockPanel.children.remove(progress)
        when {
          ex != null -> {
            val msg = if (isNowLocked) { "Lock request failed" } else { "Unlock request failed" }
            this.updateStatus(savedStatus)
            GPLogger.log(Exception(msg, ex))
          }
          else -> null
        }
      }
    }
  }

  private fun onOfflineAction() {
    val doc = this.observableDocument.get()
    val realDoc = if (doc is ProxyDocument) { doc.realDocument } else { doc }
    if (realDoc is OnlineDocument) {
      realDoc.toggleAvailableOffline()
    }
  }

  private fun onOfflineChange(observable: Any, oldValue: Boolean, newValue: Boolean) {
    Platform.runLater {
      this.updateOfflineStatus(newValue)
    }
  }

  private fun updateOfflineStatus(isOffline: Boolean) {
    if (isOffline) {
      this.btnOffline.graphic = FontAwesomeIconView(FontAwesomeIcon.CLOUD_DOWNLOAD)
      this.btnOffline.tooltip = Tooltip("Available offline. Click to remove offline mirror")
    } else {
      this.btnOffline.graphic = FontAwesomeIconView(FontAwesomeIcon.CLOUD)
      this.btnOffline.tooltip = Tooltip("Click to make offline mirror")
    }

  }

}