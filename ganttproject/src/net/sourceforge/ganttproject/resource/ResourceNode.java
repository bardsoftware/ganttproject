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

    /**
     * @inheritDoc
     */
    public String toString() {
        if (resource != null) {
            return resource.getName();
        }
        return "-";
    }

    public HumanResource getResource() {
        return resource;
    }

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
