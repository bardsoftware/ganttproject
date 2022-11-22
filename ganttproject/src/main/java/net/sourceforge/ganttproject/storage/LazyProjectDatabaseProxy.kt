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

import biz.ganttproject.storage.db.tables.records.TaskRecord
import net.sourceforge.ganttproject.ProjectEventListener
import net.sourceforge.ganttproject.storage.ProjectDatabase.*
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import net.sourceforge.ganttproject.task.event.TaskListener
import net.sourceforge.ganttproject.undo.GPUndoListener

/**
 * ProjectDatabase implementation with lazy initialization. After each shutdown, a new database is created.
 *
 * @param databaseFactory - factory for generating a project state database.
 */
class LazyProjectDatabaseProxy(private val databaseFactory: () -> ProjectDatabase, private val taskManager: () -> TaskManager): ProjectDatabase {
  private var lazyProjectDatabase: ProjectDatabase? = null
  private val projectEventListenerImpl by lazy { ProjectEventListenerImpl(this, taskManager) }

  private fun isInitialized(): Boolean = lazyProjectDatabase != null

  private fun getDatabase(): ProjectDatabase {
    return lazyProjectDatabase ?: databaseFactory().also {
      it.init();
      lazyProjectDatabase = it
      H2Functions.taskManager.set(taskManager())
    }
  }

  override fun startLog(baseTxnId: String) {
    getDatabase().startLog(baseTxnId)
  }

  private fun resetDatabase() { lazyProjectDatabase = null  }

  override fun init() {
    // Lazy initialization.
  }

  override fun createTaskUpdateBuilder(task: Task): TaskUpdateBuilder {
    return getDatabase().createTaskUpdateBuilder(task)
  }

  override fun insertTask(task: Task) {
    getDatabase().insertTask(task)
  }

  override fun insertTaskDependency(taskDependency: TaskDependency) {
    getDatabase().insertTaskDependency(taskDependency)
  }

  override fun shutdown() {
    if (isInitialized()) {
      getDatabase().shutdown()
      resetDatabase()
    }
  }

  override fun startTransaction(title: String) = getDatabase().startTransaction(title)

  override fun fetchTransactions(startLocalTxnId: Int, limit: Int): List<XlogRecord> {
    return getDatabase().fetchTransactions(startLocalTxnId, limit)
  }

  override fun findTasks(whereExpression: String, lookupById: (Int) -> Task?): List<Task> = getDatabase().findTasks(whereExpression, lookupById)

  override fun mapTasks(vararg columnConsumer: ColumnConsumer) {
    getDatabase().mapTasks(*columnConsumer)
  }

  override fun validateColumnConsumer(columnConsumer: ColumnConsumer) {
    getDatabase().validateColumnConsumer(columnConsumer)
  }

  override fun applyUpdate(logRecords: List<XlogRecord>, baseTxnId: String, targetTxnId: String) {
    getDatabase().applyUpdate(logRecords, baseTxnId, targetTxnId)
  }

    override val outgoingTransactions: List<XlogRecord>
        get() = getDatabase().outgoingTransactions

    override fun readAllTasks(): List<TaskRecord> {
    return getDatabase().readAllTasks()
  }

  override fun addExternalUpdatesListener(listener: ProjectDatabaseExternalUpdateListener) {
    getDatabase().addExternalUpdatesListener(listener)
  }

  fun createProjectEventListener(): ProjectEventListener = projectEventListenerImpl
  fun createTaskEventListener(): TaskListener = projectEventListenerImpl
  fun createUndoListener(): GPUndoListener = projectEventListenerImpl
}
