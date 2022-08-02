/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev

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

import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskSelectionManager;

import java.util.List;

import static biz.ganttproject.task.TreeAlgorithmsKt.retainRoots;

public class TaskDeleteAction extends TaskActionBase {

  public TaskDeleteAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade) {
    super("task.delete", taskManager, selectionManager, uiFacade);
  }

  private TaskDeleteAction(TaskManager taskManager, TaskSelectionManager selectionManager, UIFacade uiFacade,
      IconSize size) {
    super("task.delete", taskManager, selectionManager, uiFacade, size);
  }

  @Override
  protected boolean isEnabled(List<Task> selection) {
    return !selection.isEmpty();
  }

  @Override
  protected void run(List<Task> selection) throws Exception {
    List<Task> roots = retainRoots(selection);
    roots.forEach((task) -> getTaskManager().deleteTask(task));
  }

  @Override
  public TaskDeleteAction asToolbarAction() {
    TaskDeleteAction result = new TaskDeleteAction(getTaskManager(), getSelectionManager(), getUIFacade());
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    return result;
  }
}
