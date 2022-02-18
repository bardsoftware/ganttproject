/*
Copyright 2003-2012 GanttProject Team

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
package net.sourceforge.ganttproject.io

import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.option.BooleanOption
import biz.ganttproject.core.option.DefaultBooleanOption
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Maps
import com.google.common.collect.Sets
import net.sourceforge.ganttproject.ResourceDefaultColumn

class CSVOptions {
  private val myTaskOptions: MutableMap<String, BooleanOption> = Maps.newLinkedHashMap()
  private val myResourceOptions: MutableMap<String, BooleanOption> = Maps.newLinkedHashMap()
  val bomOption: BooleanOption = DefaultBooleanOption("write-bom", false)

  fun createTaskExportOption(taskColumn: TaskDefaultColumn): BooleanOption {
    val result = DefaultBooleanOption(taskColumn.stub.id, true)
    myTaskOptions[taskColumn.stub.id] = result
    return result
  }

  fun createTaskExportOption(id: String): BooleanOption {
    val result = DefaultBooleanOption(id, true)
    myTaskOptions[id] = result
    return result
  }

  val taskOptions: Map<String, BooleanOption>
    get() = myTaskOptions
  val resourceOptions: Map<String, BooleanOption>
    get() = myResourceOptions
  @JvmField
  var bFixedSize = false
  @JvmField
  var sSeparatedChar = ","
  @JvmField
  var sSeparatedTextChar = "\""

  init {
    val orderedColumns: List<TaskDefaultColumn> = ImmutableList.of(
      TaskDefaultColumn.ID, TaskDefaultColumn.NAME, TaskDefaultColumn.BEGIN_DATE, TaskDefaultColumn.END_DATE,
      TaskDefaultColumn.DURATION, TaskDefaultColumn.COMPLETION, TaskDefaultColumn.COST
    )
    val columns = Sets.newLinkedHashSet(listOf(*TaskDefaultColumn.values()))
    columns.removeAll(orderedColumns)
    for (taskColumn in orderedColumns) {
      createTaskExportOption(taskColumn)
    }
    for (taskColumn in columns) {
      if (!ourIgnoredTaskColumns.contains(taskColumn)) {
        createTaskExportOption(taskColumn)
      }
    }
    createTaskExportOption("webLink")
    createTaskExportOption("notes")
    myResourceOptions["id"] = DefaultBooleanOption("id", true)
    ResourceDefaultColumn.values().filter { it != ResourceDefaultColumn.ID }.map {
      DefaultBooleanOption(it.stub.id, true)
    }.forEach { myResourceOptions[it.id] = it }
  }

  /**
   * @return a list of the possible separated char.
   */
  val separatedTextChars: Array<String>
    get() = arrayOf("   \'   ", "   \"   ")

  companion object {
    private val ourIgnoredTaskColumns: Set<TaskDefaultColumn> = ImmutableSet.of(
      TaskDefaultColumn.TYPE, TaskDefaultColumn.PRIORITY, TaskDefaultColumn.INFO
    )
  }
}
