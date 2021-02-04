/*
Copyright 2021 Dmitry Barashev, BarD Software s.r.o

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

import biz.ganttproject.core.time.GanttCalendar
import com.google.common.base.Strings
import net.sourceforge.ganttproject.CustomProperty
import net.sourceforge.ganttproject.CustomPropertyClass
import net.sourceforge.ganttproject.CustomPropertyDefinition
import java.io.IOException

@Throws(IOException::class)
fun writeCustomPropertyValues(
    writer: SpreadsheetWriter,
    defs: List<CustomPropertyDefinition>,
    values: List<CustomProperty>) {
  val definedProps = values.map { it.definition.id to it}.toMap()
  for (def in defs) {
    val value = definedProps[def.id]
    if (value == null) {
      writer.print(null as String?)
    } else {
      when (value.definition.propertyClass) {
        CustomPropertyClass.TEXT -> {
          writer.print(Strings.nullToEmpty(value.valueAsString))
        }
        CustomPropertyClass.DOUBLE -> {
          writer.print(value.valueAsString.toDoubleOrNull())
        }
        CustomPropertyClass.INTEGER -> {
          writer.print(value.valueAsString.toIntOrNull())
        }
        CustomPropertyClass.DATE -> {
          writer.print(value.value as GanttCalendar)
        }
        CustomPropertyClass.BOOLEAN -> {
          writer.print(value.valueAsString.toBoolean())
        }
      }
    }
  }
  writer.println()
}
