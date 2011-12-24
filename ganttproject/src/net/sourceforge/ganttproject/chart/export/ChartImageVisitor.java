package net.sourceforge.ganttproject.chart.export;

import java.awt.Component;
import java.awt.Image;
import net.sourceforge.ganttproject.chart.ChartModel;

public interface ChartImageVisitor {
    void acceptLogo(ChartDimensions d, Image logo);
    void acceptTable(ChartDimensions d, Component header, Component table);
    void acceptChart(ChartDimensions d, ChartModel model);
}
