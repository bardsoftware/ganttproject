/*
Copyright 2026 BarD Software s.r.o

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
package biz.ganttproject.ganttview

import biz.ganttproject.app.getCurrentLocale
import biz.ganttproject.app.setLocale
import biz.ganttproject.core.io.XmlProjectImporter
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.ColumnBuilder
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.CalendarFactory.LocaleApi
import biz.ganttproject.core.time.CalendarFactory.setLocaleApi
import biz.ganttproject.lib.fx.commitDecimalEdit
import biz.ganttproject.lib.fx.onFxThread
import biz.ganttproject.lib.fx.parseDecimalCellText
import biz.ganttproject.lib.fx.runningUndoManager
import javafx.scene.control.TreeTableColumn
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.io.GanttXMLSaver
import net.sourceforge.ganttproject.task.Task
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Locale

/**
 * Editing the effort cell of the task table, from the text typed into the cell to the attribute in the project file.
 */
class EffortCellTest {
  private val initialLocale = getCurrentLocale()

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
  }

  @AfterEach
  fun restoreLocale() {
    setLocale(initialLocale)
  }

  private val projectXml = """
    <project name="p">
      <tasks>
        <task id="1" uid="u1" name="T" start="2026-11-09" duration="1" effort-hours="12.5"/>
      </tasks>
    </project>
  """.trimIndent()

  private fun save(project: IGanttProject): String {
    val out = ByteArrayOutputStream()
    GanttXMLSaver(project).save(out)
    return out.toString(Charsets.UTF_8)
  }

  /** Types [text] into the effort cell of [task] and finishes the edit. */
  private fun editEffortCell(project: IGanttProject, task: Task, text: String) {
    val columnBuilder = object : ColumnBuilder<Task, TaskDefaultColumn>(
      TaskTableModel(project.taskCustomColumnManager), project.taskCustomColumnManager, runningUndoManager, { null }
    ) {
      fun effortColumn() = createDefaultColumn(TaskDefaultColumn.EFFORT)
    }
    @Suppress("UNCHECKED_CAST")
    val column = columnBuilder.effortColumn() as TreeTableColumn<Task, BigDecimal>
    commitDecimalEdit(column, task, parseDecimalCellText<Task>(text))
  }

  @Test
  fun `effort typed with a decimal comma is saved with the German locale`() = onFxThread {
    setLocale(Locale.GERMANY)
    val project = XmlProjectImporter().import(projectXml)
    val task = project.taskManager.getTask(1)
    editEffortCell(project, task, "7,5")

    assertEquals(BigDecimal("7.5"), task.effort)
    val saved = save(project)
    assertTrue(saved.contains("""effort-hours="7.5""""), "the edited effort has to be saved, the document was:\n$saved")
  }

  @Test
  fun `emptied effort cell is saved without the effort attribute and loads empty`() = onFxThread {
    setLocale(Locale.GERMANY)
    val project = XmlProjectImporter().import(projectXml)
    val task = project.taskManager.getTask(1)
    editEffortCell(project, task, "")

    assertNull(task.effort)
    val saved = save(project)
    assertFalse(saved.contains("effort-hours"), "an emptied effort must not be saved, the document was:\n$saved")
    assertNull(XmlProjectImporter().import(saved).taskManager.getTask(1).effort)
  }
}
