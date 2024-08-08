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
  var name: String
  var propertyClass: CustomPropertyClass
  val type: Class<*>
  val typeAsString: String
  val id: String
  val defaultValue: Any?
  var defaultValueAsString: String?
  val attributes: MutableMap<String, String>
  var calculationMethod: CalculationMethod?
}

interface CalculationMethod {
  val propertyId: String
  val resultClass: Class<*>
}

enum class CustomPropertyClass(val iD: String, private val myDefaultValue: String?, val javaClass: Class<*>) {
  TEXT("text", "", java.lang.String::class.java),
  INTEGER("integer", "0", java.lang.Integer::class.java),
  DOUBLE("double", "0.0", java.lang.Double::class.java),
  DATE("date", null, GregorianCalendar::class.java),
  BOOLEAN("boolean", "false", java.lang.Boolean::class.java);

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

class CustomPropertyEvent {
  val type: Int
  var definition: CustomPropertyDefinition
    private set
  var oldValue: CustomPropertyDefinition? = null
    private set

  constructor(type: Int, definition: CustomPropertyDefinition) {
    this.type = type
    this.definition = definition
  }

  constructor(type: Int, def: CustomPropertyDefinition, oldDef: CustomPropertyDefinition?) {
    this.type = type
    definition = def
    oldValue = oldDef
  }

  companion object {
    const val EVENT_ADD = 0
    const val EVENT_REMOVE = 1
    const val EVENT_REBUILD = 2
    const val EVENT_NAME_CHANGE = 3
    const val EVENT_TYPE_CHANGE = 4
  }
}

sealed class CustomPropertyValueEvent(val def: CustomPropertyDefinition)
class CustomPropertyValueEventStub(def: CustomPropertyDefinition): CustomPropertyValueEvent(def)

interface CustomPropertyListener {
  fun customPropertyChange(event: CustomPropertyEvent)
}

interface CustomPropertyManager {
  val definitions: List<CustomPropertyDefinition>

  fun createDefinition(id: String, typeAsString: String, name: String, defaultValueAsString: String?): CustomPropertyDefinition

  fun createDefinition(typeAsString: String, colName: String, defValue: String?): CustomPropertyDefinition
  fun createDefinition(propertyClass: CustomPropertyClass, colName: String, defValue: String?): CustomPropertyDefinition

  fun getCustomPropertyDefinition(id: String): CustomPropertyDefinition?

  fun deleteDefinition(def: CustomPropertyDefinition)

  fun importData(source: CustomPropertyManager): Map<CustomPropertyDefinition, CustomPropertyDefinition>

  fun addListener(listener: CustomPropertyListener)

  fun reset()
}


