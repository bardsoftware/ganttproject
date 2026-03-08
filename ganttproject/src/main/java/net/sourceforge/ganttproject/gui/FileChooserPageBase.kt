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

//import biz.ganttproject.platform.getMeaningfulMessage
//import biz.ganttproject.platform.getMeaningfulMessage
import biz.ganttproject.app.PropertySheetBuilder
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.option.*
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import javafx.beans.property.SimpleObjectProperty
import javafx.embed.swing.SwingNode
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder
import net.sourceforge.ganttproject.gui.projectwizard.WizardPage
import org.osgi.service.prefs.Preferences
import java.awt.BorderLayout
import java.awt.Component
import java.io.File
import javax.swing.BorderFactory
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Base class for the file chooser pages in the Import and Export wizards.
 */
abstract class FileChooserPageBase protected constructor(
  private val myDocument: Document?,
  uiFacade: UIFacade?,
  val fileChooserTitle: String?,
  val pageTitle: String?,
  val fileChooserSelectionMode: Int = JFileChooser.FILES_ONLY,
  val errorMessage: ObservableString
) : WizardPage {
  val fxFile = ObservableFile("file", null)
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
  val selectedFileProperty: SimpleObjectProperty<File?> = SimpleObjectProperty<File?>(null)
  private val secondaryOptionsSwingNode = SwingNode()

  init {
    fxFile.addWatcher { event ->
      tryChosenFile(event.newValue)
    }
//    selectedFileProperty.addListener { _, _, newValue ->
//      if (fxFile.value != newValue) {
//        fxFile.value = newValue
//      }
//    }
    fxOverwrite.addWatcher { tryChosenFile(fxFile.value) }
  }

  override val component: Component? = null
  override val fxComponent: Node by lazy {
    val root = BorderPane()
    root.styleClass.add("file-chooser-page")
    val sheet = PropertySheetBuilder(RootLocalizer).pane {
      file(fxFile) {
        editorStyles.add("file-chooser")
        this@FileChooserPageBase.extensionFilters = this.extensionFilters
      }
      if (hasOverwriteOption) {
        checkbox(fxOverwrite)
      }
    }
    root.top = sheet.node

    root.center = secondaryOptionsSwingNode

    fun showError(msg: String?) {
      if (msg != null) {
        //UIUtil.setupErrorLabel(myFileLabel, it.newValue)
        root.bottom = HBox().apply {
          styleClass.add("alert-embedded-box")
          children.add(Label(msg).also { it.styleClass.add("alert-error") })
        }
      } else {
        root.bottom = null
      }
    }
    errorMessage.addWatcher { showError(it.newValue) }
    showError(errorMessage.value)
    // We need to update the secondary options panel when it's created.
    // In FileChooserPageBase, it's updated in setActive(true).
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
  private val myFileLabel = JLabel("")
//  protected val overwriteOption: BooleanOption = DefaultBooleanOption("overwrite")
//
  protected var hasOverwriteOption: Boolean = true
  protected var updateChosenFile: (File) -> File = { it }
  protected var proposeChosenFile: () -> File = { File(defaultFileName) }

  protected val defaultFileName: String
    get() = if (myDocument == null) "document.gan" else myDocument.getFileName()


  override val title: String get() = pageTitle ?: ""

  fun tryChosenFile(file: File?) {
    myFileLabel.setOpaque(true)
    validateFile(file).onSuccess {
      //selectedFileProperty.set(file)
      //UIUtil.clearErrorLabel(myFileLabel)
      showError(null)
      preferences.put(PREF_SELECTED_FILE, fxFile.value?.absolutePath)
    }.onFailure {
      showError(it ?: "Something went wrong")
      //UIUtil.setupErrorLabel(myFileLabel, it)
    }
  }

//  override val component: Component by lazy {
//    val myComponent = JPanel(BorderLayout())
//    chooser.setFileSelectionMode(this.fileChooserSelectionMode)
//    val contentPanel: JComponent = JPanel(BorderLayout())
//    val fileBox = Box.createVerticalBox()
//    chooser.setAlignmentX(Component.LEFT_ALIGNMENT)
//    fileBox.add(this.chooser)
//    myFileLabel.setAlignmentX(Component.LEFT_ALIGNMENT)
//    fileBox.add(myFileLabel)
//    if (hasOverwriteOption) {
//      fileBox.add(
//        myOptionsBuilder.createOptionComponent(
//          GPOptionGroup("exporter", this.overwriteOption), this.overwriteOption
//        )
//      )
//    }
//    contentPanel.add(fileBox, BorderLayout.NORTH)
//    contentPanel.add(mySecondaryOptionsComponent, BorderLayout.CENTER)
//    myComponent.add(contentPanel, BorderLayout.NORTH)
//    myComponent
//  }

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
      mySecondaryOptionsComponent.removeAll()
      mySecondaryOptionsComponent.add(createSecondaryOptionsPanel(), BorderLayout.NORTH)

      secondaryOptionsSwingNode.content = mySecondaryOptionsComponent
      fileFilter = createFileFilter().also {
        println("filter = $it")
      }
      loadPreferences()
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

const val PREF_SELECTED_FILE: String = "selected_file"
