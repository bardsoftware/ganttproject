/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

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
package net.sourceforge.ganttproject.task.algorithm

import biz.ganttproject.core.time.TimeDuration
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManagerImpl
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException

class ShiftTaskTreeAlgorithm(private val taskManager: TaskManagerImpl) {
  @Throws(AlgorithmException::class)
  fun run(tasks: List<Task>, shift: TimeDuration, deep: Boolean) {
    taskManager.setEventsEnabled(false)
    try {
      for (t in tasks) {
        shiftTask(t, shift, deep)
      }
      try {
        taskManager.algorithmCollection.scheduler.run()
      } catch (e: TaskDependencyException) {
        throw AlgorithmException("Failed to reschedule the following tasks tasks after move:\n$tasks", e)
      }
    } finally {
      taskManager.setEventsEnabled(true)
    }
  }

  @Throws(AlgorithmException::class)
  fun run(rootTask: Task, shift: TimeDuration?, deep: Boolean) {
    run(listOf(rootTask), shift!!, deep)
  }

  private fun shiftTask(rootTask: Task, shift: TimeDuration, deep: Boolean) {
    if (rootTask !== taskManager.rootTask) {
      rootTask.shift(shift)
    }
    if (deep) {
      val nestedTasks = rootTask.manager.taskHierarchy.getNestedTasks(rootTask)
      for (i in nestedTasks.indices) {
        val next = nestedTasks[i]
        shiftTask(next, shift, true)
      }
    }
  }

  companion object {
    const val DEEP = true
    const val SHALLOW = false
  }
}