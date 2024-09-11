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
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableList
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

  internal val escCloseEnabled = SimpleBooleanProperty(true)

  private val listItems = FXCollections.observableArrayList<ColumnAsListItem>()
  private val selectedItem = ObservableObject<ColumnAsListItem?>("", null)

  internal val dialogModel: ItemListDialogModel<ColumnAsListItem> = ItemListDialogModel<ColumnAsListItem>(
    listItems,
    newItemFactory = {
      ColumnAsListItem(null, isVisible = true, isCustom = true, customColumnsManager, {customPropertyEditor.updateVisibility(it)})
    },
    selection = {
      dialogPane.listView.selectionModel.selectedItems
    }
  )

  private val customPropertyEditor: CustomPropertyEditor = CustomPropertyEditor(
    selectedItemProperty = selectedItem,
    btnDeleteController = dialogModel.btnDeleteController,
    escCloseEnabled = escCloseEnabled,
    listItems = listItems,
    model = EditorModel(
      calculationMethodValidator = calculationMethodValidator,
      expressionAutoCompletion = expressionAutoCompletion,
      nameClash = { tryName ->
        listItems.find { it.title == tryName && it != selectedItem.value?.cloneOf } != null
      },
      localizer = ourEditorLocalizer
    ),
    errorUi = { dialogPane.onError(it) })

  private val mergedColumns: MutableList<ColumnList.Column> = mutableListOf()
  internal val dialogPane = ItemListDialogPane<ColumnAsListItem>(
    listItems = listItems,
    selectedItem = selectedItem,
    listItemConverter = { ShowHideListItem(it.title, it.isVisibleProperty) },
    dialogModel = dialogModel,
    editor = customPropertyEditor,
    localizer = ourEditorLocalizer
  )
  init {
    mergedColumns.addAll(currentTableColumns.exportData())
    // First go columns which are shown in the table now
    // and they are ordered the same way are shown in the table.
    listItems.setAll(mergedColumns.sortedWith { col1, col2 -> columnsOrder(col1, col2) }.map { col ->
      val isCustom = customColumnsManager.definitions.find { it.id == col.id } != null
      ColumnAsListItem(col, col.isVisible, isCustom, customColumnsManager, customPropertyEditor::updateVisibility)
    })
    customColumnsManager.definitions.forEach { def ->
      if (mergedColumns.find { it.id == def.id } == null) {
        val columnStub = ColumnList.ColumnStub(def.id, def.name, false, -1, -1)
        mergedColumns.add(columnStub)
        listItems.add(ColumnAsListItem(columnStub, columnStub.isVisible, true, customColumnsManager, customPropertyEditor::updateVisibility))
      }
    }

  }

  internal fun onApply() {
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
  this.propertyClass = item.type.getCustomPropertyClass()
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
 * This the model of the property editor. It encapsulates the values that are being edited and their validators.
 */
internal class EditorModel(
  private val calculationMethodValidator: CalculationMethodValidator,
  private val expressionAutoCompletion: (String, Int) -> List<Completion>,
  private val nameClash: (String)-> Boolean,
  private val localizer: Localizer,
) {
  val nameOption = ObservableString(id = "name", validator = { value ->
    if (nameClash(value)) {
      throw ValidationException(localizer.formatText("columnExists", value))
    }
    if (value.isBlank()) {
      throw ValidationException(localizer.formatText("name.validation.empty"))
    }
    value
  })
  val typeOption = ObservableEnum(id ="type", initValue = PropertyType.STRING, allValues = PropertyType.values())
  val defaultValueOption = ObservableString(
    id = "defaultValue",
    initValue = "",
    validator = {
      if (it.isNotBlank()) {
        (typeOption.value.createValidator().parse(it) ?: it).toString()
      } else it
    })

  val isCalculatedOption: ObservableBoolean = ObservableBoolean(id = "isCalculated").also {
    it.addWatcher { evt -> expressionOption.setWritable(evt.newValue) }
  }

  val expressionOption = ObservableString(id ="expression", initValue = "",
    validator = {
      if (!isCalculatedOption.value) {
        ""
      } else {
        if (it.isNotBlank()) {
          calculationMethodValidator.validate(
            // Incomplete instance just for validation purposes
            SimpleSelect("", it, typeOption.value.getCustomPropertyClass().javaClass)
          )
          it
        } else {
          throw ValidationException(localizer.formatText("expression.validation.empty"))
        }
      }
    }
  ).also {
    it.completions = expressionAutoCompletion
  }

  val allOptions = listOf(nameOption, typeOption, defaultValueOption, isCalculatedOption, expressionOption)
}
/**
 * Editor component shown to the right of the property list.
 */
internal class CustomPropertyEditor(
  private val model: EditorModel,
  private val selectedItemProperty: ObservableProperty<ColumnAsListItem?>,
  private val btnDeleteController: BtnController<Unit>,
  escCloseEnabled: BooleanProperty,
  private val listItems: ObservableList<ColumnAsListItem>,
  private val errorUi: (String?) -> Unit
) : ItemEditorPane<ColumnAsListItem?>(selectedItemProperty, model.allOptions, ourEditorLocalizer) {

  init {
    escCloseEnabled.bind(propertySheet.isEscCloseEnabled)
    selectedItemProperty.addWatcher {
      if (it.trigger != this) {
        selectedItem = it.newValue
      }
    }
  }

  private var isPropertyChangeIgnored = false
  private var selectedItem: ColumnAsListItem?
    get() = selectedItemProperty.value
  set(value) {
    isPropertyChangeIgnored = true
    if (value != null) {
      model.nameOption.set(value.title)
      model.typeOption.set(value.type)
      model.defaultValueOption.set(value.defaultValue)
      visibilityToggle.isSelected = value.isVisible

      if (value.isCustom) {
        propertySheetLabel.text = ourLocalizer.formatText("propertyPane.title.custom")
        propertySheet.isDisable = false
        btnDeleteController.isDisabled.value = false
        model.isCalculatedOption.set(value.isCalculated)
        model.expressionOption.set(value.expression)
      } else {
        btnDeleteController.isDisabled.value = true
        propertySheetLabel.text = ourLocalizer.formatText("propertyPane.title.builtin")
        propertySheet.isDisable = true
        model.isCalculatedOption.set(false)
        model.expressionOption.set("")
      }
    }
    isPropertyChangeIgnored = false
  }

  init {
    model.allOptions.forEach { it.addWatcher { onEdit() } }

    visibilityToggle.selectedProperty().addListener { _, _, _ ->
      onEdit()
    }
    propertySheet.validationErrors.addListener(MapChangeListener {
      if (propertySheet.validationErrors.isEmpty()) {
        errorUi(null)
        listItems.replaceAll { if (it != selectedItem?.cloneOf) it else selectedItem }
      } else {
        errorUi(propertySheet.validationErrors.values.joinToString(separator = "\n"))
      }
    })
  }

  internal fun updateVisibility(item: ColumnAsListItem) {
    selectedItem?.let {
      if (it.column?.id == item.column?.id) {
        if (it.isVisible != item.isVisible) {
          it.isVisible = item.isVisible
          visibilityToggle.isSelected = item.isVisible
        }
      }
    }

  }
  override fun onEdit() {
    if (!isPropertyChangeIgnored) {
      selectedItem?.clone()?.let {selected ->
        selected.isVisible = visibilityToggle.isSelected
        selected.title = model.nameOption.value ?: ""
        selected.type = model.typeOption.value
        selected.defaultValue = model.defaultValueOption.value ?: ""
        selected.isCalculated = model.isCalculatedOption.value
        selected.expression = model.expressionOption.value ?: ""
        selectedItemProperty.set(selected, trigger = this)
      }
    }
  }

}

/**
 * Objects stored in the list view on the left side.
 */
internal class ColumnAsListItem(
  val column: ColumnList.Column?,
  isVisible: Boolean,
  val isCustom: Boolean,
  val customColumnsManager: CustomPropertyManager,
  val changeListener: (ColumnAsListItem)->Unit,
  override val cloneOf: ColumnAsListItem? = null
): Item<ColumnAsListItem> {

  constructor(cloneOf: ColumnAsListItem): this(
    cloneOf.column, cloneOf.isVisible, cloneOf.isCustom, cloneOf.customColumnsManager,
    cloneOf.changeListener, cloneOf) {

    this.title = cloneOf.title
    this.type = cloneOf.type
    this.defaultValue = cloneOf.defaultValue
    this.isCalculated = cloneOf.isCalculated
    this.expression = cloneOf.expression
  }

  val isVisibleProperty = ObservableBoolean("", isVisible).also {
    it.addWatcher { changeListener(this@ColumnAsListItem) }
  }

  var isVisible: Boolean
    set(value) {
      if (isVisibleProperty.value != value) {
        isVisibleProperty.set(value)
        changeListener(this)
      }
    }
    get() = isVisibleProperty.value

  override var title: String = ""

  var type: PropertyType = PropertyType.STRING

  var defaultValue: String = ""

  var isCalculated: Boolean = false

  var expression: String = ""


  override fun toString(): String {
    return "ColumnAsListItem(title='$title')"
  }

  override fun clone(): ColumnAsListItem = ColumnAsListItem(this)

  init {
    this.isVisible = isVisible
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
          else -> null
        }
      } ?: ""
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
  dialog(title = RootLocalizer.formatText("customColumns"), id = "customColumns") { dlg ->
    val columnManager = ColumnManager(
      columnList, customColumnsManager, CalculationMethodValidator(projectDatabase), ExpressionAutoCompletion()::complete, applyExecutor
    )
    columnManager.escCloseEnabled.addListener { _, _, newValue -> dlg.setEscCloseEnabled(newValue) }
    columnManager.dialogPane.build(dlg)

    columnManager.dialogModel.btnApplyController.onAction = {
      undoManager.undoableEdit(ourLocalizer.formatText("undoableEdit.title")) {
        columnManager.onApply()
      }
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
internal val ourEditorLocalizer = run {
  val fallback1 = MappingLocalizer(mapOf()) {
    when {
      it.endsWith(".label") -> {
        val key = it.split('.', limit = 2)[0]
        RootLocalizer.create(key)
      }
      it == "columnExists" -> RootLocalizer.create(it)
      else -> null
    }
  }
  val fallback2 = RootLocalizer.createWithRootKey("option.taskProperties.customColumn", fallback1)
  val fallback3 = RootLocalizer.createWithRootKey("option.customPropertyDialog", fallback2)
  RootLocalizer.createWithRootKey("", fallback3)
}
