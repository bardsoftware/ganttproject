/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev

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
package net.sourceforge.ganttproject.gui.options;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import biz.ganttproject.core.option.GPOptionGroup;

import net.sourceforge.ganttproject.gui.EditableList;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleImpl;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RoleSet;

/**
 * Provides project roles page in the project settings dialog.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class ProjectRolesOptionPageProvider extends OptionPageProviderBase {
  private EditableList<Role> myRolesList;

  public ProjectRolesOptionPageProvider() {
    super("project.roles");
  }

  @Override
  public GPOptionGroup[] getOptionGroups() {
    return new GPOptionGroup[0];
  }

  @Override
  public boolean hasCustomComponent() {
    return true;
  }

  @Override
  public Component buildPageComponent() {
    ArrayList<Role> roles = new ArrayList<Role>(Arrays.asList(getRoleManager().getProjectLevelRoles()));
    myRolesList = new EditableList<Role>(roles, Collections.<Role> emptyList()) {
      @Override
      protected Role updateValue(Role newValue, Role curValue) {
        curValue.setName(newValue.getName());
        return curValue;
      }

      @Override
      protected Role createValue(Role prototype) {
        RoleSet projectRoles = getRoleManager().getProjectRoleSet();
        return projectRoles.createRole(prototype.getName());
      }

      @Override
      protected void deleteValue(Role value) {
        getRoleManager().getProjectRoleSet().deleteRole(value);
      }

      @Override
      protected Role createPrototype(Object editValue) {
        if (editValue == null) {
          return null;
        }
        return new RoleImpl(0, String.valueOf(editValue), null);
      }

      @Override
      protected String getStringValue(Role role) {
        return role.getName();
      }
    };
    myRolesList.setUndefinedValueLabel(GanttLanguage.getInstance().getText(
        "optionPage.project.roles.undefinedValueLabel"));
    return OptionPageProviderBase.wrapContentComponent(myRolesList.createDefaultComponent(),
        GanttLanguage.getInstance().getText("resourceRole"), GanttLanguage.getInstance().getText("settingsRoles"));
  }

  private RoleManager getRoleManager() {
    return getProject().getRoleManager();
  }

  @Override
  public void commit() {
    if (myRolesList != null) {
      myRolesList.stopEditing();
    }
  }
}
