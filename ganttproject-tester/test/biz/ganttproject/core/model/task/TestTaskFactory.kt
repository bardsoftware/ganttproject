/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.core.model.task

import biz.ganttproject.core.time.GanttCalendar
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.dependency.TaskDependency
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException

class TestTaskFactory(internal val taskManager: TaskManager) {
  fun createTask(): Task {
    val result: Task = taskManager.createTask()
    result.move(taskManager.getRootTask())
    result.name = result.taskID.toString()
    return result
  }

  fun createTask(startIsoDate: String) = createTask(GanttCalendar.parseXMLDate(startIsoDate))
  fun createTask(start: GanttCalendar?): Task {
    return createTask(start, 1)
  }

  fun createTask(start: GanttCalendar?, duration: Int): Task {
    val result = createTask()
    result.start = start
    result.duration = taskManager.createLength(duration.toLong())
    return result
  }

  @Throws(TaskDependencyException::class)
  fun createDependency(dependant: Task?, dependee: Task?): TaskDependency {
    return taskManager.getDependencyCollection().createDependency(dependant, dependee)
  }
}