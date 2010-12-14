package net.sourceforge.ganttproject.test.task.dependency;

import net.sourceforge.ganttproject.test.task.TaskTestCase;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskScheduleAlgorithm;
import net.sourceforge.ganttproject.task.algorithm.AdjustTaskBoundsAlgorithm;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.TestSetupHelper;

public class TestSupertaskAdjustment extends TaskTestCase {
    public void testSupetaskDurationGrowsWhenNestedTasksGrow()
            throws TaskDependencyException {
        TaskManager taskManager = getTaskManager();
        Task supertask = taskManager.createTask();
        Task task1 = taskManager.createTask();
        Task task2 = taskManager.createTask();

        task1.move(supertask);
        task2.move(supertask);

        task1.setStart(new GanttCalendar(2000, 01, 01));
        task1.setEnd(new GanttCalendar(2000, 01, 03));
        task2.setStart(new GanttCalendar(2000, 01, 03));
        task2.setEnd(new GanttCalendar(2000, 01, 04));
        supertask.setStart(new GanttCalendar(2000, 01, 01));
        supertask.setEnd(new GanttCalendar(2000, 01, 04));

        taskManager.getDependencyCollection().createDependency(
                task2, task1, new FinishStartConstraintImpl());

        task1.setEnd(new GanttCalendar(2000, 01, 04));
        RecalculateTaskScheduleAlgorithm alg = taskManager
                .getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm();
        alg.run(task1);

        assertEquals("Unexpected start of supertask=" + supertask,
                new GanttCalendar(2000, 01, 01), supertask.getStart());
        assertEquals("Unexpected end of supertask=" + supertask,
                new GanttCalendar(2000, 01, 05), supertask.getEnd());
    }

    public void testSupertaskDurationShrinksWhenNestedTasksShrink() {
        TaskManager taskManager = getTaskManager();
        Task supertask = taskManager.createTask();
        Task task1 = taskManager.createTask();
        Task task2 = taskManager.createTask();

        task1.move(supertask);
        task2.move(supertask);

        task1.setStart(new GanttCalendar(2000, 01, 01));
        task1.setEnd(new GanttCalendar(2000, 01, 03));
        task2.setStart(new GanttCalendar(2000, 01, 03));
        task2.setEnd(new GanttCalendar(2000, 01, 04));
        supertask.setStart(new GanttCalendar(2000, 01, 01));
        supertask.setEnd(new GanttCalendar(2000, 01, 04));

        task1.setStart(new GanttCalendar(2000, 01, 02));
        task2.setStart(new GanttCalendar(2000, 01, 02));
        task2.setEnd(new GanttCalendar(2000, 01, 03));

        AdjustTaskBoundsAlgorithm alg = taskManager.getAlgorithmCollection()
                .getAdjustTaskBoundsAlgorithm();
        alg.run(new Task[] { task1, task2 });

        assertEquals("Unexpected start of supertask=" + supertask,
                new GanttCalendar(2000, 01, 02), supertask.getStart());
        assertEquals("Unexpected end of supertask=" + supertask,
                new GanttCalendar(2000, 01, 03), supertask.getEnd());
    }

    public void testTaskDurationChangeIsPropagatedTwoLevelsUp() {
    	TaskManager taskManager = getTaskManager();
    	Task supertask = taskManager.createTask();
    	supertask.move(taskManager.getRootTask());

    	Task level1task1 = taskManager.createTask();
    	level1task1.move(supertask);
    	Task level1task2 = taskManager.createTask();
    	level1task2.move(supertask);

    	Task level2task1 = taskManager.createTask();
    	level2task1.move(level1task2);

    	supertask.setStart(TestSetupHelper.newMonday());
    	supertask.setEnd(TestSetupHelper.newTuesday());
    	level1task1.setStart(TestSetupHelper.newMonday());
    	level1task1.setEnd(TestSetupHelper.newTuesday());
    	level1task2.setStart(TestSetupHelper.newMonday());
    	level1task2.setEnd(TestSetupHelper.newTuesday());
    	level2task1.setStart(TestSetupHelper.newMonday());
    	level2task1.setEnd(TestSetupHelper.newTuesday());

    	level2task1.setEnd(TestSetupHelper.newWendesday());

        AdjustTaskBoundsAlgorithm alg = taskManager.getAlgorithmCollection().getAdjustTaskBoundsAlgorithm();
        alg.run(new Task[] { level2task1 });

        assertEquals("Unexpected end of the topleveltask="+supertask, TestSetupHelper.newWendesday(), supertask.getEnd());
    }

}
