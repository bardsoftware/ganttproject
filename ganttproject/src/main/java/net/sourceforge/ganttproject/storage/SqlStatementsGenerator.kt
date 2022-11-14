/*
Copyright 2022 BarD Software s.r.o, Edgar Zhavoronkov

This file is part of GanttProject, an opensource project management tool.

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
package net.sourceforge.ganttproject.storage;

import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.InsertSetMoreStep
import org.jooq.Record
import org.jooq.conf.ParamType
import org.jooq.impl.DSL

fun generateSqlStatement(context: DSLContext, operation: OperationDto): String {
  return when (operation) {
    is OperationDto.InsertOperationDto -> {
      val table = DSL.table(operation.tableName.lowercase())
      var insertLastStep: InsertSetMoreStep<Record>? = null
      val insertFirstStep = context.insertInto(table)
      for ((fieldName, stringValue) in operation.values) {
        val field = DSL.field(fieldName)
        val value = if (stringValue != null) DSL.value(stringValue) else DSL.inline(null, field)
        insertLastStep = insertLastStep?.set(field, value) ?: insertFirstStep.set(field, value)
      }
      insertLastStep!!.getSQL(ParamType.INLINED)
    }
    is OperationDto.DeleteOperationDto -> {
      val binaryCondition = buildBinaryCondition(operation.deleteBinaryConditions)
      val rangeCondition = buildRangeCondition(operation.deleteRangeConditions)
      val table = DSL.table(operation.tableName.lowercase())
      context
        .deleteFrom(table)
        .where(binaryCondition).and(rangeCondition)
        .getSQL(ParamType.INLINED)

    }
    is OperationDto.UpdateOperationDto -> {
      val table = DSL.table(operation.tableName.lowercase())
      val binaryCondition = buildBinaryCondition(operation.updateBinaryConditions)
      val rangeCondition = buildRangeCondition(operation.updateRangeConditions)
      context
        .update(table)
        .set(operation.newValues)
        .where(binaryCondition).and(rangeCondition).getSQL(ParamType.INLINED)
    }
    is OperationDto.MergeOperationDto -> {
      val table = DSL.table(operation.tableName.lowercase())
      val binaryCondition = buildBinaryCondition(operation.mergeBinaryConditions)
      val rangeCondition = buildRangeCondition(operation.mergeRangeConditions)
      context.mergeInto(table).using(DSL.selectOne())
        .on(binaryCondition).and(rangeCondition)
        .whenMatchedThenUpdate().set(operation.whenMatchedThenUpdate)
        .whenNotMatchedThenInsert().set(operation.whenNotMatchedThenInsert)
        .getSQL(ParamType.INLINED)
    }
  }
}

private fun buildBinaryCondition(conditionsList: List<Triple<String, BinaryPred, String>>): Condition {
  var result: Condition = DSL.trueCondition()
  for ((column, pred, value) in conditionsList) {
    val field = DSL.field(column)
    val condition = when (pred) {
      BinaryPred.EQ -> field.eq(value)
      BinaryPred.GT -> field.gt(value)
      BinaryPred.LT -> field.lt(value)
      BinaryPred.LE -> field.le(value)
      BinaryPred.GE -> field.ge(value)
    }
    result = result.and(condition)
  }
  return result
}

private fun buildRangeCondition(conditionsList: List<Triple<String, RangePred, List<String>>>): Condition {
  var result: Condition = DSL.trueCondition()
  for ((column, pred, values) in conditionsList) {
    val field = DSL.field(column)
    val condition = when (pred) {
      RangePred.IN -> field.`in`(values)
      RangePred.NOT_IN -> field.notIn(values)
    }
    result = result.and(condition)
  }
  return result
}
