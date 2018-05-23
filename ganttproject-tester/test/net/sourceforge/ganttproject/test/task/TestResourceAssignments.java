package net.sourceforge.ganttproject.test.task;

import biz.ganttproject.core.calendar.AlwaysWorkingTimeCalendarImpl;
import biz.ganttproject.core.calendar.GPCalendarCalc;
import biz.ganttproject.core.option.ColorOption;
import biz.ganttproject.core.time.TimeUnitStack;
import biz.ganttproject.core.time.impl.GPTimeUnitStack;
import junit.framework.TestCase;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.ResourceAssignmentMutator;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManagerConfig;

import java.awt.*;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TestResourceAssignments extends TestCase {
    private TaskManager myTaskManager;

    private HumanResourceManager myHumanResourceManager;

    public TestResourceAssignments(String s) {
        super(s);
    }

    public void testResourceAppearsInListAfterCreation() {
        TaskManager taskManager = getTaskManager();
        Task task = taskManager.createTask();
        HumanResource res1 = getResourceManager().getById(1);
        HumanResource res2 = getResourceManager().getById(2);
        task.getAssignmentCollection().addAssignment(res1);
        task.getAssignmentCollection().addAssignment(res2);
        Set<HumanResource> actualResources = extractResources(task);
        Set<HumanResource> expectedResources = new HashSet<HumanResource>(
                Arrays.asList(new HumanResource[] { res1, res2 }));
        assertEquals("Unexpected set of resources assigned to task=" + task,
                expectedResources, actualResources);
    }

    public void testResourceDisappearsFromListAfterAssignmentDeletion() {
        TaskManager taskManager = getTaskManager();
        Task task = taskManager.createTask();
        HumanResource res1 = getResourceManager().getById(1);
        HumanResource res2 = getResourceManager().getById(2);
        task.getAssignmentCollection().addAssignment(res1);
        ResourceAssignment asgn2 = task.getAssignmentCollection()
                .addAssignment(res2);

        asgn2.delete();

        Set<HumanResource> actualResources = extractResources(task);
        Set<HumanResource> expectedResources = new HashSet<HumanResource>(
                Arrays.asList(res1));
        assertEquals("Unexpected set of resources assigned to task=" + task,
                expectedResources, actualResources);
    }

    public void testResourceIsNotAssignedTwice() {
        TaskManager taskManager = getTaskManager();
        Task task = taskManager.createTask();
        HumanResource res1 = getResourceManager().getById(1);
        task.getAssignmentCollection().addAssignment(res1);
        task.getAssignmentCollection().addAssignment(res1);
        Set<HumanResource> actualResources = extractResources(task);
        Set<HumanResource> expectedResources = new HashSet<HumanResource>(
                Arrays.asList(res1));
        assertEquals("Unexpected set of resources assigned to task=" + task,
                expectedResources, actualResources);
    }


    public void testAssignmentsDisappearOnTaskDeletion() {
        TaskManager taskManager = getTaskManager();
        Task task = taskManager.createTask();
        HumanResource res1 = getResourceManager().getById(1);
        task.getAssignmentCollection().addAssignment(res1);
        task.delete();
        ResourceAssignment[] assignments = res1.getAssignments();
        assertTrue(
                "Resource is expected to have no assignments after task deletion",
                assignments.length == 0);
    }

    public void testAssignmentsDisappearOnSummaryTaskDeletion() {
        HumanResource res1 = getResourceManager().getById(1);
        TaskManager taskManager = getTaskManager();
        Task summaryTask = taskManager.createTask();
        summaryTask.getAssignmentCollection().addAssignment(res1);

        Task childTask = taskManager.newTaskBuilder().withParent(summaryTask).build();
        childTask.getAssignmentCollection().addAssignment(res1);

        taskManager.deleteTask(summaryTask);
        ResourceAssignment[] assignments = res1.getAssignments();
        assertTrue(
            "Resource is expected to have no assignments after summary task deletion",
            assignments.length == 0);
    }

    public void testAssignmentDisappearOnResourceDeletion() {
        TaskManager taskManager = getTaskManager();
        Task task = taskManager.createTask();
        HumanResource res1 = getResourceManager().getById(1);
        task.getAssignmentCollection().addAssignment(res1);
        res1.delete();
        Set<HumanResource> resources = extractResources(task);
        assertTrue("It is expected that after resource deletion assignments disappear", resources.isEmpty());
    }

    // See https://github.com/bardsoftware/ganttproject/issues/612
    public void testAssignmentUpdateAndDelete() {
        TaskManager taskManager = getTaskManager();
        Task task = taskManager.createTask();
        HumanResource res1 = getResourceManager().getById(1);
        ResourceAssignment assignment = task.getAssignmentCollection().addAssignment(res1);
        ResourceAssignmentMutator mutator = task.getAssignmentCollection().createMutator();
        assignment.delete();
        assignment = mutator.addAssignment(res1);
        assignment.setLoad(50);
        assignment.delete();
        mutator.commit();

        Set<HumanResource> resources = extractResources(task);
        assertTrue("It is expected that assignment is removed after sequential update+delete via mutator", resources.isEmpty());
    }

    private Set<HumanResource> extractResources(Task task) {
        Set<HumanResource> result = new HashSet<HumanResource>();
        ResourceAssignment[] assignments = task.getAssignments();
        for (int i = 0; i < assignments.length; i++) {
            ResourceAssignment next = assignments[i];
            result.add(next.getResource());
            assertEquals("Unexpected task is owning resource assignment="
                    + next, task, next.getTask());
        }
        return result;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myHumanResourceManager = new HumanResourceManager(RoleManager.Access
                .getInstance().getDefaultRole(), null);
        getResourceManager().create("test resource#1", 1);
        getResourceManager().create("test resource#2", 2);
        myTaskManager = newTaskManager();
    }

    private TaskManager newTaskManager() {
        return TaskManager.Access.newInstance(null, new TaskManagerConfig() {

            @Override
            public Color getDefaultColor() {
                return null;
            }

          @Override
          public ColorOption getDefaultColorOption() {
            return null;
          }

          @Override
            public GPCalendarCalc getCalendar() {
                return new AlwaysWorkingTimeCalendarImpl();
            }

            @Override
            public TimeUnitStack getTimeUnitStack() {
                return new GPTimeUnitStack();
            }

            @Override
            public HumanResourceManager getResourceManager() {
                return null;
            }

            @Override
            public URL getProjectDocumentURL() {
                return null;
            }

            @Override
            public NotificationManager getNotificationManager() {
              return null;
            }
        });
    }

    private TaskManager getTaskManager() {
        return myTaskManager;
    }

    private HumanResourceManager getResourceManager() {
        return myHumanResourceManager;
    }
}
