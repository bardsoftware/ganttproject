/*
Copyright 2026 BarD Software s.r.o., GanttProject Cloud OU

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
package net.sourceforge.ganttproject.action.task

import biz.ganttproject.app.DefaultLocalizer
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.calendar.GanttDaysOff
import biz.ganttproject.core.model.task.LevelConflict
import biz.ganttproject.core.model.task.LevelResult
import biz.ganttproject.core.model.task.SHARED_POOL
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import net.sourceforge.ganttproject.GanttProjectImpl
import net.sourceforge.ganttproject.PrjInfos
import net.sourceforge.ganttproject.document.Document
import net.sourceforge.ganttproject.document.DocumentManager
import net.sourceforge.ganttproject.document.FileDocument
import net.sourceforge.ganttproject.document.testProxyDocument
import net.sourceforge.ganttproject.document.testUiFacade
import net.sourceforge.ganttproject.io.GanttXMLOpen
import net.sourceforge.ganttproject.io.GanttXMLSaver
import net.sourceforge.ganttproject.parser.ParserFactory
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoManagerImpl
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.easymock.EasyMock.anyString
import org.easymock.EasyMock.createNiceMock
import org.easymock.EasyMock.expect
import org.easymock.EasyMock.getCurrentArguments
import org.easymock.EasyMock.replay
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.text.DateFormat
import java.time.LocalDate
import java.util.Locale

/** The levelling menu action: the preview, cancelling, confirming and undoing a whole run. */
class LevellingActionTest {
  private lateinit var project: RestorableProject
  private val monday = LocalDate.of(2026, 11, 2)
  private val originalLocalizer = RootLocalizer
  private var edits = 0
  private val reports = mutableListOf<String>()
  private var preview: String? = null

  // Runs every edit at once and counts them.
  private val countingUndoManager = object : GPUndoManager {
    override fun undoableEdit(localizedName: String, runnableEdit: Runnable) {
      edits++
      runnableEdit.run()
    }
    override fun canUndo() = false
    override fun canRedo() = false
    override fun undo() {}
    override fun redo() {}
    override val undoPresentationName = ""
    override val redoPresentationName = ""
    override fun addUndoableEditListener(listener: GPUndoListener) {}
    override fun removeUndoableEditListener(listener: GPUndoListener) {}
    override fun die() {}
    override fun addUndoableEditTxnFactory(factory: UndoableEditTxnFactory) {}
  }

  @BeforeEach
  fun setUp() {
    object : CalendarFactory() {
      init {
        setLocaleApi(object : LocaleApi {
          override fun getLocale(): Locale = Locale.US
          override fun getShortDateFormat(): DateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US)
        })
      }
    }
    // Shows the key and its arguments, so that the tests do not depend on the translation.
    RootLocalizer = object : DefaultLocalizer() {
      override fun formatTextOrNull(key: String, vararg args: Any): String = "[$key${args.joinToString("") { " $it" }}]"
    }
    project = RestorableProject()
  }

  @AfterEach
  fun tearDown() {
    RootLocalizer = originalLocalizer
  }

  private fun resource(name: String): HumanResource =
    project.humanResourceManager.create(name, project.humanResourceManager.resources.size).also {
      it.hoursPerDay = BigDecimal(8)
    }

  private fun task(name: String, days: Int, vararg assignments: Pair<HumanResource, Float>): Task =
    project.taskManager.newTaskBuilder().withName(name)
      .withStartDate(CalendarFactory.createGanttCalendar(2026, 10, 2).time)
      .withDuration(project.taskManager.createLength(days.toLong())).build().also { task ->
        assignments.forEach { (resource, load) -> task.assignmentCollection.addAssignment(resource).load = load }
      }

  private fun action(answer: Boolean, today: LocalDate = monday, undoManager: GPUndoManager = countingUndoManager) =
    LevellingAction(project.taskManager, undoManager,
      { message, confirmed -> preview = message; confirmed(answer) },
      { _, message -> reports.add(message) },
      { today })

  private fun startOf(task: Task): LocalDate =
    project.taskManager.getTask(task.taskID).start.let { LocalDate.of(it.year, it.month + 1, it.date) }

  @Test
  fun `the preview counts the moved tasks and names a missed deadline`() {
    val anna = resource("anna")
    task("first", 3, anna to 100f)
    task("second", 2, anna to 100f).also { t ->
      t.createMutator().also { it.setDeadline(CalendarFactory.createGanttCalendar(2026, 10, 3)); it.commit() }
    }
    action(answer = false).actionPerformed(null)
    val text = preview!!
    assertTrue(text.startsWith("[task.level.preview 1]")) { text }
    assertTrue(text.contains("[task.level.deadline 1]")) { text }
    // Due on Tuesday, levelled to Thursday and Friday: three working days late.
    assertTrue(text.contains("second: 2026-11-03 / 2026-11-06 (+3)")) { text }
    assertFalse(text.contains("task.level.past")) { text }
  }

  @Test
  fun `the preview says that unstarted tasks in the past are moved`() {
    val anna = resource("anna")
    task("first", 1, anna to 50f)
    action(answer = false, today = monday.plusDays(2)).actionPerformed(null)
    val text = preview!!
    assertTrue(text.contains("[task.level.past 1]\n  • first")) { text }
  }

  @Test
  fun `the preview names an overload of two fixed dates`() {
    val anna = resource("anna")
    task("first", 1, anna to 100f).also { t -> t.createMutator().also { it.setDateFixed(true); it.commit() } }
    task("second", 1, anna to 100f).also { t -> t.createMutator().also { it.setDateFixed(true); it.commit() } }
    action(answer = false).actionPerformed(null)
    val text = preview!!
    assertTrue(text.startsWith("[task.level.preview 0]")) { text }
    assertTrue(text.contains("[task.level.overload 1]\n  • 2026-11-02: 200% (first, second)")) { text }
  }

  @Test
  fun `the preview names a fixed date which its dependency does not allow`() {
    val first = task("first", 3)
    val second = task("second", 1).also { t -> t.createMutator().also { it.setDateFixed(true); it.commit() } }
    // The scheduler would move the second task after the first one at once.
    project.taskManager.algorithmCollection.scheduler.isEnabled = false
    project.taskManager.dependencyCollection.createDependency(second, first)
    action(answer = false).actionPerformed(null)
    val text = preview!!
    assertTrue(text.contains("[task.level.unreachable 1]\n  • second: 2026-11-02 / 2026-11-05")) { text }
  }

  @Test
  fun `the preview names a task whose blocking resource is never present`() {
    val anna = resource("anna")
    val bob = resource("bob")
    // Away for three centuries, longer than any search.
    bob.addDaysOff(GanttDaysOff(CalendarFactory.createGanttCalendar(2026, 0, 1).time,
      CalendarFactory.createGanttCalendar(2326, 0, 1).time))
    task("first", 2, anna to 100f)
    task("second", 1, anna to 100f).assignmentCollection.addAssignment(bob).also {
      it.load = 0f
      it.isBlocking = true
    }
    action(answer = false).actionPerformed(null)
    val text = preview!!
    assertTrue(text.contains("[task.level.noDate 1]\n  • second: [task.level.noDate.absence bob]")) { text }
  }

  @Test
  fun `the preview gives one row per task with every reason`() {
    val anna = resource("anna")
    val bob = resource("bob")
    val first = task("first", 1, anna to 100f, bob to 0f)
    val second = task("second", 1)
    val result = LevelResult(emptyMap(), listOf(
      LevelConflict.NoPossibleDate(first.taskID.toString(), listOf(bob.id.toString()), listOf(anna.id.toString())),
      LevelConflict.NoPossibleDate(second.taskID.toString(), emptyList(), listOf(SHARED_POOL))
    ))
    val text = action(answer = false).levellingPreview(result, 0, monday)
    assertTrue(text.contains("[task.level.noDate 2]\n" +
      "  • first: [task.level.noDate.absence bob]; [task.level.noDate.capacity anna]\n" +
      "  • second: [task.level.noDate.capacity [task.level.noDate.sharedPool]]")) { text }
  }

  @Test
  fun `cancelling the preview changes nothing`() {
    val anna = resource("anna")
    task("first", 3, anna to 100f)
    val second = task("second", 2, anna to 100f)
    action(answer = false).actionPerformed(null)
    assertTrue(preview != null)
    assertEquals(0, edits)
    assertEquals(monday, startOf(second))
    assertEquals(2, second.duration.length)
  }

  @Test
  fun `confirming the preview moves the tasks in one edit`() {
    val anna = resource("anna")
    task("first", 3, anna to 100f)
    val second = task("second", 2, anna to 100f)
    action(answer = true).actionPerformed(null)
    assertEquals(1, edits)
    assertEquals(monday.plusDays(3), startOf(second))
  }

  @Test
  fun `levelling starts an effort driven task after a day off on its start`() {
    val anna = resource("anna")
    anna.addDaysOff(GanttDaysOff(CalendarFactory.createGanttCalendar(2026, 10, 2).time,
      CalendarFactory.createGanttCalendar(2026, 10, 3).time))
    val first = task("first", 1, anna to 100f).also { t ->
      t.createMutator().also { it.setEffort(BigDecimal(16)); it.setEffortDriven(true); it.commit() }
    }
    first.assignments.single().isBlocking = true
    action(answer = true).actionPerformed(null)
    assertEquals(monday.plusDays(1), startOf(first))
    assertEquals(2, first.duration.length)
  }

  @Test
  fun `nothing to move is reported without a preview`() {
    val anna = resource("anna")
    task("first", 3, anna to 50f)
    task("second", 2, anna to 50f)
    action(answer = true).actionPerformed(null)
    assertNull(preview)
    assertEquals(listOf("[task.level.nothing]"), reports)
    assertEquals(0, edits)
  }

  @Test
  fun `one undo takes back the whole run`() {
    val anna = resource("anna")
    val first = task("first", 3, anna to 100f)
    val second = task("second", 2, anna to 100f)
    val third = task("third", 1, anna to 100f)
    val undoManager = project.undoManager()
    action(answer = true, undoManager = undoManager).actionPerformed(null)
    assertEquals(monday, startOf(first))
    assertEquals(monday.plusDays(3), startOf(second))
    assertEquals(monday.plusDays(7), startOf(third))
    assertEquals("[task.level.undo]", undoManager.undoPresentationName.substringAfter(' ', ""))

    undoManager.undo()
    assertEquals(listOf(monday, monday, monday), listOf(first, second, third).map { startOf(it) })
    assertEquals(listOf(3, 2, 1), listOf(first, second, third).map { project.taskManager.getTask(it.taskID).duration.length })
    assertNull(project.taskManager.getTask(second.taskID).third)
    assertFalse(undoManager.canUndo())
  }

  /**
   * A project which undo can restore: it has a document, forgets its tasks and resources when it is closed, and saves
   * and reads through the real project file format.
   */
  private class RestorableProject : GanttProjectImpl() {
    override var document: Document = FileDocument(Files.createTempFile("levelling", ".gan").toFile())
    private val uiFacade = testUiFacade()
    private val parserFactory = object : ParserFactory {
      override fun newParser() = GanttXMLOpen(PrjInfos(), taskManager, uiFacade)
      override fun newSaver() = GanttXMLSaver(this@RestorableProject)
    }

    init {
      addProjectEventListener(taskManager.projectListener)
      addProjectEventListener(humanResourceManager.projectListener)
    }

    override fun close() = fireProjectClosed()

    fun undoManager(): UndoManagerImpl {
      val documentManager = createNiceMock<DocumentManager>(DocumentManager::class.java)
      expect(documentManager.getDocument(anyString())).andStubAnswer {
        testProxyDocument(FileDocument(File(getCurrentArguments()[0] as String)), this, uiFacade, parserFactory)
      }
      replay(documentManager)
      return UndoManagerImpl(this, parserFactory, documentManager)
    }
  }
}

