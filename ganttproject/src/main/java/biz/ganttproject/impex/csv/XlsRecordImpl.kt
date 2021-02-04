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
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import java.lang.IllegalArgumentException
import java.math.BigDecimal
import java.util.*

/**
 * @author torkhov
 */
internal class XlsRecordImpl(
    private val myValues: List<Cell>,
    private val myMapping: Map<String, Int> = mapOf()) : SpreadsheetRecord {

  override fun getType(name: String) = if (isMapped(name)) {
    myValues[idx(name)]?.let {
      when (it.cellType) {
        CellType.STRING -> CustomPropertyClass.TEXT
        CellType.NUMERIC -> {
          if (it.cellStyle.dataFormat == it.sheet.workbook.creationHelper.createDataFormat().getFormat("m/d/yy")) CustomPropertyClass.DATE
          else CustomPropertyClass.DOUBLE
        }
        CellType.BOOLEAN -> CustomPropertyClass.BOOLEAN
        else -> null
      }
    }
  } else null

  override fun get(name: String): String? =
    if (isMapped(name)) {
      myValues[idx(name)]?.let { cell ->
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
      myValues[idx(name)]?.let {
        when (it.cellType) {
          CellType.STRING -> it.stringCellValue.toDoubleOrNull()
          CellType.NUMERIC -> it.numericCellValue
          else -> null
        }
      }
    } else null

  override fun getDate(name: String): Date? =
    if (isMapped(name)) {
      myValues[idx(name)]?.let {
        when (it.cellType) {
          CellType.STRING -> GanttCSVOpen.language.parseDate(it.stringCellValue)
          CellType.NUMERIC -> it.dateCellValue
          else -> null
        }
      }
    } else null

  override fun getInt(name: String): Int? =
    if (isMapped(name)) {
      myValues[idx(name)]?.let {
        when (it.cellType) {
          CellType.STRING -> it.stringCellValue.toIntOrNull()
          CellType.NUMERIC -> it.numericCellValue.toInt()
          else -> null
        }
      }
    } else null

  override fun getBigDecimal(name: String): BigDecimal? =
    if (isMapped(name)) {
      myValues[idx(name)]?.let {
        when (it.cellType) {
          CellType.STRING -> it.stringCellValue.toBigDecimalOrNull()
          CellType.NUMERIC -> it.numericCellValue.toBigDecimal()
          else -> null
        }
      }
    } else null

  override fun getBoolean(name: String): Boolean? =
    if (isMapped(name)) {
      myValues[idx(name)]?.let {
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
    return myMapping != null && myMapping.containsKey(name)
  }

  override fun isSet(name: String): Boolean {
    return isMapped(name) && myMapping!![name]!! >= 0 && myMapping[name]!! < myValues.size
  }

  override fun iterator(): Iterator<String?> {
    return myValues.map { it.stringCellValue }.iterator()
  }

  override fun size(): Int {
    return myValues.size
  }
}

