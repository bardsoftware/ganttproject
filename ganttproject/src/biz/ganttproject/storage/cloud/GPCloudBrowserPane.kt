/*
Copyright 2018 BarD Software s.r.o

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
package biz.ganttproject.storage.cloud

import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.storage.FolderView
import biz.ganttproject.storage.StorageDialogBuilder
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.ClientContext
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicAuthCache
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.util.EntityUtils
import java.util.function.Consumer

/**
 * This pane shows the contents of GanttProject Cloud storage
 * for a signed in user.
 *
 * @author dbarashev@bardsoftware.com
 */
class GPCloudBrowserPane(
    val mode: StorageDialogBuilder.Mode,
    val dialogUi: StorageDialogBuilder.DialogUi) {
  fun createStorageUi(): Pane {
    val rootPane = VBoxBuilder("pane-service-contents")
    val listView = FolderView(
        this.dialogUi,
        Consumer { },
        Consumer { },
        SimpleBooleanProperty(true),
        SimpleBooleanProperty(true))

    rootPane.apply {
      vbox.prefWidth = 400.0
      addTitle(String.format("webdav.ui.title.%s",
          this@GPCloudBrowserPane.mode.name.toLowerCase()),
          "GanttProject Cloud")
      add(listView.listView, alignment = null, growth = Priority.ALWAYS)
    }
    return rootPane.vbox
  }

  fun loadTeams() {
    val httpHost = HttpHost(GPCLOUD_HOST, 443, "https")
    val httpClient = DefaultHttpClient()
    httpClient.credentialsProvider.setCredentials(
        AuthScope(httpHost), UsernamePasswordCredentials(GPCloudOptions.userId.value, GPCloudOptions.authToken.value))
    val authCache = BasicAuthCache()
    authCache.put(httpHost, BasicScheme())
    val context = BasicHttpContext()
    context.setAttribute(ClientContext.AUTH_CACHE, authCache)

    val teamList = HttpGet("/team/list")
    val resp = httpClient.execute(httpHost, teamList, context)
    println("Response code=${resp.statusLine.statusCode} reason=${resp.statusLine.reasonPhrase}")
    EntityUtils.consume(resp.entity)
  }
}
