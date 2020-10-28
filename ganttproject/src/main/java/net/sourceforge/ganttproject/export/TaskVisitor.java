/*
Copyright 2005-2012 GanttProject Team

This file is part of GanttProject, an opensource project management tool.

GanttProject is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

GanttProject is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sourceforge.ganttproject.export;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

/**
 * @author bard
 */
public abstract class TaskVisitor {
  public String visit(TaskManager taskManager) throws Exception {
    StringBuffer out = new StringBuffer();
    visit(taskManager.getTaskHierarchy().getRootTask(), 0, out);
    return out.toString();
  }

  void visit(Task task, int depth, StringBuffer out) throws Exception {
    Task[] nestedTasks = task.getManager().getTaskHierarchy().getNestedTasks(task);
    for (int i = 0; i < nestedTasks.length; i++) {
      Task next = nestedTasks[i];
      String nextSerialized = serializeTask(next, depth);
      out.append(nextSerialized);
      visit(next, depth + 1, out);
    }
  }

  protected abstract String serializeTask(Task t, int depth) throws Exception;
}
