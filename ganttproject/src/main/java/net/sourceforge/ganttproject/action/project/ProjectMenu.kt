/*
Copyright 2020 BarD Software s.r.o

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
// 2020-06-03 Translated to Kotlin from ProjectMenu.java

package net.sourceforge.ganttproject.action.project

import biz.ganttproject.print.createPrintAction
import biz.ganttproject.storage.StorageDialogAction
import biz.ganttproject.storage.StorageDialogBuilder
import net.sourceforge.ganttproject.GanttProject
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.document.webdav.WebDavStorageImpl
import javax.swing.Action
import javax.swing.JMenu
import javax.swing.JMenuItem

/**
 * Collection of actions present in the project menu
 */
class ProjectMenu(project: GanttProject, key: String) : JMenu(GPAction.createVoidAction(key)) {
  private val webdavStorage = project.documentManager.webDavStorageUi as WebDavStorageImpl

  private val newProjectAction = NewProjectAction(project)
  val openProjectAction = StorageDialogAction(
      project.project, project.projectUIFacade, project.documentManager, webdavStorage.serversOption,
      StorageDialogBuilder.Mode.OPEN, "project.open"
  )
  val saveProjectAction = SaveProjectAction(project, project.projectUIFacade)

  private val saveAsProjectAction = StorageDialogAction(
      project.project, project.projectUIFacade, project.documentManager, webdavStorage.serversOption,
      StorageDialogBuilder.Mode.SAVE, "project.saveas"
  )

  private val projectSettingsAction = ProjectPropertiesAction(project)
  private val importAction = ProjectImportAction(project.uiFacade, project)
  private val exportAction = ProjectExportAction(
      project.uiFacade, project, project.ganttOptions.pluginPreferences)
  //private val printAction = PrintAction(project)
  //private val printPreviewAction = ProjectPreviewAction(project)
  private val printAction = createPrintAction(project.uiFacade, project.ganttOptions.pluginPreferences)
  private val exitAction = ExitAction(project)

  override fun add(a: Action): JMenuItem {
    a.putValue(Action.SHORT_DESCRIPTION, null)
    return super.add(a)
  }

  init {
    listOf(
        newProjectAction, openProjectAction, saveProjectAction, saveAsProjectAction, projectSettingsAction,
        null,
        importAction, exportAction, printAction,//, printPreviewAction,
        null,
        exitAction
    ).forEach { if (it == null) addSeparator() else add(it) }
    toolTipText = null
  }
}
