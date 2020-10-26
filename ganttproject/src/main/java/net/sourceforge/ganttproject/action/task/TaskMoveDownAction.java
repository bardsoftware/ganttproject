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

import com.google.common.base.Predicate;
import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Move selected tasks down
 */
public class TaskMoveDownAction extends TaskActionBase {
  public TaskMoveDownAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade,
      GanttTree2 tree) {
    super("task.move.down", taskManager, selectionManager, uiFacade, tree);
  }

  @Override
  protected String getIconFilePrefix() {
    return "down_";
  }

  @Override
  protected boolean isEnabled(List<Task> selection) {
    if (selection.size() == 0) {
      return false;
    }
    TaskContainmentHierarchyFacade taskHierarchy = getTaskManager().getTaskHierarchy();
    for (Task task : selection) {
      if (taskHierarchy.getNextSibling(task) == null) {
        // task is the last child of the parent
        return false;
      }
    }
    return true;
  }

  @Override
  protected void run(List<Task> selection) throws Exception {
    getTree().commitIfEditing();
    final TaskContainmentHierarchyFacade taskHierarchy = getTaskManager().getTaskHierarchy();
    for (int i = selection.size() - 1; i >= 0; i--) {
      Task task = selection.get(i);
      final Task parent = taskHierarchy.getContainer(task);
      final int index = taskHierarchy.getTaskIndex(task) + 1;
      getTreeFacade().applyPreservingExpansionState(task, new Predicate<Task>() {
        public boolean apply(Task t) {
          taskHierarchy.move(t, parent, index);
          return true;
        }
      });
    }
    getTreeFacade().makeVisible(selection.get(selection.size() - 1));
    // TODO Ideally this should get done by the move method as it modifies the
    // document
    getUIFacade().getGanttChart().getProject().setModified();
  }

  public TaskMoveDownAction asToolbarAction() {
    final TaskMoveDownAction result = new TaskMoveDownAction(getTaskManager(), getSelectionManager(), getUIFacade(), getTree());
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    this.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if ("enabled".equals(evt.getPropertyName())) {
          result.setEnabled((Boolean)evt.getNewValue());
        }
      }
    });
    result.setEnabled(this.isEnabled());
    return result;
  }
}
