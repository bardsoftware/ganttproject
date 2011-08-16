package net.sourceforge.ganttproject.test.task;

import junit.framework.TestCase;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.TaskManagerConfig;
import net.sourceforge.ganttproject.time.TimeUnitStack;
import net.sourceforge.ganttproject.time.gregorian.GPTimeUnitStack;
import net.sourceforge.ganttproject.calendar.AlwaysWorkingTimeCalendarImpl;
import net.sourceforge.ganttproject.calendar.GPCalendar;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.roles.RoleManager;

import java.awt.Color;
import java.net.URL;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

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

    public void testAssignmentDisappearOnResourceDeletion() {
        TaskManager taskManager = getTaskManager();
        Task task = taskManager.createTask();
        taskManager.registerTask(task);
        HumanResource res1 = getResourceManager().getById(1);
        task.getAssignmentCollection().addAssignment(res1);
        res1.delete();
        Set<HumanResource> resources = extractResources(task);
        assertTrue("It is expecte that after resource deletion assignments disappear", resources.isEmpty());
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

            public Color getDefaultColor() {
                return null;
            }

            public GPCalendar getCalendar() {
                return new AlwaysWorkingTimeCalendarImpl();
            }

            public TimeUnitStack getTimeUnitStack() {
                return new GPTimeUnitStack(GanttLanguage.getInstance());
            }

            public HumanResourceManager getResourceManager() {
                return null;
            }

            public URL getProjectDocumentURL() {
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
