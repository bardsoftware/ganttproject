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

/**
 * Implementation of CustomPropertyDefinition
 */
internal class CustomColumn internal constructor(
  private val manager: CustomColumnsManager,
  colName: String,
  _propertyClass: CustomPropertyClass,
  colDefaultValue: Any?
) : CustomPropertyDefinition {
  override var id: String = ""

  override var name: String = ""
    set(value) {
      if (id.isNotBlank()) {
        val oldValue = this.copyOf()
        field = value
        manager.fireDefinitionChanged(CustomPropertyEvent.EVENT_NAME_CHANGE, this, oldValue)
      } else {
        field = value
      }
    }

  override var propertyClass: CustomPropertyClass = CustomPropertyClass.TEXT
    set(value) {
      if (id.isNotBlank()) {
        val oldValue = this.copyOf()
        field = value
        manager.fireDefinitionChanged(CustomPropertyEvent.EVENT_TYPE_CHANGE, this, oldValue)
      } else {
        field = value
      }
    }

  override var defaultValue: Any? = null

  override val attributes = mutableMapOf<String, String>()
  override var calculationMethod: CalculationMethod? = null
    set(value) {
      if (id.isNotBlank()) {
        val oldValue = this.copyOf()
        field = value
        manager.fireDefinitionChanged(CustomPropertyEvent.EVENT_TYPE_CHANGE, this, oldValue)
      } else {
        field = value
      }
    }

  init {
    name = colName
    defaultValue = colDefaultValue
    propertyClass = _propertyClass
  }

  override val type: Class<*>
    get() = propertyClass.javaClass

  override fun toString(): String {
    return this.name + " [" + type + "] <" + this.defaultValue + ">"
  }

  override var defaultValueAsString: String?
    get() = if (this.defaultValue == null) null else defaultValue.toString()
    set(value) {
      val stub = PropertyTypeEncoder.decodeTypeAndDefaultValue(typeAsString, value)
      defaultValue = stub.defaultValue
    }

  override val typeAsString get() = PropertyTypeEncoder.encodeFieldType(type) ?: CustomPropertyClass.TEXT.iD

  override fun hashCode(): Int {
    return id.hashCode()
  }

  override fun equals(obj: Any?): Boolean {
    if (this === obj) {
      return true
    }
    if (obj is CustomColumn == false) {
      return false
    }
    return this.id == obj.id
  }

  private fun copyOf() = CustomColumn(this.manager, this.name, this.propertyClass, this.defaultValue).also {
    it.calculationMethod = this.calculationMethod
  }
}