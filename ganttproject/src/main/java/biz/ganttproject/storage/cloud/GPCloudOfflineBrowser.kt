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
package biz.ganttproject.storage.cloud

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.storage.*
import javafx.event.ActionEvent
import javafx.scene.layout.Pane
import net.sourceforge.ganttproject.document.Document
import java.util.*

/**
 * @author dbarashev@bardsoftware.com
 */
class GPCloudOfflineBrowser(val mode: StorageDialogBuilder.Mode,
                            private val dialogUi: StorageDialogBuilder.DialogUi,
                            private val documentConsumer: (Document) -> Unit) : FlowPage() {
  override fun createUi() = createBrowserPane()

  override fun resetUi() {
  }

  override fun setController(controller: GPCloudUiFlow) {
  }

  private fun createBrowserPane(): Pane {
    val builder = BrowserPaneBuilder<OfflineMirrorOptionsAsFolderItem>(this.mode, this.dialogUi::error) { _, success, _ ->
      loadOfflineMirrors(success)
    }

    val paneElements = builder.apply {
      withI18N(ourLocalizer)
      withBreadcrumbs(DocumentUri(listOf(), true, ourLocalizer.formatText("breadcrumbs.root")))
      withActionButton {}
      withListView(
        onSelectionChange = actionButtonHandler::onSelectionChange,
        itemActionFactory = { Collections.emptyMap() },
        cellFactory = { CellWithBasePath() }
      )
      withActionButton { btn ->
        btn.addEventHandler(ActionEvent.ACTION) {
          actionButtonHandler.onAction()
        }
      }

    }.build()
    paneElements.breadcrumbView?.show()
    paneElements.browserPane.stylesheets.addAll(
      "/biz/ganttproject/storage/cloud/GPCloudStorage.css",
      "/biz/ganttproject/storage/FolderViewCells.css"
    )
    return paneElements.browserPane
  }

  private val actionButtonHandler = object {
    private var selectedProject: FolderItem? = null

    fun onSelectionChange(item: FolderItem) {
      this.selectedProject = item
    }

    fun onAction() {
      selectedProject?.let {
        if (it is OfflineMirrorOptionsAsFolderItem) {
          it.options.offlineMirror?.let { path ->
            documentConsumer(GPCloudDocument(
              teamRefid = null,
              teamName = it.options.teamName,
              projectRefid = it.options.projectRefid,
              projectName = it.name,
              projectJson = null
            ))
          }
        }
      }
    }
  }
}

private val ourLocalizer = RootLocalizer.createWithRootKey("storageService.cloudOffline", BROWSE_PANE_LOCALIZER)
