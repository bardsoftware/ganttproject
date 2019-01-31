/*
Copyright 2018 Dmitry Barashev, BarD Software s.r.o

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

package net.sourceforge.ganttproject.gui.taskproperties;

import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.dependency.TaskDependency;
import net.sourceforge.ganttproject.test.task.TaskTestCase;

/**
 * @author dbarashev@bardsoftware.com
 */
public class DependencyTableModelTest extends TaskTestCase {
  public void testEmptyModel() {
    Task task = createTask();

    DependencyTableModel tableModel = new DependencyTableModel(task);
    // There is always 1 empty row in a table
    assertEquals(1, tableModel.getRowCount());
  }

  public void testBasicDependencyAdd() {
    Task task2 = createTask();
    Task task3 = createTask();

    DependencyTableModel tableModel = new DependencyTableModel(task3);
    tableModel.setValueAt(new DependencyTableModel.TaskComboItem(task2),0, DependencyTableModel.MyColumn.TASK_NAME.ordinal());

    assertEquals(2, tableModel.getRowCount());
    // Not committed yet
    assertEquals(0, task3.getDependencies().toArray().length);
    //
    tableModel.commit();
    TaskDependency[] deps = task3.getDependencies().toArray();
    assertEquals(1, deps.length);
    assertEquals(task2, deps[0].getDependee());
  }

  public void testSubsequentDependeeChange_Issue1618() {
    Task task1 = createTask();
    Task task2 = createTask();
    Task task3 = createTask();

    DependencyTableModel tableModel = new DependencyTableModel(task3);
    tableModel.setValueAt(new DependencyTableModel.TaskComboItem(task2),0, DependencyTableModel.MyColumn.TASK_NAME.ordinal());

    assertEquals(2, tableModel.getRowCount());
    // Not committed yet
    assertEquals(0, task3.getDependencies().toArray().length);

    // Now change the dependee from task2 to task1
    tableModel.setValueAt(new DependencyTableModel.TaskComboItem(task1),0, DependencyTableModel.MyColumn.TASK_NAME.ordinal());
    // We still expect 2 rows
    assertEquals(2, tableModel.getRowCount());
    // Not committed yet
    assertEquals(0, task3.getDependencies().toArray().length);

    tableModel.commit();
    // After committing we expect that task1 wins over task2
    TaskDependency[] deps = task3.getDependencies().toArray();
    assertEquals(1, deps.length);
    assertEquals(task1, deps[0].getDependee());
  }
}
