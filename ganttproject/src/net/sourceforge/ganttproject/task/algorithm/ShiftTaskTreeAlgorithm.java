package net.sourceforge.ganttproject.task.algorithm;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManagerImpl;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

public class ShiftTaskTreeAlgorithm {
    public static final boolean DEEP = true;

    public static final boolean SHALLOW = false;

    private final TaskManagerImpl myTaskManager;
    private final RecalculateTaskScheduleAlgorithm myRescheduleAlgorithm;

    public ShiftTaskTreeAlgorithm(TaskManagerImpl taskManager, RecalculateTaskScheduleAlgorithm rescheduleAlgorithm) {
        myTaskManager = taskManager;
        myRescheduleAlgorithm = rescheduleAlgorithm;
    }

    public void run(List<Task> tasks, TaskLength shift, boolean deep) throws AlgorithmException {
        myTaskManager.setEventsEnabled(false);
        for (Task t : tasks) {
            shiftTask(t, shift, deep);
        }
        try {
            myRescheduleAlgorithm.run(new HashSet<Task>(tasks));
        } catch (TaskDependencyException e) {
            throw new AlgorithmException("Failed to reschedule the following tasks tasks after move:\n" + tasks, e);
        }
    }

    public void run(Task rootTask, TaskLength shift, boolean deep) throws AlgorithmException {
        run(Collections.singletonList(rootTask), shift, deep);
    }

    private void shiftTask(Task rootTask, TaskLength shift, boolean deep) {
        if (rootTask != myTaskManager.getRootTask()) {
            rootTask.shift(shift);
        }
        if (deep) {
            Task[] nestedTasks = rootTask.getManager().getTaskHierarchy().getNestedTasks(rootTask);
            for (int i = 0; i < nestedTasks.length; i++) {
                Task next = nestedTasks[i];
                shiftTask(next, shift, true);
            }
        }
    }
}
