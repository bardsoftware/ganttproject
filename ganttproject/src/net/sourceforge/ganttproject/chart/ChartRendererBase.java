package net.sourceforge.ganttproject.chart;

/**
 * Created by IntelliJ IDEA. User: bard Date: 12.10.2004 Time: 0:56:19 To change
 * this template use Options | File Templates.
 */
public class ChartRendererBase {
    private int myHeight;

    private final ChartModelBase myChartModel;

    private final GraphicPrimitiveContainer myPrimitiveContainer;

    private boolean isEnabled = true;

    public ChartRendererBase(ChartModelBase model) {
        myPrimitiveContainer = new GraphicPrimitiveContainer();
        myChartModel = model;
    }

    void setHeight(int height) {
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

    protected ChartModelBase getChartModel() {
        return myChartModel;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
    
    public void beforeProcessingTimeFrames() {
        myPrimitiveContainer.clear();
    }
    
}
