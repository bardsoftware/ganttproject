package net.sourceforge.ganttproject.resource;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.roles.Role;

public class ResourceNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 3834033541318392117L;

    private final ProjectResource resource;

    public ResourceNode(ProjectResource res) {
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
        if (resource instanceof HumanResource)
            ((HumanResource) resource).setPhone(phoneNumber);
    }

    public String getPhone() {
        if (resource instanceof HumanResource)
            return ((HumanResource) resource).getPhone();
        return null;
    }

    public void setEMail(String email) {
        if (resource instanceof HumanResource)
            ((HumanResource) resource).setMail(email);
    }

    public String getEMail() {
        if (resource instanceof HumanResource)
            return ((HumanResource) resource).getMail();
        return null;
    }

    public void setDefaultRole(Role defRole) {
        if (resource instanceof HumanResource)
            ((HumanResource) resource).setRole(defRole);
    }

    public Role getDefaultRole() {
        if (resource instanceof HumanResource)
            return ((HumanResource) resource).getRole();
        return null;
    }
    
    /* gets the value of a custom field referenced by it's title */
    public Object getCustomField(String title) {
    	if (resource instanceof HumanResource)
            return ((HumanResource) resource).getCustomFieldVal(title);
        return null;
    }
    
    /* gets the new value to the custom field referenced by it's title */
    public void setCustomField(String title, Object val) {
    	if (resource instanceof HumanResource)
            ((HumanResource) resource).setCustomFieldVal(title, val);
    }

    /**
     * @inheritDoc
     */
    public String toString() {
        if (resource != null)
            return resource.getName();
        return "-";
    }

    public ProjectResource getResource() {
        return resource;
    }

    public boolean equals(Object obj) {
        boolean res = false;
        if (this == obj)
            return true;
        if (obj instanceof ResourceNode) {
            ResourceNode rn = (ResourceNode) obj;
            res = rn.getUserObject() != null
                    && rn.getUserObject().equals(this.getUserObject());
        }
        return res;
    }
}
