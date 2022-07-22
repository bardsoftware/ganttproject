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
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

typealias BarrierExit<T> = (T)->Unit
typealias OnBarrierReached = ()->Unit

/**
 * Interface for registration of a barrier entrance activity. The activity
 * receives OnBarrierReached callback which it must call once it is completed.
 */
interface BarrierEntrance {
  fun register(activity: String): OnBarrierReached
}

/**
 * This is an interface of a synchronization primitive which allows for
 * starting a number of activities, called "barrier exits", once the barrier
 * entrance condition becomes true.
 *
 * In GanttProject code, the exit opens once a few entrance activities complete and
 * reach the barrier.
 */
fun interface Barrier<T> {
  /**
   * Registers barrier exit code which will start running when the exit opens.
   */
  fun await(code: BarrierExit<T>)
}

class SimpleBarrier<T> : Barrier<T> {
  private var value: T? = null
  private val subscribers = mutableListOf<BarrierExit<T>>()
  override fun await(code: BarrierExit<T>) {
    value?.let { code(it) } ?: subscribers.add(code)
  }
  internal fun resolve(value: T) {
    subscribers.forEach { it(value) }
    this.value = value
  }
}

/**
 * This barrier implementation has two phases. In the first phase entrance activities
 * register themselves in the barrier entrance. This phase is supposed to be synchronous,
 * that is, it runs in the same thread/coroutine where the barrier was created.
 * In the second phase the registered entrance activities complete (most likely asynchronously)
 * and notify about the completion using OnBarrierReached callback which they obtain
 * at the registration moment.
 * Once all registered entrance activities reach the barrier, it opens it's exit and
 * starts the exit activities.
 */
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

/**
 * TimeBarrier opens the exit and calls exit activities once per specified time interval, provided that there have been
 * any entrance activities in that interval.
 */
class TimerBarrier(intervalMillis: Long) : Barrier<Unit> {
  private val exits = mutableListOf<BarrierExit<Unit>>()
  private val timer = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
    this::tick, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS
  )
  private val counter = AtomicInteger(0)

  override fun await(code: BarrierExit<Unit>) {
    exits.add(code)
  }

  fun inc() {
    counter.incrementAndGet()
  }

  private fun tick() {
    val value = counter.get()
    if (value > 0) {
      exits.forEach { it(Unit) }
      counter.compareAndSet(value, 0)
    }
  }
}

class ResolvedBarrier<T>(private val value: T) : Barrier<T> {
  override fun await(code: BarrierExit<T>) {
    code(value)
  }
}

private val BARRIER_LOGGER = GPLogger.create("App.Barrier")
