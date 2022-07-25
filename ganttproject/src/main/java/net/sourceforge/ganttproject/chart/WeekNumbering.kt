package net.sourceforge.ganttproject.chart

import biz.ganttproject.core.time.CalendarFactory
import com.google.common.base.Function
import java.time.ZoneOffset
import java.time.temporal.WeekFields
import java.util.*

class WeekNumbering {


  companion object {

    @JvmStatic
    val us = Function { date: Date ->
      val weekNumbering = WeekFields.of(Locale.US)
      val localDate = date.toInstant().atZone(ZoneOffset.UTC).toLocalDate()
      localDate[weekNumbering.weekOfWeekBasedYear()]
    }

    @JvmStatic
    val european = Function { date: Date ->
      val weekNumbering = WeekFields.of(Locale.UK)
      val localDate = date.toInstant().atZone(ZoneOffset.UTC).toLocalDate()
      localDate[weekNumbering.weekOfWeekBasedYear()]
    }

    @JvmStatic
    val default = Function { date: Date ->
      val calendar = CalendarFactory.newCalendar()
      calendar.time = date
      calendar[Calendar.WEEK_OF_YEAR]
    }
  }


  class RelativeWeekNumbering(private val myStartProjectDate: Date) : Function<Date, Int> {
    override fun apply(date: Date): Int {
      val calendar = CalendarFactory.newCalendar()
      calendar.time = date
      var weekNum = calendar[Calendar.WEEK_OF_YEAR]
      calendar.time = myStartProjectDate
      val startWeekNum = calendar[Calendar.WEEK_OF_YEAR]
      weekNum = weekNum - startWeekNum
      if (weekNum >= 0) {
        weekNum++
      }
      return weekNum
    }

    override fun equals(`object`: Any?): Boolean {
      return this == `object`
    }
  }
}
