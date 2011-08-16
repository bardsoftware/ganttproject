/*
GanttProject is an opensource project management tool. License: GPL2
Copyright (C) 2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

import net.sourceforge.ganttproject.AbstractChartImplementation;
import net.sourceforge.ganttproject.GPTreeTableBase;
import net.sourceforge.ganttproject.GanttExportSettings;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.OffsetBuilder;
import net.sourceforge.ganttproject.chart.OffsetBuilder.Factory;
import net.sourceforge.ganttproject.chart.OffsetList;

public class ChartImageBuilder {
    private final ChartModelBase myChartModel;

    public ChartImageBuilder(ChartModelBase chartModel) {
        myChartModel = chartModel;
    }

    public RenderedImage getRenderedImage(GanttExportSettings settings, GPTreeTableBase treeTable) {
        final int headerHeight = AbstractChartImplementation.LOGO.getIconHeight();
        final int treeHeight = treeTable.getRowHeight() * (settings.getRowCount() + 1);
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

        ChartModelBase modelCopy = myChartModel.createCopy();
        if (settings.getZoomLevel() != null) {
            modelCopy.setBottomTimeUnit(settings.getZoomLevel().getTimeUnitPair().getBottomTimeUnit());
            modelCopy.setTopTimeUnit(settings.getZoomLevel().getTimeUnitPair().getTopTimeUnit());
            modelCopy.setBottomUnitWidth(settings.getZoomLevel().getBottomUnitWidth());
        }
        OffsetBuilder.Factory factory = modelCopy.createOffsetBuilderFactory()
            .withStartDate(settings.getStartDate())
            .withEndDate(settings.getEndDate())
            .withEndOffset(settings.getWidth() < 0 ? Integer.MAX_VALUE : settings.getWidth());

        OffsetBuilder offsetBuilder = factory.build();
        OffsetList bottomOffsets = new OffsetList();
        offsetBuilder.constructOffsets(null, bottomOffsets);
        int chartWidth = bottomOffsets.getEndPx();
        int chartHeight = wholeImageHeight;

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
