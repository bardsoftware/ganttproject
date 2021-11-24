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

/**
 * @author dbarashev@bardsoftware.com
 */
private val imageScope = CoroutineScope(Dispatchers.IO)

fun createImages(chart: Chart, columns: Int, rows: Int, channel: Channel<File>) {
  imageScope.launch {
    val wholeImage = (chart.getRenderedImage(GanttExportSettings()) as RenderedChartImage).wholeImage
    if (wholeImage is BufferedImage) {
      val pageWidth = wholeImage.width / columns
      val pageHeight = wholeImage.height / rows
      for (row in 0 until rows) {
        for (column in 0 until columns) {
          try {
            val file = kotlin.io.path.createTempFile().toFile()
            val tile = Scalr.crop(
              wholeImage,
              (column * pageWidth),
              (row * pageHeight),
              pageWidth,
              pageHeight
            )

            ImageIO.write(tile, "png", file)
            channel.send(file)
          } catch (e: IOException) {
            println(e)
          }
        }
      }
      channel.close()
    }
  }
}
