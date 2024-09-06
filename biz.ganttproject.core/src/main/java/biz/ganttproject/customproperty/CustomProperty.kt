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

/**
 * A value of a particular custom property in a task instance.
 */
interface CustomProperty {
  val definition: CustomPropertyDefinition
  val value: Any?
  val valueAsString: String
}

/**
 * Definition of a custom property, that is, metadata with its type, default value, calculation expression, etc.
 */
interface CustomPropertyDefinition {
  /**
   * Custom property name.
   */
  var name: String

  /**
   * Custom property class, in GanttProject terms. This class is shown to the user in the custom property edit dialog.
   */
  var propertyClass: CustomPropertyClass

  /**
   * Java type of this custom property. Used internally, in particular for the serialization and database purposes.
   */
  val type: Class<*>

  /**
   * This property class represented as string. Used internally for serialization purposes.
   */
  val typeAsString: String

  /**
   * Property identifier, normally tpc<Num>
   */
  val id: String

  /**
   * Default value for this property instances.
   */
  val defaultValue: Any?

  /**
   * String representation of the default value, used for the serialization purposes.
   */
  var defaultValueAsString: String?

  /**
   * A storage for the problem-specific usages of this property definition. Used e.g. in MS Project export-import
   * to keep the name of MS Project property corresponding to this one.
   */
  val attributes: MutableMap<String, String>

  /**
   * If this property is calculated, returns a calculation method.
   * Returns null is this property is stored.
   */
  var calculationMethod: CalculationMethod?

  fun isCalculated(): Boolean  = this.calculationMethod != null
}

interface CalculationMethod {
  val propertyId: String
  val resultClass: Class<*>
}

/**
 * Enumeration of the supported custom property classes.
 */
enum class CustomPropertyClass(val iD: String, val javaClass: Class<*>) {
  TEXT("text", java.lang.String::class.java),
  INTEGER("integer", java.lang.Integer::class.java),
  DOUBLE("double", java.lang.Double::class.java),
  DATE("date", GregorianCalendar::class.java),
  BOOLEAN("boolean", java.lang.Boolean::class.java);

  override fun toString(): String {
    return iD
  }

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

/**
 * Manager of the custom properties. There is one instance per task manager and one instance per resource manager.
 *
 */
interface CustomPropertyManager {
  /**
   * A list of all available custom property definitions.
   */
  val definitions: List<CustomPropertyDefinition>

  /**
   * Creates definition from the data stored in a project file.
   */
  fun createDefinition(id: String, typeAsString: String, name: String, defaultValueAsString: String? = null): CustomPropertyDefinition

  /**
   * Creates a new definition from the user input.
   */
  fun createDefinition(propertyClass: CustomPropertyClass, colName: String, defValue: String? = null): CustomPropertyDefinition

  /**
   * Get a definition by its id.
   */
  fun getCustomPropertyDefinition(id: String): CustomPropertyDefinition?

  /**
   * Deletes the definition "def".
   */
  fun deleteDefinition(def: CustomPropertyDefinition)

  /**
   * Imports data from another custom property manager  and returns a mapping of "those" definitions
   * (available in the source manager) to "these" (created in this manager).
   */
  fun importData(source: CustomPropertyManager): Map<CustomPropertyDefinition, CustomPropertyDefinition>

  /**
   * Adds the listener on custom property events.
   */
  fun addListener(listener: CustomPropertyListener)

  /**
   * Removes the listener.
   */
  fun removeListener(listener: CustomPropertyListener)

  /**
   * Resets this instances, removes all definitions and resets the id counter if any.
   */
  fun reset()
}


