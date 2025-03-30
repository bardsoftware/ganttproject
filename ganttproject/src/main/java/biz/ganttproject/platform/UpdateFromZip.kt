/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
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
package biz.ganttproject.platform

import biz.ganttproject.app.FileExtensionFilter
import biz.ganttproject.app.Localizer
import biz.ganttproject.app.PropertySheetBuilder
import biz.ganttproject.core.option.ObservableFile
import biz.ganttproject.core.option.ObservableString
import biz.ganttproject.core.option.ValidationException
import biz.ganttproject.lib.fx.vbox
import biz.ganttproject.platform.PgpUtil.verifyFile
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.text.Text
import java.io.File
import org.eclipse.core.runtime.Platform as Eclipsito

/**
 * Encapsulates UI and logic of installing a minor update from a ZIP file.
 * This requires downloading a ZIP file and a PGP signature that is available from the download site.
 * The file must match the signature.
 * This class provides a text area and a file path input, and provides validation that checks the signature.
 */
class UpdateFromZip(localizer: Localizer) {
  private val paneTitle = localizer.formatText("zip.title")
  private val fileOption = ObservableFile("zip.file")
  private val signatureOption = ObservableString("zip.signature", validator = {sigValue ->
    fileOption.value?.let {file ->
      try {
        verifyFile(file, sigValue.byteInputStream())
        sigValue
      } catch (ex: Exception) {
        throw ValidationException(localizer.formatText("validation.signature.mismatch"), ex)
      }
    } ?: throw ValidationException(localizer.formatText("validation.file.empty"))
  })

  val propertySheet = PropertySheetBuilder(localizer).pane {
    file(fileOption) {
      extensionFilters.add(FileExtensionFilter(localizer.formatText("filterzip"), listOf("*.zip")))
    }
    text(signatureOption) {
      isMultiline = true
    }
  }

  val title = Label(paneTitle).apply {
    styleClass.add("title")
  }
  val subtitle = Label("You can download and install update as ZIP file").apply {
    styleClass.setAll("subtitle")
  }

  private val errorLabel = Text().also {
    it.styleClass.addAll("hint", "hint-validation")
    it.wrappingWidth = 400.0
  }
  private val errorPane = HBox().also {
    it.styleClass.addAll("hint-validation-pane", "noerror")
    it.children.add(errorLabel)
  }

  private fun onError(it: String?) {
    if (it == null) {
      errorPane.isVisible = false
      if (!errorPane.styleClass.contains("noerror")) {
        errorPane.styleClass.add("noerror")
      }
      errorLabel.text = ""
    }
    else {
      errorLabel.text = it
      errorPane.isVisible = true
      errorPane.styleClass.remove("noerror")
    }
  }

  private fun setupValidation() {
    val updateDisable = {
      val disable = signatureOption.value.isNullOrEmpty() || false == fileOption.value?.exists() || propertySheet.validationErrors.isNotEmpty()
      disableApply.value = disable
      if (propertySheet.validationErrors.isEmpty()) {
        onError(null)
      } else {
        onError(propertySheet.validationErrors.values.joinToString(separator = "\n"))
      }
    }
    signatureOption.addWatcher { updateDisable() }
    fileOption.addWatcher { updateDisable() }
    propertySheet.validationErrors.subscribe { updateDisable() }
  }

  internal fun installUpdate(): Result<File?, Throwable> {
    return fileOption.value?.let {
      runCatching {
        Eclipsito.getUpdater().installUpdate(it).join()
      }
    } ?: Err(IllegalStateException("File is missing"))
  }

  val disableApply = SimpleBooleanProperty(false)

  val node by lazy {
    vbox {
      addClasses("update-from-zip")
      addStylesheets("/biz/ganttproject/platform/Update.css")
      add(title)
      add(subtitle)
      add(this@UpdateFromZip.propertySheet.node)
      setupValidation()
    }
  }
}
