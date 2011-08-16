/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package net.sourceforge.ganttproject.resource;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.roles.Role;

public class ResourceNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 3834033541318392117L;

    private final HumanResource resource;

    public ResourceNode(HumanResource res) {
        super(res);
        resource = res;
    }

    public void setName(String name) {
        resource.setName(name);
    }

    public String getName() {
        return resource.getName();
    }

    public void setPhone(String phoneNumber) {
            resource.setPhone(phoneNumber);
    }

    public String getPhone() {
        return resource.getPhone();
    }

    public void setEMail(String email) {
        resource.setMail(email);
    }

    public String getEMail() {
        return resource.getMail();
    }

    public void setDefaultRole(Role defRole) {
        resource.setRole(defRole);
    }

    public Role getDefaultRole() {
       return resource.getRole();
    }

    /** @return the value of a custom field referenced by its title */
    public Object getCustomField(String title) {
        return resource.getCustomField(title);
    }

    /** sets the new value to the custom field referenced by its title */
    public void setCustomField(String title, Object val) {
        resource.setCustomField(title, val);
    }

    @Override
    public String toString() {
        if (resource != null) {
            return resource.getName();
        }
        return "-";
    }

    public HumanResource getResource() {
        return resource;
    }

    @Override
    public boolean equals(Object obj) {
        boolean res = false;
        if (this == obj) {
            return true;
        }
        if (obj instanceof ResourceNode) {
            ResourceNode rn = (ResourceNode) obj;
            res = rn.getUserObject() != null
                    && rn.getUserObject().equals(this.getUserObject());
        }
        return res;
    }
}
