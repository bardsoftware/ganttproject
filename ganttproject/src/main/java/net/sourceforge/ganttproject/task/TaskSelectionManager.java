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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Supplier;

import net.sourceforge.ganttproject.gui.TaskSelectionContext;

/**
 * This class manages the selected tasks.
 *
 * @author bbaranne
 */
public class TaskSelectionManager implements TaskSelectionContext {
  public interface Listener {
    void selectionChanged(List<Task> currentSelection);

    void userInputConsumerChanged(Object newConsumer);
  }

  /**
   * List of the selected tasks.
   */
  private final List<Task> selectedTasks = new ArrayList<Task>();
  private final List<Listener> myListeners = new ArrayList<Listener>();
  private Object myUserInputConsumer;
  private final Supplier<TaskManager> myTaskManager;

  /**
   * Creates an instance of TaskSelectionManager
   */
  public TaskSelectionManager(Supplier<TaskManager> taskManager) {
    myTaskManager = taskManager;
  }

  public void setUserInputConsumer(Object consumer) {
    if (consumer != myUserInputConsumer) {
      fireUserInputConsumerChanged();
    }
    myUserInputConsumer = consumer;
  }

  /**
   * Adds <code>task</code> to the selected tasks.
   *
   * @param task
   *          A task to add to the selected tasks.
   */
  public void addTask(Task task) {
    if (!selectedTasks.contains(task)) {
      selectedTasks.add(task);
      fireSelectionChanged();
    }
  }

  /**
   * Removes <code>task</code> from the selected tasks;
   *
   * @param task
   *          A task to remove from the selected tasks.
   */
  public void removeTask(Task task) {
    if (selectedTasks.contains(task)) {
      selectedTasks.remove(task);
      fireSelectionChanged();
    }
  }

  private TaskContainmentHierarchyFacade getTaskHierarchy() {
    return myTaskManager.get().getTaskHierarchy();
  }

  public void setSelectedTasks(List<Task> tasks) {
    // selection paths in Swing are stored in a hashtable
    // and thus come to selection listeners in pretty random order.
    // For correct indent/outdent operations with need
    // to order them the way they are ordered in the tree.
    Collections.sort(tasks, new Comparator<Task>() {
      @Override
      public int compare(Task o1, Task o2) {
        return getTaskHierarchy().compareDocumentOrder(o1, o2);
      }
    });
    clear();
    for (Task t : tasks) {
      addTask(t);
    }
  }
  /**
   * @param task
   *          The task to test.
   * @return <code>true</code> if <code>task</code> is selected,
   *         <code>false</code> otherwise.
   */
  public boolean isTaskSelected(Task task) {
    return selectedTasks.contains(task);
  }

  /** @return The selected tasks list. */
  @Override
  public List<Task> getSelectedTasks() {
    return Collections.unmodifiableList(selectedTasks);
  }

  /** @return The earliest start date. */
  public Date getEarliestStart() {
    Date res = null;
    Iterator<Task> it = selectedTasks.iterator();
    while (it.hasNext()) {

      Task task = it.next();
      Date d = task.getStart().getTime();
      if (res == null) {
        res = d;
        continue;
      }
      if (d.before(res))
        res = d;
    }
    return res;
  }

  /** @return The latest end date. */
  public Date getLatestEnd() {
    Date res = null;
    Iterator<Task> it = selectedTasks.iterator();
    while (it.hasNext()) {
      Task task = it.next();
      Date d = task.getEnd().getTime();
      if (res == null) {
        res = d;
        continue;
      }
      if (d.after(res))
        res = d;
    }
    return res;
  }

  /** Clears the selected tasks list. */
  public void clear() {
    selectedTasks.clear();
    fireSelectionChanged();
  }

  public void addSelectionListener(Listener listener) {
    myListeners.add(listener);
  }

  public void removeSelectionListener(Listener listener) {
    myListeners.remove(listener);
  }

  public void fireSelectionChanged() {
    for (int i = 0; i < myListeners.size(); i++) {
      Listener next = myListeners.get(i);
      next.selectionChanged(Collections.unmodifiableList(selectedTasks));
    }
  }

  private void fireUserInputConsumerChanged() {
    for (int i = 0; i < myListeners.size(); i++) {
      Listener next = myListeners.get(i);
      next.userInputConsumerChanged(myUserInputConsumer);
    }
  }
}
