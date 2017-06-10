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

import javafx.scene.control.ListView
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.language.GanttLanguage
import java.util.*
import java.util.function.Consumer

/**
 * @author dbarashev@bardsoftware.com
 */
class RecentProjects(
        val myMode: StorageMode,
        val myRecentDocs: MutableList<String>,
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
    for (doc in myRecentDocs) {
      listView.items.add(doc)
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
