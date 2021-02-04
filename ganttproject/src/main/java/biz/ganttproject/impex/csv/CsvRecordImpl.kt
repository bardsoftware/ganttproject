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

import biz.ganttproject.core.time.GanttCalendar
import org.apache.commons.csv.CSVRecord
import biz.ganttproject.impex.csv.SpreadsheetRecord
import net.sourceforge.ganttproject.CustomPropertyClass
import java.math.BigDecimal
import java.util.*

/**
 * @author torkhov
 */
internal class CsvRecordImpl(private val myRecord: CSVRecord) : SpreadsheetRecord {
  override fun getType(name: String) = if (isMapped(name)) CustomPropertyClass.TEXT else null

  override fun get(name: String): String? {
    return if (isMapped(name)) myRecord[name] else null
  }

  override fun getDouble(name: String): Double? = get(name)?.toDoubleOrNull()

  override fun getDate(name: String): Date? = get(name)?.let {
    GanttCSVOpen.language.parseDate(it)
  }
  override fun getInt(name: String): Int? = get(name)?.toIntOrNull()
  override fun getBigDecimal(name: String): BigDecimal? = get(name)?.toBigDecimalOrNull()
  override fun getBoolean(name: String): Boolean? = get(name)?.toBoolean()

  override fun isEmpty(): Boolean = myRecord.all { it.isBlank() }

  override fun isMapped(name: String): Boolean {
    return myRecord.isMapped(name)
  }

  override fun isSet(name: String): Boolean {
    return myRecord.isSet(name)
  }

  override fun iterator(): Iterator<String?> {
    return myRecord.iterator()
  }

  override fun size(): Int {
    return myRecord.size()
  }
}
