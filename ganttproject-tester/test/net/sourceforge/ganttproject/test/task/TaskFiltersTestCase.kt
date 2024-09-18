/*
Copyright 2022 BarD Software s.r.o.

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
package net.sourceforge.ganttproject.test.task

import biz.ganttproject.core.time.impl.GPTimeUnitStack
import biz.ganttproject.ganttview.BuiltInFilters
import biz.ganttproject.ganttview.TaskFilterManager
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.storage.SQL_PROJECT_DATABASE_OPTIONS
import net.sourceforge.ganttproject.storage.SqlProjectDatabaseImpl
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import org.h2.jdbcx.JdbcDataSource
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import javax.sql.DataSource

class TaskFiltersTestCase {
  private lateinit var dataSource: DataSource
  private lateinit var projectDatabase: ProjectDatabase
  private lateinit var taskManager: TaskManager
  private lateinit var taskFilterManager: TaskFilterManager
  private fun createTask() = taskManager.createTask()

  @BeforeEach
  fun init() {
    dataSource = JdbcDataSource().also {
      it.setURL("jdbc:h2:mem:test$SQL_PROJECT_DATABASE_OPTIONS")
    }
    projectDatabase = SqlProjectDatabaseImpl(dataSource)
    projectDatabase.init()

    taskManager = TestSetupHelper.newTaskManagerBuilder().also {
      it.setTaskUpdateBuilderFactory { task -> projectDatabase.createTaskUpdateBuilder(task) }
    }.build()
    taskFilterManager = TaskFilterManager(taskManager, projectDatabase)
  }

  @Test
  fun `Filter completed tasks`() {
    val child = createTask()
    child.completionPercentage = 100
    assertFalse(BuiltInFilters.completedTasksFilter.filterFxn(taskManager.rootTask, child))

    child.completionPercentage = 50
    assert(BuiltInFilters.completedTasksFilter.filterFxn(taskManager.rootTask, child))
   }

  @Test
  fun `Filter due today`() {
    val child = createTask()
    // display value will be today
    assert(BuiltInFilters.dueTodayFilter.filterFxn(taskManager.rootTask, child))

    child.completionPercentage = 100
    assertFalse(BuiltInFilters.dueTodayFilter.filterFxn(taskManager.rootTask, child))

    child.completionPercentage = 99
    assert(BuiltInFilters.dueTodayFilter.filterFxn(taskManager.rootTask, child))

    child.shiftTask(1)
    assertFalse(BuiltInFilters.dueTodayFilter.filterFxn(taskManager.rootTask, child))

    child.shiftTask(-3)
    assertFalse(BuiltInFilters.dueTodayFilter.filterFxn(taskManager.rootTask, child))
  }

  @Test
  fun `Filter overdue today`() {
    val child = createTask()
    child.end.add(Calendar.DATE, -1)
    assert(BuiltInFilters.overdueFilter.filterFxn(taskManager.rootTask, child))

    child.completionPercentage = 100
    assertFalse(BuiltInFilters.overdueFilter.filterFxn(taskManager.rootTask, child))

    child.completionPercentage = 99
    assert(BuiltInFilters.overdueFilter.filterFxn(taskManager.rootTask, child))

    child.shiftTask(1)
    assertFalse(BuiltInFilters.overdueFilter.filterFxn(taskManager.rootTask, child))

    child.shiftTask(-3)
    assert(BuiltInFilters.overdueFilter.filterFxn(taskManager.rootTask, child))
  }

  @Test
  fun `Filter in progress today`() {
    var child = createTask()
    assert(BuiltInFilters.inProgressTodayFilter.filterFxn(taskManager.rootTask, child))

    child.completionPercentage = 100
    assertFalse(BuiltInFilters.inProgressTodayFilter.filterFxn(taskManager.rootTask, child))

    child.completionPercentage = 99
    assert(BuiltInFilters.inProgressTodayFilter.filterFxn(taskManager.rootTask, child))

    child = createTask()
    child.start.add(Calendar.DATE, -1)
    child.end.add(Calendar.DATE, 1)
    assert(BuiltInFilters.inProgressTodayFilter.filterFxn(taskManager.rootTask, child))

    child = createTask()
    child.start.add(Calendar.DATE, 1)
    child.end.add(Calendar.DATE, 2)
    assertFalse(BuiltInFilters.inProgressTodayFilter.filterFxn(taskManager.rootTask, child))

    child = createTask()
    child.start.add(Calendar.DATE, -2)
    child.end.add(Calendar.DATE, -1)
    assertFalse(BuiltInFilters.inProgressTodayFilter.filterFxn(taskManager.rootTask, child))
  }

  private fun Task.shiftTask(days: Int) {
    this.createShiftMutator().also {
      it.shift(taskManager.createLength(GPTimeUnitStack.DAY, days.toFloat()))
      it.commit()
    }
  }
}