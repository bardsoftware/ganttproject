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

import biz.ganttproject.customproperty.CustomProperty
import biz.ganttproject.customproperty.CustomPropertyHolder
import biz.ganttproject.storage.db.Tables.TASKCUSTOMCOLUMN
import net.sourceforge.ganttproject.task.Task
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.conf.ParamType
import org.jooq.impl.DSL

/**
 * Creates SQL statements for updating custom property records.
 */
internal class SqlTaskCustomPropertiesUpdateBuilder(
  task: Task, private val onCommit: (List<SqlQuery>, List<SqlUndoQuery>) -> Unit, private val dialect: SQLDialect
) {
  private val taskUid = task.uid
  private val customPropertyManager = task.manager.customPropertyManager

  internal var commit: () -> Unit = {}

  private fun generateStatements(customProperties: CustomPropertyHolder, isUndoOperation: Boolean): List<SqlQuery> {
    val statements = mutableListOf<String>()
    val colloboqueUpdateDtos = mutableListOf<OperationDto>()

    val generateDeleteFnForH2 = {
      if (isUndoOperation) generateDeleteStatementAllColumns(DSL.using(dialect))
      else generateDeleteStatement(DSL.using(dialect), customProperties)
    }
    val generateDeleteFnForColloboque = {
      if (isUndoOperation) generateDeleteDtoAllColumns()
      else generateDeleteDto(customProperties)
    }
    statements.add(generateDeleteFnForH2())
    colloboqueUpdateDtos.add(generateDeleteFnForColloboque())

    statements.addAll(generateMergeStatements(customProperties.customProperties) { return@generateMergeStatements DSL.using(dialect) })
    colloboqueUpdateDtos.addAll(generateMergeDtos(customProperties.customProperties))
    return statements.zip(colloboqueUpdateDtos).map { SqlQuery(it.first, it.second) }
  }

  internal fun setCustomProperties(oldCustomProperties: CustomPropertyHolder, newCustomProperties: CustomPropertyHolder) {
    commit = {
      onCommit(
        generateStatements(newCustomProperties, isUndoOperation = false),
        generateStatements(oldCustomProperties, isUndoOperation = true)
      )
      onCommit(
        listOf(SqlQuery(
          sqlStatementH2 = createUpdateCustomValuesStatement(taskUid, customPropertyManager, newCustomProperties),
          colloboqueOperationDto = OperationDto.NoOperationDto(""))
        ),
        listOf(SqlQuery(sqlStatementH2 = "", colloboqueOperationDto = OperationDto.NoOperationDto(""))),
      )
    }
  }

  private fun generateDeleteStatement(dsl: DSLContext, customProperties: CustomPropertyHolder): String =
    dsl.deleteFrom(TASKCUSTOMCOLUMN)
      .where(TASKCUSTOMCOLUMN.UID.eq(taskUid))
      .and(TASKCUSTOMCOLUMN.COLUMN_ID.notIn(customProperties.customProperties.map { it.definition.id }))
      .getSQL(ParamType.INLINED)

  private fun generateDeleteDto(customProperties: CustomPropertyHolder): OperationDto.DeleteOperationDto =
    OperationDto.DeleteOperationDto(
      TASKCUSTOMCOLUMN.name.lowercase(),
      listOf(
        Triple(TASKCUSTOMCOLUMN.UID.name, BinaryPred.EQ, taskUid),
      ),
      listOf(
        Triple(TASKCUSTOMCOLUMN.COLUMN_ID.name, RangePred.NOT_IN, customProperties.customProperties.map { it.definition.id } )
      )
    )

  private fun generateDeleteStatementAllColumns(dsl: DSLContext): String =
    dsl.deleteFrom(TASKCUSTOMCOLUMN)
      .where(TASKCUSTOMCOLUMN.UID.eq(taskUid))
      .getSQL(ParamType.INLINED)

  private fun generateDeleteDtoAllColumns(): OperationDto.DeleteOperationDto =
    OperationDto.DeleteOperationDto(
      TASKCUSTOMCOLUMN.name.lowercase(),
      listOf(
        Triple(TASKCUSTOMCOLUMN.UID.name, BinaryPred.EQ, taskUid),
      )
    )

  private fun generateMergeStatements(customProperties: List<CustomProperty>, dsl: ()-> DSLContext) =
    customProperties.map {
      dsl().mergeInto(TASKCUSTOMCOLUMN).using(DSL.selectOne())
        .on(TASKCUSTOMCOLUMN.UID.eq(taskUid)).and(TASKCUSTOMCOLUMN.COLUMN_ID.eq(it.definition.id))
        .whenMatchedThenUpdate().set(TASKCUSTOMCOLUMN.COLUMN_VALUE, it.valueAsString)
        .whenNotMatchedThenInsert(TASKCUSTOMCOLUMN.UID, TASKCUSTOMCOLUMN.COLUMN_ID, TASKCUSTOMCOLUMN.COLUMN_VALUE)
        .values(taskUid, it.definition.id, it.valueAsString)
        .getSQL(ParamType.INLINED)
    }

  private fun generateMergeDtos(customProperties: List<CustomProperty>) =
    customProperties.map {
      OperationDto.MergeOperationDto(
        TASKCUSTOMCOLUMN.name.lowercase(),
        listOf(
          Triple(TASKCUSTOMCOLUMN.UID.name, BinaryPred.EQ, taskUid),
          Triple(TASKCUSTOMCOLUMN.COLUMN_ID.name, BinaryPred.EQ, it.definition.id)
        ),
        listOf(),
        mapOf(
          TASKCUSTOMCOLUMN.COLUMN_VALUE.name to it.valueAsString
        ),
        mapOf(
          TASKCUSTOMCOLUMN.UID.name to taskUid,
          TASKCUSTOMCOLUMN.COLUMN_ID.name to it.definition.id,
          TASKCUSTOMCOLUMN.COLUMN_VALUE.name to it.valueAsString
        )
      )
    }
}

