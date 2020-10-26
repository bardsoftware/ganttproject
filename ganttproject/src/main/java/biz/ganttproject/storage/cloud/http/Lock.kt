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
import biz.ganttproject.storage.cloud.HttpMethod
import com.fasterxml.jackson.databind.JsonNode
import javafx.concurrent.Service
import javafx.concurrent.Task
import javafx.event.EventHandler
import net.sourceforge.ganttproject.GPLogger
import java.time.Duration
import java.util.function.Consumer

/**
 * @author dbarashev@bardsoftware.com
 */
class LockService(
    private val projectRefid: String,
    private val isLockedNow: Boolean,
    private val errorUi: ErrorUi) : Service<JsonNode>() {
  var busyIndicator: Consumer<Boolean> = Consumer {}

  var requestLockToken: Boolean = false
  lateinit var duration: Duration

  override fun createTask(): Task<JsonNode> {
    return if (isLockedNow) {
      JsonTask(HttpMethod.POST,"/p/unlock", mapOf("projectRefid" to projectRefid), {}) { task, resp ->
        LOG.error("Unlock task failed",
            kv = mapOf("project" to projectRefid, "status" to resp.code),
            exception = task.exception)
        val errorDetails = task.exception?.message ?: resp.reason
        this.errorUi("Failed to unlock the project: \n$errorDetails")
      }
    } else {
      JsonTask(HttpMethod.POST,"/p/lock", mapOf(
          "projectRefid" to projectRefid,
          "expirationPeriodSeconds" to this.duration.seconds.toString(),
          "requestLockToken" to requestLockToken.toString()
      ), {}) { task, resp ->
        LOG.error("Lock task failed",
            kv = mapOf("project" to projectRefid, "status" to resp.code),
            exception = task.exception)
        val errorDetails = task.exception?.message ?: resp.reason
        this.errorUi("Failed to lock the project: \n$errorDetails")
      }
    }
  }
}

class IsLockedService(
    private val errorUi: ErrorUi,
    private val busyIndicator: (Boolean) -> Unit,
    private val projectRefid: String,
    onSuccess: (JsonNode) -> Unit) : Service<JsonNode>() {
  init {
    this.onSucceeded = EventHandler {
      onSuccess(this.value)
    }
  }
  override fun createTask(): Task<JsonNode> {
    return JsonTask(HttpMethod.GET,"/p/is-locked", kv = mapOf("projectRefid" to projectRefid), busyIndicator) { task, resp ->
      LOG.error("Server responded with ${resp.code}",
          mapOf("project" to projectRefid, "endpoint" to "/p/is-locked"),
          task.exception
      )
      errorUi("Server responded with HTTP ${resp.code} (${resp.reason})")
    }
  }
}

private val LOG = GPLogger.create("Cloud.Http.Lock")
