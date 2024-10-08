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

import biz.ganttproject.createLogger

/**
 * This is an implementation of the custom property definition storage. It stores the definitions in a hash map in the memory.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class CustomColumnsManager : CustomPropertyManager {
  private val listeners = mutableListOf<CustomPropertyListener>()
  private val mapIdCustomColum = mutableMapOf<String, CustomColumn>()
  private var nextId = 0
  private var isImporting = false

  private fun addNewCustomColumn(customColumn: CustomColumn, fireChange: Boolean) {
    if (mapIdCustomColum[customColumn.id] != null) {
      throw CustomColumnsException(
        CustomColumnsException.ALREADY_EXIST,
        "Column with ID=${customColumn.id} is already registered"
      )
    }
    mapIdCustomColum[customColumn.id] = customColumn
    if (fireChange) {
      val event = CustomPropertyEvent(CustomPropertyEvent.EVENT_ADD, customColumn)
      fireCustomColumnsChange(event)
    }
  }

  override fun addListener(listener: CustomPropertyListener) {
    listeners.add(listener)
  }

  override fun removeListener(listener: CustomPropertyListener) {
    listeners.remove(listener)
  }

  override val definitions: List<CustomPropertyDefinition> get() = mapIdCustomColum.values.toList()

  override fun createDefinition(id: String, typeAsString: String, name: String, defaultValueAsString: String?): CustomPropertyDefinition {
    val stub = PropertyTypeEncoder.decodeTypeAndDefaultValue(typeAsString, defaultValueAsString)
    val result = CustomColumn(this, name, stub.propertyClass, stub.defaultValue)
    result.id = id
    addNewCustomColumn(result, true)
    return result
  }

  override fun createDefinition(propertyClass: CustomPropertyClass, colName: String, defValue: String?): CustomPropertyDefinition {
    val stub = PropertyTypeEncoder.create(propertyClass, defValue)
    val result = CustomColumn(this, colName, stub.propertyClass, stub.defaultValue)
    result.id = createId()
    addNewCustomColumn(result, true)
    return result
  }

  override fun importData(source: CustomPropertyManager): Map<CustomPropertyDefinition, CustomPropertyDefinition> =
    try {
      isImporting = true
      val result = mutableMapOf<CustomPropertyDefinition, CustomPropertyDefinition>()
      for (thatColumn in source.definitions) {
        var thisColumn = findByName(thatColumn.name)
        if (thisColumn == null || thisColumn.propertyClass != thatColumn.propertyClass) {
          thisColumn = CustomColumn(this, thatColumn.name, thatColumn.propertyClass, thatColumn.defaultValue)
          thisColumn.id = findById(thatColumn.id)?.let { createId() } ?: thatColumn.id
          thisColumn.attributes.putAll(thatColumn.attributes)
          thisColumn.calculationMethod = thatColumn.calculationMethod
          addNewCustomColumn(thisColumn, false)
        }
        result[thatColumn] = thisColumn
      }
      result
    } finally {
      isImporting = false
      val event = CustomPropertyEvent(CustomPropertyEvent.EVENT_REBUILD, null)
      fireCustomColumnsChange(event)
    }


  override fun getCustomPropertyDefinition(id: String): CustomPropertyDefinition? {
    return mapIdCustomColum[id]
  }

  override fun deleteDefinition(def: CustomPropertyDefinition) {
    val event = CustomPropertyEvent(CustomPropertyEvent.EVENT_REMOVE, def)
    mapIdCustomColum.remove(def.id)
    fireCustomColumnsChange(event)
  }

  override fun reset() {
    mapIdCustomColum.clear()
    nextId = 0
  }

  private fun fireCustomColumnsChange(event: CustomPropertyEvent) {
    if (!isImporting) {
      listeners.forEach {
        try {
          it.customPropertyChange(event)
        } catch (ex: Exception) {
          LOG.error("Failure when processing custom columns event", exception = ex)
        }
      }
    }
  }

   internal fun fireDefinitionChanged(event: Int, def: CustomColumn, oldDef: CustomColumn) {
    if (!isImporting) {
      val e = CustomPropertyEvent(event, def, oldDef)
      fireCustomColumnsChange(e)
    }
  }

  private fun findByName(name: String) = mapIdCustomColum.values.find { it.name == name }
  private fun findById(id: String) = mapIdCustomColum.values.find { it.id == id }
  private fun createId(): String {
    while (true) {
      val id = "$ID_PREFIX${nextId++}"
      if (!mapIdCustomColum.containsKey(id)) {
        return id
      }
    }
  }

}

private val LOG = createLogger("CustomColumns")
private const val ID_PREFIX = "tpc"