/*
 * Created on 04.12.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.ganttproject.chart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.util.List;

import net.sourceforge.ganttproject.GanttGraphicArea;
import net.sourceforge.ganttproject.GanttGraphicArea.ChartImplementation;

public abstract class RenderedChartImage extends SimpleRenderedImage {

    private BufferedImage myTaskImage;
    ColorModel myColorModel = new DirectColorModel(32,
            0x00ff0000, // Red
            0x0000ff00, // Green
            0x000000ff, // Blue
            0x0           // Alpha
            );
    SampleModel mySampleModel;
    //private final List myVisibleTasks;
	private int myCurrentTile = -1;
	private Raster myCurrentRaster;
	private final ChartModelBase myChartModel;
	
    public RenderedChartImage(ChartModelBase chartModel, BufferedImage taskImage, int chartWidth, int chartHeight) {
    	myChartModel = chartModel;
        myTaskImage = taskImage;
        sampleModel = myColorModel.createCompatibleSampleModel(chartWidth,chartHeight);
        colorModel = myColorModel;
        minX = 0;
        minY = 0;
        width = chartWidth+taskImage.getWidth();
        height = chartHeight;
        tileWidth = width;
        tileHeight = 32;
         
    }


    public BufferedImage getWholeImage() {
		BufferedImage chartImage = getChart(0, 0, getWidth(), getHeight(), getWidth(), getHeight());
        BufferedImage result = new BufferedImage(chartImage.getWidth()+myTaskImage.getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics g = result.getGraphics();
        g.drawImage(myTaskImage,0,0,null);
        g.translate(myTaskImage.getWidth(), 0);
        g.drawImage(chartImage, 0,0,null);
        return result;
	}


	public Raster getTile(int tileX, int tileY) {
		if (myCurrentTile!=tileY) {
            int offsety = tileY*getTileHeight();
            BufferedImage tile = getChart(myTaskImage.getWidth(), offsety, getTileWidth(),getTileHeight(),  getWidth(), getHeight());
            Graphics g = tile.getGraphics();
            g.translate(0, -offsety);
            g.drawImage(myTaskImage,0,0,null);
            myCurrentRaster = tile.getRaster().createTranslatedChild(0, tileY*getTileHeight());
            myCurrentTile = tileY;
		}
		return myCurrentRaster;
    }

    protected abstract void paintChart(Graphics g);
    
    private BufferedImage getChart(int offsetx, int offsety,  int width, int height, int chartWidth, int chartHeight) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB );
        Graphics g2 = result.getGraphics();
        g2.setColor(Color.white);
        g2.fillRect(0, 0, width,height);
        g2.translate(offsetx, -offsety);
        g2.clipRect(0,offsety,width,height);
        myChartModel.setBounds(new Dimension(chartWidth, chartHeight));
        paintChart(g2);
        //myChartModel.setTuningOptions(ChartModelImpl.TuningOptions.DEFAULT);
        return result;

    }
    
}