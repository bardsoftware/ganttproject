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

import biz.ganttproject.customproperty.SimpleSelect
import net.sourceforge.ganttproject.task.MutableTask
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import kotlin.jvm.Throws

open class ProjectDatabaseException: Exception {
  constructor(message: String): super(message)
  constructor(message: String, cause: Throwable): super(message, cause)
}

typealias ColumnConsumer<T> = Pair<SimpleSelect<T>, (Int, T?)->Unit>
/** Storage for holding the current state of a Gantt project. */
interface ProjectDatabase {
  /** Build and execute an update query. */
  interface TaskUpdateBuilder: MutableTask {
    /** Commit task update. */
    @Throws(ProjectDatabaseException::class)
    fun commit()

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

  /** Collect queries received after txn start and commit them all at once. */
  @Throws(ProjectDatabaseException::class)
  fun startTransaction()
  @Throws(ProjectDatabaseException::class)
  fun commitTransaction()

  /** Fetch transactions starting with the specified transaction id. */
  @Throws(ProjectDatabaseException::class)
  fun fetchTransactions(startTxnId: Int = 0, limit: Int): List<XlogRecord>

  /** Run a query with the given `whereExpression` against the Task table.
   * The query results are converted to Task instances with `lookupById`
   */
  @Throws(ProjectDatabaseException::class)
  fun findTasks(whereExpression: String, lookupById: (Int)->Task?): List<Task>

  fun <T1, T2, T3, T4, T5> mapTasks(col1: ColumnConsumer<T1>,
                                    col2: ColumnConsumer<T2>? = null,
                                    col3: ColumnConsumer<T3>? = null,
                                    col4: ColumnConsumer<T4>? = null,
                                    col5: ColumnConsumer<T5>?= null)
}
