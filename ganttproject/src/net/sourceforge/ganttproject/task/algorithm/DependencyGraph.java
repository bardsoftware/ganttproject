/*
Copyright 2012 GanttProject Team

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
package net.sourceforge.ganttproject.task.algorithm;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependency.Hardness;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import biz.ganttproject.core.calendar.GPCalendar;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.collect.BoundType;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.Ranges;
import com.google.common.collect.TreeMultimap;

/**
 * A graph of dependencies between tasks which is used for scheduling algorithm.
 * In this graph nodes are tasks, and edges are either explicit or implicit dependencies between tasks.
 *
 * Graph is topologically ordered, and each node knows its level. Adding or removing dependencies or moving tasks
 * in the task hierarchy may change node levels.
 *
 * @author dbarashev
 */
public class DependencyGraph {
  public static interface Listener {
    void onChange();
  }

  public static interface Logger {
    void log(String title, String message);
  }
  /**
   * Dependency defines a constraint on its target task start and end dates. Constraints
   * are normally either points or semi-open intervals on the date axis.
   */
  public static interface DependencyEdge {
    /**
     * @return dst node start date constraint
     */
    Range<Date> getStartRange();

    /**
     * @return dst node end date constraint
     */
    Range<Date> getEndRange();

    /**
     * @return this dependency target node
     */
    Node getDst();

    /**
     * @return this dependency source node
     */
    Node getSrc();

    /**
     * refreshes constraint information
     */
    boolean refresh();

    boolean isWeak();
  }

  /**
   * Explicit dependency is constructed from {@link TaskDependency} instances and corresponds
   * to dependencies explicitly created by a user
   */
  static class ExplicitDependencyImpl implements DependencyEdge {
    private final TaskDependency myDep;
    private Range<Date> myStartRange;
    private Range<Date> myEndRange;
    private final Node mySrcNode;
    private final Node myDstNode;
    private boolean isWeak = false;

    ExplicitDependencyImpl(TaskDependency dep, Node srcNode, Node dstNode) {
      myDep = dep;
      mySrcNode = srcNode;
      myDstNode = dstNode;
    }

    @Override
    public Range<Date> getStartRange() {
      return myStartRange;
    }

    @Override
    public Range<Date> getEndRange() {
      return myEndRange;
    }

    @Override
    public boolean refresh() {
      GPCalendar calendar = myDstNode.myTask.getManager().getCalendar();
      TaskDependencyConstraint.Collision nextCollision = myDep.getConstraint().getCollision();
      Date acceptableStart = nextCollision.getAcceptableStart().getTime();
      isWeak = !nextCollision.isActive() && myDep.getHardness() == Hardness.RUBBER;
      switch (nextCollision.getVariation()) {
      case TaskDependencyConstraint.Collision.START_EARLIER_VARIATION:
        if (calendar.isNonWorkingDay(acceptableStart)) {
          acceptableStart = calendar.findClosest(acceptableStart, myDstNode.myTask.getDuration().getTimeUnit(),
              GPCalendar.MoveDirection.BACKWARD, GPCalendar.DayType.WORKING);
        }
        myStartRange = Ranges.upTo(acceptableStart, BoundType.CLOSED);
        break;
      case TaskDependencyConstraint.Collision.START_LATER_VARIATION:
        if (calendar.isNonWorkingDay(acceptableStart)) {
          acceptableStart = calendar.findClosest(acceptableStart, myDstNode.myTask.getDuration().getTimeUnit(),
              GPCalendar.MoveDirection.FORWARD, GPCalendar.DayType.WORKING);
        }
        myStartRange = Ranges.downTo(acceptableStart, BoundType.CLOSED);
        break;
      case TaskDependencyConstraint.Collision.NO_VARIATION:
        myStartRange = Ranges.singleton(acceptableStart);
        break;
      }
      myEndRange = Ranges.all();
      return true;
    }

    @Override
    public boolean isWeak() {
      return isWeak;
    }

    @Override
    public Node getDst() {
      return myDstNode;
    }

    @Override
    public Node getSrc() {
      return mySrcNode;
    }

    @Override
    public int hashCode() {
      return myDep.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ExplicitDependencyImpl == false) {
        return false;
      }
      ExplicitDependencyImpl that = (ExplicitDependencyImpl) obj;
      return this.myDep.equals(that.myDep);
    }

    @Override
    public String toString() {
      return myDep.toString();
    }
  }

  /**
   * Implicit dependency between a subtask and a supertask. It puts constraints
   * on supertask bounds: it should start not later than subtask starts and
   * should start not earlier than subtask ends
   */
  static class ImplicitSubSuperTaskDependency implements DependencyEdge {

    private final Node mySubTask;
    private final Node mySuperTask;
    private Range<Date> myStartRange;
    private Range<Date> myEndRange;

    ImplicitSubSuperTaskDependency(Node subTask, Node superTask) {
      mySubTask = subTask;
      mySuperTask = superTask;
    }

    @Override
    public Range<Date> getStartRange() {
      return myStartRange;
    }

    @Override
    public Range<Date> getEndRange() {
      return myEndRange;
    }

    @Override
    public Node getDst() {
      return mySuperTask;
    }

    @Override
    public Node getSrc() {
      return mySubTask;
    }

    @Override
    public boolean refresh() {
      myStartRange = Ranges.upTo(mySubTask.myTask.getStart().getTime(), BoundType.CLOSED);
      myEndRange = Ranges.downTo(mySubTask.myTask.getEnd().getTime(), BoundType.CLOSED);
      return true;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(mySubTask, mySuperTask);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ImplicitSubSuperTaskDependency == false) {
        return false;
      }
      ImplicitSubSuperTaskDependency that = (ImplicitSubSuperTaskDependency) obj;
      return this.mySubTask.myTask.equals(that.mySubTask.myTask) && this.mySuperTask.myTask.equals(that.mySuperTask.myTask);
    }

    @Override
    public boolean isWeak() {
      return false;
    }

    @Override
    public String toString() {
      return mySubTask.toString() + " is a subtask of " + mySuperTask.toString();
    }
  }

  /**
   * Implicit dependency which is inherited by subtasks when explicit dependency to their supertask
   * is added to the graph.
   */
  static class ImplicitInheritedDependency implements DependencyEdge {
    private final DependencyEdge myExplicitDep;
    private final Node mySrc;
    private final Node myDst;

    private ImplicitInheritedDependency(DependencyEdge explicitIncoming, Node supertaskNode, Node subtaskNode) {
      assert explicitIncoming.getDst() == supertaskNode;
      myExplicitDep = explicitIncoming;
      mySrc = explicitIncoming.getSrc();
      myDst = subtaskNode;
    }

    @Override
    public Range<Date> getStartRange() {
      return myExplicitDep.getStartRange();
    }

    @Override
    public Range<Date> getEndRange() {
      return myExplicitDep.getEndRange();
    }

    @Override
    public Node getDst() {
      return myDst;
    }

    @Override
    public Node getSrc() {
      return mySrc;
    }

    @Override
    public boolean refresh() {
      return myExplicitDep.refresh();
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(mySrc.myTask, myDst.myTask);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ImplicitInheritedDependency == false) {
        return false;
      }
      ImplicitInheritedDependency that = (ImplicitInheritedDependency) obj;
      return this.mySrc.myTask.equals(that.mySrc.myTask) && this.myDst.myTask.equals(that.myDst.myTask);
    }

    @Override
    public boolean isWeak() {
      return myExplicitDep.isWeak();
    }

    @Override
    public String toString() {
      return "Dependency inherited from supertask:" + myExplicitDep.toString();
    }
  }

  public static class Node {
    private final Task myTask;
    private int myLevel = 0;
    private final List<DependencyEdge> myIncoming = Lists.newArrayList();
    private final List<DependencyEdge> myOutgoing = Lists.newArrayList();
    Node(Task task) {
      assert task != null;
      myTask = task;
    }

    void addOutgoing(DependencyEdge dep) {
      myOutgoing.add(dep);
    }

    void addIncoming(DependencyEdge dep) {
      myIncoming.add(dep);
    }

    boolean promoteLayer(Multimap<Integer, Node> layers) {
      int maxLevel = -1;
      for (DependencyEdge edge : myIncoming) {
        maxLevel = Math.max(maxLevel, edge.getSrc().getLevel());
      }
      if (maxLevel + 1 == myLevel) {
        return false;
      }
      layers.remove(myLevel, this);
      myLevel = maxLevel + 1;
      layers.put(myLevel, this);
      return true;
    }

    boolean demoteLayer(Multimap<Integer, Node> layers) {
      int maxLevel = -1;
      for (DependencyEdge edge : myIncoming) {
        maxLevel = Math.max(maxLevel, edge.getSrc().getLevel());
      }
      if (maxLevel + 1 == myLevel) {
        return false;
      }
      assert maxLevel + 1 < myLevel;
      layers.remove(myLevel, this);
      myLevel = maxLevel + 1;
      layers.put(myLevel, this);
      return true;
    }

    public int getLevel() {
      return myLevel;
    }

    public List<DependencyEdge> getOutgoing() {
      return myOutgoing;
    }

    public List<DependencyEdge> getIncoming() {
      return myIncoming;
    }

    void removeOutgoing(DependencyEdge edge) {
      myOutgoing.remove(edge);
    }

    void removeIncoming(DependencyEdge edge) {
      myIncoming.remove(edge);
    }

    public Task getTask() {
      return myTask;
    }

    @Override
    public String toString() {
      return myTask.toString();
    }
  }

  private final Multimap<Integer, Node> myLayers = TreeMultimap.<Integer, Node>create(new Comparator<Integer>() {
    @Override
    public int compare(Integer o1, Integer o2) {
      return o1.compareTo(o2);
    }
  }, new Comparator<Node>() {
    @Override
    public int compare(Node o1, Node o2) {
      return o1.myTask.getTaskID() - o2.myTask.getTaskID();
    }
  });

  private final Map<Task, Node> myNodeMap = Maps.newHashMap();

  private final Supplier<TaskContainmentHierarchyFacade> myTaskHierarchy;

  private final List<Listener> myListeners = Lists.newArrayList();

  private final Logger myLogger;

  public DependencyGraph(Supplier<TaskContainmentHierarchyFacade> taskHierarchy) {
    this(taskHierarchy, new Logger() {
      @Override
      public void log(String title, String message) {
        GPLogger.log(title + "\n" + message);
      }
    });
  }

  public DependencyGraph(Supplier<TaskContainmentHierarchyFacade> taskHierarchy, Logger logger) {
    myTaskHierarchy = taskHierarchy;
    myLogger = logger;
  }

  /**
   * Adds a task to the graph. It is expected that task which is added is a top-level task
   * with no subtasks
   *
   * @param t task being added
   */
  public void addTask(Task t) {
    assert t.getDependencies().toArray().length == 0;
    assert myTaskHierarchy.get().hasNestedTasks(t) == false;
    Node node = new Node(t);
    myLayers.put(0, node);
    myNodeMap.put(t, node);
    fireGraphChanged();
  }

  /**
   * Removes task from the graph with its incoming and outgoing edge
   * @param task task to remove
   */
  public void removeTask(Task task) {
    Node node = myNodeMap.get(task);
    if (node == null) {
      return;
    }
    for (DependencyEdge edge : Lists.newArrayList(node.getOutgoing())) {
      removeEdge(edge);
    }
    for (DependencyEdge edge : Lists.newArrayList(node.getIncoming())) {
      removeEdge(edge);
    }
    fireGraphChanged();
  }

  Node getNode(Task t) {
    return myNodeMap.get(t);
  }

  /**
   * Adds an explicit dependency. If dependency target is a node with
   * incoming sub-super task edges, recursively adds implicit inherited
   * dependencies to the subtree
   *
   * @param dep dependency to add
   */
  public void addDependency(TaskDependency dep) {
    Task srcTask = dep.getDependee();
    Node srcNode = myNodeMap.get(srcTask);
    if (srcNode == null) {
      return;
    }
    Node dstNode = myNodeMap.get(dep.getDependant());
    if (dstNode == null) {
      return;
    }

    DependencyEdge edge = new ExplicitDependencyImpl(dep, srcNode, dstNode);
    addEdge(edge);
    addInheritedDependencies(edge, dstNode);
    fireGraphChanged();
  }

  private void addInheritedDependencies(DependencyEdge edge, Node root) {
    Deque<Node> subtree = Lists.newLinkedList();
    subtree.add(root);
    while (!subtree.isEmpty()) {
      root = subtree.pollFirst();
      for (DependencyEdge incoming : root.getIncoming()) {
        if (incoming instanceof ImplicitSubSuperTaskDependency) {
          assert myTaskHierarchy.get().getContainer(incoming.getSrc().myTask) == root.myTask;
          ImplicitInheritedDependency implicitIncoming = new ImplicitInheritedDependency(edge, edge.getDst(), incoming.getSrc());
          addEdge(implicitIncoming);
          subtree.add(incoming.getSrc());
        }
      }
    }
  }

  private void addEdge(DependencyEdge edge) {
    edge.getSrc().addOutgoing(edge);
    edge.getDst().addIncoming(edge);
    PriorityQueue<Node> queue = new PriorityQueue<DependencyGraph.Node>(11, new Comparator<Node>() {
      @Override
      public int compare(Node o1, Node o2) {
        return o1.getLevel() - o2.getLevel();
      }
    });
    queue.add(edge.getDst());
    Map<Task, DependencyEdge> queuedTasks = Maps.newHashMap();
    Map<Task, DependencyEdge> pastTasks = Maps.newHashMap();

    pastTasks.put(edge.getSrc().getTask(), null);
    queuedTasks.put(edge.getDst().getTask(), edge);

    while (!queue.isEmpty()) {
      Node node = queue.poll();
      pastTasks.put(node.getTask(), queuedTasks.remove(node.getTask()));

      if (node.promoteLayer(myLayers)) {
        for (DependencyEdge outEdge : node.getOutgoing()) {
          if (!queuedTasks.containsKey(outEdge.getDst().getTask())) {
            if (pastTasks.containsKey(outEdge.getDst().getTask())) {
              myLogger.log("Dependency loop detected", buildLoop(pastTasks, outEdge) + "\n\nLast dependency has been ignored");
              continue;
            }
            queue.add(outEdge.getDst());
            queuedTasks.put(outEdge.getDst().getTask(), outEdge);
          }
        }
      }
    }
  }

  private static String buildLoop(Map<Task, DependencyEdge> pastTasks, DependencyEdge closingEdge) {
    List<String> trace = Lists.newArrayList();
    trace.add(closingEdge.toString());
    for (DependencyEdge prevEdge = pastTasks.get(closingEdge.getSrc().getTask());
        prevEdge != null; prevEdge = pastTasks.get(prevEdge.getSrc().getTask())) {
      trace.add(prevEdge.toString());
    }
    Collections.reverse(trace);
    return Joiner.on("<br>").join(trace);
  }

  /**
   * Removes explicit dependency. Also removes all inherited dependencies constructed from that one
   *
   * @param dep dependency to remove
   */
  public void removeDependency(TaskDependency dep) {
    Node srcNode = myNodeMap.get(dep.getDependee());
    Node dstNode = myNodeMap.get(dep.getDependant());
    if (srcNode == null && dstNode == null) {
      return;
    }
    assert (srcNode != null && dstNode != null) : "Inconsistent dependency graph state: for dep=" + dep + " one of the ends is missing";
    DependencyEdge diedEdge = findExplicitDependency(dep, srcNode, dstNode);
    if (diedEdge == null) {
      return;
    }
    removeEdge(diedEdge);
    for (DependencyEdge edge : Lists.newArrayList(srcNode.getOutgoing())) {
      if (edge instanceof ImplicitInheritedDependency) {
        if (((ImplicitInheritedDependency)edge).myExplicitDep == diedEdge) {
          removeEdge(edge);
        }
      }
    }
    fireGraphChanged();
  }

  private DependencyEdge findExplicitDependency(TaskDependency dep, Node srcNode, Node dstNode) {
    for (DependencyEdge edge : srcNode.getOutgoing()) {
      if (edge.getDst() == dstNode && edge instanceof ExplicitDependencyImpl) {
        if (((ExplicitDependencyImpl)edge).myDep == dep) {
          return edge;
        }
      }
    }
    return null;
  }

  private void removeEdge(DependencyEdge edge) {
    edge.getSrc().removeOutgoing(edge);
    edge.getDst().removeIncoming(edge);

    Deque<DependencyEdge> queue = new LinkedList<DependencyEdge>();
    queue.add(edge);
    while (!queue.isEmpty()) {
      edge = queue.pollFirst();
      if (edge.getDst().demoteLayer(myLayers)) {
        queue.addAll(edge.getDst().getOutgoing());
      }
    }
  }

  /**
   * Reflects moving tasks in the task hierarchy. In graph task move consists of removing
   * implicit inherited dependencies from the whole subtree being moved and adding new dependencies
   * in the destination.
   *
   * @param what task being moved
   * @param where new container or {@code null} if task is moved to the top level
   */
  public void move(Task what, Task where) {
    Node subNode = myNodeMap.get(what);
    if (subNode == null) {
      return;
    }
    boolean removedAny = removeImplicitDependencies(subNode);
    Node superNode = myNodeMap.get(where);
    if (superNode == null) {
      if (removedAny) {
        fireGraphChanged();
      }
      return;
    }

    for (DependencyEdge incomingEdge : Lists.newArrayList(superNode.getIncoming())) {
      if (incomingEdge instanceof ImplicitSubSuperTaskDependency == false) {
        if (incomingEdge instanceof ImplicitInheritedDependency) {
          incomingEdge = ((ImplicitInheritedDependency)incomingEdge).myExplicitDep;
        }
        ImplicitInheritedDependency implicitIncoming = new ImplicitInheritedDependency(incomingEdge, superNode, subNode);
        addEdge(implicitIncoming);
        addInheritedDependencies(incomingEdge, subNode);
      }
    }
    addEdge(new ImplicitSubSuperTaskDependency(subNode, superNode));
    fireGraphChanged();
  }

  private boolean removeImplicitDependencies(final Node root) {
    boolean removed = false;
    Deque<Node> queue = Lists.newLinkedList();
    queue.add(root);
    for (DependencyEdge outgoing : Lists.newArrayList(root.getOutgoing())) {
      if (outgoing instanceof ImplicitSubSuperTaskDependency) {
        removed = true;
        removeEdge(outgoing);
      }
    }
    while (!queue.isEmpty()) {
      Node node = queue.pollFirst();
      for (DependencyEdge incoming : Lists.newArrayList(node.getIncoming())) {
        if (incoming instanceof ImplicitInheritedDependency) {
          if (((ImplicitInheritedDependency)incoming).myExplicitDep.getDst() != root) {
            removed = true;
            removeEdge(incoming);
          }
        }
        if (incoming instanceof ImplicitSubSuperTaskDependency) {
          queue.add(incoming.getSrc());
        }
      }
    }
    return removed;
  }

  int checkLayerValidity() {
    int prev = -1;
    for (Integer num : myLayers.keySet()) {
      assert num == prev + 1;
      prev = num;
    }
    return myLayers.keySet().size();
  }

  public Collection<Node> getLayer(int num) {
    return myLayers.get(num);
  }

  public void addListener(Listener l) {
    myListeners.add(l);
  }

  private void fireGraphChanged() {
    for (Listener l : myListeners) {
      l.onChange();
    }
  }

  public void clear() {
    myLayers.clear();
    myNodeMap.clear();
  }
}
