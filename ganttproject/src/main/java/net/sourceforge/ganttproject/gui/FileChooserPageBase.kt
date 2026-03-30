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
import javafx.scene.control.ListView
import javafx.scene.layout.BorderPane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
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
  val errorMessage: ObservableString,
  val coroutineScope: CoroutineScope
) : WizardPage {
  val fxFile = ObservableFile("file", null)
  val fxFiles = ObservableFiles("files", emptyList())
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
        "files.label" to "file",
        "overwrite.label" to "option.exporter.overwrite.label.trailing"
      ))
    }
    if (allowMultipleChoice) {
      val updateFileList = { files: List<File> ->
        chosenFiles.clear()
        files.forEach { file ->
          val newChosenFile = ChosenFile(file, fxFiles,chosenFiles, this::validateFile)
          if (!chosenFiles.contains(newChosenFile)) {
            chosenFiles.add(newChosenFile)
          }
        }
      }
      fxFiles.addWatcher { updateFileList(it.newValue) }
      updateFileList(fxFiles.value)

    } else {
      fxFile.addWatcher { event ->
        tryChosenFile(event.newValue)
//        if (allowMultipleChoice) {
//          event.newValue?.let {
//            val newChosenFile = ChosenFile(event.newValue!!, fxFchosenFiles, this::validateFile)
//            if (!chosenFiles.contains(newChosenFile)) {
//              chosenFiles.add(newChosenFile)
//            }
//          }
//        }
      }
    }

    val sheet = PropertySheetBuilder(i18n).pane {
      if (allowMultipleChoice) {
        files(fxFiles) {
          chooserTitle = fileChooserTitle ?: ""
          isSaveNotOpen = fileChooserSelectionMode != JFileChooser.FILES_ONLY
          browseButtonText = RootLocalizer.formatText("fileChooser.browse")
          allowMultipleSelection = true
          editorStyles.add("file-chooser")
          this@FileChooserPageBase.extensionFilters = this.extensionFilters
        }
      } else {
        file(fxFile) {
          chooserTitle = fileChooserTitle ?: ""
          isSaveNotOpen = fileChooserSelectionMode != JFileChooser.FILES_ONLY
          browseButtonText = RootLocalizer.formatText("fileChooser.browse")
          editorStyles.add("file-chooser")
          this@FileChooserPageBase.extensionFilters = this.extensionFilters
        }
      }
      if (hasOverwriteOption) {
        checkbox(fxOverwrite)
      }
    }
    root.top = sheet.node

    resetCenterPane = {
      coroutineScope.launch {
        val addOptionsComponentSwing: Boolean = withContext(Dispatchers.JavaFx) {
          val optionsComponentFx: Node? = createSecondaryOptionsPanelFx()
          root.center = vbox {
            if (allowMultipleChoice) {
              val listView = ListView(chosenFiles).also {
                it.styleClass.addAll("chosen-files-list", "swing-background")
                it.setCellFactory { _ -> ListCellImpl() }
                it.prefHeight = 200.0
              }
              add(listView)
            }
            if (optionsComponentFx == null) {
              add(secondaryOptionsSwingNode)
            } else {
              add(optionsComponentFx)
            }
          }
          return@withContext optionsComponentFx == null
        }
        if (addOptionsComponentSwing) {
          withContext(Dispatchers.Swing) {
            mySecondaryOptionsComponent.removeAll()
            mySecondaryOptionsComponent.add(createSecondaryOptionsPanel(), BorderLayout.NORTH)
            secondaryOptionsSwingNode.content = mySecondaryOptionsComponent
          }
        }
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
      if (allowMultipleChoice) {
        // TODO: restore fxFiles too?
      } else {
        fxFile.value?.let {
          preferences.put(PREF_SELECTED_FILE, it.absolutePath)
        }
      }
    } else {
      for (optionGroup in optionGroups) {
        optionGroup.lock()
      }

      fileFilter = createFileFilter()
      loadPreferences()
      resetCenterPane()
    }
  }

  protected open fun createSecondaryOptionsPanel(): Component? {
    return myOptionsBuilder.buildPlanePage(this.optionGroups.toTypedArray())
  }

  protected open fun createSecondaryOptionsPanelFx(): Node? = null

  protected abstract fun createFileFilter(): FileExtensionFilter?

  protected abstract val optionGroups: List<GPOptionGroup>

  protected open fun validateFile(file: File?): Result<File?, String?> {
    return basicValidateFile(file)
  }
}

class ChosenFile(val file: File, val fileOption: ObservableFiles, val chosenFiles: ObservableList<ChosenFile>, val validator: (File) -> Result<File?, String?>) {
  val isValid: Boolean get() = validator(file).isOk

  fun remove() {
    fileOption.set(fileOption.value.filter { it != file }, chosenFiles)
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

const val PREF_SELECTED_FILE: String = "selected_file"
