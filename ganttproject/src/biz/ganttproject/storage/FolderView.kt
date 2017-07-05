// Copyright (C) 2016 BarD Software
package biz.ganttproject.storage

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.Observable
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.util.Callback
import net.sourceforge.ganttproject.document.webdav.WebDavResource
import java.util.*

class ListViewItem(resource: WebDavResource) {
  val isSelected: BooleanProperty = SimpleBooleanProperty()
  val resource: ObjectProperty<WebDavResource>

  init {
    this.resource = SimpleObjectProperty(resource)
  }
}

object ListViewItemToObservables : Callback<ListViewItem, Array<Observable>> {
  override fun call(item: ListViewItem?): Array<Observable> {
    return item?.let { arrayOf(
        item.isSelected as Observable, item.resource as Observable)
    }?: emptyArray()
  }
}

fun createListCell(
    dialogUi: StorageDialogBuilder.DialogUi,
    onDeleteResource: Runnable,
    onToggleLockResource: Runnable,
    isLockingSupported: BooleanProperty) : ListCell<ListViewItem> {
  return object : ListCell<ListViewItem>() {
    override fun updateItem(item: ListViewItem?, empty: Boolean) {
      try {
        doUpdateItem(item, empty)
      } catch (e: WebDavResource.WebDavException) {
        dialogUi.error(e)
      }

    }

    @Throws(WebDavResource.WebDavException::class)
    private fun doUpdateItem(item: ListViewItem?, empty: Boolean) {
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
      val hbox = HBox()
      hbox.styleClass.add("webdav-list-cell")
      val isLocked = item.resource.value.isLocked
      val isLockable = item.resource.value.isLockSupported(true)
      if (isLockable && !isLockingSupported.value) {
        isLockingSupported.value = true
      }

      val icon = if (isLocked)
        FontAwesomeIconView(FontAwesomeIcon.LOCK)
      else
        FontAwesomeIconView(FontAwesomeIcon.FOLDER)
      if (!item.resource.value.isCollection) {
        icon.styleClass.add("hide")
      } else {
        icon.styleClass.add("icon")
      }
      val label = Label(item.resource.value.name, icon)
      hbox.children.add(label)
      if (item.isSelected.value!! && !item.resource.value.isCollection) {
        val btnBox = HBox()
        btnBox.styleClass.add("webdav-list-cell-button-pane")
        val btnDelete = Button("", FontAwesomeIconView(FontAwesomeIcon.TRASH))
        btnDelete.addEventHandler(ActionEvent.ACTION) { event -> onDeleteResource.run() }

        var btnLock: Button? = null
        if (isLocked) {
          btnLock = Button("", FontAwesomeIconView(FontAwesomeIcon.UNLOCK))
        } else if (isLockable) {
          btnLock = Button("", FontAwesomeIconView(FontAwesomeIcon.LOCK))
        }
        if (btnLock != null) {
          btnLock.addEventHandler(ActionEvent.ACTION) { event -> onToggleLockResource.run() }
          btnBox.children.add(btnLock)
        }
        btnBox.children.add(btnDelete)
        HBox.setHgrow(btnBox, Priority.ALWAYS)
        hbox.children.add(btnBox)
      } else {
        val placeholder = Button("")
        placeholder.styleClass.add("hide")
        hbox.children.add(placeholder)
      }
      graphic = hbox
    }
  }
}
/**
 * @author dbarashev@bardsoftware.com
 */
class FolderView(val myDialogUi: StorageDialogBuilder.DialogUi,
                 onDeleteResource: Runnable,
                 onToggleLockResource: Runnable,
                 isLockingSupported: BooleanProperty) {

  val listView: ListView<ListViewItem> = ListView<ListViewItem>()
  init {
    listView.setCellFactory { _ ->
      createListCell(myDialogUi, onDeleteResource, onToggleLockResource, isLockingSupported)
    }
    listView.selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
      if (oldValue != null) {
        oldValue.isSelected.value = false
      }
      if (newValue != null) {
        newValue.isSelected.value = true
      }
    }
  }

  fun setResources(webDavResources: ObservableList<WebDavResource>) {
    val items = FXCollections.observableArrayList(ListViewItemToObservables)
    webDavResources.stream()
        .map({resource -> ListViewItem(resource) })
        .forEach({ items.add(it) })
    listView.items = items
  }


  val selectedResource: Optional<WebDavResource>
    get() {
      val result = listView.selectionModel.selectedItem?.resource?.value ?: null
      return Optional.ofNullable(result)
    }
}
