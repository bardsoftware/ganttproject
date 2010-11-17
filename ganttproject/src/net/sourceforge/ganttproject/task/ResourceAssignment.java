package net.sourceforge.ganttproject.task;

import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.roles.Role;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 05.02.2004
 */
public interface ResourceAssignment {
    Task getTask();

    HumanResource getResource();

    float getLoad();

    void setLoad(float load);

    /** Deletes this assignment */
    void delete();
    
    void setCoordinator(boolean responsible);

    boolean isCoordinator();

    Role getRoleForAssignment();

    void setRoleForAssignment(Role role);
}
