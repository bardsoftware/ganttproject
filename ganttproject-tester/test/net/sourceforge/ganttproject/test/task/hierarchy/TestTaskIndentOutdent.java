// Copyright (C) 2018 BarD Software
package net.sourceforge.ganttproject.test.task.hierarchy;

import com.google.common.collect.ImmutableList;
import net.sourceforge.ganttproject.action.task.TaskIndentAction;
import net.sourceforge.ganttproject.action.task.TaskUnindentAction;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

import java.util.Arrays;

/**
 * @author dbarashev@bardsoftware.com
 */
public class TestTaskIndentOutdent extends TaskTestCase {
  private static TaskIndentAction.IndentApplyFxn INDENT_APPLY_FXN = new TaskIndentAction.IndentApplyFxn() {
    @Override
    public void apply(Task task, Task newParent) {
      task.move(newParent);
    }
  };

  public void testSimpleIndent() {
    Task task1 = getTaskManager().createTask();
    Task task2 = getTaskManager().createTask();
    Task task3 = getTaskManager().createTask();

    TaskIndentAction.indent(ImmutableList.of(task2, task3), getTaskManager().getTaskHierarchy(), INDENT_APPLY_FXN);
    Task[] children = getTaskManager().getTaskHierarchy().getNestedTasks(task1);
    assertEquals(2, children.length);
    assertEquals(task2, children[0]);
    assertEquals(task3, children[1]);
  }

  public void testIndentWholeSubtrees() {
    Task task1 = getTaskManager().createTask();
    Task task2 = getTaskManager().createTask();
    Task task3 = getTaskManager().createTask();
    Task task4 = getTaskManager().createTask();
    Task task5 = getTaskManager().createTask();
    task4.move(task2);
    task5.move(task3);

    TaskIndentAction.indent(ImmutableList.of(task2, task3, task4, task5), getTaskManager().getTaskHierarchy(), INDENT_APPLY_FXN);
    Task[] children = getTaskManager().getTaskHierarchy().getNestedTasks(task1);
    assertEquals(2, children.length);
    assertEquals(task2, children[0]);
    assertEquals(task3, children[1]);
    assertEquals(task4, getTaskManager().getTaskHierarchy().getNestedTasks(task2)[0]);
    assertEquals(task5, getTaskManager().getTaskHierarchy().getNestedTasks(task3)[0]);
  }

  public void testIndentSubtreeRoots() {
    Task task1 = getTaskManager().createTask();
    Task task2 = getTaskManager().createTask();
    Task task3 = getTaskManager().createTask();
    Task task4 = getTaskManager().createTask();
    Task task5 = getTaskManager().createTask();
    task4.move(task2);
    task5.move(task3);

    TaskIndentAction.indent(ImmutableList.of(task2, task3), getTaskManager().getTaskHierarchy(), INDENT_APPLY_FXN);
    Task[] children = getTaskManager().getTaskHierarchy().getNestedTasks(task1);
    assertEquals(2, children.length);
    assertEquals(task2, children[0]);
    assertEquals(task3, children[1]);
    assertEquals(task4, getTaskManager().getTaskHierarchy().getNestedTasks(task2)[0]);
    assertEquals(task5, getTaskManager().getTaskHierarchy().getNestedTasks(task3)[0]);
  }

  public void testIndentIntoDifferentTargets() {
    Task task1 = getTaskManager().createTask();
    Task task2 = getTaskManager().createTask();
    Task task3 = getTaskManager().createTask();
    Task task4 = getTaskManager().createTask();
    Task task5 = getTaskManager().createTask();

    TaskIndentAction.indent(ImmutableList.of(task2, task4, task5), getTaskManager().getTaskHierarchy(), INDENT_APPLY_FXN);
    assertEquals(task2, getTaskManager().getTaskHierarchy().getNestedTasks(task1)[0]);
    assertEquals(task4, getTaskManager().getTaskHierarchy().getNestedTasks(task3)[0]);
    assertEquals(task5, getTaskManager().getTaskHierarchy().getNestedTasks(task3)[1]);
  }


  private static TaskUnindentAction.UnindentApplyFxn OUTDENT_APPLY_FXN = new TaskUnindentAction.UnindentApplyFxn() {
    @Override
    public void apply(Task task, Task newParent, int position) {
      task.move(newParent, position);
    }
  };

  public void testSimpleOutdent() {
    Task task1 = getTaskManager().createTask();
    Task task2 = getTaskManager().createTask();
    Task task3 = getTaskManager().createTask();
    task2.move(task1);
    task3.move(task1);

    TaskUnindentAction.unindent(ImmutableList.of(task2, task3), getTaskManager().getTaskHierarchy(), OUTDENT_APPLY_FXN);
    Task[] children = getTaskManager().getTaskHierarchy().getNestedTasks(getTaskManager().getRootTask());
    assertEquals(3, children.length);
    assertEquals(Arrays.asList(children).toString(), task1, children[0]);
    assertEquals(Arrays.asList(children).toString(), task2, children[1]);
    assertEquals(Arrays.asList(children).toString(), task3, children[2]);

  }

  public void testOutdentWholeSubtrees() {
    Task task1 = getTaskManager().createTask();
    Task task2 = getTaskManager().createTask();
    Task task3 = getTaskManager().createTask();
    Task task4 = getTaskManager().createTask();
    Task task5 = getTaskManager().createTask();

    task3.move(task2);
    task2.move(task1);

    task5.move(task4);
    task4.move(task1);

    TaskUnindentAction.unindent(ImmutableList.of(task2, task3, task4, task5), getTaskManager().getTaskHierarchy(), OUTDENT_APPLY_FXN);
    Task[] children = getTaskManager().getTaskHierarchy().getNestedTasks(getTaskManager().getRootTask());
    assertEquals(3, children.length);
    assertEquals(Arrays.asList(children).toString(), task1, children[0]);
    assertEquals(Arrays.asList(children).toString(), task2, children[1]);
    assertEquals(Arrays.asList(children).toString(), task4, children[2]);
  }

  public void testOutdentSubtreeRoots() {
    Task task1 = getTaskManager().createTask();
    Task task2 = getTaskManager().createTask();
    Task task3 = getTaskManager().createTask();
    Task task4 = getTaskManager().createTask();
    Task task5 = getTaskManager().createTask();

    task3.move(task2);
    task2.move(task1);

    task5.move(task4);
    task4.move(task1);

    TaskUnindentAction.unindent(ImmutableList.of(task2, task4), getTaskManager().getTaskHierarchy(), OUTDENT_APPLY_FXN);
    Task[] children = getTaskManager().getTaskHierarchy().getNestedTasks(getTaskManager().getRootTask());
    assertEquals(3, children.length);
    assertEquals(Arrays.asList(children).toString(), task1, children[0]);
    assertEquals(Arrays.asList(children).toString(), task2, children[1]);
    assertEquals(Arrays.asList(children).toString(), task4, children[2]);
  }

  public void testOudentFromDifferentSources() {
    Task task1 = getTaskManager().createTask();
    Task task2 = getTaskManager().createTask();
    Task task3 = getTaskManager().createTask();
    Task task4 = getTaskManager().createTask();
    Task task5 = getTaskManager().createTask();

    task3.move(task2);
    task2.move(task1);

    task5.move(task4);

    TaskUnindentAction.unindent(ImmutableList.of(task2, task3, task5), getTaskManager().getTaskHierarchy(), OUTDENT_APPLY_FXN);

    Task[] children = getTaskManager().getTaskHierarchy().getNestedTasks(getTaskManager().getRootTask());
    assertEquals(4, children.length);
    assertEquals(ImmutableList.of(task1, task2, task4, task5), Arrays.asList(children));
  }

}
