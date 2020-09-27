/*
Copyright 2019-2020 BarD Software s.r.o

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

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.storage.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.FileDocument
import org.controlsfx.validation.ValidationResult
import java.io.File

/**
 * The local storage state object keeps currently selected directory and file, decides if action on file can be invoked
 * (e.g. if we can't save a file when it is read-only), if user confirmation is required (e.g. when we save to some
 * other existing file) and if it was received.
 *
 * @author Dmitry Barashev (dbarashev@bardsoftware.com)
 */
class LocalStorageState(val currentDocument: Document,
                        val mode: StorageMode,
                        private val defaultLocalFolder: File) {
  // This property indicates if the user must confirm the action on the current file. This may be needed if
  // we're saving a document and currentFile exists.
  val confirmationRequired: SimpleBooleanProperty = SimpleBooleanProperty(false)

  // This property indicates if the user has confirmed his decision to take the action on the current file.
  var confirmationReceived: SimpleBooleanProperty = SimpleBooleanProperty(false).also {
    it.addListener { _, _, _ -> validate() }
  }

  // The target directory.
  val currentDir: SimpleObjectProperty<File> = SimpleObjectProperty(currentDocument.asFile().parentFile)

  // The target file which will be read or written when the user decides to invoke the action.
  // It is nullable. Null value means that the user did not select a file nor typed any file name. That is,
  // just a directory is selected.
  // It may also point to not-existing file.
  val currentFile: SimpleObjectProperty<File?> = SimpleObjectProperty(currentDocument.asFile())

  // This property indicates if the action is possible. E.g. if the action is SAVE and currentFile is read-only,
  // this property value will be false.
  val canWrite = SimpleBooleanProperty(false)

  // The result of validation of the action against the currentFile. It provides human-readable error or warning
  // messages which are supposed to be shown in the UI.
  var validation = SimpleObjectProperty(ValidationResult())

  private fun validate() {
    LOG.debug(">>> validate: currentFile={} dir={}", currentFile.get()?.name ?: "<no file>", currentDir.get().name)
    val file = this.currentFile.get()
    if (file == null) {
      canWrite.set(false)
      validation.value = ValidationResult.fromWarning(null, i18n.formatText("validation.emptyFileName"))
      LOG.debug("<<< validate")
      return
    }
    try {
      LOG.debug("trying file with mode={}", mode.name)
      mode.tryFile(file)
      LOG.debug("try is ok. confirmation required={} received={}", confirmationRequired.value, confirmationReceived.value)
      validation.value = ValidationResult()
      canWrite.value = !confirmationRequired.value || confirmationReceived.value
    } catch (ex: StorageMode.FileException) {
      canWrite.set(false)
      LOG.debug("bad luck: error={}", ex.message ?: "")
      validation.value = ValidationResult.fromError(null, RootLocalizer.formatText(ex.message!!, *ex.args))
    }
    LOG.debug("<<< validate")
  }

  internal fun resolveFile(typedString: String): File =
    File(typedString).let {
      if (it.isAbsolute) { it } else File(this.currentDir.get(), typedString)
    }

  @Throws(StorageMode.FileException::class)
  internal fun trySetFile(typedString: String): File =
    resolveFile(typedString).also {
      mode.tryFile(it)
    }

  internal fun setCurrentFile(filename: String?)  {
    if (filename.isNullOrBlank()) {
      this.currentFile.set(null)
      validate()
    } else {
      resolveFile(filename).also { this.setCurrentFile(it) }
    }
  }

  internal fun setCurrentFile(file: File?) {
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
    this.currentFile.set(file)
    validate()
  }

  private fun Document.asFile() =
      this.asLocalDocument()?.file ?: defaultLocalFolder.resolve(this.fileName)
}

private val i18n = RootLocalizer.createWithRootKey("storageService.local", BROWSE_PANE_LOCALIZER)
private val LOG = GPLogger.create("LocalStorage")
