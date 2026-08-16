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
import biz.ganttproject.lib.fx.vbox
import biz.ganttproject.storage.cloud.*
import biz.ganttproject.storage.cloud.http.JsonHttpException
import biz.ganttproject.storage.cloud.http.JsonTask
import biz.ganttproject.storage.cloud.http.ResourceDto
import biz.ganttproject.storage.cloud.http.loadTeamResources
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import kotlinx.coroutines.*
import net.sourceforge.ganttproject.action.CancelAction
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.resource.HumanResourceManager
import java.awt.event.ActionEvent
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

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
  private val dialog: DialogController,
  private val resourceManager: HumanResourceManager,
  private val isInProject: (ResourceDto) -> Boolean,
  private val coroutineScope: CoroutineScope
) : FlowPage() {

  private val listView = ListView<ResourceDto>().apply {
    setCellFactory {  ResourceListCell(isInProject, this@ResourceListPage::onCheckedToggle) }
  }
  private fun onCheckedToggle() {
    canAddResourcesProperty.value = listView.items.filter { it.isChecked && !isInProject(it) }.any()
  }

  internal val canAddResourcesProperty = SimpleBooleanProperty(false)
  private lateinit var uiFlow: GPCloudUiFlow

  override fun createUi(): Pane =
    vbox {
      addClasses("content-pane")
      add(listView, alignment = null, growth = Priority.ALWAYS)
    }

  override fun resetUi() {
  }

  override fun setController(controller: GPCloudUiFlow) {
    this.uiFlow = controller
  }

  override var active: Boolean = false
    set(value) {
      field = value
      if (value) {
        coroutineScope.launch {
          val stopProgress = dialog.toggleProgress(true)
          withContext(Dispatchers.IO) {
            try {
              loadTeams()
                .map {
                  async { loadTeamResources(it) }
                }
                .awaitAll()
                .reduce { acc, list -> acc + list }
                .distinctBy { it.email }
                .sortedWith(
                  compareBy<ResourceDto> { if (isInProject(it)) 0 else 1 }.thenBy { it.name }
                )
                .let(::fillListView)
            } catch (ex: JsonHttpException) {
              dialog.showAlert(RootLocalizer.create("error.channel.itemTitle"), createAlertBody(ex.message ?: ""))
              fillListView(emptyList())
            } finally {
              stopProgress()
            }
          }
        }
      }
    }

  private fun fillListView(resources: List<ResourceDto>) {
    Platform.runLater {
      listView.items.addAll(resources)
    }
  }

  private fun loadTeams(): List<String> {
    val jsonTeams = JsonTask(
      method = HttpMethod.GET,
      uri = "/team/list",
      kv = mapOf("owned" to "true", "participated" to "true"),
      busyIndicator = {},
      onFailure = {_, _ -> }
    ).execute()

    return if (jsonTeams.isArray) {
      jsonTeams.elements().asSequence().map { it["refid"].asText() }.toList()
    } else emptyList()
  }

  internal fun addResourcesToProject() {
    listView.items.filter { it.isChecked && !isInProject(it) }.forEach {
      this.resourceManager.newResourceBuilder().withEmail(it.email).withName(it.name).withPhone(it.phone).build()
    }
  }

}

/**
 * Builds a UI flow with the main page (resources list) and sign-in and other pages.
 */
class GPCloudResourceListDialog(private val resourceManager: HumanResourceManager) {
  private fun isInProject(dto: ResourceDto): Boolean = resourceManager.resources.find { it.mail == dto.email } != null
  fun show() {
    dialog(id = "cloud.resource.list", title = RootLocalizer.formatText("cloud.resource.list.title")) { dlg ->

      fun handleAsyncException(ctx: CoroutineContext, th: Throwable) {
        dlg.showAlert(RootLocalizer.create("error.channel.itemTitle"), createAlertBody(th.message ?: ""))
      }
      val coroutineScope = CoroutineScope(EmptyCoroutineContext + SupervisorJob() + CoroutineExceptionHandler(::handleAsyncException))

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

      val resourceListPage = ResourceListPage(dlg, resourceManager, this@GPCloudResourceListDialog::isInProject, coroutineScope)
      dlg.setupButton(ButtonType.APPLY) { btn ->
        btn.textProperty().bind(RootLocalizer.create("cloud.resource.list.btnApply"))
        btn.styleClass.add("btn-attention")
        btn.setOnAction {
          resourceListPage.addResourcesToProject()
        }
        btn.disableProperty().bind(resourceListPage.canAddResourcesProperty.not())
      }
      dlg.setupButton(CancelAction.create("cancel") {
        coroutineScope.cancel()
      })

      val wrapper = BorderPane()

      dlg.setContent(wrapper)
      val cloudUiFlow = GPCloudUiFlowBuilder().run {
        flowPageChanger = createFlowPageChanger(wrapper, dlg)
        mainPage = resourceListPage
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
private class ResourceListCell(
  private val isInProject: (ResourceDto)-> Boolean,
  private val onCheckedToggle: ()->Unit) : ListCell<ResourceDto>() {
  private val checkBox = CheckBox()
  private var isChecked = SimpleBooleanProperty()

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
      checkBox.selectedProperty().bindBidirectional(isChecked)
      children.add(
        VBoxBuilder("labels").apply {
          add(Label(item.name).also { it.styleClass.add("name") })
          add(Label(item.email).also { it.styleClass.add("email") })
        }.vbox.also {
          it.onMouseClicked = EventHandler { evt ->
            if (evt.clickCount == 2 && !item.isReadOnly) {
              isChecked.value = !isChecked.value
              item.isChecked = isChecked.value
              onCheckedToggle()
            }
          }
        }
      )
      if (isInProject(item)) {
        isChecked.value = true
        this.isDisable = true
      }
    }
  }
}

private val i18n = RootLocalizer.createWithRootKey("cloud.resource.list")
