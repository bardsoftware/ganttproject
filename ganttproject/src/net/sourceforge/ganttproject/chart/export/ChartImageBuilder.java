package net.sourceforge.ganttproject.chart.export;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import net.sourceforge.ganttproject.AbstractChartImplementation;
import net.sourceforge.ganttproject.GPTreeTableBase;
import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.OffsetBuilder;
import net.sourceforge.ganttproject.chart.OffsetList;

public class ChartImageBuilder {
    private final ChartModelBase myChartModel;

    public ChartImageBuilder(ChartModelBase chartModel) {
        myChartModel = chartModel;
    }
    
    public RenderedImage getRenderedImage(GanttExportSettings settings, GPTreeTableBase treeTable) {
        final int headerHeight = AbstractChartImplementation.LOGO.getIconHeight();
        final int treeHeight = treeTable.getHeight();
        final int treeWidth = treeTable.getWidth();
        final int wholeImageHeight = treeHeight + headerHeight;
        
        BufferedImage treeImage  = new BufferedImage(treeWidth, wholeImageHeight, BufferedImage.TYPE_INT_RGB);
        {
            Graphics2D g = treeImage.createGraphics();
            g.setBackground(Color.WHITE);
            g.clearRect(0, 0, treeImage.getWidth(), headerHeight);
            g.drawImage(AbstractChartImplementation.LOGO.getImage(), 0, 0, null);
        }        
        {
            Graphics2D g = treeImage.createGraphics();
            g.translate(0, headerHeight);
            treeTable.printAll(g);
        }

        OffsetBuilder offsetBuilder = myChartModel.createOffsetBuilderFactory()
            .withStartDate(settings.getStartDate())
            .withEndDate(settings.getEndDate())
            .withEndOffset(Integer.MAX_VALUE)
            .build();
        OffsetList bottomOffsets = new OffsetList();
        offsetBuilder.constructOffsets(null, bottomOffsets);
        int chartWidth = bottomOffsets.getEndPx();
        int chartHeight = wholeImageHeight;

        ChartModelBase modelCopy = myChartModel.createCopy();
        modelCopy.setHeaderHeight(headerHeight + treeTable.getTable().getTableHeader().getHeight());
        modelCopy.setVisibleTasks(settings.getVisibleTasks());
        
        RenderedChartImage result = new RenderedChartImage(
            modelCopy, 
            treeImage, 
            chartWidth, 
            chartHeight);
        return result;
    }
    
}
