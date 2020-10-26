/*
Copyright 2003-2012 GanttProject Team

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
package net.sourceforge.ganttproject.parser;

import java.util.Set;

import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RolePersistentID;
import net.sourceforge.ganttproject.roles.RoleSet;

import org.xml.sax.Attributes;

import com.google.common.collect.ImmutableSet;

/** Class to parse the attributes of resources handler */
public class RoleTagHandler  extends AbstractTagHandler {
  private final Set<String> TAGS = ImmutableSet.of("roles", "role");
  private RoleSet myRoleSet;
  private final RoleManager myRoleManager;

  public RoleTagHandler(RoleManager roleManager) {
    super(null, false);
    myRoleManager = roleManager;
    myRoleManager.clear(); // Cleanup the old stuff
  }

  /**
   * @see net.sourceforge.ganttproject.parser.TagHandler#endElement(String,
   *      String, String)
   */
  @Override
  public void endElement(String namespaceURI, String sName, String qName) {
    if (!TAGS.contains(qName)) {
      return;
    }
    if (qName.equals("roles")) {
      clearRoleSet();
    }
    setTagStarted(false);
  }

  private void clearRoleSet() {
    myRoleSet = null;
  }

  /**
   * @see net.sourceforge.ganttproject.parser.TagHandler#startElement(String,
   *      String, String, Attributes)
   */
  @Override
  public void startElement(String namespaceURI, String sName, String qName, Attributes attrs) {
    if (!TAGS.contains(qName)) {
      return;
    }
    setTagStarted(true);
    if (qName.equals("roles")) {
      findRoleSet(attrs.getValue("roleset-name"));
    } else if (qName.equals("role")) {
      loadRoles(attrs);
    }
  }

  private void findRoleSet(String roleSetName) {
    if (roleSetName == null) {
      myRoleSet = myRoleManager.getProjectRoleSet();
    } else {
      myRoleSet = myRoleManager.getRoleSet(roleSetName);
      if (myRoleSet == null) {
        myRoleSet = myRoleManager.createRoleSet(roleSetName);
      }
      myRoleSet.setEnabled(true);
    }
  }

  /** Las a role */
  private void loadRoles(Attributes atts) {
    String roleName = atts.getValue("name");
    RolePersistentID persistentID = new RolePersistentID(atts.getValue("id"));
    Role existingRole = myRoleSet.findRole(persistentID.getRoleID());
    if (existingRole == null) {
      myRoleSet.createRole(roleName, persistentID.getRoleID());
    }
  }
}
