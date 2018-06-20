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
import biz.ganttproject.storage.BreadcrumbView
import biz.ganttproject.storage.FolderItem
import biz.ganttproject.storage.FolderView
import biz.ganttproject.storage.StorageDialogBuilder
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import net.sourceforge.ganttproject.language.GanttLanguage
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.controlsfx.control.StatusBar
import java.nio.file.Paths
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
  private val i18n = GanttLanguage.getInstance()
  private lateinit var listView: FolderView<FolderItem>

  fun createStorageUi(): Pane {
    val rootPane = VBoxBuilder("pane-service-contents")
    this.listView = FolderView(
        this.dialogUi,
        Consumer { },
        Consumer { },
        SimpleBooleanProperty(true),
        SimpleBooleanProperty(true))

    val breadcrumbView = BreadcrumbView(Paths.get("/", "GanttProject Cloud"), Consumer {})

    val busyIndicator = StatusBar().apply {
      styleClass.add("notification")
      text = ""
    }
    HBox.setHgrow(busyIndicator, Priority.ALWAYS)
    val btnSave = Button(i18n.getText("storageService.local.${this.mode.name.toLowerCase()}.actionLabel"))
    btnSave.styleClass.add("btn-attention")
    val saveBox = HBox().apply {
      children.addAll(busyIndicator, btnSave)
      styleClass.add("doclist-save-box")
    }

    rootPane.apply {
      vbox.prefWidth = 400.0
      addTitle(String.format("webdav.ui.title.%s",
          this@GPCloudBrowserPane.mode.name.toLowerCase()),
          "GanttProject Cloud")
      add(breadcrumbView.breadcrumbs)
      add(listView.listView, alignment = null, growth = Priority.ALWAYS)
      add(saveBox)
    }
    return rootPane.vbox
  }

  fun loadTeams() {
    val http = HttpClientBuilder.buildHttpClient()
    val teamList = HttpGet("/team/list")
    val resp = http.client.execute(http.host, teamList, http.context)
    if (resp.statusLine.statusCode == 200) {
      val objectMapper = ObjectMapper()
      val jsonNode = objectMapper.readTree(resp.entity.content)
      if (jsonNode is ArrayNode) {
        this.listView.setResources(FXCollections.observableArrayList<FolderItem>(
            jsonNode.map(::TeamJsonAsFolderItem)))

      }
    } else {
      println("Response code=${resp.statusLine.statusCode} reason=${resp.statusLine.reasonPhrase}")
      println(EntityUtils.toString(resp.entity))
    }
  }
}

class TeamJsonAsFolderItem(val node: JsonNode) : FolderItem {
  override val isLocked: Boolean
    get() = false
  override val isLockable: Boolean
    get() = false
  override val name: String
    get() = this.node["name"].asText()
  override val isDirectory: Boolean
    get() = false

}
