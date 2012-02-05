/*
Copyright 2003-2012 Dmitry Barashev, GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify 
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.sourceforge.ganttproject.task.dependency;

import java.util.List;
import net.sourceforge.ganttproject.task.Task;


public class TaskDependencySliceImpl implements TaskDependencySlice {
    private final Task myTask;

    private final TaskDependencyCollection myDependencyCollection;

    public TaskDependencySliceImpl(Task task,
            TaskDependencyCollection dependencyCollection) {
        myTask = task;
        myDependencyCollection = dependencyCollection;
    }

    @Override
    public TaskDependency[] toArray() {
        return myDependencyCollection.getDependencies(myTask);
    }

    @Override
    public void clear() {
        TaskDependency[] deps = toArray();
        for (int i = 0; i < deps.length; i++) {
            deps[i].delete();
        }
    }

    /** Unlinks only tasks that are selected and leaves links to not selected tasks. */
    @Override
    public void clear(List<Task> selection) {
        TaskDependency[] deps = toArray();
        for (int i = 0; i < deps.length; i++) {
            if (selection.contains(deps[i].getDependant()) && selection.contains(deps[i].getDependee())) {
                deps[i].delete();
            }
        }
    }

    @Override
    public boolean hasLinks(List<Task> selection) {
        TaskDependency[] deps = toArray();
        for (int i = 0; i < deps.length; i++) {
            if (selection.contains(deps[i].getDependant()) && selection.contains(deps[i].getDependee())) {
                return true;
            }
        }
        return false;
    }

    protected Task getTask() {
        return myTask;
    }

    protected TaskDependencyCollection getDependencyCollection() {
        return myDependencyCollection;
    }
}
