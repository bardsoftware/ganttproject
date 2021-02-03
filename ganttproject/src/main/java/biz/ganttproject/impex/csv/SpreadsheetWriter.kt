/*
Copyright 2017 Alexandr Kurutin, BarD Software s.r.o

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
package biz.ganttproject.impex.csv

import biz.ganttproject.core.time.GanttCalendar
import java.lang.AutoCloseable
import java.io.IOException
import java.math.BigDecimal

/**
 * @author akurutin on 04.04.2017.
 */
interface SpreadsheetWriter : AutoCloseable {
  @Throws(IOException::class)
  fun print(value: String?)

  fun print(value: Int?)
  fun print(value: Double?)
  fun print(value: BigDecimal?)
  fun print(value: GanttCalendar?)

  @Throws(IOException::class)
  fun println()
}
