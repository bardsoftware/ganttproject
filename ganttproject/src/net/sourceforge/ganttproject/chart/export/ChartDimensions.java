package net.sourceforge.ganttproject.chart.export;

import net.sourceforge.ganttproject.AbstractChartImplementation;
import net.sourceforge.ganttproject.GPTreeTableBase;
import net.sourceforge.ganttproject.GanttExportSettings;

public class ChartDimensions {
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

    public int getTreeWidth() {
        return treeWidth;
    }

    public int getLogoHeight() {
        return logoHeight;
    }

    public int getTableHeaderHeight() {
        return tableHeaderHeight;
    }
}