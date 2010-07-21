package net.sourceforge.ganttproject.task;

import net.sourceforge.ganttproject.resource.ProjectResource;
import net.sourceforge.ganttproject.roles.Role;

/**
 * Created by IntelliJ IDEA.
 * 
 * @author bard Date: 05.02.2004
 */
public interface ResourceAssignment {
    Task getTask();

    ProjectResource getResource();

    float getLoad();

    void setLoad(float load);

    /** Deletes this assignment */
    void delete();
    
    void setCoordinator(boolean responsible);

    boolean isCoordinator();

    Role getRoleForAssignment();

    void setRoleForAssignment(Role role);
}
