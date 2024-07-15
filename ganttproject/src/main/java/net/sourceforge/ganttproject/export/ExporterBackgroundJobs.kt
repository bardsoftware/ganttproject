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
import net.sourceforge.ganttproject.export.ExporterBase.ExporterJob
import net.sourceforge.ganttproject.gui.ViewLogDialog
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import java.util.concurrent.Executors

interface JobMonitor<T> {
  fun setJobStarted(jobNumber: Int, jobName: String)
  fun setJobCompleted(jobNumber: Int, jobResult: Result<T, Exception>)
  fun setOnCancel(cancelHandler: () -> Unit)
}

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

  override fun setJobStarted(jobNumber: Int, jobName: String) {
    if (this.dlg == null) {
      FXUtil.runLater(this::createDialog)
    }
    FXUtil.runLater {
      this.jobName.set(jobName)
    }
  }

  override fun setOnCancel(cancelHandler: () -> Unit) {
    this.onCancel = cancelHandler
  }

  override fun setJobCompleted(jobNumber: Int, jobResult: Result<T, Exception>) {
    val jobNumber1 = jobNumber + 1
    FXUtil.runLater {
      jobResult.fold(
        success = {
          if (jobNumber1 == jobCount) {
            this.dlg?.hide()
          } else {
            val progress = jobNumber1.toDouble() / jobCount.toDouble()
            this.progressValue.set(progress)
            this.jobName.set("")
          }
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
fun export(jobs: List<ExporterJob>, jobMonitor: JobMonitor<IStatus>) {
  val job = exporterScope.launch {
    var result: Result<IStatus, Exception> = Ok(Status.OK_STATUS)
    jobs.forEachIndexed { index, job ->
      result = result.andThen {
        try {
          jobMonitor.setJobStarted(index, job.name)
          val status = job.run()
          if (status.isOK) {
            Ok(status)
          } else {
            Err(RuntimeException(status.message))
          }
        } catch (ex: Exception) {
          Err(ex)
        }.also {
          jobMonitor.setJobCompleted(index, it)
        }
      }
    }
  }
  jobMonitor.setOnCancel { job.cancel("Cancelled by user") }
}

internal val exporterScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
private val LOG = GPLogger.create("Export")