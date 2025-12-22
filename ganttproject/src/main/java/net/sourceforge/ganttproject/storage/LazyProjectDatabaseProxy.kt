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

import biz.ganttproject.customproperty.CalculatedPropertyUpdater
import biz.ganttproject.customproperty.CustomPropertyListener
import biz.ganttproject.customproperty.CustomPropertyManager
import biz.ganttproject.storage.db.tables.records.TaskRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sourceforge.ganttproject.*
import net.sourceforge.ganttproject.storage.ProjectDatabase.*
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import net.sourceforge.ganttproject.task.event.TaskListener
import net.sourceforge.ganttproject.undo.UndoableEditTxn
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory

/**
 * ProjectDatabase implementation with lazy initialization. After each shutdown, a new database is created.
 *
 * @param databaseFactory - factory for generating a project state database.
 */
class LazyProjectDatabaseProxy(
  private val databaseFactory: () -> ProjectDatabase,
  private val taskManager: () -> TaskManager,
  private val filterUpdater: () -> Unit): ProjectDatabase {

  var projectOpenActivityFactory: ProjectOpenActivityFactory? = null
  set(value) {
    field = value
    value?.addListener(this::onProjectOpenStateMachine)
  }

  private fun onProjectOpenStateMachine(sm: ProjectOpenStateMachine) {
    // We wait for the state when task and resource tables are initialized with the project data
    sm.stateTablesReady.await {
      sm.scope.launch {
        withContext(Dispatchers.IO) {
          sm.transition(ProjectOpenActivityCalculatedModelReady(it.project)) {
            projectEventListenerImpl.whenTablesInitialized(it.project)
          }
        }
      }
    }
  }

  var isProjectOpen: Boolean = true

  private var lazyProjectDatabase: ProjectDatabase? = null
  private val calculatedPropertyUpdater by lazy { CalculatedPropertyUpdater(this,
    { taskManager().customPropertyManager },
    {
      taskManager().tasks.map {
        it.taskID to it.customValues
      }.toMap()
    }
  ) }

  override fun updateBuiltInCalculatedColumns() {
    getDatabase().updateBuiltInCalculatedColumns()
  }

  private val projectEventListenerImpl by lazy {
    ProjectEventListenerImpl(this, taskManager, calculatedPropertyUpdater, filterUpdater)
  }

  private fun isInitialized(): Boolean = lazyProjectDatabase != null

  private fun getDatabase(): ProjectDatabase {
    return synchronized(this) {
      lazyProjectDatabase ?: databaseFactory().also {
        it.init();
        lazyProjectDatabase = it
        H2Functions.taskManager.set(taskManager())
      }
    }
  }

  override fun startLog(baseTxnId: BaseTxnId) {
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

  override fun applyUpdate(logRecords: List<XlogRecord>, baseTxnId: BaseTxnId, targetTxnId: BaseTxnId) {
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

  override fun onCustomColumnChange(customPropertyManager: CustomPropertyManager) {
    if (isProjectOpen) {
      getDatabase().onCustomColumnChange(customPropertyManager)
    }
  }

  fun createProjectEventListener(): ProjectEventListener = projectEventListenerImpl
  fun createTaskEventListener(): TaskListener = projectEventListenerImpl

  fun createUndoTxnFactory(): UndoableEditTxnFactory = {
    UndoableEditTxnImpl()
  }

  fun createTaskCustomPropertyListener(): CustomPropertyListener  = projectEventListenerImpl

  private inner class UndoableEditTxnImpl : UndoableEditTxn {
    var txn: ProjectDatabaseTxn? = null
    override fun start(displayName: String) {
      txn = getDatabase().startTransaction(displayName)
    }

    override fun commit() {
      try {
        txn?.commit()
        calculatedPropertyUpdater.update()
      } catch (ex: ProjectDatabaseException) {
        GPLogger.log(ex)
      }
      finally {
        txn = null
      }
    }

    override fun rollback(ex: Throwable) {
    }

    override fun undo() {
      txn?.undo()
    }

    override fun redo() {
      txn?.redo()
    }
  }
}


