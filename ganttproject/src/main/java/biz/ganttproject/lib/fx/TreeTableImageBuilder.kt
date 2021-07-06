package biz.ganttproject.lib.fx

import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.render.TextLengthCalculatorImpl
import biz.ganttproject.core.model.task.TaskDefaultColumn
import biz.ganttproject.core.table.TableSceneBuilder
import biz.ganttproject.core.table.TreeTableSceneBuilder
import biz.ganttproject.core.time.GanttCalendar
import biz.ganttproject.ganttview.TaskTable
import biz.ganttproject.ganttview.depthFirstWalk
import javafx.scene.control.TreeItem
import net.sourceforge.ganttproject.chart.ChartUIConfiguration
import net.sourceforge.ganttproject.chart.StyledPainterImpl
import net.sourceforge.ganttproject.task.Task
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.util.*

/**
 * @author dbarashev@bardsoftware.com
 */
fun TaskTable.buildImage(graphics2D: Graphics2D) {

  val taskTable = this
//  val bufferedImage = taskTable.treeTable.createEmptyBufferedImage()
  val textMetrics = TextLengthCalculatorImpl(graphics2D)
  val sceneBuilderInput = TreeTableSceneBuilder.InputApi(
    textMetrics = textMetrics,
    headerHeight = taskTable.headerHeightProperty.intValue(),
    rowHeight = taskTable.taskTableChartConnector.rowHeight.value,
    depthIndent = 15,
    horizontalOffset = 0
  )
  val treeTableSceneBuilder = TreeTableSceneBuilder(sceneBuilderInput)

  val visibleColumns = taskTable.columnList.exportData().filter { it.isVisible && TaskDefaultColumn.COLOR.stub.id != it.id}
  val columnMap = visibleColumns.associateWith {
    val defaultColumn = TaskDefaultColumn.find(it.id)
    TableSceneBuilder.Table.Column(
      name = it.name,
      width = it.width,
      isTreeColumn = it.id == TaskDefaultColumn.NAME.stub.id,
      alignment = defaultColumn?.alignment() ?: Canvas.HAlignment.LEFT
    )
  }
  val treeItem2sceneItem = mutableMapOf<TreeItem<Task>, TreeTableSceneBuilder.Item>()
  val rootSceneItems = mutableListOf<TreeTableSceneBuilder.Item>()
  taskTable.rootItem.depthFirstWalk { item ->
    val sceneItem = TreeTableSceneBuilder.Item(
      values = visibleColumns.associate {
        val key = columnMap[it]
        val value: String = TaskDefaultColumn.find(it.id)?.let { tdc ->
          taskTable.taskTableModel.getValueAt(item.value, tdc).toString()
        } ?: ""
        key?.let { key to value } ?: columnMap.values.first() to ""
      }
    )
    treeItem2sceneItem[item] = sceneItem
    treeItem2sceneItem[item.parent]?.let { it.subitems.add(sceneItem) } ?: run { rootSceneItems.add(sceneItem) }
    item.isExpanded
  }
  val canvas = treeTableSceneBuilder.build(
    columns = columnMap.values.toList(),
    items = rootSceneItems
  )
  val painter = StyledPainterImpl(ChartUIConfiguration( taskTable.project.uiConfiguration))
  painter.setGraphics(graphics2D)

  graphics2D.setRenderingHint(
    RenderingHints.KEY_TEXT_ANTIALIASING,
    RenderingHints.VALUE_TEXT_ANTIALIAS_GASP
  )
  graphics2D.color = Color.white
  graphics2D.fillRect(0, 0, this.treeTable.width.toInt(), treeItem2sceneItem.size * sceneBuilderInput.rowHeight)

  canvas.paint(painter)
}

fun GPTreeTableView<*>.createEmptyBufferedImage(): BufferedImage =
  BufferedImage(width.toInt(), height.toInt(), BufferedImage.TYPE_INT_RGB).also {
    val g2 = it.graphics as Graphics2D
  }

fun (TaskDefaultColumn?).alignment(): Canvas.HAlignment? {
  if (this == null) {
    return null
  }
  return when {
    java.lang.Number::class.java.isAssignableFrom(this.valueClass) -> Canvas.HAlignment.RIGHT
    GregorianCalendar::class.java.isAssignableFrom(this.valueClass) -> Canvas.HAlignment.RIGHT
    else -> Canvas.HAlignment.LEFT
  }
}
