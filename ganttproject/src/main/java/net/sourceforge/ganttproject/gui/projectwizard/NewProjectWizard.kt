/*
Copyright 2003-2024 Dmitry Barashev, BarD Software s.r.o.

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
package net.sourceforge.ganttproject.gui.projectwizard

import biz.ganttproject.app.Barrier
import biz.ganttproject.app.SimpleBarrier
import biz.ganttproject.core.calendar.GPCalendar
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.gui.UIFacade

data class NewProjectData(
  val name: String,
  val description: String,
  val organization: String,
  val webLink: String,
  val calendar: GPCalendar,
)

/**
 * Opens a wizard that shows a UI for the new project setup and returns a barrier that resolves when user hits OK
 * in the wizard.
 */
fun createNewProject(project: IGanttProject, uiFacade: UIFacade): Barrier<NewProjectData> {
  val result = SimpleBarrier<NewProjectData>()
  val roleSets = project.roleManager.roleSets

  val i18n = I18N()
  val projectNamePage = ProjectNamePage(project, i18n)
  val calendarCopy = project.activeCalendar.copy()
  val weekendPage = WeekendConfigurationPage(calendarCopy, i18n, uiFacade)

  val newProjectWizard = object : WizardImpl(uiFacade, i18n.newProjectWizardWindowTitle) {
    override fun onOkPressed() {
      super.onOkPressed()
      result.resolve(NewProjectData(
        projectNamePage.projectName,
        projectNamePage.projectDescription,
        projectNamePage.projectOrganization,
        projectNamePage.webLink,
        calendarCopy
      ))
    }
  }

  newProjectWizard.addPage(RoleSetPage(roleSets, i18n))
  newProjectWizard.addPage(weekendPage)
  newProjectWizard.show()
  return result
}
