/*
GanttProject is an opensource project management tool.
Copyright (C) 2004-2011 Dmitry Barashev, GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
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
        Set<Task> checked = new LinkedHashSet<Task>();
        checked.add(dep.getDependee());
        return isLooping(checked, dep.getDependant());
    }

    private boolean isLooping(Set<Task> checked, Task incoming) {
        boolean result = false;
        Set<Task> newChecked = new LinkedHashSet<Task>(checked);
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
