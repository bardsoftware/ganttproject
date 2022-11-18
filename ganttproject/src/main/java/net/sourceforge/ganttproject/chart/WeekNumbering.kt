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
import biz.ganttproject.core.option.EnumerationOption
import biz.ganttproject.core.option.ObservableProperty
import biz.ganttproject.core.time.CalendarFactory
import com.google.common.base.Function
import net.sourceforge.ganttproject.gui.UIConfiguration
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter
import java.util.*

/**
 * @author apopov77@gmail.com
 */

val usWeekNumbering = Function { date: Date ->
//  val weekNumbering = WeekFields.of(Locale.US)
//  val localDate = date.toInstant().atZone(ZoneOffset.UTC).toLocalDate()
//  localDate[weekNumbering.weekOfWeekBasedYear()]
//  Calendar.getInstance(Locale.US).let {
//    it.time = date
//    it[Calendar.WEEK_OF_YEAR]
//  }
  Calendar.Builder().setLocale(Locale.US)
    .setWeekDefinition(Calendar.SUNDAY, 1)
    .setInstant(date)
    .build().get(Calendar.WEEK_OF_YEAR)
}

val europeanWeekNumbering = Function { date: Date ->
//  val weekNumbering = WeekFields.of(Locale.UK)
//  val localDate = date.toInstant().atZone(ZoneOffset.UTC).toLocalDate()
//  localDate[weekNumbering.weekOfWeekBasedYear()]
  Calendar.Builder().setLocale(Locale.UK)
    .setWeekDefinition(Calendar.MONDAY, 4)
    .setInstant(date)
    .build().also {println(it)}.get(Calendar.WEEK_OF_YEAR).also {
    System.err.println("[EUR] date=$date week=$it")
  }
}

val defaultWeekNumbering = Function { date: Date ->
  CalendarFactory.createGanttCalendar(date).let {
    it[Calendar.WEEK_OF_YEAR]
  }.also {
    System.err.println("[DEF ${GanttLanguage.getInstance().locale}] date=$date week=$it")
  }
}

class RelativeWeekNumbering(private val startProjectDate: Date) : Function<Date, Int> {
  override fun apply(date: Date): Int {
    val calendar = CalendarFactory.newCalendar()
    calendar.time = date
    var weekNum = calendar[Calendar.WEEK_OF_YEAR]
    calendar.time = startProjectDate
    val startWeekNum = calendar[Calendar.WEEK_OF_YEAR]
    weekNum -= startWeekNum
    if (weekNum >= 0) {
      weekNum++
    }
    return weekNum
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

class WeekNumbering(private val taskManager: TaskManager) {
  val option = WeekOption
  val numberingFunction = ObservableProperty<WeekNumberingFunction>("weekNumbering", defaultWeekNumbering)

  init {
    option.addChangeValueListener {
      updateWeekNumbering()
    }
    updateWeekNumbering()
    taskManager.addTaskListener(TaskListenerAdapter(this::updateWeekNumbering))
  }

  fun updateWeekNumbering() {
    numberingFunction.value = when (val optionValue = option.selectedValue ?: DEFAULT) {
      US -> usWeekNumbering
      EUROPEAN -> europeanWeekNumbering
      DEFAULT -> defaultWeekNumbering
      RELATIVE_TO_PROJECT -> RelativeWeekNumbering(taskManager.projectStart)
      else -> error("Unexpected value of week numbering option: $optionValue")
    }
  }

}
