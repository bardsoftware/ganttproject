/*
Copyright 2019 BarD Software s.r.o

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
package biz.ganttproject.storage

import biz.ganttproject.app.dialog
import biz.ganttproject.storage.cloud.GPCloudStorageOptions
import javafx.application.Platform
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.gui.ProjectUIFacade
import java.awt.event.ActionEvent

/**
 * @author dbarashev@bardsoftware.com
 */
class StorageDialogAction(
    private val myProject: IGanttProject,
    private val myProjectUiFacade: ProjectUIFacade,
    private val myDocumentManager: DocumentManager,
    private val myCloudStorageOptions: GPCloudStorageOptions) : GPAction("myProjects.action") {

  override fun actionPerformed(actionEvent: ActionEvent?) {
    Platform.runLater {
      dialog { dialogBuildApi ->
        val dialogBuilder = StorageDialogBuilder(myProject, myProjectUiFacade, myDocumentManager, myCloudStorageOptions, dialogBuildApi)
        dialogBuilder.build()
      }
    }
  }
}
