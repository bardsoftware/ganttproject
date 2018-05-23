/*
GanttProject is an opensource project management tool.
Copyright (C) 2002-2011 Dmitry Barashev, GanttProject Team

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
package net.sourceforge.ganttproject.action.task;

import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.action.ActionDelegate;
import net.sourceforge.ganttproject.action.ActionStateChangedListener;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.TaskTreeUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class TaskActionBase extends GPAction implements TaskSelectionManager.Listener, ActionDelegate {
  private final List<ActionStateChangedListener> myListeners = new ArrayList<ActionStateChangedListener>();
  private final TaskManager myTaskManager;
  private final UIFacade myUIFacade;
  private final TaskSelectionManager mySelectionManager;
  private final GanttTree2 myTree;
  private List<Task> mySelection;

  protected TaskActionBase(String name, TaskManager taskManager, TaskSelectionManager selectionManager,
      UIFacade uiFacade, GanttTree2 tree) {
    this(name, taskManager, selectionManager, uiFacade, tree, IconSize.MENU);
  }

  protected TaskActionBase(String name, TaskManager taskManager, TaskSelectionManager selectionManager,
      UIFacade uiFacade, GanttTree2 tree, IconSize size) {
    super(name, size);
    myTaskManager = taskManager;
    mySelectionManager = selectionManager;
    myUIFacade = uiFacade;
    myTree = tree;
    selectionManager.addSelectionListener(this);
    selectionChanged(selectionManager.getSelectedTasks());
  }

  @Override
  public void addStateChangedListener(ActionStateChangedListener l) {
    myListeners.add(l);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (calledFromAppleScreenMenu(e)) {
      return;
    }
    final List<Task> selection = new ArrayList<Task>(mySelection);
    Collections.sort(selection, new Comparator<Task>() {
      private final TaskContainmentHierarchyFacade myTaskHierarchy = getTaskManager().getTaskHierarchy();
      @Override
      public int compare(Task o1, Task o2) {
        return myTaskHierarchy.compareDocumentOrder(o1, o2);
      }
    });
    if (isEnabled() && askUserPermission(selection)) {
      myUIFacade.getUndoManager().undoableEdit(getLocalizedDescription(), new Runnable() {
        @Override
        public void run() {
          try {
            TaskActionBase.this.run(selection);
          } catch (Exception e) {
            getUIFacade().showErrorDialog(e);
          }
        }
      });
    }
  }

  /**
   * @param selection
   *          of tasks for which permission is required
   * @return true if the operation is accepted by the user
   */
  protected boolean askUserPermission(List<Task> selection) {
    // Accept operation by default
    return true;
  }

  @Override
  public void selectionChanged(List<Task> currentSelection) {
    setEnabled(isEnabled(currentSelection));
    mySelection = currentSelection;
  }

  @Override
  public void setEnabled(boolean newValue) {
    super.setEnabled(newValue);
    for (ActionStateChangedListener l : myListeners) {
      l.actionStateChanged();
    }
  }

  @Override
  public void userInputConsumerChanged(Object newConsumer) {
  }

  protected TaskManager getTaskManager() {
    return myTaskManager;
  }

  protected TaskSelectionManager getSelectionManager() {
    return mySelectionManager;
  }

  protected UIFacade getUIFacade() {
    return myUIFacade;
  }

  protected GanttTree2 getTree() {
    return myTree;
  }

  protected TaskTreeUIFacade getTreeFacade() {
    return myTree;
  }

  protected void forwardScheduling() throws TaskDependencyException {
    // TODO 07 Sep 2011: It does seem necessary to reset() the charts: remove if
    // this indeed is the case
    // // TODO Find out which chart is opened and only reset that one (maybe add
    // a resetChart to UIFacade?)
    // myUIFacade.getGanttChart().reset();
    // myUIFacade.getResourceChart().reset();
    myTaskManager.getAlgorithmCollection().getRecalculateTaskScheduleAlgorithm().run();
    getUIFacade().getTaskTree().getTreeComponent().repaint();
  }

  protected abstract boolean isEnabled(List<Task> selection);

  protected abstract void run(List<Task> selection) throws Exception;
}
