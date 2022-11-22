/*
Copyright 2022 BarD Software s.r.o., Anastasiia Postnikova

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
package net.sourceforge.ganttproject.storage

import biz.ganttproject.storage.db.Tables
import biz.ganttproject.storage.db.tables.records.TaskRecord
import net.sourceforge.ganttproject.io.externalizedColor
import net.sourceforge.ganttproject.io.externalizedNotes
import net.sourceforge.ganttproject.io.externalizedWebLink
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskImpl
import org.jooq.DSLContext
import org.jooq.Insert
import java.math.BigDecimal

fun buildInsertTaskQuery(dsl: DSLContext, task: Task): Insert<TaskRecord> {
  var costManualValue: BigDecimal? = null
  var isCostCalculated: Boolean? = null
  if (!(task.cost.isCalculated && task.cost.manualValue == BigDecimal.ZERO)) {
    costManualValue = task.cost.manualValue
    isCostCalculated = task.cost.isCalculated
  }
  return dsl
    .insertInto(Tables.TASK)
    .set(Tables.TASK.UID, task.uid)
    .set(Tables.TASK.NUM, task.taskID)
    .set(Tables.TASK.NAME, task.name)
    .set(Tables.TASK.COLOR, (task as TaskImpl).externalizedColor())
    .set(Tables.TASK.SHAPE, task.shape?.array)
    .set(Tables.TASK.IS_MILESTONE, task.isLegacyMilestone)
    .set(Tables.TASK.IS_PROJECT_TASK, task.isProjectTask)
    .set(Tables.TASK.START_DATE, task.start.toLocalDate())
    .set(Tables.TASK.DURATION, task.duration.length)
    .set(Tables.TASK.COMPLETION, task.completionPercentage)
    .set(Tables.TASK.EARLIEST_START_DATE, task.third?.toLocalDate())
    .set(Tables.TASK.PRIORITY, task.priority.persistentValue)
    .set(Tables.TASK.WEB_LINK, task.externalizedWebLink())
    .set(Tables.TASK.COST_MANUAL_VALUE, costManualValue)
    .set(Tables.TASK.IS_COST_CALCULATED, isCostCalculated)
    .set(Tables.TASK.NOTES, task.externalizedNotes())

}

fun buildInsertTaskDto(task: Task): OperationDto.InsertOperationDto {
  var costManualValue: BigDecimal? = null
  var isCostCalculated: Boolean? = null
  if (!(task.cost.isCalculated && task.cost.manualValue == BigDecimal.ZERO)) {
    costManualValue = task.cost.manualValue
    isCostCalculated = task.cost.isCalculated
  }
  return OperationDto.InsertOperationDto(
    Tables.TASK.name.lowercase(),
    mapOf(
      Tables.TASK.UID.name to task.uid,
      Tables.TASK.NUM.name to task.taskID.toString(),
      Tables.TASK.NAME.name to task.name,
      Tables.TASK.COLOR.name to (task as TaskImpl).externalizedColor(),
      Tables.TASK.SHAPE.name to task.shape?.array,
      Tables.TASK.IS_MILESTONE.name to task.isLegacyMilestone.toString(),
      Tables.TASK.IS_PROJECT_TASK.name to task.isProjectTask.toString(),
      Tables.TASK.START_DATE.name to task.start.toLocalDate().toString(),
      Tables.TASK.DURATION.name to task.duration.length.toString(),
      Tables.TASK.COMPLETION.name to task.completionPercentage.toString(),
      Tables.TASK.EARLIEST_START_DATE.name to (task.third?.toLocalDate()?.toString()),
      Tables.TASK.PRIORITY.name to task.priority.persistentValue,
      Tables.TASK.WEB_LINK.name to task.externalizedWebLink(),
      Tables.TASK.COST_MANUAL_VALUE.name to (costManualValue?.toString()),
      Tables.TASK.IS_COST_CALCULATED.name to (isCostCalculated?.toString()),
      Tables.TASK.NOTES.name to task.externalizedNotes(),
    )
  )
}
