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

import biz.ganttproject.core.option.ValidationException
import net.sourceforge.ganttproject.storage.ColumnConsumer
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.storage.ProjectDatabaseException

sealed class CalculationMethod(val propertyId: String, val resultClass: Class<*>)

class SimpleSelect(propertyId: String,
                   val selectExpression: String = "id",
                   resultClass: Class<*>) : CalculationMethod(propertyId, resultClass)

class CalculationMethodValidator(private val projectDatabase: ProjectDatabase) {
  fun validate(calculationMethod: CalculationMethod) {
    when (calculationMethod) {
      is SimpleSelect -> {
        try {
          projectDatabase.validateColumnConsumer(ColumnConsumer(calculationMethod) {_,_->})
        } catch (ex: ProjectDatabaseException) {
          throw ValidationException(ex.message)
        }
      }
    }
  }
}
