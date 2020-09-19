/*
Copyright 2020 Dmitry Barashev, BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox

/**
 * @author dbarashev@bardsoftware.com
 */
class FolderItemCell : ListCell<ListViewItem<RecentDocAsFolderItem>>() {
  override fun updateItem(item: ListViewItem<RecentDocAsFolderItem>?, empty: Boolean) {
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

    pane.children.add(VBox().also { vbox ->
      vbox.isFillWidth = true
      vbox.children.add(
          Label(item.resource.get().basePath).apply {
            styleClass.add("list-item-path")
          }
      )
      vbox.children.add(
          Label(item.resource.get().name).apply {
            styleClass.add("list-item-filename")
          }
      )
      item.resource.value.tags.let {
        if (it.isNotEmpty()) {
          vbox.children.add(
              HBox(Label(it.joinToString(", "))).apply {
                styleClass.add("list-item-tags")
              }
          )
        }
      }
      StackPane.setAlignment(vbox, Pos.BOTTOM_LEFT)
    })

    graphic = pane
  }
}
