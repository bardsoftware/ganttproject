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
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.ProxyDocument
import org.controlsfx.control.ToggleSwitch

/**
 * @author dbarashev@bardsoftware.com
 */
class DocumentLockSwitch(private val observableDocument: ObservableObjectValue<Document>) {
  private val switch = ToggleSwitch().also {
    it.styleClass.add("lock-toggle")
  }
  private val label = Label().also {
    it.styleClass.add("lock-label")
  }
  val lockPanel = HBox().also {
    it.styleClass.add("statusbar")
    it.children.addAll(switch, label)
    HBox.setHgrow(label, Priority.ALWAYS)
  }

  private var isChangingSelected: Boolean = false
  private lateinit var status: LockStatus


  init {
    observableDocument.addListener(this::onDocumentChange)
    switch.selectedProperty().addListener(this::onSwitchAction)
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
        this.switch.isSelected = false
        this.switch.isDisable = true
        this.switch.text = ""
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
    this.switch.isSelected = status.locked
    this.isChangingSelected = false
    this.switch.text = ""
    if (status.locked) {
      this.label.graphic = FontAwesomeIconView(FontAwesomeIcon.LOCK)
      this.label.text = "Locked by ${status.lockOwnerName}"
    } else {
      this.label.graphic = FontAwesomeIconView(FontAwesomeIcon.UNLOCK)
      this.label.text = "Not locked"
    }

    this.switch.isDisable = status.locked && status.lockOwnerId != GPCloudOptions.userId.value
    this.status = status
  }

  private fun onSwitchAction(observable: Any, oldValue: Boolean, newValue: Boolean) {
    if (this.isChangingSelected) {
      return
    }
    val doc = this.observableDocument.get()
    val realDoc = if (doc is ProxyDocument) { doc.realDocument } else { doc }
    if (realDoc is LockableDocument) {
      val progress = ProgressIndicator()
      lockPanel.children.add(progress)
      val isNowLocked = this.switch.isSelected
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
}