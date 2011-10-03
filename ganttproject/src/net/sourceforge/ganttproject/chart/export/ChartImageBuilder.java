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
import java.awt.Dimension;
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

abstract public class ChartImageBuilder {
    private final ChartModelBase myChartModel;

    public ChartImageBuilder(ChartModelBase chartModel) {
        myChartModel = chartModel;
    }

    protected static class ChartDimensions {
        final int logoHeight;
        final int treeHeight;
        final int tableHeaderHeight;
        final int treeWidth;
        int chartWidth;

        ChartDimensions(GanttExportSettings settings, GPTreeTableBase treeTable) {
            logoHeight = AbstractChartImplementation.LOGO.getIconHeight();
            treeHeight = treeTable.getRowHeight() * (settings.getRowCount());
            tableHeaderHeight = treeTable.getTable().getTableHeader().getHeight();
            treeWidth = treeTable.getTable().getWidth();
        }

        public int getChartHeight() {
            return treeHeight + tableHeaderHeight + logoHeight;
        }

        public int getChartWidth() {
            return chartWidth;
        }
    }

    public void buildImage(GanttExportSettings settings, GPTreeTableBase treeTable) {
        ChartDimensions d = new ChartDimensions(settings, treeTable);

        BufferedImage treeImage  = new BufferedImage(d.treeWidth, d.getChartHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = treeImage.createGraphics();
        printTreeTable(g, settings, treeTable);

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
        d.chartWidth = bottomOffsets.getEndPx();
        modelCopy.setBounds(new Dimension(d.chartWidth, d.getChartHeight()));

        modelCopy.setHeaderHeight(d.logoHeight + d.tableHeaderHeight - 1);
        modelCopy.setVisibleTasks(settings.getVisibleTasks());

        process(modelCopy, treeImage, d);
    }

    protected abstract void process(ChartModelBase modelCopy, BufferedImage treeImage, ChartDimensions d);

    public static void printTreeTable(Graphics2D g, GanttExportSettings settings, GPTreeTableBase treeTable) {
        ChartDimensions d = new ChartDimensions(settings, treeTable);
        {
            g.setBackground(Color.WHITE);
            g.clearRect(0, 0, d.treeWidth, d.logoHeight);
            g.drawImage(AbstractChartImplementation.LOGO.getImage(), 0, 0, null);
        }
        {
            g.translate(0, d.logoHeight);
            treeTable.getTable().getTableHeader().printAll(g);
        }
        {
            g.translate(0, d.tableHeaderHeight);
            treeTable.getTable().printAll(g);
        }
    }
}
