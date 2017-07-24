package net.sourceforge.ganttproject.resource;

import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.TestSetupHelper.TaskManagerBuilder;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

public class TestResourceTotalLoad extends TaskTestCase {

    public void testResourceTotalLoad() {
	    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
	    setTaskManager(builder.build());
	    HumanResource joe = new HumanResource("Joe", 1, builder.getResourceManager());

	    assertEquals(0.0, joe.getTotalLoad());

	    builder.getResourceManager().add(joe);

	    Task t = createTask();
	    t.setDuration(t.getManager().createLength(2));
	    t.getAssignmentCollection().addAssignment(joe).setLoad(100f);
	    // two days at 100% load
	    assertEquals(2.0, joe.getTotalLoad());

	    t = createTask();
	    t.setDuration(t.getManager().createLength(4));
	    t.getAssignmentCollection().addAssignment(joe).setLoad(75f);
	    // add another 4 days at 75% load
	    assertEquals(5.0, joe.getTotalLoad());

	    t = createTask();
	    t.setDuration(t.getManager().createLength(10));
	    t.getAssignmentCollection().addAssignment(joe).setLoad(0f);
	    // add another 10 days at 0% load
	    assertEquals(5.0, joe.getTotalLoad());
	  }

}
