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

import com.google.common.collect.Sets;
import net.sourceforge.ganttproject.gui.TaskSelectionContext;
import net.sourceforge.ganttproject.task.event.TaskHierarchyEvent;
import net.sourceforge.ganttproject.task.event.TaskListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * This class manages the selected tasks.
 *
 * @author bbaranne
 */
public class TaskSelectionManager implements TaskSelectionContext {
  public interface Listener {
    void selectionChanged(List<Task> currentSelection, Object source);

    void userInputConsumerChanged(Object newConsumer);
  }

  /**
   * List of the selected tasks.
   */
  private final List<Task> selectedTasks = new ArrayList<>();
  private final List<Listener> myListeners = new ArrayList<>();
  private Object myUserInputConsumer;
  private final TaskManager myTaskManager;

  /**
   * Creates an instance of TaskSelectionManager
   */
  public TaskSelectionManager(Supplier<TaskManager> taskManager) {
    myTaskManager = taskManager.get();
    myTaskManager.addTaskListener(new TaskListenerAdapter() {
      @Override
      public void taskModelReset() {
        clear();
      }

      @Override
      public void taskRemoved(@NotNull TaskHierarchyEvent e) {
        selectedTasks.remove(e.getTask());
      }
    });
  }

  public void setUserInputConsumer(Object consumer) {
    var currentConsumer = myUserInputConsumer;
    myUserInputConsumer = consumer;
    if (consumer != currentConsumer) {
      fireUserInputConsumerChanged();
    }
  }


  private TaskContainmentHierarchyFacade getTaskHierarchy() {
    return myTaskManager.getTaskHierarchy();
  }

  public void setSelectedTasks(List<Task> tasks, Object source) {
    if (Sets.newHashSet(selectedTasks).equals(Sets.newHashSet(tasks))) {
      return;
    }
    // selection paths in Swing are stored in a hashtable
    // and thus come to selection listeners in pretty random order.
    // For correct indent/outdent operations with need
    // to order them the way they are ordered in the tree.
    var copy = new ArrayList<>(tasks);
    copy.sort((o1, o2) -> getTaskHierarchy().compareDocumentOrder(o1, o2));
    selectedTasks.clear();
    for (Task t : copy) {
      if (!t.isDeleted()) {
        selectedTasks.add(t);
      }
    }
    doFireSelectionChanged(new ArrayList<>(selectedTasks), source);
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

  /** Clears the selected tasks list. */
  public void clear() {
    selectedTasks.clear();
    fireSelectionChanged();
  }

  public void addSelectionListener(Listener listener) {
    myListeners.add(listener);
  }

  private void doFireSelectionChanged(List<Task> selection, Object source) {
    myListeners.forEach(l -> l.selectionChanged(selection, source));
  }

  public void fireSelectionChanged() {
    doFireSelectionChanged(new ArrayList<>(selectedTasks), myUserInputConsumer);
  }

  private void fireUserInputConsumerChanged() {
    myListeners.forEach(next -> next.userInputConsumerChanged(myUserInputConsumer));
  }
}
