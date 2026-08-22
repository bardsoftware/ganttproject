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
import javafx.scene.control.ListView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.resource.HumanResourceManager

/**
 * Model backing [GPCloudResourceListDialog]. Loads the teams the current user belongs to and,
 * for each team, the resources shared in that team. Resources which are already added to the
 * current project are marked read-only so the UI can disable their checkbox.
 */
internal class GPCloudResourceListModel(private val resourceManager: HumanResourceManager) {

  suspend fun loadResources(): List<ResourceDto> {
    val projectEmails = resourceManager.resources.map { it.mail.lowercase() }.filter { it.isNotBlank() }.toSet()
    val isInProject: (ResourceDto) -> Boolean = { it.email.isNotBlank() && projectEmails.contains(it.email.lowercase()) }

    return withContext(Dispatchers.IO) {
      val results: List<Result<List<ResourceDto>>> = loadTeams()
        .map { teamRefid ->
          async {
            runCatching {
              loadTeamResources(teamRefid)
            }
          }
        }
        .awaitAll()
      // If all results are failures then throw the first exception.
      results.mapNotNull { it.exceptionOrNull() }.forEach { LOG.error("Failed to load team resources", exception = it) }
      if (results.isNotEmpty() && results.all { it.isFailure }) {
        throw results.first().exceptionOrNull()!!
      }
      results.mapNotNull { it.getOrNull() }
        .flatten()
        .distinctBy { it.email.ifBlank { it.name } }
        .onEach { dto ->
          val isInProject = isInProject(dto)
          dto.isReadOnly = isInProject
          dto.isChecked = isInProject
        }
        .sortedWith(compareByDescending<ResourceDto> { it.isChecked }.thenBy { it.name })
    }
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
      jsonTeams.elements().asSequence().mapNotNull { it["refid"]?.asText() }.toList()
    } else emptyList()
  }

  fun addResources(listView: ListView<ResourceDto>) {
    listView.items.filter { it.isChecked && !it.isReadOnly }.forEach {
      this.resourceManager.newResourceBuilder().withEmail(it.email).withName(it.name).withPhone(it.phone).build()
    }
  }
}

private val LOG = GPLogger.create("Cloud.Http")
