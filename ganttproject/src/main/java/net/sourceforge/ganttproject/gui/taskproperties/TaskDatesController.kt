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

import biz.ganttproject.core.option.ObservableBoolean
import biz.ganttproject.core.option.ObservableDate
import biz.ganttproject.core.option.ObservableEnum
import biz.ganttproject.core.option.ObservableInt
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.core.time.TimeDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.task.Task
import org.w3c.util.DateParser
import java.time.LocalDate
import java.util.Calendar

/**
 * Manages a group of related fields of a task properties dialog: start date, end date and duration.
 * Only two fields out of three can be enabled, as the third one is calculated from two. The controller provides
 * a special enumeration option to choose the disabled field.
 *
 * Besides, only the start date can be enabled if a task is a milestone.
 */
class TaskDatesController(private val task: Task, milestoneOption: ObservableBoolean, private val coroutineScope: CoroutineScope) {
  private val isMilestone = task.isMilestone()
  private val calendar = task.manager.calendar

  // TODO: get back "around project start" validator.
  //    var validator = DateValidators.INSTANCE.aroundProjectStart(myProject.getTaskManager().getProjectStart());
  internal val startDateOption = ObservableDate("startDate", task.start.toLocalDate())
  internal val endDateOption = ObservableDate("realEndDate", task.end.toLocalDate())
  internal val displayEndDateOption = ObservableDate("endDate", task.end.displayValue.toLocalDate())
  internal val durationOption = ObservableInt("duration", task.duration.value.toInt())
  internal val schedulingOptions = ObservableEnum("scheduling.manual", CalculatedPart.END,
    CalculatedPart.entries.toTypedArray()
  )

  private fun onSchedulingOptionChange(calculatedPart: CalculatedPart) {
    when (calculatedPart) {
      CalculatedPart.START -> {
        startDateOption.setWritable(false)
        displayEndDateOption.setWritable(true)
        durationOption.setWritable(true)
      }
      CalculatedPart.END -> {
        displayEndDateOption.setWritable(false)
        startDateOption.setWritable(true)
        durationOption.setWritable(true)
      }
      CalculatedPart.DURATION -> {
        durationOption.setWritable(false)
        startDateOption.setWritable(true)
        displayEndDateOption.setWritable(true)
      }
    }
  }


  init {
    milestoneOption.addWatcher { event ->
      if (event.newValue) {
        durationOption.setWritable(false)
        displayEndDateOption.setWritable(false)
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
      if (event.trigger != this@TaskDatesController) {
        coroutineScope.launch {
          event.newValue?.let(::setStart)
        }
      }
    }
    endDateOption.addWatcher { event ->
        coroutineScope.launch {
          displayEndDateOption.value = event.newValue?.asDisplayValue()
          event.newValue?.let(::setEnd)
        }
    }
    displayEndDateOption.addWatcher { event ->
      if (event.trigger != this@TaskDatesController) {
        coroutineScope.launch {
          event.newValue?.let(GanttCalendar::fromLocalDate)?.let {
            it.plusOneDay().toLocalDate()?.let { realEndDate ->
              endDateOption.set(realEndDate, event.trigger)
            }
          }
        }
      }
    }
    durationOption.addWatcher { event ->
      if (event.trigger != this@TaskDatesController) {
        setLength(event.newValue)
      }
    }
  }

  private fun setLength(length: Int) {
    durationOption.set(length, this)
    when (schedulingOptions.value) {
      CalculatedPart.START -> {
        val startDate =
          if (isMilestone) endDateOption.value!!
          else DateParser.toLocalDate(calendar.shiftDate(DateParser.toJavaDate(endDateOption.value), durationOption.asTaskDuration().reverse()))
        setStart(startDate, false)
      }
      CalculatedPart.END -> {
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

  fun GanttCalendar.plusOneDay() = this.apply { add(Calendar.DATE, 1) }
  fun LocalDate.asDisplayValue() = GanttCalendar.fromLocalDate(this).displayValue.toLocalDate()

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
  START, END, DURATION
}
