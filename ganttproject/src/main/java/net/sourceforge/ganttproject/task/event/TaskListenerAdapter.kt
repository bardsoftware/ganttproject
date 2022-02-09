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
package net.sourceforge.ganttproject.task.event

import biz.ganttproject.app.TimerBarrier


/**
 * This adapter class allows for delegating all events to the single handler and overriding handling of each
 * particular event type.
 *
 * @author Dmitry Barashev(dbarashev@ganttproject.biz)
 */
open class TaskListenerAdapter(private val allEventsHandler: ()->Unit = {}) : TaskListener {
  var dependencyAddedHandler: ((TaskDependencyEvent) -> Unit)? = null
  var dependencyRemovedHandler: ((TaskDependencyEvent) -> Unit)? = null
  var dependencyChangedHandler: ((TaskDependencyEvent) -> Unit)? = null
  var taskAddedHandler: ((TaskHierarchyEvent) -> Unit)? = null
  var taskRemovedHandler: ((TaskHierarchyEvent) -> Unit)? = null
  var taskMovedHandler: ((TaskHierarchyEvent) -> Unit)? = null
  var taskPropertiesChangedHandler: ((TaskPropertyEvent) -> Unit)? = null
  var taskProgressChangedHandler: ((TaskPropertyEvent) -> Unit)? = null
  var taskScheduleChangedHandler: ((TaskScheduleEvent) -> Unit)? = null
  var taskModelResetHandler: (() -> Unit)? = null

  override fun taskScheduleChanged(e: TaskScheduleEvent) {
    taskScheduleChangedHandler?.also { it(e) } ?: allEventsHandler()
  }

  override fun dependencyAdded(e: TaskDependencyEvent) {
    dependencyAddedHandler?.also { it(e) } ?: allEventsHandler()
  }

  override fun dependencyRemoved(e: TaskDependencyEvent) {
    dependencyRemovedHandler?.also { it(e) } ?: allEventsHandler()
  }

  override fun dependencyChanged(e: TaskDependencyEvent) {
    dependencyChangedHandler?.also { it(e) } ?: allEventsHandler()
  }

  override fun taskAdded(e: TaskHierarchyEvent) {
    taskAddedHandler?.also { it(e) } ?: allEventsHandler()
  }

  override fun taskRemoved(e: TaskHierarchyEvent) {
    taskRemovedHandler?.also { it(e) } ?: allEventsHandler()
  }

  override fun taskMoved(e: TaskHierarchyEvent) {
    taskMovedHandler?.also { it(e) } ?: allEventsHandler()
  }

  override fun taskPropertiesChanged(e: TaskPropertyEvent) {
    taskPropertiesChangedHandler?.also { it(e) } ?: allEventsHandler()
  }

  override fun taskProgressChanged(e: TaskPropertyEvent) {
    taskProgressChangedHandler?.also { it(e) } ?: allEventsHandler()
  }
  override fun taskModelReset() {
    taskModelResetHandler?.also { it() } ?: allEventsHandler()
  }
}

fun createTaskListenerWithTimerBarrier(timerBarrier: TimerBarrier) =
  TaskListenerAdapter(allEventsHandler = { timerBarrier.inc() })