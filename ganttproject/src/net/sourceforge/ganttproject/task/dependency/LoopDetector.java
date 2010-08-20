/* LICENSE: GPL
 * This code is provided under the terms of GPL version 2.
 * Please see LICENSE file for details
 * (C) Dmitry Barashev, GanttProject team, 2004-2008
 */
package net.sourceforge.ganttproject.task.dependency;

import java.util.LinkedHashSet;
import java.util.Set;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * Loop detector answers whether a dependency will create a loop in the dependency graph
 * @author dbarashev
 */
public class LoopDetector {
	private final TaskManager myTaskManager;

    public LoopDetector(TaskManager taskManager) {
        myTaskManager = taskManager;
    }

    /**
     * @param dep new dependecy which needs to be check whether it results in a loop when added
     * @return true if dep results in a loop
     */
    public boolean isLooping(TaskDependency dep) {
        Set<Task> checked = new LinkedHashSet<Task>();
        checked.add(dep.getDependee());
        return isLooping(checked, dep.getDependant());
    }

    /**
     * The used method is to check recursively whether the dependencies of the
     * incoming task are already added to the checked set. Before calling this
     * method for the dependent tasks the incoming task is also added to the
     * checked set
     */
    private boolean isLooping(Set<Task> checked, Task incoming) {
        boolean result = false;
        Set<Task> newChecked = new LinkedHashSet<Task>(checked);
        newChecked.add(incoming);

        // If one of the incoming tasks dependencies are present in newChecked,
        // it indicates there is a loop
        TaskDependency[] nextDeps = incoming.getDependenciesAsDependee().toArray();
        for (int i = 0; !result && i < nextDeps.length; i++) {
            Task dependant = nextDeps[i].getDependant();
            if (!newChecked.contains(dependant)) {
                result = isLooping(newChecked, dependant);
            } else {
                result = true;
            }
        }

        if (!result) {
            // Check if the tasks supertask has dependencies which might loop
            Task supertask = myTaskManager.getTaskHierarchy().getContainer(incoming);
            if (supertask != null && myTaskManager.getTaskHierarchy().getRootTask() != supertask) {
                result = isLooping(newChecked, supertask);
            }
        }

        if(!result){
            // Check if nested tasks have dependencies which might loop
            Task[] nestedTasks = myTaskManager.getTaskHierarchy().getNestedTasks(incoming);
            for(int i = 0; !result && i< nestedTasks.length; i++) {
                if (!newChecked.contains(nestedTasks[i])) {
                    result = isLooping(newChecked, nestedTasks[i]);
                } else {
                    result = true;
                }
            }
        }

        return result;
	}
}
