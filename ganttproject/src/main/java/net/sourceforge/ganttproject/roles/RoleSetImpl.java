/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 GanttProject Team

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

import java.util.ArrayList;

/**
 * @author bard
 */
public class RoleSetImpl implements RoleSet {
  private final String myName;

  private final ArrayList<RoleImpl> myRoles = new ArrayList<RoleImpl>();

  private boolean isEnabled;

  private final RoleManagerImpl myRoleManager;

  RoleSetImpl(String name, RoleManagerImpl roleManager) {
    myName = name;
    myRoleManager = roleManager;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public Role[] getRoles() {
    return myRoles.toArray(new Role[0]);
  }

  @Override
  public Role createRole(String name) {
    int maxId = 0;
    for (Role role : myRoles) {
      if (role.getID() > maxId) {
        maxId = role.getID();
      }
    }
    return createRole(name, maxId + 1);
  }

  @Override
  public Role createRole(String name, int persistentID) {
    RoleImpl result = new RoleImpl(persistentID, name, this);
    myRoles.add(result);
    myRoleManager.fireRolesChanged(this);
    return result;
  }

  @Override
  public void deleteRole(Role role) {
    myRoles.remove(role);
    myRoleManager.fireRolesChanged(this);
  }

  @Override
  public void changeRole(String name, int roleID) {
    Role role = findRole(roleID);
    if (role != null) {
      role.setName(name);
    }
  }

  @Override
  public Role findRole(int roleID) {
    Role result = null;
    for (int i = 0; i < myRoles.size(); i++) {
      Role next = myRoles.get(i);
      if (next.getID() == roleID) {
        result = next;
        break;
      }
    }
    return result;
  }

  @Override
  public boolean isEnabled() {
    return isEnabled;
  }

  @Override
  public String toString() {
    return getName();
  }

  @Override
  public void setEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
    myRoleManager.fireRolesChanged(this);
  }

  @Override
  public boolean isEmpty() {
    return myRoles.isEmpty();
  }

  @Override
  public void clear() {
    myRoles.clear();
  }

  void importData(RoleSet original) {
    Role[] originalRoles = original.getRoles();
    for (int i = 0; i < originalRoles.length; i++) {
      Role nextRole = originalRoles[i];
      createRole(nextRole.getName(), nextRole.getID());
    }
  }
}
