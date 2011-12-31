package net.sourceforge.ganttproject.chart;

import net.sourceforge.ganttproject.calendar.GPCalendar;

/**
 * Created by IntelliJ IDEA. User: bard Date: 12.10.2004 Time: 0:56:19 To change
 * this template use Options | File Templates.
 */
public class ChartRendererBase {
    private int myHeight;

    private ChartModel myChartModel;

    private final GraphicPrimitiveContainer myPrimitiveContainer;

    private boolean isEnabled = true;

    protected ChartRendererBase() {
        myPrimitiveContainer = new GraphicPrimitiveContainer();
    }
    public ChartRendererBase(ChartModel model) {
        this();
        myChartModel = model;
    }

    public void setHeight(int height) {
        myHeight = height;
    }

    protected int getHeight() {
        return myHeight;
    }

    protected int getWidth() {
        return (int) getChartModel().getBounds().getWidth();
    }

    protected ChartUIConfiguration getConfig() {
        return getChartModel().getChartUIConfiguration();
    }

    public GraphicPrimitiveContainer getPrimitiveContainer() {
        return myPrimitiveContainer;
    }

    protected ChartModel getChartModel() {
        return myChartModel;
    }

    protected GPCalendar getCalendar() {
        return myChartModel.getTaskManager().getCalendar();
    }
    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public void clear() {
        getPrimitiveContainer().clear();
    }
//    protected void setChartModel(ChartModelBase chartModel) {
//        myChartModel = chartModel;
//    }
    public void render() {
    }

}