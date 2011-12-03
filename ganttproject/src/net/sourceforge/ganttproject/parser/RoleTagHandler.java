/***************************************************************************
 ResourceTagHandler.java  -  description
 -------------------
 begin                : may 2003

 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package net.sourceforge.ganttproject.parser;

import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RolePersistentID;
import net.sourceforge.ganttproject.roles.RoleSet;

import org.xml.sax.Attributes;

/** Class to parse the attributes of resources handler */
public class RoleTagHandler implements TagHandler {
    private RoleSet myRoleSet;
    private final RoleManager myRoleManager;

    public RoleTagHandler(RoleManager roleManager) {
        myRoleManager = roleManager;
        myRoleManager.clear(); // Cleanup the old stuff
    }

    /**
     * @see net.sourceforge.ganttproject.parser.TagHandler#endElement(String,
     *      String, String)
     */
    @Override
    public void endElement(String namespaceURI, String sName, String qName) {
        if (qName.equals("roles")) {
            clearRoleSet();
        }
    }

    private void clearRoleSet() {
        myRoleSet = null;
    }

    /**
     * @see net.sourceforge.ganttproject.parser.TagHandler#startElement(String,
     *      String, String, Attributes)
     */
    @Override
    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) {

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
        RolePersistentID persistentID = new RolePersistentID(atts
                .getValue("id"));
        Role existingRole = myRoleSet.findRole(persistentID.getRoleID());
        if (existingRole == null) {
            myRoleSet.createRole(roleName, persistentID.getRoleID());
        }
    }
}
