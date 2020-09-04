/*
Copyright 2018-2020 Dmitry Barashev, BarD Software s.r.o

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

import biz.ganttproject.storage.cloud.ErrorUi
import biz.ganttproject.storage.cloud.HttpClientBuilder
import biz.ganttproject.storage.cloud.ProjectJsonAsFolderItem
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.MissingNode
import com.google.common.io.CharStreams
import javafx.concurrent.Service
import javafx.concurrent.Task
import javafx.event.EventHandler
import net.sourceforge.ganttproject.GPLogger
import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.message.BasicNameValuePair
import java.io.IOException
import java.io.InputStreamReader
import java.time.Duration
import java.util.function.Consumer

/**
 * @author dbarashev@bardsoftware.com
 */
class LockService(private val errorUi: ErrorUi) : Service<JsonNode>() {
  var busyIndicator: Consumer<Boolean> = Consumer {}
  lateinit var project: ProjectJsonAsFolderItem
  var requestLockToken: Boolean = false
  lateinit var duration: Duration

  override fun createTask(): Task<JsonNode> {
    val task = LockTask(this.busyIndicator, project, requestLockToken, duration)
    task.onFailed = EventHandler {
      LOG.error("Lock task failed", kv = mapOf("project" to project.refid), exception = task.exception)
      val errorDetails = task.exception?.message ?: ""
      this.errorUi("Failed to lock project: \n$errorDetails")
    }
    return task
  }
}

open class JsonTask(private val uri: String, private val kv: Map<String, String>,
               private val busyIndicator: (Boolean) -> Unit,
               private val onFailure: (HttpResponse) -> Unit) : Task<JsonNode>() {
  override fun call(): JsonNode {
    busyIndicator(true)
    val resp = HttpPost(uri).let {httpPost ->
      httpPost.entity = UrlEncodedFormEntity(kv.map { BasicNameValuePair(it.key, it.value) }.toList())
      http.execute(httpPost)
    }
    if (resp.statusLine.statusCode == 200) {
      val jsonBody = resp.entity.content.reader(Charsets.UTF_8).readText()
      return if (jsonBody == "") {
        MissingNode.getInstance()
      } else {
        OBJECT_MAPPER.readTree(jsonBody)
      }
    } else {
      onFailure(resp)
      throw IOException("Server responded with HTTP ${resp.statusLine.statusCode}")
    }
  }

}
class LockTask(private val busyIndicator: Consumer<Boolean>,
               val project: ProjectJsonAsFolderItem,
               val requestLockToken: Boolean,
               val duration: Duration) : Task<JsonNode>() {
  override fun call(): JsonNode {
    busyIndicator.accept(true)
    val resp = if (project.isLocked) {
      val projectUnlock = HttpPost("/p/unlock")
      val params = listOf(
          BasicNameValuePair("projectRefid", project.refid))
      projectUnlock.entity = UrlEncodedFormEntity(params)
      http.client.execute(http.host, projectUnlock, http.context)
    } else {
      val projectLock = HttpPost("/p/lock")
      val params = listOf(
          BasicNameValuePair("projectRefid", project.refid),
          BasicNameValuePair("expirationPeriodSeconds", this.duration.seconds.toString()),
          BasicNameValuePair("requestLockToken", requestLockToken.toString())
      )
      projectLock.entity = UrlEncodedFormEntity(params)

      http.client.execute(http.host, projectLock, http.context)
    }
    if (resp.statusLine.statusCode == 200) {
      val jsonBody = CharStreams.toString(InputStreamReader(resp.entity.content, Charsets.UTF_8))
      return if (jsonBody == "") {
        MissingNode.getInstance()
      } else {
        OBJECT_MAPPER.readTree(jsonBody)
      }
    } else {
      LOG.error("Failed to get lock on project", kv = mapOf(
          "Response code" to resp.statusLine.statusCode,
          "reason" to resp.statusLine.reasonPhrase,
          "project" to project.refid))
      throw IOException("Server responded with HTTP ${resp.statusLine.statusCode}")
    }
  }
}

class IsLockedService(
    private val errorUi: ErrorUi,
    private val busyIndicator: (Boolean) -> Unit,
    private val projectRefid: String) : Service<JsonNode>() {
  override fun createTask(): Task<JsonNode> {
    return JsonTask("/p/is-locked", kv = mapOf("projectRefid" to projectRefid), busyIndicator, {})
//    return IsLockedTask(busyIndicator, projectRefid).also {task ->
//      task.onFailed = EventHandler {
//        LOG.error("Lock task failed", kv = mapOf("project" to projectRefid), exception = task.exception)
//        val errorDetails = task.exception?.message ?: ""
//        this.errorUi("Failed to get lock status: \n$errorDetails")
//      }
//    }
  }
}

private val http = HttpClientBuilder.buildHttpClientApache()
private val OBJECT_MAPPER = ObjectMapper()
private val LOG = GPLogger.create("Cloud.Http.Lock")
