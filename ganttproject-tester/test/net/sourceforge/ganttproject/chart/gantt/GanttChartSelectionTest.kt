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

import net.sourceforge.ganttproject.GPTransferable
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.TestSetupHelper
import net.sourceforge.ganttproject.export.ConsoleUIFacade
import net.sourceforge.ganttproject.importer.BufferProject
import net.sourceforge.ganttproject.task.TaskSelectionManager
import org.junit.jupiter.api.Assertions.assertEquals
import biz.ganttproject.customproperty.CustomColumnsManager
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.roles.Role
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection

class GanttChartSelectionTest {
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

    assumeTrue(!GraphicsEnvironment.isHeadless() && !isRunningOnGitHubActions(), "needs a system clipboard")

    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    assertTrue(clipboard.isDataFlavorAvailable(GPTransferable.EXTERNAL_DOCUMENT_FLAVOR))
    val clipboardProject = getProjectFromClipboard(BufferProject(GanttProjectImpl(), ConsoleUIFacade(null))) ?: error("Clipboard project is null")
    assertEquals(1, clipboardProject.taskManager.tasks.size)
    assertEquals(setOf(task1, task2, task3), taskManager.taskHierarchy.tasksInDocumentOrder.toSet())
  }

  /**
   * When someone else (e.g. another GanttProject window) takes over the system clipboard, the internal clipboard
   * contents must be discarded, so that Paste action imports the external data instead.
   * See https://github.com/bardsoftware/ganttproject/issues/2861
   */
  @Test fun testLostOwnershipDiscardsInternalContents_Issue2861() {
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val selectionManager = TaskSelectionManager { taskManager }
    val task1 = taskManager.newTaskBuilder().withName("task1").build()
    val task2 = taskManager.newTaskBuilder().withName("task2").build()
    val ganttChartSelection = GanttChartSelection(taskManager, selectionManager)
    assertFalse(ganttChartSelection.hasOwnClipboardContents())

    selectionManager.setSelectedTasks(listOf(task1), this)
    ganttChartSelection.startMoveClipboardTransaction()
    assertTrue(ganttChartSelection.hasOwnClipboardContents())

    ganttChartSelection.lostOwnership(Clipboard("test"), StringSelection("foo"))
    assertFalse(ganttChartSelection.hasOwnClipboardContents())

    // Paste after losing the ownership must not move anything.
    selectionManager.setSelectedTasks(listOf(task2), this)
    ganttChartSelection.commitClipboardTransaction()
    assertEquals(listOf(task1, task2), taskManager.taskHierarchy.tasksInDocumentOrder)
    assertEquals(taskManager.rootTask, task1.supertask)
  }

  /**
   * Resources copied in the resource chart must not be pasted into the Gantt chart.
   */
  @Test fun testAcceptsOnlyTasks() {
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val selectionManager = TaskSelectionManager { taskManager }
    val task1 = taskManager.newTaskBuilder().withName("task1").build()
    val ganttChartSelection = GanttChartSelection(taskManager, selectionManager)

    val resourceContents = ClipboardContents(taskManager).also {
      it.addResource(HumanResource("Joe", 1, HumanResourceManager(null as Role?, CustomColumnsManager())))
      it.copy()
    }
    assertFalse(ganttChartSelection.acceptsClipboardContents(resourceContents))

    val taskContents = ClipboardContents(taskManager).also {
      it.addTasks(listOf(task1))
      it.copy()
    }
    assertTrue(ganttChartSelection.acceptsClipboardContents(taskContents))
  }

  /**
   * The system clipboard contents put by this app instance are recognized as our own.
   */
  @Test fun testOwnClipboardContents() {
    assumeTrue(!GraphicsEnvironment.isHeadless() && !isRunningOnGitHubActions(), "needs a system clipboard")
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val selectionManager = TaskSelectionManager { taskManager }
    val task1 = taskManager.newTaskBuilder().withName("task1").build()
    val ganttChartSelection = GanttChartSelection(taskManager, selectionManager)
    selectionManager.setSelectedTasks(listOf(task1), this)
    ganttChartSelection.startCopyClipboardTransaction()

    val ownContents = getOwnClipboardContents() ?: error("Own clipboard contents are null")
    assertEquals(listOf("task1"), ownContents.tasks.map { it.name })

    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection("foo"), null)
    assertNull(getOwnClipboardContents())
  }

  /**
   * Cut tasks are moved to the end of the top level if nothing is selected when pasting.
   */
  @Test fun testCutAndPasteWithNoSelection() {
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val selectionManager = TaskSelectionManager { taskManager }
    val summary = taskManager.newTaskBuilder().withName("summary").build()
    val child = taskManager.newTaskBuilder().withName("child").withParent(summary).build()
    taskManager.newTaskBuilder().withName("task2").build()
    val ganttChartSelection = GanttChartSelection(taskManager, selectionManager)
    val taskCount = taskManager.taskCount

    selectionManager.setSelectedTasks(listOf(child), this)
    ganttChartSelection.startMoveClipboardTransaction()
    selectionManager.clear()
    assertTrue(ganttChartSelection.hasOwnClipboardContents())
    ganttChartSelection.commitClipboardTransaction()

    val hierarchy = taskManager.taskHierarchy
    assertEquals(listOf("summary", "task2", "child"), hierarchy.getNestedTasks(taskManager.rootTask).map { it.name })
    assertEquals(0, hierarchy.getNestedTasks(summary).size)
    assertEquals(taskCount, taskManager.taskCount)
  }

  /**
   * Copied tasks are copied to the end of the top level if nothing is selected when pasting.
   */
  @Test fun testCopyAndPasteWithNoSelection() {
    val taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    val selectionManager = TaskSelectionManager { taskManager }
    val summary = taskManager.newTaskBuilder().withName("summary").build()
    val child = taskManager.newTaskBuilder().withName("child").withParent(summary).build()
    taskManager.newTaskBuilder().withName("task2").build()
    val ganttChartSelection = GanttChartSelection(taskManager, selectionManager)

    // startCopyClipboardTransaction exports the tasks into the system clipboard, which is not available in headless mode.
    assumeTrue(!GraphicsEnvironment.isHeadless() && !isRunningOnGitHubActions(), "needs a system clipboard")
    taskManager.taskCopyNamePrefixOption.value = "{1}_copy"
    val taskCount = taskManager.taskCount
    selectionManager.setSelectedTasks(listOf(child), this)
    ganttChartSelection.startCopyClipboardTransaction()
    selectionManager.clear()
    ganttChartSelection.commitClipboardTransaction()

    val hierarchy = taskManager.taskHierarchy
    val topLevel = hierarchy.getNestedTasks(taskManager.rootTask).toList()
    assertEquals(listOf("summary", "task2", "child_copy"), topLevel.map { it.name })
    assertEquals(listOf("child"), hierarchy.getNestedTasks(summary).map { it.name })
    assertEquals(taskCount + 1, taskManager.taskCount)
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

    assumeTrue(!GraphicsEnvironment.isHeadless() && !isRunningOnGitHubActions(), "needs a system clipboard")
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

private fun isRunningOnGitHubActions(): Boolean = System.getenv("CI")?.lowercase() == "true"