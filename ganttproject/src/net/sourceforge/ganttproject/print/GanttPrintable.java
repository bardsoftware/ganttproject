/***************************************************************************
 GanttPrintable.java  -  description
 -------------------
 begin                : sep 2003
 copyright            : (C) 2003 by Thomas Alexandre
 email                : alexthomas(at)ganttproject.org
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 3 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

package net.sourceforge.ganttproject.print;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

/**
 * Class able to print an image
 */
public class GanttPrintable implements Printable {

    public final static double REDUCE_FACTOR_DEFAULT = 1.5d;

    private double reduceFactor;

    /** The image to print */
    private RenderedImage image;

    public GanttPrintable(RenderedImage image, double reduceFactor) {
        super();
        this.image = image;
        this.reduceFactor = reduceFactor < 1.0d ? REDUCE_FACTOR_DEFAULT
                : reduceFactor;
    }

    /** Print the page */
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) {
        System.err.println("[GanttPrintable] print(): reduceFactor="
                + reduceFactor);
        System.err.println("[GanttPrintable] print(): image: w="
                + image.getWidth() + " h=" + image.getHeight());
        System.err.println("[GanttPrintable] print(): page=" + pageIndex);

        int pagesPerRow = (int) (image.getWidth() / reduceFactor
                / pageFormat.getImageableWidth() + 1);
        int numRows = (int) (image.getHeight() / reduceFactor
                / pageFormat.getImageableHeight() + 1);

        System.err.println("[GanttPrintable] print(): numrows=" + numRows
                + " pagesPerRow=" + pagesPerRow);
        int totalPages = pagesPerRow * numRows;
        if (pageIndex >= totalPages) {
            return Printable.NO_SUCH_PAGE;
        }

        int currentRow = pageIndex / pagesPerRow;
        int currentColumn = pageIndex - currentRow * pagesPerRow;
        System.err.println("[GanttPrintable] print(): curentpage="
                + currentColumn + " current row=" + currentRow);

        int leftx = (int) (currentColumn * (pageFormat.getImageableWidth()
                * reduceFactor - 2 / 3 * pageFormat.getImageableX()));
        int topy = (int) (currentRow * pageFormat.getImageableHeight() * reduceFactor);
        System.err.println("[GanttPrintable] print(): leftx=" + leftx
                + " topy=" + topy);

        Graphics2D g2d = (Graphics2D) graphics;
        g2d.setClip((int) pageFormat.getImageableX(), (int) pageFormat
                .getImageableY(), (int) pageFormat.getImageableWidth(),
                (int) pageFormat.getImageableHeight());

        AffineTransform transform = AffineTransform.getScaleInstance(
                1 / reduceFactor, 1 / reduceFactor);
        transform.translate(pageFormat.getImageableX() - leftx, pageFormat
                .getImageableY()
                - topy);
        g2d.drawRenderedImage(image, transform);

        return Printable.PAGE_EXISTS;
    }
}
