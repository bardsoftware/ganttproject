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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Represents all objects which are involved into a clipboard transaction on Gantt chart: tasks, dependencies
 * and resource assignments. It is not really what is placed in the system clipboard, it is rather a grouping of
 * the model objects
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class ClipboardContents {
  private final List<Task> myTasks = Lists.newArrayList();
  private final List<TaskDependency> myIntraDeps = Lists.newArrayList();
  private final List<TaskDependency> myIncomingDeps = Lists.newArrayList();
  private final List<TaskDependency> myOutgoingDeps = Lists.newArrayList();
  private final List<ResourceAssignment> myAssignments = Lists.newArrayList();
  private final TaskManager myTaskManager;

  public ClipboardContents(TaskManager taskManager) {
    myTaskManager = taskManager;
  }

  /**
   * Adds tasks to the clipboard contents
   * @param tasks
   */
  public void addTasks(List<Task> tasks) {
    myTasks.addAll(tasks);
  }

  /**
   * Adds appropriate objects (dependencies and assignments) to the clipboard depending on the already placed tasks.
   */
  private void build() {
    TaskContainmentHierarchyFacade taskHierarchy = myTaskManager.getTaskHierarchy();
    Set<Task> subtree = Sets.newHashSet();
    for (Task t : myTasks) {
      subtree.addAll(taskHierarchy.breadthFirstSearch(t, true));
    }
    Set<TaskDependency> intraDeps = Sets.newLinkedHashSet();
    for (TaskDependency dependency : myTaskManager.getDependencyCollection().getDependencies()) {
      Task dependant = dependency.getDependant();
      Task dependee = dependency.getDependee();
      if (subtree.contains(dependant) && subtree.contains(dependee)) {
        intraDeps.add(dependency);
      }
    }

    for (Task t : subtree) {
      for (TaskDependency dep : t.getDependenciesAsDependant().toArray()) {
        if (intraDeps.contains(dep)) {
          continue;
        }
        myIncomingDeps.add(dep);
      }
      for (TaskDependency dep : t.getDependenciesAsDependee().toArray()) {
        if (intraDeps.contains(dep)) {
          continue;
        }
        myOutgoingDeps.add(dep);
      }
    }
    myIntraDeps.addAll(intraDeps);
    GPLogger.getLogger("Clipboard").fine(String.format(
        "Clipboard task (only roots): %s\ninternal-dependencies: %s\nincoming dependencies:%s\noutgoing dependencies:%s",
        myTasks, myIntraDeps, myIncomingDeps, myOutgoingDeps));
  }

  /**
   * @return all clipboard tasks
   */
  public List<Task> getTasks() {
    return myTasks;
  }

  /**
   * @return a list of dependencies where both successor and predecessor are in clipboard for any dep
   */
  public List<TaskDependency> getIntraDeps() {
    return myIntraDeps;
  }

  /**
   * @return a list of dependencies where only successor is in clipboard for any dep
   */
  public List<TaskDependency> getIncomingDeps() {
    return myIncomingDeps;
  }

  /**
   * @return a list of dependencies where only predecessor is in clipboard for any dep
   */
  public List<TaskDependency> getOutgoingDeps() {
    return myOutgoingDeps;
  }
  public List<ResourceAssignment> getAssignments() {
    return myAssignments;
  }

  /**
   * Processes objects placed into the clipboard so that it was "cut" transaction
   */
  public void cut() {
    build();
    for (Task t : getTasks()) {
      myAssignments.addAll(Arrays.asList(t.getAssignments()));
      myTaskManager.deleteTask(t);
      t.delete();
    }
  }

  /**
   * Processes objects placed into the clipboard so that it was "copy" transaction
   */
  public void copy() {
    build();
    // Nothing needs to be done, actually, in addition to what build() already does
  }
}
