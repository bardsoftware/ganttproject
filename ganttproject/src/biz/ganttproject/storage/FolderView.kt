// Copyright (C) 2017 BarD Software
// Author: dbarashev@bardsoftware.com
//
// A set of classes for showing filesystem-like hierarchy as a list view and breadcrumb bar.
// Shown elements are expected to implement interface FolderItem.
//
// FolderView class encapsulates a list representing the contents of a single folder.
package biz.ganttproject.storage

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.Observable
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.util.Callback
import net.sourceforge.ganttproject.document.webdav.WebDavResource
import net.sourceforge.ganttproject.gui.UIUtil
import org.controlsfx.control.BreadCrumbBar
import org.controlsfx.control.textfield.TextFields
import java.nio.file.Path
import java.util.*
import java.util.function.Consumer

/**
 * Interface of a single filesystem item.
 */
interface FolderItem {
  // Is this item locked?
  val isLocked: Boolean
  // Is it possible to acquire exclusive lock on this item?
  val isLockable: Boolean
  // Item name
  val name: String
  // Is it a directory?
  val isDirectory: Boolean
  // Is it possible to change lock state: unlock if locked or lock if unlocked
  val canChangeLock: Boolean

  val buttons: List<Node>
}

/**
 * Encapsulates a list view showing the contents of a single folder.
 */
class FolderView<T : FolderItem>(
    val myDialogUi: StorageDialogBuilder.DialogUi,
    onDeleteResource: Consumer<T>,
    onToggleLockResource: Consumer<T>,
    isLockingSupported: BooleanProperty,
    isDeleteSupported: ReadOnlyBooleanProperty) {

  var myContents: ObservableList<T> = FXCollections.observableArrayList()
  val listView: ListView<ListViewItem<T>> = ListView()

  init {
    listView.setCellFactory { _ ->
      createListCell(myDialogUi, onDeleteResource, onToggleLockResource, isLockingSupported, isDeleteSupported)
    }
    listView.selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
      if (oldValue != null) {
        oldValue.isSelected.value = false
      }
      if (newValue != null) {
        newValue.isSelected.value = true
      }
    }
    listView.styleClass.add("folder-view")
  }

  /**
   * Loads the list of folder contents into the view.
   */
  fun setResources(folderContents: ObservableList<T>, keepSelection: Boolean = true) {
    val selectedItem = listView.selectionModel.selectedItem
    myContents = folderContents
    reloadItems(folderContents)
    if (selectedItem != null) {
      val newSelection = listView.items.find { it.resource.value.name == selectedItem.resource.value.name }
      listView.selectionModel.select(newSelection)
    }
  }

  private fun reloadItems(folderContents: ObservableList<T>) {
    val items = FXCollections.observableArrayList(createExtractor<T>())
    folderContents.stream()
        .map({ resource -> ListViewItem(resource) })
        .forEach({ items.add(it) })
    listView.items = items
  }

  /**
   * Property returning the selected resource.
   */
  val selectedResource: Optional<T>
    get() {
      val result = listView.selectionModel.selectedItem?.resource?.value
      return Optional.ofNullable(result)
    }

  fun filter(byValue: String) {
    reloadItems(FXCollections.observableArrayList(
        doFilter(FXCollections.observableArrayList(myContents), byValue)))
  }

  fun doFilter(contents: List<T>, byValue: String): List<T> {
    return contents.filter { it.name.toLowerCase().contains(byValue.toLowerCase()) }
  }

  fun doFilter(byValue: String): List<T> {
    return doFilter(FXCollections.observableArrayList(myContents), byValue)
  }

  fun requestFocus() {
    this.listView.requestFocus()
    this.listView.selectionModel.selectIndices(0)
  }

  fun isSelectedTopmost(): Boolean {
    return this.listView.selectionModel.isSelected(0)
  }
}

class ListViewItem<T : FolderItem>(resource: T) {
  val isSelected: BooleanProperty = SimpleBooleanProperty()
  val resource: ObjectProperty<T>

  init {
    this.resource = SimpleObjectProperty(resource)

  }
}

fun <T : FolderItem> createExtractor(): Callback<ListViewItem<T>, Array<Observable>> {
  return Callback { item ->
    item?.let {
      arrayOf(
          item.isSelected as Observable, item.resource as Observable)
    } ?: emptyArray()
  }
}

fun <T : FolderItem> createListCell(
    dialogUi: StorageDialogBuilder.DialogUi,
    onDeleteResource: Consumer<T>,
    onToggleLockResource: Consumer<T>,
    isLockingSupported: BooleanProperty,
    isDeleteSupported: ReadOnlyBooleanProperty): ListCell<ListViewItem<T>> {
  return object : ListCell<ListViewItem<T>>() {
    override fun updateItem(item: ListViewItem<T>?, empty: Boolean) {
      try {
        doUpdateItem(item, empty)
      } catch (e: WebDavResource.WebDavException) {
        dialogUi.error(e)
      }

    }

    @Throws(WebDavResource.WebDavException::class)
    private fun doUpdateItem(item: ListViewItem<T>?, empty: Boolean) {
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
          btnLock.addEventHandler(MouseEvent.MOUSE_CLICKED) { _ -> onToggleLockResource.accept(item.resource.value) }
          btnBox.children.add(btnLock)
        }
        //btnBox.children.add(Label("Foo"))
        if (btnDelete != null) {
          btnDelete.addEventHandler(ActionEvent.ACTION) { _ -> onDeleteResource.accept(item.resource.value) }
          btnBox.children.add(btnDelete)
        }
        btnBox.children.addAll(item.resource.value.buttons)
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
}

data class BreadcrumbNode(val path: Path, val label: String) {
  override fun toString(): String = this.label
}

class BreadcrumbView(initialPath: Path, val onSelectCrumb: Consumer<Path>) {
  val breadcrumbs = BreadCrumbBar<BreadcrumbNode>()
  var path: Path
    get() = breadcrumbs.selectedCrumb.value.path
    set(value) {
      var lastItem: TreeItem<BreadcrumbNode>? = null
      for (idx in 1..value.nameCount) {
        val treeItem = TreeItem<BreadcrumbNode>(
            BreadcrumbNode(value.root.resolve(value.subpath(0, idx)),
                value.getName(idx - 1).toString()))
        if (lastItem != null) {
          lastItem.children.add(treeItem)
        }
        lastItem = treeItem
        breadcrumbs.selectedCrumb = lastItem
      }
      onSelectCrumb.accept(value)
    }

  init {
    breadcrumbs.styleClass.add("breadcrumb")
    breadcrumbs.onCrumbAction = EventHandler { node ->
      node.selectedCrumb.children.clear()
      onSelectCrumb.accept(node.selectedCrumb.value.path)
    }
    path = initialPath
  }

  fun append(name: String) {
    val selectedPath = breadcrumbs.selectedCrumb.value.path
    val appendPath = selectedPath.resolve(name)
    val treeItem = TreeItem<BreadcrumbNode>(BreadcrumbNode(appendPath, name))
    breadcrumbs.selectedCrumb.children.add(treeItem)
    breadcrumbs.selectedCrumb = treeItem
    onSelectCrumb.accept(appendPath)
  }

  fun pop() {
    val parent = breadcrumbs.selectedCrumb.parent ?: return
    parent.children.clear()
    breadcrumbs.selectedCrumb = parent
    onSelectCrumb.accept(parent.value.path)
  }

}

fun <T : FolderItem> connect(
    filename: TextField, listView: FolderView<T>, breadcrumbView: BreadcrumbView,
    selectItem: (withEnter: Boolean, withControl: Boolean) -> Unit,
    onFilenameEnter: () -> Unit) {
  listView.listView.onMouseClicked = EventHandler { evt ->
    val dblClick = evt.clickCount == 2
    selectItem(dblClick, dblClick)
  }
  listView.listView.selectionModel.selectedIndices.addListener(
      ListChangeListener { selectItem(false, false) }
  )
  listView.listView.onKeyPressed = EventHandler { keyEvent ->
    when (keyEvent.code) {
      KeyCode.ENTER -> {
        selectItem(true, (keyEvent.isControlDown || keyEvent.isMetaDown))
      }
      KeyCode.UP -> {
        if (listView.isSelectedTopmost()) {
          filename.requestFocus()
          listView.listView.selectionModel.clearSelection()
        }
      }
      KeyCode.BACK_SPACE -> {
        breadcrumbView.pop()
      }
      else -> {
      }
    }
  }

  TextFields.bindAutoCompletion(filename) { req ->
    // Filter folder with user text and map each item to its name. Return the result if
    // filtered list has less than 5 items.
    listView.doFilter(req.userText).let {
      if (it.size <= 5) it.map { it -> it.name }.toList() else emptyList<String>()
    }
  }
  filename.onKeyPressed = EventHandler { keyEvent ->
    when (keyEvent.code) {
      KeyCode.DOWN -> listView.requestFocus()
      KeyCode.ENTER -> {
        onFilenameEnter()
      }
      else -> {
      }
    }
  }
}

fun createButton(id: String, onAction: () -> Unit): Node {
  val text = UIUtil.getUiProperty("projectPane.browser.item.action.$id.text")
  val iconName = UIUtil.getUiProperty("projectPane.browser.item.action.$id.icon")
  val label = Label(text, FontAwesomeIconView(FontAwesomeIcon.valueOf(iconName))).also {
    it.contentDisplay = ContentDisplay.GRAPHIC_ONLY
    it.styleClass.add("item-action")
  }
  label.addEventHandler(MouseEvent.MOUSE_CLICKED) { _ -> onAction() }
  return label
}
