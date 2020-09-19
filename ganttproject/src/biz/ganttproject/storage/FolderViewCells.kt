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

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.event.ActionEvent
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import net.sourceforge.ganttproject.document.webdav.WebDavResource

open class FolderViewCell<T: FolderItem> : ListCell<ListViewItem<T>>() {
  protected fun whenNotEmpty(item: ListViewItem<T>?, empty: Boolean, code: FolderViewCell<T>.(item: ListViewItem<T>) -> Unit) {
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
    code(this, item)
  }
}
/**
 * Creates a list cell with the folder item name in bigger font, folder item base path above in smaller font
 * and folder item tags in the bottom right corner.
 *
 * @author dbarashev@bardsoftware.com
 */
class CellWithBasePath<T: FolderItem> : FolderViewCell<T>() {
  override fun updateItem(listViewItem: ListViewItem<T>?, empty: Boolean) {
    whenNotEmpty(listViewItem, empty) {item ->
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
}

class CellWithButtons<T: FolderItem>(
    private val exceptionUi: ExceptionUi,
    private val onDeleteResource: OnItemAction<T>,
    private val onToggleLockResource: OnItemAction<T>,
    private val isLockingSupported: BooleanProperty,
    private val isDeleteSupported: ReadOnlyBooleanProperty,
    private val itemActionFactory: ItemActionFactory<T>
) : FolderViewCell<T>() {

  override fun updateItem(item: ListViewItem<T>?, empty: Boolean) {
    try {
      doUpdateItem(item, empty)
    } catch (e: WebDavResource.WebDavException) {
      exceptionUi(e)
    } catch (e: Exception) {
      println(e)
    }

  }

  @Throws(WebDavResource.WebDavException::class)
  private fun doUpdateItem(listViewItem: ListViewItem<T>?, empty: Boolean) {
    whenNotEmpty(listViewItem, empty) { item ->

      val hbox = HBox()
      hbox.styleClass.add("webdav-list-cell")
      if (this.isSelected) {
        hbox.styleClass.add("selected")
      } else {
        hbox.styleClass.remove("selected")
      }
      val isLockable = item.resource.value.isLockable
      if (isLockable && !isLockingSupported.value) {
        isLockingSupported.value = true
      }

      val icon = if (item.resource.value.isDirectory) {
        FontAwesomeIconView(FontAwesomeIcon.FOLDER).also { it.styleClass.add("icon") }
      } else {
        null
      }
      val label = if (icon == null) Label(item.resource.value.name) else Label(item.resource.value.name, icon)
      hbox.children.add(label)
      hbox.children.add(buildLockButtons(item))
      graphic = hbox
    }
  }

  private fun buildLockButtons(item: ListViewItem<T>): Node {
    val isLocked = item.resource.value.isLocked
    val isLockable = item.resource.value.isLockable
    val canChangeLock = item.resource.value.canChangeLock

    val btnBox = HBox()
    btnBox.styleClass.add("webdav-list-cell-button-pane")
    if (item.isSelected.value && !item.resource.value.isDirectory) {
      val btnDelete =
          if (isDeleteSupported.get()) {
            Button("", FontAwesomeIconView(FontAwesomeIcon.TRASH))
          } else null

      val btnLock =
          when {
            isLocked -> Label("unlock", FontAwesomeIconView(FontAwesomeIcon.LOCK)).also {
              //.also {
              it.contentDisplay = ContentDisplay.GRAPHIC_ONLY
              it.tooltip = Tooltip("Click to release lock")
              it.styleClass.add("item-action")
            }
            isLockable -> Label("lock", FontAwesomeIconView(FontAwesomeIcon.UNLOCK)).also {
              //.also {
              it.contentDisplay = ContentDisplay.GRAPHIC_ONLY
              it.tooltip = Tooltip("Click to lock ${item.resource.value.name}")
              it.styleClass.add("item-action")
            }
            else -> null
          }

      if (btnLock != null) {
        btnLock.addEventHandler(MouseEvent.MOUSE_CLICKED) { onToggleLockResource(item.resource.value) }
        btnBox.children.add(btnLock)
      }
      if (btnDelete != null) {
        btnDelete.addEventHandler(ActionEvent.ACTION) { onDeleteResource(item.resource.value) }
        btnBox.children.add(btnDelete)
      }
      itemActionFactory.apply(item.resource.value).forEach { key, action ->
        createButton(key).also {
          it.addEventHandler(MouseEvent.MOUSE_CLICKED) { action(item.resource.value) }
          btnBox.children.add(it)
        }
      }
    } else {
      if (isLocked) {
        btnBox.children.add(Label("", FontAwesomeIconView(FontAwesomeIcon.LOCK)).also {
          it.contentDisplay = ContentDisplay.GRAPHIC_ONLY
          if (canChangeLock) {
            it.disableProperty().set(true)
            it.tooltip = Tooltip("Project ${item.resource.value.name} is locked. Click to release lock")
          } else {
            it.tooltip = Tooltip("Project ${item.resource.value.name} is locked.")
            it.disableProperty().set(true)
          }
        })
      }
    }
    if (!btnBox.children.isEmpty()) {
      HBox.setHgrow(btnBox, Priority.ALWAYS)
    }
    return btnBox
  }
}
