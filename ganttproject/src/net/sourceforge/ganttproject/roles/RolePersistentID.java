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

public class RolePersistentID {
  private static final String ROLESET_DELIMITER = ":";

  private final String myRoleSetID;

  private final int myRoleID;

  public RolePersistentID(String persistentID) {
    int posDelimiter = persistentID.lastIndexOf(ROLESET_DELIMITER);
    String rolesetName = posDelimiter == -1 ? null : persistentID.substring(0, posDelimiter);
    String roleIDasString = posDelimiter == -1 ? persistentID : persistentID.substring(posDelimiter + 1);
    int roleID;
    try {
      roleID = Integer.parseInt(roleIDasString);
    } catch (NumberFormatException e) {
      roleID = 0;
    }
    myRoleID = roleID;
    myRoleSetID = rolesetName;

  }

  public String getRoleSetID() {
    return myRoleSetID;
  }

  public int getRoleID() {
    return myRoleID;
  }

  public String asString() {
    return myRoleSetID + ROLESET_DELIMITER + myRoleID;
  }

}
