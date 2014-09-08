/*
Copyright 2014 BarD Software s.r.o
Copyright 2010-2013 GanttProject Team

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
package net.sourceforge.ganttproject.test.task.hierarchy;

import net.sourceforge.ganttproject.test.task.TaskTestCase;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskContainmentHierarchyFacade;
import net.sourceforge.ganttproject.util.collect.Pair;

import java.util.Arrays;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Tests for {@link TaskContainmentHierarchyFacade}
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class TestTaskHierarchy extends TaskTestCase {
    public void testCreateSimpleHierarchy() {
        Task task1 = getTaskManager().createTask();
        Task task2 = getTaskManager().createTask();
        task2.move(task1);
        assertEquals("Unexpected supertask of task=" + task2, task1, task2
                .getSupertask());
        assertEquals("Unexpected nested tasks of task=" + task1, Arrays
                .asList(new Task[] { task2 }), Arrays.asList(task1
                .getNestedTasks()));
    }

    public void testBreadthFirstSearch() {
      Task task1 = getTaskManager().createTask();
      Task task2 = getTaskManager().createTask();
      Task task3 = getTaskManager().createTask();
      Task task4 = getTaskManager().createTask();
      Task task5 = getTaskManager().createTask();
      final Task task6 = getTaskManager().createTask();
      Task task7 = getTaskManager().createTask();

      task6.move(task7);
      task5.move(task7);
      task4.move(task6);
      task3.move(task6);
      task2.move(task5);
      task1.move(task5);

      assertEquals(ImmutableList.of(task7, task6, task5, task4, task3, task2, task1),
          getTaskManager().getTaskHierarchy().breadthFirstSearch(null, false));
      assertEquals(ImmutableList.of(task6, task4, task3),
          getTaskManager().getTaskHierarchy().breadthFirstSearch(task6, true));
      assertEquals(ImmutableList.of(task2, task1),
          getTaskManager().getTaskHierarchy().breadthFirstSearch(task5, false));

      final List<Task> filteredBfs = Lists.newArrayList();
      getTaskManager().getTaskHierarchy().breadthFirstSearch(getTaskManager().getRootTask(), new Predicate<Pair<Task,Task>>() {
        @Override
        public boolean apply(Pair<Task, Task> parent_child) {
          filteredBfs.add(parent_child.second());
          return parent_child.second() != task6;
        }
      });
      assertEquals(ImmutableList.of(getTaskManager().getRootTask(), task7, task6, task5, task2, task1), filteredBfs);
    }
}
