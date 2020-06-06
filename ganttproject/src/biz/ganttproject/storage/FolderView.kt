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
import javafx.geometry.Pos
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
import java.io.File
import java.util.*
import java.util.function.Consumer
import java.util.function.Function

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
  // Item tags, indicating whether item is local, is read-only, etc
  val tags: List<String>
}

private val unsupported = SimpleBooleanProperty(false)

typealias CellFactory<R> = () -> ListCell<ListViewItem<R>>
typealias ExceptionUi = (Exception) -> Unit

/**
 * Encapsulates a list view showing the contents of a single folder.
 */
class FolderView<T : FolderItem>(
    val exceptionUi: ExceptionUi,
    onDeleteResource: OnItemAction<T> = {},
    onToggleLockResource: OnItemAction<T> = {},
    isLockingSupported: BooleanProperty = unsupported,
    isDeleteSupported: ReadOnlyBooleanProperty = unsupported,
    private val itemActionFactory: ItemActionFactory<T> = Function { Collections.emptyMap() },
    maybeCellFactory: CellFactory<T>? = null) {

  private val cellFactory: CellFactory<T> = maybeCellFactory ?: {
    createListCell(exceptionUi, onDeleteResource, onToggleLockResource, isLockingSupported, isDeleteSupported, itemActionFactory)
  }
  var document: OnlineDocument? = null
  var myContents: ObservableList<T> = FXCollections.observableArrayList()
  val listView: ListView<ListViewItem<T>> = ListView()

  init {
    listView.setCellFactory {
      this.cellFactory()
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
  fun setResources(folderContents: ObservableList<T>) {
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
        .map { resource -> ListViewItem(resource) }
        .forEach { items.add(it) }
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
    exceptionUi: ExceptionUi,
    onDeleteResource: OnItemAction<T>,
    onToggleLockResource: OnItemAction<T>,
    isLockingSupported: BooleanProperty,
    isDeleteSupported: ReadOnlyBooleanProperty,
    itemActionFactory: ItemActionFactory<T>): ListCell<ListViewItem<T>> {
  return object : ListCell<ListViewItem<T>>() {
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
}

typealias Path = DocumentUri

fun createPath(file: File): Path {
  return DocumentUri.createPath(file)
}

fun createPath(pathAsString: String): Path {
  return DocumentUri.createPath(pathAsString)
}

data class BreadcrumbNode(val path: Path, val label: String) {
  override fun toString(): String = this.label

}

class BreadcrumbView(initialPath: Path, private val onSelectCrumb: Consumer<Path>) {
  val breadcrumbs = BreadCrumbBar<BreadcrumbNode>()
  private val defaultCrumbFactory = breadcrumbs.crumbFactory
  var path: Path
    get() = breadcrumbs.selectedCrumb.value.path
    set(value) {
      var lastItem: TreeItem<BreadcrumbNode> = TreeItem(BreadcrumbNode(value.getRoot(), value.getRootName()))
      for (idx in 0 until value.getNameCount()) {
        val treeItem = TreeItem<BreadcrumbNode>(
            BreadcrumbNode(value.subpath(0, idx+1),
                value.getName(idx)))
        lastItem.children?.add(treeItem)
        lastItem = treeItem
      }
      breadcrumbs.selectedCrumb = lastItem
      onSelectCrumb.accept(value)
    }

  init {
    breadcrumbs.styleClass.add("breadcrumb")
    breadcrumbs.onCrumbAction = EventHandler { event ->
      event.selectedCrumb.children.clear()
      val idx = event.selectedCrumb.value.path.getNameCount()
      val selectedButton = breadcrumbs.childrenUnmodifiable[idx]
      breadcrumbs.childrenUnmodifiable.forEach { it.styleClass.add("crumb-selected") }
      onSelectCrumb.accept(event.selectedCrumb.value.path)
    }
    breadcrumbs.crumbFactory = Callback { treeItem ->
      defaultCrumbFactory.call(treeItem).also {
        if (treeItem.isLeaf) {
          it.styleClass.add("crumb-selected")
        }
      }
    }

    path = initialPath
  }

  fun append(name: String) {
    val selectedPath = breadcrumbs.selectedCrumb.value.path
    val appendPath = selectedPath.resolve(name)
    val treeItem = TreeItem(BreadcrumbNode(appendPath, name))
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
    filename: TextField?, listView: FolderView<T>, breadcrumbView: BreadcrumbView?,
    /**
     * This is called on selection change or on some action with the selected item.
     * In case of mere selection change both withEnter and withControl are false
     * In case of double-click or hitting Ctrl+Enter in the list both flags are true.
     * In case of hitting Enter in the list withEnter == true and withControl == false.
     * In case of typing in the text search field and hitting Enter, withEnter == true and
     * withControl depends on whether modifier key is hold.
     *
     * Typical expected behavior of the handler:
     * - both flags false (selection change) may update some UI control state (e.g. disable or enable action button)
     * - both flags true (dbl-click or Ctrl+Enter) is equivalent to action button click or to opening a folder
     * - withEnter == true may open a folder or do something with file, depending on what is selected
     */
    selectItem: (withEnter: Boolean, withControl: Boolean) -> Unit,
    onFilenameEnter: (withEnter: Boolean, withControl: Boolean) -> Unit) {
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
        if (listView.isSelectedTopmost() && filename != null) {
          filename.requestFocus()
          listView.listView.selectionModel.clearSelection()
        }
      }
      KeyCode.BACK_SPACE -> {
        breadcrumbView?.pop()
      }
      else -> {
      }
    }
  }

//  TextFields.bindAutoCompletion(filename) { req ->
//    // Filter folder with user text and map each item to its name. Return the result if
//    // filtered list has less than 5 items.
//    listView.doFilter(req.userText).let {
//      if (it.size <= 5) it.map { it.name }.toList() else emptyList()
//    }
//  }
  filename?.onKeyPressed = EventHandler { keyEvent ->
    when (keyEvent.code) {
      KeyCode.DOWN -> listView.requestFocus()
      KeyCode.ENTER -> {
        onFilenameEnter(true, keyEvent.isControlDown || keyEvent.isMetaDown)
      }
      else -> {
        onFilenameEnter(false, keyEvent.isControlDown || keyEvent.isMetaDown)
      }
    }
  }
}

fun createButton(id: String): Node {
  val text = UIUtil.getUiProperty("projectPane.browser.item.action.$id.text")
  val iconName = UIUtil.getUiProperty("projectPane.browser.item.action.$id.icon")
  val label = Label(text, FontAwesomeIconView(FontAwesomeIcon.valueOf(iconName))).also {
    it.contentDisplay = ContentDisplay.GRAPHIC_ONLY
    it.styleClass.add("item-action")
    it.alignment = Pos.CENTER
  }
  return label
}
