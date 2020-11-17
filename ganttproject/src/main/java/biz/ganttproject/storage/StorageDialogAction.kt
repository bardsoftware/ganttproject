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

import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.dialog
import biz.ganttproject.storage.cloud.GPCloudStorageOptions
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.gui.ProjectUIFacade
import net.sourceforge.ganttproject.gui.UIUtil
import java.awt.event.ActionEvent

/**
 * @author dbarashev@bardsoftware.com
 */
class StorageDialogAction(
    private val project: IGanttProject,
    private val projectUiFacade: ProjectUIFacade,
    private val documentManager: DocumentManager,
    private val cloudStorageOptions: GPCloudStorageOptions,
    private val mode: StorageDialogBuilder.Mode,
    private val actionId: String) : GPAction(actionId) {

  override fun actionPerformed(actionEvent: ActionEvent?) {
    dialog(RootLocalizer.create("myProjects.title")) { dialogBuildApi ->
      val dialogBuilder = StorageDialogBuilder(
          project, projectUiFacade, documentManager, cloudStorageOptions, dialogBuildApi
      )
      dialogBuilder.build(mode)
    }
  }

  override fun asToolbarAction(): GPAction {
    return StorageDialogAction(project, projectUiFacade, documentManager, cloudStorageOptions, mode, actionId).also {
      it.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(it))
    }
  }
}
