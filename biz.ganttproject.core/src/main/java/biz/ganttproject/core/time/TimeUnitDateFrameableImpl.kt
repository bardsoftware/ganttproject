/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package biz.ganttproject.core.time

import java.time.temporal.ChronoUnit
import java.util.*

/**
 * @author bard
 */
open class TimeUnitDateFrameableImpl(
  name: String,
  timeUnitGraph: TimeUnitGraph,
  atomUnit: TimeUnit,
  private val myFramer: DateFrameable,
  durationCalculator: DurationCalculator? = null
) :
  TimeUnitImpl(name, timeUnitGraph, atomUnit, durationCalculator) {
  override fun adjustRight(baseDate: Date): Date {
    return myFramer.adjustRight(baseDate)
  }

  override fun adjustLeft(baseDate: Date): Date {
    return myFramer.adjustLeft(baseDate)
  }

  override fun jumpLeft(baseDate: Date): Date {
    return myFramer.jumpLeft(baseDate)
  }
}

fun createDayDurationCalculator(): DurationCalculator {
  return { timeUnit: TimeUnit, startDate: Date, endDate: Date ->
    TimeDurationImpl(timeUnit, ChronoUnit.DAYS.between(startDate.toInstant(), endDate.toInstant()))
  }
}