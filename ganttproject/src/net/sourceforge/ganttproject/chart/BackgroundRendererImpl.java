package net.sourceforge.ganttproject.chart;

import java.awt.Color;

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