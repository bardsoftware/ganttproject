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

fun rebuildTaskDataTable(dataSource: DataSource, customPropertyManager: CustomPropertyManager) {
  runStatements(dataSource, createCustomColumnStatements(customPropertyManager))
}

fun runStatements(dataSource: DataSource, statements: List<String>) {
  val sqlScript = """
    ${statements.joinToString(separator = ";\n")};
  """.trimIndent()
  println("Running \n $sqlScript")
  dataSource.connection.use {
    it.prepareStatement(sqlScript).use { stmt ->
      stmt.execute()
    }
    it.commit()
  }
}

fun createCustomColumnStatements(customPropertyManager: CustomPropertyManager): List<String> =
  customPropertyManager.definitions.map { def ->
    def.calculationMethod?.let {
      when (it) {
        is SimpleSelect -> "ALTER TABLE Task ADD COLUMN ${def.id} ${def.propertyClass.asSqlType()} GENERATED ALWAYS AS (${it.selectExpression})"
        else -> null
      }
    } ?: run {
      "ALTER TABLE Task ADD COLUMN ${def.id} ${def.propertyClass.asSqlType()}"
    }
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

fun CustomPropertyClass.asSqlType() = when (this) {
  CustomPropertyClass.TEXT -> "varchar"
  CustomPropertyClass.INTEGER -> "integer"
  CustomPropertyClass.DATE -> "date"
  CustomPropertyClass.BOOLEAN -> "boolean"
  CustomPropertyClass.DOUBLE -> "numeric"
}