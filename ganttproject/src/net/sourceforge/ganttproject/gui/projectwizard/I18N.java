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

import java.text.MessageFormat;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleSet;

public class I18N {
  public I18N() {
    myDayNames = new String[7];
    for (int i = 0; i < 7; i++) {
      myDayNames[i] = GanttLanguage.getInstance().getDay(i);
    }
  }

  public String getNewProjectWizardWindowTitle() {
    return GanttLanguage.getInstance().getText("createNewProject");
  }

  public String getProjectDomainPageTitle() {
    return GanttLanguage.getInstance().getText("selectProjectDomain");
  }

  public String getProjectWeekendPageTitle() {
    return GanttLanguage.getInstance().getText("selectProjectWeekend");
  }

  public String getRolesetTooltipHeader(String roleSetName) {
    return MessageFormat.format("<html><body><h3>{0}</h3><ul>", (Object[]) new String[] { roleSetName });
  }

  public String getRolesetTooltipFooter() {
    return "</ul></body></html>";
  }

  public String formatRoleForTooltip(Role role) {
    return MessageFormat.format("<li>{0}</li>", (Object[]) new String[] { role.getName() });
  }

  String[] getDayNames() {
    return myDayNames;
    // DateFormatSymbols symbols = new
    // DateFormatSymbols(Locale.getDefault());
    // return symbols.getWeekdays();
  }

  final String[] myDayNames;

  public String getRoleSetDisplayName(RoleSet roleSet) {
    return GanttLanguage.getInstance().getText("roleSet." + roleSet.getName() + ".displayName");
  }
}
