/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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
package net.sourceforge.ganttproject

import biz.ganttproject.app.Barrier
import biz.ganttproject.app.SimpleBarrier
import biz.ganttproject.app.TwoPhaseBarrierImpl
import biz.ganttproject.app.i18n
import biz.ganttproject.storage.FetchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.document.Document
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Possible states that the process or project opening passes.
 */
sealed class ProjectOpenActivityState(val id: String) {
  override fun toString(): String {
    return this::class.java.simpleName
  }
}
/** This is the initial state when we just have created a state machine. */
class ProjectOpenActivityCreated : ProjectOpenActivityState("created")

/** This is the state when project opening process started. */
class ProjectOpenActivityStarted(val document: Document) : ProjectOpenActivityState("started")

/**
 * At this state we have loaded the project document and are ready to proceed with loading the main model.
 */
class ProjectOpenActivityDocumentReady(val document: Document): ProjectOpenActivityState(ID) {
  companion object {
    val ID = "documentReady"
  }
}

class ProjectOpenActivityDocumentForked(val document: Document, val fetchResult: FetchResult, val forkCase: ForkCase): ProjectOpenActivityState(ID) {
  enum class ForkCase { FORK, OFFLINE_AHEAD }
  companion object {
    val ID = "documentForked"
  }
}

class ProjectOpenActivityAuthRequired(val document: Document): ProjectOpenActivityState(ID) {
  companion object {
    val ID = "authRequired"
  }
}

/**
 * At this state we have loaded the task and resource models from the document and have run all possible
 * checks that run on project opening, such as initial re-scheduling.
 */
class ProjectOpenActivityMainModelReady(val document: Document) : ProjectOpenActivityState(ID) {
  companion object {
    val ID = "mainModelReady"
  }
}

/** This is the state when task and resource tables are filled with the project data */
class ProjectOpenActivityTablesReady(val project: IGanttProject, val document: Document) : ProjectOpenActivityState("tablesReady")

/** This is the state when calculated properties and filters are applied to the project data */
class ProjectOpenActivityCalculatedModelReady(val project: IGanttProject, val document: Document) : ProjectOpenActivityState("calculatedModelReady")

/** The state when the whole process is completed */
class ProjectOpenActivityCompleted(val project: IGanttProject, val document: Document) : ProjectOpenActivityState("completed")

/** The state when the user cancels the project opening process, e.g. in the fork resolution dialog. */
class ProjectOpenActivityCancelled(val project: IGanttProject, val document: Document) : ProjectOpenActivityState("cancelled")

/**
 * This state indicates that the activity has failed.
 */
class ProjectOpenActivityFailed(
  val errorTitle: String,
  val errorDescription: String,
  val throwable: Throwable? = null
): ProjectOpenActivityState("failed")

/**
 * The state machine that manages the states of the project opening process.
 * States are represented as barriers, and code that is triggered on state transitions can
 * await() on the barriers.
 */
class ProjectOpenStateMachine(val project: IGanttProject, val scope: CoroutineScope) {
  // The project opening process started.
  val stateStarted = SimpleBarrier<ProjectOpenActivityStarted>()
  // The project opening process completed successfully. It is okay to close the UI that might be waiting for the result.
  val stateCompleted = SimpleBarrier<ProjectOpenActivityCompleted>()
  // The project opening process was cancelled by the user. It is okay to close the UI that might be waiting for the result.
  val stateCancelled = SimpleBarrier<ProjectOpenActivityCancelled>()
  // We need authentication to proceed with the project opening process.
  val stateAuthRequired = SimpleBarrier<ProjectOpenActivityAuthRequired>()
  // The document has been forked, and we may need to take a decision on how to proceed.
  val stateDocumentForked = SimpleBarrier<ProjectOpenActivityDocumentForked>()
  // The document has been successfully loaded and is ready for further processing.
  val stateDocumentReady = SimpleBarrier<ProjectOpenActivityDocumentReady>()
  val stateMainModelReady = SimpleBarrier<ProjectOpenActivityMainModelReady>()
  val stateTablesReady = TwoPhaseBarrierImpl<ProjectOpenActivityTablesReady>("Tables Initialized").also { barrier ->
    barrier.await {
      state = it
    }
  }
  val stateCalculatedModelReady = SimpleBarrier<ProjectOpenActivityCalculatedModelReady>()
  val stateFailed = SimpleBarrier<ProjectOpenActivityFailed>()

  var state: ProjectOpenActivityState = ProjectOpenActivityCreated()
  set(state) {
    LOG.debug("Transitioning: {} => {}", field, state)
    fun doSetState(condition: Boolean, state: ProjectOpenActivityState, code: () -> Unit) {
      assert(condition) { "Invalid transition: $field => $state" }
      field = state
      try {
        code()
      } catch (ex: Exception) {
        stateFailed.resolve(ProjectOpenActivityFailed(
          errorTitle = i18n.formatText("error.title"),
          errorDescription = i18n.formatText("error.state.${state.id}", ex.message ?: ""),
          throwable = ex
        ))
      }
    }
    when (state) {
      is ProjectOpenActivityStarted -> {
        doSetState(field is ProjectOpenActivityCreated || field is ProjectOpenActivityAuthRequired, state) {
          stateStarted.resolve(state)
        }
      }
      is ProjectOpenActivityFailed -> {
        doSetState(true, state) {
          stateFailed.resolve(state)
        }
      }
      is ProjectOpenActivityAuthRequired -> {
        doSetState(field is ProjectOpenActivityStarted, state) {
          stateAuthRequired.resolve(state)
        }
      }
      is ProjectOpenActivityDocumentForked -> {
        doSetState(field is ProjectOpenActivityStarted, state) {
          stateDocumentForked.resolve(state)
        }
      }
      is ProjectOpenActivityDocumentReady -> {
        doSetState(field is ProjectOpenActivityStarted || field is ProjectOpenActivityDocumentForked, state) {
          stateDocumentReady.resolve(state)
        }
      }
      is ProjectOpenActivityMainModelReady -> {
        doSetState(field is ProjectOpenActivityDocumentReady, state) {
          stateMainModelReady.resolve(state)
          stateTablesReady.activate(ProjectOpenActivityTablesReady(project, state.document))
        }
      }
      is ProjectOpenActivityTablesReady -> {
        doSetState(field is ProjectOpenActivityMainModelReady, state) {}
      }
      is ProjectOpenActivityCalculatedModelReady -> {
        doSetState(field is ProjectOpenActivityTablesReady, state) {
          stateCalculatedModelReady.resolve(state)
        }
      }
      is ProjectOpenActivityCompleted -> {
        doSetState(field is ProjectOpenActivityCalculatedModelReady || field is ProjectOpenActivityDocumentForked, state) {
          stateCompleted.resolve(state)
        }
      }
      is ProjectOpenActivityCancelled -> {
        doSetState(field is ProjectOpenActivityDocumentForked, state) {
          stateCancelled.resolve(state)
        }
      }
      else -> {
        stateFailed.resolve(ProjectOpenActivityFailed("Unexpected state", "Unexpected state: $state"))
      }
    }
  }

  fun <Source : ProjectOpenActivityState, Target : ProjectOpenActivityState> transition(
    fromState: Barrier<Source>,
    toStateId: String,
    code: suspend (Source) -> Target
  ) {
    fromState.await { value ->
      scope.launch {
        state = try {
          code(value)
        } catch (ex: Throwable) {
          ProjectOpenActivityFailed(
                errorTitle = i18n.formatText("error.title"),
                errorDescription = i18n.formatText("error.state.${toStateId}", ex.message ?: ""),
                throwable = ex
          )
        }
      }
    }
  }

  fun transition(targetState: ProjectOpenActivityState, code: ()->Unit) {
    try {
      code()
      state = targetState
    } catch (ex: Throwable) {
      state = ProjectOpenActivityFailed(
        errorTitle = i18n.formatText("error.title"),
        errorDescription = i18n.formatText("error.state.${targetState.id}", ex.message ?: ""),
        throwable = ex
      )
    }
  }

  private fun assert(condition: Boolean, msg: ()->String) {
    if (!condition) error(msg())
  }

  fun fail(ex: Exception) {
    state = ProjectOpenActivityFailed(
      errorTitle = i18n.formatText("error.title"),
      errorDescription = i18n.formatText("error.state.${state.id}", ex.message ?: ""),
      throwable = ex

    )
  }

  fun start(document: Document) {
    state = ProjectOpenActivityStarted(document)
  }
}

typealias ProjectOpenStateMachineBuilder = (ProjectOpenStateMachine) -> Unit

/**
 * This class creates project open activities. A new activity is created when GanttProject opens a project
 * document. Listeners receive an instance of the state machine and can subscribe to the state changes
 * and run the appropriate code when a state machine enters into the state they are waiting for.
 */
object ProjectOpenActivityFactory {
  private val builders = mutableListOf<ProjectOpenStateMachineBuilder>()
  fun addBuilder(l: ProjectOpenStateMachineBuilder) = builders.add(l)

  fun createStateMachine(project: IGanttProject): ProjectOpenStateMachine {
    val coroutineScope = CoroutineScope(EmptyCoroutineContext)
    return ProjectOpenStateMachine(project, coroutineScope).also { sm ->
      builders.forEach { it.invoke(sm) }
    }
  }
}


private val i18n = i18n {
  default()
  prefix("project.open")
}
private val LOG = GPLogger.create("Project.OpenStateMachine")
