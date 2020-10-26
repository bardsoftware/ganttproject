/*
GanttProject is an opensource project management tool.
Copyright (C) 2009-2011 Dmitry Barashev, GanttProject team

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
package net.sourceforge.ganttproject.task.algorithm;

import biz.ganttproject.core.calendar.GPCalendarCalc;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint.Collision;

import java.util.*;
import java.util.logging.Logger;

public class CriticalPathAlgorithmImpl implements CriticalPathAlgorithm {
  private static final Logger ourLogger = GPLogger.getLogger(CriticalPathAlgorithm.class);

  private final TaskManager myTaskManager;
  private final GPCalendarCalc myCalendar;

  public CriticalPathAlgorithmImpl(TaskManager taskManager, GPCalendarCalc calendar) {
    myTaskManager = taskManager;
    myCalendar = calendar;
  }

  static class Node {
    private final Task task;
    private final List<Task> dependees = new ArrayList<Task>();
    private int numDependants;
    private final Date est;
    private final Date eft;
    private Date lst;
    private Date lft;
    private boolean lftFromSupertask = false;

    public Node(Task t, Set<Task> taskScope) {
      assert t != null;
      task = t;
      est = t.getStart().getTime();
      eft = t.getEnd().getTime();
      lst = null;
      lft = null;
      numDependants = 0;
      TaskDependency[] deps = t.getDependenciesAsDependee().toArray();
      for (int i = 0; i < deps.length; i++) {
        if (taskScope.contains(deps[i].getDependant())) {
          numDependants++;
        }
      }
      collectDependees(t, taskScope);
    }

    public Node(Task t, Date est, Date eft, Date lst, Date lft, int numDependants, Set<Task> taskScope) {
      task = t;
      this.est = est;
      this.eft = eft;
      this.lst = lst;
      this.lft = lft;
      this.numDependants = numDependants;
      if (task != null) {
        collectDependees(task, taskScope);
      }
    }

    void collectDependees(Task task, Set<Task> taskScope) {
      TaskDependency[] deps = task.getDependenciesAsDependant().toArray();
      for (TaskDependency dep : deps) {
        if (taskScope.contains(dep.getDependee())) {
          dependees.add(dep.getDependee());
        }
      }
    }

    boolean isCritical() {
      return est.equals(lst);
    }

    @Override
    public String toString() {
      return task == null ? "[Deadline node " + eft + "]" : task.toString();
    }
  }

  @Override
  public Task[] getCriticalTasks() {
    Date projectEnd = myTaskManager.getProjectEnd();
    Node fakeFinalNode = new Node(null, projectEnd, projectEnd, projectEnd, projectEnd, 0, null);
    Task[] tasks = myTaskManager.getTasks();
    if (tasks.length == 0) {
      return tasks;
    }
    Map<Task, Node> task_node = createTaskNodeMap(tasks, fakeFinalNode);
    for (Node curNode : task_node.values()) {
      curNode.numDependants += myTaskManager.getTaskHierarchy().getDepth(curNode.task) - 1;
    }
    assert fakeFinalNode.dependees.size() > 0;

    LinkedHashSet<Task> result = new LinkedHashSet<Task>();
    Processor p = new Processor(task_node, fakeFinalNode);
    result.addAll(p.run());
    return result.toArray(new Task[result.size()]);
  }

  private Map<Task, Node> createTaskNodeMap(Task[] tasks, Node deadlineNode) {
    Set<Task> taskScope = new HashSet<Task>(Arrays.asList(tasks));
    Map<Task, Node> task_node = new HashMap<Task, Node>();
    for (Task task : tasks) {
      Node newNode = new Node(task, taskScope);
      deadlineNode.dependees.add(task);
      newNode.numDependants++;
      task_node.put(task, newNode);
    }
    return task_node;
  }

  class Processor {
    private final Map<Task, Node> myTask_Node;
    private LinkedList<Node> myQueue = new LinkedList<Node>();
    private final ArrayList<Task> myResult = new ArrayList<Task>();
    private final Node myDeadlineNode;

    Processor(Map<Task, Node> task_node, Node deadlineNode) {
      myDeadlineNode = deadlineNode;
      myTask_Node = task_node;
      myQueue.add(myDeadlineNode);
    }

    boolean hasMoreInput() {
      return !myQueue.isEmpty();
    }

    List<Task> run() {
      while (hasMoreInput()) {
        myQueue = processQueue();
      }
      return myResult;
    }

    private LinkedList<Node> processQueue() {
      LinkedList<Node> newQueue = new LinkedList<Node>();
      for (Iterator<Node> nodes = myQueue.iterator(); nodes.hasNext();) {
        Node curNode = nodes.next();
        if (curNode.lft == null || curNode.lftFromSupertask) {
          calculateLatestDates(curNode);
          Task[] nestedTasks = myTaskManager.getTaskHierarchy().getNestedTasks(curNode.task);
          for (Task nestedTask : nestedTasks) {
            Node nested = myTask_Node.get(nestedTask);
            nested.numDependants -= (myTaskManager.getTaskHierarchy().getDepth(nested.task) - 1);
            assert nested.numDependants >= 0;
            if (nested.numDependants == 0) {
              newQueue.add(nested);
            }
            if (curNode.isCritical()) {
              nested.lft = curNode.lft;
              nested.lftFromSupertask = true;
            }
          }

          if (curNode.isCritical()) {
            ourLogger.fine("\n\nNode=" + curNode + " is critical\n\n");
            myResult.add(curNode.task);
          }
        } else {
          assert curNode.task == null || curNode.lftFromSupertask;
        }
        enqueueDependees(newQueue, curNode);
      }
      return newQueue;
    }

    private void calculateLatestDates(Node curNode) {
      ourLogger.fine("Calculating latest dates for:" + curNode);
      curNode.lft = findLatestFinishTime(myTask_Node, curNode);
      curNode.lst = myCalendar.shiftDate(curNode.lft,
          myTaskManager.createLength(-curNode.task.getDuration().getLength()));
      ourLogger.fine("latest start date=" + curNode.lst);
    }

    private void enqueueDependees(LinkedList<Node> newQueue, Node curNode) {
      for (int i = 0; i < curNode.dependees.size(); i++) {
        Task dependeeTask = curNode.dependees.get(i);
        Node dependeeNode = myTask_Node.get(dependeeTask);
        assert dependeeNode.numDependants > 0;
        if (--dependeeNode.numDependants == 0) {
          newQueue.add(dependeeNode);
        }
      }
    }

    private Date findLatestFinishTime(Map<Task, Node> task_node, Node curNode) {
      Date result = curNode.lft;
      Node resultNode = null;
      TaskDependency[] deps = curNode.task.getDependenciesAsDependee().toArray();
      for (TaskDependency dep : deps) {
        Node depNode = task_node.get(dep.getDependant());
        if (depNode != null) {
          Date lft = findLatestFinishTime(curNode, depNode, dep);
          if (result == null || result.after(lft)) {
            result = lft;
            resultNode = depNode;
          }
        }
      }
      if (result == null || result.after(myDeadlineNode.lft)) {
        result = myDeadlineNode.lft;
      }
      ourLogger.fine("latest finish time=" + result + " (defined by:" + resultNode + ")");
      return result;
    }

    Date findLatestFinishTime(Node curNode, Node depNode, TaskDependency dep) {
      Collision backwardCollision = dep.getConstraint().getBackwardCollision(depNode.lst);
      if (backwardCollision == null) {
        return depNode.lst;
      }
      return backwardCollision.getAcceptableStart().getTime();
    }
  }
}
