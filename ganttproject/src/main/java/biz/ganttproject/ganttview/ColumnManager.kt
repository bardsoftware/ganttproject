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

import biz.ganttproject.app.*
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.option.*
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.customproperty.*
import biz.ganttproject.core.option.Completion
import biz.ganttproject.lib.fx.VBoxBuilder
import biz.ganttproject.lib.fx.vbox
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import de.jensd.fx.glyphs.materialicons.MaterialIconView
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.util.Callback
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.undo.GPUndoManager
import javax.swing.SwingUtilities

/**
 * @author dbarashev@bardsoftware.com
 */
class ColumnManager(
  // The list of columns shown in the task table
  private val currentTableColumns: ColumnList,
  private val customColumnsManager: CustomPropertyManager,
  calculationMethodValidator: CalculationMethodValidator,
  expressionAutoCompletion: (String, Int) -> List<Completion>,
  private val applyExecutor: ApplyExecutorType
) {

  internal val btnAddController = BtnController(onAction = this::onAddColumn)
  internal val btnDeleteController = BtnController(onAction = this::onDeleteColumn)
  internal val btnApplyController = BtnController(onAction = this::onApply)
  internal val escCloseEnabled = SimpleBooleanProperty(true)

  private val listItems = FXCollections.observableArrayList<ColumnAsListItem>()
  private val listView: ListView<ColumnAsListItem> = ListView()
  private val errorLabel = Label().also {
    it.styleClass.addAll("hint", "hint-validation")
  }
  private val errorPane = HBox().also {
    it.styleClass.addAll("hint-validation-pane", "noerror")
    it.children.add(errorLabel)
  }

  private val customPropertyEditor = CustomPropertyEditor(calculationMethodValidator, expressionAutoCompletion, btnDeleteController, escCloseEnabled, listItems,
    errorUi = {
      if (it == null) {
        errorPane.isVisible = false
        if (!errorPane.styleClass.contains("noerror")) {
          errorPane.styleClass.add("noerror")
        }
        errorLabel.text = ""
        btnApplyController.isDisabled.value = false
      }
      else {
        errorLabel.text = it
        errorPane.isVisible = true
        errorPane.styleClass.remove("noerror")
        btnApplyController.isDisabled.value = true
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
    val propertySheetBox = vbox {
      addClasses("property-sheet-box")
      add(customPropertyEditor.propertySheetLabel, Pos.CENTER_LEFT, Priority.NEVER)
      add(customPropertyEditor.propertySheet.node, Pos.CENTER, Priority.ALWAYS)
      add(errorPane)
    }
    content = HBox().also {
      it.styleClass.add("content-pane")
      it.children.addAll(listView, propertySheetBox)
      HBox.setHgrow(propertySheetBox, Priority.ALWAYS)
    }

    listView.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
      customPropertyEditor.selectedItem = newValue.clone()
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

  private fun onApply() {
    when (applyExecutor) {
      ApplyExecutorType.DIRECT -> applyChanges()
      ApplyExecutorType.SWING -> SwingUtilities.invokeLater { applyChanges() }
    }
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
          customColumnsManager.definitions.find { def -> def.id == it.id }?.importColumnItem(columnItem)
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
  STRING("text"),
  INTEGER("integer"),
  DATE("date"),
  DECIMAL("double"),
  BOOLEAN("boolean");

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

internal fun CustomPropertyDefinition.importColumnItem(item: ColumnAsListItem) {
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
  private val calculationMethodValidator: CalculationMethodValidator,
  private val expressionAutoCompletion: (String, Int) -> List<Completion>,
  private val btnDeleteController: BtnController,
  private val escCloseEnabled: BooleanProperty,
  private val listItems: ObservableList<ColumnAsListItem>,
  private val errorUi: (String?) -> Unit
) {
  private val localizer = run {
    val fallback1 = MappingLocalizer(mapOf()) {
      if (it.endsWith(".label")) {
        val key = it.split('.', limit = 2)[0]
        RootLocalizer.formatText(key)
      } else {
        null
      }
    }
    val fallback2 = RootLocalizer.createWithRootKey("option.taskProperties.customColumn", fallback1)
    RootLocalizer.createWithRootKey("option.customPropertyDialog", fallback2)
  }
  private val nameOption = ObservableString(id = "name")
  private val typeOption = ObservableEnum(id ="type", initValue = PropertyType.STRING, allValues = PropertyType.values())
  private val defaultValueOption = ObservableString(
    id = "defaultValue",
    initValue = "",
    validator = {
      if (it.isNotBlank()) {
        (typeOption.value.createValidator().parse(it) ?: it).toString()
      } else it
    })

  private val isCalculatedOption: ObservableBoolean = ObservableBoolean(id = "isCalculated").also {
    it.addWatcher { evt -> expressionOption.setWritable(evt.newValue) }
  }

  private val expressionOption = ObservableString(id ="expression", initValue = "",
    validator = {
      if (isCalculatedOption.value && it.isNotBlank()) {
        calculationMethodValidator.validate(
          // Incomplete instance just for validation purposes
          SimpleSelect("", it, typeOption.value.getCustomPropertyClass().javaClass)
        )
      }
      it
    }
  ).also {
    it.completions = expressionAutoCompletion
  }

  private val allOptions = listOf(nameOption, typeOption, defaultValueOption, isCalculatedOption, expressionOption)

  internal val propertySheet = PropertySheetBuilder(localizer).createPropertySheet(allOptions).also {
    escCloseEnabled.bind(it.isEscCloseEnabled)
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
      nameOption.set(selectedItem.title)
      typeOption.set(selectedItem.type)
      defaultValueOption.set(selectedItem.defaultValue)

      if (selectedItem.isCustom) {
        propertySheetLabel.text = ourLocalizer.formatText("propertyPane.title.custom")
        propertySheet.isDisable = false
        btnDeleteController.isDisabled.value = false
        isCalculatedOption.set(selectedItem.isCalculated)
        expressionOption.set(selectedItem.expression)
      } else {
        btnDeleteController.isDisabled.value = true
        propertySheetLabel.text = ourLocalizer.formatText("propertyPane.title.builtin")
        propertySheet.isDisable = true
        isCalculatedOption.set(false)
        expressionOption.set("")
      }
    }
    isPropertyChangeIgnored = false
  }

  init {
    allOptions.forEach { it.addWatcher { onEdit() } }
    propertySheet.validationErrors.addListener(MapChangeListener {
      if (propertySheet.validationErrors.isEmpty()) {
        errorUi(null)
        listItems[listItems.indexOf(selectedItem)] = selectedItem
      } else {
        errorUi(propertySheet.validationErrors.values.joinToString(separator = "\n"))
      }
    })
  }

  private fun onEdit() {
    if (!isPropertyChangeIgnored) {
      selectedItem?.let {selected ->
        selected.title = nameOption.value ?: ""
        selected.type = typeOption.value
        selected.defaultValue = defaultValueOption.value ?: ""
        selected.isCalculated = isCalculatedOption.value
        selected.expression = expressionOption.value ?: ""
        listItems.replaceAll { if (it.title == selected.cloneOf?.title) { selected } else { it } }
        selectedItem = selected.clone()
      }
    }
  }

  fun focus() {
    propertySheet.requestFocus()
    //editors["title"]?.editor?.requestFocus()
    onEdit()
  }
}

internal class ColumnAsListItem(
  val column: ColumnList.Column?,
  var isVisible: Boolean,
  val isCustom: Boolean,
  val customColumnsManager: CustomPropertyManager
) {
  constructor(cloneOf: ColumnAsListItem): this(cloneOf.column, cloneOf.isVisible, cloneOf.isCustom, cloneOf.customColumnsManager) {
    this.cloneOf = cloneOf
    this.title = cloneOf.title
    this.type = cloneOf.type
    this.defaultValue = cloneOf.defaultValue
    this.isCalculated = cloneOf.isCalculated
    this.expression = cloneOf.expression
  }
  internal var cloneOf: ColumnAsListItem? = null

  var title: String = ""

  var type: PropertyType = PropertyType.STRING

  var defaultValue: String = ""

  var isCalculated: Boolean = false

  var expression: String = ""


  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ColumnAsListItem

    if (title != other.title) return false

    return true
  }

  override fun hashCode(): Int {
    return title.hashCode()
  }

  override fun toString(): String {
    return "ColumnAsListItem(title='$title')"
  }

  fun clone(): ColumnAsListItem = ColumnAsListItem(this)

  init {
    if (column != null) {
      title = column.name
      val customColumn = customColumnsManager.definitions.find { it.id == column.id }
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
        }
      } ?: ""
    }
  }
}

private class CellImpl : ListCell<ColumnAsListItem>() {
  private val iconVisible = MaterialIconView(MaterialIcon.VISIBILITY)
  private val iconHidden = MaterialIconView(MaterialIcon.VISIBILITY_OFF)
  private val iconPane = StackPane().also {
    it.onMouseClicked = EventHandler { _ ->
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
    val columnManager = ColumnManager(
      columnList, customColumnsManager, CalculationMethodValidator(projectDatabase), ExpressionAutoCompletion()::complete, applyExecutor
    )
    columnManager.escCloseEnabled.addListener { _, _, newValue -> dlg.setEscCloseEnabled(newValue) }
    dlg.setContent(columnManager.content)
    dlg.setupButton(ButtonType.APPLY) { btn ->
      btn.text = localizer.formatText("apply")
      btn.styleClass.add("btn-attention")
      btn.disableProperty().bind(columnManager.btnApplyController.isDisabled)
      btn.setOnAction {
        undoManager.undoableEdit(ourLocalizer.formatText("undoableEdit.title")) {
          columnManager.btnApplyController.onAction()
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
