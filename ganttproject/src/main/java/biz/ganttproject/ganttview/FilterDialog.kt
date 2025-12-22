/*
 * Copyright 2024 BarD Software s.r.o., Dmitry Barashev.
 *
 * This file is part of GanttProject, an opensource project management tool.
 *
 * GanttProject is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * GanttProject is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GanttProject.  If not, see <http://www.gnu.org/licenses/>.
 */
package biz.ganttproject.ganttview

import biz.ganttproject.app.MappingLocalizer
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.dialog
import biz.ganttproject.core.option.Completion
import biz.ganttproject.core.option.ObservableObject
import biz.ganttproject.core.option.ObservableString
import biz.ganttproject.core.option.ValidationException
import biz.ganttproject.customproperty.CalculationMethodValidator
import biz.ganttproject.customproperty.CustomPropertyClass
import biz.ganttproject.customproperty.CustomPropertyManager
import biz.ganttproject.customproperty.ExpressionAutoCompletion
import biz.ganttproject.customproperty.SimpleSelect
import javafx.collections.FXCollections
import net.sourceforge.ganttproject.storage.ProjectDatabase

/**
 * Shows a dialog that allows for creating custom task filters.
 */
fun showFilterDialog(
  filterManager: TaskFilterManager,
  customPropertyManager: CustomPropertyManager,
  projectDatabase: ProjectDatabase
) {
  dialog(title = i18n.formatText("title")) { dlg ->
    val listItems = FXCollections.observableArrayList(filterManager.filters)
    val editItem = ObservableObject<TaskFilter?>("", null)
    val expressionCompletion = ExpressionAutoCompletion(customPropertyManager)
    val editorModel = FilterEditorModel(editItem, CalculationMethodValidator(projectDatabase), expressionCompletion::complete)
    val dialogModel = ItemListDialogModel<TaskFilter>(
      listItems,
      newItemFactory = filterManager::createCustomFilter,
      i18n
    )
    dialogModel.btnApplyController.onAction = {
      filterManager.importFilters(listItems)
    }
    val editor = FilterEditor(dialogModel, editorModel, editItem, dialogModel)
    val dialogPane = ItemListDialogPane<TaskFilter>(
      listItems,
      editItem,
      { filter -> ShowHideListItem({filter.getLocalizedTitle()}, {filter.isEnabledProperty.value}, {filter.isEnabledProperty.set(!filter.isEnabledProperty.get())}) },
      dialogModel,
      editor,
      i18n
    )
    dialogPane.build(dlg)
  }
}

/**
 * The editor pane model class.
 */
internal class FilterEditorModel(
  editItem: ObservableObject<TaskFilter?>,
  calculationMethodValidator: CalculationMethodValidator,
  expressionAutoCompletion: (String, Int) -> List<Completion>) {

  val nameField = ObservableString(id="name", "")
  val descriptionField = ObservableString(id="description", "")
  val expressionField = ObservableString(id="expression", initValue = "",
    validator = {
      if (editItem.value?.isBuiltIn == true) {
        ""
      } else {
        if (it.isNotBlank()) {
          calculationMethodValidator.validate(
            // Incomplete instance just for validation purposes
            SimpleSelect("", "num", whereExpression = it, CustomPropertyClass.INTEGER.javaClass)
          )
          it
        } else {
          throw ValidationException(i18n.formatText("expression.validation.empty"))
        }
      }
    }
  ).also {
    it.completions = expressionAutoCompletion
  }

  val fields = listOf(nameField, descriptionField, expressionField)
}

/**
 * Editor pane for editing the selected filter properties.
 */
internal class FilterEditor(
  private val dialogModel: ItemListDialogModel<TaskFilter>,
  private val editorModel: FilterEditorModel,
  editItem: ObservableObject<TaskFilter?>,
  model: ItemListDialogModel<TaskFilter>
)
  : ItemEditorPaneImpl<TaskFilter>(
  editorModel.fields, editItem, model, i18n
) {
  override fun loadData(item: TaskFilter?) {
    if (item != null) {
      editorModel.nameField.set(item.getLocalizedTitle())
      editorModel.descriptionField.set(item.getLocalizedDescription())
      editorModel.expressionField.set(item.expression)
      propertySheet.isDisable = item.isBuiltIn
      dialogModel.btnDeleteController.isDisabled.set(item.isBuiltIn)
      visibilityToggle.isSelected = item.isEnabledProperty.get()
    } else {
      editorModel.nameField.set("")
      editorModel.descriptionField.set("")
      editorModel.expressionField.set("")
      propertySheet.isDisable = true
      dialogModel.btnDeleteController.isDisabled.set(true)
    }
  }

  override fun saveData(item: TaskFilter) {
    item.title = editorModel.nameField.value ?: ""
    item.description = editorModel.descriptionField.value ?: ""
    item.expression = editorModel.expressionField.value ?: ""
    item.isEnabledProperty.set(visibilityToggle.isSelected)
  }
}

private val i18n = run {
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
  val fallback2 = RootLocalizer.createWithRootKey("taskTable.filterDialog", fallback1)
  RootLocalizer.createWithRootKey("", fallback2)
}
