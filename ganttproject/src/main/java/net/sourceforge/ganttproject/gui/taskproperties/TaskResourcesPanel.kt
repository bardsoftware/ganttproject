/*
Copyright 2003-2026 Dmitry Barashev, BarD Software s.r.o.

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
package net.sourceforge.ganttproject.gui.taskproperties

import biz.ganttproject.app.FXThread
import biz.ganttproject.app.PropertyPane
import biz.ganttproject.app.PropertyPaneBuilderImpl
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.option.ObservableBoolean
import biz.ganttproject.core.option.ObservableMoney
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.cell.CheckBoxTableCell
import javafx.scene.control.cell.ChoiceBoxTableCell
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Region
import javafx.util.Callback
import javafx.util.StringConverter
import javafx.util.converter.DefaultStringConverter
import net.sourceforge.ganttproject.gui.AbstractTableAndActionsComponentFx
import net.sourceforge.ganttproject.gui.TableActionsModel
import net.sourceforge.ganttproject.gui.TableView2TableActionsModel
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.resource.HumanResourceManager
import net.sourceforge.ganttproject.roles.Role
import net.sourceforge.ganttproject.roles.RoleManager
import net.sourceforge.ganttproject.task.CostStub
import net.sourceforge.ganttproject.task.Task
import net.sourceforge.ganttproject.task.TaskMutator
import org.controlsfx.control.tableview2.TableColumn2
import org.controlsfx.control.tableview2.TableView2
import java.math.BigDecimal

/**
 * JavaFX panel for managing task resource assignments.
 */
class TaskResourcesPanel(
  private val task: Task,
  private val hrManager: HumanResourceManager,
  private val roleManager: RoleManager
) {
  private val tableItems: ObservableList<ResourceAssignmentRow> = FXCollections.observableArrayList()
  private val tableView = TableView2<ResourceAssignmentRow>()
  private val model = object : TableView2TableActionsModel<ResourceAssignmentRow>(tableView) {
    private val innerModel = ResourceAssignmentTableModel(task)

    override fun delete(indices: IntArray) {
      innerModel.delete(indices)
      refreshTable()
    }

    override fun onAdd() {
      FXThread.runLater {
        tableView.edit(tableItems.size - 1, tableView.columns[1])
        tableView.selectionModel.select(tableItems.size - 1)
      }
    }

    override fun refreshTable() {
      val currentSelection = selectedRow
      tableItems.clear()
      val assignments = innerModel.assignments
      assignments.forEach { assignment ->
        tableItems.add(ResourceAssignmentRow(assignment))
      }
      // The last row is for adding new assignments
      tableItems.add(ResourceAssignmentRow(null))
      selectedRow = currentSelection
    }

    fun commit() {
      innerModel.commit()
    }

    fun setValueAt(value: Any?, row: Int, col: Int) {
      innerModel.setValueAt(value, row, col)
    }
  }

  private val costIsCalculated = ObservableBoolean("option.taskProperties.cost.calculated.label", task.cost.isCalculated)
  private val costValue = ObservableMoney("option.taskProperties.cost.value", task.cost.value)

  val title: String = i18n.formatText("human")
  private val tableAndActions = AbstractTableAndActionsComponentFx(tableView, model)

  val fxComponent by lazy {
    getFxNode().also {
      model.refreshTable()
    }
  }

  private var selectedIndices = FXCollections.emptyObservableList<Int>()
  private var selectedRow: Int
    set(value) {
      selectRow(value)
    }
    get() = selectedIndices.firstOrNull() ?: -1
  private var selectRow: (Int) -> Unit = {}

  private fun getFxNode(): Node {
    tableView.apply {
      isEditable = true
      items = tableItems
      selectionModel.selectionMode = SelectionMode.SINGLE
      selectedIndices = selectionModel.selectedIndices
      selectRow = { selectionModel.select(it) }

      // ID column
      val idCol = TableColumn2<ResourceAssignmentRow, String>(i18n.formatText("id")).apply {
        setCellValueFactory { SimpleStringProperty(it.value.assignment?.resource?.id?.toString() ?: "") }
        prefWidth = 50.0
        isEditable = false
      }

      // Resource name column with combo box editor
      val nameCol = TableColumn2<ResourceAssignmentRow, HumanResource?>(i18n.formatText("resourcename")).apply {
        setCellValueFactory { SimpleObjectProperty(it.value.assignment?.resource) }
        setCellFactory { ResourceComboTableCell(hrManager) }
        setOnEditCommit { event ->
          val row = event.rowValue
          val newResource = event.newValue
          if (row.assignment == null) {
            if (newResource != null) {
              model.setValueAt(newResource, tableItems.size - 1, 1)
              model.refreshTable()
            }
          } else {
            model.setValueAt(newResource, event.tablePosition.row, 1)
            model.refreshTable()
          }
        }
        isEditable = true
        prefWidth = 200.0
      }

      // Load/Unit column
      val unitCol = TableColumn2<ResourceAssignmentRow, String>(i18n.formatText("unit")).apply {
        setCellValueFactory { SimpleStringProperty(it.value.assignment?.load?.toString() ?: "") }
        setCellFactory { TextFieldTableCell(DefaultStringConverter()) }
        setOnEditCommit { event ->
          try {
            model.setValueAt(event.newValue, event.tablePosition.row, 2)
          } catch (e: NumberFormatException) {
          }
          model.refreshTable()
        }
        isEditable = true
        prefWidth = 80.0
      }

      // Coordinator column
      val coordinatorCol = TableColumn2<ResourceAssignmentRow, Boolean>(i18n.formatText("coordinator")).apply {
        setCellValueFactory { SimpleBooleanProperty(it.value.assignment?.isCoordinator ?: false) }
        cellFactory = CheckBoxTableCell.forTableColumn(this)
        setOnEditCommit { event ->
          model.setValueAt(event.newValue, event.tablePosition.row, 3)
          model.refreshTable()
        }
        isEditable = true
        prefWidth = 100.0
      }

      // Role column
      val roleStringConverter: StringConverter<Role> = object : StringConverter<Role>() {
        override fun toString(role: Role?): String = role?.name ?: ""
        override fun fromString(value: String?): Role? {
          return roleManager.enabledRoles.find { it.name == value }
        }
      }
      val roleCol = TableColumn2<ResourceAssignmentRow, Role>(i18n.formatText("role")).apply {
        setCellValueFactory { SimpleObjectProperty(it.value.assignment?.roleForAssignment) }
        cellFactory = ChoiceBoxTableCell.forTableColumn(
          roleStringConverter,
          FXCollections.observableArrayList(roleManager.enabledRoles.toList())
        )
        setOnEditCommit { event ->
          model.setValueAt(event.newValue, event.tablePosition.row, 4)
          model.refreshTable()
        }
        isEditable = true
        prefWidth = 150.0
      }

      columns.addAll(idCol, nameCol, unitCol, coordinatorCol, roleCol)
    }

    // Create split layout with table and cost panel
//    val mainContent = HBox().apply {
//      spacing = 10.0
//      children.addAll(
//        tableView.apply { HBox.setHgrow(this, Priority.SOMETIMES) },
//        createCostPanel().apply {
//          HBox.setHgrow(this, Priority.SOMETIMES)
//        }
//      )
//    }
    val tableComponent = tableAndActions.fxComponent
    return BorderPane().apply {
      stylesheets.add("/biz/ganttproject/task/TaskPropertiesDialog.css")
      stylesheets.add("/biz/ganttproject/app/tables.css")
      stylesheets.add("/biz/ganttproject/app/buttons.css")
      styleClass.addAll("tab-contents", "pane-task-resources")
      center = tableComponent
      right = createCostPanel()
    }
  }

  private fun createCostPanel(): Region {
    val propertyPane = PropertyPane()
    val builder = PropertyPaneBuilderImpl(i18n, propertyPane)
    val radioUi = builder.createRadioButtonOptionEditor(costIsCalculated, null)
    propertyPane.add(radioUi.yesButton, 0, 1)
    propertyPane.add(radioUi.noButton, 0, 2)
    costIsCalculated.value = task.cost.isCalculated
    costValue.setWritable(!costIsCalculated.value)

    costIsCalculated.addWatcher {
      costValue.setWritable(!costIsCalculated.value)
      costValue.value = task.cost.value
    }

    // Title
    propertyPane.add(Label(i18n.formatText("optionGroup.task.cost.label")).apply {
      styleClass.add("section-title")
    }, 0, 0, 2, 1)

    // Calculated cost radio button
    val calculatedValueLabel = Label(task.cost.calculatedValue.toPlainString())
    propertyPane.add(calculatedValueLabel, 1, 1)
    builder.createMoneyOptionEditor(costValue).also { propertyPane.add(it, 1, 2) }

    return propertyPane
  }

  fun commit(mutator: TaskMutator) {
    model.commit()
    val cost = if (costIsCalculated.value) {
      CostStub(BigDecimal.ZERO, true)
    } else {
      CostStub(costValue.value, false)
    }
    mutator.setCost(cost)
  }

  fun requestFocus() {
    selectedRow = 0
  }
}

// --------------------------------------------------------------------------------------------------------------------
private val i18n = RootLocalizer

// --------------------------------------------------------------------------------------------------------------------

/**
 * Table model for resource assignments.
 */
private class ResourceAssignmentTableModel(task: Task) {
  private val assignmentCollection = task.assignmentCollection
  private val mutator = assignmentCollection.createMutator()
  private val _assignments = assignmentCollection.assignments.toMutableList()

  val assignments: List<net.sourceforge.ganttproject.task.ResourceAssignment>
    get() = _assignments.toList()

  fun setValueAt(value: Any?, row: Int, col: Int) {
    if (row >= _assignments.size) {
      createAssignment(value)
    } else {
      updateAssignment(value, row, col)
    }
  }

  private fun updateAssignment(value: Any?, row: Int, col: Int) {
    val assignment = _assignments[row]
    when (col) {
      4 -> { // Role
        if (value is Role) {
          assignment.roleForAssignment = value
        }
      }
      3 -> { // Coordinator
        if (value is Boolean) {
          assignment.isCoordinator = value
        }
      }
      2 -> { // Load
        try {
          val load = value.toString().toFloat()
          assignment.load = load
        } catch (e: NumberFormatException) {
        }
      }
      1 -> { // Resource
        if (value == null) {
          assignment.delete()
          _assignments.removeAt(row)
        } else if (value is HumanResource) {
          val load = assignment.load
          val coord = assignment.isCoordinator
          assignment.delete()
          mutator.deleteAssignment(assignment.resource)
          val newAssignment = mutator.addAssignment(value)
          newAssignment.load = load
          newAssignment.isCoordinator = coord
          _assignments[row] = newAssignment
        }
      }
    }
  }

  private fun createAssignment(value: Any?) {
    if (value is HumanResource) {
      val newAssignment = mutator.addAssignment(value)
      newAssignment.load = 100f
      newAssignment.isCoordinator = _assignments.isEmpty()
      newAssignment.roleForAssignment = value.role
      _assignments.add(newAssignment)
    }
  }

  fun delete(selectedRows: IntArray) {
    val toDelete = selectedRows.filter { it < _assignments.size }.map { _assignments[it] }
    toDelete.forEach { it.delete() }
    _assignments.removeAll(toDelete)
  }

  fun commit() {
    mutator.commit()
  }
}

/**
 * Row data class for the table.
 */
private class ResourceAssignmentRow(val assignment: net.sourceforge.ganttproject.task.ResourceAssignment?)

/**
 * Custom table cell for resource selection with combo box.
 */
private class ResourceComboTableCell(hrManager: HumanResourceManager) :
  TableCell<ResourceAssignmentRow, HumanResource?>() {

  private val comboBox = ComboBox<HumanResource>()
  private val resources = hrManager.resources.toList()

  init {
    comboBox.items.addAll(resources)
    comboBox.cellFactory = Callback { ResourceListCell() }
    comboBox.buttonCell = ResourceListCell()
    comboBox.maxWidth = Double.MAX_VALUE
    comboBox.setOnAction {
      if (isEditing) {
        commitEdit(comboBox.value)
      }
    }
  }

  override fun startEdit() {
    super.startEdit()
    comboBox.value = item
    graphic = comboBox
    text = null
  }

  override fun cancelEdit() {
    super.cancelEdit()
    graphic = null
    text = item?.name
  }

  override fun updateItem(item: HumanResource?, empty: Boolean) {
    super.updateItem(item, empty)
    if (empty || item == null) {
      text = null
      graphic = null
    } else {
      if (isEditing) {
        graphic = comboBox
        text = null
      } else {
        text = item.name
        graphic = null
      }
    }
  }
}

/**
 * List cell for displaying resources in combo box.
 */
private class ResourceListCell : ListCell<HumanResource>() {
  override fun updateItem(item: HumanResource?, empty: Boolean) {
    super.updateItem(item, empty)
    if (empty || item == null) {
      text = null
      graphic = null
    } else {
      text = item.name
    }
  }
}
