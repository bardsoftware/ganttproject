/*
Copyright 2022 BarD Software s.r.o

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
package net.sourceforge.ganttproject.chart.gantt

import junit.framework.TestCase
import net.sourceforge.ganttproject.GPTransferable
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.export.ConsoleUIFacade
import net.sourceforge.ganttproject.importer.BufferProject
import net.sourceforge.ganttproject.task.TaskSelectionManager
import org.junit.jupiter.api.Test
import java.awt.GraphicsEnvironment
import java.awt.Toolkit

class GanttChartSelectionTest: TestCase() {
  /**
   * This tests the fix made in GanttChartSelection::exportTasksIntoSystemClipboard for cut'n'paste operation.
   *
   * On Windows, if the system clipboard contains the EXTERNAL_DOCUMENT_FLAVOR, it is immediately requested and
   * GPTransferable starts building an InputStream in its GPTransferable::createDocumentFlavor method.
   * It involves the task tree modification, and effectively the task which is cut disappears from the hierarchy before
   * Paste action runs (and before undoable edit creates an undo file). As a result, undoing cut'n'paste produces the
   * task tree where the cut task is missing.
   *
   * See discussions on the support portal:
   * https://help.ganttproject.biz/t/nullpointerexception-in-cut-and-paste-feature/4413/22
   * https://help.ganttproject.biz/t/where-is-the-subtask/4527
   *
   * and the issue in the bug tracker: https://github.com/bardsoftware/ganttproject/issues/2050
   */
  @Test fun testStartMoveTransactionAndExternalDocumentFlavor_Issue2050() {
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val selectionManager = TaskSelectionManager { taskManager }
    val task1 = taskManager.newTaskBuilder().withName("task1").build()
    val task2 = taskManager.newTaskBuilder().withName("task2").build()
    val task3 = taskManager.newTaskBuilder().withName("task3").build()
    val ganttChartSelection = GanttChartSelection(taskManager, selectionManager)
    selectionManager.setSelectedTasks(listOf(task3), this)
    ganttChartSelection.startMoveClipboardTransaction()

    if (!GraphicsEnvironment.isHeadless()) {
      val clipboard = Toolkit.getDefaultToolkit().systemClipboard
      assertTrue(clipboard.isDataFlavorAvailable(GPTransferable.EXTERNAL_DOCUMENT_FLAVOR))
      val clipboardProject = getProjectFromClipboard(BufferProject(GanttProjectImpl(), ConsoleUIFacade(null))) ?: error("Clipboard project is null")
      assertEquals(1, clipboardProject.taskManager.tasks.size)
    }
    assertEquals(setOf(task1, task2, task3), taskManager.taskHierarchy.tasksInDocumentOrder.toSet())
  }

  @Test fun testDependenciesInTheClipboardProject() {
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val selectionManager = TaskSelectionManager { taskManager }
    val task1 = taskManager.newTaskBuilder().withName("task1").build()
    val task2 = taskManager.newTaskBuilder().withName("task2").build()
    val task3 = taskManager.newTaskBuilder().withName("task3").build()

    taskManager.dependencyCollection.createDependency(task2, task1)
    taskManager.dependencyCollection.createDependency(task3, task1)

    val ganttChartSelection = GanttChartSelection(taskManager, selectionManager)
    selectionManager.setSelectedTasks(listOf(task1, task2), this)
    ganttChartSelection.startMoveClipboardTransaction()

    if (!GraphicsEnvironment.isHeadless()) {
      val clipboardProject = getProjectFromClipboard(BufferProject(GanttProjectImpl(), ConsoleUIFacade(null)))
        ?: error("Clipboard project is null")
      assertEquals(setOf("task1", "task2"), clipboardProject.taskManager.tasks.map { it.name }.toSet())
      clipboardProject.taskManager.dependencyCollection.dependencies?.let {
        assertEquals(1, it.size)
        assertEquals("task1", it[0].dependee.name)
        assertEquals("task2", it[0].dependant.name)
      } ?: error("Dependencies are null")
    }
  }
}