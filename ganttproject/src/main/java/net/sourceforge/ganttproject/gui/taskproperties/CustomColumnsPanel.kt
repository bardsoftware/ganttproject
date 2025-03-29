/*
Copyright 2003-2024 Dmitry Barashev, BarD Software s.r.o.

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

import biz.ganttproject.FXUtil
import biz.ganttproject.app.RootLocalizer
import biz.ganttproject.core.table.ColumnList
import biz.ganttproject.createButton
import biz.ganttproject.customproperty.CustomColumnsException
import biz.ganttproject.customproperty.CustomPropertyClass
import biz.ganttproject.customproperty.CustomPropertyDefinition
import biz.ganttproject.customproperty.CustomPropertyEvent
import biz.ganttproject.customproperty.CustomPropertyHolder
import biz.ganttproject.customproperty.CustomPropertyListener
import biz.ganttproject.customproperty.CustomPropertyManager
import biz.ganttproject.ganttview.ApplyExecutorType
import biz.ganttproject.ganttview.showResourceColumnManager
import biz.ganttproject.ganttview.showTaskColumnManager
import biz.ganttproject.lib.fx.vbox
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.embed.swing.JFXPanel
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Label
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableColumn.CellDataFeatures
import javafx.scene.control.cell.TextFieldTableCell
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.util.Callback
import javafx.util.converter.DefaultStringConverter
import net.sourceforge.ganttproject.action.GPAction
import net.sourceforge.ganttproject.language.GanttLanguage
import net.sourceforge.ganttproject.storage.ProjectDatabase
import net.sourceforge.ganttproject.task.TaskMutator
import net.sourceforge.ganttproject.undo.GPUndoManager
import org.controlsfx.control.tableview2.TableColumn2
import org.controlsfx.control.tableview2.TableView2
import javax.swing.JComponent

/**
 * This class implements a UI component for editing custom properties.
 *
 * @author dbarashev (Dmitry Barashev)
 */
class CustomColumnsPanel(
  private val manager: CustomPropertyManager,
  private val myProjectDatabase: ProjectDatabase,
  private val myType: Type,
  private val myUndoManager: GPUndoManager,
  customPropertyHolder: CustomPropertyHolder,
  tableHeaderFacade: ColumnList
) {

  enum class Type {
    TASK, RESOURCE
  }

  private val myHolder: CustomPropertyHolder = customPropertyHolder
  private val tableItems: ObservableList<TableViewRow> = FXCollections.observableArrayList()
  private val customPropertyListener = object : CustomPropertyListener {
    override fun customPropertyChange(event: CustomPropertyEvent) {
      createTableRows()
    }
  }.also(manager::addListener)

  private val myTableHeaderFacade: ColumnList = tableHeaderFacade

  private val actionShowManager = GPAction.create("columns.manage.label") {
      when (myType) {
        Type.TASK -> showTaskColumnManager(
          myTableHeaderFacade,
          manager,
          myUndoManager,
          myProjectDatabase,
          ApplyExecutorType.DIRECT
        )

        Type.RESOURCE -> showResourceColumnManager(
          myTableHeaderFacade,
          manager,
          myUndoManager,
          myProjectDatabase,
          ApplyExecutorType.SWING
        )
      }
  }.also {
    it.putValue(GPAction.TEXT_DISPLAY, ContentDisplay.TEXT_ONLY)
  }

  private fun createTableRows() {
    tableItems.clear()
    tableItems.addAll(
      manager.definitions.map { def ->
        val currentValue = myHolder.customProperties.find { it.definition == def }?.valueAsString
        TableViewRow(
          SimpleStringProperty(def.name),
          SimpleStringProperty(currentValue ?: ""),
          SimpleBooleanProperty(def.calculationMethod != null),
          def
        )
      }.sortedWith { o1, o2 ->
        var result: Int = o1.def.isCalculated().compareTo(o2.def.isCalculated())
        if (result == 0) {
          result = o1.def.name.compareTo(o2.def.name)
        }
        result
      }
    )
  }

  val title = RootLocalizer.formatText("customColumns")
  fun getFxNode() = BorderPane().apply {
    createTableRows()
    stylesheets.add("/biz/ganttproject/task/TaskPropertiesDialog.css")
    stylesheets.add("/biz/ganttproject/app/tables.css")
    stylesheets.add("/biz/ganttproject/app/buttons.css")
    styleClass.addAll("tab-contents", "pane-custom-columns")
    this.top = vbox {
      add(HBox().also {
        it.children.add(createButton(actionShowManager, onlyIcon = false).also { btn ->
          btn.styleClass.addAll("btn", "btn-regular", "secondary", "small")
        })
      })
      add(HBox().also {
        it.styleClass.add("medskip")
      })
    }
    this.center = TableView2<TableViewRow>().also { table ->
      table.placeholder = vbox {
        addClasses("placeholder")
        add(Pane(), Pos.CENTER, Priority.ALWAYS)
        add(Label().also {
          it.text = RootLocalizer.formatText("option.taskProperties.customColumn.placeholder.label")
        }, Pos.CENTER)
        add(createButton(actionShowManager, onlyIcon = false).also { btn ->
          btn.styleClass.addAll("btn", "btn-attention", "secondary")
        }, Pos.CENTER)
        add(Pane(), Pos.CENTER, Priority.ALWAYS)
      }
      table.isEditable = true
      val nameColumn = TableColumn2<TableViewRow, String>(GanttLanguage.getInstance().getText("name")).also {
        it.cellValueFactory = object : Callback<CellDataFeatures<TableViewRow, String>, ObservableValue<String>> {
          override fun call(param: CellDataFeatures<TableViewRow, String>): ObservableValue<String> {
            return param.value.name
          }
        }
        it.cellFactory = createCellFactory {
          false to (it.def.calculationMethod != null)
        }
        it.isResizable = true
      }
      val valueColumn = TableColumn2<TableViewRow, String>(GanttLanguage.getInstance().getText("value")).also {
        it.cellValueFactory = object : Callback<CellDataFeatures<TableViewRow, String>, ObservableValue<String>> {
          override fun call(param: CellDataFeatures<TableViewRow, String>): ObservableValue<String> {
            return param.value.value
          }
        }
        it.cellFactory = createCellFactory {
          (it.def.calculationMethod != null).let { !it to it }
        }

        it.isResizable = true
        it.isEditable = true
      }

      valueColumn.onEditCommit = EventHandler { evt ->
        val tableViewRow = evt.tableView.items.get(evt.tablePosition.row)
        tableViewRow.value.set(evt.newValue)
      }

      table.columns.setAll(listOf(nameColumn, valueColumn))
      table.selectionModel.selectionMode = SelectionMode.SINGLE
      table.items = tableItems
      table.widthProperty().addListener { _, oldValue, newValue ->
        if (oldValue == 0.0 && newValue != 0.0) {
          nameColumn.prefWidth = newValue.toDouble()/2.0
          valueColumn.prefWidth= newValue.toDouble()/2.0
        }
      }
    }
  }

  private fun createCellFactory(isEditable: (TableViewRow)-> Pair<Boolean, Boolean>): ValueCellFactory {
    return object : ValueCellFactory {
      override fun call(param: TableColumn<TableViewRow, String>?): TableCell<TableViewRow, String>? {
        val result = object : TextFieldTableCell<TableViewRow, String>(DefaultStringConverter()) {
          override fun updateItem(item: String?, empty: Boolean) {
            super.updateItem(item, empty)
            val isEditable = this.tableRow?.item?.let(isEditable) ?: (false to false)
            this.isEditable = isEditable.first
            this.isDisable = isEditable.second
          }
        }
        return result
      }
    }
  }

  fun getComponent(): JComponent {
    return JFXPanel().also { jfxPanel ->
      FXUtil.run {
        jfxPanel.scene = Scene(getFxNode())
      }
    }
  }

  fun save(mutator: TaskMutator) {
//    CommonPanel.saveColumnWidths(myTable, ourColumnWidth)
    manager.removeListener(customPropertyListener)
    tableItems.forEach {
      if (it.def.calculationMethod == null) {
        myHolder.addCustomProperty(it.def, it.value.get())
      }
    }

    try {
      mutator.setCustomProperties(myHolder)
    } catch (e: CustomColumnsException) {
      throw RuntimeException(e)
    }
  }

  fun cancel() {
    manager.removeListener(customPropertyListener)
  }
}

typealias ValueCellFactory = Callback<TableColumn<TableViewRow, String>, TableCell<TableViewRow, String>>
data class TableViewRow(var name: StringProperty, var value: StringProperty, var isCalculated: BooleanProperty, val def: CustomPropertyDefinition)
private fun getCustomPropertyClassDisplayName(propertyClass: CustomPropertyClass): String? {
  return GanttLanguage.getInstance().formatText(propertyClass.iD)
}
