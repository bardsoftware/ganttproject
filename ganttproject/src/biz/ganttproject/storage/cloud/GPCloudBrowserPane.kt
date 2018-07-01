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

import biz.ganttproject.storage.BrowserPaneBuilder
import biz.ganttproject.storage.FolderItem
import biz.ganttproject.storage.StorageDialogBuilder
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.common.io.CharStreams
import javafx.application.Platform
import javafx.beans.property.Property
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Service
import javafx.concurrent.Task
import javafx.event.EventHandler
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.document.Document
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.logging.Level

/**
 * Wraps JSON node matching a team to FolderItem
 */
class TeamJsonAsFolderItem(val node: JsonNode) : FolderItem {
  override val isLocked: Boolean
    get() = false
  override val isLockable: Boolean
    get() = false
  override val name: String
    get() = this.node["name"].asText()
  override val isDirectory: Boolean
    get() = true
}

class ProjectJsonAsFolderItem(val node: JsonNode) : FolderItem {
  override val isLocked: Boolean
    get() = false
  override val isLockable: Boolean
    get() = false
  override val name: String
    get() = this.node["name"].asText()
  override val isDirectory: Boolean
    get() = false

}
/**
 * This pane shows the contents of GanttProject Cloud storage
 * for a signed in user.
 *
 * @author dbarashev@bardsoftware.com
 */
class GPCloudBrowserPane(
    private val mode: StorageDialogBuilder.Mode,
    private val dialogUi: StorageDialogBuilder.DialogUi,
    private val documentConsumer: Consumer<Document>) {
  private val loaderService = LoaderService(dialogUi)

  fun createStorageUi(): Pane {
    val builder = BrowserPaneBuilder(this.mode, this.dialogUi) { path, success, loading ->
      loadTeams(path, success, loading)
    }
    val browserPaneElements = builder.apply {
      withBreadcrumbs()
      withActionButton(EventHandler {})
      withListView(
          onLaunch = Consumer {
            if (it is ProjectJsonAsFolderItem) {
              this@GPCloudBrowserPane.openDocument(it)
            }
          }
      )
    }.build()

    Platform.runLater {
      browserPaneElements.breadcrumbView.path = Paths.get("/")
    }
    return browserPaneElements.pane
  }

  private fun openDocument(item: ProjectJsonAsFolderItem) {
    if (item.node is ObjectNode) {
      this.documentConsumer.accept(GPCloudDocument(item.node))
    }
  }

  private fun loadTeams(path: Path, setResult: Consumer<ObservableList<FolderItem>>, showMaskPane: Consumer<Boolean>) {
    loaderService.apply {
      busyIndicator = showMaskPane
      this.path = path
      onSucceeded = EventHandler { _ ->
        setResult.accept(value)
        showMaskPane.accept(false)
      }
      onFailed = EventHandler { _ ->
        showMaskPane.accept(false)
        dialogUi.error("Loading failed!")
      }
      onCancelled = EventHandler { _ ->
        showMaskPane.accept(false)
        GPLogger.log("Loading cancelled!")
      }
      restart()
    }
  }
}

// Background tasks which communicate with GP Cloud server and load
// user team and project list.

// Create LoadTask or CachedTask depending on whether we have cached response from GP Cloud or not
class LoaderService(private val dialogUi: StorageDialogBuilder.DialogUi) : Service<ObservableList<FolderItem>>() {
  var busyIndicator: Consumer<Boolean> = Consumer {}
  var path: Path = Paths.get("/")
  var jsonResult: SimpleObjectProperty<JsonNode> = SimpleObjectProperty()

  override fun createTask(): Task<ObservableList<FolderItem>> {
    if (jsonResult.value == null) {
      val task = LoaderTask(busyIndicator, this.jsonResult)
      task.onFailed = EventHandler { _ ->
        val errorDetails = if (task.exception != null) {
          GPLogger.getLogger("GPCloud").log(Level.WARNING, "", task.exception)
          "\n${task.exception.message}"
        } else {
          ""
        }
        this.dialogUi.error("Failed to load data from GanttProject Cloud $errorDetails")
      }
      return task
    } else {
      return CachedTask(this.path, this.jsonResult)
    }
  }
}

// Takes the root node of GP Cloud response and filters teams
fun filterTeams(jsonNode: JsonNode, filter: Predicate<JsonNode>): List<JsonNode> {
  return if (jsonNode is ArrayNode) {
    jsonNode.filter(filter::test)
  } else {
    emptyList()
  }
}

// Takes a list of team nodes and returns filtered projects.
// This can work if teams.size > 1 (e.g. to find all projects matching some criteria)
// but in practice we expect teams.size == 1
fun filterProjects(teams: List<JsonNode>, filter: Predicate<JsonNode>): List<JsonNode> {
  return teams.flatMap { team -> team.get("projects").let {
    if (it is ArrayNode) {
      it.filter(filter::test).map { project -> project.also { (it as ObjectNode).put("team", team["name"].asText()) } }
    } else {
      emptyList()
    }
  }}
}

// Processes cached response from GP Cloud
class CachedTask(val path: Path, val jsonNode: SimpleObjectProperty<JsonNode>) : Task<ObservableList<FolderItem>>() {
  override fun call(): ObservableList<FolderItem> {
    return FXCollections.observableArrayList(
        when (path.nameCount) {
          1 -> filterTeams(jsonNode.value, Predicate { true }).map(::TeamJsonAsFolderItem)
          2 -> {
            filterProjects(
                filterTeams(jsonNode.value, Predicate { it["name"].asText() == path.getName(1).toString() }),
                Predicate { true }
            ).map(::ProjectJsonAsFolderItem)
          }
          else -> emptyList()
        })
  }
}

// Sends HTTP request to GP Cloud and returns a list of teams.
class LoaderTask(val busyIndicator: Consumer<Boolean>, val resultStorage: Property<JsonNode>) : Task<ObservableList<FolderItem>>() {
  override fun call(): ObservableList<FolderItem>? {
    busyIndicator.accept(true)
    val log = GPLogger.getLogger("GPCloud")
    val http = HttpClientBuilder.buildHttpClient()
    val teamList = HttpGet("/team/list")

    val jsonBody = let {
      val resp = http.client.execute(http.host, teamList, http.context)
      if (resp.statusLine.statusCode == 200) {
        CharStreams.toString(InputStreamReader(resp.entity.content))
      } else {
        with(log) {
          warning(
              "Failed to get team list. Response code=${resp.statusLine.statusCode} reason=${resp.statusLine.reasonPhrase}")
          fine(EntityUtils.toString(resp.entity))
        }
        throw IOException("Server responded with HTTP ${resp.statusLine.statusCode}")
      }
    }

    val objectMapper = ObjectMapper()
    val jsonNode = objectMapper.readTree(jsonBody)
    resultStorage.value = jsonNode
    return FXCollections.observableArrayList(filterTeams(jsonNode, Predicate { true }).map(::TeamJsonAsFolderItem))
  }
}

