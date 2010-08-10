package net.sourceforge.ganttproject.resource;

import javax.swing.tree.DefaultMutableTreeNode;

import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;

public class AssignmentNode extends DefaultMutableTreeNode {

    private final ResourceAssignment resourceAssignment;

    public AssignmentNode(ResourceAssignment res) {
        super(res);
        resourceAssignment = res;
    }

    public ResourceAssignment getAssignment() {
        return resourceAssignment;
    }

    public Role getRoleForAssigment() {
        return resourceAssignment.getRoleForAssignment();
    }

    public void setRoleForAssigment(Role role) {
        resourceAssignment.setRoleForAssignment(role);
    }

    public Task getTask() {
        return resourceAssignment.getTask();
    }

    public String toString() {
        return resourceAssignment.getTask().getName();
    }

}
