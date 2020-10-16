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
package net.sourceforge.ganttproject.roles;

import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.language.GanttLanguage.Event;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 25.01.2004
 */
public class RoleImpl implements Role {
  private String myName;

  private final int myID;

  private final RoleSet myRoleSet;

  public RoleImpl(int id, String name, RoleSet roleSet) {
    myID = id;
    myName = name;
    myRoleSet = roleSet;

    if (myRoleSet != null) {
      GanttLanguage.getInstance().addListener(new GanttLanguage.Listener() {
        @Override
        public void languageChanged(Event event) {
          Role role = myRoleSet.findRole(myID);
          if (role != null) {
            myName = role.getName();
          }
        }
      });
    }
  }

  @Override
  public int getID() {
    return myID;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public void setName(String name) {
    myName = name;
  }

  @Override
  public String getPersistentID() {
    return (myRoleSet.getName() == null ? "" : myRoleSet.getName() + ":") + getID();
  }

  @Override
  public String toString() {
    return getName();
  }

}
