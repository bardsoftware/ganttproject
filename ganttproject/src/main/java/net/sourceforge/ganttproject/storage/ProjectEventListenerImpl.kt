/*
Copyright 2022 BarD Software s.r.o., Anastasiia Postnikova

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
package net.sourceforge.ganttproject.storage

import biz.ganttproject.app.Barrier
import biz.ganttproject.app.BarrierEntrance
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.ProjectEventListener
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.event.*
import net.sourceforge.ganttproject.undo.GPUndoListener
import javax.swing.event.UndoableEditEvent

/**
 * Holds the current state of a Gantt project, updating it on events.
 *
 * @param projectDatabase - database which holds the current project state.
 */
internal class ProjectEventListenerImpl(
  private val projectDatabase: ProjectDatabase, private val taskManagerSupplier: ()-> TaskManager)
  : TaskListener, ProjectEventListener.Stub(), GPUndoListener {

  private fun withLogger(errorMessage: () -> String, body: () -> Unit) {
    try {
      body()
    } catch (e: Exception) {
      LOG.error("${errorMessage()} {}", e)
    }
  }

  override fun projectOpened(barrierRegistry: BarrierEntrance, barrier: Barrier<IGanttProject>) {
    projectDatabase.shutdown()
    barrier.await { it.taskManager.tasks.forEach(projectDatabase::insertTask) }
  }

  override fun projectClosed() = withLogger({ "Failed to close project" }) {
    // TODO: Keep project database on restore.
    // projectDatabase.shutdown()
  }

  override fun taskAdded(event: TaskHierarchyEvent) = withLogger({ "Failed to add task ${event.task.taskID}" }) {
    projectDatabase.insertTask(event.task)
  }

  override fun taskScheduleChanged(e: TaskScheduleEvent) {
    // ...
  }

  override fun dependencyAdded(e: TaskDependencyEvent) {
    // ...
  }

  override fun dependencyRemoved(e: TaskDependencyEvent) {
    // ...
  }

  override fun dependencyChanged(e: TaskDependencyEvent) {
    // ...
  }

  override fun taskRemoved(e: TaskHierarchyEvent) {
    // ...
  }

  override fun taskMoved(e: TaskHierarchyEvent) {
    // ...
  }

  override fun taskPropertiesChanged(e: TaskPropertyEvent) {
    // ...
  }

  override fun taskProgressChanged(e: TaskPropertyEvent) {
    // ...
  }

  override fun taskModelReset() {
    // ...
  }

  override fun undoableEditHappened(e: UndoableEditEvent?) {
  }

  override fun undoOrRedoHappened() {
    projectDatabase.shutdown()
    taskManagerSupplier().tasks.forEach(projectDatabase::insertTask)
  }

  override fun undoReset() {
  }
}

private val LOG = GPLogger.create("ProjectStateHolderEventListener")
