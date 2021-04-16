/*
Copyright 2020 Dmitry Barashev, BarD Software s.r.o

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

import biz.ganttproject.storage.cloud.GPCloudHttpClient
import biz.ganttproject.storage.cloud.HttpClientBuilder
import biz.ganttproject.storage.cloud.HttpMethod
import biz.ganttproject.storage.cloud.HttpPostEncoding
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.MissingNode
import javafx.concurrent.Task
import java.io.IOException

/**
 * This is a task for JavaFX services which send HTTP request and expect JSON response.
 */
class JsonTask(
    private val method: HttpMethod = HttpMethod.POST,
    private val uri: String,
    private val kv: Map<String, String>,
    private val busyIndicator: (Boolean) -> Unit,
    private val onFailure: (JsonTask, GPCloudHttpClient.Response) -> Unit) : Task<JsonNode>() {
  override fun call(): JsonNode {
    busyIndicator(true)
    val resp = when (this.method) {
      HttpMethod.GET -> http.sendGet(uri, kv)
      HttpMethod.POST -> http.sendPost(uri, kv, HttpPostEncoding.URLENCODED)
    }
    busyIndicator(false)
    if (resp.code == 200) {
      val jsonBody = resp.rawBody.decodeToString()
      return if (jsonBody == "") {
        MissingNode.getInstance()
      } else {
        OBJECT_MAPPER.readTree(jsonBody)
      }
    } else {
      onFailure(this, resp)
      throw JsonHttpException(resp.code, resp.reason)
    }
  }

  fun execute(): JsonNode = call()
}

class JsonHttpException(val statusCode: Int, val statusPhrase: String) : IOException(statusPhrase)

private val http: GPCloudHttpClient = HttpClientBuilder.buildHttpClient()
private val OBJECT_MAPPER = ObjectMapper()
