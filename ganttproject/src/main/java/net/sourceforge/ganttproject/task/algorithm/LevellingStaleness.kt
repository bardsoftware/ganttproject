/*
Copyright 2026 BarD Software s.r.o., GanttProject Cloud OU

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

import biz.ganttproject.core.calendar.GPCalendarListener
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.resource.ResourceEvent
import net.sourceforge.ganttproject.resource.ResourceView
import net.sourceforge.ganttproject.task.event.TaskDependencyEvent
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent
import net.sourceforge.ganttproject.task.event.TaskListener
import net.sourceforge.ganttproject.task.event.TaskPropertyEvent
import net.sourceforge.ganttproject.task.event.TaskScheduleEvent
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Remembers that the plan has changed since the last levelling run, so that the user can be told that the levelling
 * is due.
 *
 * It only observes events and never calculates: finding out whether a run would move anything costs a whole run. A
 * task property event does not tell which property changed, and the levelling reads custom columns, so every such
 * event counts. A plan which has just been loaded is not marked, because the program does not know when it was last
 * levelled; the mark means "changed since the last run or since the load".
 *
 * Changes made while a levelling run is being written back are the run's own, and they are ignored. Otherwise the mark
 * would be set again right after the run has cleared it.
 */
class LevellingStaleness(private val runNotifier: LevellingRunNotifier = levellingRunNotifier) {
  private val listeners = CopyOnWriteArrayList<(Boolean) -> Unit>()

  /** True when the plan has changed since the last levelling run or since it was loaded. */
  @Volatile
  var isStale: Boolean = false
    private set

  private val onLevellingRun: (LevellingRunFinished) -> Unit = { clear() }

  init {
    runNotifier.addListener(onLevellingRun)
  }

  /** Stops listening to the levelling runs and drops the listeners of the mark. */
  fun detach() {
    runNotifier.removeListener(onLevellingRun)
    listeners.clear()
  }

  /** Calls [listener] when [isStale] changes, not when it is set to the value it already has. */
  fun addListener(listener: (Boolean) -> Unit) {
    listeners.add(listener)
  }

  fun removeListener(listener: (Boolean) -> Unit) {
    listeners.remove(listener)
  }

  fun markStale() {
    if (runNotifier.isRunning) {
      return
    }
    set(true)
  }

  fun clear() = set(false)

  private fun set(value: Boolean) {
    if (isStale == value) {
      return
    }
    isStale = value
    listeners.forEach { listener ->
      try {
        listener(value)
      } catch (e: Exception) {
        GPLogger.log(e)
      }
    }
  }

  /** Marks the plan on every task change, and clears the mark when another plan is loaded. */
  val taskListener: TaskListener = object : TaskListener {
    override fun taskScheduleChanged(e: TaskScheduleEvent) = markStale()
    override fun dependencyAdded(e: TaskDependencyEvent) = markStale()
    override fun dependencyRemoved(e: TaskDependencyEvent) = markStale()
    override fun dependencyChanged(e: TaskDependencyEvent) = markStale()
    override fun taskAdded(e: TaskHierarchyEvent) = markStale()
    override fun taskRemoved(e: TaskHierarchyEvent) = markStale()
    override fun taskMoved(e: TaskHierarchyEvent) = markStale()
    override fun taskPropertiesChanged(e: TaskPropertyEvent) = markStale()
    override fun taskProgressChanged(e: TaskPropertyEvent) = markStale()
    override fun taskModelReset() = clear()
  }

  /** Marks the plan on every resource change, and clears the mark when another plan is loaded. */
  val resourceView: ResourceView = object : ResourceView {
    override fun resourceAdded(event: ResourceEvent) = markStale()
    override fun resourcesRemoved(event: ResourceEvent) = markStale()
    override fun resourceChanged(e: ResourceEvent) = markStale()
    override fun resourceAssignmentsChanged(e: ResourceEvent) = markStale()
    override fun resourceStructureChanged() = markStale()
    override fun resourceModelReset() = clear()
  }

  /** Marks the plan when the calendar changes. */
  val calendarListener: GPCalendarListener = GPCalendarListener { markStale() }
}
