/*
Copyright 2025 Dmitry Barashev,  BarD Software s.r.o

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
package biz.ganttproject.core.table

import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.customproperty.CustomPropertyClass
import biz.ganttproject.customproperty.CustomPropertyManager
import biz.ganttproject.lib.fx.createBooleanColumn
import biz.ganttproject.lib.fx.createDateColumn
import biz.ganttproject.lib.fx.createDecimalColumn
import biz.ganttproject.lib.fx.createDoubleColumn
import biz.ganttproject.lib.fx.createIntegerColumn
import biz.ganttproject.lib.fx.createTextColumn
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.beans.value.ObservableValue
import javafx.scene.control.TreeTableColumn
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.math.BigDecimal
import java.util.GregorianCalendar

open class ColumnBuilder<NodeType, DefaultColumnType: BuiltinColumn>(
    private val tableModel: TableModel<NodeType, DefaultColumnType>,
    private val customPropertyManager: CustomPropertyManager,
    protected val undoManager: GPUndoManager,
    private val id2column: (String)->DefaultColumnType?) {

  fun buildColumns(columns: List<ColumnList.Column>,
                   currentColumns: List<ColumnList.Column>): List<TreeTableColumn<NodeType, Any>> {

    val filteredColumns = columns.filter { col -> (id2column(col.id)?.isIconified ?: false).not()  }
    return (if (anyDifference(filteredColumns, currentColumns)) {
      columns.mapNotNull { column ->
        val taskDefaultColumn = id2column(column.id)
        when {
          taskDefaultColumn == null -> createCustomColumn(column)
          taskDefaultColumn.isIconified -> null
          else -> createDefaultColumn(taskDefaultColumn).also { tableColumn ->
            (tableColumn as TreeTableColumn<NodeType, Any>).let {
              postCreateDefaultColumn(it, taskDefaultColumn, column)
            }
          }
        }?.also {
          it.prefWidth = column.width.toDouble()
        }
      }.toList()
      //(treeTable.lookup(".virtual-flow") as Region).minWidth = columnList.totalWidth.toDouble()
    } else {
      emptyList()
    }) as List<TreeTableColumn<NodeType, Any>>
  }

  private fun anyDifference(newColumns: List<ColumnList.Column>, oldColumns: List<ColumnList.Column>): Boolean {
    if (newColumns.size != oldColumns.size) {
      LOGGER.debug("anyDifference: columns list sizes are different: new={}, old={}", newColumns.size, oldColumns.size)
      return true
    }
    newColumns.forEach { col ->
      oldColumns.find { it.id == col.id }?.let {
        if (it != col) {
          LOGGER.debug("anyDifference: column {} != old column with the same id={}", col, it)
          return true
        }
      } ?: run {
        LOGGER.debug("anyDifference: column {} not found in the old columns", col)
        return true
      }
    }
    oldColumns.forEach { col ->
      newColumns.find { it.id == col.id }?.let {
        if (it != col) {
          LOGGER.debug("anyDifference: column {} != new column with the same id={}", col, it)
          return true
        }
      } ?: run {
        LOGGER.debug("anyDifference: column {} not found in the new columns", col)
        return true
      }
    }
    LOGGER.debug("anyDifference: no difference in the column list")
    return false
  }

  protected open fun postCreateDefaultColumn(
      tableColumn: TreeTableColumn<NodeType, Any>, modelColumn: DefaultColumnType, viewData: ColumnList.Column) {
    tableColumn.isEditable = modelColumn.isEditable(null)
    tableColumn.isVisible = viewData.isVisible
    tableColumn.userData = viewData
    tableColumn.prefWidth = viewData.width.toDouble()
  }


  protected open fun createDefaultColumn(modelColumn: DefaultColumnType): TreeTableColumn<NodeType, out Any> {
    return when {
      // TEXT column
      modelColumn.valueClass == java.lang.String::class.java -> {
        val column: TreeTableColumn<NodeType, String> = createTextColumn<NodeType>(
            name = modelColumn.getName(),
            getValue = {
                tableModel.getValueAt(it, modelColumn).toString()
            },
            setValue = { node, value ->
                undoManager.undoableEdit("Edit properties") {
                    tableModel.setValue(value, node, modelColumn)
                    onEditCompleted(node)
                }
            },
            onEditCompleted = {
                onEditCompleted(it)
            }
        )
        column
      }

      // DATE column
      GregorianCalendar::class.java.isAssignableFrom(modelColumn.valueClass) -> {
          createDateColumn<NodeType>(
              modelColumn.getName(),
              getValue = {
                  tableModel.getValueAt(it, modelColumn) as GanttCalendar?
              },
              setValue = { node, value ->
                  undoManager.undoableEdit("Edit properties") {
                      tableModel.setValue(value, node, modelColumn)
                  }
              }
          )
      }

      // INT column
      modelColumn.valueClass == Integer::class.java -> {
          createIntegerColumn<NodeType>(
              modelColumn.getName(),
              getValue = {
                  tableModel.getValueAt(it, modelColumn) as Int
              },
              setValue = { node, value ->
                  undoManager.undoableEdit("Edit properties") {
                      tableModel.setValue(value, node, modelColumn)
                  }
              }
          )
      }

      // FLOATING POINT column
      modelColumn.valueClass == java.lang.Double::class.java -> {
          createDoubleColumn<NodeType>(
              modelColumn.getName(),
              getValue = { tableModel.getValueAt(it, modelColumn) as Double },
              setValue = { node, value ->
                  undoManager.undoableEdit("Edit properties") {
                      tableModel.setValue(value, node, modelColumn)
                  }
              }
          )
      }

      // DECIMAL column
      modelColumn.valueClass == BigDecimal::class.java -> {
          createDecimalColumn<NodeType>(
              modelColumn.getName(),
              getValue = { tableModel.getValueAt(it, modelColumn) as BigDecimal? },
              setValue = { node, value ->
                  undoManager.undoableEdit("Edit properties") {
                      tableModel.setValue(value, node, modelColumn)
                  }
              }
          )
      }

      else -> TreeTableColumn<NodeType, Any>(modelColumn.getName()).apply {
        setCellValueFactory {
          ReadOnlyStringWrapper(
              tableModel.getValueAt(it.value.value, modelColumn).toString()
          ) as ObservableValue<in Any>?
        }
      }
    }
  }

  private fun createCustomColumn(column: ColumnList.Column): TreeTableColumn<NodeType, *>? {
    val customProperty = customPropertyManager.getCustomPropertyDefinition(column.id) ?: return null
    return when (customProperty.propertyClass) {
      CustomPropertyClass.TEXT -> {
          createTextColumn(
              name = customProperty.name,
              getValue = { tableModel.getValue(it, customProperty)?.toString() },
              setValue = { node, value ->
                  undoManager.undoableEdit("Edit properties") {
                      tableModel.setValue(value, node, customProperty)
                      onEditCompleted(node)
                  }
              },
              onEditCompleted = {
                  onEditCompleted(it)
              }
          )
      }
      CustomPropertyClass.BOOLEAN -> {
          createBooleanColumn<NodeType>(
              customProperty.name,
              { tableModel.getValue(it, customProperty) as Boolean? },
              { task, value ->
                  undoManager.undoableEdit("Edit properties") {
                      tableModel.setValue(value, task, customProperty)
                  }
              }
          )
      }
      CustomPropertyClass.INTEGER -> {
          createIntegerColumn(
              customProperty.name,
              { tableModel.getValue(it, customProperty) as Int? },
              { task, value ->
                  undoManager.undoableEdit("Edit properties") {
                      tableModel.setValue(value, task, customProperty)
                  }
              }
          )
      }
      CustomPropertyClass.DOUBLE -> {
          createDoubleColumn(
              customProperty.name,
              { tableModel.getValue(it, customProperty) as Double? },
              { task, value ->
                  undoManager.undoableEdit("Edit properties") {
                      tableModel.setValue(value, task, customProperty)
                  }
              }
          )
      }
      CustomPropertyClass.DATE -> {
          createDateColumn(
              customProperty.name,
              { tableModel.getValue(it, customProperty) as GanttCalendar? },
              { task, value ->
                  undoManager.undoableEdit("Edit properties") {
                      tableModel.setValue(value, task, customProperty)
                  }
              }
          )
      }
    }.also {
      it.isEditable = customProperty.calculationMethod == null
      it.isVisible = column.isVisible
      it.userData = column
      it.prefWidth = column.width.toDouble()
    }
  }

  protected open fun onEditCompleted(node: NodeType) {}
}