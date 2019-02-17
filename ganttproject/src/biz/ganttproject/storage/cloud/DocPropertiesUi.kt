/*
Copyright 2019 BarD Software s.r.o

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
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.storage.LockableDocument
import com.fasterxml.jackson.databind.JsonNode
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.GPLogger
import java.time.Duration

typealias OnLockDone = (JsonNode?) -> Unit
typealias BusyUi = (Boolean) -> Unit
typealias LockDurationHandler = (Duration) -> Unit
/**
 * @author dbarashev@bardsoftware.com
 */
class DocPropertiesUi(val errorUi: ErrorUi, val busyUi: BusyUi) {

  private fun lockPaneBuilder(): OptionPaneBuilder<Duration> {
    return OptionPaneBuilder<Duration>().apply {
      i18n.rootKey = "cloud.lockOptionPane"
      styleClass = "dlg-lock"
      styleSheets.add("/biz/ganttproject/storage/cloud/GPCloudStorage.css")
      graphic = FontAwesomeIconView(FontAwesomeIcon.UNLOCK)
      elements = listOf(
          OptionElementData("lock0h", Duration.ZERO),
          OptionElementData("lock1h", Duration.ofHours(1), isSelected = true),
          OptionElementData("lock2h", Duration.ofHours(2)),
          OptionElementData("lock24h", Duration.ofHours(24))
      )
    }
  }

  private fun lockDurationHandler(document: LockableDocument, onLockDone: OnLockDone): LockDurationHandler {
    return { duration ->
      if (!duration.isZero || document.status.get().locked) {
        toggleProjectLock(
            document = document,
            done = onLockDone,
            busyIndicator = busyUi,
            lockDuration = duration
        )
      } else {
        onLockDone(null)
      }
    }
  }

  fun createLockSuggestionPane(document: LockableDocument, onLockDone: OnLockDone): Pane {
    return lockPaneBuilder().run {
      buildDialogPane(lockDurationHandler(document, onLockDone))
    }
  }

  private fun toggleProjectLock(document: LockableDocument,
                                done: OnLockDone,
                                busyIndicator: BusyUi,
                                lockDuration: Duration = Duration.ofMinutes(10)) {
    busyIndicator(true)
    document.toggleLocked(lockDuration)
        .thenAccept { status ->
          done(status?.raw)
          busyIndicator(false)
        }
        .exceptionally { ex ->
          errorUi("Failed to lock document")
          GPLogger.log(ex)
          busyIndicator(false)
          return@exceptionally null
        }
  }

  fun showDialog(document: LockableDocument, onLockDone: OnLockDone) {
    Platform.runLater {
      val toggleGroup = ToggleGroup()
      val lockDurationHandler = lockDurationHandler(document, onLockDone)
      val vboxBuilder = VBoxBuilder().apply {
        add(node = lockPaneBuilder().let {
          it.toggleGroup = toggleGroup
          it.buildPane()
        }, alignment = Pos.CENTER, growth = Priority.ALWAYS)
        add(node = Button("Lock").also {
          it.addEventHandler(ActionEvent.ACTION) {
            val selectedDuration = toggleGroup.selectedToggle.userData as Duration
            lockDurationHandler(selectedDuration)
          }
        })
      }
      Dialog<Unit>().also {
        it.dialogPane.apply {
          content = vboxBuilder.vbox
          styleClass.addAll("dlg-lock")
          stylesheets.addAll("/biz/ganttproject/storage/cloud/GPCloudStorage.css", "/biz/ganttproject/storage/StorageDialog.css")

          buttonTypes.add(ButtonType.OK)
          lookupButton(ButtonType.OK).apply {
            styleClass.add("btn-attention")
          }
        }
        it.show()
      }
    }

  }
}
