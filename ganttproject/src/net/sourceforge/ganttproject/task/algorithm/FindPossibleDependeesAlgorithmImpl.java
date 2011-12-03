package net.sourceforge.ganttproject.task.algorithm;

import java.util.ArrayList;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public abstract class FindPossibleDependeesAlgorithmImpl implements
        FindPossibleDependeesAlgorithm {
    private TaskContainmentHierarchyFacade myContainmentFacade;

    public FindPossibleDependeesAlgorithmImpl() {
    }

    @Override
    public Task[] run(Task dependant) {
        myContainmentFacade = createContainmentFacade();
        ArrayList<Task> result = new ArrayList<Task>();
        Task root = myContainmentFacade.getRootTask();
        Task[] nestedTasks = myContainmentFacade.getNestedTasks(root);
        processTask(nestedTasks, dependant, result);
        return result.toArray(new Task[0]);
    }

    protected abstract TaskContainmentHierarchyFacade createContainmentFacade();

    private void processTask(Task[] taskList, Task dependant, ArrayList<Task> result) {
        for (int i = 0; i < taskList.length; i++) {
            Task next = taskList[i];
            if (!next.equals(dependant)) {
                Task[] nested = myContainmentFacade.getNestedTasks(next);
                // if (nested.length==0) {
                result.add(next);
                // }
                // else {
                processTask(nested, dependant, result);
                // }
            }
        }
    }
}
