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

//import biz.ganttproject.lib.fx.treetable.TreeTableCellSkin
import biz.ganttproject.app.Localizer
import biz.ganttproject.app.getModifiers
import biz.ganttproject.core.option.FontOption
import biz.ganttproject.core.option.FontSpec
import biz.ganttproject.core.option.ValidationException
import biz.ganttproject.core.option.createStringDateValidator
import biz.ganttproject.core.time.CalendarFactory
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.lib.fx.treetable.TreeTableCellSkin
import de.jensd.fx.glyphs.GlyphIcon
import javafx.application.Platform
import javafx.beans.property.*
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.effect.InnerShadow
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.text.Font
import javafx.util.Callback
import javafx.util.StringConverter
import javafx.util.converter.BigDecimalStringConverter
import javafx.util.converter.DefaultStringConverter
import javafx.util.converter.NumberStringConverter
import net.sourceforge.ganttproject.language.GanttLanguage
import java.math.BigDecimal
import javax.swing.UIManager

data class MyStringConverter<S, T>(
  val toString: (cell: TextCell<S, T>, cellValue: T?) -> String?,
  val fromString: (cell: TextCell<S, T>, stringValue: String) -> T?
)
fun <S, T> StringConverter<T>.adapt(): MyStringConverter<S, T> =
  MyStringConverter(
    toString = { _, cellValue -> this.toString(cellValue) },
    fromString = { _, stringValue -> this.fromString(stringValue) }
  )

val applicationFont = SimpleObjectProperty(Font.getDefault())
fun initFontProperty(appFontOption: FontOption) {
  appFontOption.addChangeValueListener { event ->
    event.newValue?.let {
      if (it is FontSpec) {
        Font.font(it.family)?.let { font ->
          applicationFont.set(font)
        }
      }
    }
  }
}
val applicationBackground = SimpleObjectProperty(Color.BLACK)
val applicationForeground = SimpleObjectProperty<Paint>(Color.BLACK)
fun initColorProperties() {
  UIManager.addPropertyChangeListener { evt ->
    if ("lookAndFeel" == evt.propertyName && evt.oldValue != evt.newValue) {
      UIManager.getColor("TableHeader.background")?.let { swingColor ->
        val fxColor = Color.color(swingColor.red / 255.0, swingColor.green / 255.0, swingColor.blue / 255.0)
        applicationBackground.value = fxColor
      }
      UIManager.getColor("TableHeader.foreground")?.let { swingColor ->
        applicationForeground.value = Color.color(swingColor.red / 255.0, swingColor.green / 255.0, swingColor.blue / 255.0)
      }
    }
  }
}

class TextCell<S, T>(
  private val converter: MyStringConverter<S, T>,
  private val editingCellController: (TextCell<S, T>?) -> Boolean
) : TreeTableCell<S, T>() {
  private var savedGraphic: Node? = null
  var graphicSupplier: (T) -> Node? = { null }
  private val textField: TextField = createTextField().also {
    it.fontProperty().bind(applicationFont)
  }
  private val disclosureNode: Node? get() = parent?.lookup(".arrow")

  override fun createDefaultSkin(): Skin<*> {
    return TreeTableCellSkin(this)
  }

  init {
    styleClass.add("gp-tree-table-cell")
    fontProperty().bind(applicationFont)
  }

  override fun startEdit() {
    if (!isEditable) {
      return
    }
    super.startEdit()
    contentDisplay = ContentDisplay.GRAPHIC_ONLY
    //treeTableRow.styleClass.add("editing-row")
    disclosureNode?.let {
      it.isVisible = false
    }

    if (isEditing && editingCellController(this)) {
      treeTableView.requestFocus()
      doStartEdit()
    } else {
      println("NOPE!")
    }
  }

  private fun doStartEdit() {
    textField.text = getItemText()
    text = " "
    savedGraphic = graphic
    graphic = textField


    // requesting focus so that key input can immediately go into the
    // TextField (see RT-28132)
    Platform.runLater {
      textField.selectAll()
      textField.requestFocus()
    }
  }

  override fun cancelEdit() {
    super.cancelEdit()
    styleClass.remove("validation-error")
    disclosureNode?.let {
      it.isVisible = true
    }
    editingCellController(null)
    doCancelEdit()
    treeTableView.requestFocus()
    treeTableView.refresh()
  }

  private fun doCancelEdit() {
    text = getItemText()
    graphic = savedGraphic
    savedGraphic = null
    disclosureNode?.let {
      it.isVisible = true
    }
    parent?.requestLayout()
  }

  override fun commitEdit(newValue: T?) {
    disclosureNode?.let {
      it.isVisible = true
    }
    editingCellController(null)
    super.commitEdit(newValue)
    treeTableView.requestFocus()
    graphic = savedGraphic
    savedGraphic = null
  }

  fun commitEdit() {
    commitEdit(converter.fromString(this, textField.text))
  }

  private fun commitText(text: String) = commitEdit(converter.fromString(this, text))

  override fun updateItem(cellValue: T?, empty: Boolean) {
    super.updateItem(cellValue, empty)
    if (treeTableView.focusModel.isFocused(treeTableRow.index, tableColumn)) {
      if (styleClass.indexOf("focused") < 0) {
        styleClass.add("focused")
      }
    } else {
      styleClass.removeAll("focused")
    }
    doUpdateItem()
  }

  private fun doUpdateItem() {
    if (isEmpty) {
      text = null
      graphic = null
    } else {
      doUpdateFilledItem()
    }
  }

  private fun doUpdateFilledItem() {
    if (isEditing) {
      textField.text = getItemText()
      text = null
      graphic = textField
    } else {
      text = getItemText()
      graphic = graphicSupplier(this.item)
      contentDisplay = ContentDisplay.RIGHT
    }
  }

  private fun getItemText() = converter.toString(this, this.item)
  private fun createTextField() =
    TextField(getItemText()).also { textField ->
      //textField.prefWidth = this.width
      // Use onAction here rather than onKeyReleased (with check for Enter),
      // as otherwise we encounter RT-34685
      textField.onAction = EventHandler { event: ActionEvent ->
        try {
          commitText(textField.text)
          styleClass.remove("validation-error")
          effect = null
        } catch (ex: ValidationException) {
          styleClass.add("validation-error")
          effect = InnerShadow(10.0, Color.RED)
        }
        finally {
          event.consume()
        }
      }
      textField.onKeyPressed = EventHandler { event ->
        if (event.code == KeyCode.INSERT && event.getModifiers() == 0) {
          try {
            commitText(textField.text)
            styleClass.remove("validation-error")
          } catch (ex: ValidationException) {
            styleClass.add("validation-error")
          }
          finally {
            event.consume()
          }
        }
      }
    }
}

fun <S> createTextColumn(name: String, getValue: (S) -> String?, setValue: (S, String) -> Unit): TreeTableColumn<S, String> =
  TreeTableColumn<S, String>(name).apply {
    setCellValueFactory {
      ReadOnlyStringWrapper(getValue(it.value.value) ?: "")
    }
    cellFactory = TextCellFactory<S, String>(converter = DefaultStringConverter().adapt()) {
      it.styleClass.add("text-left")
    }
    onEditCommit = EventHandler { event ->
      setValue(event.rowValue.value, event.newValue)
    }
  }

class GanttCalendarStringConverter : StringConverter<GanttCalendar>() {
  private val validator = createStringDateValidator(null) {
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
    cellFactory = Callback { TextCell<S, GanttCalendar>(converter.adapt(), { true }).also {
      it.styleClass.add("text-left")
    } }
    onEditCommit = EventHandler { event -> setValue(event.rowValue.value, event.newValue) }
  }

fun <S> createIntegerColumn(name: String, getValue: (S) -> Int?, setValue: (S, Int) -> Unit) =
  TreeTableColumn<S, Number>(name).apply {
    setCellValueFactory {
      ReadOnlyIntegerWrapper(getValue(it.value.value) ?: 0)
    }
    cellFactory = Callback {
      TextCell<S, Number>(NumberStringConverter().adapt(), { true }).also {
        it.styleClass.add("text-right")
      }
    }
    onEditCommit = EventHandler { event -> setValue(event.rowValue.value, event.newValue.toInt()) }
  }

fun <S> createDoubleColumn(name: String, getValue: (S) -> Double?, setValue: (S, Double) -> Unit) =
  TreeTableColumn<S, Number>(name).apply {
    setCellValueFactory {
      ReadOnlyDoubleWrapper(getValue(it.value.value) ?: 0.0)
    }
    cellFactory = Callback {
      TextCell<S, Number>(NumberStringConverter().adapt(), { true }).also {
        it.styleClass.add("text-right")
      }
    }
    onEditCommit = EventHandler { event -> setValue(event.rowValue.value, event.newValue.toDouble()) }
  }

fun <S> createDecimalColumn(name: String, getValue: (S) -> BigDecimal?, setValue: (S, BigDecimal) -> Unit) =
  TreeTableColumn<S, BigDecimal>(name).apply {
    setCellValueFactory {
      ReadOnlyObjectWrapper(getValue(it.value.value) ?: 0.toBigDecimal())
    }
    cellFactory = Callback {
      TextCell<S, BigDecimal>(BigDecimalStringConverter().adapt(), { true }).also {
        it.styleClass.add("text-right")
      }
    }
    onEditCommit = EventHandler { event -> setValue(event.rowValue.value, event.newValue.toDouble().toBigDecimal()) }
  }

fun <S, T> createIconColumn(name: String, getValue: (S) ->T?, iconFactory: (T) -> GlyphIcon<*>?, i18n: Localizer) =
  TreeTableColumn<S, T>(name).apply {
    setCellValueFactory {
      ReadOnlyObjectWrapper(getValue(it.value.value))
    }
    cellFactory = Callback {
      val cell = TextCell<S, T>(MyStringConverter(
        toString = { _, value -> i18n.formatText(value?.toString()?.lowercase() ?: "") },
        fromString = { _, _ -> null}
      ), {true})
      cell.graphicSupplier = {
        iconFactory(it)
      }
      cell.contentDisplay = ContentDisplay.LEFT
      cell.alignment = Pos.CENTER_LEFT

      cell
    }
  }

class TextCellFactory<S, T>(
  private val converter: MyStringConverter<S, T>,
  private val cellSetup: (TextCell<S, T>) -> Unit = {}
): Callback<TreeTableColumn<S, T>, TreeTableCell<S, T>> {
  internal var editingCell: TextCell<S, T>? = null

  private fun setEditingCell(cell: TextCell<S, T>?): Boolean {
    //println("editingcell=$editingCell cell=$cell")
    return when {
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
        // new editing cell when old is not yet released
        editingCell?.treeTableRow?.index != cell.treeTableRow.index
      }
      else -> true
    }
  }
  override fun call(param: TreeTableColumn<S, T>?) =
    TextCell(converter, this::setEditingCell).also(cellSetup)
}

