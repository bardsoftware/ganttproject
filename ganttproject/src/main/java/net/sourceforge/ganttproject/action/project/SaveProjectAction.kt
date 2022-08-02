/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.action.project

import biz.ganttproject.app.BarrierEntrance
import biz.ganttproject.app.Barrier
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.ProjectEventListener
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.gui.ProjectUIFacade
import net.sourceforge.ganttproject.gui.UIUtil
import java.awt.event.ActionEvent

class SaveProjectAction private constructor(
    private val myProject: IGanttProject,
    private val myProjectUiFacade: ProjectUIFacade,
    size: IconSize) : GPAction("project.save", size), ProjectEventListener {
  internal constructor(mainFrame: IGanttProject, projectFacade: ProjectUIFacade) : this(mainFrame, projectFacade, IconSize.MENU)

  init {
    myProject.addProjectEventListener(this)
    isEnabled = false
  }

  override fun getIconFilePrefix(): String {
    return "save_"
  }

  override fun actionPerformed(e: ActionEvent?) {
    if (calledFromAppleScreenMenu(e)) {
      return
    }
    myProjectUiFacade.saveProject(myProject, null)
  }

  override fun projectModified() {
    isEnabled = true
  }

  override fun projectSaved() {
    isEnabled = false
  }

  override fun projectClosed() {
    isEnabled = false
  }

  override fun projectCreated() {
    isEnabled = false
  }

  override fun projectOpened(
    barrierRegistry: BarrierEntrance,
    barrier: Barrier<IGanttProject>
  ) {
    isEnabled = false
  }

  override fun projectRestoring(completion: Barrier<Document>) {}

  override fun asToolbarAction(): SaveProjectAction {
    val result = SaveProjectAction(myProject, myProjectUiFacade)
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result))
    return result
  }
}
