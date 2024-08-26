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
package biz.ganttproject.core.time

import java.util.*

/**
 * @author bard Date: 01.02.2004
 */
open class TimeUnitImpl(
  override val name: String,
  private val myGraph: TimeUnitGraph,
  override val directAtomUnit: TimeUnit?,
  private val durationCalculator: DurationCalculator? = null
) :
  TimeUnit {
  override fun isConstructedFrom(atomUnit: TimeUnit): Boolean {
    return myGraph.getComposition(this, atomUnit) != null
  }

  override fun getAtomCount(atomUnit: TimeUnit): Int {
    val composition = myGraph.getComposition(this, atomUnit)
      ?: throw RuntimeException("Failed to find a composition of time unit=$this from time unit=$atomUnit")
    return composition.atomCount
  }

  override fun toString(): String {
    return name + " hash=" + hashCode()
  }

  override fun adjustRight(baseDate: Date): Date {
    throw UnsupportedOperationException("Time unit=$this doesnt support this operation")
  }

  override fun adjustLeft(baseDate: Date): Date {
    throw UnsupportedOperationException("Time unit=$this doesnt support this operation")
  }

  override fun jumpLeft(baseDate: Date): Date {
    throw UnsupportedOperationException("Time unit=$this doesnt support this operation")
  }

  override fun duration(startDate: Date, endDate: Date): TimeDuration =
    durationCalculator?.let { it(this, startDate, endDate) } ?: run {
      var startDate = startDate
      var endDate = endDate
      var sign = 1
      if (endDate.before(startDate)) {
        sign = -1
        val temp = endDate
        endDate = startDate
        startDate = temp
      }
      var unitCount = 0
      while (startDate.before(endDate)) {
        startDate = adjustRight(startDate)
        unitCount++
      }
      TimeDurationImpl(this, (unitCount * sign).toLong())
    }


  override fun equals(obj: Any?): Boolean {
    if (false == obj is TimeUnitImpl) {
      return false
    }
    return this.name == obj.name
  }

  override fun hashCode(): Int {
    return name.hashCode()
  }
}
