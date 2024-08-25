/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.core.calendar.walker

import biz.ganttproject.core.calendar.GPCalendar
import biz.ganttproject.core.calendar.GPCalendar.DayMask
import biz.ganttproject.core.calendar.GPCalendar.DayType
import biz.ganttproject.core.calendar.GPCalendarCalc.MoveDirection
import biz.ganttproject.core.time.DateFrameable
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.abs

/**
 * This class scans the calendar days until it finds a day with the specified day type.
 */
class DayTypeScan(
  private val calendar: GPCalendar,
  private val startDate: Date,
  private val lookupDayType: DayType,
  private val framer: DateFrameable,
  private val limit: Date? = null,
  private val scanDirection: MoveDirection = MoveDirection.FORWARD,
) {
  private val scanStep = if (scanDirection == MoveDirection.FORWARD) framer::adjustRight else framer::jumpLeft

  fun scan(): Date? {
    var currentDate = startDate
    while (true) {
      val nextStepStart = scanStep(currentDate)
      val nextStepMask = calendar.getDayMask(nextStepStart)
      when (lookupDayType) {
        DayType.WORKING -> {
          if ((nextStepMask and DayMask.WORKING) == DayMask.WORKING) {
            return nextStepStart
          }
        }
        DayType.HOLIDAY, DayType.NON_WORKING, DayType.WEEKEND -> {
          if ((nextStepMask and DayMask.WORKING) == 0) {
            return nextStepStart
          }
        }
      }
      if (limit != null) {
        when (scanDirection) {
          MoveDirection.FORWARD -> {
            if (nextStepStart.after(limit)) {
              return null
            }
          }
          MoveDirection.BACKWARD -> {
            if (nextStepStart.before(limit)) {
              return null
            }
          }
        }
      }
      currentDate = nextStepStart
//      if (abs(ChronoUnit.DAYS.between(currentDate.toInstant(), startDate.toInstant())) > 1000) {
//        throw RuntimeException("OVERFLOW!!!")
//      } else {
//
//      }
    }
  }
}