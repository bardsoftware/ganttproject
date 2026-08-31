/*
Copyright 2026 Dmitry Barashev, BarD Software s.r.o

This file is part of GanttProject, an open-source project management tool.

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
package net.sourceforge.ganttproject.gui.taskproperties

import biz.ganttproject.customproperty.CustomColumnsManager
import javafx.beans.property.BooleanProperty
import javafx.scene.control.TableView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.roles.RoleManagerImpl
import net.sourceforge.ganttproject.task.ResourceAssignment
import net.sourceforge.ganttproject.task.Task
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests behavior of the UI components in the task resource panel.
 */
class TaskResourcesPanelTest {

  @Test
  fun `ticking coordinator reaches the assignment`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val fixture = Fixture()
      assertFalse(fixture.assignment.isCoordinator, "precondition: nobody is the coordinator yet")

      fixture.coordinatorCellValue().value = true

      assertTrue(fixture.assignment.isCoordinator, "the tick did not reach the assignment")
    }
  }

  @Test
  fun `removing the coordinator tick reaches the assignment`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val fixture = Fixture()
      fixture.assignment.isCoordinator = true

      fixture.coordinatorCellValue().value = false

      assertFalse(fixture.assignment.isCoordinator, "the removed tick did not reach the assignment")
    }
  }

  /**
   * The last row of the table stands for "add a new assignment" and has no assignment behind it.
   * Ticking its check box must neither throw nor touch another row.
   */
  @Test
  fun `ticking coordinator in the empty last row does nothing`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val fixture = Fixture()

      fixture.coordinatorCellValue(row = 1).value = true

      assertFalse(fixture.assignment.isCoordinator, "the empty row must not touch another row")
    }
  }
}

/**
 * A task with a single assignment, and the panel which shows it.
 */
private class Fixture {
  private val roleManager = RoleManagerImpl()
  private val resourceManager =
    HumanResourceManager(roleManager.defaultRole, CustomColumnsManager(), roleManager)
  private val task: Task
  val assignment: ResourceAssignment
  private val panel: TaskResourcesPanel

  init {
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    task = taskManager.newTaskBuilder().withName("Task0").build()
    val resource = resourceManager.newResourceBuilder().withName("Joe").withID(1).build()
    assignment = task.assignmentCollection.addAssignment(resource).also { it.load = 100f }

    panel = TaskResourcesPanel(task, resourceManager, roleManager)
    // Builds the columns and fills the table.
    panel.fxComponent
  }

  /**
   * The property which the check box of the coordinator column is bound to, for the given row.
   */
  fun coordinatorCellValue(row: Int = 0): BooleanProperty {
    val tableViewField = TaskResourcesPanel::class.java.getDeclaredField("tableView")
    tableViewField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val tableView = tableViewField.get(panel) as TableView<Any>
    return tableView.columns[COORDINATOR_COLUMN].getCellObservableValue(row) as BooleanProperty
  }
}

/** Column order of the table: id, resource name, load, coordinator, role. */
private const val COORDINATOR_COLUMN = 3
