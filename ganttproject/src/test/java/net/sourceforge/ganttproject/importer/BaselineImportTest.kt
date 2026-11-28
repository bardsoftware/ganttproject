/*
Copyright 2026 BarD Software s.r.o

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
package net.sourceforge.ganttproject.importer

import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.core.time.CalendarFactory
import net.sourceforge.ganttproject.GanttPreviousState
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.export.ConsoleUIFacade
import net.sourceforge.ganttproject.gui.UIFacade
import net.sourceforge.ganttproject.io.GanttXMLSaver
import net.sourceforge.ganttproject.resource.HumanResourceMerger
import net.sourceforge.ganttproject.resource.MergeResourcesEnum
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.easymock.EasyMock
import org.easymock.EasyMock.createNiceMock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.osgi.service.prefs.Preferences
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Tests importing the baselines of a project file into the current project.
 */
class BaselineImportTest {
  @BeforeEach
  fun setUp() {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : LocaleApi {
          override fun getLocale(): Locale = Locale.US
          override fun getShortDateFormat(): DateFormat =
            DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        })
      }
    }
  }

  private fun GanttProjectImpl.addTask(name: String): Task =
    taskManager.newTaskBuilder().withName(name).withParent(taskManager.rootTask).build()

  private fun GanttProjectImpl.task(name: String): Task = taskManager.tasks.single { it.name == name }

  private fun GanttProjectImpl.takeBaseline(name: String) =
    GanttPreviousState(name, GanttPreviousState.createTasks(taskManager)).also { baselines.add(it) }

  private fun importProject(target: GanttProjectImpl, buffer: BufferProject): List<GanttPreviousState> {
    val mapping = target.importProject(buffer, HumanResourceMerger.MergeResourcesOption(), null, false)
    return importBaselines(buffer.baselines, target.baselines, mapping)
  }

  @Test
  fun `imported baseline refers to the imported tasks`() {
    val target = GanttProjectImpl()
    val targetTask = target.addTask("Target task")
    val buffer = BufferProject(target, createNiceMock(UIFacade::class.java))
    buffer.addTask("Imported task")
    buffer.takeBaseline("Baseline")

    val imported = importProject(target, buffer)

    assertEquals(listOf("Baseline"), target.baselines.map { it.name })
    assertEquals(imported, target.baselines)
    val importedTask = target.task("Imported task")
    assertNotEquals(targetTask.taskID, importedTask.taskID)
    assertEquals(listOf(importedTask.taskID), target.baselines.single().tasks.map { it.id },
      "the baseline has to point to the imported task, not to the task which had this id in the imported file")
  }

  @Test
  fun `entries of tasks which are not imported are dropped`() {
    val target = GanttProjectImpl()
    target.addTask("Target task")
    val buffer = BufferProject(target, createNiceMock(UIFacade::class.java))
    val deleted = buffer.addTask("Deleted task")
    buffer.addTask("Imported task")
    buffer.takeBaseline("Baseline")
    buffer.taskManager.deleteTask(deleted)

    importProject(target, buffer)

    assertEquals(listOf(target.task("Imported task").taskID), target.baselines.single().tasks.map { it.id })
  }

  @Test
  fun `project without baselines leaves the baselines of the target project as they are`() {
    val target = GanttProjectImpl()
    target.addTask("Target task")
    val own = target.takeBaseline("Own")
    val buffer = BufferProject(target, createNiceMock(UIFacade::class.java))
    buffer.addTask("Imported task")

    val imported = importProject(target, buffer)

    assertEquals(emptyList<GanttPreviousState>(), imported)
    assertEquals(listOf(own), target.baselines)
  }

  @Test
  fun `baseline with a name which is already taken gets a number appended`() {
    val target = GanttProjectImpl()
    target.addTask("Target task")
    val own = target.takeBaseline("Baseline")
    val ownTasks = own.tasks.toList()

    val first = BufferProject(target, createNiceMock(UIFacade::class.java))
    first.addTask("Imported task")
    first.takeBaseline("Baseline")
    importProject(target, first)

    val second = BufferProject(target, createNiceMock(UIFacade::class.java))
    second.addTask("Imported task")
    second.takeBaseline("Baseline")
    importProject(target, second)

    assertEquals(listOf("Baseline", "Baseline (2)", "Baseline (3)"), target.baselines.map { it.name })
    assertEquals(own, target.baselines[0], "the baseline of the target project has to stay in place")
    assertEquals(ownTasks, own.tasks)
  }

  @Test
  fun `undo of the import removes the imported baseline`() {
    val target = GanttProjectImpl()
    target.addTask("Target task")
    val own = target.takeBaseline("Own")
    val undoManager = BaselineSnapshotUndoManager(target.baselines)
    val importer = ImporterFromGanttFile().also {
      val model = ImporterWizardModel()
      model.file = savedFileWithBaseline("Imported")
      it.setContext(target, TestUIFacade(undoManager), nicePreferences())
      it.setModel(model)
    }

    importer.run()
    assertEquals(listOf("Own", "Imported"), target.baselines.map { it.name },
      "the importer has to import the baseline of the file")

    undoManager.undo()
    assertEquals(listOf(own), target.baselines)
  }

  @Test
  fun `no question is asked if no baseline name is taken`() {
    val target = GanttProjectImpl()
    target.addTask("Target task")
    target.takeBaseline("Own")
    val questions = mutableListOf<List<String>>()

    importer(target, BaselineSnapshotUndoManager(target.baselines), listOf(savedFileWithBaseline("Imported"))) {
      questions.add(it)
      BaselineNameCollision.RENAME
    }.run()

    assertEquals(emptyList<List<String>>(), questions)
    assertEquals(listOf("Own", "Imported"), target.baselines.map { it.name })
  }

  @Test
  fun `one question is asked about all taken baseline names of an import`() {
    val target = GanttProjectImpl()
    target.addTask("Target task")
    target.takeBaseline("A")
    target.takeBaseline("B")
    val questions = mutableListOf<List<String>>()
    val files = listOf(savedFileWithBaseline("A", "B", "C"), savedFileWithBaseline("C", "D"))

    importer(target, BaselineSnapshotUndoManager(target.baselines), files) {
      questions.add(it)
      BaselineNameCollision.RENAME
    }.run()

    assertEquals(listOf(listOf("A", "B", "C")), questions,
      "one question for both files, also about the name which the first file takes for the second one")
    assertEquals(listOf("A", "B", "A (2)", "B (2)", "C", "C (2)", "D"), target.baselines.map { it.name })
  }

  @Test
  fun `the answer applies to all taken baseline names`() {
    fun importWith(answer: BaselineNameCollision): Pair<List<GanttPreviousState>, List<GanttPreviousState>> {
      val target = GanttProjectImpl()
      target.addTask("Target task")
      val own = listOf(target.takeBaseline("A"), target.takeBaseline("B"))
      importer(target, BaselineSnapshotUndoManager(target.baselines), listOf(savedFileWithBaseline("A", "B", "C"))) {
        answer
      }.run()
      return own to target.baselines.toList()
    }

    importWith(BaselineNameCollision.RENAME).let { (own, result) ->
      assertEquals(listOf("A", "B", "A (2)", "B (2)", "C"), result.map { it.name })
      assertEquals(own, result.take(2))
    }
    importWith(BaselineNameCollision.REPLACE).let { (own, result) ->
      assertEquals(listOf("A", "B", "C"), result.map { it.name })
      assertEquals(emptyList<GanttPreviousState>(), result.filter { it in own }, "both own baselines have to be replaced")
    }
    importWith(BaselineNameCollision.KEEP_BOTH).let { (own, result) ->
      assertEquals(listOf("A", "B", "A", "B", "C"), result.map { it.name })
      assertEquals(own, result.take(2))
    }
  }

  @Test
  fun `cancelling the question imports nothing`() {
    val target = GanttProjectImpl()
    target.addTask("Target task")
    val own = target.takeBaseline("Baseline")
    val undoManager = BaselineSnapshotUndoManager(target.baselines)

    importer(target, undoManager, listOf(savedFileWithBaseline("Baseline"))) { null }.run()

    assertEquals(listOf(own), target.baselines)
    assertEquals(listOf("Target task"), target.taskManager.tasks.map { it.name }, "no task may be imported")
    assertEquals(false, undoManager.canUndo(), "there has to be no undoable edit")
  }

  @Test
  fun `import while another thread reads the tasks leaves a complete task list`() {
    // The import runs on a background thread while the task table and the chart read the tasks.
    // A task list cached without the last imported task made the critical path calculation after
    // the import fail with "Cannot read field numDependants because nested is null".
    val file = savedFileWithSubtasks()
    repeat(200) { round ->
      val target = GanttProjectImpl()
      target.addTask("Target task")
      target.takeBaseline("Baseline")
      val undoManager = BaselineSnapshotUndoManager(target.baselines)
      val stop = AtomicBoolean(false)
      val reader = Thread { while (!stop.get()) { target.taskManager.tasks } }.also { it.start() }
      try {
        importer(target, undoManager, listOf(file)) { null }.run()
        importer(target, undoManager, listOf(file)) { BaselineNameCollision.RENAME }.run()
      } finally {
        stop.set(true)
        reader.join()
      }
      val taskManager = target.taskManager
      assertEquals(taskManager.taskHierarchy.getDeepNestedTasks(taskManager.rootTask).toSet(), taskManager.tasks.toSet(),
        "round $round: the task list has to contain every task of the hierarchy")
      assertEquals(listOf("Baseline", "Baseline (2)"), target.baselines.map { it.name })
      taskManager.processCriticalPath(taskManager.rootTask)
    }
  }

  private fun importer(
    target: GanttProjectImpl, undoManager: GPUndoManager, files: List<File>, question: (List<String>) -> BaselineNameCollision?
  ) = ImporterFromGanttFile().also {
    val model = ImporterWizardModel()
    model.files = files
    it.setContext(target, TestUIFacade(undoManager), nicePreferences())
    it.setModel(model)
    it.setBaselineNameCollisionQuestion(question)
  }

  private fun savedFileWithSubtasks(): File {
    val project = GanttProjectImpl()
    val summary = project.addTask("Summary")
    (1..5).forEach { project.taskManager.newTaskBuilder().withName("Subtask $it").withParent(summary).build() }
    project.addTask("Task")
    project.addTask("Milestone")
    project.takeBaseline("Baseline")
    val file = File.createTempFile("BaselineImportTest", ".gan").also { it.deleteOnExit() }
    FileOutputStream(file).use { GanttXMLSaver(project).save(it) }
    return file
  }

  private fun savedFileWithBaseline(vararg names: String): File {
    val project = GanttProjectImpl()
    project.addTask("Task in file")
    names.forEach { project.takeBaseline(it) }
    val file = File.createTempFile("BaselineImportTest", ".gan").also { it.deleteOnExit() }
    FileOutputStream(file).use { GanttXMLSaver(project).save(it) }
    return file
  }

  private fun nicePreferences(): Preferences {
    val node = createNiceMock<Preferences>(Preferences::class.java)
    EasyMock.expect(node.get(EasyMock.anyString(), EasyMock.anyString()))
      .andReturn(MergeResourcesEnum.BY_ID.name.lowercase()).anyTimes()
    EasyMock.replay(node)
    val prefs = createNiceMock<Preferences>(Preferences::class.java)
    EasyMock.expect(prefs.node(EasyMock.anyString())).andReturn(node).anyTimes()
    EasyMock.replay(prefs)
    return prefs
  }
}

private class TestUIFacade(private val undoManager: GPUndoManager) : ConsoleUIFacade(null) {
  private val taskColumns = createNiceMock<ColumnList>(ColumnList::class.java)
  private val resourceColumns = createNiceMock<ColumnList>(ColumnList::class.java)

  override fun getUndoManager(): GPUndoManager = undoManager
  override fun getTaskColumnList(): ColumnList = taskColumns
  override fun getResourceColumnList(): ColumnList = resourceColumns
  override fun refresh() {}
}

/**
 * Remembers the baselines before an edit and puts them back on undo. Only what is changed inside
 * the edit is undone, as with the real undo manager.
 */
private class BaselineSnapshotUndoManager(private val baselines: MutableList<GanttPreviousState>) : GPUndoManager {
  private var before: List<GanttPreviousState>? = null

  override fun undoableEdit(localizedName: String, runnableEdit: Runnable) {
    before = baselines.toList()
    runnableEdit.run()
  }
  override fun canUndo() = before != null
  override fun canRedo() = false
  override fun undo() {
    before?.let {
      baselines.clear()
      baselines.addAll(it)
    }
    before = null
  }
  override fun redo() {}
  override val undoPresentationName = ""
  override val redoPresentationName = ""
  override fun addUndoableEditListener(listener: GPUndoListener) {}
  override fun removeUndoableEditListener(listener: GPUndoListener) {}
  override fun die() {}
  override fun addUndoableEditTxnFactory(factory: UndoableEditTxnFactory) {}
}
