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

data class ProgressButtonState(val text: String, val styleClass: String = "", val action: ()->Unit)

sealed class JobState {
  object Idle : JobState()
  object ProcessStarted : JobState()
  object ProcessCompleted : JobState()
  data class JobStarted(val title: String) : JobState()
  data class ProcessFailed(val result: Result<IStatus, Exception>) : JobState()
}
class JobMonitorModel {
  val processState = ObservableObject<JobState>("", JobState.Idle)
  val progressButtonState = ObservableObject("", ProgressButtonState("", styleClass = "",{}))
  val statusText = ObservableString("", "")
  val errorText = ObservableString("", "")
  val jobCount = ObservableInt("", 0)
}


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

//  override fun startProcess() {
//    coroutineScope.launch {
//      model.processState.set(JobState.ProcessStarted)
//      setComponent(createComponent())
//    }
//  }
//
//  override fun setJobStarted(jobNumber: Int, jobName: String) {
//    coroutineScope.launch {
//      model.statusText.set(jobName)
//    }
//  }
//
//  override fun setProcessCompleted(processResult: Result<IStatus, Exception>) {
//    coroutineScope.launch {
//      processResult.fold(
//        success = {
//          model.processState.set(JobState.ProcessCompleted)
//        },
//        failure = {
//          model.errorText.set(it.message ?: i18n.formatText("exportWizard.failure"))
//          model.processState.set(JobState.ProcessFailed(processResult))
//        }
//      )
//    }
//  }

  private fun createComponent(): Parent {
    return vbox {
      addClasses(styleClasses)
      add(spinner.pane, alignment = Pos.CENTER)
      add(
        hbox {
          label = progressLabel
          button(progressActionButton)
          //actions.add(progressAction)
          styleClasses.add("job-status-label")
        }.apply {
          maxWidth = 400.0
        }, alignment = Pos.CENTER, growth = Priority.NEVER)
      add(errorPane.fxNode, alignment = Pos.CENTER, growth = Priority.NEVER)
    }.also {
      it.alignment = Pos.CENTER
    }
  }
}