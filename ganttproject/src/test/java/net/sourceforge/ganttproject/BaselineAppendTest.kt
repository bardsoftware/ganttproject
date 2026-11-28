/*
Copyright 2026

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
package net.sourceforge.ganttproject

import biz.ganttproject.core.io.parseXmlProject
import biz.ganttproject.core.time.CalendarFactory
import net.sourceforge.ganttproject.io.HistorySaver
import net.sourceforge.ganttproject.io.SaverBase
import net.sourceforge.ganttproject.parser.BaselineSerializer
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskManager
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.util.Locale
import javax.xml.transform.stream.StreamResult

class BaselineAppendTest : SaverBase() {
  private lateinit var taskManager: TaskManager
  private lateinit var oldTask: Task
  private lateinit var summary: Task
  private lateinit var newTask: Task

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
    taskManager = TestSetupHelper.newTaskManagerBuilder().build()
    oldTask = taskManager.newTaskBuilder().withName("old")
      .withStartDate(CalendarFactory.createGanttCalendar(2026, 8, 11).time).withDuration(taskManager.createLength(3)).build()
    summary = taskManager.newTaskBuilder().withName("summary")
      .withStartDate(CalendarFactory.createGanttCalendar(2026, 8, 11).time).build()
    taskManager.newTaskBuilder().withName("child").withParent(summary)
      .withStartDate(CalendarFactory.createGanttCalendar(2026, 8, 11).time).build()
    newTask = taskManager.newTaskBuilder().withName("new")
      .withStartDate(CalendarFactory.createGanttCalendar(2026, 9, 5).time).withDuration(taskManager.createLength(4)).build()
  }

  private fun baselineOfOldTasks(): GanttPreviousState {
    val entries = GanttPreviousState.createTasks(taskManager).filter { it.id != newTask.taskID }
    return GanttPreviousState("base", entries)
  }

  private fun rows(baseline: GanttPreviousState) = baseline.tasks.map {
    listOf(it.id, it.start.toXMLString(), it.duration, it.isMilestone, it.hasNested())
  }

  @Test
  fun `a new task gets an entry that looks like one taken with the baseline`() {
    val source = baselineOfOldTasks()
    val appended = (appendToBaseline(source, newTask, taskManager) as BaselineAppendResult.Appended).baseline

    assertEquals(source.tasks.size + 1, appended.tasks.size)
    val expected = GanttPreviousState.createTasks(taskManager).single { it.id == newTask.taskID }
    assertEquals(
      listOf(expected.id, expected.start.toXMLString(), expected.duration, expected.isMilestone, expected.hasNested()),
      rows(appended).last()
    )
    assertEquals(listOf(newTask.taskID, "2026-10-05", 4, false, false), rows(appended).last())
  }

  @Test
  fun `existing entries stay unchanged and the source is not touched`() {
    val source = baselineOfOldTasks()
    val before = rows(source)
    // The live plan moves on after the baseline was taken.
    oldTask.start.setTime(CalendarFactory.createGanttCalendar(2027, 0, 1).time)

    val appended = (appendToBaseline(source, newTask, taskManager, "other") as BaselineAppendResult.Appended).baseline

    assertEquals(before, rows(source), "the source baseline must not change")
    assertEquals(before, rows(appended).take(before.size), "the old entries must keep the values they were taken with")
    assertEquals("other", appended.name)
    assertEquals("base", source.name)
  }

  @Test
  fun `a task which already has an entry is skipped, not overwritten`() {
    val source = baselineOfOldTasks()
    val before = rows(source)
    oldTask.start.setTime(CalendarFactory.createGanttCalendar(2027, 0, 1).time)

    val result = appendToBaseline(source, oldTask, taskManager)

    assertEquals(BaselineAppendResult.AlreadyPresent(oldTask.taskID), result)
    assertEquals(before, rows(source))
  }

  @Test
  fun `the appended entry does not follow the live start date`() {
    val appended = (appendToBaseline(baselineOfOldTasks(), newTask, taskManager) as BaselineAppendResult.Appended).baseline
    newTask.start.setTime(CalendarFactory.createGanttCalendar(2030, 0, 1).time)
    assertEquals("2026-10-05", appended.tasks.last().start.toXMLString())
  }

  @Test
  fun `the appended baseline survives a save and load round trip`() {
    val appended = (appendToBaseline(baselineOfOldTasks(), newTask, taskManager) as BaselineAppendResult.Appended).baseline
    val xml = saveXml(listOf(appended))

    val loaded = mutableListOf<GanttPreviousState>()
    BaselineSerializer().loadBaselines(parseXmlProject(xml), loaded)

    assertEquals("base", loaded.single().name)
    assertEquals(rows(appended), rows(loaded.single()), "the document was:\n$xml")
  }

  @Test
  fun `undo restores the old baseline and redo the supplemented one`() {
    val source = baselineOfOldTasks()
    val list = mutableListOf(source)
    val undoManager = SerializingUndoManager(list)

    val result = appendToBaselineUndoable(list, source, newTask, taskManager, undoManager, "Supplement baseline")

    assertTrue(result is BaselineAppendResult.Appended)
    assertEquals("Supplement baseline", undoManager.lastName)
    assertEquals(source.tasks.size + 1, list.single().tasks.size)

    undoManager.undo()
    assertEquals(rows(source), rows(list.single()))

    undoManager.redo()
    assertEquals(rows((result as BaselineAppendResult.Appended).baseline), rows(list.single()))
  }

  @Test
  fun `a task which is already in the baseline registers no undoable edit`() {
    val source = baselineOfOldTasks()
    val list = mutableListOf(source)
    val undoManager = SerializingUndoManager(list)

    appendToBaselineUndoable(list, source, oldTask, taskManager, undoManager, "Supplement baseline")

    assertEquals(null, undoManager.lastName)
    assertSame(source, list.single())
  }

  @Test
  fun `a baseline which is not in the list is refused`() {
    val list = mutableListOf<GanttPreviousState>()
    assertThrows(IllegalArgumentException::class.java) {
      appendToBaselineUndoable(list, baselineOfOldTasks(), newTask, taskManager, SerializingUndoManager(list), "x")
    }
  }

  private fun saveXml(baselines: List<GanttPreviousState>): String {
    val out = ByteArrayOutputStream()
    val handler = createHandler(StreamResult(out))
    handler.startDocument()
    startElement("project", handler)
    startElement("previous", handler)
    baselines.forEach { HistorySaver().saveBaseline(it, handler) }
    endElement("previous", handler)
    endElement("project", handler)
    handler.endDocument()
    return out.toString(Charsets.UTF_8)
  }

  /**
   * Undo the way the real manager does it: the project is written before and after the edit and
   * undo/redo read one of the two back. Here "the project" is the baseline list, written with the
   * real HistorySaver and read with the real BaselineSerializer.
   */
  private inner class SerializingUndoManager(private val list: MutableList<GanttPreviousState>) : GPUndoManager {
    var lastName: String? = null
    private var before = ""
    private var after = ""

    override fun undoableEdit(localizedName: String, runnableEdit: Runnable) {
      lastName = localizedName
      before = saveXml(list)
      runnableEdit.run()
      after = saveXml(list)
    }

    private fun restore(xml: String) {
      val loaded = mutableListOf<GanttPreviousState>()
      BaselineSerializer().loadBaselines(parseXmlProject(xml), loaded)
      list.clear()
      list.addAll(loaded)
    }

    override fun undo() = restore(before)
    override fun redo() = restore(after)
    override fun canUndo() = before.isNotEmpty()
    override fun canRedo() = after.isNotEmpty()
    override val undoPresentationName = ""
    override val redoPresentationName = ""
    override fun addUndoableEditListener(listener: GPUndoListener) {}
    override fun removeUndoableEditListener(listener: GPUndoListener) {}
    override fun die() {}
    override fun addUndoableEditTxnFactory(factory: UndoableEditTxnFactory) {}
  }
}
