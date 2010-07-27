package net.sourceforge.ganttproject.print;

import java.awt.image.BufferedImage;
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
        BufferedImage image = chart.getChart(settings);

        PrinterJob printJob = PrinterJob.getPrinterJob();

        printJob.setPrintable(new GanttPrintable(image,
                GanttPrintable.REDUCE_FACTOR_DEFAULT));

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
