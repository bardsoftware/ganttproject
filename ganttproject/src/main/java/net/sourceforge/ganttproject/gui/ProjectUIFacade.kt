/*
GanttProject is an opensource project management tool.
Copyright (C) 2005-2011 GanttProject team

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
package net.sourceforge.ganttproject.gui

import biz.ganttproject.app.Barrier
import biz.ganttproject.core.option.GPOptionGroup
import kotlinx.coroutines.channels.Channel
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.document.Document
import java.io.IOException

typealias AuthenticationFlow = (()->Unit)->Unit

interface ProjectUIFacade {
  fun saveProject(project: IGanttProject, onFinish: Channel<Boolean>?)
  fun saveProjectAs(project: IGanttProject)
  fun ensureProjectSaved(project: IGanttProject): Barrier<Boolean>

  @Throws(IOException::class, Document.DocumentException::class)
  fun openProject(document: Document, project: IGanttProject, onFinish: Channel<Boolean>?, authenticationFlow: AuthenticationFlow? = null)
  fun createProject(project: IGanttProject)
  fun getOptionGroups(): Array<GPOptionGroup>
}
