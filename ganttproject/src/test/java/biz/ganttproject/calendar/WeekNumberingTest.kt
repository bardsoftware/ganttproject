/*
Copyright 2022 BarD Software s.r.o

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
package biz.ganttproject.calendar

import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.impl.GPTimeUnitStack
import net.sourceforge.ganttproject.chart.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class WeekNumberingTest {
  @BeforeEach
  fun setUp() {
    setDefaultCalendarLocale()
  }

  @Test
  fun `project relative week numbers`() {
    // Feb 24, 2022 was Thursday
    val relativeWeekNumbering = RelativeWeekNumbering("2022-02-24".asDate())
    assertEquals(1, relativeWeekNumbering.apply("2022-02-24".asDate()))
    assertEquals(1, relativeWeekNumbering.apply("2022-02-23".asDate()))
    assertEquals(1, relativeWeekNumbering.apply("2022-02-22".asDate()))
    assertEquals(1, relativeWeekNumbering.apply("2022-02-21".asDate()))
    assertEquals(1, relativeWeekNumbering.apply("2022-02-20".asDate()))
    assertEquals(2, relativeWeekNumbering.apply("2022-02-27".asDate()))
    assertEquals(53, relativeWeekNumbering.apply("2023-02-24".asDate()))
    assertEquals(54, relativeWeekNumbering.apply("2023-03-03".asDate()))

    assertEquals(-1, relativeWeekNumbering.apply("2022-02-19".asDate()))
    assertEquals(-2, relativeWeekNumbering.apply("2022-02-12".asDate()))
  }

  @Test
  fun `decorator accounts for the week numbering option`() {
    val weekOption = WeekOption
    val decorator = WeekTimeUnitDecorator(GPTimeUnitStack.WEEK, weekOption)
    weekOption.selectedValue = DEFAULT
    assertEquals("2022-02-20".asDate(), decorator.adjustLeft("2022-02-24".asDate()))
    weekOption.selectedValue = EUROPEAN
    assertEquals("2022-02-21".asDate(), decorator.adjustLeft("2022-02-24".asDate()))
  }
}

private fun String.asDate() = SimpleDateFormat("yyyy-MM-dd").parse(this)
private fun setDefaultCalendarLocale(locale: Locale = Locale.US) {
  object : CalendarFactory() {
    init {
      setLocaleApi(object : LocaleApi {
        override fun getLocale(): Locale {
          return locale
        }

        override fun getShortDateFormat(): DateFormat {
          return DateFormat.getDateInstance(DateFormat.SHORT, locale)
        }
      })
    }
  }
}