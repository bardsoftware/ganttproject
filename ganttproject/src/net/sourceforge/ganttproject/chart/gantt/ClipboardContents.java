/*
Copyright 2014 BarD Software s.r.o

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
package net.sourceforge.ganttproject.chart.gantt;

import java.util.List;
import java.util.Set;

import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ClipboardContents {
  private final List<Task> myTasks = Lists.newArrayList();
  private final List<TaskDependency> myDeps = Lists.newArrayList();
  private final HumanResourceManager myResourceManager;
  private final TaskManager myTaskManager;

  public ClipboardContents(TaskManager taskManager, HumanResourceManager resourceManager) {
    myTaskManager = taskManager;
    myResourceManager = resourceManager;
  }

  public void addTasks(List<Task> tasks) {
    myTasks.addAll(tasks);
  }

  public void build() {
    TaskContainmentHierarchyFacade taskHierarchy = myTaskManager.getTaskHierarchy();
    Set<Task> subtree = Sets.newHashSet();
    for (Task t : myTasks) {
      subtree.addAll(taskHierarchy.breadthFirstSearch(t, true));
    }
    for (TaskDependency dependency : myTaskManager.getDependencyCollection().getDependencies()) {
      Task dependant = dependency.getDependant();
      Task dependee = dependency.getDependee();
      if (subtree.contains(dependant) && subtree.contains(dependee)) {
        myDeps.add(dependency);
      }
    }

  }

  public List<Task> getTasks() {
    return myTasks;
  }

  public List<TaskDependency> getDeps() {
    return myDeps;
  }

}
