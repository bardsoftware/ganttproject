/*
Copyright 2014-2021 Dmitry Barashev, BarD Software s.r.o

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

import com.google.common.base.Strings
import com.google.common.collect.Sets
import net.sourceforge.ganttproject.util.collect.Pair
import java.util.logging.Level

typealias RecordProcessor = (SpreadsheetRecord, RecordGroup) -> Boolean
/**
 * Record group is a set of homogeneous CSV records. CSV file consists of a few
 * record groups separated with blank records. Each group may have its own header and
 * may have mandatory and optional fields.
 *
 * @author dbarashev (Dmitry Barashev)
 */
open class RecordGroup {
  private val myFields: Set<String>
  private val myMandatoryFields: Set<String>
  private var myCustomFields: Sets.SetView<String>? = null
  private var myHeader: SpreadsheetRecord? = null
  private val myName: String
  private var myErrorOutput: MutableList<Pair<Level, String>>? = null
  private val recordProcessor: RecordProcessor

  constructor(name: String, fields: Set<String>, customProcessor: RecordProcessor?) {
    myName = name
    myFields = fields
    myMandatoryFields = fields
    recordProcessor = customProcessor ?: {_, _ -> myHeader != null}
  }

  constructor(name: String, regularFields: Set<String>, mandatoryFields: Set<String>, customProcessor: RecordProcessor?) {
    myName = name
    myFields = regularFields
    myMandatoryFields = mandatoryFields
    recordProcessor = customProcessor ?: {_, _ -> myHeader != null}
  }

  open fun doProcess(record: SpreadsheetRecord): Boolean {
    return recordProcessor(record, this)
  }

  open fun postProcess() {}

  fun isHeader(record: SpreadsheetRecord): Boolean {
    val thoseFields: MutableSet<String?> = Sets.newHashSet()
    val it = record.iterator()
    while (it.hasNext()) {
      thoseFields.add(it.next())
    }
    return thoseFields.containsAll(myMandatoryFields)
  }

  fun hasMandatoryFields(record: SpreadsheetRecord): Boolean {
    for (s in myMandatoryFields) {
      if (!record.isSet(s!!)) {
        return false
      }
      if (Strings.isNullOrEmpty(record[s])) {
        return false
      }
    }
    return true
  }

  protected fun getOrNull(record: SpreadsheetRecord, columnName: String?): String? {
    return record[columnName!!]
  }

  open var header: SpreadsheetRecord?
    get() = myHeader
    set(header) {
      myHeader = header
      header?.let {
        myCustomFields = Sets.difference(
          header.notBlankValues().toHashSet(),
          myFields
        )
      }
    }
  val customFields: Collection<String>?
    get() = myCustomFields

  override fun toString(): String {
    return myName
  }

  fun setErrorOutput(errors: MutableList<Pair<Level, String>>?) {
    myErrorOutput = errors
  }

  protected val errorOutput: MutableList<Pair<Level, String>>?
    protected get() = myErrorOutput

  fun addError(level: Level, message: String) {
    myErrorOutput?.let { addError(it, level, message) }
  }
}

fun addError(output: MutableList<Pair<Level, String>>, level: Level, message: String) {
  output.add(Pair.create(level, message))
}
