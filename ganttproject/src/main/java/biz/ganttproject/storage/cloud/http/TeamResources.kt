/*
Copyright 2021 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.storage.cloud.http

import biz.ganttproject.storage.cloud.HttpMethod
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.math.BigDecimal

class TeamResourcesException : Exception()

data class ResourceDto @JsonCreator constructor(
  @JsonProperty(value = "name", required = true)
  var name: String = "",
  @JsonProperty(value = "email", required = true)
  var email: String = "",
  @JsonProperty("role")
  var role: String = "",
  @JsonProperty("phone")
  var phone: String = "",
  @JsonProperty("paymentRate")
  var paymentRate: BigDecimal? = null
)

fun loadTeamResources(teamRefid: String) : List<ResourceDto> {
  val resourcesJson = JsonTask(
    method = HttpMethod.GET,
    uri = "/team/resources/list",
    kv = mapOf(
      "teamRefid" to teamRefid
    ),
    busyIndicator = {},
    onFailure = {_, _ -> }
  ).let {
    it.execute()
  }
  return if (resourcesJson.isArray) {
    resourcesJson.map {
      OBJECT_MAPPER.treeToValue(it, ResourceDto::class.java)
    }
  } else {
    emptyList()
  }
}

private val OBJECT_MAPPER = ObjectMapper()
