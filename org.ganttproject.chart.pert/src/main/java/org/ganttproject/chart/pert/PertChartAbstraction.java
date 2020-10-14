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
package org.ganttproject.chart.pert;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import biz.ganttproject.core.time.TimeDuration;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencySlice;

public class PertChartAbstraction {

  private final TaskManager myTaskManager;

  private final List<TaskGraphNode> myTaskGraph;

  public PertChartAbstraction(TaskManager taskManager) {
    myTaskManager = taskManager;
    myTaskGraph = new ArrayList<TaskGraphNode>();
    load();
  }

  /**
   * Loads data from task manager into pert chart abstraction. It creates all
   * TaskGraphNodes.
   */
  private void load() {
    Task[] tasks = myTaskManager.getTasks();
    for (int i = 0; i < tasks.length; i++) {
      Task task = tasks[i];
      TaskGraphNode tgn = getTaskGraphNode(task);
      TaskDependencySlice dependencies = task.getDependenciesAsDependee();
      TaskDependency[] relationship = dependencies.toArray();
      for (int j = 0; j < relationship.length; j++) {
        Task successor = relationship[j].getDependant();
        tgn.addSuccessor(getTaskGraphNode(successor));
      }
    }
  }

  /**
   * @param task
   *          The task from which we want the <code>TaskGraphNode</code>
   * @return The <code>TaskGraphNode</code> corresponding of the given
   *         <code>task</code>.
   */
  private TaskGraphNode getTaskGraphNode(Task task) {
    TaskGraphNode res = getTaskGraphNodeByID(task.getTaskID());
    if (res == null) {
      res = new TaskGraphNode(task);
      if (task.isMilestone()) {
        res.setType(Type.MILESTONE);
      } else if (myTaskManager.getTaskHierarchy().getNestedTasks(task).length == 0) {
        res.setType(Type.NORMAL);
      } else {
        res.setType(Type.SUPER);
      }
      myTaskGraph.add(res);
    }
    return res;
  }

  /** @return The list of <code>TaskGraphNodes</code>. */
  public List<TaskGraphNode> getTaskGraphNodes() {
    return myTaskGraph;
  }

  /**
   * @param id
   *          The task ID from which we want the <code>TaskGraphNode</code>
   * @return The <code>TaskGraphNode</code> corresponding to the given task ID.
   */
  public TaskGraphNode getTaskGraphNodeByID(int id) {
    for (TaskGraphNode tgn : myTaskGraph) {
      if (tgn.getID() == id) {
        return tgn;
      }
    }
    return null;
  }

  /**
   * PERT graph node abstraction
   *
   * @author bbaranne
   */
  static class TaskGraphNode {

    private List<TaskGraphNode> successors;

    private int type;

    private Task myTask;

    TaskGraphNode(Task task) {
      successors = new ArrayList<TaskGraphNode>();
      myTask = task;
    }

    void setType(int type) {
      this.type = type;
    }

    int getType() {
      return this.type;
    }

    void addSuccessor(TaskGraphNode successor) {
      this.successors.add(successor);
    }

    List<TaskGraphNode> getSuccessors() {
      return this.successors;
    }

    String getName() {
      return myTask.getName();
    }

    TimeDuration getDuration() {
      return myTask.getDuration();
    }

    int getID() {
      return myTask.getTaskID();
    }

    boolean isCritical() {
      return myTask.isCritical();
    }

    @Override
    public String toString() {
      return "{" + getName() + ", " + getDuration() + /* ", " + successors + */"}";
    }

    GregorianCalendar getEndDate() {
      return myTask.getDisplayEnd();
    }

    GregorianCalendar getStartDate() {
      return myTask.getStart();
    }
  }

  /**
   * Type of the node: NORMAL, SUPER (for super tasks) and MILESTONE.
   *
   * @author bbaranne
   *
   */
  static class Type {
    public static final int NORMAL = 0;

    public static final int SUPER = 1;

    public static final int MILESTONE = 2;
  }
}
