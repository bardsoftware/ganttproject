/*
 * Copyright (c) 2003-2026 Dmitry Barashev, BarD Software s.r.o.
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

import biz.ganttproject.app.*
import biz.ganttproject.core.option.BooleanOption
import biz.ganttproject.core.option.ChangeValueEvent
import biz.ganttproject.core.option.ChangeValueListener
import biz.ganttproject.core.option.ObservableBooleanOption
import biz.ganttproject.lib.fx.openFile
import kotlinx.coroutines.cancel
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.ViewLogDialog
import net.sourceforge.ganttproject.plugins.PluginManager
import org.osgi.service.prefs.Preferences
import java.io.File

/**
 * The model of the export wizard.
 */
class ExportWizardModel(id: String, title: String, private val ftpOptions: DocumentManager.FTPOptions) : WizardModel(id, title) {
  val publishInWebOption: BooleanOption = ObservableBooleanOption("exporter.publishInWeb")

  var exporter: Exporter? = null
    set(exporter) {
      field = exporter
      ourLastSelectedExporter = exporter
    }

  var file: File? = null
    set(value) {
      field = value
      needsRefresh.set(true, this)
    }

  init {
    canFinish = {
      exporter != null && file != null && errorMessage.value.isNullOrBlank()
    }
    onOk = { monitor ->
      exportAndFinalize(monitor)
    }
  }

  private fun exportAndFinalize(monitor: JobMonitorModel) {
    val btnCancel = ProgressButtonState("Cancel", styleClass = "btn-cancel") {
      coroutineScope.cancel()
      monitor.statusText.set("Cancelled")
      monitor.processState.set(JobState.Idle)
    }
    val btnOpenFile = ProgressButtonState("Open File", styleClass = "btn-regular") {
      this.file?.let {
        openFile(it)
      }
    }
    val btnViewLog = ProgressButtonState("View Log", styleClass = "btn-regular") {
      ViewLogDialog.show()
    }
    exporter?.let { selectedExporter ->
      try {
        monitor.statusText.set("Exporting...")
        monitor.processState.addWatcher { event ->
          when (val j = event.newValue) {
            is JobState.ProcessStarted -> {
              monitor.progressButtonState.set(btnCancel)
            }
            is JobState.JobStarted -> {
              monitor.statusText.set(j.title)
            }
            is JobState.ProcessCompleted -> {
              monitor.progressButtonState.set(btnOpenFile)
              monitor.statusText.set("Written to ${file?.absolutePath}")
            }
            is JobState.ProcessFailed -> {
              monitor.progressButtonState.set(btnViewLog)
            }
            else -> {}
          }
        }
        val finalizationJob = ExportFinalizationJobImpl()
        selectedExporter.run(coroutineScope, this.file!!, finalizationJob, monitor)
      } catch (e: Exception) {
        GPLogger.log(e)
      }
    }
  }

  private inner class ExportFinalizationJobImpl : ExportFinalizationJob {
    override fun run(exportedFiles: Array<File?>) {
      if (publishInWebOption.isChecked() && exportedFiles.isNotEmpty()) {
        val publisher = WebPublisher()
        publisher.run(exportedFiles, ftpOptions)
      }
    }
  }
}

class ExportFileWizardImpl(
  uiFacade: UIFacade,
  project: IGanttProject,
  pluginPreferences: Preferences,
  exporters: MutableList<Exporter> = findExporters()
) {
  private val myProject = project
  private val wizardModel = ExportWizardModel(
  "wizard.export", RootLocalizer.formatText("exportWizard.dialog.title"),
  project.documentManager.getFTPOptions()
  )

  init {
    val exportNode = pluginPreferences.node("/instance/net.sourceforge.ganttproject/export")
    wizardModel.publishInWebOption.setValue(exportNode.getBoolean("publishInWeb", false))
    wizardModel.publishInWebOption.addChangeValueListener(object : ChangeValueListener {
      override fun changeValue(event: ChangeValueEvent?) {
        exportNode.putBoolean("publishInWeb", wizardModel.publishInWebOption.getValue())
      }
    })
    wizardModel.exporter = ourLastSelectedExporter ?: exporters.firstOrNull()
    for (e in exporters) {
      e.setContext(project, uiFacade, pluginPreferences)
    }

    val fileChooserPage = ExportFileChooserPage(wizardModel, myProject, exportNode)
    wizardModel.addPage(ExporterChooserPageFx(exporters, wizardModel))
    wizardModel.addPage(fileChooserPage)
  }

  fun show() {
    showWizard(wizardModel)
  }

}

private fun findExporters(): MutableList<Exporter> {
  return PluginManager.getExporters()
}

// The last exporter that was selected by the user. Used to recover the first page state when the wizard is reopened.
var ourLastSelectedExporter: Exporter? = null