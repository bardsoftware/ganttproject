/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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
package net.sourceforge.ganttproject.roles;

import java.util.EventListener;
import java.util.EventObject;

/**
 * @author athomas
 */
public interface RoleManager {
  public RoleSet createRoleSet(String name);

  public RoleSet[] getRoleSets();

  /** Clear the role list */
  public void clear();

  /** Return all roles except the default roles */
  // public String [] getRolesShort();
  public Role[] getProjectLevelRoles();

  public class Access {
    public static RoleManager getInstance() {
      return ourInstance;
    }

    private static RoleManager ourInstance = new RoleManagerImpl();
  }

  public static int DEFAULT_ROLES_NUMBER = 11;

  public RoleSet getProjectRoleSet();

  public RoleSet getRoleSet(String rolesetName);

  public Role[] getEnabledRoles();

  public Role getDefaultRole();

  public Role getRole(String persistentID);

  public void importData(RoleManager roleManager);

  public void addRoleListener(Listener listener);

  public void removeRoleListener(Listener listener);

  public interface Listener extends EventListener {
    public void rolesChanged(RoleEvent e);
  }

  public class RoleEvent extends EventObject {
    private RoleSet myChangedRoleSet;

    public RoleEvent(RoleManager source, RoleSet changedRoleSet) {
      super(source);
      myChangedRoleSet = changedRoleSet;
    }

    public RoleSet getChangedRoleSet() {
      return myChangedRoleSet;
    }
  }
}
