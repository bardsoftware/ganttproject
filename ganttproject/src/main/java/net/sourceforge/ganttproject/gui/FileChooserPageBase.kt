/*
 * Copyright (c) 2011-2026 Dmitry Barashev, BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
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
package net.sourceforge.ganttproject.gui

import biz.ganttproject.app.*
import biz.ganttproject.core.option.*
import biz.ganttproject.lib.fx.GPListCell
import biz.ganttproject.lib.fx.buildFontAwesomeButton
import biz.ganttproject.lib.fx.hbox
import biz.ganttproject.lib.fx.vbox
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.embed.swing.SwingNode
import javafx.scene.Node
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder
import org.osgi.service.prefs.Preferences
import java.awt.BorderLayout
import java.awt.Component
import java.io.File
import javax.swing.BorderFactory
import javax.swing.JFileChooser
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Base class for the file chooser pages in the Import and Export wizards.
 */
abstract class FileChooserPageBase protected constructor(
  private val myDocument: Document?,
  val fileChooserTitle: String?,
  val pageTitle: String?,
  val fileChooserSelectionMode: Int = JFileChooser.FILES_ONLY,
  val errorMessage: ObservableString
) : WizardPage {
  val fxFile = ObservableFile("file", null)
  val chosenFiles = FXCollections.observableArrayList<ChosenFile>()
  var allowMultipleChoice: Boolean = false

  private var fileFilter: FileExtensionFilter? = null
    set(value) {
      field = value
      if (value != null) {
        extensionFilters.clear()
        extensionFilters.add(value)
      }
    }
  private var extensionFilters: MutableList<FileExtensionFilter> = mutableListOf()
    set(value) {
      if (field.isNotEmpty()) {
        value.addAll(field)
      }
      field = value
    }
  protected val fxOverwrite = ObservableBoolean("overwrite", false)
  abstract val preferences: Preferences
  private val secondaryOptionsSwingNode = SwingNode()

  init {
    fxFile.addWatcher { event ->
      tryChosenFile(event.newValue)
      if (allowMultipleChoice) {
        event.newValue?.let {
          val newChosenFile = ChosenFile(event.newValue!!, chosenFiles, this::validateFile)
          if (!chosenFiles.contains(newChosenFile)) {
            chosenFiles.add(newChosenFile)
          }
        }
      }
    }
    fxOverwrite.addWatcher { tryChosenFile(fxFile.value) }
  }

  override val component: Component? = null
  private var resetCenterPane: ()->Unit = {}

  override val fxComponent: Node by lazy {
    val root = BorderPane()
    root.styleClass.add("file-chooser-page")
    val i18n = i18n {
      default()
      map(mapOf(
        "file.label" to "file",
        "overwrite.label" to "option.exporter.overwrite.label.trailing"
      ))
    }
    val sheet = PropertySheetBuilder(i18n).pane {
      file(fxFile) {
        chooserTitle = fileChooserTitle ?: ""
        isSaveNotOpen = fileChooserSelectionMode != JFileChooser.FILES_ONLY
        browseButtonText = RootLocalizer.formatText("fileChooser.browse")
        editorStyles.add("file-chooser")
        this@FileChooserPageBase.extensionFilters = this.extensionFilters
      }
      if (hasOverwriteOption) {
        checkbox(fxOverwrite)
      }
    }
    root.top = sheet.node

    resetCenterPane = {
      root.center = vbox {
        if (allowMultipleChoice) {
          val listView = ListView(chosenFiles).also {
            it.styleClass.addAll("chosen-files-list", "swing-background")
            it.setCellFactory { _ -> ListCellImpl() }
            it.prefHeight = 200.0
          }
          add(listView)
        }
        add(secondaryOptionsSwingNode)
      }
    }
    resetCenterPane()
    val errorPane = ErrorPane()
    root.bottom = errorPane.fxNode

    errorMessage.addWatcher { evt ->
      FXThread.runLater {
        errorPane.onError(evt.newValue?.let(::html2md))
      }
    }
    errorPane.onError(errorMessage.value?.let(::html2md))
    root
  }

  private fun showError(msg: String?) {
    errorMessage.value = msg
  }

  private val myOptionsBuilder: OptionsPageBuilder = OptionsPageBuilder().also {
//    it.i18N = object : OptionsPageBuilder.I18N() {
//      protected override fun hasValue(key: String): Boolean {
//        return if (key == getCanonicalOptionLabelKey(overwriteOption) + ".trailing") true else super.hasValue(
//          key
//        )
//      }
//
//      protected override fun getValue(key: String): String? {
//        if (key == getCanonicalOptionLabelKey(overwriteOption) + ".trailing") {
//          return RootLocalizer.formatText("document.overwrite")
//        }
//        return super.getValue(key)
//      }
//    }
  }
  private val mySecondaryOptionsComponent = JPanel(BorderLayout()).also {
    it.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0))
  }
  protected var hasOverwriteOption: Boolean = true
  protected var updateChosenFile: (File) -> File = { it }
  protected var proposeChosenFile: () -> File = { File(defaultFileName) }

  protected val defaultFileName: String
    get() = if (myDocument == null) "document.gan" else myDocument.getFileName()


  override val title: String get() = pageTitle ?: ""

  fun tryChosenFile(file: File?) {
    validateFile(file).onSuccess {
      showError(null)
      preferences.put(PREF_SELECTED_FILE, fxFile.value?.absolutePath)
    }.onFailure {
      showError(it ?: "Something went wrong")
    }
  }

  protected open fun loadPreferences() {
    val oldFile = preferences.get(PREF_SELECTED_FILE, null)
    if (oldFile != null) {
      fxFile.value = updateChosenFile(File(oldFile))
    } else {
      fxFile.value = proposeChosenFile()
    }
  }

  override fun setActive(isActive: Boolean) {
    val optionGroups = this.optionGroups
    if (!isActive) {
      for (optionGroup in optionGroups) {
        optionGroup.commit()
      }
      fxFile.value?.let {
        preferences.put(PREF_SELECTED_FILE, it.absolutePath)
      }
    } else {
      for (optionGroup in optionGroups) {
        optionGroup.lock()
      }
      SwingUtilities.invokeLater {
        mySecondaryOptionsComponent.removeAll()
        mySecondaryOptionsComponent.add(createSecondaryOptionsPanel(), BorderLayout.NORTH)
      }

      secondaryOptionsSwingNode.content = mySecondaryOptionsComponent
      fileFilter = createFileFilter()
      loadPreferences()
      resetCenterPane()
    }
  }

  protected open fun createSecondaryOptionsPanel(): Component {
    return myOptionsBuilder.buildPlanePage(this.optionGroups.toTypedArray())
  }

  protected abstract fun createFileFilter(): FileExtensionFilter?

  protected abstract val optionGroups: List<GPOptionGroup>

  protected open fun validateFile(file: File?): Result<File?, String?> {
    return basicValidateFile(file)
  }
}

class ChosenFile(val file: File, val chosenFiles: ObservableList<ChosenFile>, val validator: (File) -> Result<File?, String?>) {
  val isValid: Boolean get() = validator(file).isOk

  fun remove() {
    chosenFiles.remove(this)
  }

}

class ListCellImpl: GPListCell<ChosenFile>() {
  override fun updateItem(item: ChosenFile?, empty: Boolean) {
    whenNotEmpty(item, empty) { chosenFile ->
      graphic = hbox {
        styleClasses.add("chosen-file-item")
        if (!chosenFile.isValid) {
          styleClasses.add("validation-error")
        }
        isSelected = this@ListCellImpl.isSelected
        label = chosenFile.file.name.asObservable()
        actions.add(GPAction.create("impex.fileChooserPage.action.remove") {
          FXThread.runLater(chosenFile::remove)
        }.also {
          it.putValue(GPAction.TEXT_DISPLAY, ContentDisplay.GRAPHIC_ONLY)
        })
      }
    }
  }
}
class ChosenFileListCell: ListCell<ChosenFile>() {
  override fun updateItem(item: ChosenFile?, empty: Boolean) {
    super.updateItem(item, empty)
    if (empty || item == null) {
      text = null
      graphic = null
    } else {
      graphic = HBox().also { hbox ->
        hbox.styleClass.add("chosen-file-item")
        if (this.isSelected) {
          hbox.styleClass.add("selected")
        } else {
          hbox.styleClass.remove("selected")
        }
        val label = Label(item.file.name).also { label ->
          HBox.setHgrow(label, Priority.ALWAYS)
          label.maxWidth = Double.MAX_VALUE
        }
        val removeButton = buildFontAwesomeButton("trash", null, {
          item.remove()
        })
        if (!item.isValid) {
          hbox.styleClass.add("validation-error")
        }
        hbox.children.addAll(label, removeButton)
      }
    }
  }
}

const val PREF_SELECTED_FILE: String = "selected_file"
