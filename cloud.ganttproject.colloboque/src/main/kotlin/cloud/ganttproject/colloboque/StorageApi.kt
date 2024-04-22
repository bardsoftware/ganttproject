/*
Copyright 2024 BarD Software s.r.o., Dmitry Barashev

This file is part of GanttProject Cloud.

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
package cloud.ganttproject.colloboque

import cloud.ganttproject.colloboque.db.project_template.tables.records.ProjectfilesnapshotRecord
import net.sourceforge.ganttproject.storage.XlogRecord
import net.sourceforge.ganttproject.task.Task

interface StorageApi {
  fun initProject(projectRefid: String)
  fun getTransactionLogs(projectRefid: ProjectRefid, baseTxnId: BaseTxnId): List<XlogRecord>
  fun insertXlogs(projectRefid: ProjectRefid, baseTxnId: BaseTxnId, xlog: List<XlogRecord>)
  fun insertTask(projectRefid: String, task: Task)

  /**
   * Fetches a snapshot record corresponding to the given project and base transaction identifier. If the latter is null,
   * returns the latest available snapshot.
   */
  fun getProjectSnapshot(projectRefid: String, baseTxnId: BaseTxnId? = null): ProjectfilesnapshotRecord?
  fun insertActualSnapshot(projectRefid: String, baseTxnId: BaseTxnId, projectXml: String)
}

class PluggableStorageApi(
  private val initProject_: (projectRefid: String) -> Unit = {},
  private val getTransactionLogs_: (projectRefid: ProjectRefid, baseTxnId: BaseTxnId)->List<XlogRecord> = { _, _ ->
    error("Not implemented")},
  private val insertXlogs_: (projectRefid: ProjectRefid, baseTxnId: BaseTxnId, xlog: List<XlogRecord>) -> Unit = {_, _, _ ->
    error("Not implemented")
  },
  private val insertTask_: (projectRefid: String, task: Task) -> Unit = {_, _ ->},

  private val getProjectSnapshot_: (projectRefid: String, baseTxnId: BaseTxnId?) -> ProjectfilesnapshotRecord? = { _, _ ->
    error("Not implemented")
  },
  private val insertActualSnapshot_: (projectRefid: String, baseTxnId: BaseTxnId, projectXml: String) -> Unit = {_, _, _ -> },
) : StorageApi {
  override fun initProject(projectRefid: String) = initProject_(projectRefid)

  override fun getTransactionLogs(projectRefid: ProjectRefid, baseTxnId: BaseTxnId) = getTransactionLogs_(projectRefid, baseTxnId)

  override fun insertXlogs(projectRefid: ProjectRefid, baseTxnId: BaseTxnId, xlog: List<XlogRecord>) = insertXlogs_(projectRefid, baseTxnId, xlog)

  override fun insertTask(projectRefid: String, task: Task) = insertTask_(projectRefid, task)

  override fun getProjectSnapshot(projectRefid: String, baseTxnId: BaseTxnId?) = getProjectSnapshot_(projectRefid, baseTxnId)

  override fun insertActualSnapshot(projectRefid: String, baseTxnId: BaseTxnId, projectXml: String) = insertActualSnapshot_(projectRefid, baseTxnId, projectXml)
}
