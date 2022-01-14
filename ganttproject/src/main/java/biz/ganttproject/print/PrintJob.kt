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

import com.google.common.util.concurrent.AtomicDouble
import javafx.print.PageOrientation
import javafx.print.Printer
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import javax.imageio.ImageIO
import javax.print.attribute.standard.*
import kotlin.math.max
import javafx.print.PrinterJob as FxPrinterJob
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import net.sourceforge.ganttproject.GPLogger
import java.awt.print.*
import javax.print.attribute.HashPrintRequestAttributeSet
import kotlin.math.min
import javafx.print.Paper as FxPaper

internal fun printPages(images: List<PrintPage>, mediaSize: MediaSize, orientation: Orientation) {
  val printJob = PrinterJob.getPrinterJob()
  val pageable = PageableImpl(images, mediaSize, orientation)
  printJob.setPageable(pageable)
  val attr = HashPrintRequestAttributeSet().also {
    it.add(DialogTypeSelection.NATIVE)
    it.add(mediaSize.mediaSizeName)
    it.add(if (orientation == Orientation.LANDSCAPE) OrientationRequested.LANDSCAPE else OrientationRequested.PORTRAIT)
  }
  if (printJob.printDialog(attr)) {
    try {
      val printableArea = attr.get(MediaPrintableArea::class.java) as? MediaPrintableArea
      val mediaSizeName = attr.get(MediaSizeName::class.java) as? MediaSizeName
      val selectedMediaSize = mediaSizeName?.let { mediaSizes.values.firstOrNull { it.mediaSizeName == it } } ?: mediaSize
      //val mediaSize = attr.get(MediaSize::class.java) as? MediaSize
      val orientation = attr.get(OrientationRequested::class.java) as? OrientationRequested
      if (printableArea != null && mediaSize != null && orientation != null) {
        pageable.pageFormat = createPageFormat(selectedMediaSize, printableArea, if (orientation == OrientationRequested.LANDSCAPE) Orientation.LANDSCAPE else Orientation.PORTRAIT)
      }
      printJob.print(attr)
    } catch (e: Exception) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err)
      }
    }
  }
}
internal fun printPages(images: List<PrintPage>, paper: FxPaper) {
  val printJob = FxPrinterJob.createPrinterJob()
  printJob.jobSettings.pageLayout = printJob.printer.createPageLayout(
    paper, PageOrientation.LANDSCAPE, Printer.MarginType.HARDWARE_MINIMUM
  )
  if (printJob.showPrintDialog(null)) {
    images.forEach { page ->
      val image = Image(page.imageFile.inputStream())
      printJob.printPage(ImageView(image))
    }
  }
}

internal class PageableImpl(private val pages: List<PrintPage>, mediaSize: MediaSize, orientation: Orientation) : Pageable {
  private val commonScale = AtomicDouble(0.0)
  internal var pageFormat = PageFormat().also {
    it.orientation = if (orientation == Orientation.LANDSCAPE) PageFormat.LANDSCAPE else PageFormat.PORTRAIT
    it.paper = Paper().also {
      it.setImageableArea(0.0, 0.0, it.width, it.height)
    }
  }
  override fun getNumberOfPages() = pages.size
  override fun getPageFormat(pageIndex: Int) = pageFormat
  override fun getPrintable(pageIndex: Int) = PrintableImpl(pages[pageIndex], commonScale)
}

internal class PrintableImpl(private val page: PrintPage, private val commonScale: AtomicDouble) : Printable {
  override fun print(graphics: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
    val image = ImageIO.read(page.imageFile)
    val g2d = graphics as? Graphics2D ?: return Printable.NO_SUCH_PAGE

    val scale = if (commonScale.get() == 0.0) {
      val imageWidthPt = image.width
      val imageHeightPt = image.height
      val scaleX = pageFormat.imageableWidth/image.width
      val scaleY = pageFormat.imageableHeight/image.height

//      println("image width px=${image.width} pt=${imageWidthPt}")
//      println("image height px=${image.height} pt=${imageHeightPt}")
//      println("page width pt=${pageFormat.imageableWidth}")
//      println("page height pt=${pageFormat.imageableHeight}")
//      println("scaleX=$scaleX scaleY=$scaleY")

      min(scaleX, scaleY)
    } else commonScale.get()
    commonScale.set(scale)
    g2d.translate(pageFormat.imageableX, pageFormat.imageableY)
    g2d.scale(scale, scale)
    g2d.drawImage(image, 0, 0, null)

    return Printable.PAGE_EXISTS
  }

}

private fun createPageFormat(mediaSize: MediaSize, printableArea: MediaPrintableArea, orientation: Orientation) = PageFormat().also {
  it.paper = Paper().also {
    it.setImageableArea(
      printableArea.getX(MediaPrintableArea.INCH)*72.0,
      printableArea.getY(MediaPrintableArea.INCH)*72.0,
      printableArea.getWidth(MediaPrintableArea.INCH)*72.0,
      printableArea.getHeight(MediaPrintableArea.INCH)*72.0
    )
    it.setSize(mediaSize.getX(MediaSize.INCH)*72.0, mediaSize.getY(MediaSize.INCH)*72.0)
  }
  it.orientation = if (orientation == Orientation.LANDSCAPE) PageFormat.LANDSCAPE else PageFormat.PORTRAIT
}

