package net.sourceforge.ganttproject.chart;

import java.awt.Color;

import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;


public class BackgroundRendererImpl extends ChartRendererBase {

    public BackgroundRendererImpl() {

    }
    public BackgroundRendererImpl(ChartModel model) {
        super(model);
    }

    public GraphicPrimitiveContainer paint() {
        return getPrimitiveContainer();
    }

    public void render() {
        getPrimitiveContainer().clear();
        GraphicPrimitiveContainer.Rectangle r = getPrimitiveContainer().createRectangle(0, 0, getWidth(), getHeight());
        r.setBackgroundColor(Color.WHITE);
    }
}