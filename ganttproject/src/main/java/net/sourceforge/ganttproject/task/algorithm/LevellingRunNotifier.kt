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

import net.sourceforge.ganttproject.GPLogger
import java.util.concurrent.CopyOnWriteArrayList

/** A levelling run has written its dates back; [movedTasks] may be zero. */
data class LevellingRunFinished(val movedTasks: Int)

/**
 * Tells listeners that a levelling run has ended.
 *
 * The event comes after the dates are written back, so that a listener sees the levelled plan, and it comes also when
 * nothing was moved. A run inside a run is reported once, by the outer one. An exception in a listener is logged and
 * does not stop the other listeners.
 */
class LevellingRunNotifier {
  private val listeners = CopyOnWriteArrayList<(LevellingRunFinished) -> Unit>()

  /** Whether a run is being written back right now. */
  @Volatile
  var isRunning: Boolean = false
    private set

  fun addListener(listener: (LevellingRunFinished) -> Unit) {
    listeners.add(listener)
  }

  fun removeListener(listener: (LevellingRunFinished) -> Unit) {
    listeners.remove(listener)
  }

  /** Runs [writeBack], which returns the number of moved tasks, and reports the run afterwards. */
  fun runAndReport(writeBack: () -> Int): Int {
    if (isRunning) {
      return writeBack()
    }
    isRunning = true
    try {
      val moved = writeBack()
      listeners.forEach { listener ->
        try {
          listener(LevellingRunFinished(moved))
        } catch (e: Exception) {
          GPLogger.log(e)
        }
      }
      return moved
    } finally {
      isRunning = false
    }
  }
}

/** The notifier of the levelling runs of the application. */
val levellingRunNotifier = LevellingRunNotifier()
