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
import biz.ganttproject.ganttview.TaskFilterManager
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.task.Task
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.*

class TaskFiltersTestCase {
  private val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
  private val taskFilterManager = TaskFilterManager(taskManager)
  private fun createTask() = taskManager.createTask()

  @Test
  fun `Filter completed tasks`() {
    val child = createTask()
    child.completionPercentage = 100
    assertFalse(taskFilterManager.completedTasksFilter(taskManager.rootTask, child))

    child.completionPercentage = 50
    assert(taskFilterManager.completedTasksFilter(taskManager.rootTask, child))
   }

  @Test
  fun `Filter due today`() {
    val child = createTask()
    // display value will be today
    assert(taskFilterManager.dueTodayFilter(taskManager.rootTask, child))

    child.completionPercentage = 100
    assertFalse(taskFilterManager.dueTodayFilter(taskManager.rootTask, child))

    child.completionPercentage = 99
    assert(taskFilterManager.dueTodayFilter(taskManager.rootTask, child))

    child.shiftTask(1)
    assertFalse(taskFilterManager.dueTodayFilter(taskManager.rootTask, child))

    child.shiftTask(-3)
    assertFalse(taskFilterManager.dueTodayFilter(taskManager.rootTask, child))
  }

  @Test
  fun `Filter overdue today`() {
    val child = createTask()
    child.end.add(Calendar.DATE, -1)
    assert(taskFilterManager.overdueFilter(taskManager.rootTask, child))

    child.completionPercentage = 100
    assertFalse(taskFilterManager.overdueFilter(taskManager.rootTask, child))

    child.completionPercentage = 99
    assert(taskFilterManager.overdueFilter(taskManager.rootTask, child))

    child.shiftTask(1)
    assertFalse(taskFilterManager.overdueFilter(taskManager.rootTask, child))

    child.shiftTask(-3)
    assert(taskFilterManager.overdueFilter(taskManager.rootTask, child))
  }

  @Test
  fun `Filter in progress today`() {
    var child = createTask()
    assert(taskFilterManager.inProgressTodayFilter(taskManager.rootTask, child))

    child.completionPercentage = 100
    assertFalse(taskFilterManager.inProgressTodayFilter(taskManager.rootTask, child))

    child.completionPercentage = 99
    assert(taskFilterManager.inProgressTodayFilter(taskManager.rootTask, child))

    child = createTask()
    child.start.add(Calendar.DATE, -1)
    child.end.add(Calendar.DATE, 1)
    assert(taskFilterManager.inProgressTodayFilter(taskManager.rootTask, child))

    child = createTask()
    child.start.add(Calendar.DATE, 1)
    child.end.add(Calendar.DATE, 2)
    assertFalse(taskFilterManager.inProgressTodayFilter(taskManager.rootTask, child))

    child = createTask()
    child.start.add(Calendar.DATE, -2)
    child.end.add(Calendar.DATE, -1)
    assertFalse(taskFilterManager.inProgressTodayFilter(taskManager.rootTask, child))
  }

  private fun Task.shiftTask(days: Int) {
    this.createShiftMutator().also {
      it.shift(taskManager.createLength(GPTimeUnitStack.DAY, days.toFloat()))
      it.commit()
    }
  }
}