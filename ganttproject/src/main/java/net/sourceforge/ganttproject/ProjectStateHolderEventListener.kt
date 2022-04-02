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

package net.sourceforge.ganttproject

import net.sourceforge.ganttproject.ProjectEventListener.Stub
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.task.event.*

/**
 * Holds the current state of a Gantt project, updating it on events.
 *
 * @param databaseFactory - factory for generating a project state storage.
 */
class ProjectStateHolderEventListener(private val databaseFactory: () -> ProjectDatabase) : TaskListener, Stub() {
  private var lazyProjectDatabase: ProjectDatabase? = null

  private fun isInitialized(): Boolean = lazyProjectDatabase != null

  private fun getDatabase(): ProjectDatabase {
    return lazyProjectDatabase ?: databaseFactory().also { it.init(); lazyProjectDatabase = it }
  }

  private fun resetDatabase() { lazyProjectDatabase = null  }

  private fun withLogger(errorMessage: String, body: () -> Unit) {
    try {
      body()
    } catch (e: Exception) {
      LOG.error("$errorMessage {}", e)
    }
  }

  override fun projectClosed() = withLogger("Failed to close project") {
    if (isInitialized()) {
      // ...
      getDatabase().shutdown()
      resetDatabase()
    }
  }

  override fun taskAdded(event: TaskHierarchyEvent) = withLogger("Failed to add task ${event.task.taskID}") {
    getDatabase().insertTask(event.task)
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
}

private val LOG = GPLogger.create("ProjectStateHolderEventListener")
