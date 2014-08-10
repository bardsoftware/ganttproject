/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.gui.projectwizard;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.PrjInfos;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.roles.RoleSet;

public class NewProjectWizard {

  public PrjInfos createNewProject(IGanttProject project, UIFacade uiFacade) {
    RoleSet[] roleSets = project.getRoleManager().getRoleSets();
    NewProjectWizardWindow newProjectWizard = new NewProjectWizardWindow(project, uiFacade, new I18N());
    newProjectWizard.addProjectNamePage(project);
    newProjectWizard.addRoleSetPage(roleSets);
    newProjectWizard.addWeekendConfigurationPage(project);
    newProjectWizard.show();
    return new PrjInfos();
  }

}
