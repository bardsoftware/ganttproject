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

import java.util.*

/**
 * @author bard
 */
class TimeUnitFunctionOfDateImpl(
  name: String,
  graph: TimeUnitGraph,
  directAtomUnit: TimeUnit,
  framer: DateFrameable
) :
  TimeUnitDateFrameableImpl(name, graph, directAtomUnit, framer), TimeUnitFunctionOfDate {
  private val myDirectFrameable: DateFrameable = directAtomUnit

  override fun createTimeUnit(date: Date): TimeUnit {
    // TODO Only works if myBaseDate is not a TimeUnitFunctiongOfDateImpl!
    // (Quarter -> Month -> Day fails!)
    return ParameterizedTimeUnitImpl(date)
  }

  override fun getAtomCount(atomUnit: TimeUnit): Int {
    throw UnsupportedOperationException(
      "This time unit is function of date. Use method createTimeUnit() to create a parameterized instance."
    )
  }

  private inner class ParameterizedTimeUnitImpl(myBaseDate: Date) : TimeUnit {
    private val myRightDate: Date = this@TimeUnitFunctionOfDateImpl.adjustRight(myBaseDate)

    private val myLeftDate: Date = this@TimeUnitFunctionOfDateImpl.adjustLeft(myBaseDate)

    private var myAtomCount = -1

    override val name = this@TimeUnitFunctionOfDateImpl.name

    override fun isConstructedFrom(unit: TimeUnit): Boolean {
      return this@TimeUnitFunctionOfDateImpl.isConstructedFrom(unit)
    }

    override fun getAtomCount(atomUnit: TimeUnit): Int {
      if (atomUnit === this@TimeUnitFunctionOfDateImpl || atomUnit === this) {
        return 1
      }
      val directAtomCount: Int = this.directAtomCount
      return directAtomCount * directAtomUnit!!.getAtomCount(atomUnit)
    }

    val directAtomCount: Int
      get() {
        if (myAtomCount == -1) {
          myAtomCount = 0
          var leftDate =
            myDirectFrameable.jumpLeft(myRightDate)
          while (leftDate.compareTo(myLeftDate) >= 0) {
            leftDate = myDirectFrameable.jumpLeft(leftDate)
            myAtomCount++
          }
        }
        return myAtomCount
      }

    override val directAtomUnit = this@TimeUnitFunctionOfDateImpl.directAtomUnit!!

    override fun adjustRight(baseDate: Date): Date {
      return this@TimeUnitFunctionOfDateImpl.adjustRight(baseDate)
    }

    override fun adjustLeft(baseDate: Date): Date {
      return this@TimeUnitFunctionOfDateImpl.adjustLeft(baseDate)
    }

    override fun jumpLeft(baseDate: Date): Date {
      return this@TimeUnitFunctionOfDateImpl.jumpLeft(baseDate)
    }

    override fun duration(startDate: Date, endDate: Date): TimeDuration {
      return this@TimeUnitFunctionOfDateImpl.duration(startDate, endDate)
    }

    override fun equals(o: Any?): Boolean {
      return this@TimeUnitFunctionOfDateImpl == o
    }

    override fun hashCode(): Int {
      return super.hashCode()
    }
  }
}
