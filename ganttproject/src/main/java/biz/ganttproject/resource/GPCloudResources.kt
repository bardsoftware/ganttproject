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
package biz.ganttproject.resource

import biz.ganttproject.app.*
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.storage.cloud.FlowPage
import biz.ganttproject.storage.cloud.GPCloudUiFlow
import biz.ganttproject.storage.cloud.GPCloudUiFlowBuilder
import biz.ganttproject.storage.cloud.HttpMethod
import biz.ganttproject.storage.cloud.http.JsonHttpException
import biz.ganttproject.storage.cloud.http.JsonTask
import biz.ganttproject.storage.cloud.http.ResourceDto
import biz.ganttproject.storage.cloud.http.loadTeamResources
import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.resource.HumanResourceManager
import java.awt.event.ActionEvent

/**
 * Just an action to plug into the application menu.
 */
class GPCloudResourceListAction(private val resourceManager: HumanResourceManager) : GPAction("cloud.resource.list.action") {
  override fun actionPerformed(e: ActionEvent?) {
    GPCloudResourceListDialog(resourceManager).show()
  }
}

/**
 * Pane with a list of resources fetched from GP Cloud.
 */
class ResourceListPage(
  private val listView: ListView<ResourceDto>,
  private val dialog: DialogController,
  private val resource2selected: MutableMap<String, BooleanProperty>,
  private val resourceManager: HumanResourceManager
) : FlowPage() {
  private lateinit var uiFlow: GPCloudUiFlow

  override fun createUi(): Pane =
    VBoxBuilder("content-pane").apply {
      add(listView, alignment = null, growth = Priority.ALWAYS)
    }.vbox

  override fun resetUi() {
  }

  override fun setController(controller: GPCloudUiFlow) {
    this.uiFlow = controller
  }

  override var active: Boolean = false
    set(value) {
      field = value
      if (value) {
        GlobalScope.launch(Dispatchers.IO) {
          val stopProgress = dialog.toggleProgress(true)
          val allResources = try {
            loadTeams(dialog).map {
              async { loadTeamResources(it) }
            }.map { it.await() }.reduce { acc, list -> acc + list }.distinctBy { it.email }
          } catch (ex: JsonHttpException) {
            dialog.showAlert(RootLocalizer.create("error.channel.itemTitle"), createAlertBody(ex.message ?: ""))
            emptyList()
          } finally {
            stopProgress()
          }
          fillListView(allResources)
        }

        dialog.setupButton(ButtonType.APPLY) { btn ->
          btn.textProperty().bind(RootLocalizer.create("cloud.resource.list.btnApply"))
          btn.styleClass.add("btn-attention")
          btn.setOnAction {
            addResourcesToProject()
          }
        }
      }
    }

  private fun fillListView(resources: List<ResourceDto>) {
    resources.forEach {
      resource2selected[it.email] = SimpleBooleanProperty(false)
    }
    Platform.runLater {
      listView.items.addAll(resources)
    }
  }

  private fun loadTeams(dlg: DialogController): List<String> = try {
    JsonTask(
      method = HttpMethod.GET,
      uri = "/team/list",
      kv = mapOf("owned" to "true", "participated" to "true"),
      busyIndicator = {},
      onFailure = {_, _ -> }
    ).execute().let { result ->
      if (result.isArray) {
        result.elements().asSequence().map { it["refid"].asText() }.toList()
      } else emptyList()
    }
  } catch (ex: JsonHttpException) {

    dlg.showAlert(i18n.create("http.error"), createAlertBody("Server returned HTTP ${ex.statusCode}"))
    emptyList()
  }

  private fun addResourcesToProject() {
    val selectedEmails = resource2selected.filter { it.value.value }.keys
    listView.items.filter { it.email in selectedEmails }.forEach {
      this.resourceManager.newResourceBuilder().withEmail(it.email).withName(it.name).withPhone(it.phone).build()
    }
  }

}

/**
 * Builds a UI flow with the main page (resources list) and sign-in and other pages.
 */
class GPCloudResourceListDialog(private val resourceManager: HumanResourceManager) {
  private val resource2selected = mutableMapOf<String, BooleanProperty>()
  private val listView = ListView<ResourceDto>().apply {
    setCellFactory { _ ->
      ResourceListCell(resourceManager) {
        resource2selected[it.email]
      }
    }
  }

  fun show() {
    dialog { dlg ->
      dlg.addStyleClass("dlg-cloud-resource-list")
      dlg.addStyleSheet(
        "/biz/ganttproject/resource/GPCloudResources.css"
      )
      dlg.setHeader(
        VBoxBuilder("header").apply {
          addTitle(LocalizedString("cloud.resource.list.title", RootLocalizer)).also { hbox ->
            hbox.alignment = Pos.CENTER_LEFT
            hbox.isFillHeight = true
          }
        }.vbox
      )

      val wrapper = BorderPane()

      dlg.setContent(wrapper)
      val cloudUiFlow = GPCloudUiFlowBuilder().run {
        wrapperPane = wrapper
        dialog = dlg
        mainPage = ResourceListPage(listView, dlg, resource2selected, resourceManager)
        build()
      }

      dlg.onShown = {
        cloudUiFlow.start()
        dlg.resize()
      }
    }
  }
}

/**
 * Renders a cell in the list of resources.
 */
class ResourceListCell(private val resourceManager: HumanResourceManager,
                       private val resource2checked: (ResourceDto) -> BooleanProperty?) : ListCell<ResourceDto>() {
  private val checkBox = CheckBox()
  private var isChecked: BooleanProperty? = null

  override fun updateItem(item: ResourceDto?, empty: Boolean) {
    super.updateItem(item, empty)

    if (empty || item == null) {
      text = null
      graphic = null
      return
    }

    graphic = HBox().apply {
      styleClass.add("resource-cell")
      children.add(checkBox)
      children.add(
        VBoxBuilder("labels").apply {
          add(Label(item.name).also { it.styleClass.add("name") })
          add(Label(item.email).also { it.styleClass.add("email") })
        }.vbox.also {
          it .onMouseClicked = EventHandler { evt ->
            if (evt.clickCount == 2) {
              isChecked?.let { it.value = !it.value }
            }
          }
        }
      )
      if (resourceManager.resources.find { it.mail == item.email } == null) {
        isChecked?.let { checkBox.selectedProperty().unbindBidirectional(it) }
        isChecked = resource2checked(item)
        isChecked?.let { checkBox.selectedProperty().bindBidirectional(it) }
      } else {
        checkBox.selectedProperty().value = true
        this.isDisable = true
      }
    }
  }
}

private val i18n = RootLocalizer.createWithRootKey("cloud.resource.list")
