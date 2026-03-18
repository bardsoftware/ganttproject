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
package net.sourceforge.ganttproject.importer

import biz.ganttproject.app.*
import biz.ganttproject.core.option.FileExtensionFilter
import biz.ganttproject.core.option.GPOptionGroup
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Node
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.gui.FileChooserPageBase
import net.sourceforge.ganttproject.gui.UIFacade
import biz.ganttproject.app.WizardModel
import biz.ganttproject.app.WizardPage
import biz.ganttproject.app.showWizard
import javafx.collections.ListChangeListener
import net.sourceforge.ganttproject.plugins.PluginManager.getExtensions
import org.osgi.service.prefs.Preferences
import java.awt.Component
import java.io.File

/**
 * Wizard for importing files into a Gantt project.
 *
 * An import wizard consists of 2-3 pages.
 * On the first page, the user chooses an importer.
 * On the second page, the user chooses a file to import.
 * On the third page, some importers may show a preview of the imported data.
 */
class ImportFileWizard(uiFacade: UIFacade, project: IGanttProject, pluginPreferences: Preferences,
                       importers: List<Importer> = getImporters()) {
  private val wizardModel = ImporterWizardModel()
  init {
    importers.forEach {
      it.setContext(project, uiFacade, pluginPreferences)
      it.setModel(wizardModel)
    }
    val filePage = ImportFileChooserPage(wizardModel, project, pluginPreferences)
    wizardModel.importer = importers.firstOrNull()
    wizardModel.addPage(ImporterChooserPageFx(importers, wizardModel))
    wizardModel.addPage(filePage)
    wizardModel.customPageProperty.addListener { _, oldValue, newValue ->
      if (oldValue == null && newValue != null) {
        wizardModel.addPage(newValue)
      } else if (oldValue != null && newValue == null) {
        wizardModel.removePage(oldValue)
      }
    }
  }

  fun show() {
    showWizard(wizardModel)
  }
}

// --------------------------------------------------------------------------------------------------------------------

/**
 * Model for the import wizard, managing importer and file selection.
 */
class ImporterWizardModel: WizardModel("wizard.import", i18n.formatText("importWizard.dialog.title")) {
  // Selected importer. Updates a customPageProperty with the custom page of the importer, if any.
  var importer: Importer? = null
    set(value) {
      field = value
      customPageProperty.set(null)
      value?.customPage?.let { customPageProperty.set(it) }
      needsRefresh.set(true, this)
    }

  // Selected file.
  var file: File? = null
    set(value) {
      files = listOfNotNull(value)
    }

  var files: List<File> = emptyList()
    set(value) {
      field = value
      needsRefresh.set(true, this)
    }

  // Some importers, e.g. ICS importer, provide a custom page that is appended to the wizard.
  val customPageProperty = SimpleObjectProperty<WizardPage?>(null)

  init {
    canFinish = {
      importer != null && files.isNotEmpty() && errorMessage.value.isNullOrBlank()
    }
    hasNext = { when (currentPage) {
      0 -> importer != null
      1 -> customPageProperty.get() != null && files.isNotEmpty() && errorMessage.value.isNullOrBlank()
      else -> false
    } }
    onOk = { importer?.run() }
  }
}

private fun getImporters(): MutableList<Importer> {
  return getExtensions(Importer.EXTENSION_POINT_ID, Importer::class.java)
}

// --------------------------------------------------------------------------------------------------------------------

/**
 * The first page in the import wizard that allows the user to choose an importer.
 */
private class ImporterChooserPageFx(importers: List<Importer>, model: ImporterWizardModel) : WizardPage {
  override val title: String = RootLocalizer.formatText("importerChooserPageTitle")

  private val titles = importers.flatMapIndexed { index, importer -> listOf(
    "title.$index" to { LocalizedString(importer.fileTypeDescription, DummyLocalizer)},
    "title.$index.help" to { LocalizedString("", DummyLocalizer)}
  )}.toMap()

  override val fxComponent: Node? by lazy {
    val optionPaneBuilder = OptionPaneBuilder<Importer>().apply {
      this.i18n = MappingLocalizer(titles, DummyLocalizer::create)
      this.styleClass = "exporter-chooser-page"
      elements = importers.mapIndexed { index, importer ->
        OptionElementData("title.${index}", importer, isSelected = (index == 0),
          customContent = null)
      }
      onSelect = { model.importer = it }
    }
    optionPaneBuilder.buildPane()
  }

  override val component: Component? = null

  override fun setActive(b: Boolean) {}
}

// --------------------------------------------------------------------------------------------------------------------

/**
 * Wizard page for choosing a file to import from.
 */
private class ImportFileChooserPage(
  private val model: ImporterWizardModel, project: IGanttProject, private val prefs: Preferences)
  : FileChooserPageBase(project.document,
  fileChooserTitle = i18n.formatText("importerFileChooserPageTitle"),
  pageTitle = i18n.formatText("importerFileChooserPageTitle"),
  errorMessage = model.errorMessage) {

  override val optionGroups: List<GPOptionGroup> = emptyList()
  override val preferences: Preferences get() = prefs.node(model.importer?.id ?: "")

  val importer get() = model.importer

  init {
    hasOverwriteOption = false
    chosenFiles.addListener(ListChangeListener {
      model.files = chosenFiles.mapNotNull { if (it.isValid) it.file else null }
    })
  }

  override fun setActive(isActive: Boolean) {
    super.setActive(isActive)
    if (isActive) {
      allowMultipleChoice = importer is ImporterFromGanttFile
    }
  }

  override fun createFileFilter(): FileExtensionFilter? =
    importer?.let {
      FileExtensionFilter(it.getFileTypeDescription(), it.getFileNamePattern().split("|").map { "*.$it" })
    }

  override fun validateFile(file: File?): Result<File?, String?> {
    return super.validateFile(file).andThen { file ->
      if (file?.isDirectory == true) {
        Err(i18n.formatText("document.storage.error.write.isDirectory"))
      } else {
        Ok(file)
      }
    }
  }
}

private val i18n = RootLocalizer
