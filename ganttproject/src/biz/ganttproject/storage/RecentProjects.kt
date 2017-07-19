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

import biz.ganttproject.storage.local.State
import biz.ganttproject.storage.local.ValidationHelper
import biz.ganttproject.storage.local.setupErrorLabel
import biz.ganttproject.storage.local.setupSaveButton
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.util.Callback
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.language.GanttLanguage
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * @author dbarashev@bardsoftware.com
 */
class RecentProjects(
        val myMode: StorageMode,
        val myDocumentManager: DocumentManager,
        val myCurrentDocument: Document,
        val myDocumentReceiver: Consumer<Document>) : StorageDialogBuilder.Ui {

  private val i18n = GanttLanguage.getInstance()
  private val  myUtil = StorageUtil(myMode)

  override fun getCategory(): String {
    return "desktop"
  }

  override fun getId(): String {
    return "recent"
  }

  override fun createUi(): Pane {
    val btnSave = Button(i18n.getText(myUtil.i18nKey("storageService.local.%s.actionLabel")))
    val filePath = Paths.get(myCurrentDocument.filePath)
    val state = State(
        SimpleObjectProperty(myUtil.absolutePrefix(filePath, filePath.nameCount - 1).toFile()),
        SimpleObjectProperty(myUtil.absolutePrefix(filePath).toFile()))

    val rootPane = VBox()
    rootPane.stylesheets.add("biz/ganttproject/storage/StorageDialog.css")
    rootPane.stylesheets.add("biz/ganttproject/storage/RecentProjects.css")
    rootPane.styleClass.add("pane-service-contents")
    rootPane.prefWidth = 400.0

    val listView = ListView<Path>()
    listView.cellFactory = Callback  {_ -> object: ListCell<Path>() {
      override fun updateItem(item: Path?, empty: Boolean) {
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
        val pathLabel = Label(item.parent.normalize().toString())
        pathLabel.styleClass.add("list-item-path")
        val nameLabel = Label(item.fileName.toString())
        nameLabel.styleClass.add("list-item-filename")
        val labelBox = VBox()
        labelBox.children.addAll(pathLabel, nameLabel)
        StackPane.setAlignment(labelBox, Pos.BOTTOM_LEFT)
        pane.children.add(labelBox)
        graphic = pane
      }
    }}
    listView.items.add(Paths.get(myCurrentDocument.path))
    for (doc in myDocumentManager.recentDocuments) {
      listView.items.add(Paths.get(doc))
    }
    val fakeTextField = TextField()
    listView.onMouseClicked = EventHandler { event ->
      if (listView.selectionModel.selectedItem != null) {
        fakeTextField.text = listView.selectionModel.selectedItem.toString()
      }
    }

    val validationHelper = ValidationHelper(fakeTextField,
        Supplier{-> listView.items.isEmpty()},
        state, myMode)
    val btnSaveBox = setupSaveButton(btnSave, state, myDocumentReceiver, validationHelper)
    val errorLabel = Label("", FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE))
    setupErrorLabel(errorLabel, validationHelper)
    rootPane.children.addAll(listView, errorLabel, btnSaveBox)
    return rootPane
  }

  override fun getName(): String {
    return "Recent Projects"
  }

  override fun createSettingsUi(): Optional<Pane> {
    return Optional.empty()
  }
}

