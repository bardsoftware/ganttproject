/*
Copyright 2017 Roman Torkhov, BarD Software s.r.o

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

import net.sourceforge.ganttproject.CustomPropertyClass
import net.sourceforge.ganttproject.language.GanttLanguage
import java.math.BigDecimal
import java.util.*
import java.util.logging.Level

/**
 * @author torkhov
 */
interface SpreadsheetRecord {
  fun getType(name: String): CustomPropertyClass?
  operator fun get(name: String): String?
  fun getDouble(name: String): Double?
  fun getDate(name: String): Date?
  fun getInt(name: String): Int?
  fun getBigDecimal(name: String): BigDecimal?
  fun getBoolean(name: String): Boolean?

  fun isEmpty(): Boolean
  fun isMapped(name: String): Boolean
  fun isSet(name: String): Boolean
  operator fun iterator(): Iterator<String?>
  fun size(): Int
}

internal fun parseDateOrError(strDate: String?, addError: (Level, String) -> Any): Date? {
  val result = GanttCSVOpen.language.parseDate(strDate)
  if (result == null) {
    addError(
      Level.WARNING, GanttLanguage.getInstance().formatText(
        "impex.csv.error.parse_date",
        strDate,
        GanttLanguage.getInstance().shortDateFormat.toPattern(),
        GanttLanguage.getInstance().shortDateFormat.format(Date())
      )
    )
  }
  return result
}
