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
package biz.ganttproject.core.chart.scene.gantt

import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.option.DefaultEnumerationOption
import biz.ganttproject.core.option.EnumerationOption
import biz.ganttproject.core.table.ColumnList.Column
import biz.ganttproject.core.table.ColumnList.ColumnStub
import biz.ganttproject.customproperty.CustomPropertyDefinition


data class TaskLabelSceneInput(
  val topLabelOption: EnumerationOption,
  val bottomLabelOption: EnumerationOption,
  val leftLabelOption: EnumerationOption,
  val rightLabelOption: EnumerationOption,
  val fontSize: Int,
  val baseline: Boolean
)

fun buildOptionValues(customProps: List<CustomPropertyDefinition>): Array<Column> {
  val columns: List<Column> = TaskDefaultColumn.entries.map { it.stub } + customProps.map { ColumnStub(it.id, it.name,true, -1, -1) }
  return columns.toTypedArray()
}

class TaskColumnEnumerationOption(id: String, customProps: List<CustomPropertyDefinition>)
  : DefaultEnumerationOption<Column>(id, buildOptionValues(customProps)) {
  override fun objectToString(obj: Column): String {
    return TaskDefaultColumn.entries.find { it.stub.id == obj.id }?.getName() ?: obj.name
  }

  override fun stringToObject(value: String): Column? {
    return typedValues.firstOrNull { it.name == value }
  }

  fun reload(customProps: List<CustomPropertyDefinition>) {
    reloadValues(buildOptionValues(customProps).toList())
  }
}
