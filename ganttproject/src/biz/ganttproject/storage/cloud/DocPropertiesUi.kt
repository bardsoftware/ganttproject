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
import biz.ganttproject.storage.LockableDocument
import com.fasterxml.jackson.databind.JsonNode
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.scene.control.Dialog
import javafx.scene.control.DialogPane
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.GPLogger
import java.time.Duration
import java.util.function.Consumer

typealias OnLockDone = (JsonNode?) -> Unit
typealias BusyUi = (Boolean) -> Unit

/**
 * @author dbarashev@bardsoftware.com
 */
class DocPropertiesUi(val errorUi: ErrorUi, val busyUi: BusyUi) {

  fun createLockSuggestionPane(document: LockableDocument, onLockDone: OnLockDone): Pane {
    return OptionPaneBuilder<Duration>().run {
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

      buildDialogPane { duration ->
        if (duration.isZero) {
          if (document.status.get().locked) {

          } else {
            onLockDone(null)
          }
        } else {
          toggleProjectLock(
              document = document,
              done = Consumer { onLockDone(it) },
              busyIndicator = busyUi,
              lockDuration = duration
          )
        }
      }
    }
  }

  private fun toggleProjectLock(document: LockableDocument,
                                done: Consumer<JsonNode>,
                                busyIndicator: BusyUi,
                                lockDuration: Duration = Duration.ofMinutes(10)) {
    busyIndicator(true)
    document.toggleLocked(lockDuration)
        .thenAccept { status ->
          done.accept(status.raw!!)
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
      Dialog<Unit>().apply {
        this.dialogPane = createLockSuggestionPane(document, onLockDone) as DialogPane
        show()
      }
    }

  }
}
