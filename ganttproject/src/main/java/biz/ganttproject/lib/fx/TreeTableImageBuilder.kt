/*
 * Copyright (c) 2021 Dmitry Barashev, BarD Software s.r.o.
 *
 * This file is part of GanttProject, an open-source project management tool.
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
package biz.ganttproject.lib.fx

import biz.ganttproject.app.applicationFont
import biz.ganttproject.app.applicationFontSpec
import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.render.TextLengthCalculatorImpl
import biz.ganttproject.core.table.*
import biz.ganttproject.core.time.TimeDuration
import javafx.scene.control.TreeItem
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.chart.ChartUIConfiguration
import net.sourceforge.ganttproject.chart.StyledPainterImpl
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.util.*
import kotlin.math.roundToInt

/**
 * This function creates an image of the tree component on the provided graphics instance.
 *
 * @author dbarashev@bardsoftware.com
 */
fun <NodeType, BuiltinColumnType: BuiltinColumn> BaseTreeTableComponent<NodeType, BuiltinColumnType>.buildImage(
  graphics2D: Graphics2D, builtinColumnValue: (NodeType, BuiltinColumnType)->String) {
  val treeTable = this

  val textMetrics = TextLengthCalculatorImpl((graphics2D.create() as Graphics2D).also {
    it.font = applicationFontSpec.value.asAwtFontOfSize(applicationFont.value.size.roundToInt())
  })
  val sceneBuilderInput = TreeTableSceneBuilder.InputApi(
    textMetrics = textMetrics,
    headerHeight = treeTable.headerHeightProperty.intValue(),
    headerAlignment = Canvas.HAlignment.CENTER,
    rowHeight = treeTable.rowHeightProperty.intValue(),
    depthIndent = 15,
    horizontalOffset = 0,
    fontSpec = applicationFontSpec.value
  )
  val treeTableSceneBuilder = TreeTableSceneBuilder(sceneBuilderInput)

  val visibleColumns: List<ColumnList.Column> = treeTable.columnList.exportData().filter {
    // We will not print as columns the color and notes columns: they are shown as icons in the table UI.
    it.isVisible && !treeTable.builtinColumns.isZeroWidth(it.id)
  }
  val columnMap: Map<ColumnList.Column, TableSceneBuilder.Table.Column> = visibleColumns.mapNotNull {
    builtinColumns.find(it.id)?.let { defaultColumn ->
      it to TableSceneBuilder.Table.Column(
        name = it.name,
        width = it.width,
        isTreeColumn = treeTable.isTreeColumn(defaultColumn as BuiltinColumnType),
        alignment = defaultColumn.alignment() ?: Canvas.HAlignment.LEFT
      )
    }
  }.toMap()
  val treeItem2sceneItem = mutableMapOf<TreeItem<NodeType>, TreeTableSceneBuilder.Item>()
  val rootSceneItems = mutableListOf<TreeTableSceneBuilder.Item>()

  treeTable.rootItem.depthFirstWalk { item ->
    val sceneItem = TreeTableSceneBuilder.Item(
      values = visibleColumns.associate {
        val key = columnMap[it]
        val value: String = treeTable.builtinColumns.find(it.id)?.let { tdc ->
          builtinColumnValue(item.value, tdc as BuiltinColumnType)
        } ?: run {
          val customPropertyManager = treeTable.customPropertyManager
          val def = customPropertyManager.getCustomPropertyDefinition(it.id)
          if (def == null) {
            LOGGER.error("can't find def for custom property=${it.id}")
          }
          def?.let { d -> treeTable.getCustomValues(item.value).getValue(d) }?.toString() ?: ""
        }
        key?.let { key to value } ?: (columnMap.values.first() to "")
      }
    )
    treeItem2sceneItem[item] = sceneItem
    treeItem2sceneItem[item.parent]?.subitems?.add(sceneItem) ?: run { rootSceneItems.add(sceneItem) }
    item.isExpanded
  }
  val canvas = treeTableSceneBuilder.build(
    columns = columnMap.values.toList(),
    items = rootSceneItems
  )
  val painter = StyledPainterImpl(ChartUIConfiguration( treeTable.project.uIConfiguration).also {
    // Use font from the application font settings for the export.
    val fontsize = applicationFont.value.size.roundToInt()
    val font = applicationFontSpec.value.asAwtFontOfSize(fontsize)
    it.setBaseFont(font, fontsize)
  })
  painter.setGraphics(graphics2D)

  graphics2D.setRenderingHint(
    RenderingHints.KEY_TEXT_ANTIALIASING,
    RenderingHints.VALUE_TEXT_ANTIALIAS_GASP
  )
  graphics2D.color = Color.white
  graphics2D.fillRect(0, 0, this.treeTable.width.toInt(), treeItem2sceneItem.size * sceneBuilderInput.rowHeight)

  canvas.paint(painter)
}

fun (BuiltinColumn?).alignment(): Canvas.HAlignment? {
  if (this == null) {
    return null
  }
  return when {
    java.lang.Number::class.java.isAssignableFrom(this.valueClass) -> Canvas.HAlignment.RIGHT
    GregorianCalendar::class.java.isAssignableFrom(this.valueClass) -> Canvas.HAlignment.RIGHT
    else -> Canvas.HAlignment.LEFT
  }
}

private val LOGGER = GPLogger.create("TaskTable.ImageBuilder")