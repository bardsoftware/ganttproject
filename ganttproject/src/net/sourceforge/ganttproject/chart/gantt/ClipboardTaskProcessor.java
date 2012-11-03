/*
Copyright 2012 GanttProject Team

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
package net.sourceforge.ganttproject.chart.gantt;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManager.TaskBuilder;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Implements procedures for clipboard operations with tasks.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class ClipboardTaskProcessor {
  private final TaskManager myTaskManager;

  public ClipboardTaskProcessor(TaskManager taskManager) {
    myTaskManager = taskManager;
  }

  public List<Task> paste(
      Task selectedTask, List<DefaultMutableTreeTableNode> nodes, List<TaskDependency> deps) {
    Task pasteRoot = myTaskManager.getTaskHierarchy().getContainer(selectedTask);
    if (pasteRoot == null) {
      pasteRoot = myTaskManager.getRootTask();
      selectedTask = null;
    }

    List<Task> result = Lists.newArrayListWithExpectedSize(nodes.size());
    Map<Task, Task> original2copy = Maps.newHashMap();
    for (DefaultMutableTreeTableNode taskNode : nodes) {
      Task task = (Task) taskNode.getUserObject();
      Task copy = copyAndInsert(task, pasteRoot, selectedTask, original2copy);
      result.add(copy);
    }
    copyDependencies(deps, original2copy);
    return result;
  }

  private void copyDependencies(List<TaskDependency> deps, Map<Task, Task> original2copy) {
    for (TaskDependency td : deps) {
      Task dependee = td.getDependee();
      Task dependant = td.getDependant();
      TaskDependencyConstraint constraint = td.getConstraint();
      try {
        TaskDependency dep = myTaskManager.getDependencyCollection().createDependency(
            original2copy.get(dependant),
            original2copy.get(dependee),
            myTaskManager.createConstraint(constraint.getType()));
        dep.setDifference(td.getDifference());
        dep.setHardness(td.getHardness());
      } catch (TaskDependencyException e) {
        GPLogger.log(e);
      }
    }
  }

  private Task copyAndInsert(Task task, Task newContainer, Task prevSibling, Map<Task, Task> original2copy) {
    TaskBuilder builder = myTaskManager.newTaskBuilder().withPrototype(task).withParent(newContainer).withPrevSibling(prevSibling);
    String newName = MessageFormat.format(myTaskManager.getTaskCopyNamePrefixOption().getValue(), GanttLanguage.getInstance().getText("copy2"), task.getName());
    builder = builder.withName(newName);
    Task result = builder.build();
    original2copy.put(task, result);
    for (Task child : myTaskManager.getTaskHierarchy().getNestedTasks(task)) {
      copyAndInsert(child, result, null, original2copy);
    }
    return result;
  }
}
