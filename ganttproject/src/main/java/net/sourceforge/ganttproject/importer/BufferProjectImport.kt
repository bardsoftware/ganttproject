/*
Copyright 2021 BarD Software s.r.o

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
package net.sourceforge.ganttproject.importer

import biz.ganttproject.core.calendar.ImportCalendarOption
import biz.ganttproject.core.table.ColumnList
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.resource.HumanResourceMerger.MergeResourcesOption
import net.sourceforge.ganttproject.resource.OverwritingMerger
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManagerImpl

data class ImportBufferProjectApi(
  val taskColumnList: () -> ColumnList,
  val resourceColumnList: () -> ColumnList,
  val refresh: () -> Unit
)
/**
 * @author dbarashev@bardsoftware.com
 */
fun importBufferProject(
  targetProject: IGanttProject,
  bufferProject: BufferProject,
  importApi: ImportBufferProjectApi,
  mergeOption: MergeResourcesOption,
  importCalendarOption: ImportCalendarOption?
): Map<Task, Task> {
  targetProject.roleManager.importData(bufferProject.roleManager)
  if (importCalendarOption != null) {
    targetProject.activeCalendar.importCalendar(bufferProject.activeCalendar, importCalendarOption)
  }
  val targetResCustomPropertyMgr = targetProject.resourceCustomPropertyManager
  val that2thisResourceCustomDefs = targetResCustomPropertyMgr.importData(bufferProject.resourceCustomPropertyManager)
  val original2ImportedResource = targetProject.humanResourceManager.importData(
    bufferProject.humanResourceManager, OverwritingMerger(mergeOption), that2thisResourceCustomDefs
  )
  val result = run {
    val targetCustomColumnStorage = targetProject.taskCustomColumnManager
    val that2thisCustomDefs =
      targetCustomColumnStorage.importData(bufferProject.taskCustomColumnManager)
    val origTaskManager = targetProject.taskManager as TaskManagerImpl
    try {
      origTaskManager.setEventsEnabled(false)
      val result = origTaskManager.importData(bufferProject.taskManager, that2thisCustomDefs)
      origTaskManager.importAssignments(
        bufferProject.taskManager, targetProject.humanResourceManager,
        result, original2ImportedResource
      )
      result
    } finally {
      origTaskManager.setEventsEnabled(true)
    }
  }
  importApi.refresh()
  importApi.taskColumnList().importData(bufferProject.visibleFields, true)
  importApi.resourceColumnList().importData(bufferProject.myResourceVisibleFields, true)
  return result
}

fun UIFacade.asImportBufferProjectApi() = ImportBufferProjectApi(
  { taskColumnList },
  { resourceTree.visibleFields },
  { refresh() }
)
