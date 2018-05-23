/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2012 GanttProject Team

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
package net.sourceforge.ganttproject.chart.gantt;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import net.sourceforge.ganttproject.GanttTask;
import net.sourceforge.ganttproject.TestSetupHelper;
import net.sourceforge.ganttproject.TestSetupHelper.TaskManagerBuilder;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;

import java.util.List;

/**
 * Tests for clipboard operations with tasks.
 *
 * @author dbarashev
 */
public class ClipboardTaskProcessorTest extends TestCase {
  public void testCopyDates() {
    TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder().build();
    GanttTask task = taskManager.createTask();
    task.setDuration(taskManager.createLength(3));

    ClipboardContents contents = new ClipboardContents(taskManager);
    contents.addTasks(ImmutableList.<Task>of(task));
    contents.copy();
    ClipboardTaskProcessor clipboardProcessor = new ClipboardTaskProcessor(taskManager);
    Task pastedTask = clipboardProcessor.pasteAsSibling(taskManager.getRootTask(), contents).get(0);
    assertEquals(task.getStart(), pastedTask.getStart());
    assertEquals(task.getEnd(), pastedTask.getEnd());
    assertEquals(task.getDuration(), pastedTask.getDuration());
  }

  public void testIntraDependencies() {
    TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder().build();
    Task task1 = taskManager.newTaskBuilder().build();
    Task task2 = taskManager.newTaskBuilder().build();
    taskManager.getDependencyCollection().createDependency(task2, task1);
    Task target = taskManager.newTaskBuilder().build();

    ClipboardContents contents = new ClipboardContents(taskManager);
    contents.addTasks(ImmutableList.of(task1, task2));
    contents.copy();
    ClipboardTaskProcessor clipboardProcessor = new ClipboardTaskProcessor(taskManager);
    List<Task> pasted = clipboardProcessor.pasteAsSibling(target, contents);
    assertEquals(2, pasted.size());
    assertTrue(pasted.get(0).getDependencies().hasLinks(pasted));
    assertTrue(pasted.get(1).getDependencies().hasLinks(pasted));
  }

  public void testExtraDependencies() {
    TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder().build();
    Task task1 = taskManager.newTaskBuilder().build();
    Task task2 = taskManager.newTaskBuilder().build();

    Task predecessor = taskManager.newTaskBuilder().build();
    taskManager.getDependencyCollection().createDependency(task1, predecessor);
    taskManager.getDependencyCollection().createDependency(task2, predecessor);
    Task target = taskManager.newTaskBuilder().build();

    ClipboardContents contents = new ClipboardContents(taskManager);
    contents.addTasks(ImmutableList.of(task1, task2));
    contents.copy();
    ClipboardTaskProcessor clipboardProcessor = new ClipboardTaskProcessor(taskManager);
    List<Task> pasted = clipboardProcessor.pasteAsSibling(target, contents);
    assertEquals(2, pasted.size());
    assertTrue(pasted.get(0).getDependencies().hasLinks(ImmutableList.of(pasted.get(0), predecessor)));
    assertTrue(pasted.get(1).getDependencies().hasLinks(ImmutableList.of(pasted.get(1), predecessor)));
  }

  public void testTruncateExternalDependencies() {
    TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder().build();
    Task task1 = taskManager.newTaskBuilder().build();
    Task task2 = taskManager.newTaskBuilder().build();

    Task predecessor = taskManager.newTaskBuilder().build();
    Task successor = taskManager.newTaskBuilder().build();
    taskManager.getDependencyCollection().createDependency(task1, predecessor);
    taskManager.getDependencyCollection().createDependency(task2, task1);
    taskManager.getDependencyCollection().createDependency(successor, task2);

    Task target = taskManager.newTaskBuilder().build();

    ClipboardContents contents = new ClipboardContents(taskManager);
    contents.addTasks(ImmutableList.of(task1, task2));
    contents.copy();
    ClipboardTaskProcessor clipboardProcessor = new ClipboardTaskProcessor(taskManager);
    clipboardProcessor.setTruncateExternalDeps(true);
    List<Task> pasted = clipboardProcessor.pasteAsSibling(target, contents);
    assertEquals(2, pasted.size());
    assertEquals(1, predecessor.getDependencies().toArray().length);
    assertEquals(1, successor.getDependencies().toArray().length);
    assertEquals(1, pasted.get(1).getDependencies().toArray().length);
    assertEquals(1, pasted.get(0).getDependencies().toArray().length);
  }

  public void testAssignmentsCopy() {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    HumanResourceManager hrMgr = builder.getResourceManager();
    TaskManager taskManager = builder.build();
    Task task1 = taskManager.newTaskBuilder().build();

    HumanResource res1 = new HumanResource("Joe", 1, hrMgr);
    hrMgr.add(res1);
    ResourceAssignment assgn1 = task1.getAssignmentCollection().addAssignment(res1);
    assgn1.setLoad(100f);

    ClipboardContents contents = new ClipboardContents(taskManager);
    contents.addTasks(ImmutableList.of(task1));
    contents.copy();
    ClipboardTaskProcessor clipboardProcessor = new ClipboardTaskProcessor(taskManager);
    List<Task> pasted = clipboardProcessor.pasteAsSibling(taskManager.getRootTask(), contents);
    ResourceAssignment[] newAssignments = pasted.get(0).getAssignments();
    assertEquals(1, newAssignments.length);
    assertEquals(res1, newAssignments[0].getResource());
    assertEquals(100f, newAssignments[0].getLoad());
    assertEquals(2, res1.getAssignments().length);
  }

  public void testAssignmentsCut() {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    HumanResourceManager hrMgr = builder.getResourceManager();
    TaskManager taskManager = builder.build();
    Task task1 = taskManager.newTaskBuilder().build();

    HumanResource res1 = new HumanResource("Joe", 1, hrMgr);
    hrMgr.add(res1);
    ResourceAssignment assgn1 = task1.getAssignmentCollection().addAssignment(res1);
    assgn1.setLoad(100f);

    ClipboardContents contents = new ClipboardContents(taskManager);
    contents.addTasks(ImmutableList.of(task1));
    contents.cut();

    assertEquals(0, res1.getAssignments().length);

    ClipboardTaskProcessor clipboardProcessor = new ClipboardTaskProcessor(taskManager);
    List<Task> pasted = clipboardProcessor.pasteAsSibling(taskManager.getRootTask(), contents);
    ResourceAssignment[] newAssignments = pasted.get(0).getAssignments();
    assertEquals(1, newAssignments.length);
    assertEquals(res1, newAssignments[0].getResource());
    assertEquals(100f, newAssignments[0].getLoad());
    assertEquals(1, res1.getAssignments().length);
  }

  public void testAssignmentsTruncated() {
    TaskManagerBuilder builder = TestSetupHelper.newTaskManagerBuilder();
    HumanResourceManager hrMgr = builder.getResourceManager();
    TaskManager taskManager = builder.build();
    Task task1 = taskManager.newTaskBuilder().build();

    HumanResource res1 = new HumanResource("Joe", 1, hrMgr);
    hrMgr.add(res1);
    ResourceAssignment assgn1 = task1.getAssignmentCollection().addAssignment(res1);
    assgn1.setLoad(100f);

    ClipboardContents contents = new ClipboardContents(taskManager);
    contents.addTasks(ImmutableList.of(task1));
    contents.copy();
    ClipboardTaskProcessor clipboardProcessor = new ClipboardTaskProcessor(taskManager);
    clipboardProcessor.setTruncateAssignments(true);

    List<Task> pasted = clipboardProcessor.pasteAsSibling(taskManager.getRootTask(), contents);
    ResourceAssignment[] newAssignments = pasted.get(0).getAssignments();
    assertEquals(0, newAssignments.length);
  }

  /**
   * Tests that tasks cut and pasted with the help of ClipboardContents appear in the paste
   * target in the same document order which they had before clipboard operation, no matter in which
   * order they were added to clipboard.
   */
  public void testTaskOrder() {
    TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder().build();
    taskManager.getTaskCopyNamePrefixOption().setValue("{1}");
    Task task1 = taskManager.newTaskBuilder().withName("1").build();
    Task task2 = taskManager.newTaskBuilder().withName("2").build();
    Task task3 = taskManager.newTaskBuilder().withName("3").build();
    Task task4 = taskManager.newTaskBuilder().withName("4").build();
    Task task5 = taskManager.newTaskBuilder().withName("5").build();
    Task task6 = taskManager.newTaskBuilder().withName("6").build();
    taskManager.getTaskHierarchy().move(task2, task1);
    taskManager.getTaskHierarchy().move(task4, task3);
    taskManager.getTaskHierarchy().move(task5, task3);

    ClipboardContents contents = new ClipboardContents(taskManager);
    contents.addTasks(ImmutableList.of(task3, task1));
    contents.cut();
    ClipboardTaskProcessor clipboardProcessor = new ClipboardTaskProcessor(taskManager);
    clipboardProcessor.pasteAsChild(task6, contents);
    List<Task> search = taskManager.getTaskHierarchy().breadthFirstSearch(task6, false);

    assertEquals(ImmutableList.of("1", "3", "2", "4", "5"), Lists.transform(search, new Function<Task, String>() {
      @Override
      public String apply(Task t) {
        return t.getName();
      }
    }));
  }

  public void testNestedTasksCopy() {
    TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder().build();
    Task task1 = taskManager.newTaskBuilder().build();
    Task task2 = taskManager.newTaskBuilder().build();
    taskManager.getTaskHierarchy().move(task2, task1);
    Task target = taskManager.newTaskBuilder().build();

    {
      ClipboardContents contents = new ClipboardContents(taskManager);
      contents.addTasks(ImmutableList.of(task1));
      contents.copy();
      ClipboardTaskProcessor clipboardProcessor = new ClipboardTaskProcessor(taskManager);
      List<Task> pasted = clipboardProcessor.pasteAsChild(target, contents);
      assertEquals(1, pasted.size());
      assertTrue(taskManager.getTaskHierarchy().hasNestedTasks(pasted.get(0)));
      assertEquals(1, taskManager.getTaskHierarchy().getNestedTasks(pasted.get(0)).length);
    }
    {
      ClipboardContents contents = new ClipboardContents(taskManager);
      contents.addTasks(ImmutableList.of(task1));
      contents.cut();
      ClipboardTaskProcessor clipboardProcessor = new ClipboardTaskProcessor(taskManager);
      List<Task> pasted = clipboardProcessor.pasteAsChild(target, contents);
      assertEquals(1, pasted.size());
      assertTrue(taskManager.getTaskHierarchy().hasNestedTasks(pasted.get(0)));
      assertEquals(1, taskManager.getTaskHierarchy().getNestedTasks(pasted.get(0)).length);
    }
  }

  public void testTaskNamePattern() {
    TaskManager taskManager = TestSetupHelper.newTaskManagerBuilder().build();
    taskManager.getTaskCopyNamePrefixOption().setValue("{1}_new");
    GanttTask task = taskManager.createTask();

    ClipboardContents contents = new ClipboardContents(taskManager);
    contents.addTasks(ImmutableList.<Task>of(task));
    contents.copy();
    ClipboardTaskProcessor clipboardProcessor = new ClipboardTaskProcessor(taskManager);
    Task pastedTask = clipboardProcessor.pasteAsSibling(taskManager.getRootTask(), contents).get(0);
    assertEquals(task.getName(), pastedTask.getName());

    clipboardProcessor.setTaskCopyNameOption(taskManager.getTaskCopyNamePrefixOption());
    pastedTask = clipboardProcessor.pasteAsSibling(taskManager.getRootTask(), contents).get(0);
    assertEquals(task.getName() + "_new", pastedTask.getName());
  }
}
