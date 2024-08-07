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

import biz.ganttproject.core.chart.scene.gantt.TaskLabelSceneBuilder.*
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.option.DefaultEnumerationOption
import biz.ganttproject.core.option.EnumerationOption
import biz.ganttproject.core.table.ColumnList.Column
import biz.ganttproject.core.table.ColumnList.ColumnStub
import biz.ganttproject.customproperty.CustomPropertyDefinition

typealias PropertyValue<T> = (task: T, id: String) -> String

/**
 * Input interface for the label scene building. Encapsulates four options that control the placement of labels on the
 * sides of the task bar, a font size option, an option that indicates if baselines are switched on, and an accessor
 * to the task property values by their ids.
 */
data class TaskLabelSceneInput<T>(
  val topLabelOption: EnumerationOption,
  val bottomLabelOption: EnumerationOption,
  val leftLabelOption: EnumerationOption,
  val rightLabelOption: EnumerationOption,
  val fontSize: Int,
  val baseline: Boolean,
  val propertyValue: PropertyValue<T>
)

/**
 * Builds a list of fields that can be shown around the task bars.
 */
fun buildOptionValues(customProps: List<CustomPropertyDefinition>): Array<Column> {
  val columns: List<Column> =
    // Empty label and legacy "both task dates" label
    listOf(
      ColumnStub("", "", true, -1 ,-1),
      ColumnStub(ID_TASK_DATES, ID_TASK_DATES, true, -1 ,-1),
    ) +
      // Default columns
    TaskDefaultColumn.entries.filter { it.canBeShownAsLabel() }.map { it.stub } +
      // Custom columns
      customProps.map { ColumnStub(it.id, it.name,true, -1, -1) }
  return columns.toTypedArray()
}

/**
 * Wraps a list of available task labels as the option class.
 */
class TaskColumnEnumerationOption(id: String, var customProps: List<CustomPropertyDefinition>)
  : DefaultEnumerationOption<Column>(id, buildOptionValues(customProps)) {

  override fun objectToString(obj: Column): String {
    return obj.id
  }

  override fun stringToObject(value: String): Column? {
    return typedValues.firstOrNull { it.id == value }
  }

  fun pubStringToObject(value: String): Column?  = stringToObject(value)

  override fun loadPersistentValue(value: String) {
    val validatedValue = when (value) {
      ID_TASK_ADVANCEMENT -> TaskDefaultColumn.COMPLETION.stub.id
      ID_TASK_COORDINATOR -> TaskDefaultColumn.COORDINATOR.stub.id
      ID_TASK_ID -> TaskDefaultColumn.ID.stub.id
      ID_TASK_LENGTH -> TaskDefaultColumn.DURATION.stub.id
      ID_TASK_NAME -> TaskDefaultColumn.NAME.stub.id
      ID_TASK_PREDECESSORS -> TaskDefaultColumn.PREDECESSORS.stub.id
      ID_TASK_RESOURCES -> TaskDefaultColumn.RESOURCES.stub.id
      else -> {
        if (TaskDefaultColumn.find(value) != null || customProps.find { it.id == value } != null) {
          value
        } else null
      }
    }
    if (validatedValue != null) {
      super.loadPersistentValue(validatedValue)
    }
  }

  fun reload(customProps: List<CustomPropertyDefinition>) {
    this.customProps = customProps
    val newValues = buildOptionValues(customProps).toList()
    reloadValues(newValues)
  }
}

fun TaskDefaultColumn.canBeShownAsLabel() = when (this) {
  TaskDefaultColumn.TYPE, TaskDefaultColumn.INFO, TaskDefaultColumn.COLOR, TaskDefaultColumn.NOTES, TaskDefaultColumn.ATTACHMENTS -> false
  else -> true
}