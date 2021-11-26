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
import javafx.print.PrinterJob as FxPrinterJob;
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import net.sourceforge.ganttproject.GPLogger
import java.awt.print.*
import javax.print.attribute.HashPrintRequestAttributeSet
import javafx.print.Paper as FxPaper

fun printPages(images: List<PrintPage>, mediaSize: MediaSize) {
  val printJob = PrinterJob.getPrinterJob()
  printJob.setPageable(PageableImpl(images, mediaSize))
  //val format = printJob.pageDialog(attr)
  if (printJob.printDialog()) {
    val attr = HashPrintRequestAttributeSet().also {
      it.add(DialogTypeSelection.NATIVE)
      it.add(mediaSize.mediaSizeName)
      it.add(OrientationRequested.LANDSCAPE)
    }
    try {
      printJob.print(attr)
    } catch (e: Exception) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err)
      }
    }
  }
}
fun printPages(images: List<PrintPage>, paper: FxPaper) {
  val printJob = FxPrinterJob.createPrinterJob()
  printJob.jobSettings.pageLayout = printJob.printer.createPageLayout(
    paper, PageOrientation.LANDSCAPE, Printer.MarginType.HARDWARE_MINIMUM
  )
  if (printJob.showPrintDialog(null)) {
    images.forEach { page ->
      printJob.printPage(ImageView(Image(page.imageFile.inputStream())))
    }
  }
}

class PageableImpl(private val pages: List<PrintPage>, mediaSize: MediaSize) : Pageable {
  private val commonScale = AtomicDouble(0.0)
  private val pageFormat = createPageFormat(mediaSize)
  override fun getNumberOfPages() = pages.size
  override fun getPageFormat(pageIndex: Int) = pageFormat
  override fun getPrintable(pageIndex: Int) = PrintableImpl(pages[pageIndex], commonScale)
}

class PrintableImpl(private val page: PrintPage, private val commonScale: AtomicDouble) : Printable {
  override fun print(graphics: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
    val image = ImageIO.read(page.imageFile)
    val g2d = graphics as? Graphics2D

    val scale = if (commonScale.get() == 0.0) {
      val imageWidthPt = image.width
      val imageHeightPt = image.height
      val scaleX = if (imageWidthPt < pageFormat.width) 1.0 else imageWidthPt/pageFormat.width
      val scaleY = if (imageHeightPt < pageFormat.height) 1.0 else imageHeightPt/pageFormat.height

      println("image width px=${image.width} pt=${imageWidthPt}")
      println("image height px=${image.height} pt=${imageHeightPt}")
      println("page width pt=${pageFormat.imageableWidth}")
      println("page height pt=${pageFormat.imageableHeight}")
      println("scaleX=$scaleX scaleY=$scaleY")

      1/(max(scaleX, scaleY))
    } else commonScale.get()
    println("scale=$scale")
    commonScale.set(scale)
    g2d?.setClip(
      0,
      0,
      image.width,
      image.height
    )

    val transform = AffineTransform.getScaleInstance(scale, scale)
    g2d?.drawRenderedImage(image, transform)

    return Printable.PAGE_EXISTS
  }

}
fun createPageFormat(mediaSize: MediaSize): PageFormat {
  return PageFormat().also { format ->
    //format.paper = Paper().also { paper ->
      //paper.setSize( mediaSize.getX(MediaSize.INCH) * 72.0, mediaSize.getY(MediaSize.INCH) * 72.0)
      //paper.setImageableArea(0.0, 0.0, paper.width, paper.height)
    //}
    format.orientation = PageFormat.LANDSCAPE
  }
}


