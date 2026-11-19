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
package biz.ganttproject.lib.fx

import biz.ganttproject.app.getCurrentLocale
import biz.ganttproject.app.setLocale
import biz.ganttproject.core.option.ValidationException
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.TreeTablePosition
import javafx.scene.control.TreeTableView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.sourceforge.ganttproject.undo.GPUndoListener
import net.sourceforge.ganttproject.undo.GPUndoManager
import net.sourceforge.ganttproject.undo.UndoableEditTxnFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.Locale

/** Runs [code] in the JavaFX application thread, which cells and tables need. */
internal fun <T> onFxThread(code: () -> T): T = runBlocking { withContext(Dispatchers.JavaFx) { code() } }

/** Parses [text] the way a decimal cell does when the user finishes editing it. */
internal fun <S> parseDecimalCellText(text: String): BigDecimal? =
  decimalConverter<S>().let { converter -> converter.fromString(TextCell(converter), text) }

/** Commits [newValue] to the cell of [column] in the row of [rowValue], as the table does when an edit is finished. */
internal fun <S> commitDecimalEdit(column: TreeTableColumn<S, BigDecimal>, rowValue: S, newValue: BigDecimal?) {
  val table = TreeTableView(TreeItem(rowValue))
  table.columns.add(column)
  column.onEditCommit.handle(TreeTableColumn.CellEditEvent(
    table, TreeTablePosition(table, 0, column), TreeTableColumn.editCommitEvent(), newValue
  ))
}

/** Undo manager which just runs the edits. */
internal val runningUndoManager = object : GPUndoManager {
  override fun undoableEdit(localizedName: String, runnableEdit: Runnable) = runnableEdit.run()
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

class DecimalColumnTest {
  private val initialLocale = getCurrentLocale()

  @AfterEach
  fun restoreLocale() {
    setLocale(initialLocale)
  }

  @Test
  fun `a decimal comma is accepted in a decimal cell with the German locale`() = onFxThread {
    setLocale(Locale.GERMANY)
    assertEquals(BigDecimal("12.5"), parseDecimalCellText<String>("12,5"))
  }

  @Test
  fun `a decimal point is accepted in a decimal cell with the English locale`() = onFxThread {
    setLocale(Locale.US)
    assertEquals(BigDecimal("12.5"), parseDecimalCellText<String>("12.5"))
  }

  @Test
  fun `a text which is not a number is rejected as invalid input`() = onFxThread {
    assertThrows<ValidationException> { parseDecimalCellText<String>("12x") }
    Unit
  }

  @Test
  fun `an emptied decimal cell commits no value rather than zero`() = onFxThread {
    val committed = mutableListOf<BigDecimal?>()
    val column = createDecimalColumn<String>("decimal", getValue = { BigDecimal.ONE }, setValue = { _, value ->
      committed.add(value)
    })
    commitDecimalEdit(column, "row", parseDecimalCellText<String>(""))
    assertEquals(1, committed.size)
    assertNull(committed[0])
  }
}
