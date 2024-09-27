/*
Copyright 2022 BarD Software s.r.o.

This file is part of GanttProject, an open-source project management tool.

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
package biz.ganttproject.customproperty

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.option.ValidationException
import biz.ganttproject.core.option.Completion
import biz.ganttproject.storage.db.Tables
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.storage.ColumnConsumer
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.storage.ProjectDatabaseException
import kotlin.math.min

sealed class CalculationMethodImpl(override val propertyId: String, override val resultClass: Class<*>): CalculationMethod

/**
 * Simple select is a calculation method that uses a simple SQL expression operating only with the property values of
 * the same task, like those that are usually used in SQL SELECT statement.
 *
 * The examples of such expressions:
 * "duration * 8", "CASE WHEN completion=100 THEN true ELSE false END"
 */
class SimpleSelect(propertyId: String,
                   val selectExpression: String = "id",
                   val whereExpression: String? = null,
                   resultClass: Class<*>) : CalculationMethodImpl(propertyId, resultClass)

class CalculationMethodValidator(private val projectDatabase: ProjectDatabase) {
  fun validate(calculationMethod: CalculationMethod) {
    when (calculationMethod) {
      is SimpleSelect -> {
        try {
          projectDatabase.validateColumnConsumer(ColumnConsumer(calculationMethod) {_,_->})
        } catch (ex: ProjectDatabaseException) {
          //GPLogger.create("ProjectDatabase").error("calculation method validation failed: ${ex.reason}", exception = ex)
          //ex.printStackTrace(System.out)
          throw ValidationException(RootLocalizer.formatText("option.customPropertyDialog.expression.validation.syntax", ex.reason))
        }
      }
    }
  }
}

private val ourTaskTableFields: List<String> = Tables.TASKVIEWFORCOMPUTEDCOLUMNS.run {
  listOf(
    COLOR.name, COST_MANUAL_VALUE.name, COMPLETION.name, DURATION.name, EARLIEST_START_DATE.name, IS_COST_CALCULATED.name,
    IS_MILESTONE.name, IS_PROJECT_TASK.name, NAME.name, NOTES.name, ID.name, PRIORITY.name, START_DATE.name, WEB_LINK.name, COST.name, END_DATE.name
  )
}

class ExpressionAutoCompletion {
  fun complete(text: String, pos: Int): List<Completion> {
    var seekPos = min(pos, text.length - 1)
    while (seekPos >= 0 && (text[seekPos].isJavaIdentifierPart() || text[seekPos].isJavaIdentifierStart())) {
      seekPos--
    }
    val completionPrefix = if (seekPos + 1 in 0..text.length && pos+1 in 0..text.length) text.substring(seekPos + 1, pos + 1) else ""
    return ourTaskTableFields.filter { it.startsWith(completionPrefix) }.sorted().map { Completion(seekPos + 1, pos + 1, it) }
  }
}