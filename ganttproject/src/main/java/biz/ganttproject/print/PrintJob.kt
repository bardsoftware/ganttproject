package biz.ganttproject.print

import net.sourceforge.ganttproject.GPLogger
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.print.*
import java.io.File
import javax.imageio.ImageIO
import javax.print.attribute.HashPrintRequestAttributeSet
import javax.print.attribute.standard.MediaSize
import javax.print.attribute.standard.MediaSizeName
import javax.print.attribute.standard.OrientationRequested

fun printPages(images: List<File>) {
  val printJob = PrinterJob.getPrinterJob()
  printJob.setPageable(PageableImpl(images))
  val attr = HashPrintRequestAttributeSet().also {
    it.add(MediaSizeName.ISO_A4)
    it.add(OrientationRequested.LANDSCAPE)
  }

  if (printJob.printDialog()) {
    try {
      printJob.print(attr)
    } catch (e: Exception) {
      if (!GPLogger.log(e)) {
        e.printStackTrace(System.err)
      }
    }
  }

}

class PageableImpl(private val imageFiles: List<File>) : Pageable {
  private val pageFormat = createPageFormat(MediaSizeName.ISO_A4)
  override fun getNumberOfPages() = imageFiles.size
  override fun getPageFormat(pageIndex: Int) = pageFormat
  override fun getPrintable(pageIndex: Int) = PrintableImpl(imageFiles[pageIndex])
}

class PrintableImpl(private val imageFile: File) : Printable {
  override fun print(graphics: Graphics, pageFormat: PageFormat, pageIndex: Int): Int {
    val image = ImageIO.read(imageFile)
    val g2d = graphics as? Graphics2D
    g2d?.let {
      it.drawRenderedImage(image, AffineTransform())
    }
    return Printable.PAGE_EXISTS
  }

}
fun createPageFormat(mediaSizeName: MediaSizeName): PageFormat {
  val size = MediaSize.getMediaSizeForName(mediaSizeName).getSize(MediaSize.INCH)
  return PageFormat().also { format ->
    format.paper = Paper().also { paper ->
      paper.setSize(size[0] * 72.0, size[1] * 72.0)
    }
  }
}
