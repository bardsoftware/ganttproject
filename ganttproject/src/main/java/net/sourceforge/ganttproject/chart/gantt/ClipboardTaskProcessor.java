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

import biz.ganttproject.core.option.StringOption;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.TaskManager.TaskBuilder;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyConstraint;
import net.sourceforge.ganttproject.task.dependency.TaskDependencyException;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

/**
 * Implements procedures for clipboard operations with tasks.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class ClipboardTaskProcessor {
  private final TaskManager myTaskManager;
  private boolean myTruncateExternalDeps;
  private boolean myTruncateAssignments;
  private StringOption myTaskCopyNameOption;

  public ClipboardTaskProcessor(TaskManager taskManager) {
    myTaskManager = taskManager;
  }

  public void setTruncateExternalDeps(boolean value) {
    myTruncateExternalDeps = value;
  }

  public void setTruncateAssignments(boolean value) {
    myTruncateAssignments = value;
  }

  public List<Task> pasteAsSibling(Task selectedTask, ClipboardContents clipboardContents) {
    Task pasteRoot = myTaskManager.getTaskHierarchy().getContainer(selectedTask);
    if (pasteRoot == null) {
      pasteRoot = myTaskManager.getRootTask();
      selectedTask = null;
    }
    return pasteAsChild(pasteRoot, selectedTask, clipboardContents);
  }

  public List<Task> pasteAsChild(Task selectedTask, ClipboardContents clipboardContents) {
    return pasteAsChild(selectedTask, null, clipboardContents);
  }

  private List<Task> pasteAsChild(Task pasteRoot, Task anchor, ClipboardContents clipboardContents) {
    List<Task> result = Lists.newArrayListWithExpectedSize(clipboardContents.getTasks().size());
    Map<Task, Task> original2copy = Maps.newHashMap();
    for (Task task : clipboardContents.getTasks()) {
      Task copy = copyAndInsert(task, pasteRoot, anchor, original2copy, clipboardContents);
      anchor = copy;
      result.add(copy);
    }
    copyDependencies(clipboardContents, original2copy);
    if (!myTruncateAssignments) {
      copyAssignments(clipboardContents, original2copy);
    }
    return result;
  }

  private void copyAssignments(ClipboardContents clipboardContents, Map<Task, Task> original2copy) {
    for (ResourceAssignment ra : clipboardContents.getAssignments()) {
      Task copy = original2copy.get(ra.getTask());
      if (copy == null) {
        continue;
      }
      ResourceAssignment newAssignment = copy.getAssignmentCollection().addAssignment(ra.getResource());
      newAssignment.setLoad(ra.getLoad());
      newAssignment.setRoleForAssignment(ra.getRoleForAssignment());
      newAssignment.setCoordinator(ra.isCoordinator());
    }
  }

  private void copyDependencies(ClipboardContents clipboardContents, Map<Task, Task> original2copy) {
    for (TaskDependency td : clipboardContents.getIntraDeps()) {
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

    if (myTruncateExternalDeps) {
      return;
    }

    for (TaskDependency td : clipboardContents.getIncomingDeps()) {
      Task predecessor = td.getDependee();
      Task newSuccessor = original2copy.get(td.getDependant());
      if (predecessor == null || newSuccessor == null) {
        continue;
      }
      TaskDependency newDep = myTaskManager.getDependencyCollection().createDependency(
          newSuccessor, predecessor, myTaskManager.createConstraint(td.getConstraint().getType()));
      newDep.setDifference(td.getDifference());
      newDep.setHardness(td.getHardness());
    }
    for (TaskDependency td : clipboardContents.getOutgoingDeps()) {
      Task successor = td.getDependant();
      Task newPredecessor = original2copy.get(td.getDependee());
      if (newPredecessor == null || successor == null) {
        continue;
      }
      TaskDependency newDep = myTaskManager.getDependencyCollection().createDependency(
          successor, newPredecessor, myTaskManager.createConstraint(td.getConstraint().getType()));
      newDep.setDifference(td.getDifference());
      newDep.setHardness(td.getHardness());
    }
  }

  private Task copyAndInsert(Task task, Task newContainer, Task prevSibling, Map<Task, Task> original2copy, ClipboardContents clipboardContents) {
    TaskBuilder builder = myTaskManager.newTaskBuilder()
        .withPrototype(task)
        .withParent(newContainer)
        .withPrevSibling(prevSibling);
    if (clipboardContents.isCut()) {
      builder = builder.withId(task.getTaskID()).withName(task.getName());
    } else {
      String newName = (myTaskCopyNameOption == null)
          ? task.getName()
          : MessageFormat.format(
              myTaskCopyNameOption.getValue(),
              GanttLanguage.getInstance().getText("copy2"),
              task.getName());
      builder = builder.withName(newName);
    }
    Task result = builder.build();
    if (myTruncateAssignments) {
      result.getAssignmentCollection().clear();
    }
    original2copy.put(task, result);
    Task anchor = null;
    for (Task child : clipboardContents.getNestedTasks(task)) {
      Task copied = copyAndInsert(child, result, anchor, original2copy, clipboardContents);
      anchor = copied;
    }
    return result;
  }

  public boolean canMove(Task dropTarget, ClipboardContents clipboard) {
    TaskContainmentHierarchyFacade hierarchy = myTaskManager.getTaskHierarchy();
    for (Task t : clipboard.getTasks()) {
      if (!hierarchy.areUnrelated(t, dropTarget)) {
        return false;
      }
    }
    return true;
  }

  public void setTaskCopyNameOption(StringOption option) {
    myTaskCopyNameOption = option;
  }
}
