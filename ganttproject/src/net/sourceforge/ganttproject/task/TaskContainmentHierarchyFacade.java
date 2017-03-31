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
package net.sourceforge.ganttproject.task;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sourceforge.ganttproject.util.collect.Pair;

import com.google.common.base.Predicate;

/**
 * @author bard
 */
public interface TaskContainmentHierarchyFacade {
  Task[] getNestedTasks(Task container);

  Task[] getDeepNestedTasks(Task container);

  boolean hasNestedTasks(Task container);

  Task getRootTask();

  Task getContainer(Task nestedTask);

  void sort(Comparator<Task> comparator);

  /**
   * @return the previous sibling or null if task is the first child of the
   *         parent task
   */
  Task getPreviousSibling(Task nestedTask);

  /**
   * @return the next sibling or null if task is the last child of the parent
   *         task
   */
  Task getNextSibling(Task task);

  /** @return the index of the nestedTask with respect of its siblings */
  int getTaskIndex(Task nestedTask);

  List<Integer> getOutlinePath(Task task);

  /** Move whatMove to whereMove, added as a child at the end */
  void move(Task whatMove, Task whereMove);

  /** Move whatMove to whereMove, added as a child at index */
  void move(Task whatMove, Task whereMove, int index);

  boolean areUnrelated(Task dependant, Task dependee);

  int getDepth(Task task);

  int compareDocumentOrder(Task next, Task dependeeTask);

  List<Task> getTasksInDocumentOrder();

  void breadthFirstSearch(Task root, final Predicate<Pair<Task, Task>> predicate);
  List<Task> breadthFirstSearch(Task root, boolean includeRoot);

  boolean contains(Task task);

  interface Factory {
    TaskContainmentHierarchyFacade createFacade();
  }

  TaskContainmentHierarchyFacade STUB = new TaskContainmentHierarchyFacade() {
    @Override
    public Task[] getNestedTasks(Task container) {
      return new Task[0];
    }

    @Override
    public Task[] getDeepNestedTasks(Task container) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean hasNestedTasks(Task container) {
      return false;
    }

    @Override
    public Task getRootTask() {
      return null;
    }

    @Override
    public Task getContainer(Task nestedTask) {
      return null;
    }

    @Override
    public void sort(Comparator<Task> comparator) {
    }

    @Override
    public Task getPreviousSibling(Task nestedTask) {
      return null;
    }

    @Override
    public Task getNextSibling(Task nestedTask) {
      return null;
    }

    @Override
    public int getTaskIndex(Task nestedTask) {
      return 0;
    }

    @Override
    public List<Integer> getOutlinePath(Task task) {
      return Collections.emptyList();
    }

    @Override
    public void move(Task whatMove, Task whereMove) {
    }

    @Override
    public void move(Task whatMove, Task whereMove, int index) {
    }

    @Override
    public boolean areUnrelated(Task dependant, Task dependee) {
      return false;
    }

    @Override
    public int getDepth(Task task) {
      return 0;
    }

    @Override
    public int compareDocumentOrder(Task next, Task dependeeTask) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Task task) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Task> getTasksInDocumentOrder() {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Task> breadthFirstSearch(Task root, boolean includeRoot) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void breadthFirstSearch(Task root, Predicate<Pair<Task, Task>> predicate) {
      throw new UnsupportedOperationException();
    }
  };
}
