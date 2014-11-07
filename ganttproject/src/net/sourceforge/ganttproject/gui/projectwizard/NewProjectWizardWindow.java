/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

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
package net.sourceforge.ganttproject.gui.projectwizard;

import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.calendar.ImportCalendarOption;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.roles.RoleSet;

public class NewProjectWizardWindow extends WizardImpl {
  private I18N myI18n;
  private GPCalendarCalc myCalendar;
  private IGanttProject myProject;

  public NewProjectWizardWindow(IGanttProject project, UIFacade uiFacade, I18N i18n) {
    super(uiFacade, i18n.getNewProjectWizardWindowTitle());
    myI18n = i18n;
    myProject = project;
  }

  public void addRoleSetPage(RoleSet[] roleSets) {
    WizardPage roleSetPage = new RoleSetPage(roleSets, myI18n);
    addPage(roleSetPage);
  }

  public void addProjectNamePage(IGanttProject project) {
    WizardPage projectNamePage = new ProjectNamePage(project, myI18n);
    addPage(projectNamePage);
  }

  public void addWeekendConfigurationPage(IGanttProject project) {
    myCalendar = project.getActiveCalendar().copy();
    WizardPage weekendPage;
    try {
      weekendPage = new WeekendConfigurationPage(myCalendar, myI18n, getUIFacade());
      addPage(weekendPage);
    } catch (Exception e) {
      getUIFacade().showErrorDialog(e);
    }
  }

  @Override
  protected void onOkPressed() {
    super.onOkPressed();
    myProject.getActiveCalendar().importCalendar(myCalendar, new ImportCalendarOption(ImportCalendarOption.Values.REPLACE));
  }


}
