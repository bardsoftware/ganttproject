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

import biz.ganttproject.customproperty.CustomPropertyHolder
import biz.ganttproject.customproperty.SimpleSelect
import net.sourceforge.ganttproject.task.MutableTask
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import kotlin.jvm.Throws

open class ProjectDatabaseException: Exception {
  constructor(message: String): super(message)
  constructor(message: String, cause: Throwable): super(message, cause)
}

interface ProjectDatabaseTxn {
  @Throws(ProjectDatabaseException::class)
  fun commit()

  @Throws(ProjectDatabaseException::class)
  fun undo()

  @Throws(ProjectDatabaseException::class)
  fun redo()
}

typealias ColumnConsumer = Pair<SimpleSelect, (Int, Any?)->Unit>
/** Storage for holding the current state of a Gantt project. */
interface ProjectDatabase {
  /** Build and execute an update query. */
  interface TaskUpdateBuilder: MutableTask {
    /** Commit task update. */
    @Throws(ProjectDatabaseException::class)
    fun commit()

    fun setName(oldName: String?, newName: String?)
    fun setCustomProperties(oldCustomProperties: CustomPropertyHolder, newCustomProperties: CustomPropertyHolder)

    fun interface Factory {
      fun createTaskUpdateBuilder(task: Task): TaskUpdateBuilder
    }
  }

  /** Initialize the database. */
  @Throws(ProjectDatabaseException::class)
  fun init()

  fun createTaskUpdateBuilder(task: Task): TaskUpdateBuilder

  /** Insert the task. */
  @Throws(ProjectDatabaseException::class)
  fun insertTask(task: Task)

  /** Insert the task dependency. */
  @Throws(ProjectDatabaseException::class)
  fun insertTaskDependency(taskDependency: TaskDependency)

  /** Close connections and release the resources. */
  @Throws(ProjectDatabaseException::class)
  fun shutdown()

  /**
   * Collect queries received after txn start and commit them all at once.
   * Once committed, undo & redo actions are supported by the transaction.
   */
  @Throws(ProjectDatabaseException::class)
  fun startTransaction(title: String = ""): ProjectDatabaseTxn

  /** Fetch transactions starting with the specified transaction id. */
  @Throws(ProjectDatabaseException::class)
  fun fetchTransactions(startLocalTxnId: Int = 0, limit: Int): List<XlogRecord>

  /** Run a query with the given `whereExpression` against the Task table.
   * The query results are converted to Task instances with `lookupById`
   */
  @Throws(ProjectDatabaseException::class)
  fun findTasks(whereExpression: String, lookupById: (Int)->Task?): List<Task>

  @Throws(ProjectDatabaseException::class)
  fun mapTasks(vararg columnConsumer: ColumnConsumer)

  @Throws(ProjectDatabaseException::class)
  fun validateColumnConsumer(columnConsumer: ColumnConsumer)
}
