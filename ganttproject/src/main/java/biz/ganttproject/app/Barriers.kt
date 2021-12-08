/*
 * Copyright (c) 2021 Dmitry Barashev, BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
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
package biz.ganttproject.app

import net.sourceforge.ganttproject.GPLogger
import java.util.concurrent.atomic.AtomicInteger

typealias BarrierExit<T> = (T)->Unit
typealias OnBarrierReached = ()->Unit

interface BarrierEntrance {
  fun register(activity: String): OnBarrierReached
}
fun interface Barrier<T> {
  fun await(code: BarrierExit<T>)
}

class SimpleBarrier<T> : Barrier<T> {
  private val subscribers = mutableListOf<BarrierExit<T>>()
  override fun await(code: BarrierExit<T>) { subscribers.add(code) }
  internal fun resolve(value: T) = subscribers.forEach { it(value) }
}

class TwoPhaseBarrierImpl<T>(private val value: T) : Barrier<T>, BarrierEntrance {
  private val counter = AtomicInteger(0)
  private val exits = mutableListOf<BarrierExit<T>>()
  private val activities = mutableMapOf<String, OnBarrierReached>()
  override fun await(code: BarrierExit<T>) {
    if (counter.get() == 0) {
      code(value)
    } else {
      exits.add(code)
    }
  }

  override fun register(activity: String): OnBarrierReached {
    return {
      if (counter.get() > 0) {
        BARRIER_LOGGER.debug("Barrier reached: $activity")
        //println("Barrier reached: $activity")
        activities.remove(activity)
        tick()
      }
    }.also {
      BARRIER_LOGGER.debug("Barrier waiting: $activity")
      //println("Barrier waiting: $activity")
      activities[activity] = it
      counter.incrementAndGet()
    }
  }
  private fun tick() {
    if (counter.decrementAndGet() == 0) {
      exits.forEach { it(value) }
    }
  }
}

private val BARRIER_LOGGER = GPLogger.create("App.Barrier")
