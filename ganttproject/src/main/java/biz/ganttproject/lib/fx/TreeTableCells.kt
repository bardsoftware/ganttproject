/*
Copyright 2021 BarD Software s.r.o

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

import biz.ganttproject.core.option.ValidationException
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import javafx.application.Platform
import javafx.beans.property.ReadOnlyDoubleWrapper
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.Cell
import javafx.scene.control.TextField
import javafx.scene.control.TreeTableCell
import javafx.scene.control.TreeTableColumn
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.util.Callback
import javafx.util.StringConverter
import javafx.util.converter.DefaultStringConverter
import javafx.util.converter.NumberStringConverter
import net.sourceforge.ganttproject.gui.UIUtil
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.task.Task

class TextCell<T, S>(
  private val converter: StringConverter<S>,
  private val editingCellController: (TextCell<T, S>?) -> Boolean
) : TreeTableCell<T, S>() {
  private val textField: TextField = createTextField(this, converter)

  init {
    styleClass.add("gp-tree-table-cell")
  }
  override fun startEdit() {
    if (!isEditable) {
      return
    }
    super.startEdit()

    if (isEditing && editingCellController(this)) {
      treeTableView.requestFocus()
      startEdit(this, converter, null, null, textField)
    }
  }

  override fun cancelEdit() {
    super.cancelEdit()
    styleClass.remove("validation-error")
    editingCellController(null)
    cancelEdit(this, converter, null)
    treeTableView.requestFocus()
  }

  fun commitEdit() {
    commitEdit(converter.fromString(textField.text))
  }

  override fun commitEdit(newValue: S?) {
    editingCellController(null)
    super.commitEdit(newValue)
    treeTableView.requestFocus()
  }

  override fun updateItem(item: S?, empty: Boolean) {
    super.updateItem(item, empty)
    if (treeTableView.focusModel.isFocused(treeTableRow.index, tableColumn)) {
      styleClass.add("focused")
    } else {
      styleClass.removeAll("focused")
    }
    updateItem(this, converter, null, null, textField)
  }
}

fun <T> startEdit(cell: Cell<T>, converter: StringConverter<T>, hbox: HBox?, graphic: Node?, textField: TextField) {
  textField.text = getItemText(cell, converter)
  cell.text = null
  if (graphic != null) {
    hbox?.children?.setAll(graphic, textField)
    cell.setGraphic(hbox)
  } else {
    cell.setGraphic(textField)
  }

  // requesting focus so that key input can immediately go into the
  // TextField (see RT-28132)
  Platform.runLater {
    textField.selectAll()
    textField.requestFocus()
  }
}

fun <T> cancelEdit(cell: Cell<T>, converter: StringConverter<T>, graphic: Node?) {
  cell.text = getItemText(cell, converter)
  cell.graphic = graphic
}

fun <T> createTextField(cell: Cell<T>, converter: StringConverter<T>) =
  TextField(getItemText(cell, converter)).also { textField ->
    // Use onAction here rather than onKeyReleased (with check for Enter),
    // as otherwise we encounter RT-34685
    textField.onAction = EventHandler { event: ActionEvent ->
      try {
        cell.commitEdit(converter.fromString(textField.text))
        cell.styleClass.remove("validation-error")
      } catch (ex: ValidationException) {
        cell.styleClass.add("validation-error")
      }
      finally {
        event.consume()
      }
    }
//    textField.onKeyTyped = EventHandler { t: KeyEvent ->
//      if (t.code == KeyCode.ESCAPE) {
//        cell.cancelEdit()
//        println("styleclass=${cell.styleClass}")
//        t.consume()
//      }
//    }
  }

private fun <T> getItemText(cell: Cell<T>?, converter: StringConverter<T>?) =
  converter?.toString(cell?.item) ?: cell?.item?.toString() ?: ""


private fun <T> updateItem(cell: Cell<T>, converter: StringConverter<T>, hbox: HBox?, graphic: Node?, textField: TextField?) {
  if (cell.isEmpty) {
    cell.text = null
    cell.setGraphic(null)
  } else {
    if (cell.isEditing) {
      if (textField != null) {
        textField.text = getItemText(cell, converter)
      }
      cell.text = null
      if (graphic != null) {
        hbox!!.children.setAll(graphic, textField)
        cell.setGraphic(hbox)
      } else {
        cell.setGraphic(textField)
      }
    } else {
      cell.text = getItemText(cell, converter)
      cell.setGraphic(graphic)
    }
  }
}

fun <S> createTextColumn(name: String, getValue: (S) -> String?, setValue: (S, String) -> Unit): TreeTableColumn<S, String> =
  TreeTableColumn<S, String>(name).apply {
    setCellValueFactory {
      ReadOnlyStringWrapper(getValue(it.value.value) ?: "")
    }
    cellFactory = TextCellFactory<S>()
    onEditCommit = EventHandler { event ->
      setValue(event.rowValue.value, event.newValue)
    }
  }

class GanttCalendarStringConverter : StringConverter<GanttCalendar>() {
  private val validator = UIUtil.createStringDateValidator(null) {
    listOf(GanttLanguage.getInstance().shortDateFormat)
  }
  override fun toString(value: GanttCalendar?) = value?.toString() ?: ""

  override fun fromString(text: String): GanttCalendar =
    CalendarFactory.createGanttCalendar(validator.parse(text))
}

fun <S> createDateColumn(name: String, getValue: (S) -> GanttCalendar?, setValue: (S, GanttCalendar) -> Unit): TreeTableColumn<S, GanttCalendar> =
  TreeTableColumn<S, GanttCalendar>(name).apply {
    setCellValueFactory {
      ReadOnlyObjectWrapper(getValue(it.value.value))
    }
    val converter = GanttCalendarStringConverter()
    cellFactory = Callback { TextCell<S, GanttCalendar>(converter) { true } }
    onEditCommit = EventHandler { event -> setValue(event.rowValue.value, event.newValue) }
  }

fun <S> createIntegerColumn(name: String, getValue: (S) -> Int?, setValue: (S, Int) -> Unit) =
  TreeTableColumn<S, Number>(name).apply {
    setCellValueFactory {
      ReadOnlyIntegerWrapper(getValue(it.value.value) ?: 0)
    }
    cellFactory = Callback { TextCell<S, Number>(NumberStringConverter()) { true } }
    onEditCommit = EventHandler { event -> setValue(event.rowValue.value, event.newValue.toInt()) }
  }

fun <S> createDoubleColumn(name: String, getValue: (S) -> Double?, setValue: (S, Double) -> Unit) =
  TreeTableColumn<S, Number>(name).apply {
    setCellValueFactory {
      ReadOnlyDoubleWrapper(getValue(it.value.value) ?: 0.0)
    }
    cellFactory = Callback { TextCell<S, Number>(NumberStringConverter()) { true } }
    onEditCommit = EventHandler { event -> setValue(event.rowValue.value, event.newValue.toDouble()) }
  }

class TextCellFactory<S>(private val cellSetup: (TextCell<S, String>) -> Unit = {}): Callback<TreeTableColumn<S, String>, TreeTableCell<S, String>> {
  internal var editingCell: TextCell<S, String>? = null

  private fun setEditingCell(cell: TextCell<S, String>?): Boolean =
    when {
      editingCell == null && cell == null -> true
      editingCell == null && cell != null -> {
        editingCell = cell
        true
      }
      editingCell != null && cell == null -> {
        editingCell = cell
        true
      }
      editingCell != null && cell != null -> {
        // new editing cell when old is not yet releases
        false
      }
      else -> true
    }

  override fun call(param: TreeTableColumn<S, String>?) =
    TextCell(DefaultStringConverter(), this::setEditingCell).also(cellSetup)
}

