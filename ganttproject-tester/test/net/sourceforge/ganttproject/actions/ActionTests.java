package net.sourceforge.ganttproject.actions;

import java.util.ArrayList;

import net.sourceforge.ganttproject.action.resource.AssignmentToggleAction;
import net.sourceforge.ganttproject.action.resource.ResourceDeleteAction;
import net.sourceforge.ganttproject.action.resource.ResourceNewAction;
import net.sourceforge.ganttproject.action.task.TaskDeleteAction;
import net.sourceforge.ganttproject.action.task.TaskNewAction;

import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.*;

import javax.swing.*;


public class ActionTests extends ScenarioTestCase {
    public void testTaskNewAction(){
        TaskManager taskManager = getTaskManager();
        TaskNewAction taskNewAction = makeNewTaskAction();

        assertEquals(0, taskManager.getTaskCount());

        taskNewAction.actionPerformed(null);

        assertEquals(1, taskManager.getTaskCount());

        taskNewAction.actionPerformed(null);

        assertEquals(2, taskManager.getTaskCount());
    }

    public void testTaskDeleteActionWithSelectedTask() {
        ArrayList<Task> selection;
        TaskManager taskManager = getTaskManager();
        TaskDeleteAction taskDeleteAction = makeDeleteTaskAction();
        TaskNewAction taskNewAction = makeNewTaskAction();

        taskNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);

        selection = new ArrayList<>();

        assertEquals(2, taskManager.getTaskCount());

        selection.add(taskManager.getTasks()[1]);
        taskDeleteAction.selectionChanged(selection);
        taskDeleteAction.actionPerformed(null);

        assertEquals(1, taskManager.getTaskCount());
    }

    public void testTaskDeleteActionWithoutSelectedTask() {
        TaskManager taskManager = getTaskManager();
        TaskDeleteAction taskDeleteAction = makeDeleteTaskAction();
        TaskNewAction taskNewAction = makeNewTaskAction();

        taskNewAction.actionPerformed(null);

        assertEquals(1, taskManager.getTaskCount());

        taskDeleteAction.selectionChanged(new ArrayList<>());
        taskDeleteAction.actionPerformed(null);

        assertEquals(1, taskManager.getTaskCount());
    }

    public void testResourceNewAction() {
        HumanResourceManager resourceManager = getHumanResourceManger();
        ResourceNewAction resourceNewAction = makeNewResourceAction();

        resourceNewAction.actionPerformed(null);

        assertEquals(1, resourceManager.getResources().size());

        resourceNewAction.actionPerformed(null);

        assertEquals(2, resourceManager.getResources().size());
    }

    public void testResourceDeleteAction(){
        HumanResourceManager resourceManager = getHumanResourceManger();
        ResourceNewAction resourceNewAction = makeNewResourceAction();
        ResourceDeleteAction resourceDeleteAction = makeDeleteResourceAction();

        resourceNewAction.actionPerformed(null);
        resourceNewAction.actionPerformed(null);

        HumanResource[] resources = resourceManager.getResourcesArray();

        assertEquals(2, resourceManager.getResources().size());

        addHumanResourceToSelection(resources[1]);
        resourceDeleteAction.actionPerformed(null);

        assertEquals(1, resourceManager.getResources().size());

        resetHumanResourceSelection();
        addHumanResourceToSelection(resources[0]);
        resourceDeleteAction.actionPerformed(null);

        assertEquals(0, resourceManager.getResources().size());
    }

    public void  testResourceDeleteActionWithoutSelectedResource(){
        HumanResourceManager resourceManager = getHumanResourceManger();
        ResourceNewAction resourceNewAction = makeNewResourceAction();
        ResourceDeleteAction resourceDeleteAction = makeDeleteResourceAction();

        resourceNewAction.actionPerformed(null);
        resourceNewAction.actionPerformed(null);

        assertEquals(2, resourceManager.getResources().size());

        resourceDeleteAction.actionPerformed(null);

        assertEquals(2, resourceManager.getResources().size());
    }

    public void testAssignmentToggleAction(){
        HumanResourceManager resourceManager = getHumanResourceManger();
        TaskManager taskManager = getTaskManager();
        ResourceNewAction resourceNewAction = makeNewResourceAction();
        TaskNewAction taskNewAction = makeNewTaskAction();

        resourceNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);

        HumanResource resource = resourceManager.getResourcesArray()[0];
        Task task = taskManager.getTask(0);

        assertEquals(0, task.getAssignments().length);

        AssignmentToggleAction assignmentToggleAction = makeAssignmentToggleAction(resource, task);
        assignmentToggleAction.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction.actionPerformed(null);

        assertEquals(1, task.getAssignments().length);

        assignmentToggleAction.putValue(Action.SELECTED_KEY, false);
        assignmentToggleAction.actionPerformed(null);

        assertEquals(0, task.getAssignments().length);
    }

    public void testDeletionOfAssignedResources(){
        HumanResourceManager resourceManager = getHumanResourceManger();
        TaskManager taskManager = getTaskManager();
        ResourceNewAction resourceNewAction = makeNewResourceAction();
        TaskNewAction taskNewAction = makeNewTaskAction();

        resourceNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);

        HumanResource resource = resourceManager.getResourcesArray()[0];
        Task task = taskManager.getTask(0);

        assertEquals(0, task.getAssignments().length);

        AssignmentToggleAction assignmentToggleAction = makeAssignmentToggleAction(resource, task);
        assignmentToggleAction.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction.actionPerformed(null);

        assertEquals(1, task.getAssignments().length);

        ResourceDeleteAction resourceDeleteAction = makeDeleteResourceAction();
        addHumanResourceToSelection(resourceManager.getResourcesArray()[0]);
        resourceDeleteAction.actionPerformed(null);

        assertEquals(0, task.getAssignments().length);
    }

    public void testDeletionOfAssignedTask(){
        HumanResourceManager resourceManager = getHumanResourceManger();
        TaskManager taskManager = getTaskManager();
        ResourceNewAction resourceNewAction = makeNewResourceAction();
        TaskNewAction taskNewAction = makeNewTaskAction();

        resourceNewAction.actionPerformed(null);
        taskNewAction.actionPerformed(null);

        HumanResource resource = resourceManager.getResourcesArray()[0];
        Task task = taskManager.getTask(0);

        assertEquals(0, task.getAssignments().length);

        AssignmentToggleAction assignmentToggleAction = makeAssignmentToggleAction(resource, task);
        assignmentToggleAction.putValue(Action.SELECTED_KEY, true);
        assignmentToggleAction.actionPerformed(null);

        assertEquals(1, task.getAssignments().length);

        TaskDeleteAction taskDeleteAction = makeDeleteTaskAction();
        ArrayList<Task> selection = new ArrayList<>();
        selection.add(taskManager.getTask(1));
        taskDeleteAction.selectionChanged(selection);
        taskDeleteAction.actionPerformed(null);

        assertEquals(0, resource.getAssignments().length);
    }
}
