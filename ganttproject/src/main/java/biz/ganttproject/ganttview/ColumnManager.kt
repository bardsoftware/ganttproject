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
package biz.ganttproject.ganttview

import biz.ganttproject.app.Localizer
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.dialog
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.option.*
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.customproperty.*
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.vbox
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.effect.InnerShadow
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.util.Callback
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.undo.GPUndoManager
import org.controlsfx.control.PropertySheet
import org.controlsfx.property.BeanProperty
import org.controlsfx.property.editor.PropertyEditor
import java.beans.PropertyDescriptor
import javax.swing.SwingUtilities

/**
 * @author dbarashev@bardsoftware.com
 */
class ColumnManager(
  // The list of columns shown in the task table
  private val currentTableColumns: ColumnList,
  private val customColumnsManager: CustomPropertyManager,
  private val undoManager: GPUndoManager,
  private val calculationMethodValidator: CalculationMethodValidator
  ) {

  internal val btnAddController = BtnController(onAction = this::onAddColumn)
  internal val btnDeleteController = BtnController(onAction = this::onDeleteColumn)

  private val listItems = FXCollections.observableArrayList<ColumnAsListItem>()
  private val listView: ListView<ColumnAsListItem> = ListView()
  private val errorLabel = Label().also {
    it.styleClass.addAll("hint", "hint-validation")
  }
  private val errorPane = HBox().also {
    it.styleClass.addAll("hint-validation-pane", "noerror")
    it.children.add(errorLabel)
  }

  private val customPropertyEditor = CustomPropertyEditor(
    customColumnsManager, calculationMethodValidator, btnDeleteController, listItems,
    errorUi = {
      if (it == null) {
        errorPane.isVisible = false
        if (!errorPane.styleClass.contains("noerror")) {
          errorPane.styleClass.add("noerror")
        }
        errorLabel.text = ""
      }
      else {
        errorLabel.text = it
        errorPane.isVisible = true
        errorPane.styleClass.remove("noerror")
      }
    })
  internal val content: Node
  private val mergedColumns: MutableList<ColumnList.Column> = mutableListOf()

  init {
    mergedColumns.addAll(currentTableColumns.exportData())
    // First go columns which are shown in the table now
    // and they are ordered the same way are shown in the table.
    listItems.setAll(mergedColumns.sortedWith { col1, col2 -> columnsOrder(col1, col2) }.map { col ->
      val isCustom = customColumnsManager.definitions.find { it.id == col.id } != null
      ColumnAsListItem(col, col.isVisible, isCustom, customColumnsManager)
    })
    customColumnsManager.definitions.forEach { def ->
      if (mergedColumns.find { it.id == def.id } == null) {
        val columnStub = ColumnList.ColumnStub(def.id, def.name, false, -1, -1)
        mergedColumns.add(columnStub)
        listItems.add(ColumnAsListItem(columnStub, columnStub.isVisible, true, customColumnsManager))
      }
    }
    listView.items = listItems
    listView.cellFactory = Callback { CellImpl() }
    customPropertyEditor.propertySheet.apply {
      items.setAll(FXCollections.observableArrayList(customPropertyEditor.props))
      isModeSwitcherVisible = false
      isSearchBoxVisible = false
    }
    val propertySheetBox = vbox {
      addClasses("property-sheet-box")
      add(customPropertyEditor.propertySheetLabel, Pos.CENTER_LEFT, Priority.NEVER)
      add(customPropertyEditor.propertySheet, Pos.CENTER, Priority.ALWAYS)
      add(errorPane)
    }
    content = HBox().also {
      it.styleClass.add("content-pane")
      it.children.addAll(listView, propertySheetBox)
      HBox.setHgrow(propertySheetBox, Priority.ALWAYS)
    }

    listView.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
      customPropertyEditor.selectedItem = newValue
    }
    listView.selectionModel.select(0)

  }

  private fun onAddColumn() {
    val item = ColumnAsListItem(
      null,
      isVisible = true, isCustom = true, customColumnsManager
    ).also {
      val title = RootLocalizer.create("addCustomColumn").also {
        it.update("")
      }
      var count = 1
      while (listView.items.any { it.title == title.value.trim() }) {
        title.update(count.toString())
        count++
      }
      it.title = title.value.trim()
    }
    listItems.add(item)
    listView.scrollTo(item)
    listView.selectionModel.select(item)
    customPropertyEditor.focus()
  }

  private fun onDeleteColumn() {
    listItems.removeAll(listView.selectionModel.selectedItems)
  }

  fun applyChanges() {
    // First we remove custom columns which were removed from the list.
    mergedColumns.forEach { existing ->
      listItems.find { it.column?.id == existing.id } ?: run {
        customColumnsManager.definitions.find { def -> def.id == existing.id }?.let(customColumnsManager::deleteDefinition)
      }
    }

    listItems.forEach { columnItem ->
      columnItem.column?.let {
        // Apply changes made in the columns which has existed before.
        it.isVisible = columnItem.isVisible
        if (columnItem.isCustom) {
          customColumnsManager.definitions.find { def -> def.id == it.id }?.fromColumnItem(columnItem)
        }
      } ?: run {
        // Create custom columns which were added in the dialog
        val def = customColumnsManager.createDefinition(columnItem.type.getCustomPropertyClass(), columnItem.title, columnItem.defaultValue)
        if (columnItem.isVisible) {
          mergedColumns.add(ColumnList.ColumnStub(def.id, def.name, true, mergedColumns.size, 50))
        }
        if (columnItem.isCalculated) {
          def.calculationMethod = SimpleSelect(def.id, columnItem.expression, def.propertyClass.javaClass)
        }
      }
    }
    mergedColumns.filter { it.isVisible }.sortedWith{col1, col2 -> columnsOrder(col1, col2) }
      .forEachIndexed { index, column -> column.order = index }
    currentTableColumns.importData(ColumnList.Immutable.fromList(mergedColumns), false)
  }
}

internal data class BtnController(
  val isDisabled: SimpleBooleanProperty = SimpleBooleanProperty(false),
  val onAction: () -> Unit
)
internal enum class PropertyType(private val displayName: String) {
  STRING(RootLocalizer.formatText("text")),
  INTEGER(RootLocalizer.formatText("integer")),
  DATE(RootLocalizer.formatText("date")),
  DECIMAL(RootLocalizer.formatText("double")),
  BOOLEAN(RootLocalizer.formatText("boolean"));

  override fun toString() = this.displayName
}

internal fun CustomPropertyDefinition.getPropertyType(): PropertyType = when (this.propertyClass) {
  CustomPropertyClass.TEXT -> PropertyType.STRING
  CustomPropertyClass.DATE -> PropertyType.DATE
  CustomPropertyClass.INTEGER -> PropertyType.INTEGER
  CustomPropertyClass.DOUBLE -> PropertyType.DECIMAL
  CustomPropertyClass.BOOLEAN -> PropertyType.BOOLEAN
}

internal fun PropertyType.getCustomPropertyClass(): CustomPropertyClass = when (this) {
  PropertyType.STRING -> CustomPropertyClass.TEXT
  PropertyType.INTEGER -> CustomPropertyClass.INTEGER
  PropertyType.DATE -> CustomPropertyClass.DATE
  PropertyType.BOOLEAN -> CustomPropertyClass.BOOLEAN
  PropertyType.DECIMAL -> CustomPropertyClass.DOUBLE
}

internal fun PropertyType.createValidator(): ValueValidator<*> = when (this) {
  PropertyType.INTEGER -> integerValidator
  PropertyType.DECIMAL -> doubleValidator
  PropertyType.DATE -> createStringDateValidator {
    listOf(GanttLanguage.getInstance().shortDateFormat, GanttLanguage.getInstance().mediumDateFormat)
  }
  else -> voidValidator
}

internal fun CustomPropertyDefinition.fromColumnItem(item: ColumnAsListItem) {
  this.name = item.title
  if (item.defaultValue.trim().isNotBlank()) {
    this.defaultValueAsString = item.defaultValue
  }
  this.setPropertyClass(item.type.getCustomPropertyClass())
  if (item.isCalculated) {
    this.calculationMethod = SimpleSelect(this.id, item.expression, this.propertyClass.javaClass)
  } else {
    this.calculationMethod = null
  }
}

internal fun TaskDefaultColumn.getPropertyType(): PropertyType = when (this) {
  TaskDefaultColumn.ID, TaskDefaultColumn.DURATION, TaskDefaultColumn.COMPLETION -> PropertyType.INTEGER
  TaskDefaultColumn.BEGIN_DATE, TaskDefaultColumn.END_DATE -> PropertyType.DATE
  TaskDefaultColumn.COST -> PropertyType.DECIMAL
  else -> PropertyType.STRING
}

/**
 * Editor component shown to the right of the property list.
 */
internal class CustomPropertyEditor(
  customColumnsManager: CustomPropertyManager,
  private val calculationMethodValidator: CalculationMethodValidator,
  private val btnDeleteController: BtnController,
  private val listItems: ObservableList<ColumnAsListItem>,
  private val errorUi: (String?) -> Unit
) {
  internal val propertySheet: PropertySheet = PropertySheet().also {
    it.styleClass.add("custom-column-props")
  }
  internal val propertySheetLabel = Label().also {
    it.styleClass.add("title")
  }
  private var isPropertyChangeIgnored = false
  var selectedItem: ColumnAsListItem? = null
  set(selectedItem) {
    isPropertyChangeIgnored = true
    field = selectedItem
    if (selectedItem != null) {
      editableValue.title = selectedItem.title
      editableValue.type = selectedItem.type
      if (selectedItem.isCustom) {
        propertySheetLabel.text = ourLocalizer.formatText("propertyPane.title.custom")
        propertySheet.isDisable = false
        btnDeleteController.isDisabled.value = false
        editableValue.defaultValue = selectedItem.defaultValue
        editableValue.isCalculated = selectedItem.isCalculated
        editableValue.expression = selectedItem.expression
      } else {
        btnDeleteController.isDisabled.value = true
        propertySheetLabel.text = ourLocalizer.formatText("propertyPane.title.builtin")
        propertySheet.isDisable = true
        editableValue.isCalculated = false
        editableValue.expression = ""
      }
    }
    isPropertyChangeIgnored = false
  }
  private val editableValue = ColumnAsListItem(column = null, isVisible = true, isCustom = true, customColumnsManager = customColumnsManager)
  private val title = BeanProperty(editableValue,
    PropertyDescriptor("title", ColumnAsListItem::class.java).also {
      it.displayName = RootLocalizer.formatText("option.customPropertyDialog.name.label")
    }
  )
  private val type = BeanProperty(editableValue,
    PropertyDescriptor("type", ColumnAsListItem::class.java).also {
      it.displayName = RootLocalizer.formatText("option.taskProperties.customColumn.type.label")
    }
  )
  private val defaultValue = BeanProperty(editableValue,
    PropertyDescriptor("defaultValue", ColumnAsListItem::class.java).also {
      it.displayName = RootLocalizer.formatText("option.customPropertyDialog.defaultValue.label")
    }
  )
  private val isCalculated = BeanProperty(editableValue, PropertyDescriptor("calculated", ColumnAsListItem::class.java).also {
    it.displayName = RootLocalizer.formatText("option.customPropertyDialog.isCalculated.label")
  })
  private val expression = BeanProperty(editableValue, PropertyDescriptor("expression", ColumnAsListItem::class.java).also {
    it.displayName = RootLocalizer.formatText("option.customPropertyDialog.expression.label")
  })
  val props = listOf(title, type, defaultValue, isCalculated, expression)
  private val editors = mutableMapOf<String, PropertyEditor<*>>()

  init {
    val defaultEditor = propertySheet.propertyEditorFactory
    propertySheet.propertyEditorFactory = Callback { item ->
      val propertyName = props.map {it.propertyDescriptor}.find { it.displayName == item.name }?.name
      defaultEditor.call(item).also { propertyEditor ->
        editors[propertyName!!] = propertyEditor
      }
    }
    props.forEach { it.observableValue.get().addListener { _, _, _ -> onPropertyChange() } }
  }

  private fun PropertyEditor<*>.markValid() {
    this.editor.styleClass.remove("validation-error")
    this.editor.effect = null
  }

  private fun PropertyEditor<*>.markInvalid() {
    if (!this.editor.styleClass.contains("validation-error")) {
      this.editor.styleClass.add("validation-error")
      this.editor.effect = InnerShadow(10.0, Color.RED)
    }
  }

  private fun onPropertyChange() {
    if (!isPropertyChangeIgnored) {
      selectedItem?.title = editableValue.title
      selectedItem?.type = editableValue.type

      var errorMessage: String? = null
      editors["expression"]?.let {editor ->
        try {
          editor.editor.isDisable = !editableValue.isCalculated
          if (editableValue.isCalculated && editableValue.expression.isNotBlank()) {
            calculationMethodValidator.validate(
              // Incomplete instance just for validation purposes
              SimpleSelect(
              "", editableValue.expression, editableValue.type.getCustomPropertyClass().javaClass
              )
            )
          }

          editor.markValid()
          selectedItem?.isCalculated = editableValue.isCalculated
          selectedItem?.expression = editableValue.expression
        } catch (ex: ValidationException) {
          editor.markInvalid()
          errorMessage = ex.message ?: ""
        }
      }
      editors["defaultValue"]?.let { editor ->
        try {
          if (editableValue.defaultValue.isNotBlank()) {
            editableValue.type.createValidator().parse(editableValue.defaultValue)
          }

          editor.markValid()
          selectedItem?.defaultValue = editableValue.defaultValue
        } catch (ex: ValidationException) {
          editor.markInvalid()
          errorMessage = ex.message ?: ""
        }
      }
      if (errorMessage != null) {
        errorUi(errorMessage)
      } else {
        errorUi(null)
        listItems[listItems.indexOf(selectedItem)] = selectedItem
      }
    }
  }

  fun focus() {
    editors["title"]?.editor?.requestFocus()
    onPropertyChange()
  }
}

internal class ColumnAsListItem(
  val column: ColumnList.Column?,
  var isVisible: Boolean,
  val isCustom: Boolean,
  val customColumnsManager: CustomPropertyManager
) {
  private val _title = SimpleStringProperty("")
  var title: String
    get() = _title.value
    set(value) { _title.value = value }
  fun titleProperty() = _title

  private val _type = SimpleObjectProperty(PropertyType.STRING)
  var type: PropertyType
    get() = _type.value
    set(value) { _type.value = value }
  fun typeProperty() = _type

  private val _defaultValue = SimpleStringProperty("")
  var defaultValue: String
    get() = _defaultValue.value
    set(value) { _defaultValue.value = value }
  fun defaultValueProperty() = _defaultValue

  private val _isCalculated = SimpleBooleanProperty(false)
  var isCalculated: Boolean
    get() = _isCalculated.value
    set(value) { _isCalculated.value = value }
  fun calculatedProperty() = _isCalculated

  private val _expression = SimpleStringProperty("")
  var expression: String
    get() = _expression.value
    set(value) { _expression.value = value }
  fun expressionProperty() = _expression

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ColumnAsListItem

    if (_title != other._title) return false

    return true
  }

  override fun hashCode(): Int {
    return _title.hashCode()
  }

  init {
    if (column != null) {
      title = column.name
      val customColumn = customColumnsManager.definitions.find { it.id == column?.id }
      type = run {
        if (isCustom) {
          customColumn?.getPropertyType()
        } else {
          TaskDefaultColumn.find(column?.id)?.getPropertyType()
        }
      } ?: PropertyType.STRING
      defaultValue =
        if (!isCustom) ""
        else customColumn?.defaultValueAsString ?: ""
      isCalculated = customColumn?.calculationMethod != null
      expression = customColumn?.calculationMethod?.let {
        when (it) {
          is SimpleSelect -> it.selectExpression
          else -> ""
        }
      } ?: ""
    }
  }
}

private class CellImpl : ListCell<ColumnAsListItem>() {
  private val iconVisible = MaterialIconView(MaterialIcon.VISIBILITY)
  private val iconHidden = MaterialIconView(MaterialIcon.VISIBILITY_OFF)
  private val iconPane = StackPane().also {
    it.onMouseClicked = EventHandler { evt ->
      item.isVisible = !item.isVisible
      updateItem(item, false)
    }
    //it.children.addAll(iconVisible, iconHidden)
  }

  init {
    styleClass.add("column-item-cell")
  }

  override fun updateItem(item: ColumnAsListItem?, empty: Boolean) {
    super.updateItem(item, empty)
    if (item == null || empty) {
      text = ""
      graphic = null
      return
    }
    text = item.title
    if (graphic == null) {
      graphic = iconPane
    }
    if (item.isVisible) {
      styleClass.remove("is-hidden")
      iconPane.children.setAll(iconVisible)
    } else {
      if (!styleClass.contains("is-hidden")) {
        styleClass.add("is-hidden")
        iconPane.children.setAll(iconHidden)
      }
    }
  }
}


enum class ApplyExecutorType { DIRECT, SWING }

fun showResourceColumnManager(
    columnList: ColumnList, customColumnsManager: CustomPropertyManager, undoManager: GPUndoManager,
    projectDatabase: ProjectDatabase,
    applyExecutor: ApplyExecutorType = ApplyExecutorType.DIRECT) {
  val localizer = RootLocalizer.createWithRootKey("resourceTable.columnManager", baseLocalizer = ourLocalizer)
  showColumnManager(columnList, customColumnsManager, undoManager, localizer, projectDatabase, applyExecutor)
}

fun showTaskColumnManager(
    columnList: ColumnList, customColumnsManager: CustomPropertyManager, undoManager: GPUndoManager,
    projectDatabase: ProjectDatabase,
    applyExecutor: ApplyExecutorType = ApplyExecutorType.DIRECT) {
  showColumnManager(columnList, customColumnsManager, undoManager, ourLocalizer, projectDatabase, applyExecutor)
}

private fun showColumnManager(columnList: ColumnList, customColumnsManager: CustomPropertyManager,
                              undoManager: GPUndoManager,
                              localizer: Localizer,
                              projectDatabase: ProjectDatabase,
                              applyExecutor: ApplyExecutorType = ApplyExecutorType.DIRECT) {
  dialog { dlg ->
    dlg.addStyleClass("dlg-column-manager")
    dlg.addStyleSheet("/biz/ganttproject/ganttview/ColumnManager.css")
    dlg.setHeader(
      VBoxBuilder("header").apply {
        addTitle(localizer.create("title")).also { hbox ->
          hbox.alignment = Pos.CENTER_LEFT
          hbox.isFillHeight = true
        }
      }.vbox
    )
    val columnManager = ColumnManager(columnList, customColumnsManager, undoManager, CalculationMethodValidator(projectDatabase))
    dlg.setContent(columnManager.content)
    dlg.setupButton(ButtonType.APPLY) { btn ->
      btn.text = localizer.formatText("apply")
      btn.styleClass.add("btn-attention")
      btn.setOnAction {
        undoManager.undoableEdit(ourLocalizer.formatText("undoableEdit.title")) {
          when (applyExecutor) {
            ApplyExecutorType.DIRECT -> columnManager.applyChanges()
            ApplyExecutorType.SWING -> SwingUtilities.invokeLater { columnManager.applyChanges() }
          }
        }
        dlg.hide()
      }
    }
    dlg.setupButton(ButtonType.CANCEL) { btn ->
      btn.text = localizer.formatText("add")
      ButtonBar.setButtonData(btn, ButtonBar.ButtonData.HELP)
      btn.disableProperty().bind(columnManager.btnAddController.isDisabled)
      btn.setOnAction {
        it.consume()
        columnManager.btnAddController.onAction()
      }
      btn.styleClass.addAll("btn-attention", "secondary")
    }
    dlg.setupButton(ButtonType.CANCEL) { btn ->
      btn.text = localizer.formatText("delete")
      ButtonBar.setButtonData(btn, ButtonBar.ButtonData.HELP_2)
      btn.disableProperty().bind(columnManager.btnDeleteController.isDisabled)
      btn.setOnAction {
        it.consume()
        columnManager.btnDeleteController.onAction()
      }
      btn.styleClass.addAll("btn-regular", "secondary")
    }
  }
}

private fun columnsOrder(col1: ColumnList.Column, col2: ColumnList.Column): Int =
  when {
    col1.isVisible && !col2.isVisible -> -1
    col2.isVisible && !col1.isVisible -> 1
    else -> col1.order - col2.order
  }
private val ourLocalizer = RootLocalizer.createWithRootKey(
  rootKey = "taskTable.columnManager", baseLocalizer = RootLocalizer)
