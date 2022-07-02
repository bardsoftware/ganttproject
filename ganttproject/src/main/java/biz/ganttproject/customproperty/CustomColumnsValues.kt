/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package biz.ganttproject.customproperty

import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.customproperty.PropertyTypeEncoder.decodeTypeAndDefaultValue
import net.sourceforge.ganttproject.language.GanttLanguage

/**
 * Keeps a map of custom property ID to value.
 *
 * @author bbaranne Mar 2, 2005 -- initial code
 * @auuthor dbarashev (Dmitry Barashev) -- complete rewrite
 */
class CustomColumnsValues(private val customPropertyManager: CustomPropertyManager,
                          private val eventDispatcher: (CustomPropertyValueEventStub)->Unit)
  : CustomPropertyHolder, Cloneable {
  /**
   * CustomColumnName(String) -> Value (Object)
   */
  private val mapCustomColumnValue: MutableMap<String, Any> = HashMap()
  @Throws(CustomColumnsException::class)
  override fun setValue(def: CustomPropertyDefinition, value: Any?) {
    if (value == null) {
      mapCustomColumnValue.remove(def.id)
      return
    }
    val c1 = def.type
    val c2: Class<*> = value.javaClass
    if (!c1.isAssignableFrom(c2)) {
      throw CustomColumnsException(CustomColumnsException.CLASS_MISMATCH, "Failed to set value=" + value
          + ". value class=" + c2 + ", column class=" + c1)
    }
    mapCustomColumnValue[def.id] = value
    eventDispatcher(CustomPropertyValueEventStub((def)))
  }

  fun getValue(def: CustomPropertyDefinition): Any? {
    val result = mapCustomColumnValue[def.id]
    return result ?: def.defaultValue
  }

  fun hasOwnValue(def: CustomPropertyDefinition): Boolean {
    return mapCustomColumnValue.containsKey(def.id)
  }

  fun removeCustomColumn(definition: CustomPropertyDefinition) {
    mapCustomColumnValue.remove(definition.id)
  }

  fun copyOf(): CustomColumnsValues {
    val res = CustomColumnsValues(customPropertyManager, eventDispatcher)
    res.mapCustomColumnValue.putAll(mapCustomColumnValue)
    return res
  }

  @Throws(CustomColumnsException::class)
  fun importFrom(value: CustomPropertyHolder) {
    mapCustomColumnValue.clear()
    for (prop in value.customProperties) {
      setValue(prop.definition, prop.value)
    }
  }

  override fun toString(): String {
    return mapCustomColumnValue.toString()
  }

  override fun getCustomProperties(): List<CustomProperty> {
    val result: MutableList<CustomProperty> = ArrayList(mapCustomColumnValue.size)
    for ((id, value) in mapCustomColumnValue) {
      val def = getCustomPropertyDefinition(customPropertyManager, id)
      if (def != null) {
        result.add(CustomPropertyImpl(def, value))
      }
    }
    return result
  }

  override fun addCustomProperty(definition: CustomPropertyDefinition, valueAsString: String?): CustomProperty {
    val defStub = decodeTypeAndDefaultValue(
        definition.typeAsString, valueAsString)
    try {
      setValue(definition, defStub.defaultValue!!)
    } catch (e: CustomColumnsException) {
      // TODO Auto-generated catch block
      e.printStackTrace()
    }
    return CustomPropertyImpl(definition, defStub.defaultValue!!)
  }

  private class CustomPropertyImpl(private val myDefinition: CustomPropertyDefinition, private val myValue: Any) : CustomProperty {
    override fun getDefinition(): CustomPropertyDefinition {
      return myDefinition
    }

    override fun getValue(): Any {
      return myValue
    }

    override fun getValueAsString(): String {
      return getValueAsString(myValue)!!
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is CustomColumnsValues) return false
    return mapCustomColumnValue == other.mapCustomColumnValue
  }

  override fun hashCode(): Int {
    return mapCustomColumnValue.hashCode()
  }

  companion object {
    private fun getCustomPropertyDefinition(manager: CustomPropertyManager, id: String): CustomPropertyDefinition? {
      return manager.getCustomPropertyDefinition(id)
    }

    fun getValueAsString(value: Any?): String? {
      var result: String? = null
      if (value != null) {
        result = if (value is GanttCalendar) {
          GanttLanguage.getInstance().formatShortDate(value as GanttCalendar?)
        } else {
          value.toString()
        }
      }
      return result
    }
  }
}