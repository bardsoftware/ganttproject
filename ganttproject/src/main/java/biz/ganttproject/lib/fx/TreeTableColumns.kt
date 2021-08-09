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

import biz.ganttproject.core.table.ColumnList
import javafx.beans.property.SimpleDoubleProperty
import javafx.scene.control.TreeTableColumn
import net.sourceforge.ganttproject.CustomPropertyManager

data class BuiltinColumns(
  val isZeroWidth: (String) -> Boolean,
  val allColumns: () -> List<ColumnList.Column>
)

/**
 * @author dbarashev@bardsoftware.com
 */
class ColumnListImpl(
  private val columnList: MutableList<ColumnList.Column>,
  private val customPropertyManager: CustomPropertyManager,
  private val tableColumns: () -> List<TreeTableColumn<*, *>>,
  private val onColumnChange: () -> Unit = {},
  private val builtinColumns: BuiltinColumns
) : ColumnList {

  val totalWidth: Double get() = totalWidthProperty.value
  val totalWidthProperty = SimpleDoubleProperty()

  init {
    updateTotalWidth()
  }
  override fun getSize(): Int = columnList.size

  override fun getField(index: Int): ColumnList.Column = columnList[index]

  override fun clear() = synchronized(columnList) {
    columnList.clear()
    updateTotalWidth()
  }

  override fun add(id: String, order: Int, width: Int) {
    synchronized(columnList) {
      (this.columnList.firstOrNull { it.id == id } ?: run {
        customPropertyManager.getCustomPropertyDefinition(id)?.let { def ->
          ColumnList.ColumnStub(id, def.name, true,
            if (order == -1) tableColumns().count { it.isVisible } else order,
            if (width == -1) 75 else width
          )
        }
      })?.let {
        this.columnList.add(it)
      }
      updateTotalWidth()
    }
  }

  override fun importData(source: ColumnList, keepVisibleColumns: Boolean) {
    val remainVisible = if (keepVisibleColumns) {
      tableColumns().filter { it.isVisible }.map { it.userData as ColumnList.Column }
    } else emptyList()

    var importedList = source.copyOf()
    remainVisible.forEach { old -> importedList.firstOrNull { new -> new.id == old.id }?.isVisible = true }
    if (importedList.firstOrNull { it.isVisible } == null) {
      importedList = builtinColumns.allColumns()
    }
    importedList = importedList.sortedWith { left, right ->
      // test1 places visible columns before invisible
      val test1 = (if (left.isVisible) -1 else 0) + if (right.isVisible) 1 else 0
      when {
        test1 != 0 -> test1
        // Invisible columns are compared by name (why?)
        !left.isVisible && !right.isVisible && left.name != null && right.name != null -> {
          left.name.compareTo(right.name)
        }
        // If both columns are visible and their order value happens to be the same (may happen when importing
        // one project into another) then we compare ids (what we do if they are equal too?)
        left.order == right.order -> left.id.compareTo(right.id)
        // Otherwise we use order value
        else -> left.order - right.order
      }
    }

    // Here we merge the imported list with the currently available one.
    // We maintain the invariant: the list prefix [0, idxImported) is the same in the imported list and
    // in the result list.
    synchronized(columnList) {
      val currentList = columnList.map { it }.toMutableList()
      importedList.forEachIndexed { idxImported, column ->
        val idxCurrent = currentList.indexOfFirst { it.id == column.id }
        if (idxCurrent >= 0) {
          if (idxCurrent != idxImported) {
            // Because of the invariant, it can only be greater. We will remove all
            // the columns between the imported and existing. This may be an excessive measure
            // because the removed columns may be found later in the imported list, but that's ok.
            assert(idxCurrent > idxImported) {
              "Unexpected column indices: imported=$idxImported current=$idxCurrent for column=$column"
            }
            currentList.subList(idxImported, idxCurrent).clear()
          }
          if (currentList[idxImported] != column) {
            currentList[idxImported] = ColumnList.ColumnStub(column).also {
              it.setOnChange {
                updateTotalWidth()
                onColumnChange()
              }
            }
          }
        } else {
          currentList.add(idxImported, ColumnList.ColumnStub(column).also {
            it.setOnChange {
              updateTotalWidth()
              onColumnChange()
            }
          })
        }
        assert(currentList.subList(0, idxImported) == importedList.subList(0, idxImported))
      }
      // Finally clear the remaining tail.
      if (currentList.size > importedList.size) {
        currentList.subList(importedList.size, currentList.size).clear()
      }

      var firstDiff = -1
      columnList.forEachIndexed { index, column ->
        if (firstDiff == -1 && currentList.indexOfFirst { it.id == column.id } != index) {
          firstDiff = index
        }
      }
      if (firstDiff == -1 && currentList.size > columnList.size) {
        firstDiff = columnList.size
      }
      if (firstDiff != -1) {
        if (firstDiff < columnList.size) {
          columnList.subList(firstDiff, columnList.size).clear()
        }
        if (firstDiff < currentList.size) {
          columnList.addAll(currentList.subList(firstDiff, currentList.size))
        }
      }
      updateTotalWidth()
    }
  }

  override fun exportData(): List<ColumnList.Column> {
    synchronized(columnList) {
      tableColumns().forEachIndexed { index, column ->
        (column.userData as ColumnList.Column).let { userData ->
          columnList.firstOrNull { it.id == userData.id }?.let {
            it.order = index
            it.width = column.width.toInt()
          }
        }
      }
      return copyOf()
    }
  }

  fun columns(): List<ColumnList.Column> {
    synchronized(columnList) {
      return copyOf()
    }
  }

  private fun updateTotalWidth() {
    totalWidthProperty.value = columnList.filter { it.isVisible  }.sumOf {
      if (builtinColumns.isZeroWidth(it.id)) 0 else it.width
    }.toDouble()
  }
}

fun ColumnList.copyOf(): List<ColumnList.Column> {
  val copy = mutableListOf<ColumnList.ColumnStub>()
  for (i in 0 until this.size) {
    copy.add(ColumnList.ColumnStub(this.getField(i)))
  }
  return copy
}
