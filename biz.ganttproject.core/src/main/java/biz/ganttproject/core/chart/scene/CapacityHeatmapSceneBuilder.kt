/*
Copyright 2020 Dmitry Kazakov, BarD Software s.r.o

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
package biz.ganttproject.core.chart.scene

import biz.ganttproject.core.chart.canvas.Canvas
import biz.ganttproject.core.chart.canvas.Canvas.HAlignment
import biz.ganttproject.core.chart.canvas.Canvas.VAlignment
import biz.ganttproject.core.chart.grid.Offset
import biz.ganttproject.core.chart.grid.OffsetLookup
import java.awt.Color
import java.time.Instant
import java.util.*
import kotlin.math.roundToInt

/**
 * Builds resource heatmaps.
 */
class CapacityHeatmapSceneBuilder(
  private val input: InputApi,
  private val resources: List<Resource>,
  val canvas: Canvas = Canvas()
) {
  /**
   * Builds per-resource heatmaps one by one, from top of the chart downwards
   * If some resource is expanded, calls rendering of the load details
   */
  fun build() {
    canvas.clear()
    canvas.setOffset(0, input.getYCanvasOffset())
    var ypos = 0
    resources.forEach { resource ->
      // Draw day off loads
      buildLoads(calcLoadDistribution(resource.loads.filter { it.load == -1f }), ypos)
      // Draw working time loads
      buildLoads(calcLoadDistribution(resource.loads.filter { it.load != -1f }), ypos)
      if (resource.isExpanded) {
        ypos = buildLoadDetails(resource.loads, ypos)
      }
      ypos += input.getRowHeight()
      val nextLine = canvas.createLine(0, ypos, input.getChartWidth(), ypos)
      nextLine.foregroundColor = Color.GRAY
    }
  }

  /**
   * Builds resource load details, that is, tasks where the resource is
   * assigned to, with that resource load percentage
   */
  private fun buildLoadDetails(loads: List<Load>, ypos: Int): Int {
    var yPos2 = ypos
    loads.groupBy { it.taskId }.forEach {
      if (it.key != null) {
        yPos2 += input.getRowHeight()
        buildTasksLoadsRectangles(it.value, yPos2)
      }
    }
    return yPos2
  }

  /**
   * Builds the list of loads in a single chart row
   * Preconditions: loads come from the same distribution and are ordered by their time offsets
   */
  private fun buildLoads(loads: List<LoadBorder>, ypos: Int) {
    var suffix = ""
    loads.zipWithNext().forEach {
      if (it.first.load != 0f) {
        buildLoads(it.first, it.second, ypos, suffix)
        suffix = ""
      } else if (it.second.load > 0) {
        suffix = ".first"
      }
    }
  }

  /**
   * Builds prevLoad, with curLoad serving as a load right border marker and style hint
   */
  private fun buildLoads(prevLoad: LoadBorder, curLoad: LoadBorder, ypos: Int, suffix: String) {
    val nextRect = createRectangle(prevLoad.ts.toDate(), curLoad.ts.toDate(), ypos) ?: return
    nextRect.style = 
        if (prevLoad.load == -1f) "dayoff" 
        else prevLoad.load.getStyle() + suffix + if (curLoad.load == 0f) ".last" else ""
    nextRect.modelObject = prevLoad.load
    if (prevLoad.load != -1f) {
      createLoadText(nextRect, prevLoad.load)
    }
  }

  /**
   * Builds a list of loads in a single chart row
   * Precondition: loads belong to the same pair (resource, task) and are ordered by their time values
   */
  private fun buildTasksLoadsRectangles(partition: List<Load>, ypos: Int) {
    partition.forEach {
      val nextRect = createRectangle(it.startTs.toDate(), it.endTs.toDate(), ypos)
      if (nextRect != null) {
        nextRect.style = it.load.getStyle() + ".first.last"
        nextRect.modelObject = it.load
        createLoadText(nextRect, it.load)
      }
    }
  }

  private fun createLoadText(rect: Canvas.Rectangle, load: Float) {
    val loadLabel = canvas.createText(rect.middleX, rect.middleY - input.getYCanvasOffset(), "")
    loadLabel.setSelector { textLengthCalculator ->
      val loadInt = load.roundToInt()
      val loadStr = "$loadInt%"
      val emsLength = textLengthCalculator.getTextLength(loadStr)
      val displayLoad = loadInt != 100 && emsLength <= rect.width
      if (displayLoad) arrayOf(loadLabel.createLabel(loadStr, rect.width)) else emptyArray()
    }
    loadLabel.setAlignment(HAlignment.CENTER, VAlignment.CENTER)
    loadLabel.style = "text.resource.load"
  }

  private fun createRectangle(start: Date, end: Date, ypos: Int): Canvas.Rectangle? {
    if (start.after(input.getChartEndDate()) || end <= input.getChartStartDate()) {
      return null
    }
    val offsetLookup = OffsetLookup()
    val bounds = offsetLookup.getBounds(start, end, input.getOffsets())
    return canvas.createRectangle(bounds[0], ypos, bounds[1] - bounds[0], input.getRowHeight())
  }

  private fun Long.toDate() = Date.from(Instant.ofEpochMilli(this))

  class Resource(val loads: List<Load>, val isExpanded: Boolean = false)
  class Load(val startTs: Long, val endTs: Long, val load: Float, val taskId: Int? = null)

  interface InputApi {
    fun getYCanvasOffset(): Int
    fun getRowHeight(): Int
    fun getChartWidth(): Int
    fun getChartStartDate(): Date
    fun getChartEndDate(): Date
    fun getOffsets(): List<Offset>
  }
}

data class LoadBorder(val ts: Long, val load: Float)

fun calcLoadDistribution(loads: List<CapacityHeatmapSceneBuilder.Load>): List<LoadBorder> {
  // This is the list of moments where resource load grows or decreases.
  return loads.filter { it.load != 0f }.flatMap {
    listOf(
            LoadBorder(it.startTs, it.load),
            LoadBorder(it.endTs, -it.load)
    )
  }
          // Zero load starting from the Jurassic period
          .plus(LoadBorder(Long.MIN_VALUE, 0f))
          // Now we accumulate the load changes which happen at the same moment.
          // For instance, when we have two tasks which are assigned to some resource
          // and starting at the same day, their load change is summed.
          .groupBy(LoadBorder::ts, LoadBorder::load)
          .mapValues { it.value.sum() }
          // Sort the result by the timestamp. Now we have the ordered list of load changes
          // where all change moments are unique.
          .toSortedMap()
          .map { LoadBorder(it.key, it.value) }
          // Now let's accumulate the load change values from the beginning to the end.
          // If we have e.g. load change +50 at ts=100, then load change +50 at ts=200,
          // then load change -50 at ts=250 and finally the load change -50 at ts=300
          // then we'll have: [(100, 50), (200, 100), (250, 50), (300, 0)
          .runningReduce { accumulatedLoad, nextLoad ->
            LoadBorder(nextLoad.ts, accumulatedLoad.load + nextLoad.load)
          }
}

private fun Float.getStyle() = when {
  this < 100f -> "load.underload"
  this > 100f -> "load.overload"
  else -> "load.normal"
}
