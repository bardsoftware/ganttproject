/*
Copyright 2026

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
package biz.ganttproject.ganttview

import biz.ganttproject.app.DefaultLocalizer
import biz.ganttproject.app.Localizer
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.app.dialog
import biz.ganttproject.core.option.ObservableObject
import biz.ganttproject.core.option.ObservableString
import javafx.collections.FXCollections

/**
 * Manage named views: rename, create, delete, and tick the one that is in force.
 *
 * BUILT ON THE EXISTING DIALOG, not next to it. ItemListDialogModel and ItemListDialogPane are
 * generic over `interface Item<T> { var title; val isEnabledProperty }` (SharedUiComponents.kt:217),
 * and [NamedTaskView] fits that without a single change to those classes. List, name field, New, Delete
 * and the visibility toggle come for free.
 *
 * WHY NOT IN THE SETTINGS DIALOG. That one is opened with `dialog(id = "settings")`, which restores
 * a stored size (Dialog.kt:108-114) and has been measured to be wider than 1024 px, so its Ok
 * button ends up off the screen. This dialog is opened WITHOUT an id, the same way the filter
 * dialog is, and is centred afresh every time.
 */
fun showViewDialog(viewManager: TaskViewManager) {
  dialog(title = i18n.formatText("title")) { dlg ->
    // A copy: pressing Apply takes the list over, closing the dialog throws it away.
    val listItems = FXCollections.observableArrayList(viewManager.views)
    val editItem = ObservableObject<NamedTaskView?>("", null)
    val editorModel = ViewEditorModel()
    val dialogModel = ItemListDialogModel<NamedTaskView>(
      listItems,
      newItemFactory = { viewManager.createView() },
      i18n
    )
    dialogModel.btnApplyController.onAction = {
      viewManager.importViews(listItems)
    }
    val editor = ViewEditor(dialogModel, editorModel, editItem, dialogModel)
    val dialogPane = ItemListDialogPane<NamedTaskView>(
      listItems,
      editItem,
      { view -> ShowHideListItem(
        { view.title },
        { view.isEnabledProperty.value },
        { view.isEnabledProperty.set(!view.isEnabledProperty.get()) }
      ) },
      dialogModel,
      editor,
      i18n
    )
    dialogPane.build(dlg)
  }
}

/**
 * The fields on the right-hand side. Simpler than the filter editor: a view carries no expression
 * that would have to be checked, only a name.
 *
 * The number of hidden tasks is shown and cannot be edited. WHICH tasks are hidden is decided in
 * the task table, where one can see them -- a list of 32-digit identifiers in a dialog would be
 * unreadable and would tell nobody anything.
 */
internal class ViewEditorModel {
  val nameField = ObservableString(id = "name", "")
  val hiddenCountField = ObservableString(id = "hiddenCount", "").also { it.setWritable(false) }

  val fields = listOf(nameField, hiddenCountField)
}

internal class ViewEditor(
  private val dialogModel: ItemListDialogModel<NamedTaskView>,
  private val editorModel: ViewEditorModel,
  editItem: ObservableObject<NamedTaskView?>,
  model: ItemListDialogModel<NamedTaskView>
) : ItemEditorPaneImpl<NamedTaskView>(editorModel.fields, editItem, model, i18n) {

  override fun loadData(item: NamedTaskView?) {
    if (item != null) {
      editorModel.nameField.set(item.title)
      editorModel.hiddenCountField.set(item.hiddenCount.toString())
      propertySheet.isDisable = false
      dialogModel.btnDeleteController.isDisabled.set(false)
      visibilityToggle.isSelected = item.isEnabledProperty.get()
    } else {
      editorModel.nameField.set("")
      editorModel.hiddenCountField.set("")
      propertySheet.isDisable = true
      dialogModel.btnDeleteController.isDisabled.set(true)
    }
  }

  override fun saveData(item: NamedTaskView) {
    item.title = editorModel.nameField.value ?: ""
    item.isEnabledProperty.set(visibilityToggle.isSelected)
  }
}

/**
 * Only the dialog's own keys, `taskViewDialog.*`, then GanttProject's common ones such as "add",
 * "delete" and "apply". The filter dialog's keys are NOT asked: as long as the view keys are not in
 * the translation bundle, they would answer "Task Filters", "New Filter" and "Enable filter", and
 * the dialog would pass itself off as the filter dialog.
 */
internal fun viewDialogLocalizer(root: DefaultLocalizer = RootLocalizer): Localizer =
  root.createWithRootKey("taskViewDialog", root)

private val i18n: Localizer = viewDialogLocalizer()
