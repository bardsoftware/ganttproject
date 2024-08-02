/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
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
package biz.ganttproject.customproperty

import java.util.*

interface CustomProperty {
  val definition: CustomPropertyDefinition
  val value: Any?
  val valueAsString: String
}

interface CustomPropertyDefinition {
  val propertyClass: CustomPropertyClass
  fun setPropertyClass(propertyClass: CustomPropertyClass)
  val type: Class<*>
  val typeAsString: String
  val id: String
  val defaultValue: Any?
  var name: String
  var defaultValueAsString: String?
  val attributes: MutableMap<String, String>
  var calculationMethod: CalculationMethod?
}

interface CalculationMethod {
  val propertyId: String
  val resultClass: Class<*>
}

enum class CustomPropertyClass(val iD: String, private val myDefaultValue: String?, val javaClass: Class<*>) {
  TEXT("text", "", String::class.java),
  INTEGER("integer", "0", Int::class.java),
  DOUBLE("double", "0.0", Double::class.java),
  DATE("date", null, GregorianCalendar::class.java),
  BOOLEAN("boolean", "false", Boolean::class.java);

  override fun toString(): String {
    return iD
  }

  val defaultValueAsString: String?
    get() = null

  val isNumeric: Boolean
    get() = this == INTEGER || this == DOUBLE

  companion object {
    fun fromJavaClass(javaClass: Class<*>): CustomPropertyClass? {
      for (klass in entries) {
        if (klass.javaClass == javaClass) {
          return klass
        }
      }
      return null
    }
  }
}

