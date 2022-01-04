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

package biz.ganttproject.print

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import net.sourceforge.ganttproject.GPLogger
import net.sourceforge.ganttproject.chart.Chart
import net.sourceforge.ganttproject.chart.export.RenderedChartImage
import org.imgscalr.Scalr
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.*
import javax.imageio.ImageIO
import javax.print.attribute.Size2DSyntax
import javax.print.attribute.standard.MediaSize
import kotlin.jvm.Throws
import kotlin.math.min

/**
 * Minimal interface which is required from the chart to be printable.
 */
interface PrintChartApi {
  fun exportChart(startDate: Date, endDate: Date, zoomLevel: Int = -1, isHeadless: Boolean = false): BufferedImage
}

internal enum class Orientation {
  PORTRAIT, LANDSCAPE
}

/**
 * Represents a tile of the whole image which is supposed to be printed on its own page.
 * The tiles are built taking into account the specified page dimensions and dots per inch
 * (DPI) value.
 * All tiles are grouped into a grid, very grid element is a single image which
 * is written into a temporary file.
 */
internal data class PrintPage(
  val row: Int,
  val column: Int,
  val imageFile: File,
  /**
   * Some images may occupy less than the whole page. This is the fraction of the
   * page width which is occupied by the image.
   */
  val widthFraction: Double,
  /**
   * Some images may occupy less than the whole page. This is the fraction of the
   * page height which is occupied by the image. All images in the same row have the same
   * heightFraction value.
   */
  val heightFraction: Double
)

internal fun createImages(chart: Chart, media: MediaSize, dpi: Int, orientation: Orientation, dateRange: ClosedRange<Date>, channel: Channel<PrintPage>) {
  PrintImageProcessor(chart, media, dpi, orientation, dateRange, channel).run()
}

internal class PrintImageProcessor(
  private val chart: Chart,
  private val media: MediaSize,
  private val dpi: Int,
  private val orientation: Orientation,
  private val dateRange: ClosedRange<Date>,
  private val channel: Channel<PrintPage>) {

  fun run() {
    val job = imageScope.launch {
      try {
        doRun()
      } catch (ex: IOException) {
        ourLogger.error("Image generation job failed", ex)
        channel.close(ex)
      }
    }
    ourLogger.debug("Started image generation job ${job.key}", mapOf("orientation" to orientation))
  }

  @Throws(IOException::class)
  private suspend fun doRun() {
    val wholeImage = chart.asPrintChartApi()
      .exportChart(startDate = dateRange.start, endDate = dateRange.endInclusive).let {
        when (it) {
          is RenderedChartImage -> it.wholeImage
          is BufferedImage -> it
          else -> null
        }
      }
    if (wholeImage is BufferedImage) {
      val pageWidth = if (orientation == Orientation.PORTRAIT) media.pageWidthPx(dpi) else media.pageHeightPx(dpi)
      val pageHeight = if (orientation == Orientation.PORTRAIT) media.pageHeightPx(dpi) else media.pageWidthPx(dpi)
      ourLogger.debug("Calculated page properties:", mapOf(
        "width(px)" to pageWidth,
        "height(px)" to pageHeight,
        "dpi" to dpi
      ))
      val imageHeight = wholeImage.height

      for (row in 0..imageHeight/pageHeight) {
        val rowHeight = (imageHeight - (row * pageHeight)).let {
          if (it < 0) pageHeight else min(pageHeight, it)
        }
        val topy = (row * pageHeight)
        val rowImages = buildRowImages(wholeImage, topy, rowHeight)
        rowImages.forEachIndexed { column, image ->
          //println("size=${image.width}x${image.height} page=${pageWidth}x${pageHeight}")
          val widthFraction = image.width.toDouble()/pageWidth
          val file = kotlin.io.path.createTempFile().toFile()
          ImageIO.write(image, "png", file)
          channel.send(PrintPage(row, column, file, widthFraction, rowHeight.toDouble()/pageHeight))
        }
      }
      channel.close()
    }
  }

  @Throws(IOException::class)
  private fun buildRowImages(wholeImage: BufferedImage, topy: Int, rowHeight: Int): List<BufferedImage> {
    val pageWidth = if (orientation == Orientation.PORTRAIT) media.pageWidthPx(dpi) else media.pageHeightPx(dpi)
    val imageWidth = wholeImage.width
    val result = mutableListOf<BufferedImage>()
    for (column in 0..imageWidth/pageWidth) {
      result.add(Scalr.crop(
        wholeImage,
        (column * pageWidth),
        topy,
        (imageWidth - (column * pageWidth)).let {
          if (it < 0) pageWidth else min(pageWidth, it)
        },
        rowHeight
      ))
    }
    return result
  }
}

private fun MediaSize.pageWidthPx(dpi: Int) = (this.getX(Size2DSyntax.INCH) * dpi).toInt()
private fun MediaSize.pageHeightPx(dpi: Int) = (this.getY(Size2DSyntax.INCH) * dpi).toInt()

private val imageScope = CoroutineScope(Dispatchers.IO)
private val ourLogger = GPLogger.create("Print.ImageProcessor")
