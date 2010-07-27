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
	public boolean isLooping(TaskDependency dep) {
		Set checked = new LinkedHashSet();
		checked.add(dep.getDependee());
		return isLooping(checked, dep.getDependant());
	}

	private boolean isLooping(Set checked, Task incoming) {
		boolean result = false;
		Set newChecked = new LinkedHashSet(checked);
		newChecked.add(incoming);
		TaskDependency[] nextDeps = incoming.getDependenciesAsDependee().toArray();
		for (int i=0; !result && i<nextDeps.length; i++) {
			if (!newChecked.contains(nextDeps[i].getDependant())) {
				result = isLooping(newChecked, nextDeps[i].getDependant());
			}
			else {
				result = true;
			}
		}
		if (!result) {
			Task supertask = myTaskManager.getTaskHierarchy().getContainer(incoming);
			if (supertask!=null && myTaskManager.getTaskHierarchy().getRootTask()!=supertask) {
				result = isLooping(newChecked, supertask);
			}
		}
		return result;
	}
}
