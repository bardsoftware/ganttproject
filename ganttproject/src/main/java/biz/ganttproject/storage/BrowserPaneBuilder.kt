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

import biz.ganttproject.app.Localizer
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.vbox
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView
import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.ReadOnlyBooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.value.ObservableValue
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.*
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
typealias OnItemAction<T> = (T) -> Unit

typealias ItemActionFactory<T> = Function<FolderItem, Map<String, OnItemAction<T>>>

data class BrowserPaneElements<T: FolderItem>(
    val breadcrumbView: BreadcrumbView?,
    val listView: FolderView<T>,
    val filenameInput: CustomTextField,
    val browserPane: Pane,
    val busyIndicator: Consumer<Boolean>,
    val errorLabel: Label,
    val validationSupport: ValidationSupport,
    val confirmationCheckBox: CheckBox?
) {
  init {
    validationSupport.validationResultProperty().addListener { _, _, validationResult ->
      setValidationResult(validationResult)
    }
  }

  val filenameWithExtension: String get() = filenameInput.text.withGanExtension()

  fun setValidationResult(validationResult: ValidationResult) {
    if (validationResult.errors.size + validationResult.warnings.size > 0) {
      errorLabel.text = formatError(validationResult)
      errorLabel.parent.styleClass.removeAll("noerror")
      if (validationResult.errors.isNotEmpty()) {
        errorLabel.graphic = FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE)
        errorLabel.parent.styleClass.removeAll("warning")
        errorLabel.parent.styleClass.add("error")
        filenameInput.styleClass.removeAll("warning")
        filenameInput.styleClass.add("error")
        //btnSave.isDisable = true
      } else if (validationResult.warnings.isNotEmpty()) {
        errorLabel.graphic = null
        errorLabel.parent.styleClass.removeAll("error")
        errorLabel.parent.styleClass.add("warning")
        filenameInput.styleClass.removeAll("error")
        filenameInput.styleClass.add("warning")
        //btnSave.isDisable = false
      }
    } else {
      errorLabel.text = ""
      filenameInput.styleClass.removeAll("error", "warning")
      errorLabel.parent.styleClass.removeAll("error", "warning")
      errorLabel.parent.styleClass.add("noerror")
      //btnSave.isDisable = false
    }
  }

}

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
//  private val busyIndicator = StatusBar().apply {
//    text = ""
//    HBox.setHgrow(this, Priority.ALWAYS)
//  }
  private val progressBar = ProgressBar().also {
    it.styleClass.add("folder-view-progress")
  }
  private val errorLabel = Label("", FontAwesomeIconView(FontAwesomeIcon.EXCLAMATION_TRIANGLE))
  private val filename = CustomTextField()
  private val listViewHint = Label().also {
    it.styleClass.addAll("hint", "folder-view-hint", "noerror")
  }
  private val confirmationCheckBox = CheckBox()
  private var hasConfirmation = false

  private lateinit var btnSave: Button

  private var breadcrumbView: BreadcrumbView? = null
  private lateinit var onSelectionChange: OnItemAction<T>
  private lateinit var onLaunch: OnItemAction<T>
  private lateinit var onOpenDirectory: OnItemAction<T>
  private lateinit var onNameTyped: (filename: String, matchedItems: List<T>, withEnter: Boolean, withControl: Boolean) -> Unit
  private var validationSupport: ValidationSupport = ValidationSupport()
  private lateinit var i18n: Localizer
  private var hasValidator = false

  val busyIndicatorToggler: Consumer<Boolean>
    get() = Consumer { Platform.runLater {
      progressBar.progress = if (it) -1.0 else 0.0
    }}

  val resultConsumer: Consumer<ObservableList<T>>
    get() = Consumer { Platform.runLater { this.listView.setResources(it) } }


  fun withI18N(i18n: Localizer) {
    this.i18n = i18n
  }

  fun withListView(
      /** This is called on double-click or Enter */
      onSelectionChange: OnItemAction<T> = {},
      onLaunch: OnItemAction<T> = {},
      onOpenDirectory: OnItemAction<T> = {},
      onDelete: OnItemAction<T> = {},
      onLock: OnItemAction<T> = {},
      onNameTyped: (filename: String, matchedItems: List<T>, withEnter: Boolean, withControl: Boolean) -> Unit = { _, _, _, _ ->},
      canLock: BooleanProperty = SimpleBooleanProperty(false),
      canDelete: ReadOnlyBooleanProperty = SimpleBooleanProperty(false),
      itemActionFactory: ItemActionFactory<T> = Function { Collections.emptyMap() },
      cellFactory: CellFactory<T>? = null) {
    this.listView = FolderView(
        this.exceptionUi,
        onDelete,
        onLock,
        canLock, canDelete, itemActionFactory, cellFactory)
    this.onSelectionChange = onSelectionChange
    this.onOpenDirectory = onOpenDirectory
    this.onLaunch = onLaunch
    this.onNameTyped = onNameTyped
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

  fun withActionButton(btnSetup: (Button) -> Unit) {
    this.btnSave = Button().also {
      it.textProperty().bind(i18n.create("${this.mode.name.toLowerCase()}.actionLabel"))
    }
    btnSave.styleClass.add("btn-attention")

    btnSetup(btnSave)
  }

  fun withValidator(validator: Validator<String>) {
    hasValidator = true
    errorLabel.styleClass.addAll("hint", "hint-validation", "noerror")
    this.validationSupport.apply {
      registerValidator(filename, validator)
      validationDecorator = StyleClassValidationDecoration("error", "warning")
    }
  }

  fun withListViewHint(value: ObservableValue<String>) {
    this.listViewHint.textProperty().bind(value)
  }

  fun withConfirmation(label: ObservableValue<String>, isRequired: ObservableValue<Boolean>) {
    this.confirmationCheckBox.textProperty().bind(label)
    this.confirmationCheckBox.visibleProperty().bind(isRequired)
    this.hasConfirmation = true
  }

  private fun installEventHandlers() {
    fun selectItem(item: T, withEnter: Boolean, withControl: Boolean) {
      when {
        // It is just selection change. Call the listener and update
        // filename in the text field unless it is a directory.
        !withEnter && item.isDirectory -> {
          this.onSelectionChange(item)
        }
        !withEnter && !item.isDirectory -> {
          this.onSelectionChange(item)
          filename.text = item.name
        }
        // Enter key is hold. We need to distinguish "launch" case (Ctrl+Enter or double-click) for
        // non-folders vs just Enter.
        withEnter && item.isDirectory -> {
          // We just open directories, no matter if Ctrl is hold or not
          breadcrumbView?.append(item.name)
          this.onSelectionChange(item)
          this.onOpenDirectory(item)
        }
        withEnter && !item.isDirectory -> {
          this.onSelectionChange(item)
          filename.text = item.name
          if (withControl) {
            this.onLaunch(item)
          }
        }
      }
    }

    fun selectItem(withEnter: Boolean, withControl: Boolean) {
      listView.selectedResource.ifPresent { item -> selectItem(item, withEnter, withControl) }
    }

    fun onFilenameEnter(withEnter: Boolean, withControl: Boolean) {
      val filtered = listView.doFilter(filename.text)
      if (filtered.size == 1 && withEnter) {
        selectItem(filtered[0], withEnter, withEnter)
      }
      listView.filter(filename.text)
      onNameTyped(filename.text, filtered, withEnter, withControl)
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
      add(vbox {
        addClasses("nav-search")
        breadcrumbView?.let { add(it.breadcrumbs) }
        add(filename)
        if (hasValidator) {
          add(HBox().also {
            it.styleClass.add("hint-validation-pane")
            it.children.add(errorLabel)
          })
        }
      })
      add(progressBar.also {
        it.maxWidth = Double.MAX_VALUE
      })
      add(listView.listView, alignment = null, growth = Priority.ALWAYS)
      add(listViewHint)
      HBox().apply {
        children.addAll(
            Pane().also {
              if (hasConfirmation) {
                it.children.add(confirmationCheckBox)
              }
              HBox.setHgrow(it, Priority.ALWAYS)
            },
            btnSave
        )
        styleClass.add("doclist-save-box")
      }.also {
        add(it)
      }
    }

    return BrowserPaneElements(breadcrumbView, listView, filename, rootPane.vbox, busyIndicatorToggler, errorLabel, validationSupport,
        if (hasConfirmation) confirmationCheckBox else null
    )
  }
}

private fun formatError(validation: ValidationResult): String {
  return Stream.concat(validation.errors.stream(), validation.warnings.stream())
      .map { error -> error.text }
      .collect(Collectors.joining("\n"))
}

val BROWSE_PANE_LOCALIZER = RootLocalizer.createWithRootKey("storageService._default")
