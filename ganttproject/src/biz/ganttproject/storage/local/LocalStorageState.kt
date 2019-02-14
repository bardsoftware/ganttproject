/*
Copyright 2019 BarD Software s.r.o

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
package biz.ganttproject.storage.local

import biz.ganttproject.storage.DocumentUri
import biz.ganttproject.storage.StorageMode
import biz.ganttproject.storage.createPath
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
import org.controlsfx.validation.ValidationResult
import org.controlsfx.validation.ValidationSupport
import java.io.File
import java.nio.file.Paths

class LocalStorageState(val currentDocument: Document,
                        val mode: StorageMode) {
  private val currentFilePath = createPath(Paths.get(currentDocument.filePath ?: "/").toFile())

  var confirmationReceived: SimpleBooleanProperty = SimpleBooleanProperty(false)

  val currentDir: SimpleObjectProperty<File> = SimpleObjectProperty(
      DocumentUri.toFile(absolutePrefix(currentFilePath, currentFilePath.getNameCount() - 1))
  )

  val currentFile: SimpleObjectProperty<File> = SimpleObjectProperty(DocumentUri.toFile(absolutePrefix(currentFilePath)))

  val confirmationRequired: SimpleBooleanProperty = SimpleBooleanProperty(false)

  val submitOk: SimpleBooleanProperty = SimpleBooleanProperty(false)

  var validationSupport: ValidationSupport? = null
    set(value) {
      if (value != null) {
        validationResult = value.validationResultProperty()
        value.invalidProperty().addListener({ _, _, _ ->
          validate()
        })
        confirmationReceived.addListener({ _, _, _ -> validate() })
      }
    }

  var validationResult: ReadOnlyObjectProperty<ValidationResult>? = null

  private fun validate() {
    val result = validationResult?.get()
    val needsConfirmation = confirmationRequired.get() && !confirmationReceived.get()
    if (result == null) {
      submitOk.set(!needsConfirmation)
      return
    }
    submitOk.set((result.errors.size + result.warnings.size == 0) && !needsConfirmation)
  }

  fun resolveFile(typedString: String): File {
    val typedPath = Paths.get(typedString)
    return if (typedPath.isAbsolute) {
      typedPath.toFile()
    } else {
      File(this.currentDir.get(), typedString)
    }
  }

  fun trySetFile(typedString: String) {
    val resolvedFile = resolveFile(typedString)
    mode.tryFile(resolvedFile)
  }

  fun setCurrentFile(file: File?) {
    if (mode is StorageMode.Save
        && file != null
        && file.exists()
        && currentDocument.uri != FileDocument(file).uri
        && (file != currentFile.get() || !confirmationReceived.get())) {
      confirmationReceived.set(false)
      confirmationRequired.set(true)
    } else {
      confirmationRequired.set(false)
    }
    println("setCurrentFile 1=" + file)
    this.currentFile.set(file)
    println("setCurrentFile 2=" + this.currentFile.get())
    validationResult = null
    validate()
  }

}
