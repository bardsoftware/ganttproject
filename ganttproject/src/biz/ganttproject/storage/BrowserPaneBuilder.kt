/*
Copyright 2018 BarD Software s.r.o

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

import biz.ganttproject.lib.fx.VBoxBuilder
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.language.GanttLanguage
import org.controlsfx.control.StatusBar
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer

/**
 * This function takes path and loads path contents from the storage.
 * It should use "loading" callback to show or hide progress bar and
 * "success" callback to show created list of FolderItem elements in the UI.
 *
 * This function is supposed to run asynchronously in a background task.
 */
typealias Loader = (path: Path, success: Consumer<ObservableList<FolderItem>>, loading: Consumer<Boolean>) -> Unit

/**
 * This function is called when some action happens on some element, e.g. "select",
 * "delete", "open" or "launch".
 */
typealias OnItemAction = Consumer<FolderItem>


data class BrowserPaneElements(val breadcrumbView: BreadcrumbView,
                               val listView: FolderView<FolderItem>,
                               val pane: Pane)

/**
 * Builds browser pane UI from elements: breadcrumbs, list view, action button
 * and status bar with customization options.
 *
 * @author dbarashev@bardsoftware.com
 */
class BrowserPaneBuilder(
    private val mode: StorageDialogBuilder.Mode,
    private val dialogUi: StorageDialogBuilder.DialogUi,
    private val loader: Loader) {
  private val i18n = GanttLanguage.getInstance()
  private val rootPane = VBoxBuilder("pane-service-contents")

  private lateinit var listView: FolderView<FolderItem>
  private val busyIndicator = StatusBar().apply {
    text = ""
    HBox.setHgrow(this, Priority.ALWAYS)
  }
  private val errorLabel = Label("", FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE)).apply {
    styleClass.addAll("hint", "noerror")
  }
  private val filename = TextField()

  private lateinit var breadcrumbView: BreadcrumbView
  private lateinit var saveBox: HBox
  private lateinit var onOpenItem: OnItemAction
  private lateinit var onLaunch: OnItemAction

  fun withListView(
      onOpenItem: OnItemAction = Consumer {},
      onLaunch: OnItemAction = Consumer {},
      onDelete: OnItemAction = Consumer {},
      onLock: OnItemAction = Consumer {},
      canLock: BooleanProperty = SimpleBooleanProperty(false),
      canDelete: ReadOnlyBooleanProperty = SimpleBooleanProperty(false)) {
    this.listView = FolderView(
        this.dialogUi,
        onDelete,
        onLock,
        canLock, canDelete)
    this.onOpenItem = onOpenItem
    this.onLaunch = onLaunch
  }

  fun withBreadcrumbs() {
    val onSelectCrumb = Consumer { path: Path ->
      loader(path,
          Consumer { items -> this.listView.setResources(items) },
          Consumer { busyIndicator.progress = if (it) -1.0 else 0.0 }
      )
    }

    this.breadcrumbView = BreadcrumbView(Paths.get("/", "GanttProject Cloud"), onSelectCrumb)
  }

  fun withActionButton(onAction: EventHandler<ActionEvent>) {
    val btnSave = Button(i18n.getText("storageService.local.${this.mode.name.toLowerCase()}.actionLabel"))
    btnSave.addEventHandler(ActionEvent.ACTION, onAction)
    btnSave.styleClass.add("btn-attention")
    this.saveBox = HBox().apply {
      children.addAll(busyIndicator, btnSave)
      styleClass.add("doclist-save-box")
    }
  }

  private fun installEventHandlers() {
    fun selectItem(item: FolderItem, withEnter: Boolean, withControl: Boolean) {
      if (!withEnter) {
        return
      }
      if (item.isDirectory) {
        breadcrumbView.append(item.name)
        this.onOpenItem.accept(item)
        filename.text = ""
      } else if (!item.isDirectory) {
        this.onOpenItem.accept(item)
        filename.text = item.name
        if (withControl) {
          this.onLaunch.accept(item)
        }
      }
    }

    fun selectItem(withEnter: Boolean, withControl: Boolean) {
      listView.selectedResource.ifPresent { item -> selectItem(item, withEnter, withControl) }
    }

    fun onFilenameEnter() {
      val filtered = listView.doFilter(filename.text)
      if (filtered.size == 1) {
        selectItem(filtered[0], true, true)
      }
    }

    connect(filename, listView, breadcrumbView, ::selectItem, ::onFilenameEnter)
  }

  fun build(): BrowserPaneElements {
    installEventHandlers()
    rootPane.apply {
      vbox.prefWidth = 400.0
      addTitle(String.format("webdav.ui.title.%s",
          this@BrowserPaneBuilder.mode.name.toLowerCase()),
          "GanttProject Cloud")
      add(breadcrumbView.breadcrumbs)
      add(errorLabel)
      add(filename)
      add(listView.listView, alignment = null, growth = Priority.ALWAYS)
      add(saveBox)
    }
    return BrowserPaneElements(breadcrumbView, listView, rootPane.vbox)
  }
}
