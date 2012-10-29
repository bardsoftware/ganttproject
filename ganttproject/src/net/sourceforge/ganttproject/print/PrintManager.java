/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.print;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.awt.print.PrinterJob;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.chart.Chart;

public class PrintManager {

  public static void printChart(Chart chart, GanttExportSettings settings) {
    RenderedImage image = chart.getRenderedImage(settings);
    BufferedImage bufferedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
    image.copyData(bufferedImage.getRaster());

    PrinterJob printJob = PrinterJob.getPrinterJob();

    printJob.setPrintable(new GanttPrintable(bufferedImage, GanttPrintable.REDUCE_FACTOR_DEFAULT));

    PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
    attr.add(MediaSizeName.ISO_A4);
    attr.add(OrientationRequested.LANDSCAPE);

    if (printJob.printDialog(attr)) {
      try {
        printJob.print(attr);
      } catch (Exception e) {
        if (!GPLogger.log(e)) {
          e.printStackTrace(System.err);
        }
      }
    }
  }
}
