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

import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.calendar.GPCalendarCalc
import com.google.common.collect.Range
import com.google.common.collect.TreeRangeSet
import java.time.Instant
import java.util.*
import java.util.function.Supplier

/**
 * @author dbarashev@bardsoftware.com
 */
interface TaskCalendar {
  var workingWeekends: Boolean
}

class TaskCalendarImpl(private val weekendRangeSet: WeekendExceptionRangeSet,
                       private val taskRange: Supplier<Range<Instant>>): TaskCalendar {
  override var workingWeekends: Boolean = false
  set(value) {
    field = value
    if (value) {
      this.weekendRangeSet.addRange(this.taskRange.get())
    } else {
      this.weekendRangeSet.removeRange(this.taskRange.get())
    }
  }
}

class TaskManagerCalendarImpl(
    val projectCalendar: GPCalendarCalc, val weekendExceptions: WeekendExceptionRangeSet): GPCalendarCalc by projectCalendar {
  override fun getDayMask(date: Date): Int {
    val projectDayMask = this.projectCalendar.getDayMask(date)
    if (projectDayMask.and(GPCalendar.DayMask.WORKING) == 0) {
      if (this.weekendExceptions.contains(date.toInstant())) {
        return projectDayMask.or(GPCalendar.DayMask.WORKING)
      }
    }
    return projectDayMask
  }
}

class WeekendExceptionRangeSet {
  private val rangeSet = TreeRangeSet.create<Instant>()

  fun addRange(range: Range<Instant>) {
    this.rangeSet.add(range)
    println(this.rangeSet)
  }

  fun removeRange(range: Range<Instant>) {
    this.rangeSet.remove(range)
    println(this.rangeSet)
  }

  fun contains(instant: Instant): Boolean {
    return this.rangeSet.contains(instant)
  }
}