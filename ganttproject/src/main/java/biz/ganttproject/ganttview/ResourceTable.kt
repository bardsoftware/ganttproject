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
import biz.ganttproject.app.MenuBuilder
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
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.ResourceDefaultColumn
import net.sourceforge.ganttproject.action.resource.ResourceActionSet
import net.sourceforge.ganttproject.resource.HumanResource
import net.sourceforge.ganttproject.resource.ResourceEvent
import net.sourceforge.ganttproject.resource.ResourceSelectionManager
import net.sourceforge.ganttproject.resource.ResourceView
import net.sourceforge.ganttproject.roles.Role
import net.sourceforge.ganttproject.task.ResourceAssignment
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.math.BigDecimal
import kotlin.math.ceil

sealed class ResourceTableNode
class RootNode: ResourceTableNode()
class ResourceNode(internal val resource: HumanResource) : ResourceTableNode() {
  override fun toString(): String {
    return "ResourceNode(${resource.name})"
  }
}
class AssignmentNode(internal val assignment: ResourceAssignment): ResourceTableNode() {
  override fun toString(): String {
    return "AssignmentNode(assignment=$assignment)"
  }
}

/**
 * This class connects the table and the resource chart
 */
data class ResourceTableChartConnector(
  // Row height set by the chart. The table can listen this property and set the cell height appropriately.
  val rowHeight: IntegerProperty,

  // Minimum row height, defined by the fonts and padding.
  val minRowHeight: DoubleProperty,

  // Collapse view that keeps the resource nodes expansion state.
  // The chart listens to the changes and updates appropriately.
  val collapseView: TreeCollapseView<HumanResource>
)

/**
 * This class is a controller of the tree table UI widget that shows project human resources.
 */
class ResourceTable(private val project: IGanttProject,
                    private val undoManager: GPUndoManager,
                    private val resourceSelectionManager: ResourceSelectionManager,
                    private val resourceActions: ResourceActionSet,
                    private val resourceChartConnector: ResourceTableChartConnector) :
  BaseTreeTableComponent<ResourceTableNode, ResourceDefaultColumn>(
    GPTreeTableView(TreeItem<ResourceTableNode>(RootNode())), project, undoManager, project.resourceCustomPropertyManager
  ) {

  override val tableModel = ResourceTableModel()
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
  private val resource2treeItem = mutableMapOf<HumanResource, TreeItem<ResourceNode>>()
  private val task2treeItem = mutableMapOf<ResourceAssignment, TreeItem<AssignmentNode>>()
  override val selectionKeeper = SelectionKeeper(this.treeTable) { node ->
    val result: TreeItem<ResourceTableNode>? = when (node) {
      is ResourceNode -> resource2treeItem[node.resource] as? TreeItem<ResourceTableNode>
      is AssignmentNode -> task2treeItem[node.assignment] as? TreeItem<ResourceTableNode>
      else -> null
    }
    result
  }

  init {
    resourceChartConnector.rowHeight.subscribe { value ->
      treeTable.fixedCellSize = ceil(maxOf(value.toDouble(), minCellHeight.value))
    }
    columnBuilder = ColumnBuilder(
      tableModel, project.humanResourceManager.customPropertyManager, undoManager,
      ResourceDefaultColumn::find
    )
    initProjectEventHandlers()
    initKeyboardEventHandlers(listOf(resourceActions.resourceMoveUpAction, resourceActions.resourceMoveDownAction))
    project.humanResourceManager.addView(object: ResourceView {
      override fun resourceAdded(event: ResourceEvent?) {
        sync(keepFocus = true)
      }

      override fun resourcesRemoved(event: ResourceEvent?) {
        sync(keepFocus = true)
      }

      override fun resourceChanged(e: ResourceEvent?) {
        sync(keepFocus = true)
      }

      override fun resourceAssignmentsChanged(e: ResourceEvent?) {
        sync(keepFocus = true)
      }

      override fun resourceStructureChanged() {
        sync(keepFocus = true)
      }
    })
    treeTable.selectionModel.selectedItems.addListener(ListChangeListener<TreeItem<ResourceTableNode>> { change ->
      this.resourceSelectionManager.select(treeTable.selectionModel.selectedItems.map { it.value as? ResourceTableNode }.mapNotNull {
        when (it) {
          is ResourceNode -> it.resource
          else -> null
        }
      }.toList(), true, this@ResourceTable)
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

  override fun onProperties() {
    resourceActions.resourcePropertiesAction.actionPerformed(null)
  }

  override fun contextMenuActions(builder: MenuBuilder) {
    builder.apply {
      items(resourceActions.resourceNewAction)
    }
    if (resourceSelectionManager.resources.isNotEmpty()) {
      builder.apply {
        items(
          resourceActions.resourcePropertiesAction,
          resourceActions.resourceMoveUpAction,
          resourceActions.resourceMoveDownAction
        )
        separator()
        items(
          resourceActions.copyAction,
          resourceActions.cutAction,
          resourceActions.pasteAction,
          resourceActions.resourceDeleteAction
        )
      }
    }
  }

  private fun onColumnsChange()  {
    FXUtil.runLater {
      columnList.columns().forEach { it.resourceDefaultColumn()?.isVisible = it.isVisible }
      val newColumns = columnBuilder.buildColumns(
        columns = columnList.columns(),
        currentColumns = treeTable.columns.map { it.userData as ColumnList.Column }.toList(),
      )
      if (newColumns.isNotEmpty()) {
        treeTable.setColumns(newColumns)
      }
      treeTable.reload(::sync)
    }
  }

  override fun sync(keepFocus: Boolean) {
    selectionKeeper.keepSelection(keepFocus) {
      task2treeItem.clear()
      resource2treeItem.clear()

      val parent = treeTable.root
      LOGGER.debug(">>> sync: root={} |children|={}", parent, parent.children.size)
      val resourceList = project.humanResourceManager.resources
      resourceList.forEachIndexed { idx, hr ->
        LOGGER.debug("... idx={} [hr]={}", idx, hr)
        if (parent.children.size > idx) {
          LOGGER.debug("... there is existing node @{}", idx)
          val childItem = parent.children[idx]
          val childRes = (childItem.value as? ResourceNode)?.resource ?: return@forEachIndexed
          if (childRes == hr) {
            LOGGER.debug("... it is the same as [hr]")
            resource2treeItem[hr] = childItem as TreeItem<ResourceNode>
          } else {
            LOGGER.debug("... it is {}, different from [hr]. Replacing with [hr]")
            parent.children.removeAt(idx)
            addResourceNode(parent, hr, idx)
          }
        } else {
          LOGGER.debug("... there is no node@{}, adding ", idx)
          addResourceNode(parent, hr, -1)
        }
      }
      LOGGER.debug("... now children size={}", parent.children.size)
      parent.children.subList(resourceList.size, parent.children.size).clear()
    }
  }

  private fun addResourceNode(parentItem: TreeItem<ResourceTableNode>, res: HumanResource, pos: Int): TreeItem<ResourceTableNode> {
    val childItem = resource2treeItem[res] ?: TreeItem(ResourceNode(res)).also {
      it.expandedProperty().subscribe(::onTreeItemExpanded)
    }
    childItem.parent?.children?.remove(childItem)
    if (pos == -1 || pos > parentItem.children.size) {
      parentItem.children.add(childItem as TreeItem<ResourceTableNode>)
    } else {
      parentItem.children.add(pos, childItem as TreeItem<ResourceTableNode>)
    }
//    LOGGER.debug("addChildTreeItem: child=$child pos=$pos parent=$parentItem")
//    LOGGER.debug("addChildTreeItem: parentItem.children=${parentItem.children}")
//    LOGGER.delegate().debug("Stack: ", Exception())
    resource2treeItem[res] = childItem
    childItem.children.clear()
    res.assignments.forEach { assignment ->
      val assignmentItem = TreeItem(AssignmentNode(assignment))
      childItem.children.add(assignmentItem as TreeItem<ResourceTableNode>)
      task2treeItem[assignment] = assignmentItem
    }

    return childItem

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

private val LOGGER = GPLogger.create("ResourceTable")