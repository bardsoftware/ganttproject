/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.ganttview

import biz.ganttproject.app.dialog
import biz.ganttproject.core.option.ObservableObject
import biz.ganttproject.core.option.ObservableProperty
import biz.ganttproject.core.option.ObservableString
import biz.ganttproject.lib.fx.VBoxBuilder
import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.util.Callback
import net.sourceforge.ganttproject.action.CancelAction
import net.sourceforge.ganttproject.action.OkAction

fun showFilterDialog(filterManager: TaskFilterManager) {
  dialog(title = "Filter Dialog") { dlg ->
    dlg.addStyleClass("dlg-list-view-editor")
    dlg.addStyleSheet("/biz/ganttproject/ganttview/ListViewEditorDialog.css")
    dlg.setHeader(
      VBoxBuilder("header").apply {
        addTitle("Task Filters").also { hbox ->
          hbox.alignment = Pos.CENTER_LEFT
          hbox.isFillHeight = true
        }
      }.vbox
    )

    val editItem = ObservableObject<TaskFilter?>("", null)
    val model = FilterEditorModel(editItem)
    val editor = FilterEditor(editItem, model.fields)

    val listView = ListView<TaskFilter>().apply {
      cellFactory = Callback { ShowHideListCell { filter ->
        ShowHideListItem(filter.title, filter.isEnabledProperty)
      } }
      items = FXCollections.observableArrayList(filterManager.filters)
      selectionModel.selectedItemProperty().addListener { _, _, newValue ->
        if (newValue != null) {
          editItem.set(newValue, this@apply)
        }
      }
      selectionModel.select(0)
    }

    val content = HBox().also {
      it.children.addAll(listView, editor.node)
      HBox.setHgrow(editor.node, Priority.ALWAYS)
    }
    dlg.setContent(content)

    dlg.setupButton(OkAction.create("ok") {})
    dlg.setupButton(CancelAction.create("cancel") {})
  }
}

internal class FilterEditorModel(editItem: ObservableObject<TaskFilter?>) {
  val nameField = ObservableString("name", "")
  val descriptionField = ObservableString("description", "")
  val fields = listOf(nameField)

  init {
    editItem.addWatcher {
      if (it.trigger != this) {
        nameField.set(it.newValue?.title)
        descriptionField.set(it.newValue?.description)
      }
    }
  }

}
internal class FilterEditor(editItem: ObservableProperty<TaskFilter?>, fields: List<ObservableProperty<*>>): ItemEditorPane<TaskFilter?>(
  editItem = editItem, fields = fields, ourEditorLocalizer
)