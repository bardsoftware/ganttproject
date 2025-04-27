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
package biz.ganttproject.ganttview

import biz.ganttproject.FXUtil
import biz.ganttproject.core.table.*
import biz.ganttproject.core.table.ColumnList.ColumnStub
import biz.ganttproject.customproperty.CustomPropertyDefinition
import biz.ganttproject.lib.fx.*
import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.ResourceDefaultColumn
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.resource.ResourceEvent
import net.sourceforge.ganttproject.resource.ResourceView
import net.sourceforge.ganttproject.roles.Role
import net.sourceforge.ganttproject.task.ResourceAssignment
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.math.BigDecimal
import kotlin.math.ceil

sealed class ResourceTableNode
class RootNode: ResourceTableNode()
class ResourceNode(internal val resource: HumanResource) : ResourceTableNode()
class AssignmentNode(internal val assignment: ResourceAssignment): ResourceTableNode()

data class ResourceTableChartConnector(
  val rowHeight: IntegerProperty,
  val minRowHeight: DoubleProperty,
  val collapseView: TreeCollapseView<HumanResource>
)

/**
 * This class connects a tree table UI widget to the chart and resource/task models.
 */
class ResourceTable(private val project: IGanttProject,
                    private val undoManager: GPUndoManager,
                    private val resourceChartConnector: ResourceTableChartConnector) :
  BaseTreeTableComponent<ResourceTableNode, ResourceDefaultColumn>(
    GPTreeTableView<ResourceTableNode>(TreeItem<ResourceTableNode>(RootNode())), project, undoManager
  ) {

  private val tableModel = ResourceTableModel()
  private val columns: ObservableList<ColumnList.Column> = FXCollections.observableArrayList()
  val columnList: ColumnListImpl = ColumnListImpl(columns, project.resourceCustomPropertyManager,
    { treeTable.columns },
    { onColumnsChange() },
    BuiltinColumns(
      isZeroWidth = {
        ResourceDefaultColumn.find(it)?.isIconified ?: false
      },
      allColumns = {
        ColumnList.Immutable.fromList(ResourceDefaultColumn.getColumnStubs()).copyOf()
      }
    )
  )

  init {
    resourceChartConnector.rowHeight.subscribe { value ->
      treeTable.fixedCellSize = ceil(maxOf(value.toDouble(), minCellHeight.value))
    }
    columnBuilder = ColumnBuilder(
      tableModel, project.humanResourceManager.customPropertyManager, undoManager,
      ResourceDefaultColumn::find
    )
    initProjectEventHandlers()
    project.humanResourceManager.addView(object: ResourceView {
      override fun resourceAdded(event: ResourceEvent?) {
        treeTable.reload(::sync)
      }

      override fun resourcesRemoved(event: ResourceEvent?) {
        treeTable.reload(::sync)
      }

      override fun resourceChanged(e: ResourceEvent?) {
        treeTable.reload(::sync)
      }

      override fun resourceAssignmentsChanged(e: ResourceEvent?) {
        treeTable.reload(::sync)
      }

    })
  }

  override fun loadDefaultColumns() = FXUtil.runLater {
    treeTable.columns.clear()
    columnList.importData(ColumnList.Immutable.fromList(
      ResourceDefaultColumn.getColumnStubs().map { ColumnStub(it) }.toList()
    ), false)
    val tableColumns = columnBuilder.buildColumns(
      columns = columnList.columns(),
      currentColumns = treeTable.columns.map { it.userData as ColumnList.Column }.toList(),
    )
    treeTable.setColumns(tableColumns)
    columns.addListener(ListChangeListener {
      onColumnsChange()
    })
  }

  private fun onColumnsChange()  {
    FXUtil.runLater {
      columnList.columns().forEach { it.resourceDefaultColumn()?.isVisible = it.isVisible }
      val newColumns = columnBuilder.buildColumns(
        columns = columnList.columns(),
        currentColumns = treeTable.columns.map { it.userData as ColumnList.Column }.toList(),
      )
      treeTable.setColumns(newColumns)
      treeTable.reload(::sync)
    }
  }

  override fun sync(keepFocus: Boolean) {
    treeTable.root.children.clear()
    project.humanResourceManager.resources.forEach { hr ->
      val resourceNode: TreeItem<ResourceTableNode> = TreeItem<ResourceTableNode>(ResourceNode(hr)).also {
        it.expandedProperty().subscribe(::onTreeItemExpanded)
      }
      treeTable.root.children.add(resourceNode)
      hr.assignments.forEach { assignment ->
        resourceNode.children.add(TreeItem(AssignmentNode(assignment)))
      }
    }
  }

  private fun onTreeItemExpanded(value: Boolean) {
    treeTable.root.depthFirstWalk { treeItem ->
      (treeItem.value as? ResourceNode)?.let {
        resourceChartConnector.collapseView.setExpanded(it.resource, treeItem.isExpanded)
      }
      true
    }
  }
}

private fun ColumnList.Column.resourceDefaultColumn() = ResourceDefaultColumn.find(this.id)?.stub

/**
 * A model class that moves data between the table cells and HumanResource instances.
 */
class ResourceTableModel: TableModel<ResourceTableNode, ResourceDefaultColumn> {
  override fun getValueAt(t: ResourceTableNode, defaultColumn: ResourceDefaultColumn): Any? {
    return if (t is ResourceNode) {
      when (defaultColumn) {
        ResourceDefaultColumn.NAME -> t.resource.name
        ResourceDefaultColumn.EMAIL -> t.resource.mail
        ResourceDefaultColumn.ROLE -> t.resource.role
        ResourceDefaultColumn.PHONE -> t.resource.phone
        ResourceDefaultColumn.ID -> t.resource.id
        ResourceDefaultColumn.STANDARD_RATE -> t.resource.standardPayRate
        ResourceDefaultColumn.TOTAL_COST -> t.resource.totalCost
        ResourceDefaultColumn.TOTAL_LOAD -> t.resource.totalLoad
        else -> null
      }
    } else if (t is AssignmentNode) {
      when (defaultColumn) {
        ResourceDefaultColumn.NAME -> t.assignment.task.name
        else -> null
      }
    } else {
      null
    }
  }

  override fun getValue(t: ResourceTableNode, customProperty: CustomPropertyDefinition): Any? {
    return if (t is ResourceNode) {
      t.resource.getCustomField(customProperty)
    } else null
  }

  override fun setValue(value: Any, node: ResourceTableNode, property: ResourceDefaultColumn) {
    if (node is ResourceNode) {
      when (property) {
        ResourceDefaultColumn.NAME -> node.resource.name = "$value"
        ResourceDefaultColumn.PHONE -> node.resource.phone = "$value"
        ResourceDefaultColumn.EMAIL -> node.resource.mail = "$value"
        ResourceDefaultColumn.ROLE -> node.resource.role = value as Role
        ResourceDefaultColumn.STANDARD_RATE -> (value as? Double)?.let { node.resource.standardPayRate = BigDecimal.valueOf(it) }
        else -> {}
      }
    }
  }

  override fun setValue(value: Any, node: ResourceTableNode, column: CustomPropertyDefinition) {
    if (node is ResourceNode) {
      node.resource.setValue(column, value)
    }
  }

}

class ResourceColumnBuilder(tableModel: ResourceTableModel,
                            project: IGanttProject,
                            undoManager: GPUndoManager,
                            private val nameCellFactory: TextCellFactory<ResourceTableNode, ResourceTableNode>,)
  : ColumnBuilder<ResourceTableNode, ResourceDefaultColumn>(
  tableModel, project.humanResourceManager.customPropertyManager, undoManager, ResourceDefaultColumn::find) {

  override fun createDefaultColumn(modelColumn: ResourceDefaultColumn): TreeTableColumn<ResourceTableNode, out Any> {
    return when (modelColumn) {
      ResourceDefaultColumn.NAME -> {
        TreeTableColumn<ResourceTableNode, ResourceTableNode>(modelColumn.name).apply {
          cellFactory = nameCellFactory
        }
      }
      else -> super.createDefaultColumn(modelColumn)
    }
  }
}