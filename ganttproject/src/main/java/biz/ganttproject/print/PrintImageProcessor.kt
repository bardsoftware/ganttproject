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
import net.sourceforge.ganttproject.GanttExportSettings
import net.sourceforge.ganttproject.chart.Chart
import net.sourceforge.ganttproject.chart.export.RenderedChartImage
import org.imgscalr.Scalr
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import javax.print.attribute.Size2DSyntax
import javax.print.attribute.standard.MediaSize
import kotlin.math.min

enum class Orientation {
  PORTRAIT, LANDSCAPE
}

data class PrintPage(
  val row: Int,
  val column: Int,
  val imageFile: File,
  val widthFraction: Double,
  val heightFraction: Double
)

fun createImages(chart: Chart, media: MediaSize, dpi: Int, orientation: Orientation, channel: Channel<PrintPage>) {
  PrintImageProcessor(chart, media, dpi, orientation, channel).run()
}

class PrintImageProcessor(
  private val chart: Chart,
  private val media: MediaSize,
  private val dpi: Int,
  private val orientation: Orientation,
  private val channel: Channel<PrintPage>) {

  fun run() {
    println("media width pt=${media.getX(Size2DSyntax.INCH)*72}, height pt=${media.getY(Size2DSyntax.INCH)*72}")
    println("media width px=${media.pageWidthPx(dpi)} height px=${media.pageHeightPx(dpi)}")
    imageScope.launch {
      val wholeImage = (chart.getRenderedImage(GanttExportSettings()) as RenderedChartImage).wholeImage
      if (wholeImage is BufferedImage) {
        val pageWidth = if (orientation == Orientation.PORTRAIT) media.pageWidthPx(dpi) else media.pageHeightPx(dpi)
        val pageHeight = if (orientation == Orientation.PORTRAIT) media.pageHeightPx(dpi) else media.pageWidthPx(dpi)
        val imageHeight = wholeImage.height

        for (row in 0..imageHeight/pageHeight) {
          val rowHeight = (imageHeight - (row * pageHeight)).let {
            if (it < 0) pageHeight else min(pageHeight, it)
          }
          val topy = (row * pageHeight)
          val rowImages = buildRowImages(wholeImage, topy, rowHeight)
          rowImages.forEachIndexed { column, image ->
            println("size=${image.width}x${image.height} page=${pageWidth}x${pageHeight}")
            val widthFraction = image.width.toDouble()/pageWidth
            val file = kotlin.io.path.createTempFile().toFile()
            ImageIO.write(image, "png", file)
            channel.send(PrintPage(row, column, file, widthFraction, rowHeight.toDouble()/pageHeight))
          }
        }
        channel.close()
      }
    }
  }

  private fun buildRowImages(wholeImage: BufferedImage, topy: Int, rowHeight: Int): List<BufferedImage> {
    val pageWidth = if (orientation == Orientation.PORTRAIT) media.pageWidthPx(dpi) else media.pageHeightPx(dpi)
    val imageWidth = wholeImage.width
    val result = mutableListOf<BufferedImage>()
    for (column in 0..imageWidth/pageWidth) {
      try {
        result.add(Scalr.crop(
          wholeImage,
          (column * pageWidth),
          topy,
          (imageWidth - (column * pageWidth)).let {
            if (it < 0) pageWidth else min(pageWidth, it)
          },
          rowHeight
        ))
      } catch (e: IOException) {
        println(e)
      }
    }
    return result
  }
}

fun MediaSize.pageWidthPx(dpi: Int) = (this.getX(Size2DSyntax.INCH) * dpi).toInt()
fun MediaSize.pageHeightPx(dpi: Int) = (this.getY(Size2DSyntax.INCH) * dpi).toInt()

private val imageScope = CoroutineScope(Dispatchers.IO)
