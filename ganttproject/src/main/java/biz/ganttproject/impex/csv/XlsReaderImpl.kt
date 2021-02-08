/*
Copyright 2017-2021 Roman Torkhov, Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.impex.csv

import com.google.common.collect.Iterables
import com.google.common.collect.Iterators
import com.google.common.collect.Lists
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * This is a SpreadsheetReader implementation which reads the input as an Excel workbook.
 *
 * @author Dmitry Barashev
 * ------
 * 2021: translated to Kotlin and significantly updated to allow for data types other than strings
 *       by Dmitry Barashev
 * 2017: initially written in Java by Roman Torkhov
 */
class XlsReaderImpl(`is`: InputStream, columnHeaders: List<String>?) : SpreadsheetReader {
  private val myBook: Workbook
  private val myHeaders: Map<String, Int>?
  init {
    myBook = HSSFWorkbook(`is`)
    myHeaders = initializeHeader(columnHeaders)
  }

  @Throws(IOException::class)
  override fun close() {
    myBook.close()
  }

  override fun iterator(): Iterator<SpreadsheetRecord> {
    val rows = myBook.getSheetAt(0).iterator().asSequence().toList()
    val result = mutableListOf<SpreadsheetRecord>()
    var lastIdx = -1
    for (row in rows) {
      if (lastIdx != -1) {
        if (row.rowNum - lastIdx > 1) {
          repeat(row.rowNum - lastIdx - 1) {
            result.add(XlsRecordImpl(emptyList()))
          }
        }
      }
      result.add(myHeaders?.let { XlsRecordImpl(Lists.newArrayList(row), it) }
        ?: XlsRecordImpl(Lists.newArrayList(row)))
      lastIdx = row.rowNum

    }
    return result.iterator()
  }

  private fun getCellValues(row: Row): List<String> {
    return Lists.newArrayList(Iterables.transform(row) { obj: Cell? -> obj!!.stringCellValue })
  }

  /**
   * This method was taken from [org.apache.commons.csv.CSVParser.initializeHeader]
   * Create the name to index mapping if the column headers not `null`.
   * @param columnHeaders column headers
   * @return the name to index mapping
   */
  private fun initializeHeader(columnHeaders: List<String>?): Map<String, Int>? {
    var hdrMap: MutableMap<String, Int>? = null
    if (columnHeaders != null) {
      hdrMap = LinkedHashMap()
      var headerRecord: List<String>? = null
      if (columnHeaders.isEmpty()) {
        // read the header from the first line of the file
        val row = myBook.getSheetAt(0).getRow(0)
        if (row != null) {
          headerRecord = getCellValues(row)
        }
      } else {
        headerRecord = columnHeaders
      }

      // build the name to index mappings
      if (headerRecord != null) {
        for (i in headerRecord.indices) {
          val header = headerRecord[i]
          require(!hdrMap.containsKey(header)) { "The header contains a duplicate name: \"$header\" in $headerRecord" }
          hdrMap[header] = i
        }
      }
    }
    return hdrMap
  }
}
