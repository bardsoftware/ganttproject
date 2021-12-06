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
package net.sourceforge.ganttproject.chart

import biz.ganttproject.print.PrintChartApi
import net.sourceforge.ganttproject.GanttExportSettings
import net.sourceforge.ganttproject.chart.export.*
import net.sourceforge.ganttproject.gui.zoom.ZoomManager
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage
import java.util.*

/**
 * @author dbarashev@bardsoftware.com
 */
class PrintChartApiImpl(
  private val modelCopy: ChartModelBase,
  private val setup: (GanttExportSettings) -> Unit,
  private val treeTableApi: () -> TreeTableApi,
  private val zoomManager: ZoomManager) : PrintChartApi {

  override fun exportChart(startDate: Date, endDate: Date, zoomLevel: Int, isHeadless: Boolean): BufferedImage {
    val exportSettings = GanttExportSettings().also {
      it.startDate = startDate
      it.endDate = endDate
      it.zoomLevel =
        if (zoomLevel < 0) zoomManager.zoomState else zoomManager.getZoomState(zoomLevel)
      it.isCommandLineMode = isHeadless
    }
    val visitor = ChartRasterImageBuilder()
    buildImage(exportSettings, visitor)
    return visitor.result
  }

  fun buildImage(exportSettings: GanttExportSettings, visitor: ChartImageVisitor) {
    setup(exportSettings)
    ChartImageBuilder(exportSettings, modelCopy, treeTableApi()).buildImage(visitor)
  }
}

internal class ChartRasterImageBuilder : ChartImageVisitor {
  private var myRenderedImage: RenderedChartImage? = null
  private var myGraphics: Graphics2D? = null
  private var myTreeImage: BufferedImage? = null

  val result: BufferedImage get() = myRenderedImage!!.wholeImage

  override fun acceptLogo(d: ChartDimensions, logo: Image?) {
    if (d.treeWidth <= 0) {
      return
    }
    val g = getGraphics(d)
    g!!.background = Color.WHITE
    g.clearRect(0, 0, d.treeWidth, d.logoHeight)
    // Hack: by adding 35, the left part of the logo becomes visible,
    // otherwise it gets chopped off
    g.drawImage(logo, 0, 0, null)
  }

  override fun acceptTable(d: ChartDimensions, treeTable: TreeTableApi) {
    if (d.treeWidth <= 0) {
      return
    }
    val g = getGraphics(d)
    g!!.background = Color.WHITE
    g.clearRect(0, d.logoHeight, d.treeWidth, d.chartHeight + d.logoHeight)
    g.translate(0, d.logoHeight)
    val header = treeTable.tableHeaderComponent.invoke()
    if (header != null) {
      header.print(g)
      g.translate(0, d.tableHeaderHeight)
    }
    val table = treeTable.tableComponent.invoke()
    if (table != null) {
      table.print(g)
    } else {
      treeTable.tablePainter!!.invoke(g)
    }
  }

  override fun acceptChart(d: ChartDimensions, model: ChartModel) {
    if (myTreeImage == null) {
      myTreeImage = BufferedImage(1, d.chartHeight, BufferedImage.TYPE_INT_RGB)
    }
    myRenderedImage = RenderedChartImage(
      model, myTreeImage, d.chartWidth, d.chartHeight, d.logoHeight
    )
  }

  private fun getGraphics(d: ChartDimensions): Graphics2D? {
    if (myGraphics == null) {
      myTreeImage = BufferedImage(
        d.treeWidth, d.chartHeight,
        BufferedImage.TYPE_INT_RGB
      )
      myGraphics = myTreeImage!!.createGraphics()
    }
    return myGraphics
  }
}
