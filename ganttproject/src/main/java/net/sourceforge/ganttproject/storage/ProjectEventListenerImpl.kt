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
import biz.ganttproject.customproperty.CalculatedPropertyUpdater
import biz.ganttproject.customproperty.CustomPropertyEvent
import biz.ganttproject.customproperty.CustomPropertyListener
import biz.ganttproject.ganttview.TaskFilterManager
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.ProjectEventListener
import net.sourceforge.ganttproject.document.Document
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
  private val projectDatabase: LazyProjectDatabaseProxy,
  private val taskManagerSupplier: ()->TaskManager,
  private val calculatedPropertyUpdater: CalculatedPropertyUpdater,
  private val filterUpdater: ()->Unit)
  : TaskListener, ProjectEventListener.Stub(), GPUndoListener, CustomPropertyListener {

  private fun withLogger(errorMessage: () -> String, body: () -> Unit) {
    try {
      body()
    } catch (e: Exception) {
      LOG.error("${errorMessage()} {}", e)
    }
  }

  override fun projectOpened(barrierRegistry: BarrierEntrance, barrier: Barrier<IGanttProject>) {
    projectDatabase.shutdown()
    barrier.await {
      projectDatabase.isProjectOpen = true
      projectDatabase.onCustomColumnChange(it.taskCustomColumnManager)
      it.taskManager.tasks.forEach(projectDatabase::insertTask)
      calculatedPropertyUpdater.update()
      filterUpdater()
    }
  }

  override fun projectClosed() = withLogger({ "Failed to close project" }) {
    projectDatabase.isProjectOpen = false
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
    calculatedPropertyUpdater.update()
  }

  override fun undoOrRedoHappened() {
  }

  override fun projectRestoring(completion: Barrier<Document?>) {
    completion.await {
      projectDatabase.shutdown()
      projectDatabase.onCustomColumnChange(taskManagerSupplier().customPropertyManager)
      taskManagerSupplier().tasks.forEach(projectDatabase::insertTask)
      calculatedPropertyUpdater.update();
    }
  }

  override fun undoReset() {
  }

  override fun customPropertyChange(event: CustomPropertyEvent) {
    projectDatabase.onCustomColumnChange(taskManagerSupplier().customPropertyManager)
  }
}

private val LOG = GPLogger.create("ProjectStateHolderEventListener")
