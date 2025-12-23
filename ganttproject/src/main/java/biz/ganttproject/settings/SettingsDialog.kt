/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.settings

import biz.ganttproject.FXUtil
import biz.ganttproject.FxUiComponent
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.dialog
import biz.ganttproject.colorFromUiManager
import biz.ganttproject.core.option.ObservableObject
import biz.ganttproject.ganttview.*
import biz.ganttproject.ganttview.ItemListDialogPane
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.embed.swing.SwingNode
import javafx.geometry.Insets
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundFill
import javafx.scene.layout.BorderPane
import javafx.scene.layout.CornerRadii
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.CancelAction
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.gui.options.OptionPageProviderPanel
import net.sourceforge.ganttproject.gui.options.model.OptionPageProvider
import net.sourceforge.ganttproject.plugins.PluginManager
import javax.swing.SwingUtilities

data class OptionPageItem(
  override var title: String,
  override val isEnabledProperty: BooleanProperty = SimpleBooleanProperty(true),
  val provider: OptionPageProvider?,
  val project: IGanttProject,
  val uiFacade: UIFacade,
  val isHeader: Boolean = provider == null
): Item<OptionPageItem> {

  val fxNode by lazy {
    provider?.let {
      if (it is FxUiComponent) {
        it.init(project, uiFacade)
        it.buildNode()
      } else {
        SwingNode().also {
          SwingUtilities.invokeLater {
            it.content = OptionPageProviderPanel(provider, project, uiFacade).component
          }
        }
      }
    } ?: Label(title)
  }
}

class OptionPageUi(editItem: ObservableObject<OptionPageItem?>, var resize: ()->Unit): ItemEditorPane {
  private val borderPane = BorderPane()
  override val node: Node
    get() = borderPane

  init {
    editItem.addWatcher { event ->
      event.newValue?.let {
        if (it != event.oldValue) {
          FXUtil.transitionCenterPane(borderPane, it.fxNode) {
            FXUtil.runLater(500) {
              resize()
              it.fxNode.requestFocus()
              if (borderPane.width != 0.0) {
                resize = {}
              }
            }
          }
          it.provider?.setActive(true)
        }
      }
    }
  }
  override fun focus() {
  }
}

class SettingsDialogFx(private val project: IGanttProject,
                       private val uiFacade: UIFacade,
                       private val pageOrderKey: String,
                       private val titleKey: String) {
  fun showSettingsDialog() {
    dialog(id = "settings", title = ourLocalizer.formatText(titleKey)) { dialogApi ->
      val listItems = FXCollections.observableArrayList(getPageProviders())
      val editItem = ObservableObject<OptionPageItem?>("", null)
      val dialogModel = ItemListDialogModel<OptionPageItem>(
        listItems,
        newItemFactory = { null },
        ourLocalizer
      )
      dialogModel.btnApplyController.onAction = {
        listItems.forEach { it.provider?.commit() }
      }


      val dialogPane = ItemListDialogPane<OptionPageItem>(
        listItems,
        editItem,
        { item -> ShowHideListItem(
          { item.title },
          { true },
          { },
          if (item.isHeader) "settings-item-header" else "settings-item-page",
          false
        )},
        dialogModel,
        OptionPageUi(editItem, dialogApi::resize),
        ourLocalizer
      )

      dialogApi.addStyleClass("dlg-settings")
      dialogApi.addStyleSheet(
        "/biz/ganttproject/app/Dialog.css",
        "/biz/ganttproject/app/Util.css",
        "/biz/ganttproject/settings/SettingsDialog.css"
      )

      dialogPane.isHeaderEnabled = false
      dialogPane.isAddRemoveEnabled = false
      dialogPane.contentNode.background = Background(BackgroundFill("Panel.background".colorFromUiManager(), CornerRadii.EMPTY, Insets.EMPTY))
      dialogPane.build(dialogApi)

      dialogApi.setupButton(CancelAction.create("cancel") {})
    }
  }

  private fun getPageProviders() =
    ourLocalizer.formatText(pageOrderKey).split(",").map { pageId ->
      if (pageId.startsWith("pageGroup.")) {
        OptionPageItem(title = ourLocalizer.formatText(pageId), provider = null, project = project, uiFacade = uiFacade)
      } else {
        ourProviders.first { it.pageID == pageId }.let { provider ->
          OptionPageItem(provider.toString(), provider = provider, project = project, uiFacade = uiFacade)
        }
      }
    }.toList()
}


private val ourProviders = PluginManager.getExtensions("net.sourceforge.ganttproject.OptionPageProvider",
OptionPageProvider::class.java)

private val ourLocalizer = RootLocalizer