package net.sourceforge.ganttproject.test.task.dependency;

import net.sourceforge.ganttproject.test.task.TaskTestCase;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.algorithm.RecalculateTaskScheduleAlgorithm;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;
import net.sourceforge.ganttproject.GanttCalendar;
import net.sourceforge.ganttproject.GanttTaskRelationship;
import net.sourceforge.ganttproject.time.gregorian.GregorianTimeUnitStack;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class TestRecalculateTaskScheduleAlgorithm extends TaskTestCase {
    public void testFinishStartDependenciesOnMovingEndDateForward()
            throws TaskDependencyException {
        TaskManager taskManager = getTaskManager();
        Task task1 = taskManager.createTask();
        task1.setStart(new GanttCalendar(2000, 01, 01));
        task1.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY,
                1));

        Task task2 = taskManager.createTask();
        task2.setStart(new GanttCalendar(2000, 01, 02));
        task2.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY,
                1));

        Task task3 = taskManager.createTask();
        task3.setStart(new GanttCalendar(2000, 01, 03));
        task3.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY,
                1));
        // System.err.println("task3 end="+task3.getEnd());

        taskManager.getDependencyCollection().createDependency(task3, task2,
                new FinishStartConstraintImpl());
        taskManager.getDependencyCollection().createDependency(task2, task1,
                new FinishStartConstraintImpl());
        task1.setEnd(new GanttCalendar(2000, 01, 03));

        RecalculateTaskScheduleAlgorithm alg = taskManager
                .getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm();
        alg.run(task1);

        assertEquals("Unexpected value of end of task=" + task3,
                new GanttCalendar(2000, 01, 05), task3.getEnd());
    }

    public void testFinishStartConstraintOnMovingEndDateBackward()
            throws TaskDependencyException {
        TaskManager taskManager = getTaskManager();
        Task task1 = taskManager.createTask();
        task1.setStart(new GanttCalendar(2000, 01, 01));
        task1.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY,
                3));

        Task task2 = taskManager.createTask();
        task2.setStart(new GanttCalendar(2000, 01, 01));
        task2.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY,
                2));

        Task task3 = taskManager.createTask();
        task3.setStart(new GanttCalendar(2000, 01, 04));
        task3.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY,
                1));

        taskManager.getDependencyCollection().createDependency(task3, task1,
                new FinishStartConstraintImpl());
        taskManager.getDependencyCollection().createDependency(task3, task2,
                new FinishStartConstraintImpl());
        // Now the task schedule is the following:
        // task1 xxx
        // task3    x
        // task2 xx

        // Decrease length of task1
        task1.setEnd(new GanttCalendar(2000, 01, 03));
        // And expect:
        // task1 xx
        // task3   x
        // task2 xx
        RecalculateTaskScheduleAlgorithm alg = taskManager
                .getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm();
        alg.run(task1);
        assertEquals("Unexpected value of start of task=" + task3,
                new GanttCalendar(2000, 01, 03), task3.getStart());

        // Decrease length of task1 again
        task1.setEnd(new GanttCalendar(2000, 01, 02));
        // Now we expect:
        // task1 x
        // task3   x
        // task2 xx
        // because task3 also depends on task2 with "finish-start" constraint
        alg = taskManager.getAlgorithmCollection()
                .getRecalculateTaskScheduleAlgorithm();
        alg.run(task1);
        assertEquals("Unexpected value of start of task=" + task3,
                new GanttCalendar(2000, 01, 03), task3.getStart());
    }

    public void testFinishFinishConstraintOnMovingEndDateForward()
            throws TaskDependencyException {
        TaskManager taskManager = getTaskManager();
        Task task1 = taskManager.createTask();
        task1.setStart((new GanttCalendar(2000, 01, 01)));
        task1.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY,
                1));

        Task task2 = taskManager.createTask();
        task2.setStart((new GanttCalendar(2000, 01, 01)));
        task2.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY,
                3));

        // Now we have:
        // task1 x
        // task2 xxx

        // Create dependency task2->task1 with "finish-finish" constraint.
        // This means that task2 can't finish earlier than task1 finishes
        TaskDependency dep = taskManager
                .getDependencyCollection()
                .createDependency(task2, task1, new FinishStartConstraintImpl());
        dep.setConstraint(taskManager
                .createConstraint(GanttTaskRelationship.FF));

        // Increase the length of task1.
        task1.setEnd(new GanttCalendar(2000, 01, 05));
        RecalculateTaskScheduleAlgorithm alg = taskManager
                .getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm();
        alg.run(task1);
        // We expect that task2 will be shifted forward in time:
        // task1 xxxxx
        // task2   xxx
        assertEquals("Unexpected end of task=" + task2, new GanttCalendar(2000,
                01, 05), task2.getEnd());
    }

    public void testStartStartConstraintOnMovingStartDateForward()
            throws TaskDependencyException {
        TaskManager taskManager = getTaskManager();
        Task task1 = taskManager.createTask();
        task1.setStart((new GanttCalendar(2000, 01, 01)));
        task1.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY,
                3));

        Task task2 = taskManager.createTask();
        task2.setStart((new GanttCalendar(2000, 01, 02)));
        task2.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY,
                1));

        // Now we have:
        // task1 xxx____
        // task2 _x_____

        // Create dependency task2->task1 with "start-start" constraint.
        // This means that task2 can't start earlier than task1 starts
        TaskDependency dep = taskManager
                .getDependencyCollection()
                .createDependency(task2, task1, new FinishStartConstraintImpl());
        dep.setConstraint(taskManager
                .createConstraint(GanttTaskRelationship.SS));

        // Shift task1 forward in time.
        TaskLength task1Duration = task1.getDuration();
        task1.setStart(new GanttCalendar(2000, 01, 04));
        task1.setDuration(task1Duration);

        RecalculateTaskScheduleAlgorithm alg = taskManager
                .getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm();
        alg.run(task1);

        // We expect that task2 will also be shifted forward in time:
        // task1 ___xxx___
        // task2 ___x_____

        assertEquals("Unexpected start of task=" + task2, new GanttCalendar(
                2000, 01, 04), task2.getStart());
    }

    public void testStartFinishConstraintOnMovingStartDateForward()
            throws TaskDependencyException {
        TaskManager taskManager = getTaskManager();
        Task task1 = taskManager.createTask();
        task1.setStart((new GanttCalendar(2000, 01, 01)));
        task1.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY,
                3));
        //
        Task task2 = taskManager.createTask();
        task2.setStart((new GanttCalendar(2000, 01, 01)));
        task2.setDuration(taskManager.createLength(GregorianTimeUnitStack.DAY,
                2));
        //
        // now we have:
        // task1 xxx____
        // task2 xx_____
        //
        // create dependency task2->task1 with "start-finish" constraint. It
        // means that
        // task2 can't finish earlier than task1 starts
        TaskDependency dep = taskManager
                .getDependencyCollection()
                .createDependency(task2, task1, new FinishStartConstraintImpl());
        dep.setConstraint(taskManager
                .createConstraint(GanttTaskRelationship.SF));
        //
        // shift task1 forward. We expect that task2 will also be shifted
        // forward
        //
        // task1 ___xxx___
        // task2 _xx______
        TaskLength task1Duration = task1.getDuration();
        task1.setStart(new GanttCalendar(2000, 01, 04));
        task1.setDuration(task1Duration);
        //
        RecalculateTaskScheduleAlgorithm alg = taskManager
                .getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm();
        alg.run(task1);
        assertEquals("Unexpected end of task=" + task2, new GanttCalendar(2000,
                01, 04), task2.getEnd());

    }
}
