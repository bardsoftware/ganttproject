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

class GPCloudResourceListAction(private val resourceManager: HumanResourceManager) : GPAction("cloud.resource.list.action") {
  override fun actionPerformed(e: ActionEvent?) {
    GPCloudResourceListDialog(resourceManager).show()
  }
}

class ResourceListPage(
  private val listView: ListView<ResourceDto>,
  private val controller: DialogController,
  private val resource2selected: MutableMap<String, BooleanProperty>,
  private val resourceManager: HumanResourceManager
) : FlowPage() {
  private lateinit var controllerr: GPCloudUiFlow

  override fun createUi(): Pane =
    VBoxBuilder("content-pane").apply {
      add(listView, alignment = null, growth = Priority.ALWAYS)
    }.vbox

  override fun resetUi() {
  }

  override fun setController(controller: GPCloudUiFlow) {
    this.controllerr = controller
  }

  override var active: Boolean = false
    get() = super.active
    set(value) {
      if (value) {
        field = value
        GlobalScope.launch(Dispatchers.IO) {
          val stopProgress = controller.toggleProgress(true)
          val allResources = try {
            loadTeams(controller).map {
              async { loadTeamResources(it) }
            }.map { it.await() }.reduce { acc, list -> acc + list }.distinctBy { it.email }
          } finally {
            stopProgress()
          }
          fillListView(allResources)
        }

        controller.setupButton(ButtonType.APPLY) { btn ->
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

class GPCloudResourceListDialog(private val resourceManager: HumanResourceManager) {
  private val resource2selected = mutableMapOf<String, BooleanProperty>()
  private val listView = ListView<ResourceDto>().also {
    it.setCellFactory {
      ResourceListCell() {
        resource2selected[it.email]
      }
    }
  }

  fun show() {
    dialog { controller ->
      controller.addStyleClass("dlg-cloud-resource-list")
      controller.addStyleSheet(
        "/biz/ganttproject/resource/GPCloudResources.css"
      )
      controller.setHeader(
        VBoxBuilder("header").apply {
          addTitle(LocalizedString("cloud.resource.list.title", RootLocalizer)).also { hbox ->
            hbox.alignment = Pos.CENTER_LEFT
            hbox.isFillHeight = true
          }
        }.vbox
      )

      val wrapper = BorderPane()

      controller.setContent(wrapper)
      val cloudUiFlow = GPCloudUiFlowBuilder().run {
        wrapperPane = wrapper
        dialog = controller
        mainPage = ResourceListPage(listView, controller, resource2selected, resourceManager)
        build()
      }

      controller.onShown = {
        cloudUiFlow.start()
        controller.resize()
      }
    }
  }

}

class ResourceListCell(private val resource2checked: (ResourceDto) -> BooleanProperty?) : ListCell<ResourceDto>() {
  private val checkBox = CheckBox()
  private var isChecked: BooleanProperty? = null

  override fun updateItem(item: ResourceDto?, empty: Boolean) {
    super.updateItem(item, empty);

    if (empty || item == null) {
      text = null
      graphic = null
      return
    }

    graphic = HBox().apply {
      styleClass.add("resource-cell")
      children.add(checkBox)
      children.add(
        VBoxBuilder("labels").also {
          it.add(Label(item.name).also { it.styleClass.add("name") })
          it.add(Label(item.email).also { it.styleClass.add("email") })
        }.vbox
      )
      isChecked?.let { checkBox.selectedProperty().unbindBidirectional(it) }
      isChecked = resource2checked(item)
      isChecked?.let { checkBox.selectedProperty().bindBidirectional(it) }
    }
  }
}

private val i18n = RootLocalizer.createWithRootKey("cloud.resource.list")
