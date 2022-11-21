/*
Copyright 2022 BarD Software s.r.o, Alexander Popov

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

package net.sourceforge.ganttproject.chart

import biz.ganttproject.core.option.DefaultEnumerationOption
import biz.ganttproject.core.option.ObservableObject
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.TimeUnit
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import biz.ganttproject.core.time.impl.WeekFramerImpl
import com.google.common.base.Function
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * @author apopov77@gmail.com
 */

val usWeekNumbering = Function { date: Date ->
  Calendar.Builder().setLocale(Locale.US)
    .setWeekDefinition(Calendar.SUNDAY, 1)
    .setInstant(date)
    .build().get(Calendar.WEEK_OF_YEAR)
}

val europeanWeekNumbering = Function { date: Date ->
  Calendar.Builder().setLocale(Locale.UK)
    .setWeekDefinition(Calendar.MONDAY, 4)
    .setInstant(date)
    .build().get(Calendar.WEEK_OF_YEAR)
}

val defaultWeekNumbering = Function { date: Date ->
  CalendarFactory.createGanttCalendar(date).let {
    it[Calendar.WEEK_OF_YEAR]
  }
}

class RelativeWeekNumbering(private val startProjectDate: Date) : Function<Date, Int> {
  override fun apply(date: Date): Int {
    val weekFramer = WeekFramerImpl()
    val adjustedDate = weekFramer.adjustLeft(date)
    val adjustedProjectStartDate = weekFramer.adjustLeft(startProjectDate)
    return adjustedProjectStartDate.toInstant().until(adjustedDate.toInstant(), ChronoUnit.DAYS).toInt() /7 +
      if (adjustedDate.before(adjustedProjectStartDate)) 0 else 1
  }
}

typealias WeekNumberingFunction = java.util.function.Function<Date, Int>
const val DEFAULT = "chart.weekNumbering.default"
const val EUROPEAN = "chart.weekNumbering.european"
const val US = "chart.weekNumbering.us"
const val RELATIVE_TO_PROJECT = "chart.weekNumbering.relative_to_project"

object WeekOption : DefaultEnumerationOption<String?>(
  "chart.weekNumbering",
  arrayOf(
    DEFAULT,
    EUROPEAN,
    US,
    RELATIVE_TO_PROJECT
  ))

/**
 * Encapsulates objects and functions required for correct work of week numbering option.
 */
class WeekNumbering(private val taskManager: TaskManager) {
  val option = WeekOption
  val numberingFunction = ObservableObject<WeekNumberingFunction>(initValue = defaultWeekNumbering)

  init {
    option.addChangeValueListener {
      updateWeekNumbering()
    }
    updateWeekNumbering()
    taskManager.addTaskListener(TaskListenerAdapter(this::updateWeekNumbering))
  }

  private fun updateWeekNumbering() {
    numberingFunction.set(
      when (val optionValue = option.selectedValue ?: DEFAULT) {
        US -> usWeekNumbering
        EUROPEAN -> europeanWeekNumbering
        DEFAULT -> defaultWeekNumbering
        RELATIVE_TO_PROJECT -> RelativeWeekNumbering(taskManager.projectStart)
        else -> error("Unexpected value of week numbering option: $optionValue")
      }
    )
  }

  fun decorate(timeUnit: TimeUnit) =
    if (timeUnit != GPTimeUnitStack.WEEK) {
      timeUnit
    } else {
      WeekTimeUnitDecorator(timeUnit, option)
    }
}

private val weekFramerUs = WeekFramerImpl {
  Calendar.Builder().setLocale(Locale.US)
    .setWeekDefinition(Calendar.SUNDAY, 1)
    .build()
}

private val weekFramerEurope = WeekFramerImpl {
  Calendar.Builder().setLocale(Locale.UK)
    .setWeekDefinition(Calendar.MONDAY, 4)
    .build()
}

/**
 * Time units defined in GPTimeUnitStack are integrated with the default locale which is set via GanttLanguage,
 * and it is not easy to make them aware of the week numbering option.
 *
 * We need to change the first day of the week depending on the week numbering option value, however, it seems
 * that we only need it to display the week boundaries and number on the chart. Hence, we decorate time
 * units which are passed to the renderers so that they account for the week numbering option value.
 */
class WeekTimeUnitDecorator(
  private val weekTimeUnit: TimeUnit,
  private val weekNumberingOption: WeekOption): TimeUnit by weekTimeUnit {

  override fun adjustRight(baseDate: Date): Date =
    when (weekNumberingOption.selectedValue) {
      US -> weekFramerUs.adjustRight(baseDate)
      EUROPEAN -> weekFramerEurope.adjustRight(baseDate)
      else -> weekTimeUnit.adjustRight(baseDate)
    }

  override fun adjustLeft(baseDate: Date): Date =
    when (weekNumberingOption.selectedValue) {
      US -> weekFramerUs.adjustLeft(baseDate)
      EUROPEAN -> weekFramerEurope.adjustLeft(baseDate)
      else -> weekTimeUnit.adjustLeft(baseDate)
    }

  override fun jumpLeft(baseDate: Date): Date =
    when (weekNumberingOption.selectedValue) {
      US -> weekFramerUs.jumpLeft(baseDate)
      EUROPEAN -> weekFramerEurope.jumpLeft(baseDate)
      else -> weekTimeUnit.jumpLeft(baseDate)
    }
}
