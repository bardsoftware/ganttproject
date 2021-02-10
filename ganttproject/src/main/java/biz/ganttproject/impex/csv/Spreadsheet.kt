/*
 * Copyright (c) 2017-2021 Alexander Kurutin, Roman Torkhov, Dmitry Barashev,
 *                         BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
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

/**
 * Change history:
 * 2021: translated to Kotlin and significantly updated to allow for data types other than strings
 *       by Dmitry Barashev
 * 2017: initially written in Java by Roman Torkhov and Alexander Kurutin
 */
package biz.ganttproject.impex.csv

import biz.ganttproject.core.time.GanttCalendar
import com.google.common.base.Preconditions
import net.sourceforge.ganttproject.CustomPropertyClass
import net.sourceforge.ganttproject.language.GanttLanguage
import java.io.Closeable
import java.io.IOException
import java.math.BigDecimal
import java.util.*
import java.util.logging.Level

/**
 * Interface of a record which maps to a row on a sheet in
 * a spreadsheet.
 */
interface SpreadsheetRecord {
  fun getType(name: String): CustomPropertyClass?
  fun getType(idx: Int): CustomPropertyClass?
  operator fun get(name: String): String?
  operator fun get(idx: Int): String?

  fun getDouble(name: String): Double?
  fun getDouble(idx: Int): Double?

  fun getDate(name: String): Date?
  fun getDate(idx: Int): Date?

  fun getInt(name: String): Int?
  fun getInt(idx: Int): Int?

  fun getBigDecimal(name: String): BigDecimal?
  fun getBigDecimal(idx: Int): BigDecimal?

  fun getBoolean(name: String): Boolean?
  fun getBoolean(idx: Int): Boolean?

  fun isEmpty(): Boolean
  fun isMapped(name: String): Boolean
  fun isSet(name: String): Boolean
  operator fun iterator(): Iterator<String?>
  fun size(): Int

  fun notBlankValues(): List<String> =
    iterator().asSequence().filterNotNull().filter { it.isNotBlank() }.toList()

}

/**
 * Interface of a stream of spreadsheet records.
 */
interface SpreadsheetReader : Closeable {
  operator fun iterator(): Iterator<SpreadsheetRecord>
}

/**
 * Interface of writer object which fills a spreadsheet with the passed values.
 * It maintains a current cell which is defined by row and column number.
 * Every call of print methods fills the current cell with the passed value and increments the column number
 * Every call of println increments the row number and resets the column number to the initial value
 */
interface SpreadsheetWriter : AutoCloseable {
  @Throws(IOException::class)
  fun print(value: String?)

  fun print(value: Int?)
  fun print(value: Double?)
  fun print(value: BigDecimal?)
  fun print(value: GanttCalendar?)
  fun print(value: Boolean?)

  @Throws(IOException::class)
  fun println()
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

enum class SpreadsheetFormat(val extension: String) {
  CSV("csv"), XLS("xls");

  override fun toString(): String {
    return "impex.csv.fileformat." + name.toLowerCase()
  }

  companion object {
    fun getSpreadsheetFormat(extension: String): SpreadsheetFormat {
      var extension = extension
      extension = Preconditions.checkNotNull(extension)
      for (format in values()) {
        if (format.extension.equals(extension, ignoreCase = true)) {
          return format
        }
      }
      throw IllegalArgumentException("No enum constant extension: $extension")
    }
  }
}
