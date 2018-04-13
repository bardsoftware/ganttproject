/*
Copyright 2018 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.core.model.task

import biz.ganttproject.core.calendar.GPCalendarCalc
import com.google.common.collect.Range
import com.google.common.collect.TreeRangeSet
import java.time.Instant
import java.util.function.Supplier

/**
 * @author dbarashev@bardsoftware.com
 */
interface TaskCalendar {
  var workingWeekends: Boolean
  fun asGPCalendar(): GPCalendarCalc
}

class TaskCalendarImpl(private val projectCalendar: GPCalendarCalc,
                       private val weekendRangeSet: WeekendExceptionRangeSet,
                       private val taskRange: Supplier<Range<Instant>>,
                       private val onChange: Runnable): TaskCalendar {
  override var workingWeekends: Boolean = false
  set(value) {
    field = value
    if (value) {
      this.weekendRangeSet.addRange(this.taskRange.get())
    } else {
      this.weekendRangeSet.removeRange(this.taskRange.get())
    }
    onChange.run()
  }

  override fun asGPCalendar(): GPCalendarCalc {
    return this.projectCalendar.copy().setWeekendExceptions(this.weekendRangeSet.taskRangeSet).build()
  }
}


class WeekendExceptionRangeSet(val globalRangeSet: TreeRangeSet<Instant>) {
  val taskRangeSet = TreeRangeSet.create<Instant>()
  fun addRange(range: Range<Instant>) {
    this.taskRangeSet.add(range)
    this.globalRangeSet.add(range)
    println(this.taskRangeSet)
  }

  fun removeRange(range: Range<Instant>) {
    this.taskRangeSet.remove(range)
    this.globalRangeSet.remove(range)
    println(this.taskRangeSet)
  }

  fun contains(instant: Instant): Boolean {
    return this.taskRangeSet.contains(instant)
  }
}