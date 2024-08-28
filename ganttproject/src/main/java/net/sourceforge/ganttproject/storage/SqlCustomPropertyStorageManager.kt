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
package net.sourceforge.ganttproject.storage

import biz.ganttproject.customproperty.*
import javax.sql.DataSource

/**
 * Responsible for the storage of custom properties in the database tables.
 * Creates and updates table columns appropriately.
 *
 * Custom properties are ordered so that "stored" properties go first and calculated properties go last.
 * This allows for creating and dropping them without errors.
 */
class SqlCustomPropertyStorageManager(private val dataSource: DataSource) {
  // This set caches the SQL statements that build custom columns in the task table. Should anything change in the
  // custom columns, we will rebuild this set and re-construct the table columns if there are any changes.
  private val customColumnStatements = mutableSetOf<String>()
  // This list keeps DROP statements that need to be executed prior to creating new columns.
  private val dropStatements = mutableListOf<String>()

  /**
   * This function must be called whenever something changes in the custom property definitions,
   * e.g. a new one is created or deleted, or the existing one changes.
   */
  fun onCustomColumnChange(customPropertyManager: CustomPropertyManager) {
    val newStatements = createCustomColumnStatements(customPropertyManager)
    synchronized(customColumnStatements) {
      if (customColumnStatements != newStatements.toSet()) {
        runStatements(dataSource, dropStatements)
        runStatements(dataSource, newStatements)

        customColumnStatements.clear()
        customColumnStatements.addAll(newStatements)
        dropStatements.clear()
        dropStatements.addAll(customPropertyManager.orderedDefinitions().asReversed().map {
          "ALTER TABLE Task DROP COLUMN ${it.id}"
        })
      }
    }
  }
}

fun rebuildTaskDataTable(dataSource: DataSource, customPropertyManager: CustomPropertyManager) {
  runStatements(dataSource, createCustomColumnStatements(customPropertyManager))
}

fun createCustomColumnStatements(customPropertyManager: CustomPropertyManager): List<String> {
  return customPropertyManager.orderedDefinitions().map { def ->
    def.calculationMethod?.let {
      when (it) {
        is SimpleSelect -> "ALTER TABLE Task ADD COLUMN ${def.id} ${def.propertyClass.asSqlType()} GENERATED ALWAYS AS (${it.selectExpression})"
        else -> null
      }
    } ?: run {
      "ALTER TABLE Task ADD COLUMN ${def.id} ${def.propertyClass.asSqlType()}"
    }
  }.toList()
}

fun createUpdateCustomValuesStatement(taskUid: String, customPropertyManager: CustomPropertyManager, customPropertyHolder: CustomPropertyHolder): String {
  return customPropertyManager.definitions.mapNotNull { def ->
    if (def.calculationMethod == null) {
      val value = customPropertyHolder.customProperties.find { it.definition.id == def.id }?.value
      "${def.id}=${generateSqlValueLiteral(def, value)}"
    } else null
  }.joinToString(separator = ",", prefix = "UPDATE Task SET ", postfix = " WHERE uid='$taskUid';")
}

fun generateSqlValueLiteral(def: CustomPropertyDefinition, value: Any?): String =
  value?.let {
    when (def.propertyClass) {
      CustomPropertyClass.TEXT -> "'${value}'"
      else -> "$value"
    }
  } ?: "NULL"

// Orders the definitions so that all stored precede all calculated properties.
private fun CustomPropertyManager.orderedDefinitions() = this.definitions.sortedWith { o1, o2 ->
  var result: Int = o1.isCalculated().compareTo(o2.isCalculated())
  if (result == 0) {
    result = o1.id.compareTo(o2.id)
  }
  result
}

private fun CustomPropertyDefinition.isCalculated() = this.calculationMethod != null

private fun CustomPropertyClass.asSqlType() = when (this) {
  CustomPropertyClass.TEXT -> "varchar"
  CustomPropertyClass.INTEGER -> "integer"
  CustomPropertyClass.DATE -> "date"
  CustomPropertyClass.BOOLEAN -> "boolean"
  CustomPropertyClass.DOUBLE -> "numeric"
}