/*
Copyright 2017 Dmitry Barashev, BarD Software s.r.o

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

import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.util.Callback
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.language.GanttLanguage
import java.util.*
import java.util.function.Consumer

/**
 * @author dbarashev@bardsoftware.com
 */
class RecentProjects(
        val myMode: StorageMode,
        val myDocumentManager: DocumentManager,
        val myCurrentDocument: Document,
        val myDocumentReceiver: Consumer<Document>) : StorageDialogBuilder.Ui {

  private val i18n = GanttLanguage.getInstance()

  override fun getCategory(): String {
    return "desktop"
  }

  override fun getId(): String {
    return "recent"
  }

  override fun createUi(): Pane {
    val rootPane = VBox()
    rootPane.styleClass.add("pane-service-contents")
    rootPane.prefWidth = 400.0

    val listView = ListView<String>()
    listView.cellFactory = Callback  {param -> object: ListCell<String>() {
      override fun updateItem(item: String?, empty: Boolean) {
        if (item == null) {
          text = ""
          graphic = null
          return
        }
        super.updateItem(item, empty)
        if (empty) {
          text = ""
          graphic = null
          return
        }
        val pane = StackPane()
        pane.minWidth = 0.0
        pane.prefWidth = 1.0
        val label = Label(item)
        StackPane.setAlignment(label, Pos.BOTTOM_LEFT)
        pane.children.add(label)
        graphic = pane
      }
    }}
    for (doc in myDocumentManager.recentDocuments) {
      listView.items.add(doc)
    }
    listView.onMouseClicked = EventHandler { event ->
      if (event.clickCount == 2 && listView.selectionModel.selectedItem != null) {
        myDocumentReceiver.accept(myDocumentManager.getDocument(listView.selectionModel.selectedItem))
      }
    }
    rootPane.children.add(listView)
    return rootPane
  }

  override fun getName(): String {
    return "Recent Projects"
  }

  override fun createSettingsUi(): Optional<Pane> {
    return Optional.empty()
  }


}
