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
import biz.ganttproject.customproperty.CustomPropertyHolder
import biz.ganttproject.lib.fx.*
import javafx.beans.property.DoubleProperty
import javafx.beans.property.IntegerProperty
import javafx.collections.ListChangeListener
import javafx.scene.control.SelectionMode
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import javafx.util.StringConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.IGanttProject
import net.sourceforge.ganttproject.ProjectOpenActivityFactory
import net.sourceforge.ganttproject.ResourceDefaultColumn
import net.sourceforge.ganttproject.action.resource.ResourceActionSet
import net.sourceforge.ganttproject.chart.export.TreeTableApi
import net.sourceforge.ganttproject.resource.*
import net.sourceforge.ganttproject.roles.Role
import net.sourceforge.ganttproject.task.ResourceAssignment
import net.sourceforge.ganttproject.undo.GPUndoManager
import java.math.BigDecimal
import java.util.function.Consumer
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
  val collapseView: TreeCollapseView<HumanResource>,

  // Vertical scroll offset in the table.
  val tableScrollOffset: DoubleProperty,

  // Callback that is called whenever the chart vertical scroll offset changes.
  var chartScrollOffset: Consumer<Double>?,

  // Object that provides functions required for exporting a tree table as an image.
  var exportTreeTableApi: () -> TreeTableApi? = { null }
)

/**
 * This class is a controller of the tree table UI widget that shows project human resources.
 */
class ResourceTable(
  project: IGanttProject,
  private val undoManager: GPUndoManager,
  private val resourceSelectionManager: ResourceSelectionManager,
  val resourceActions: ResourceActionSet,
  private val resourceChartConnector: ResourceTableChartConnector,
  projectOpenActivityFactory: ProjectOpenActivityFactory) :
  BaseTreeTableComponent<ResourceTableNode, ResourceDefaultColumn>(
    GPTreeTableView(TreeItem<ResourceTableNode>(RootNode())),
    project,
    undoManager,
    project.resourceCustomPropertyManager,
    BuiltinColumns(
      isZeroWidth = {
        ResourceDefaultColumn.find(it)?.isIconified ?: false
      },
      allColumns = {
        ResourceDefaultColumn.entries
      }
    ),
    projectOpenActivityFactory
  ) {

  override val tableModel = ResourceTableModel(areChangesIgnored = { this.areChangesIgnored })
  private val resource2treeItem = mutableMapOf<HumanResource, TreeItem<ResourceNode>>()
  private val task2treeItem = mutableMapOf<ResourceAssignment, TreeItem<AssignmentNode>>()
  override val selectionKeeper = SelectionKeeper(logger = LOGGER, treeTable = this.treeTable, node2treeItem = { node ->
    val result: TreeItem<ResourceTableNode>? = when (node) {
      is ResourceNode -> resource2treeItem[node.resource] as? TreeItem<ResourceTableNode>
      is AssignmentNode -> task2treeItem[node.assignment] as? TreeItem<ResourceTableNode>
      else -> null
    }
    result
  })

  init {
    resourceChartConnector.rowHeight.subscribe { value ->
      treeTable.fixedCellSize = ceil(maxOf(value.toDouble(), minCellHeight.value))
    }
    resourceChartConnector.chartScrollOffset = Consumer { newValue ->
      FXUtil.runLater {
        treeTable.scrollBy(newValue)
      }
    }
    resourceChartConnector.exportTreeTableApi = {
      TreeTableApi(
        rowHeight = { resourceChartConnector.rowHeight.value },
        tableHeaderHeight = { treeTable.headerHeight.intValue()  },
        width = { fullWidthNotViewport ->
          if (fullWidthNotViewport) {
            // TODO: we need to autosize onl if column widths are not in the file,
            // that is, when we import from MS Project or CSV
            val job = CoroutineScope(Dispatchers.JavaFx).launch {
              treeTable.autosizeColumns()
              columnList.reloadWidthFromUi()
            }
            runBlocking {
              job.join()
            }
            columnList.totalWidth.toInt()
          } else {
            treeTable.width.toInt() - treeTable.vbarWidth().toInt()
          }
        },
        tableHeaderComponent = { null },
        tableComponent = { null },
        tablePainter = { this.buildImage(it, this::builtinColumnValueForPrinting) }
      )
    }
    treeTable.addScrollListener { newValue ->
      resourceChartConnector.tableScrollOffset.value = newValue
    }

    columnBuilder = ResourceColumnBuilder(tableModel, project, undoManager)
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

      override fun resourceModelReset() {
        LOGGER.debug(">> resourceModelReset()")
        sync(keepFocus = true)
        LOGGER.debug("<< resourceModelReset()")
      }
    })
    treeTable.selectionModel.selectedItems.addListener(ListChangeListener<TreeItem<ResourceTableNode>> { change ->
      if (selectionKeeper.ignoreSelectionChange) {
        return@ListChangeListener
      }
      val selectedResources = treeTable.selectionModel.selectedItems.map { it.value }.mapNotNull {
        when (it) {
          is ResourceNode -> it.resource
          else -> null
        }
      }.toList()
      val selectedAssignments = treeTable.selectionModel.selectedItems.map { it.value }.mapNotNull {
        when (it) {
          is AssignmentNode -> it.assignment
          else -> null
        }
      }.toList()
      if (selectedAssignments.isNotEmpty() && selectedResources.isNotEmpty()) {
        this.resourceSelectionManager.select(emptyList(), replace = true, trigger = this@ResourceTable)
        this.resourceSelectionManager.select(emptyList<ResourceAssignment>(), trigger = this@ResourceTable)
      } else {
        this.resourceSelectionManager.select(selectedResources, true, this@ResourceTable)
        this.resourceSelectionManager.select(selectedAssignments, this@ResourceTable)
      }
    })
    treeTable.selectionModel.selectionMode = SelectionMode.MULTIPLE
  }

  private fun builtinColumnValueForPrinting(
    resourceTableNode: ResourceTableNode,
    resourceDefaultColumn: ResourceDefaultColumn): String {

    val value = tableModel.getValueAt(resourceTableNode, resourceDefaultColumn)
    return value?.toString() ?: ""
  }

  override fun isTreeColumn(column: ResourceDefaultColumn): Boolean = ResourceDefaultColumn.NAME == column
  override fun getCustomValues(node: ResourceTableNode): CustomPropertyHolder = when {
    node is ResourceNode -> node.resource.customValues
    node is AssignmentNode -> CustomPropertyHolder.EMPTY
    else -> error("Unexpected node type")
  }

  override fun loadDefaultColumns() = FXUtil.runLater {
    columnList.importData(ColumnList.Immutable.fromList(
      ResourceDefaultColumn.getColumnStubs().map { ColumnStub(it) }.toList()
    ), false)
    val tableColumns = columnBuilder.buildColumns(
      columns = columnList.columns(),
      currentColumns = treeTable.columns.map { it.userData as ColumnList.Column }.toList(),
    )
    if (tableColumns.isNotEmpty()) {
      treeTable.setColumns(tableColumns)
    }
    columns.addListener(ListChangeListener {
      onColumnsChange()
    })
  }

  override fun sync(keepFocus: Boolean) {
    selectionKeeper.keepSelection(keepFocus) {
      val sync = ResourceSyncAlgorithm(project.humanResourceManager, treeTable.root, resource2treeItem, task2treeItem,
        onCreateTreeItem = { treeItem ->
          val node = treeItem.value
          if (node is ResourceNode) {
            treeItem.isExpanded = this.resourceChartConnector.collapseView.isExpanded(node.resource)
            treeItem.expandedProperty().subscribe(this::onTreeItemExpanded)
          }
        }
      )
      sync.sync()
    }
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
          resourceActions.resourceSendMailAction
        )
        separator()
        items(
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
    if (this.resourceSelectionManager.resourceAssignments.isNotEmpty()) {
      builder.apply {
        items(
          //resourceActions.assignedTaskProperties,
          resourceActions.assignmentDelete
        )
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
 * This object synchronizes the resource table widget contents with the model classes.
 */
class ResourceSyncAlgorithm(
  private val resourceManager: HumanResourceManager,
  private val rootItem: TreeItem<ResourceTableNode>,
  private val resource2treeItem: MutableMap<HumanResource, TreeItem<ResourceNode>>,
  private val task2treeItem: MutableMap<ResourceAssignment, TreeItem<AssignmentNode>>,
  private val onCreateTreeItem: (TreeItem<out ResourceTableNode>) -> Unit
  ) {

  fun sync() {
      task2treeItem.clear()
      resource2treeItem.clear()

      val parent = rootItem
      LOGGER.debug(">>> sync: root={} |children|={}", parent, parent.children.size)
      val resourceList = resourceManager.resources
      resourceList.forEachIndexed { idx, hr ->
        LOGGER.debug("... idx={} [hr]={}", idx, hr)
        if (parent.children.size > idx) {
          LOGGER.debug("... there is existing node @{}", idx)
          val childItem = parent.children[idx]
          val childRes = (childItem.value as? ResourceNode)?.resource ?: return@forEachIndexed
          if (childRes == hr) {
            LOGGER.debug("... it is the same as [hr]")
            childItem.value = ResourceNode(hr)
            resource2treeItem[hr] = childItem as TreeItem<ResourceNode>
            syncAssignments(childItem, hr)

          } else {
            LOGGER.debug("... it is {}, different from [hr]. Replacing with [hr]")
            parent.children.removeAt(idx)
            addResourceNode(parent, hr, idx).also {
              resource2treeItem[hr] = it as TreeItem<ResourceNode>
            }
          }
        } else {
          LOGGER.debug("... there is no node@{}, adding ", idx)
          addResourceNode(parent, hr, -1).also {
            resource2treeItem[hr] = it as TreeItem<ResourceNode>
          }
        }
      }
      LOGGER.debug("... now children size={}", parent.children.size)
      parent.children.subList(resourceList.size, parent.children.size).clear()
      LOGGER.debug("task2treeitem={}", task2treeItem)
  }

  private fun syncAssignments(parent: TreeItem<ResourceTableNode>, res: HumanResource) {
    res.assignments.forEachIndexed { idx, assignment ->
      if (parent.children.size > idx) {
        LOGGER.debug("... there is existing node @{}", idx)
        val childItem = parent.children[idx]
        val childAssignment = (childItem.value as? AssignmentNode)?.assignment ?: return@forEachIndexed
        if (childAssignment.task == assignment.task) {
          LOGGER.debug("... it is the same assignment as [assignment]")
          childItem.value = AssignmentNode(assignment)
          task2treeItem[assignment] = childItem as TreeItem<AssignmentNode>
        } else {
          LOGGER.debug("... it is different assignment, replacing")
          parent.children.removeAt(idx)
          addAssignmentNode(parent, assignment, idx)
        }
      } else {
        LOGGER.debug("... there is no node@{}, adding ", idx)
        addAssignmentNode(parent, assignment, -1)
      }
    }
    LOGGER.debug("... now children size={}", parent.children.size)
    parent.children.subList(res.assignments.size, parent.children.size).clear()
  }

  private fun addAssignmentNode(parentItem: TreeItem<ResourceTableNode>, assignment: ResourceAssignment, pos: Int) {
    val childItem = task2treeItem[assignment] ?: TreeItem(AssignmentNode(assignment))
    childItem.parent?.children?.remove(childItem)
    if (pos == -1 || pos > parentItem.children.size) {
      parentItem.children.add(childItem as TreeItem<ResourceTableNode>)
    } else {
      parentItem.children.add(pos, childItem as TreeItem<ResourceTableNode>)
    }
    task2treeItem[assignment] = childItem
  }


  private fun addResourceNode(parentItem: TreeItem<ResourceTableNode>, res: HumanResource, pos: Int): TreeItem<ResourceTableNode> {
    val childItem = resource2treeItem[res] ?: TreeItem(ResourceNode(res)).also {
      onCreateTreeItem(it)
    }
    childItem.parent?.children?.remove(childItem)
    if (pos == -1 || pos > parentItem.children.size) {
      parentItem.children.add(childItem as TreeItem<ResourceTableNode>)
    } else {
      parentItem.children.add(pos, childItem as TreeItem<ResourceTableNode>)
    }
    syncAssignments(childItem, res)
    return childItem
  }


}
/**
 * A model class that moves data between the table cells and HumanResource instances.
 */
class ResourceTableModel(private val areChangesIgnored: ()->Boolean)
  : TableModel<ResourceTableNode, ResourceDefaultColumn> {
  override fun getValueAt(t: ResourceTableNode, defaultColumn: ResourceDefaultColumn): Any? {
    return when (t) {
        is ResourceNode -> {
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
        }

      is AssignmentNode -> {
        when (defaultColumn) {
          ResourceDefaultColumn.NAME -> t.assignment.task.name
          ResourceDefaultColumn.ID -> t.assignment.task.taskID
          ResourceDefaultColumn.ROLE_IN_TASK -> t.assignment.roleForAssignment
          else -> null
        }
      }
      else -> ""

    }
  }

  override fun getValue(t: ResourceTableNode, customProperty: CustomPropertyDefinition): Any? {
    return if (t is ResourceNode) {
      t.resource.getCustomField(customProperty)
    } else null
  }

  override fun setValue(value: Any, node: ResourceTableNode, property: ResourceDefaultColumn) {
    if (areChangesIgnored()) {
      return
    }
    if (node is ResourceNode) {
      when (property) {
        ResourceDefaultColumn.NAME -> node.resource.name = "$value"
        ResourceDefaultColumn.PHONE -> node.resource.phone = "$value"
        ResourceDefaultColumn.EMAIL -> node.resource.mail = "$value"
        ResourceDefaultColumn.ROLE -> node.resource.role = value as Role
        ResourceDefaultColumn.STANDARD_RATE -> (value as? Double)?.let { node.resource.standardPayRate = BigDecimal.valueOf(it) }
        else -> {}
      }
    } else if (node is AssignmentNode) {
      when (property) {
        ResourceDefaultColumn.ROLE_IN_TASK -> node.assignment.roleForAssignment = value as Role?
        else -> {}
      }
    }
  }

  override fun setValue(value: Any, node: ResourceTableNode, column: CustomPropertyDefinition) {
    if (areChangesIgnored()) {
      return
    }
    if (node is ResourceNode) {
      node.resource.setValue(column, value)
    }
  }

}

/**
 * Column builder customized for the resource table. It creates a column with dropdown for the default role
 * property.
 */
class ResourceColumnBuilder(private val tableModel: ResourceTableModel,
                            private val project: IGanttProject,
                            undoManager: GPUndoManager)
  : ColumnBuilder<ResourceTableNode, ResourceDefaultColumn>(
  tableModel, project.humanResourceManager.customPropertyManager, undoManager, ResourceDefaultColumn::find) {

  override fun createDefaultColumn(modelColumn: ResourceDefaultColumn): TreeTableColumn<ResourceTableNode, out Any> {
    fun createRoleColumn(isEditableCell: (ResourceTableNode) -> Boolean) =
      createChoiceColumn(modelColumn.getName(),
        getValue = { tableModel.getValueAt(it, modelColumn) as Role?},
        setValue = { node, value ->
          if (value != tableModel.getValueAt(node, modelColumn)) {
            undoManager.undoableEdit("Edit Role") {
              tableModel.setValue(value, node, modelColumn)
            }
          }
        },
        allValues = { project.roleManager.enabledRoles.toList()},
        stringConverter = object : StringConverter<Role?>() {
          override fun toString(role: Role?): String = role?.name ?: ""

          override fun fromString(string: String?): Role =
            project.roleManager.enabledRoles.find { it.name == string } ?: project.roleManager.defaultRole
        },
        isEditableCell = isEditableCell
      )

    return when (modelColumn) {
      ResourceDefaultColumn.ROLE -> createRoleColumn {
        it is ResourceNode
      }
      ResourceDefaultColumn.ROLE_IN_TASK -> createRoleColumn {
        it is AssignmentNode
      }
      else -> super.createDefaultColumn(modelColumn)
    }
  }
}

private val LOGGER = GPLogger.create("ResourceTable")