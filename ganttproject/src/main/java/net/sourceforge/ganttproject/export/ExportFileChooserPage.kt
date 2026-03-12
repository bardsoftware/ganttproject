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
package net.sourceforge.ganttproject.export

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.option.FileExtensionFilter
import biz.ganttproject.core.option.GPOptionGroup
import biz.ganttproject.storage.asLocalDocument
import biz.ganttproject.storage.getDefaultLocalFolder
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.gui.FileChooserPageBase
import net.sourceforge.ganttproject.gui.UIUtil
import net.sourceforge.ganttproject.util.FileUtil.replaceExtension
import org.osgi.service.prefs.Preferences
import java.awt.Component
import java.io.File
import javax.swing.JFileChooser

/**
 * A wizard page for choosing a file to export to.
 */
internal class ExportFileChooserPage(
  private val myState: ExportWizardModel,
  private val myProject: IGanttProject,
  override val preferences: Preferences
) : FileChooserPageBase(
  myProject.document,
  fileChooserTitle = i18n.formatText("selectFileToExport"),
  fileChooserSelectionMode = JFileChooser.FILES_AND_DIRECTORIES,
  pageTitle = i18n.formatText("selectFileToExport"),
  errorMessage = myState.errorMessage
) {
  private val myWebPublishingGroup: GPOptionGroup = GPOptionGroup(
    "exporter.webPublishing", myState.publishInWebOption
  ).also { it.isTitled = false }

  init {
    updateChosenFile = { file ->
      myState.exporter?.let {
        replaceExtension(file, it.proposeFileExtension())
      } ?: file
    }
    proposeChosenFile = {
      myState.exporter?.let {proposeOutputFile(myProject, it) } ?: File(defaultFileName)
    }
    fxFile.addWatcher {
      myState.file = it.newValue
    }
  }

  override fun validateFile(file: File?): Result<File, String> {
    if (file == null) {
      return Err("File cannot be null")
    }

    fxOverwrite.isWritable.value = false
    if (!file.exists()) {
      val parent = file.getParentFile()
      if (!parent.exists()) {
        return Err(
          i18n.formatText(
            "fileChooser.error.directoryDoesNotExists", UIUtil.formatPathForLabel(parent)
          )
        )
      }
      if (!parent.canWrite()) {
        return Err(
          i18n.formatText(
            "fileChooser.error.directoryIsReadOnly", UIUtil.formatPathForLabel(parent)
          )
        )
      }
    } else if (!file.canWrite()) {
      if (file.isDirectory()) {
        return Err(
          i18n.formatText(
            "fileChooser.error.directoryIsReadOnly", UIUtil.formatPathForLabel(file)
          )
        )
      } else {
        return Err(
          i18n.formatText("fileChooser.error.fileIsReadOnly", UIUtil.formatPathForLabel(file)),
        )
      }
    } else {
      fxOverwrite.isWritable.value = true
      if (!fxOverwrite.value) {
        return Err(i18n.formatText("fileChooser.warning.fileExists"))
      }
    }
    return Ok(file)
  }

  override fun createSecondaryOptionsPanel(): Component {
    return myState.exporter?.getCustomOptionsUI() ?: super.createSecondaryOptionsPanel()
  }

  override fun createFileFilter(): FileExtensionFilter? = myState.exporter?.let {
    FileExtensionFilter(it.getFileTypeDescription(), listOf(it.getFileNamePattern()))
  }

  override val optionGroups: List<GPOptionGroup>
    get() = listOf(myWebPublishingGroup) + (myState.exporter?.secondaryOptions ?: emptyList())

}

fun proposeOutputFile(project: IGanttProject, exporter: Exporter): File? {
  val proposedExtension = exporter.proposeFileExtension() ?: return null

  var result: File? = null
  val projectDocument = project.document
  if (projectDocument != null) {
    val localDocument = projectDocument.asLocalDocument()
    if (localDocument != null) {
      val localFile = localDocument.file
      if (localFile.exists()) {
        result = replaceExtension(localFile, proposedExtension)
      } else {
        val directory = localFile.getParentFile()
        if (directory.exists()) {
          result = File(directory, project.projectName + "." + proposedExtension)
        }
      }
    } else {
      result = File(
        getDefaultLocalFolder(), replaceExtension(projectDocument.getFileName(), proposedExtension)
      )
    }
  }
  if (result == null) {
    val userHome = File(System.getProperty("user.home"))
    result = File(userHome, project.projectName + "." + proposedExtension)
  }
  return result
}

private val i18n = RootLocalizer