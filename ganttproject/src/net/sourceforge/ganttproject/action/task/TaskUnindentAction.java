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

import java.util.List;

import net.sourceforge.ganttproject.GanttTree2;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

/**
 * Unindent several nodes that are selected
 */
public class TaskUnindentAction extends TaskActionBase {

  public TaskUnindentAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade,
      GanttTree2 tree) {
    super("task.unindent", taskManager, selectionManager, uiFacade, tree);
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
    TaskContainmentHierarchyFacade taskHierarchy = getTaskManager().getTaskHierarchy();
    for (int i = selection.size() - 1; i >= 0; i--) {
      // Place task at ancestor children right after parent
      Task task = selection.get(i);
      Task parent = taskHierarchy.getContainer(task);
      Task ancestor = taskHierarchy.getContainer(parent);
      int index = taskHierarchy.getTaskIndex(parent) + 1;
      taskHierarchy.move(task, ancestor, index);
    }
    // TODO Ideally this should get done by the move method as it modifies the
    // document
    //getUIFacade().getGanttChart().getProject().setModified();
  }
}
