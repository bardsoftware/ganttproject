/*
Copyright 2019 BarD Software s.r.o

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
package net.sourceforge.ganttproject.gui

import com.sandec.mdfx.MDFXNode
import javafx.application.Platform
import javafx.scene.control.ButtonType
import javafx.scene.control.Dialog
import javafx.scene.control.ScrollPane
import javafx.stage.Screen

/**
 * This class shows project opening diagnostics using JavaFX web view.
 *
 * @author dbarashev@bardsoftware.com
 */
internal class ProjectOpenDiagnosticUiFx {
  fun run(msg: String) {
    Platform.runLater {
      val screen = Screen.getPrimary().visualBounds
      Dialog<Unit>().apply {
        val mdfx = MDFXNode(msg)
        mdfx.maxWidth = screen.width / 2
        val content = ScrollPane(mdfx)
        content.maxWidth = screen.width / 2 + 100

        this.dialogPane.let {
          it.content = content
          it.buttonTypes.add(ButtonType.OK)
          it.styleClass.add("dlg-lock")
          it.stylesheets.addAll("/biz/ganttproject/storage/StorageDialog.css", "/biz/ganttproject/storage/cloud/GPCloudStorage.css")
          it.lookupButton(ButtonType.OK).apply {
            styleClass.add("btn-attention")
          }
          it.maxWidth = screen.width / 2 + 100
        }
        this.isResizable = true
        this.show()
      }
    }
  }
}
