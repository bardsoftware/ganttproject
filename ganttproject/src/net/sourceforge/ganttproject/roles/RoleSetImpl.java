/*
 * Created on 18.06.2004
 *
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

    public String getName() {
        return myName;
    }

    public Role[] getRoles() {
        return myRoles.toArray(new Role[0]);
    }

    public Role createRole(String name, int persistentID) {
        RoleImpl result = new RoleImpl(persistentID, name, this);
        myRoles.add(result);
        myRoleManager.fireRolesChanged(this);
        return result;
    }

    public void deleteRole(Role role) {
        myRoles.remove(role);
        myRoleManager.fireRolesChanged(this);
    }
    
    public void changeRole(String name, int roleID) {
        Role role = findRole(roleID);
        if (role != null) {
            role.setName(name);
        }
    }

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

    public boolean isEnabled() {
        return isEnabled;
    }

    public String toString() {
        return getName();
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
        myRoleManager.fireRolesChanged(this);
    }

    public boolean isEmpty() {
        return myRoles.isEmpty();
    }

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
