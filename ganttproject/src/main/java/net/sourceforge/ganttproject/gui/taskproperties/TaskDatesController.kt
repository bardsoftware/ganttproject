/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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
package net.sourceforge.ganttproject.gui.taskproperties

import biz.ganttproject.FXUtil
import biz.ganttproject.core.option.ObservableBoolean
import biz.ganttproject.core.option.ObservableDate
import biz.ganttproject.core.option.ObservableEnum
import biz.ganttproject.core.option.ObservableInt
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import net.sourceforge.ganttproject.task.Task
import org.w3c.util.DateParser
import java.time.LocalDate

/**
 * Manages a group of related fields of a task properties dialog: start date, end date and duration.
 * Only two fields out of three can be enabled, as the third one is calculated from two. The controller provides
 * a special enumeration option to choose the disabled field.
 *
 * Besides, only the start date can be enabled if a task is a milestone.
 */
class TaskDatesController(private val task: Task, milestoneOption: ObservableBoolean) {
  private val isMilestone = task.isMilestone()
  private val calendar = task.manager.calendar

  internal val startDateOption = ObservableDate("startDate", task.start.toLocalDate())
  internal val endDateOption = ObservableDate("endDate", task.end.toLocalDate())
  internal val durationOption = ObservableInt("duration", task.duration.value.toInt())
  internal val schedulingOptions = ObservableEnum("schedulingOptions", CalculatedPart.END_DATE,
    CalculatedPart.entries.toTypedArray()
  )

  private fun onSchedulingOptionChange(calculatedPart: CalculatedPart) {
    when (calculatedPart) {
      CalculatedPart.START_DATE -> {
        startDateOption.setWritable(false)
        endDateOption.setWritable(true)
        durationOption.setWritable(true)
      }
      CalculatedPart.END_DATE -> {
        endDateOption.setWritable(false)
        startDateOption.setWritable(true)
        durationOption.setWritable(true)
      }
      CalculatedPart.DURATION -> {
        durationOption.setWritable(false)
        startDateOption.setWritable(true)
        endDateOption.setWritable(true)
      }
    }
  }


  init {
    milestoneOption.addWatcher { event ->
      if (event.newValue) {
        durationOption.setWritable(false)
        endDateOption.setWritable(false)
        startDateOption.setWritable(true)
        schedulingOptions.setWritable(false)
      } else {
        schedulingOptions.setWritable(true)
        onSchedulingOptionChange(schedulingOptions.value)
      }
    }
    schedulingOptions.addWatcher { event -> onSchedulingOptionChange(event.newValue)}
    onSchedulingOptionChange(schedulingOptions.value)

    startDateOption.addWatcher { event ->
      if (event.trigger != this) {
        FXUtil.runLater {
          event.newValue?.let(::setStart)
        }
      }
    }
    endDateOption.addWatcher { event ->
      if (event.trigger != this) {
        FXUtil.runLater {
          event.newValue?.let(::setEnd)
        }
      }
    }
    durationOption.addWatcher { event ->
      if (event.trigger != this) {
        setLength(event.newValue)
      }
    }
  }

  private fun setLength(length: Int) {
    durationOption.set(length, this)
    when (schedulingOptions.value) {
      CalculatedPart.START_DATE -> {
        val startDate =
          if (isMilestone) endDateOption.value!!
          else DateParser.toLocalDate(calendar.shiftDate(DateParser.toJavaDate(endDateOption.value), durationOption.asTaskDuration().reverse()))
        setStart(startDate, false)
      }
      CalculatedPart.END_DATE -> {
        val endDate =
          if (isMilestone) startDateOption.value!!
          else DateParser.toLocalDate(calendar.shiftDate(DateParser.toJavaDate(startDateOption.value), durationOption.asTaskDuration()))
        setEnd(endDate, false)
      }
      CalculatedPart.DURATION -> {}
    }
  }

  private fun setStart(startDate: LocalDate, recalculateEnd: Boolean = true) {
    startDateOption.set(startDate, this)
    if (!recalculateEnd) {
      return
    }
    if (!isMilestone && schedulingOptions.value == CalculatedPart.DURATION) {
      adjustLength()
    } else {
      val endDate =
        if (isMilestone) startDate
        else DateParser.toLocalDate(calendar.shiftDate(DateParser.toJavaDate(startDate), durationOption.asTaskDuration()))
      setEnd(endDate, false)
    }
  }

  private fun setEnd(endDate: LocalDate, recalculateStart: Boolean = true) {
    endDateOption.set(endDate, this)
    if (!recalculateStart) {
      return
    }
    if (!isMilestone && schedulingOptions.value == CalculatedPart.DURATION) {
      adjustLength()
    } else {
      val startDate =
        if (isMilestone) endDate
        else DateParser.toLocalDate(calendar.shiftDate(DateParser.toJavaDate(endDate), durationOption.asTaskDuration().reverse()))
      setStart(startDate, false)
    }
  }

  private fun adjustLength() {
    task.unpluggedClone().let {
      it.start = GanttCalendar.fromLocalDate(startDateOption.value!!)
      it.end = GanttCalendar.fromLocalDate(endDateOption.value!!)
      val length = it.duration.length
      if (length > 0) {
        durationOption.set(length, this)
      }
    }
  }
  private fun ObservableInt.asTaskDuration(): TimeDuration = this@TaskDatesController.task.manager.createLength(this.value.toLong())
}

internal enum class CalculatedPart {
  START_DATE, END_DATE, DURATION
}
