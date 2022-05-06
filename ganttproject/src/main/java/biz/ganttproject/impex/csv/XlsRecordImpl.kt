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

import biz.ganttproject.customproperty.CustomPropertyClass
import net.sourceforge.ganttproject.language.GanttLanguage
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.util.*

/**
 * This is an implementation of SpreadsheetRecord over a row in Excel workbook
 *
 * @author Dmitry Barashev
 * ------
 * 2021: translated to Kotlin and significantly updated to allow for data types other than strings
 *       by Dmitry Barashev
 * 2017: initially written in Java by Roman Torkhov
 */
internal class XlsRecordImpl(
    private val myValues: List<Cell>,
    private val myMapping: Map<String, Int> = mapOf()) : SpreadsheetRecord {

  override fun getType(name: String) = if (isMapped(name)) getType(idx(name)) else null

  override fun getType(idx: Int) = if (idx >= 0 && idx < myValues.size) {
    myValues[idx]?.let {
      when (it.cellType) {
        CellType.STRING -> CustomPropertyClass.TEXT
        CellType.NUMERIC -> {
          if (DateUtil.isCellDateFormatted(it)) CustomPropertyClass.DATE
          else CustomPropertyClass.DOUBLE
        }
        CellType.BOOLEAN -> CustomPropertyClass.BOOLEAN
        else -> null
      }
    }
  } else null

  override fun get(name: String): String? =
    if (isMapped(name)) {
      get(idx(name))
    } else null

  override fun get(idx: Int): String? =
    if (idx >= 0 && idx < myValues.size) {
      myValues[idx]?.let { cell ->
        when (cell.cellType) {
          CellType.STRING -> cell.stringCellValue
          CellType.NUMERIC -> {
            if (cell.cellStyle.dataFormat == cell.sheet.workbook.creationHelper.createDataFormat().getFormat("m/d/yy")) cell.dateCellValue.let {
              GanttLanguage.getInstance().shortDateFormat.format(it)
            }
            else cell.numericCellValue.toString()
          }
          CellType.BOOLEAN -> cell.booleanCellValue.toString()
          else -> null
        }
      }
    } else null

  private fun idx(name: String) =
    myMapping[name] ?: throw IllegalArgumentException(
      "Mapping for $name not found, expected one of ${myMapping.keys}"
    )

  override fun getDouble(name: String): Double? =
    if (isMapped(name)) {
      getDouble(idx(name))
    } else null

  override fun getDouble(idx: Int): Double? = if (idx >= 0 && idx < myValues.size) {
    myValues[idx]?.let {
      when (it.cellType) {
        CellType.STRING -> it.stringCellValue.toDoubleOrNull()
        CellType.NUMERIC -> it.numericCellValue
        else -> null
      }
    }
  } else null

  override fun getDate(name: String): Date? =
    if (isMapped(name)) {
      getDate(idx(name))
    } else null

  override fun getDate(idx: Int): Date? = if (idx >= 0 && idx < myValues.size) {
    myValues[idx]?.let {
      when (it.cellType) {
        CellType.STRING -> GanttCSVOpen.language.parseDate(it.stringCellValue)
        CellType.NUMERIC -> it.dateCellValue
        else -> null
      }
    }
  } else null

  override fun getInt(name: String): Int? =
    if (isMapped(name)) {
      getInt(idx(name))
    } else null

  override fun getInt(idx: Int): Int? = if (idx >= 0 && idx < myValues.size) {
    myValues[idx]?.let {
      when (it.cellType) {
        CellType.STRING -> it.stringCellValue.toIntOrNull()
        CellType.NUMERIC -> it.numericCellValue.toInt()
        else -> null
      }
    }
  } else null

  override fun getBigDecimal(name: String): BigDecimal? =
    if (isMapped(name)) {
      getBigDecimal(idx(name))
    } else null

  override fun getBigDecimal(idx: Int): BigDecimal? = if (idx >= 0 && idx < myValues.size) {
    myValues[idx]?.let {
      when (it.cellType) {
        CellType.STRING -> it.stringCellValue.toBigDecimalOrNull()
        CellType.NUMERIC -> it.numericCellValue.toBigDecimal()
        else -> null
      }
    }
  } else null

  override fun getBoolean(name: String): Boolean? =
    if (isMapped(name)) {
      getBoolean(idx(name))
    } else null

  override fun getBoolean(idx: Int): Boolean? = if (idx >= 0 && idx < myValues.size) {
    myValues[idx]?.let {
      when (it.cellType) {
        CellType.STRING -> it.stringCellValue.toBoolean()
        CellType.BOOLEAN -> it.booleanCellValue
        else -> null
      }
    }
  } else null

  override fun isEmpty(): Boolean = myValues.all {
    it.cellType == CellType.BLANK || it.cellType == CellType.STRING && it.stringCellValue.isBlank()
  }


  override fun isMapped(name: String): Boolean {
    return myMapping.containsKey(name)
  }

  override fun isSet(name: String): Boolean {
    return isMapped(name) && myMapping[name]!! >= 0 && myMapping[name]!! < myValues.size
  }

  override fun iterator(): Iterator<String?> {
    return myValues.map { it.stringCellValue }.iterator()
  }

  override fun size(): Int {
    return myValues.size
  }
}

