package biz.ganttproject.resource

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.dialog
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.storage.cloud.HttpMethod
import biz.ganttproject.storage.cloud.http.JsonTask
import biz.ganttproject.storage.cloud.http.ResourceDto
import biz.ganttproject.storage.cloud.http.loadTeamResources
import javafx.scene.control.ButtonType
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.action.GPAction
import java.awt.event.ActionEvent

class GPCloudResourceListAction : GPAction("cloud.resource.list.action") {
  override fun actionPerformed(e: ActionEvent?) {
    GPCloudResourceListDialog().show()
  }
}

class GPCloudResourceListDialog {
  private val listView = ListView<ResourceDto>().also {
    it.setCellFactory {
      ResourceListCell()
    }
  }
  fun show() {
    dialog { controller ->
      VBoxBuilder().apply {
        addTitle("cloud.resource.list.title")
        add(Label().also {
          it.textProperty().bind(this.i18n.create("cloud.resource.list.titleHelp"))
          it.styleClass.add("help")
        })
        add(listView, alignment = null, growth = Priority.ALWAYS)
      }.also {
        controller.setContent(it.vbox)
      }

      controller.setupButton(ButtonType.APPLY) { btn ->
        btn.textProperty().bind(RootLocalizer.create("cloud.resource.list.btnApply"))
        btn.styleClass.add("btn-attention")
        btn.setOnAction {

        }
      }

      controller.beforeShow = {
        GlobalScope.launch(Dispatchers.IO) {
          loadTeams().forEach { refid ->
            listView.items.addAll(loadTeamResources(refid))
          }
        }
      }
    }
  }

  fun loadTeams(): List<String> {
    JsonTask(
      method = HttpMethod.GET,
      uri = "/team/list",
      kv = mapOf("owned" to "true", "participated" to "true"),
      busyIndicator = {  },
      onFailure = {_, resp -> }
    ).execute().let { result ->
      return if (result.isArray) {
        result.elements().asSequence().map { it["refid"].asText() }.toList()
      } else emptyList()
    }

  }
}

class ResourceListCell : ListCell<ResourceDto>() {
  fun whenNotEmpty(item: ResourceDto?, empty: Boolean, code: ResourceListCell.(item: ResourceDto) -> Unit) {
    if (item == null) {
      text = ""
      graphic = null
      return
    }
    super.updateItem(item, empty)
    if (empty) {
      text = ""
      graphic = null
      return
    }
    code(this, item)
  }

  override fun updateItem(item: ResourceDto?, empty: Boolean) {
    whenNotEmpty(item, empty) { item ->
      text = item.name
    }
  }
}
