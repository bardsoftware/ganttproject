/*
Copyright 2026 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.resource

import biz.ganttproject.storage.cloud.HttpMethod
import biz.ganttproject.storage.cloud.http.JsonTask
import biz.ganttproject.storage.cloud.http.ResourceDto
import biz.ganttproject.storage.cloud.http.loadTeamResources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import net.sourceforge.ganttproject.resource.HumanResourceManager

/**
 * Model backing [GPCloudResourceListDialog]. Loads the teams the current user belongs to and,
 * for each team, the resources shared in that team. Resources which are already added to the
 * current project are marked read-only so the UI can disable their checkbox.
 */
internal class GPCloudResourceListModel(private val resourceManager: HumanResourceManager) {

  private fun isInProject(dto: ResourceDto): Boolean = resourceManager.resources.find { it.mail == dto.email } != null

  suspend fun loadResources(): List<ResourceDto> = withContext(Dispatchers.IO) {
    loadTeams()
      .map { teamRefid -> async { loadTeamResources(teamRefid) } }
      .awaitAll()
      .flatten()
      .distinctBy { it.email }
      .onEach { it.isReadOnly = isInProject(it) }
      .sortedWith(compareBy<ResourceDto> { if (it.isReadOnly) 0 else 1 }.thenBy { it.name })
  }

  private fun loadTeams(): List<String> {
    val jsonTeams = JsonTask(
      method = HttpMethod.GET,
      uri = "/team/list",
      kv = mapOf("owned" to "true", "participated" to "true"),
      busyIndicator = {},
      onFailure = { _, _ -> }
    ).execute()

    return if (jsonTeams.isArray) {
      jsonTeams.elements().asSequence().map { it["refid"].asText() }.toList()
    } else emptyList()
  }
}
