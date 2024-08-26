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
package biz.ganttproject.core.time

import java.util.*

/**
 * Time unit is an abstract time measurement unit, such as day, month, minute or quarter.
 *
 * Time units build a stack, where every upper time unit consists of some number of lower "atom" units (e.g. 1 day is
 * constructed from 24 hours).
 *
 * A time unit instance may be bound to some start date, and different instances may consist of different number
 * of atom units, e.g. a month instance is bound to the first day of month and consists of 28..31 day unit.
 */
interface TimeUnit : DateFrameable {
  /**
   * The unit name.
   */
  val name: String

  /**
   * Returns true if this unit is directly or transitively constructed from the given unit.
   */
  fun isConstructedFrom(unit: TimeUnit): Boolean

  /**
   * @return number of atoms used to create this TimeUnit
   * @throws UnsupportedOperationException if current TimeUnit does not have constant number of atoms.
   */
  fun getAtomCount(atomUnit: TimeUnit): Int

  /** @return the direct atom unit, or null if this unit has no atom units. */
  val directAtomUnit: TimeUnit?

  fun duration(startDate: Date, endDate: Date): TimeDuration
}

typealias DurationCalculator = (timeUnit: TimeUnit, startDate: Date, endDate: Date) -> TimeDuration