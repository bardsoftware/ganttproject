/*
 * Created on 02.05.2005
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
