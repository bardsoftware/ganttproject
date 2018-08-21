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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskDocumentOrderComparator;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;
import net.sourceforge.ganttproject.task.algorithm.RetainRootsAlgorithm;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;

/**
 * Indent several nodes that are selected
 */
public class TaskIndentAction extends TaskActionBase {
  private static final Function<Task, Task> getParentTask = new Function<Task, Task>() {
    @Override
    public Task apply(Task task) {
      return task.getManager().getTaskHierarchy().getContainer(task);
    }
  };
  private static final RetainRootsAlgorithm<Task> ourRetainRootsAlgorithm = new RetainRootsAlgorithm<>();

  public TaskIndentAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade,
                          GanttTree2 tree) {
    super("task.indent", taskManager, selectionManager, uiFacade, tree);
  }

  @Override
  protected String getIconFilePrefix() {
    return "indent_";
  }

  @Override
  protected boolean isEnabled(List<Task> selection) {
    TaskMoveEnabledPredicate predicate = new TaskMoveEnabledPredicate(getTaskManager(), new IndentTargetFunctionFactory(getTaskManager()));
    return predicate.apply(selection);
  }

  @Override
  protected void run(List<Task> selection) {
    getTaskManager().getAlgorithmCollection().getScheduler().setEnabled(false);
    try {
      final TaskContainmentHierarchyFacade taskHierarchy = getTaskManager().getTaskHierarchy();
      indent(selection, taskHierarchy, new IndentApplyFxn() {
        @Override
        public void apply(Task task, final Task newParent) {
          getTreeFacade().applyPreservingExpansionState(task, new Predicate<Task>() {
            @Override
            public boolean apply(Task t) {
              taskHierarchy.move(t, newParent);
              return true;
            }
          });
        }
      });
    } finally {
      getTaskManager().getAlgorithmCollection().getScheduler().setEnabled(true);
    }
  }

  public TaskIndentAction asToolbarAction() {
    final TaskIndentAction result = new TaskIndentAction(getTaskManager(), getSelectionManager(), getUIFacade(), getTree());
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

  public interface IndentApplyFxn {
    void apply(Task task, Task newParent);
  }

  public static void indent(List<Task> selectedTasks, TaskContainmentHierarchyFacade taskHierarchy, IndentApplyFxn fxn) {
    List<Task> indentRoots = Lists.newArrayList();
    ourRetainRootsAlgorithm.run(selectedTasks, getParentTask, indentRoots);
    Collections.sort(indentRoots, new TaskDocumentOrderComparator(taskHierarchy));
    for (Task task : indentRoots) {
      final Task newParent = taskHierarchy.getPreviousSibling(task);
      fxn.apply(task, newParent);
    }
  }
}
