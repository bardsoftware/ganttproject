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
import javafx.application.Platform
import javafx.beans.value.ObservableObjectValue
import javafx.scene.control.Tooltip
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.ProxyDocument
import org.controlsfx.control.ToggleSwitch

/**
 * @author dbarashev@bardsoftware.com
 */
class DocumentLockSwitch(observableDocument: ObservableObjectValue<Document>) {
  val switch = ToggleSwitch()

  init {
    observableDocument.addListener(this::onDocumentChange)
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
        this.switch.text = "Unlocked"
      }
    }
  }

  private fun onStatusChange(observable: Any, oldStatus: LockStatus, newStatus: LockStatus) {
    Platform.runLater {
      this.updateStatus(newStatus)
    }
  }

  private fun updateStatus(status: LockStatus) {
    this.switch.isSelected = status.locked
    this.switch.text = if (status.locked) "Locked" else "Unlocked"
    this.switch.tooltip = if (status.locked) { Tooltip("by ${status.lockOwnerName}") } else { null }

    println("my user id=${GPCloudOptions.userId.value}")
    println(status)
    this.switch.isDisable = status.locked && status.lockOwnerId != GPCloudOptions.userId.value
  }
}