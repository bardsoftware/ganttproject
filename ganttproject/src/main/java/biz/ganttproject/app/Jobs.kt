/*
Copyright 2026 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.app

import biz.ganttproject.ButtonBuilder
import biz.ganttproject.core.option.ObservableEvent
import biz.ganttproject.core.option.ObservableInt
import biz.ganttproject.core.option.ObservableObject
import biz.ganttproject.core.option.ObservableString
import biz.ganttproject.lib.fx.hbox
import biz.ganttproject.lib.fx.vbox
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onFailure
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.layout.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import org.eclipse.core.runtime.IStatus

interface JobMonitor<T> {
  fun startProcess()
  fun setJobStarted(jobNumber: Int, jobName: String)
  fun setProcessCompleted(processResult: Result<T, Exception>)
}

/**
 * JobMonitor UI component is a spinner with a progres pane underneath. In the progress page we show a status
 * label and a progress button that allows for cancelling the process while the process is running or execute some other
 * action when the process is completed.
 *
 * This data class is a progress button state, which includes a label, a style class and an action.
 */
data class ProgressButtonState(val text: String, val styleClass: String = "", val action: ()->Unit)

/**
 * A process that consists of a sequence of jobs may be in one of these states.
 */
sealed class JobState {
  // The process is idle.
  object Idle : JobState()
  // The process has started.
  object ProcessStarted : JobState()
  // The process completes successfully.
  object ProcessCompleted : JobState()
  // A job with the given title has started.
  data class JobStarted(val title: String) : JobState()
  // The process failed.
  data class ProcessFailed(val result: Result<IStatus, Exception>) : JobState()
}

/**
 * Represents a model for monitoring job states and related UI elements.
 *
 * This class provides observable properties to track and update the state of a job process,
 * the progress button state, status and error messages, as well as the count of jobs.
 */
class JobMonitorModel {
  val processState = ObservableObject<JobState>("", JobState.Idle)
  val progressButtonState = ObservableObject("", ProgressButtonState("", styleClass = "",{}))
  val statusText = ObservableString("", "")
  val errorText = ObservableString("", "")
  val jobCount = ObservableInt("", 0)
}

/**
 * Implementation of a job monitoring UI component.
 *
 * This class monitors the state of job processes, updates UI components
 * such as spinners and error panes based on the current state, and manages
 * user interactions such as clicking progress buttons.
 *
 * Behavior:
 * - Reacts to changes in `processState` by updating the spinner state and rendering appropriate components.
 * - Updates a progress label whenever `statusText` changes.
 * - Handles changes in the progress button's state (`progressButtonState`), updating its appearance and action.
 * - Displays error messages through an error pane when `errorText` changes.
 */
class JobMonitorImpl(
  val model: JobMonitorModel,
  private val setComponent: (Parent)->Unit,
  private val i18n: Localizer) {

  var styleClasses = mutableListOf<String>()
  private val spinner = Spinner()
  private val errorPane = ErrorPane()
  private val coroutineScope = CoroutineScope(Dispatchers.JavaFx)
  private val progressActionButton = ButtonBuilder(
    action = {
      model.progressButtonState.value?.action?.invoke()
    }
  )
  private val progressLabel = SimpleStringProperty("")

  init {
    model.processState.addWatcher { event ->
      println("process state changed: ${event.newValue}")
      FXThread.runLater {
        val newState = event.newValue ?: JobState.Idle
        spinner.state = when (newState) {
          is JobState.ProcessStarted -> Spinner.State.WAITING
          is JobState.ProcessCompleted -> Spinner.State.INITIAL
          is JobState.ProcessFailed -> Spinner.State.ATTENTION
          is JobState.Idle -> Spinner.State.INITIAL
          else -> spinner.state
        }
        if (newState is JobState.ProcessStarted) {
          setComponent(createComponent())
        }
        if (newState is JobState.ProcessFailed) {
          newState.result.onFailure { error -> errorPane.onError(error.message ?: "") }
        }
      }
    }
    model.statusText.addWatcher { event ->
       FXThread.runLater {
        progressLabel.set(event.newValue)
      }
    }
    progressLabel.set(model.statusText.value)
    model.progressButtonState.addWatcher { event ->
      println("progress button state changed: ${event.newValue}")
      FXThread.runLater {
        event.newValue?.let {
          progressActionButton.text.observable.set(it.text)
          progressActionButton.styleClass.remove(event.oldValue?.styleClass)
          progressActionButton.styleClass.add(it.styleClass)
        }
      }
    }
    model.errorText.addWatcher { event: ObservableEvent<String?> ->
      val errorMsg = event.newValue
      if (!errorMsg.isNullOrEmpty()) {
        errorPane.onError(errorMsg)
      }
    }
  }

  private fun createComponent(): Parent {
    return vbox {
      addClasses(styleClasses)
      add(spinner.pane, alignment = Pos.CENTER)
      add(
        hbox {
          label = progressLabel
          button(progressActionButton)
          styleClasses.add("job-status-label")
        }, alignment = Pos.CENTER, growth = Priority.NEVER)
      add(errorPane.fxNode, alignment = Pos.CENTER, growth = Priority.NEVER)
    }.also {
      it.alignment = Pos.CENTER
    }
  }
}