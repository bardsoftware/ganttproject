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
package net.sourceforge.ganttproject.export

import biz.ganttproject.FXUtil
import biz.ganttproject.app.*
import biz.ganttproject.lib.fx.vbox
import com.github.michaelbull.result.*
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import kotlinx.coroutines.*
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.gui.ViewLogDialog
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status

class JobMonitorDialogFx<T>(private val title: String, private val jobCount: Int): JobMonitor<T> {
  private var onCancel: () -> Unit = {}
  private val progressValue = SimpleDoubleProperty(0.0)
  private val jobName = SimpleStringProperty("")
  private var dlg: DialogController? = null
  private val progressBar: ProgressBar = ProgressBar(0.0).also {
    it.progressProperty().bind(progressValue)
    it.prefWidth = 400.0
  }
  private val errorLabel = Label("").also { it.styleClass.add("error-label") }
  private val btnText = SimpleStringProperty(RootLocalizer.formatText("cancel"))
  private val isViewLogVisible = SimpleBooleanProperty(false)

  private fun createDialog() {
    dialog(id = "jobMonitor", title = title) { dlg ->
      this.dlg = dlg
      dlg.frameStyle = FrameStyle.NO_FRAME
      dlg.addStyleSheet("/biz/ganttproject/app/Dialog.css")
      dlg.addStyleSheet("/biz/ganttproject/impex/Exporter.css")
      dlg.addStyleClass("dlg", "dlg-export-progress")
      val content = vbox {
        addTitle(title)
        add(Label().also { it.textProperty().bind(jobName) })
        add(StackPane(this@JobMonitorDialogFx.progressBar), alignment = null, growth = Priority.ALWAYS)
        add(errorLabel, alignment = Pos.CENTER, growth = Priority.NEVER)
        vbox.prefWidth = 400.0
        vbox.prefHeight = 200.0
      }
      dlg.setContent(content)
      dlg.setupButton(ButtonType.CANCEL) { btn ->
        btn.textProperty().bind(btnText)
        btn.styleClass.addAll("btn", "btn-attention")
        btn.onAction = EventHandler {
          this.onCancel()
        }
      }
      dlg.setupButton(ButtonType("View log")) { btn ->
        btn.disableProperty().bind(isViewLogVisible.not())
        btn.text = RootLocalizer.formatText("viewLog")
        btn.styleClass.addAll("btn", "btn-regular", "secondary")
        btn.onAction = EventHandler {
          dlg.hide()
          ViewLogDialog.show()
        }
      }
    }
  }

  override fun startProcess() {
    if (this.dlg == null) {
      FXUtil.runLater(this::createDialog)
    }
  }

  override fun setJobStarted(jobNumber: Int, jobName: String) {
    FXUtil.runLater {
      this.jobName.set(jobName)
    }
  }
  override fun setProcessCompleted(processResult: Result<T, Exception>) {
    FXUtil.runLater {
      processResult.fold(
        success = {
          this.dlg?.hide()
        },
        failure = {
          LOG.error("Failed to export", exception = it)
          this.progressValue.set(1.0)
          this.progressBar.styleClass?.add("progress-error")
          this.jobName.set("")
          this.errorLabel.text = RootLocalizer.formatText("exportWizard.failure")
          this.btnText.set(RootLocalizer.formatText("close"))
          this.isViewLogVisible.set(true)
          this.dlg?.resize()
        })
    }
  }
}

fun export(coroutineScope: CoroutineScope, jobs: List<ExporterJob>, jobMonitor: JobMonitorModel) {
    var result: Result<IStatus, Exception> = Ok(Status.OK_STATUS)
    jobMonitor.processState.set(JobState.ProcessStarted)
    coroutineScope.launch {
      delay(500)
      jobMonitor.jobCount.set(jobs.size)
      for (job in jobs) {
        jobMonitor.processState.set(JobState.JobStarted(job.name))
        delay(500)
        result = result.andThen {
          try {
            val jobStatus = job.run()
            if (jobStatus.isOK) {
              Ok(jobStatus)
            } else {
              Err(RuntimeException(jobStatus.message))
            }
          } catch (ex: Exception) {
            Err(ex)
          }
        }
        if (result.isErr) {
          break
        }
      }
      if (result.isErr) {
        jobMonitor.processState.set(JobState.ProcessFailed(result))
      } else {
        jobMonitor.processState.set(JobState.ProcessCompleted)
      }
    }
}

private val LOG = GPLogger.create("Export")