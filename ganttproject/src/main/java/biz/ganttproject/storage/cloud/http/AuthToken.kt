/*
Copyright 2021 Dmitry Barashev, BarD Software s.r.o

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
package biz.ganttproject.storage.cloud.http

import biz.ganttproject.storage.cloud.GPCloudOptions
import biz.ganttproject.storage.cloud.HttpClientBuilder
import biz.ganttproject.storage.cloud.httpScope
import biz.ganttproject.storage.cloud.isNetworkAvailable
import com.google.common.base.Strings
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import java.net.UnknownHostException
import java.util.logging.Level

/**
 * @author dbarashev@bardsoftware.com
 */
fun tryAccessToken(onStart: () -> Unit = {},
                   onSuccess: (String) -> Unit,
                   onError: (String) -> Unit) {
  if (Strings.isNullOrEmpty(GPCloudOptions.authToken.value)) {
    onError("NO_ACCESS_TOKEN")
    return
  }
  if (!GPCloudOptions.isTokenValid) {
    onError("ACCESS_TOKEN_EXPIRED")
    return
  }

  onStart()
  httpScope.launch {
    try {
      callAuthCheck(onSuccess, onError)
    } catch (ex: Exception) {
      if (ex is UnknownHostException) {
        if (!isNetworkAvailable()) {
          onError("OFFLINE")
        } else {
          onError("")
        }
      } else {
        GPLogger.getLogger("GPCloud").log(Level.SEVERE, "Failed to contact GPCloud server", ex)
        onError("")
      }
    }
  }
}

private fun callAuthCheck(onSuccess: (String)->Unit, onError: (String)->Unit) {
  val http = HttpClientBuilder.buildHttpClient()
  val resp = http.sendGet("/access-token/check")
  when (resp.code) {
    200 -> onSuccess("")
    401 -> onError("INVALID")
    else -> {
      onError("INVALID")
    }
  }
}
