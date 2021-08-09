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

import biz.ganttproject.ganttview.TaskTableActionConnector;
import kotlin.jvm.functions.Function0;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import static biz.ganttproject.task.TreeAlgorithmsKt.documentOrdered;
import static biz.ganttproject.task.TreeAlgorithmsKt.retainRoots;
import static com.google.common.collect.Lists.reverse;

/**
 * Unindent several nodes that are selected
 */
public class TaskUnindentAction extends TaskActionBase {
  private final Function0<TaskTableActionConnector> myTableConnector;

  public TaskUnindentAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade,
                            Function0<TaskTableActionConnector> tableConnector) {
    super("task.unindent", taskManager, selectionManager, uiFacade);
    myTableConnector = tableConnector;
  }

  @Override
  protected String getIconFilePrefix() {
    return "unindent_";
  }

  @Override
  protected boolean isEnabled(List<Task> selection) {
    return new TaskMoveEnabledPredicate(getTaskManager(), new OutdentTargetFunctionFactory(getTaskManager())).apply(selection);
  }

  @Override
  protected void run(List<Task> selection) throws Exception {
    getTaskManager().getAlgorithmCollection().getScheduler().setEnabled(false);
    try {
      final TaskContainmentHierarchyFacade taskHierarchy = getTaskManager().getTaskHierarchy();
      unindent(selection, taskHierarchy, (task, newParent, position) ->
          myTableConnector.invoke().getRunKeepingExpansion().invoke(task, t ->  {
            taskHierarchy.move(t, newParent, position);
            return null;
          })
      );
    } finally {
      getTaskManager().getAlgorithmCollection().getScheduler().setEnabled(true);
    }
  }

  public TaskUnindentAction asToolbarAction() {
    final TaskUnindentAction result = new TaskUnindentAction(getTaskManager(), getSelectionManager(), getUIFacade(), myTableConnector);
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    this.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if ("enabled".equals(evt.getPropertyName())) {
          result.setEnabled((Boolean) evt.getNewValue());
        }
      }
    });
    result.setEnabled(this.isEnabled());
    return result;
  }

  public interface UnindentApplyFxn {
    void apply(Task task, Task newParent, int position);
  }

  public static void unindent(List<Task> selectedTasks, TaskContainmentHierarchyFacade taskHierarchy, UnindentApplyFxn fxn) {
    reverse(documentOrdered(retainRoots(selectedTasks), taskHierarchy)).forEach(task -> {
      Task parent = taskHierarchy.getContainer(task);
      Task ancestor = taskHierarchy.getContainer(parent);
      int index = taskHierarchy.getTaskIndex(parent) + 1;
      fxn.apply(task, ancestor, index);
    });
  }
}
