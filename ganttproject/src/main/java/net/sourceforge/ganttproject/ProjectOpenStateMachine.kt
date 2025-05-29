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

import biz.ganttproject.app.SimpleBarrier
import biz.ganttproject.app.TwoPhaseBarrierImpl
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Possible states that the process or project opening passes.
 */
sealed class ProjectOpenActivityState {
  override fun toString(): String {
    return this::class.java.simpleName
  }
}
/** This is the initial state when we just have created a state machine. */
class ProjectOpenActivityCreated : ProjectOpenActivityState()

/** This is the state when project opening process started. */
class ProjectOpenActivityStarted : ProjectOpenActivityState()

/**
 * At this state we have loaded the task and resource models from the document and have run all possible
 * checks that run on project opening, such as initial re-scheduling.
 */
class ProjectOpenActivityMainModelReady: ProjectOpenActivityState()

/** This is the state when task and resource tables are filled with the project data */
class ProjectOpenActivityTablesReady(val project: IGanttProject) : ProjectOpenActivityState()

/** This is the state when calculated properties and filters are applied to the project data */
class ProjectOpenActivityCalculatedModelReady(val project: IGanttProject) : ProjectOpenActivityState()

/** The state when the whole process is completed */
class ProjectOpenActivityCompleted: ProjectOpenActivityState()

/**
 * This state indicates that the activity has failed.
 */
class ProjectOpenActivityFailed(
  val errorTitle: String,
  val throwable: Throwable? = null
): ProjectOpenActivityState()

/**
 * The state machine that manages the states.
 * States are represented as barriers, and code that is triggered on state transitions can
 * await() on the barriers.
 */
class ProjectOpenStateMachine(project: IGanttProject, val scope: CoroutineScope) {
  val stateStarted = SimpleBarrier<ProjectOpenActivityStarted>()
  val stateCompleted = SimpleBarrier<ProjectOpenActivityCompleted>()
  val stateMainModelReady = SimpleBarrier<ProjectOpenActivityMainModelReady>()
  val stateTablesReady = TwoPhaseBarrierImpl("Tables Initialized", ProjectOpenActivityTablesReady(project)).also { barrier ->
    barrier.await {
      state = it
    }
  }
  val stateCalculatedModelReady = SimpleBarrier<ProjectOpenActivityCalculatedModelReady>()
  val stateFailed = SimpleBarrier<ProjectOpenActivityFailed>()

  var state: ProjectOpenActivityState? = ProjectOpenActivityCreated()
  set(state) {
    LOG.debug("Transitioning: {} => {}", field, state)
    fun doSetState(condition: Boolean, state: ProjectOpenActivityState, code: () -> Unit) {
      assert(condition) { "Invalid transition: $field => $state" }
      field = state
      try {
        code()
      } catch (ex: Exception) {
        stateFailed.resolve(ProjectOpenActivityFailed(errorTitle = "Error", throwable = ex))
      }
    }
    when (state) {
      is ProjectOpenActivityStarted -> {
        doSetState(field is ProjectOpenActivityCreated, state) {
          stateStarted.resolve(state)
        }
      }
      is ProjectOpenActivityFailed -> {
        doSetState(true, state) {
          stateFailed.resolve(state)
        }
      }
      is ProjectOpenActivityMainModelReady -> {
        doSetState(field is ProjectOpenActivityStarted, state) {
          stateMainModelReady.resolve(state)
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
        doSetState(field is ProjectOpenActivityCalculatedModelReady, state) {
          stateCompleted.resolve(state)
        }
      }
      else -> {
        error("Unexpected state: $state")
      }
    }
  }

  fun transition(targetState: ProjectOpenActivityState, code:()->Unit) {
    try {
      code()
      state = targetState
    } catch (ex: Throwable) {
      state = ProjectOpenActivityFailed(errorTitle = "Error", throwable = ex)
    }
  }

  private fun assert(condition: Boolean, msg: ()->String) {
    if (!condition) error(msg())
  }
}

typealias ProjectOpenActivityListener = (ProjectOpenStateMachine) -> Unit

/**
 * This class creates project open activities. A new activity is created when GanttProject opens a project
 * document. Listeners receive an instance of the state machine and can subscribe to the state changes
 * and run the appropriate code when a state machine enters into the state they are waiting for.
 */
class ProjectOpenActivityFactory {
  private val listeners = mutableListOf<ProjectOpenActivityListener>()
  fun addListener(l: ProjectOpenActivityListener) = listeners.add(l)

  fun createStateMachine(project: IGanttProject): ProjectOpenStateMachine {
    val coroutineScope = CoroutineScope(EmptyCoroutineContext)
    return ProjectOpenStateMachine(project, coroutineScope).also { sm ->
      listeners.forEach { it.invoke(sm) }
    }
  }
}

private val LOG = GPLogger.create("Project.OpenStateMachine")