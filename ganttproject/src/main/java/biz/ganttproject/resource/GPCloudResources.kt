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
import biz.ganttproject.lib.fx.vbox
import biz.ganttproject.storage.cloud.FlowPage
import biz.ganttproject.storage.cloud.GPCloudUiFlow
import biz.ganttproject.storage.cloud.GPCloudUiFlowBuilder
import biz.ganttproject.storage.cloud.createFlowPageChanger
import biz.ganttproject.storage.cloud.http.JsonHttpException
import biz.ganttproject.storage.cloud.http.ResourceDto
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import kotlinx.coroutines.*
import kotlinx.coroutines.javafx.JavaFx
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.resource.HumanResourceManager
import java.awt.event.ActionEvent
import kotlin.coroutines.CoroutineContext

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
private class ResourceListPage(
  private val dialog: DialogController,
  private val resourceManager: HumanResourceManager,
  private val coroutineScope: CoroutineScope
) : FlowPage() {

  private val listView = ListView<ResourceDto>().apply {
    setCellFactory {  ResourceListCell(this@ResourceListPage::onCheckedToggle) }
    // TODO: add content for the empty list case
  }
  private fun onCheckedToggle() {
    canAddResourcesProperty.value = listView.items.any { it.isChecked && !it.isReadOnly }
  }

  val canAddResourcesProperty = SimpleBooleanProperty(false)
  private val model = GPCloudResourceListModel(resourceManager)

  override fun createUi(): Pane =
    vbox {
      addClasses("content-pane")
      add(listView, alignment = null, growth = Priority.ALWAYS)
    }

  override fun resetUi() {}

  override fun setController(controller: GPCloudUiFlow) {}

  override var active: Boolean = false
    set(value) {
      field = value
      if (value) {
        coroutineScope.launch {
          val stopProgress = dialog.toggleProgress(true)
          try {
            model.loadResources().let(::fillListView)
          } catch (ex: Exception) {
            if (ex is CancellationException) {
              throw ex
            }
            dialog.showAlert(RootLocalizer.create("error.channel.itemTitle"), createAlertBody(ex.message ?: ""))
            fillListView(emptyList())
          } finally {
            stopProgress()
          }
        }
      }
    }

  private fun fillListView(resources: List<ResourceDto>) {
    FXThread.runLater {
      listView.items.setAll(resources)
      onCheckedToggle()
    }
  }

  fun addResourcesToProject() {
    listView.items.filter { it.isChecked && !it.isReadOnly }.forEach {
      this.resourceManager.newResourceBuilder().withEmail(it.email).withName(it.name).withPhone(it.phone).build()
    }
  }
}

/**
 * Builds a UI flow with the main page (resources list) and sign-in and other pages.
 */
class GPCloudResourceListDialog(private val resourceManager: HumanResourceManager) {
  fun show() {
    dialog(id = "cloud.resource.list", title = ourLocalizer.formatText("title")) { dlg ->

      fun handleAsyncException(ctx: CoroutineContext, th: Throwable) {
        dlg.showAlert(RootLocalizer.create("error.channel.itemTitle"), createAlertBody(th.message ?: th.javaClass.name))
      }
      val coroutineScope = CoroutineScope(Dispatchers.JavaFx + SupervisorJob() + CoroutineExceptionHandler(::handleAsyncException))

      dlg.addStyleClass("dlg-cloud-resource-list")
      dlg.addStyleSheet(
        "/biz/ganttproject/resource/GPCloudResources.css"
      )
      dlg.setHeader(
        vbox {
          this.i18n = ourLocalizer
          addClasses("header")
          addTitle(LocalizedString("title", i18n)).also { hbox ->
            hbox.alignment = Pos.CENTER_LEFT
            hbox.isFillHeight = true
          }
        }
      )

      val resourceListPage = ResourceListPage(dlg, resourceManager, coroutineScope)
      dlg.setupButton(ButtonType.APPLY) { btn ->
        btn.textProperty().bind(ourLocalizer.create("btnApply"))
        btn.styleClass.add("btn-attention")
        btn.setOnAction {
          resourceListPage.addResourcesToProject()
        }
        btn.disableProperty().bind(resourceListPage.canAddResourcesProperty.not())
      }

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
      dlg.onClosed = {
        coroutineScope.cancel()
      }
    }
  }
}

/**
 * Renders a cell in the list of resources.
 */
private class ResourceListCell(
  private val onCheckedToggle: ()->Unit) : ListCell<ResourceDto>() {
  private val checkBox = CheckBox()
  private val isChecked = SimpleBooleanProperty().also {
    checkBox.selectedProperty().bindBidirectional(it)
    it.addListener { _, _, newValue ->
      item?.let { it.isChecked = newValue }
      onCheckedToggle()
    }
  }
  private val resourceName = SimpleStringProperty()
  private val resourceEmail = SimpleStringProperty()

  private val nameBox = vbox {
    addClasses("labels")
    add(Label().also {
      it.styleClass.add("name")
      it.textProperty().bind(resourceName)
    })
    add(Label().also {
      it.styleClass.add("email")
      it.textProperty().bind(resourceEmail)
    })
  }
  private val cellGraphic = HBox().apply {
    styleClass.add("resource-cell")
    children.add(checkBox)
    children.add(nameBox)
  }

  override fun updateItem(item: ResourceDto?, empty: Boolean) {
    super.updateItem(item, empty)

    if (empty || item == null) {
      text = null
      graphic = null
      return
    }
    isChecked.value = item.isChecked
    this.isDisable = item.isReadOnly
    resourceName.value = item.name
    resourceEmail.value = item.email
    graphic = cellGraphic
  }
}

private val ourLocalizer = RootLocalizer.createWithRootKey("cloud.resource.list")