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
import javafx.application.Platform
import javafx.beans.Observable
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.util.Callback
import net.sourceforge.ganttproject.gui.UIUtil
import org.controlsfx.control.BreadCrumbBar
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
  // Base path is a folder where this document sits
  val basePath: String
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
      val newSelection = listView.items.find { it.resource.value == selectedItem.resource.value }
      if (newSelection != null) {
        listView.selectionModel.select(newSelection)
      }
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
  return CellWithButtons(exceptionUi, onDeleteResource, onToggleLockResource, isLockingSupported, isDeleteSupported, itemActionFactory)
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

class BreadcrumbView(private val initialPath: Path, private val onSelectCrumb: Consumer<Path>) {
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

  fun show() {
    path = initialPath
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
  listView.listView.selectionModel.selectedIndices.addListener(ListChangeListener {
    // Until selection changes completes, which apparently happens after event processing, the list selection model
    // returns an old selected item. This builds the following scenario:
    // 1. Click on some ancestor folder in the breadcrumb
    // 2. Get "setCurrentFile" call, validate, everything is okay
    // 3. Load the contents of the new folder. It happens in the background worker.
    // 4. Set the new contents to the list view. It clears the view and the selection, and sends out the events
    // 5. Catch the selection event here and get old item as the selected one. Validate it and get wrong validation results
    //
    // Running "selectItem" later lets us complete with the selection change and when execution gets into "selectItem" we
    // already have correct selection in the model.
    Platform.runLater { selectItem(false, false) }
  })
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
        // Key pressed event may arrive before the value of text field changes.
        // We rely on the actual value of the text field down the stack,
        // so we run the processing code later.
        Platform.runLater { onFilenameEnter(false, false) }
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
