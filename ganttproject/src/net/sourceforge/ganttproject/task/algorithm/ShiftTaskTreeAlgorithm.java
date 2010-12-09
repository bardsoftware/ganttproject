package net.sourceforge.ganttproject.task.algorithm;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskLength;

public class ShiftTaskTreeAlgorithm {
    public void run(Task rootTask, TaskLength shift) {
        shiftTask(rootTask, shift);
    }

    private void shiftTask(Task rootTask, TaskLength shift) {
        rootTask.shift(shift);
        Task[] nestedTasks = rootTask.getManager().getTaskHierarchy()
                .getNestedTasks(rootTask);
        for (int i = 0; i < nestedTasks.length; i++) {
            Task next = nestedTasks[i];
            shiftTask(next, shift);
        }

    }
}
