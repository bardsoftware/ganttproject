/*
 * Copyright (c) 2017-2021 Roman Torkhov, Dmitry Barashev, BarD Software s.r.o.
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

package biz.ganttproject.impex.csv

import com.google.common.base.Charsets
import com.google.common.collect.Iterators
import net.sourceforge.ganttproject.CustomPropertyClass
import net.sourceforge.ganttproject.CustomPropertyDefinition
import net.sourceforge.ganttproject.CustomPropertyManager
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.language.GanttLanguage
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.util.*

/**
 * This is a SpreadsheetReader implementation which reads the input as a CSV file
 *
 * @author Dmitry Barashev
 * ------
 * 2021: translated to Kotlin and significantly updated to allow for data types other than strings
 *       by Dmitry Barashev
 * 2017: initially written in Java by Roman Torkhov
 */
class CsvReaderImpl(`is`: InputStream, format: CSVFormat) : SpreadsheetReader {
  private val myParser: CSVParser = CSVParser(InputStreamReader(`is`, Charsets.UTF_8), format)

  @Throws(IOException::class)
  override fun close() {
    myParser.close()
  }

  override fun iterator(): Iterator<SpreadsheetRecord> {
    return Iterators.transform<CSVRecord, SpreadsheetRecord>(myParser.iterator()) {
      CsvRecordImpl(it!!)
    }
  }

}

/**
 * This is an implementation of SpreadsheetRecord over a row in a CSV file.
 *
 * @author Dmitry Barashev
 * ------
 * 2021: translated to Kotlin and significantly updated to allow for data types other than strings
 *       by Dmitry Barashev
 * 2017: initially written in Java by Roman Torkhov
 */
internal class CsvRecordImpl(private val myRecord: CSVRecord) : SpreadsheetRecord {
  override fun getType(name: String) = if (isMapped(name)) CustomPropertyClass.TEXT else null
  override fun getType(idx: Int) = if (idx >= 0 && idx < myRecord.size()) CustomPropertyClass.TEXT else null

  override fun get(name: String): String? {
    return if (isMapped(name)) myRecord[name] else null
  }
  override fun get(idx: Int): String? =
    if (idx >= 0 && idx < myRecord.size()) {
      myRecord[idx]
    } else null

  override fun getDouble(name: String): Double? = get(name)?.toDoubleOrNull()
  override fun getDouble(idx: Int): Double? = get(idx)?.toDoubleOrNull()

  override fun getDate(name: String): Date? = get(name)?.let {
    GanttCSVOpen.language.parseDate(it)
  }

  override fun getDate(idx: Int): Date? = get(idx)?.let {
    GanttCSVOpen.language.parseDate(it)
  }

  override fun getInt(name: String): Int? = get(name)?.toIntOrNull()
  override fun getInt(idx: Int): Int? = get(idx)?.toIntOrNull()

  override fun getBigDecimal(name: String): BigDecimal? = get(name)?.toBigDecimalOrNull()
  override fun getBigDecimal(idx: Int): BigDecimal? = get(idx)?.toBigDecimalOrNull()

  override fun getBoolean(name: String): Boolean? = get(name)?.toBoolean()
  override fun getBoolean(idx: Int): Boolean? = get(idx)?.toBoolean()

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

typealias CustomPropertyReceiver = (CustomPropertyDefinition, String?) -> Unit
/**
 * Looks for the values of the specified custom properties in the current record,
 * adds missing custom property definitions and calls the receiver with the custom property
 * definition and value.
 */
fun readCustomProperties(
  headerRecord: SpreadsheetRecord,
  customFields: Iterable<String>,
  record: SpreadsheetRecord,
  customPropertyMgr: CustomPropertyManager,
  receiver: CustomPropertyReceiver) {
  headerRecord.iterator().withIndex().forEach { header ->
    header.value?.let { fieldName ->
      if (fieldName in customFields) {
        val def = customPropertyMgr.let { mgr ->
          mgr.getCustomPropertyDefinition(fieldName)
            ?: record.getType(header.index)?.let { type ->
              mgr.createDefinition(fieldName, type.id, fieldName, null)
            }
        }
        if (def == null) {
          GPLogger.logToLogger("Can't find custom field with name=$fieldName value=${record[fieldName]}")
          return@let
        }

        when (def.propertyClass) {
          CustomPropertyClass.TEXT -> record.get(header.index)?.let { receiver(def, it) }
          CustomPropertyClass.INTEGER -> record.getInt(header.index)?.let {
            receiver(def, it.toString())
          }
          CustomPropertyClass.DOUBLE -> record.getDouble(header.index)?.let {
            receiver(def, it.toString())
          }
          CustomPropertyClass.DATE -> record.getDate(header.index)?.let {
            receiver(def, GanttLanguage.getInstance().formatShortDate(it))
          }
          CustomPropertyClass.BOOLEAN -> record.getBoolean(header.index)?.let {
            receiver(def, it.toString())
          }
        }
      }
    }
  }
}
