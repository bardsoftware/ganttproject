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

    /** Load roles from the file */
    /** Add a role on the list */
    public void add(int ID, String role);

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

    public Role getRole(String roleName);

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
