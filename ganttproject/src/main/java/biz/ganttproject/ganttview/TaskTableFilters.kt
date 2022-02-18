/*
Copyright 2021 BarD Software s.r.o

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
package biz.ganttproject.ganttview

import biz.ganttproject.core.option.DefaultBooleanOption
import biz.ganttproject.core.option.GPOption
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager

typealias TaskFilter = (parent: Task, child: Task?) -> Boolean

class TaskFilterManager(taskManager: TaskManager) {
  val options: List<GPOption<*>> get() = listOf(filterCompletedTasksOption)

  val filterCompletedTasksOption = DefaultBooleanOption("filter.completedTasks", false)
  var activeFilter: TaskFilter = VOID_FILTER
    set(value) {
      field = value
      sync()
    }

  internal var sync: ()->Unit = {}
}

val VOID_FILTER: TaskFilter = { _, _ -> true }
