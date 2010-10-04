/*
 * Created on 05.07.2003
 *
 */
package net.sourceforge.ganttproject.parser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.ResourceManager;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.roles.RolePersistentID;
import net.sourceforge.ganttproject.roles.RoleSet;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import org.xml.sax.Attributes;

/**
 * @author bard
 */
public class AllocationTagHandler implements TagHandler, ParsingListener {
    public AllocationTagHandler(ResourceManager resourceMgr,
            TaskManager taskMgr, RoleManager roleMgr) {
        myResourceManager = resourceMgr;
        myTaskManager = taskMgr;
        myRoleManager = roleMgr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.ganttproject.parser.TagHandler#startElement(java.lang.String,
     *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String namespaceURI, String sName, String qName,
            Attributes attrs) throws FileFormatException {

        if (qName.equals("allocation")) {
            loadAllocation(attrs);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sourceforge.ganttproject.parser.TagHandler#endElement(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public void endElement(String namespaceURI, String sName, String qName) {
        // TODO Auto-generated method stub

    }

    private void loadAllocation(Attributes attrs) throws FileFormatException {
        String aName;
        int taskId = 0;
        int resourceId = 0;
        float load = 0;
        boolean coordinator = false;

        String taskIdAsString = attrs.getValue("task-id");
        String resourceIdAsString = attrs.getValue("resource-id");
        String loadAsString = attrs.getValue("load");
        String coordinatorAsString = attrs.getValue("responsible");
        String rolePersistendIDString = attrs.getValue("function");

        if (taskIdAsString == null || resourceIdAsString == null) {
            throw new FileFormatException(
                    "Failed to load <allocation> tag: task or resource identifier is missing");
        }

        try {
            taskId = Integer.parseInt(taskIdAsString);
            resourceId = Integer.parseInt(resourceIdAsString);

            if (loadAsString != null) {
                load = Float.parseFloat(loadAsString);
            }
            if (coordinatorAsString != null) {
                coordinator = Boolean.valueOf(coordinatorAsString)
                        .booleanValue();
            }

        } catch (NumberFormatException e) {
            throw new FileFormatException(
                    "Failed to load <allocation> tag: one of attribute values is invalid",
                    e);
        }

        // if no load is specified I assume 100% load
        // this should only be the case if old files
        // were loaded.
        if (load == 0) {
            load = 100;
        }

        HumanResource human = (HumanResource) getResourceManager().getById(
                resourceId);
        if (human == null) {
            throw new FileFormatException("Human resource with id="
                    + resourceId + " not found");
        }

        Task task = getTaskManager().getTask(taskId);
        if (task == null) {
            throw new FileFormatException("Task with id=" + taskId
                    + " not found");
        }
        // TaskMutator mutator = task.createMutator();
        // ResourceAssignment assignment = mutator.addResource(human);
        // assignment.setLoad(load);
        // mutator.commit();

        ResourceAssignment assignment = task.getAssignmentCollection()
                .addAssignment(human);

        try {
            if (rolePersistendIDString != null)
                myLateAssigmnent2roleBinding.put(assignment,
                        rolePersistendIDString);
        } catch (NumberFormatException e) {
            System.out
                    .println("ERROR in parsing XML File function id is not numeric: "
                            + e.toString());
        }

        assignment.setLoad(load);
        assignment.setCoordinator(coordinator);
    }

    private ResourceManager getResourceManager() {
        return myResourceManager;
    }

    private TaskManager getTaskManager() {
        return myTaskManager;
    }

    private Role findRole(String persistentIDasString) {
        //
        RolePersistentID persistentID = new RolePersistentID(
                persistentIDasString);
        String rolesetName = persistentID.getRoleSetID();
        int roleID = persistentID.getRoleID();
        RoleSet roleSet;
        if (rolesetName == null) {
            roleSet = myRoleManager.getProjectRoleSet();
            if (roleSet.findRole(roleID) == null) {
                if (roleID <= 10 && roleID > 2) {
                    roleSet = myRoleManager
                            .getRoleSet(RoleSet.SOFTWARE_DEVELOPMENT);
                    roleSet.setEnabled(true);
                } else if (roleID <= 2) {
                    roleSet = myRoleManager.getRoleSet(RoleSet.DEFAULT);
                }
            }
        } else {
            roleSet = myRoleManager.getRoleSet(rolesetName);
        }
        Role result = roleSet.findRole(roleID);
        return result;
    }

    public void parsingStarted() {
        // TODO Auto-generated method stub

    }

    public void parsingFinished() {
        for (Iterator lateBindingEntries = myLateAssigmnent2roleBinding
                .entrySet().iterator(); lateBindingEntries.hasNext();) {
            Map.Entry nextEntry = (Entry) lateBindingEntries.next();
            String persistentID = (String) nextEntry.getValue();
            Role nextRole = findRole(persistentID);
            if (nextRole != null) {
                lateBindingEntries.remove();
                ((ResourceAssignment) nextEntry.getKey())
                        .setRoleForAssignment(nextRole);
            }
        }
        if (!myLateAssigmnent2roleBinding.isEmpty()) {
            System.err
                    .println("[ResourceTagHandler] parsingFinished(): not found roles:\n"
                            + myLateAssigmnent2roleBinding);
        }

    }

    private ResourceManager myResourceManager;

    private TaskManager myTaskManager;

    private RoleManager myRoleManager;

    private final HashMap<ResourceAssignment, String> myLateAssigmnent2roleBinding = new HashMap<ResourceAssignment, String>();

}
