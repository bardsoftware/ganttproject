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

/**
 * Created by IntelliJ IDEA.
 *
 * @author bard Date: 01.02.2004
 */
class TimeUnitGraph {
  private val myUnit2compositions: MutableMap<TimeUnit?, List<Composition>> = HashMap()

  fun createAtomTimeUnit(name: String): TimeUnit {
    val result: TimeUnit = TimeUnitImpl(name, this, null)
    val compositions: MutableList<Composition> = ArrayList()
    compositions.add(Composition(result, 1))
    myUnit2compositions[result] = compositions
    return result
  }

  fun createDateFrameableTimeUnit(
    name: String,
    atomUnit: TimeUnit,
    atomCount: Int,
    framer: DateFrameable,
    durationCalculator: DurationCalculator? = null
  ): TimeUnit {
    val result: TimeUnit = TimeUnitDateFrameableImpl(name, this, atomUnit, framer, durationCalculator)
    registerTimeUnit(result, atomCount)
    return result
  }

  fun createTimeUnitFunctionOfDate(name: String, atomUnit: TimeUnit, framer: DateFrameable): TimeUnitFunctionOfDate {
    val result: TimeUnitFunctionOfDate = TimeUnitFunctionOfDateImpl(name, this, atomUnit, framer)
    registerTimeUnit(result, -1)
    return result
  }

  private fun registerTimeUnit(unit: TimeUnit, atomCount: Int) {
    val atomUnit = unit.directAtomUnit
    val transitiveCompositions = myUnit2compositions[atomUnit]
      ?: throw RuntimeException("Atom unit=$atomUnit is unknown")
    val compositions: MutableList<Composition> = ArrayList(transitiveCompositions.size + 1)
    compositions.add(Composition(unit, 1))
    for (i in transitiveCompositions.indices) {
      val nextTransitive = transitiveCompositions[i]
      compositions.add(Composition(nextTransitive, atomCount))
    }
    myUnit2compositions[unit] = compositions
  }

  fun getComposition(timeUnit: TimeUnitImpl, atomUnit: TimeUnit): Composition? {
    var result: Composition? = null
    val compositions = myUnit2compositions[timeUnit]
      ?: throw RuntimeException("Unit=$timeUnit has no compositions")
    for (i in compositions.indices) {
      val next = compositions[i]
      if (next.myAtom == atomUnit) {
        result = next
        break
      }
    }
    return result
  }

  inner class Composition {
    val myAtom: TimeUnit

    val atomCount: Int

    constructor(atomUnit: TimeUnit, atomCount: Int) {
      myAtom = atomUnit
      this.atomCount = atomCount
    }

    constructor(transitiveComposition: Composition, atomCount: Int) {
      this.atomCount = atomCount * transitiveComposition.atomCount
      myAtom = transitiveComposition.myAtom
    }
  }
}
