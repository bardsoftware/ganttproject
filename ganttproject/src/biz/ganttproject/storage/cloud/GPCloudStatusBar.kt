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

import biz.ganttproject.storage.*
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.beans.value.ObservableObjectValue
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Tooltip
import javafx.scene.layout.HBox
import javafx.scene.shape.Circle
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.action.OkAction
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.ProxyDocument
import net.sourceforge.ganttproject.gui.UIFacade
import org.controlsfx.control.decoration.Decorator
import org.controlsfx.control.decoration.GraphicDecoration
import javax.swing.JOptionPane

private fun createWarningDecoration(): Node {
  return Circle(4.0).also {
    it.styleClass.add("decoration-warning")
    it.strokeWidth = 2.0
  }
}

/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudStatusBar(private val observableDocument: ObservableObjectValue<Document>, private val uiFacade: UIFacade) {
  private val btnLock = Button().also {
    it.tooltip = Tooltip("Currently not available offline")
    it.styleClass.add("rect")
  }
  private val btnOffline = Button().also {
    it.graphic = FontAwesomeIconView(FontAwesomeIcon.CLOUD)
    it.styleClass.add("rect")
  }
  private val btnManage = Button("Cloud Options...")
  val lockPanel = HBox().also {
    it.styleClass.add("statusbar")
    it.children.addAll(btnManage, btnOffline, btnLock)
  }

  private var isChangingSelected: Boolean = false
  private lateinit var status: LockStatus


  init {
    observableDocument.addListener(this::onDocumentChange)
    btnOffline.addEventHandler(ActionEvent.ACTION) {
      onMirrorToggle()
    }
    btnLock.addEventHandler(ActionEvent.ACTION) {
      onSwitchAction()
    }

    btnManage.addEventHandler(ActionEvent.ACTION) {
      this.observableDocument.get().apply {
        val onlineDocument = this.asOnlineDocument()
        if (onlineDocument is GPCloudDocument) {
          DocPropertiesUi(errorUi = {}, busyUi = {}).showDialog(onlineDocument, onLockDone = {})
        }
      }
    }
  }

  private fun onDocumentChange(observable: Any, oldDocument: Document?, newDocument: Document?) {
    Platform.runLater {

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

      if (oldDoc is LockableDocument) {
        oldDoc.status.removeListener(this::onLockStatusChange)
      }
      if (oldDoc is OnlineDocument) {
        oldDoc.mode.removeListener(this::onOnlineModeChange)
      }

      if (newDoc is LockableDocument) {
        newDoc.status.addListener(this::onLockStatusChange)
        this.updateLockStatus(newDoc.status.value)
      } else if (newDoc != null) {
        this.lockPanel.isDisable = true
      }

      if (newDoc is OnlineDocument) {
        newDoc.mode.addListener(this::onOnlineModeChange)
        this.updateOnlineMode(newDoc.mode.value)
      }
    }
  }

  private fun onLockStatusChange(observable: Any, oldStatus: LockStatus, newStatus: LockStatus) {
    Platform.runLater {
      this.updateLockStatus(newStatus)
    }
  }

  private fun updateLockStatus(status: LockStatus) {
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
    val realDoc = if (doc is ProxyDocument) {
      doc.realDocument
    } else {
      doc
    }
    if (realDoc is LockableDocument) {
      val progress = ProgressIndicator()
      lockPanel.children.add(progress)
      val isNowLocked = this.status.locked
      val savedStatus = this.status
      val future = realDoc.toggleLocked(duration = null)
      future.thenAccept(this::updateLockStatus).handle { ok, ex ->
        lockPanel.children.remove(progress)
        when {
          ex != null -> {
            val msg = if (isNowLocked) {
              "Lock request failed"
            } else {
              "Unlock request failed"
            }
            this.updateLockStatus(savedStatus)
            GPLogger.log(Exception(msg, ex))
          }
          else -> null
        }
      }
    }
  }

  private fun onMirrorToggle() {
    val doc = this.observableDocument.get()
    val realDoc = if (doc is ProxyDocument) {
      doc.realDocument
    } else {
      doc
    }
    if (realDoc is OnlineDocument) {
      when (realDoc.mode.value) {
        OnlineDocumentMode.ONLINE_ONLY -> realDoc.setMirrored(true)
        OnlineDocumentMode.MIRROR -> realDoc.setMirrored(false)
      }
    }
  }

  private fun onOnlineModeChange(observable: Any, oldValue: OnlineDocumentMode, newValue: OnlineDocumentMode) {
    Platform.runLater {
      this.updateOnlineMode(newValue)
    }
  }

  private fun updateOnlineMode(mode: OnlineDocumentMode) {
    when (mode) {
      OnlineDocumentMode.ONLINE_ONLY -> {
        this.btnOffline.graphic = FontAwesomeIconView(FontAwesomeIcon.CLOUD)
        this.btnOffline.tooltip = Tooltip("Click to make offline mirror")
        Decorator.removeAllDecorations(this.btnOffline)
        this.btnLock.isDisable = false
        this.btnOffline.isDisable = false
      }
      OnlineDocumentMode.MIRROR -> {
        this.btnOffline.graphic = FontAwesomeIconView(FontAwesomeIcon.CLOUD_DOWNLOAD)
        this.btnOffline.tooltip = Tooltip("Available offline. Click to remove offline mirror")
        Decorator.removeAllDecorations(this.btnOffline)
        this.btnLock.isDisable = false
        this.btnOffline.isDisable = false
      }
      OnlineDocumentMode.OFFLINE_ONLY -> {
        this.btnOffline.graphic = FontAwesomeIconView(FontAwesomeIcon.CLOUD_DOWNLOAD)
        this.btnOffline.tooltip = Tooltip("Offline only. Will try to sync when connected again")
        Decorator.addDecoration(this.btnOffline, GraphicDecoration(createWarningDecoration(), Pos.BOTTOM_LEFT, 6.0, -4.0))
        this.uiFacade.showOptionDialog(JOptionPane.WARNING_MESSAGE, "Connection lost and we're now working offline. We'll try to reconnect automatically.", arrayOf(OkAction.createVoidAction("ok")))
        this.btnLock.isDisable = true
        this.btnOffline.isDisable = true
      }
    }
  }
}
