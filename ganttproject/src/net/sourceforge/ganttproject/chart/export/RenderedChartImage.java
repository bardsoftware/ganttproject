/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2005-2011 Dmitry Barashev

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
package net.sourceforge.ganttproject.chart.export;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;

import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.chart.SimpleRenderedImage;

public class RenderedChartImage extends SimpleRenderedImage {
  private BufferedImage myTaskImage;
  ColorModel myColorModel = new DirectColorModel(32, 0x00ff0000, // Red
      0x0000ff00, // Green
      0x000000ff, // Blue
      0x0 // Alpha
  );
  SampleModel mySampleModel;
  // private final List myVisibleTasks;
  private int myCurrentTile = -1;
  private Raster myCurrentRaster;
  private final ChartModel myChartModel;
  private final int headerYOffset;

  public RenderedChartImage(ChartModel chartModel, BufferedImage taskImage, int chartWidth, int chartHeight,
      int headerYOffset) {
    myChartModel = chartModel;
    myTaskImage = taskImage;
    sampleModel = myColorModel.createCompatibleSampleModel(chartWidth, chartHeight);
    colorModel = myColorModel;
    minX = 0;
    minY = 0;
    width = chartWidth + taskImage.getWidth();
    height = chartHeight;
    tileWidth = width;
    tileHeight = 32;
    this.headerYOffset = headerYOffset;
  }

  public BufferedImage getWholeImage() {
    BufferedImage chartImage = getChart(0, 0, getWidth(), getHeight(), getWidth(), getHeight());
    BufferedImage result = new BufferedImage(chartImage.getWidth() + myTaskImage.getWidth(), getHeight(),
        BufferedImage.TYPE_INT_RGB);
    Graphics g = result.getGraphics();
    g.drawImage(myTaskImage, 0, 0, null);
    g.translate(myTaskImage.getWidth(), 0);
    g.drawImage(chartImage, 0, 0, null);
    return result;
  }

  @Override
  public Raster getTile(int tileX, int tileY) {
    if (myCurrentTile != tileY) {
      int offsety = tileY * getTileHeight() - headerYOffset;
      BufferedImage tile = getChart(myTaskImage.getWidth(), offsety, getTileWidth(), getTileHeight(), getWidth(),
          getHeight());
      Graphics g = tile.getGraphics();
      g.translate(0, -offsety);
      g.drawImage(myTaskImage, 0, 0, null);
      myCurrentRaster = tile.getRaster().createTranslatedChild(0, tileY * getTileHeight());
      myCurrentTile = tileY;
    }
    return myCurrentRaster;
  }

  protected void paintChart(Graphics g) {
    myChartModel.paint(g);
  }

  private BufferedImage getChart(int offsetx, int offsety, int width, int height, int chartWidth, int chartHeight) {
    BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics g2 = result.getGraphics();
    g2.setColor(Color.white);
    g2.fillRect(0, 0, width, height);
    g2.translate(offsetx, -offsety);
    g2.clipRect(0, offsety, width, height);
    myChartModel.setBounds(new Dimension(chartWidth, chartHeight));
    paintChart(g2);
    // myChartModel.setTuningOptions(ChartModelImpl.TuningOptions.DEFAULT);
    return result;
  }
}