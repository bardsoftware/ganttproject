/*
Copyright 2021 BarD Software s.r.o

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
package biz.ganttproject.customproperty

import biz.ganttproject.core.time.CalendarFactory
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.util.StringUtils
import org.w3c.util.DateParser
import org.w3c.util.InvalidDateException
import java.util.*

/**
 * @author dbarashev@bardsoftware.com
 */
object PropertyTypeEncoder {
  fun encodeFieldType(fieldType: Class<*>): String? {
    var result: String? = null
    if (fieldType == java.lang.String::class.java) {
      result = "text"
    } else if (fieldType == java.lang.Boolean::class.java) {
      result = "boolean"
    } else if (fieldType == java.lang.Integer::class.java) {
      result = "int"
    } else if (fieldType == java.lang.Double::class.java) {
      result = "double"
    } else if (GregorianCalendar::class.java.isAssignableFrom(fieldType)) {
      result = "date"
    }
    return result
  }

  fun decodeTypeAndDefaultValue(
    typeAsString: String?, valueAsString: String?
  ): CustomPropertyDefinition {
    return when (typeAsString) {
      "text" -> create(CustomPropertyClass.TEXT, valueAsString)
      "boolean" -> create(CustomPropertyClass.BOOLEAN, valueAsString)
      "int" -> create(CustomPropertyClass.INTEGER, valueAsString)
      "integer" -> create(CustomPropertyClass.INTEGER, valueAsString)
      "double" -> create(CustomPropertyClass.DOUBLE, valueAsString)
      "date" -> create(CustomPropertyClass.DATE, valueAsString)
      else -> create(CustomPropertyClass.TEXT, "")
    }
  }

  fun create(propertyClass: CustomPropertyClass, valueAsString: String?): CustomPropertyDefinition {
    val defaultValue = when (propertyClass) {
      CustomPropertyClass.TEXT -> valueAsString
      CustomPropertyClass.BOOLEAN -> if (valueAsString == null) null else java.lang.Boolean.valueOf(valueAsString)
      CustomPropertyClass.INTEGER -> {
        try {
          if (valueAsString == null) null else Integer.valueOf(valueAsString)
        } catch (e: NumberFormatException) {
          null
        }
      }
      CustomPropertyClass.DOUBLE -> {
        try {
          if (valueAsString == null) null else java.lang.Double.valueOf(valueAsString)
        } catch (e: NumberFormatException) {
          null
        }
      }
      CustomPropertyClass.DATE -> {
        if (StringUtils.isEmptyOrNull(valueAsString)) {
          null
        } else {
          try {
            DateParser.parse(valueAsString)
          } catch (e: InvalidDateException) {
            GanttLanguage.getInstance().parseDate(valueAsString)
          }?.let { CalendarFactory.createGanttCalendar(it) }
        }
      }
    }
    return object : CustomPropertyDefinition {
      override val propertyClass: CustomPropertyClass = propertyClass
      override fun setPropertyClass(propertyClass: CustomPropertyClass) {
        error("Don't set me")
      }
      override val type: Class<*> = propertyClass.javaClass
      override val typeAsString: String = propertyClass.id
      override val id = ""
      override val defaultValue = defaultValue
      override var name: String
        get() = ""
        set(_) = error("Don't set me")
      override var defaultValueAsString
        get() = valueAsString
        set(_) = error("Don't set me")
      override val attributes: Map<String, String> = emptyMap()
      override var calculationMethod: CalculationMethod? = null
    }
  }
}
