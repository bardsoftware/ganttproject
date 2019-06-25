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

import biz.ganttproject.app.DefaultLocalizer
import biz.ganttproject.app.Localizer
import biz.ganttproject.lib.fx.VBoxBuilder
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import org.controlsfx.control.StatusBar
import org.controlsfx.control.textfield.CustomTextField
import org.controlsfx.validation.ValidationResult
import org.controlsfx.validation.ValidationSupport
import org.controlsfx.validation.Validator
import org.controlsfx.validation.decoration.StyleClassValidationDecoration
import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.Stream

/**
 * This function takes path and loads path contents from the storage.
 * It should use "loading" callback to show or hide progress bar and
 * "success" callback to show created list of FolderItem elements in the UI.
 *
 * This function is supposed to run asynchronously in a background task.
 */
typealias Loader<T> = (path: Path, success: Consumer<ObservableList<T>>, loading: Consumer<Boolean>) -> Unit

/**
 * This function is called when some action happens on some element, e.g. "select",
 * "delete", "open" or "launch".
 */
typealias OnItemAction<T> = Consumer<T>

typealias ItemActionFactory<T> = Function<FolderItem, Map<String, OnItemAction<T>>>

data class BrowserPaneElements<T: FolderItem>(val breadcrumbView: BreadcrumbView?,
                               val listView: FolderView<T>,
                               val filenameInput: CustomTextField,
                               val browserPane: Pane,
                               val busyIndicator: Consumer<Boolean>,
                               val errorLabel: Label,
                               val validationSupport: ValidationSupport)

/**
 * Builds browser pane UI from elements: breadcrumbs, list view, action button
 * and status bar with customization options.
 *
 * @author dbarashev@bardsoftware.com
 */
class BrowserPaneBuilder<T: FolderItem>(
    private val mode: StorageDialogBuilder.Mode,
    private val exceptionUi: ExceptionUi,
    private val loader: Loader<T>) {
  private val rootPane = VBoxBuilder("pane-service-contents")

  private lateinit var listView: FolderView<T>
  private val busyIndicator = StatusBar().apply {
    text = ""
    HBox.setHgrow(this, Priority.ALWAYS)
  }
  private val errorLabel = Label("", FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE)).apply {
    styleClass.addAll("hint", "noerror")
  }
  private val filename = CustomTextField()
  private val listViewHint = Label().also {
    it.styleClass.addAll("hint", "noerror")
  }
  private lateinit var btnSave: Button

  private var breadcrumbView: BreadcrumbView? = null
  private lateinit var saveBox: HBox
  private lateinit var onOpenItem: OnItemAction<T>
  private lateinit var onLaunch: OnItemAction<T>
  private var validationSupport: ValidationSupport = ValidationSupport()
  private lateinit var i18n: Localizer

  val busyIndicatorToggler: Consumer<Boolean>
    get() = Consumer { Platform.runLater { busyIndicator.progress = if (it) -1.0 else 0.0 } }

  val resultConsumer: Consumer<ObservableList<T>>
    get() = Consumer { Platform.runLater { this.listView.setResources(it) } }


  fun withI18N(i18n: DefaultLocalizer) {
    this.i18n = i18n
  }

  fun withListView(
      onOpenItem: OnItemAction<T> = Consumer {},
      onLaunch: OnItemAction<T> = Consumer {},
      onDelete: OnItemAction<T> = Consumer {},
      onLock: OnItemAction<T> = Consumer {},
      canLock: BooleanProperty = SimpleBooleanProperty(false),
      canDelete: ReadOnlyBooleanProperty = SimpleBooleanProperty(false),
      itemActionFactory: ItemActionFactory<T> = Function { Collections.emptyMap() },
      cellFactory: CellFactory<T>? = null) {
    this.listView = FolderView(
        this.exceptionUi,
        onDelete,
        onLock,
        canLock, canDelete, itemActionFactory, cellFactory)
    this.onOpenItem = onOpenItem
    this.onLaunch = onLaunch
  }

  fun withBreadcrumbs(rootPath: Path) {
    val onSelectCrumb = Consumer { path: Path ->
      this.loader(path,
          resultConsumer,
          busyIndicatorToggler
      )
    }

    this.breadcrumbView = BreadcrumbView(rootPath, onSelectCrumb)
  }

  fun withActionButton(onAction: EventHandler<ActionEvent>) {
    this.btnSave = Button().also {
      it.textProperty().bind(i18n.create("${this.mode.name.toLowerCase()}.actionLabel"))
    }
    btnSave.addEventHandler(ActionEvent.ACTION, onAction)
    btnSave.styleClass.add("btn-attention")

    this.saveBox = HBox().apply {
      children.addAll(busyIndicator, btnSave)
      styleClass.add("doclist-save-box")
    }
  }

  fun withValidator(validator: Validator<String>) {
    errorLabel.styleClass.addAll("hint", "noerror")
    this.validationSupport.apply {
      registerValidator(filename, validator)
      validationDecorator = StyleClassValidationDecoration("error", "warning")
    }

    validationSupport.validationResultProperty().addListener { _, _, validationResult ->
      if (validationResult.errors.size + validationResult.warnings.size > 0) {
        errorLabel.text = formatError(validationResult)
        errorLabel.styleClass.remove("noerror")
        if (validationResult.errors.isNotEmpty()) {
          errorLabel.graphic = FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE)
          errorLabel.styleClass.remove("warning")
          errorLabel.styleClass.add("error")
          filename.styleClass.remove("warning")
          filename.styleClass.add("error")
          btnSave.isDisable = true
        } else if (validationResult.warnings.isNotEmpty()) {
          errorLabel.graphic = null
          errorLabel.styleClass.remove("error")
          errorLabel.styleClass.add("warning")
          filename.styleClass.remove("error")
          filename.styleClass.add("warning")
          btnSave.isDisable = false
        }
      } else {
        errorLabel.text = ""
        filename.styleClass.removeAll("error", "warning")
        errorLabel.styleClass.removeAll("error", "warning")
        errorLabel.styleClass.add("noerror")
        btnSave.isDisable = false
      }
    }
  }

  fun withListViewHint(value: ObservableValue<String>) {
    this.listViewHint.textProperty().bind(value)
  }

  private fun installEventHandlers() {
    fun selectItem(item: T, withEnter: Boolean, withControl: Boolean) {
      when {
        withEnter && item.isDirectory -> {
          breadcrumbView?.append(item.name)
          this.onOpenItem.accept(item)
          filename.text = ""
        }
        withEnter && !item.isDirectory -> {
          this.onOpenItem.accept(item)
          filename.text = item.name
          if (withControl) {
            this.onLaunch.accept(item)
          }
        }
        !withEnter && item.isDirectory -> {
        }
        !withEnter && !item.isDirectory -> {
          this.onOpenItem.accept(item)
          filename.text = item.name
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

    listView.listView.selectionModel.selectedIndices.addListener(ListChangeListener {
      if (listView.listView.selectionModel.isEmpty) {
        listViewHint.styleClass.remove("warning")
        listViewHint.styleClass.addAll("noerror")
      } else {
        listViewHint.styleClass.remove("noerror")
        listViewHint.styleClass.addAll("warning")
      }
    })

    connect(filename, listView, breadcrumbView, ::selectItem, ::onFilenameEnter)
  }

  fun build(): BrowserPaneElements<T> {
    installEventHandlers()
    rootPane.apply {
      vbox.prefWidth = 400.0
      addTitle(this@BrowserPaneBuilder.i18n.create("${this@BrowserPaneBuilder.mode.name.toLowerCase()}.title")).also {
        it.styleClass.add("title-integrated")
      }
      add(VBox().also {vbox ->
        vbox.styleClass.add("nav-search")
        breadcrumbView?.let { vbox.children.add(it.breadcrumbs) }
        vbox.children.addAll(
            filename,
            errorLabel
        )
      })
      add(listView.listView, alignment = null, growth = Priority.ALWAYS)
      add(listViewHint)
      add(saveBox)
    }
    return BrowserPaneElements(breadcrumbView, listView, filename, rootPane.vbox, busyIndicatorToggler, errorLabel, validationSupport)
  }
}

private fun formatError(validation: ValidationResult): String {
  return Stream.concat(validation.errors.stream(), validation.warnings.stream())
      .map { error -> error.text }
      .collect(Collectors.joining("\n"))
}

val BROWSE_PANE_LOCALIZER = DefaultLocalizer("storageService._default")
