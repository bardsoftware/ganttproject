package net.sourceforge.ganttproject.resource;

import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.task.*;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

/**
 * @author Wannes Marynen
 */

public class AssignmentTest extends TaskTestCase {
    public void testdeleteAssignment() {
        HumanResourceManager resourceManager = new HumanResourceManager(null, new CustomColumnsManager());
        HumanResource humanResource = new HumanResource("Foo", 1, resourceManager);
        resourceManager.add(humanResource);

        Task task = createTask(TestSetupHelper.newMonday(), 1);
        ResourceAssignmentMutator mutableAssignments = task.getAssignmentCollection().createMutator();
        ResourceAssignment assignment = mutableAssignments.addAssignment(humanResource);
        assignment.setLoad(100.0f);
        mutableAssignments.commit();

        ResourceAssignmentCollection col = task.getAssignmentCollection();

        assertEquals(1, col.getAssignments().length);

        col.deleteAssignment(humanResource);

        assertEquals(0, col.getAssignments().length);

    }

    public void testClear() {
        HumanResourceManager resourceManager = new HumanResourceManager(null, new CustomColumnsManager());
        HumanResource humanResource = new HumanResource("Foo", 1, resourceManager);
        resourceManager.add(humanResource);

        Task task = createTask(TestSetupHelper.newMonday(), 1);
        ResourceAssignmentMutator mutableAssignments = task.getAssignmentCollection().createMutator();
        ResourceAssignment assignment = mutableAssignments.addAssignment(humanResource);
        assignment.setLoad(100.0f);
        mutableAssignments.commit();

        ResourceAssignmentCollection col = task.getAssignmentCollection();

        col.clear();

        ResourceAssignment[] ra = task.getAssignments();

        assertEquals(0, ra.length);
    }
}
