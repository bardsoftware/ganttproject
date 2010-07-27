package net.sourceforge.ganttproject.task.algorithm;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;

public abstract class RecalculateTaskCompletionPercentageAlgorithm {
    public void run(Task task) {
        TaskContainmentHierarchyFacade facade = createContainmentFacade();
        recalculateSupertaskCompletionPercentageBottomUp(task, facade);
    }

    private void recalculateSupertaskCompletionPercentageBottomUp(Task task,
            TaskContainmentHierarchyFacade facade) {
        while (task != null) {
            recalculateSupertaskCompletionPercentage(task, facade);
            task = facade.getContainer(task);
        }
    }

    private void recalculateSupertaskCompletionPercentage(Task task,
            TaskContainmentHierarchyFacade facade) {
        Task[] nestedTasks = facade.getNestedTasks(task);
        if (nestedTasks.length > 0) {
            int completedDays = 0;
            long plannedDays = 0;
            for (int i = 0; i < nestedTasks.length; i++) {
                Task next = nestedTasks[i];
                long nextDuration = next.getDuration().getLength();
                completedDays += nextDuration * next.getCompletionPercentage();
                plannedDays += nextDuration;
            }
            int completionPercentage = (int) (completedDays / plannedDays);
            task.setCompletionPercentage(completionPercentage);
        }
    }

    protected abstract TaskContainmentHierarchyFacade createContainmentFacade();

}
