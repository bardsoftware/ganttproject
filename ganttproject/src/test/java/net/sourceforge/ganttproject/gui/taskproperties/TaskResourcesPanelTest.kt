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
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.roles.RoleManagerImpl
import net.sourceforge.ganttproject.task.ResourceAssignment
import net.sourceforge.ganttproject.task.Task
import org.junit.jupiter.api.Assertions.assertEquals
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

  @Test
  fun `ticking an axis reaches the assignment only on commit`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      AssignmentAxis.entries.forEach { axis ->
        val fixture = Fixture()

        fixture.axisCellValue(axis).value = true

        assertFalse(axis.get(fixture.assignment), "$axis: the tick must wait for commit")
        fixture.commit()
        assertTrue(axis.get(fixture.assignment), "$axis: commit did not apply the tick")
      }
    }
  }

  @Test
  fun `removing an axis tick reaches the assignment on commit`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      AssignmentAxis.entries.forEach { axis ->
        val fixture = Fixture { axis.set(it, true) }

        fixture.axisCellValue(axis).value = false

        assertTrue(axis.get(fixture.assignment), "$axis: the removed tick must wait for commit")
        fixture.commit()
        assertFalse(axis.get(fixture.assignment), "$axis: commit did not apply the removed tick")
      }
    }
  }

  /**
   * Cancelling the dialog throws the panel away without commit. The assignment and a new panel must not see the tick.
   */
  @Test
  fun `cancelling the dialog leaves the axes alone`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val fixture = Fixture()
      AssignmentAxis.entries.forEach { fixture.axisCellValue(it).value = true }

      val reopened = fixture.reopen()

      AssignmentAxis.entries.forEach { axis ->
        assertFalse(axis.get(fixture.assignment), "$axis: cancelling must not change the assignment")
        assertFalse(reopened.axisCellValue(axis).value, "$axis: a new dialog must not show the cancelled tick")
      }
    }
  }

  @Test
  fun `an axis tick survives refreshing the table`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val fixture = Fixture()
      fixture.axisCellValue(AssignmentAxis.BLOCKING).value = true

      fixture.refreshTable()

      assertTrue(fixture.axisCellValue(AssignmentAxis.BLOCKING).value, "the table forgot the tick")
      assertFalse(fixture.axisCellValue(AssignmentAxis.NO_EFFORT).value, "the tick went to the other axis")
    }
  }

  @Test
  fun `ticking an axis in the empty last row does nothing`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val fixture = Fixture()

      AssignmentAxis.entries.forEach { fixture.axisCellValue(it, row = 1).value = true }
      fixture.commit()

      AssignmentAxis.entries.forEach { axis ->
        assertFalse(axis.get(fixture.assignment), "$axis: the empty row must not touch another row")
      }
      assertEquals(1, fixture.task.assignments.size, "the empty row must not add an assignment")
    }
  }

  @Test
  fun `an assignment added in the dialog keeps its axis ticks`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val fixture = Fixture()
      fixture.addInDialog(fixture.ann)

      fixture.axisCellValue(AssignmentAxis.BLOCKING, row = 1).value = true
      fixture.axisCellValue(AssignmentAxis.NO_EFFORT, row = 1).value = true
      fixture.commit()

      val added = fixture.task.assignments.single { it.resource == fixture.ann }
      assertTrue(added.isBlocking, "the added assignment lost the blocking tick")
      assertTrue(added.isNoEffort, "the added assignment lost the no effort tick")
      assertFalse(fixture.assignment.isBlocking, "the tick went to the other row")
    }
  }

  @Test
  fun `replacing the resource of a row keeps its axis ticks`() = runBlocking {
    withContext(Dispatchers.JavaFx) {
      val fixture = Fixture { it.isNoEffort = true }
      fixture.axisCellValue(AssignmentAxis.BLOCKING).value = true

      fixture.setValueAt(fixture.ann, row = 0, col = 1)
      fixture.commit()

      val replaced = fixture.task.assignments.single()
      assertEquals(fixture.ann, replaced.resource)
      assertTrue(replaced.isBlocking, "the pending tick was lost with the old resource")
      assertTrue(replaced.isNoEffort, "the stored flag was lost with the old resource")
    }
  }
}

/**
 * A task with a single assignment, and the panel which shows it.
 */
private class Fixture(setUp: (ResourceAssignment) -> Unit = {}) {
  private val roleManager = RoleManagerImpl()
  private val resourceManager =
    HumanResourceManager(roleManager.defaultRole, CustomColumnsManager(), roleManager)
  val task: Task
  val assignment: ResourceAssignment
  val ann: HumanResource
  private var panel: TaskResourcesPanel

  init {
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    task = taskManager.newTaskBuilder().withName("Task0").build()
    val resource = resourceManager.newResourceBuilder().withName("Joe").withID(1).build()
    ann = resourceManager.newResourceBuilder().withName("Ann").withID(2).build()
    assignment = task.assignmentCollection.addAssignment(resource).also {
      it.load = 100f
      setUp(it)
    }
    panel = newPanel()
  }

  private fun newPanel() = TaskResourcesPanel(task, resourceManager, roleManager).also {
    // Builds the columns and fills the table.
    it.fxComponent
  }

  /** Ok in the dialog, the way TaskProperties.save does it. */
  fun commit() {
    task.createMutator().also {
      panel.commit(it)
      it.commit()
    }
  }

  /** Throws the panel away without commit, as Cancel does, and opens a new one. */
  fun reopen(): Fixture = also { panel = newPanel() }

  fun setValueAt(value: Any?, row: Int, col: Int) {
    model().javaClass.getMethod("setValueAt", Any::class.java, Int::class.java, Int::class.java)
      .invoke(model(), value, row, col)
    refreshTable()
  }

  /** Picks a resource in the empty last row. */
  fun addInDialog(resource: HumanResource) = setValueAt(resource, task.assignments.size, 1)

  fun refreshTable() {
    model().javaClass.getMethod("refreshTable").invoke(model())
  }

  private fun model(): Any =
    TaskResourcesPanel::class.java.getDeclaredField("model").let {
      it.isAccessible = true
      it.get(panel)
    }

  /**
   * The property which the check box of the given axis column is bound to, for the given row.
   */
  fun axisCellValue(axis: AssignmentAxis, row: Int = 0): BooleanProperty =
    column(FIRST_AXIS_COLUMN + axis.ordinal).getCellObservableValue(row) as BooleanProperty

  private fun column(index: Int) =
    TaskResourcesPanel::class.java.getDeclaredField("tableView").let {
      it.isAccessible = true
      @Suppress("UNCHECKED_CAST")
      (it.get(panel) as TableView<Any>).columns[index]
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

/** Column order of the table: id, resource name, load, coordinator, role, then the axes. */
private const val COORDINATOR_COLUMN = 3
private const val FIRST_AXIS_COLUMN = 5
