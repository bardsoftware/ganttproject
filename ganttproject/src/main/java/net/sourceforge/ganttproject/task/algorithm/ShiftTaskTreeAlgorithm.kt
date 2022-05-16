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

import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.calendar.GPCalendarCalc
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.TimeDuration
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskImpl.RESTLESS_CALENDAR
import net.sourceforge.ganttproject.task.TaskManagerImpl
import net.sourceforge.ganttproject.task.TaskMutator
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException
import net.sourceforge.ganttproject.task.depthFirstWalk

class ShiftTaskTreeAlgorithm(private val taskManager: TaskManagerImpl) {
  @Throws(AlgorithmException::class)
  fun run(tasks: List<Task>, shift: TimeDuration, deep: Boolean) {
    taskManager.setEventsEnabled(false)
    try {
      tasks.forEach { t ->
        val mutators = mutableListOf<TaskMutator>()
        taskManager.taskHierarchy.depthFirstWalk(t) { parent, child, _ ->
          if (child != null) deep else {
            if (parent != taskManager.rootTask) {
              val mutator = parent.createMutatorFixingDuration().also {
                mutators.add(it)
              }
              shift(parent, shift, mutator)
            }
            true
          }
        }
        mutators.forEach { it.commit() }
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

  private fun shift(task: Task, duration: TimeDuration, mutator: TaskMutator) {
    val unitCount = duration.getLength(task.duration.timeUnit)
    if (unitCount != 0f) {
      val newStart = if (unitCount > 0f) {
        RESTLESS_CALENDAR.shiftDate(task.start.time, taskManager.createLength(task.duration.timeUnit, unitCount)).let {
          if (0 == (taskManager.calendar.getDayMask(it) and GPCalendar.DayMask.WORKING)) {
            taskManager.calendar.findClosest(it, task.duration.timeUnit, GPCalendarCalc.MoveDirection.FORWARD, GPCalendar.DayType.WORKING)
          } else it
        }
      } else {
        RESTLESS_CALENDAR.shiftDate(task.start.time, taskManager.createLength(task.duration.timeUnit, unitCount.toLong().toFloat())).let {
          if (0 == (taskManager.calendar.getDayMask(it) and GPCalendar.DayMask.WORKING)) {
            taskManager.calendar.findClosest(it, task.duration.timeUnit, GPCalendarCalc.MoveDirection.BACKWARD, GPCalendar.DayType.WORKING)
          } else it
        }
      }
      mutator.setStart(CalendarFactory.createGanttCalendar(newStart))
    }
  }

  companion object {
    const val DEEP = true
    const val SHALLOW = false
  }
}