/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.dependency.TaskDependency.Hardness;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishFinishConstraintImpl;
import net.sourceforge.ganttproject.task.dependency.constraint.FinishStartConstraintImpl;

/**
 * Created by IntelliJ IDEA. User: bard Date: 14.02.2004 Time: 16:02:48 To
 * change this template use File | Settings | File Templates.
 */
public class TaskDependencyCollectionImpl implements TaskDependencyCollection {
  private Set<TaskDependency> myDependencies = new HashSet<TaskDependency>();

  private SortedMap<SearchKey, TaskDependency> mySearchKey2dependency = new TreeMap<SearchKey, TaskDependency>();

  private final EventDispatcher myEventDispatcher;

  private final TaskContainmentHierarchyFacade.Factory myTaskHierarchyFactory;

  public TaskDependencyCollectionImpl(TaskContainmentHierarchyFacade.Factory taskHierarchyFactory,
      EventDispatcher myEventDispatcher) {
    this.myEventDispatcher = myEventDispatcher;
    myTaskHierarchyFactory = taskHierarchyFactory;
  }

  @Override
  public TaskDependency[] getDependencies() {
    return myDependencies.toArray(new TaskDependency[0]);
  }

  @Override
  public TaskDependency[] getDependencies(Task task) {
    SearchKey fromKey = new RangeSearchFromKey(task);
    SearchKey toKey = new RangeSearchToKey(task);
    SortedMap<SearchKey, TaskDependency> submap = mySearchKey2dependency.subMap(fromKey, toKey);
    return submap.values().toArray(new TaskDependency[0]);
  }

  @Override
  public TaskDependency[] getDependenciesAsDependant(Task dependant) {
    SearchKey fromKey = new SearchKey(SearchKey.DEPENDANT, dependant.getTaskID(), -1);
    SearchKey toKey = new SearchKey(SearchKey.DEPENDEE, dependant.getTaskID(), -1);
    SortedMap<SearchKey, TaskDependency> submap = mySearchKey2dependency.subMap(fromKey, toKey);
    return submap.values().toArray(new TaskDependency[0]);
  }

  @Override
  public TaskDependency[] getDependenciesAsDependee(Task dependee) {
    SearchKey fromKey = new SearchKey(SearchKey.DEPENDEE, dependee.getTaskID(), -1);
    SearchKey toKey = new SearchKey(Integer.MAX_VALUE, dependee.getTaskID(), -1);
    SortedMap<SearchKey, TaskDependency> submap = mySearchKey2dependency.subMap(fromKey, toKey);
    return submap.values().toArray(new TaskDependency[0]);
  }

  @Override
  public TaskDependency createDependency(Task dependant, Task dependee) throws TaskDependencyException {
    return createDependency(dependant, dependee, new FinishStartConstraintImpl());
  }

  @Override
  public TaskDependency createDependency(Task dependant, Task dependee, TaskDependencyConstraint constraint)
      throws TaskDependencyException {
    return createDependency(dependant, dependee, constraint, getDefaultHardness());
  }

  protected TaskDependency.Hardness getDefaultHardness() {
    return TaskDependency.Hardness.STRONG;
  }
  @Override
  public TaskDependency createDependency(Task dependant, Task dependee, TaskDependencyConstraint constraint,
      Hardness hardness) throws TaskDependencyException {
    TaskDependency result = auxCreateDependency(dependant, dependee, constraint, hardness);
    addDependency(result);
    return result;
  }

  @Override
  public boolean canCreateDependency(Task dependant, Task dependee) {
    if (dependant == dependee) {
      return false;
    }
    if (false == getTaskHierarchy().areUnrelated(dependant, dependee)) {
      return false;
    }
    SearchKey key = new SearchKey(SearchKey.DEPENDANT, dependant.getTaskID(), dependee.getTaskID());
    if (mySearchKey2dependency.containsKey(key)) {
      return false;
    }
    TaskDependency testDep = new TaskDependencyImpl(dependant, dependee, this);
    if (isLooping(testDep)) {
      return false;
    }
    return true;
  }

  @Override
  public void deleteDependency(TaskDependency dependency) {
    delete(dependency);
  }

  void fireChanged(TaskDependency dependency) {
    myEventDispatcher.fireDependencyChanged(dependency);
  }

  @Override
  public void clear() {
    doClear();
  }

  @Override
  public TaskDependencyCollectionMutator createMutator() {
    return new MutatorImpl();
  }

  private class MutatorImpl implements TaskDependencyCollectionMutator {
    private Map<TaskDependency, MutationInfo> myQueue = new LinkedHashMap<TaskDependency, MutationInfo>();

    private MutationInfo myCleanupMutation;

    @Override
    public void commit() {
      List<MutationInfo> mutations = new ArrayList<MutationInfo>(myQueue.values());
      if (myCleanupMutation != null) {
        mutations.add(myCleanupMutation);
      }
      Collections.sort(mutations);
      for (int i = 0; i < mutations.size(); i++) {
        MutationInfo next = mutations.get(i);
        switch (next.myOperation) {
        case MutationInfo.ADD: {
          try {
            addDependency(next.myDependency);
          } catch (TaskDependencyException e) {
            if (!GPLogger.log(e)) {
              e.printStackTrace(System.err);
            }
          }
          break;
        }
        case MutationInfo.DELETE: {
          delete(next.myDependency);
          break;
        }
        case MutationInfo.CLEAR: {
          doClear();
          break;
        }
        }
      }
    }

    @Override
    public void clear() {
      myQueue.clear();
      myCleanupMutation = new MutationInfo(null, MutationInfo.CLEAR);
    }

    @Override
    public TaskDependency createDependency(Task dependant, Task dependee) throws TaskDependencyException {
      return createDependency(dependant, dependee, new FinishFinishConstraintImpl());
    }

    @Override
    public TaskDependency createDependency(Task dependant, Task dependee, TaskDependencyConstraint constraint)
        throws TaskDependencyException {
      return createDependency(dependant, dependee, constraint, TaskDependency.Hardness.STRONG);
    }

    @Override
    public TaskDependency createDependency(Task dependant, Task dependee, TaskDependencyConstraint constraint,
        Hardness hardness) throws TaskDependencyException {
      TaskDependency result = auxCreateDependency(dependant, dependee, constraint, hardness);
      myQueue.put(result, new MutationInfo(result, MutationInfo.ADD));
      return result;
    }


    @Override
    public void deleteDependency(TaskDependency dependency) {
      MutationInfo info = myQueue.get(dependency);
      if (info == null) {
        myQueue.put(dependency, new MutationInfo(dependency, MutationInfo.DELETE));
      } else if (info.myOperation == MutationInfo.ADD) {
        myQueue.remove(dependency);
      }
    }
  }

  private static class MutationInfo implements Comparable<MutationInfo> {
    static final int ADD = 0;

    static final int DELETE = 1;

    static final int CLEAR = 2;

    final TaskDependency myDependency;

    final int myOperation;

    final int myOrder = ourOrder++;

    static int ourOrder;

    public MutationInfo(TaskDependency myDependency, int myOperation) {
      this.myDependency = myDependency;
      this.myOperation = myOperation;
    }

    @Override
    public int compareTo(MutationInfo rvalue) {
      return myOrder - rvalue.myOrder;
    }
  }

  private TaskDependency auxCreateDependency(Task dependant, Task dependee, TaskDependencyConstraint constraint, Hardness hardness) {
    TaskDependency result = new TaskDependencyImpl(dependant, dependee, this, constraint, hardness, 0);
    return result;
  }

  void addDependency(TaskDependency dep) throws TaskDependencyException {
    if (myDependencies.contains(dep)) {
      throw new TaskDependencyException("Dependency=" + dep + " already exists");
    }
    if (this.isLooping(dep)) {
      throw new TaskDependencyException("Dependency=" + dep + " is looping");
    }
    if (false == getTaskHierarchy().areUnrelated(dep.getDependant(), dep.getDependee())) {
      throw new TaskDependencyException("In dependency=" + dep + " one of participants is a supertask of another");
    }
    myDependencies.add(dep);
    //
    mySearchKey2dependency.put(new SearchKey(SearchKey.DEPENDANT, (TaskDependencyImpl) dep), dep);
    mySearchKey2dependency.put(new SearchKey(SearchKey.DEPENDEE, (TaskDependencyImpl) dep), dep);
    myEventDispatcher.fireDependencyAdded(dep);
  }

  boolean isLooping(TaskDependency dep) {
    LoopDetector detector = new LoopDetector(dep.getDependant().getManager());
    return detector.isLooping(dep);
  }

  boolean _isLooping(TaskDependency dep) {
    Set<Task> tasksInvolved = new HashSet<Task>();
    tasksInvolved.add(dep.getDependee());
    return _isLooping(dep, tasksInvolved);
  }

  private boolean _isLooping(TaskDependency dep, Set<Task> tasksInvolved) {
    Task dependant = dep.getDependant();
    if (tasksInvolved.contains(dependant)) {
      return true;
    }
    for (Iterator<Task> tasks = tasksInvolved.iterator(); tasks.hasNext();) {
      Task nextInvolved = tasks.next();
      if (false == getTaskHierarchy().areUnrelated(nextInvolved, dependant)) {
        return true;
      }
    }
    tasksInvolved.add(dependant);
    {
      TaskDependency[] nextDeps = dependant.getDependenciesAsDependee().toArray();
      for (int i = 0; i < nextDeps.length; i++) {
        if (_isLooping(nextDeps[i], tasksInvolved)) {
          return true;
        }
      }
    }
    Task[] nestedTasks = getTaskHierarchy().getNestedTasks(dependant);
    for (int i = 0; i < nestedTasks.length; i++) {
      tasksInvolved.add(nestedTasks[i]);
      TaskDependency[] nextDeps = nestedTasks[i].getDependenciesAsDependee().toArray();
      for (int j = 0; j < nextDeps.length; j++) {
        if (_isLooping(nextDeps[j], tasksInvolved)) {
          return true;
        }
      }

    }
    tasksInvolved.remove(dependant);
    return false;
  }

  void delete(TaskDependency dep) {
    myDependencies.remove(dep);
    SearchKey key1 = new SearchKey(SearchKey.DEPENDANT, dep.getDependant().getTaskID(), dep.getDependee().getTaskID());
    SearchKey key2 = new SearchKey(SearchKey.DEPENDEE, dep.getDependee().getTaskID(), dep.getDependant().getTaskID());
    mySearchKey2dependency.remove(key1);
    mySearchKey2dependency.remove(key2);
    myEventDispatcher.fireDependencyRemoved(dep);
    // SearchKey fromKey = new RangeSearchFromKey(dep.getDependant());
    // SearchKey toKey = new RangeSearchToKey(dep.getDependant());
    // mySearchKey2dependency.subMap(fromKey, toKey).clear();
    // fromKey = new RangeSearchFromKey(dep.getDependee());
    // toKey = new RangeSearchToKey(dep.getDependee());
    // mySearchKey2dependency.subMap(fromKey, toKey).clear();
  }

  public void doClear() {
    myDependencies.clear();
    mySearchKey2dependency.clear();
  }

  protected TaskContainmentHierarchyFacade getTaskHierarchy() {
    return myTaskHierarchyFactory.createFacade();
  }

}
