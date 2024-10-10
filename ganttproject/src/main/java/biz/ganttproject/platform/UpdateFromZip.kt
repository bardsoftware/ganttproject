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
import javafx.event.ActionEvent
import javafx.scene.control.Button
import javafx.scene.control.TitledPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.text.Text
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

  private val btnApplyZip = Button(localizer.formatText("apply")).also {
    it.styleClass.addAll("btn", "btn-regular")
    it.addEventFilter(ActionEvent.ACTION) {
      installUpdate()
    }
  }

  val propertySheet = PropertySheetBuilder(localizer).pane {
    text(signatureOption) {
      isMultiline = true
    }
    file(fileOption) {
      extensionFilters.add(FileExtensionFilter(localizer.formatText("filterzip"), listOf("*.zip")))
    }
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
      btnApplyZip.isDisable = disable
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

  private fun installUpdate() {
    fileOption.value?.let {
      Eclipsito.getUpdater().installUpdate(it).thenAccept {
        println("Installed into $it")
      }.exceptionally {
        null
      }
    }

  }

  fun buildNode() = TitledPane().apply {
    text = this@UpdateFromZip.paneTitle
    setupValidation()
    this@UpdateFromZip.btnApplyZip.isDisable = true

    content = vbox {
      add(this@UpdateFromZip.propertySheet.node)
      add(HBox().also {
        it.styleClass.add("medskip")
      })
      add(HBox().also {
        it.children.add(this@UpdateFromZip.errorPane)
        it.children.add(this@UpdateFromZip.btnApplyZip)
        HBox.setHgrow(this@UpdateFromZip.errorPane, Priority.ALWAYS)
      })
    }
  }
}