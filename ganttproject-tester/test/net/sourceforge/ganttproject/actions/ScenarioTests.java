package net.sourceforge.ganttproject.actions;

import biz.ganttproject.core.time.TimeDurationImpl;
import biz.ganttproject.core.time.impl.GregorianTimeUnitStack;
import net.sourceforge.ganttproject.action.task.TaskNewAction;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.*;

public class ScenarioTests extends ScenarioTestCase {
    public void testChangeTaskDuration() {
        HumanResourceManager resourceManager = getHumanResourceManger();
        HumanResource humanResource = new HumanResource("Foo", 15, resourceManager);
        resourceManager.add(humanResource);

        TaskManager taskManager = getTaskManager();
        TaskNewAction taskNewAction = makeNewTaskAction();

        taskNewAction.actionPerformed(null);

        assertEquals(1, taskManager.getTaskCount());

        Task t = taskManager.getTasks()[0];
        t.getAssignmentCollection().createMutator();
        ResourceAssignmentMutator mutableAssignments = t.getAssignmentCollection().createMutator();
        ResourceAssignment assignment = mutableAssignments.addAssignment(humanResource);
        assignment.setLoad(100.0f);
        mutableAssignments.commit();


        // task1.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY, 1));

        assertEquals(taskManager.createLength(GregorianTimeUnitStack.DAY, 1), t.getDuration());
        var temp1 = humanResource.getLoadDistribution().getLoads();
        assertEquals(1.0, humanResource.getTotalLoad());


        t.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY, 12));
        assertEquals(1, t.getAssignments().length);
        assertEquals(12.0, humanResource.getTotalLoad());


        assertEquals(taskManager.createLength(GregorianTimeUnitStack.DAY, 12), t.getDuration());


    }

    public void testResourceWithSameName() {
        HumanResourceManager resourceManager = getHumanResourceManger();
        HumanResource humanResource1 = new HumanResource("Foo", 15, resourceManager);
        HumanResource humanResource2 = new HumanResource("Foo", 16, resourceManager);
        resourceManager.add(humanResource1);
        resourceManager.add(humanResource2);

        TaskManager taskManager = getTaskManager();
        TaskNewAction taskNewAction = makeNewTaskAction();

        taskNewAction.actionPerformed(null);
        Task t = taskManager.getTasks()[0];
        t.getAssignmentCollection().createMutator();
        ResourceAssignmentMutator mutableAssignments = t.getAssignmentCollection().createMutator();
        ResourceAssignment assignment = mutableAssignments.addAssignment(humanResource1);
        assignment.setLoad(100.0f);
        mutableAssignments.commit();

        assertEquals(0.0, humanResource2.getTotalLoad());
    }
}
