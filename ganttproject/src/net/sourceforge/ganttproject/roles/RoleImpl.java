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
        return (myRoleSet.getName() == null ? "" : myRoleSet.getName() + ":")
                + getID();
    }

    @Override
    public String toString() {
        return getName();
    }

}
