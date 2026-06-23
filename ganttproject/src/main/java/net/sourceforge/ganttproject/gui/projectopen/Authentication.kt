/*
Copyright 2026 Dmitry Barashev,  BarD Software s.r.o

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
package net.sourceforge.ganttproject.gui.projectopen

import biz.ganttproject.app.dialog
import biz.ganttproject.storage.cloud.EmptyFlowPage
import biz.ganttproject.storage.cloud.GPCloudUiFlowBuilder
import biz.ganttproject.storage.cloud.createFlowPageChanger
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Pane

/**
 * Shows a dialog with authentication options for cloud storage.
 * In case of successful authentication, calls the provided callback.
 */
fun signinDialog(onAuth: ()->Unit) {
  dialog { controller ->
    val wrapper = BorderPane()
    controller.addStyleClass("dlg-lock", "dlg-cloud-file-options")
    controller.addStyleSheet(
      "/biz/ganttproject/storage/cloud/GPCloudStorage.css",
      "/biz/ganttproject/storage/StorageDialog.css"
    )
    wrapper.center = Pane().also {
      it.prefHeight = 400.0
      it.prefWidth = 400.0
    }
    controller.setContent(wrapper)
    GPCloudUiFlowBuilder().apply {
      flowPageChanger = createFlowPageChanger(wrapper, controller)
      mainPage = object : EmptyFlowPage() {
        override var active: Boolean
          get() = super.active
          set(value) {
            if (value) {
              controller.hide()
              onAuth()
            }
          }
      }
      build().start()
    }
  }
}
