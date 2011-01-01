package net.sourceforge.ganttproject;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.Icon;

import net.sourceforge.ganttproject.ChartComponentBase.MouseInteraction;
import net.sourceforge.ganttproject.chart.Chart;
import net.sourceforge.ganttproject.chart.ChartModel;
import net.sourceforge.ganttproject.chart.ChartModelBase;
import net.sourceforge.ganttproject.chart.ChartRendererBase;
import net.sourceforge.ganttproject.chart.ChartSelection;
import net.sourceforge.ganttproject.chart.ChartSelectionListener;
import net.sourceforge.ganttproject.chart.ChartUIConfiguration;
import net.sourceforge.ganttproject.chart.TimelineChart;
import net.sourceforge.ganttproject.gui.options.model.GPOptionGroup;
import net.sourceforge.ganttproject.gui.zoom.ZoomEvent;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.task.TaskLength;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitStack;

import org.eclipse.core.runtime.IStatus;

public class AbstractChartImplementation implements TimelineChart, ZoomListener {
    private final ChartModelBase myChartModel;
    private final IGanttProject myProject;
    private Set<ChartSelectionListener> mySelectionListeners= new LinkedHashSet<ChartSelectionListener>();
    private final ChartComponentBase myChartComponent;

    public AbstractChartImplementation(IGanttProject project, ChartModelBase chartModel, ChartComponentBase chartComponent) {
        assert chartModel!=null;
        myChartModel = chartModel;
        myProject = project;
        myChartComponent = chartComponent;
    }
    public IGanttProject getProject() {
        return myProject;
    }

    public void beginScrollViewInteraction(MouseEvent e) {
        setActiveInteraction(myChartComponent.newScrollViewInteraction(e));
    }

    public MouseInteraction finishInteraction() {
        try {
            if (getActiveInteraction() != null) {
                getActiveInteraction().finish();
            }
            return getActiveInteraction();
        } finally {
            setActiveInteraction(null);
        }
    }

    protected void setActiveInteraction(MouseInteraction myActiveInteraction) {
        this.myActiveInteraction = myActiveInteraction;
    }

    public MouseInteraction getActiveInteraction() {
        return myActiveInteraction;
    }

    public void zoomChanged(ZoomEvent e) {
        myChartComponent.invalidate();
        myChartComponent.repaint();
    }


    public void paintChart(Graphics g) {
        getChartModel().paint(g);
    }
    private MouseInteraction myActiveInteraction;
    private TimeFrame myFirstTimeFrame;

    private ChartModelBase getChartModel() {
        return myChartModel;
    }
    /////////////////////////////////////////////////////////////
    // interface Chart
    public RenderedImage getRenderedImage(GanttExportSettings settings) {
        // TODO Auto-generated method stub
        return null;
    }
    public BufferedImage getChart(GanttExportSettings settings) {
        // TODO Auto-generated method stub
        return null;
    }

    public Date getStartDate() {
        return getChartModel().getStartDate();
    }
    public void setStartDate(Date startDate) {
        getChartModel().setStartDate(startDate);
        myFirstTimeFrame = scrollTimeFrame(startDate);
        startDate = myFirstTimeFrame.getStartDate();
        getChartModel().setStartDate(startDate);
    }

    public void scrollBy(TaskLength duration) {
    	setStartDate(getChartModel().getTaskManager().shift(getStartDate(), duration));
    }

    private TimeFrame scrollTimeFrame(Date scrolledDate) {
        TimeFrame result = null;
        if (getTopTimeUnit().isConstructedFrom(getBottomTimeUnit())) {
            result = getTimeUnitStack().createTimeFrame(scrolledDate,
                    getTopTimeUnit(), getBottomTimeUnit());
        } else {
            result = getTimeUnitStack().createTimeFrame(scrolledDate,
                    getBottomTimeUnit(), getBottomTimeUnit());
        }
        return result;
    }
    private TimeUnit getTopTimeUnit() {
        return getChartModel().getTopUnit();
    }
    private TimeUnit getBottomTimeUnit() {
        return getChartModel().getBottomUnit();
    }
    private TimeUnitStack getTimeUnitStack() {
        return myProject.getTimeUnitStack();
    }
    public Date getEndDate() {
        return getChartModel().getEndDate();
    }

    public void setDimensions(int height, int width) {
        //int width = (int)projectLength.getLength(getChartModel().getBottomUnit())*getChartModel().getBottomUnitWidth();
        Dimension bounds = new Dimension(width, height);
        getChartModel().setBounds(bounds);
    }

    public void setBottomUnit(TimeUnit bottomUnit) {
        getChartModel().setBottomTimeUnit(bottomUnit);
    }
    public void setTopUnit(TimeUnit topUnit) {
        getChartModel().setTopTimeUnit(topUnit);
    }

    public void setBottomUnitWidth(int width) {
        getChartModel().setBottomUnitWidth(width);
    }

    public String getName() {
        return myChartComponent.getName();
    }

    public void setTaskManager(TaskManager taskManager) {
        throw new UnsupportedOperationException();
    }

    public void reset() {
        throw new UnsupportedOperationException();
    }

    public Icon getIcon() {
        return null;
    }

    public GPOptionGroup[] getOptionGroups() {
        return getChartModel().getChartOptionGroups();
    }

    public Chart createCopy() {
        return new AbstractChartImplementation(myProject, getChartModel().createCopy(), myChartComponent);
    }

    public Object getAdapter(Class arg0) {
        return null;
    }
    public ChartSelection getSelection() {
        throw new UnsupportedOperationException();
    }
    public IStatus canPaste(ChartSelection selection) {
        throw new UnsupportedOperationException();
    }
    public void paste(ChartSelection selection) {
        throw new UnsupportedOperationException();
    }
    public void addSelectionListener(ChartSelectionListener listener) {
        mySelectionListeners.add(listener);
    }
    public void removeSelectionListener(ChartSelectionListener listener) {
        mySelectionListeners.remove(listener);
    }
    protected void fireSelectionChanged() {
        for (Iterator<ChartSelectionListener> listeners = mySelectionListeners.iterator(); listeners.hasNext();) {
            ChartSelectionListener nextListener = listeners.next();
            nextListener.selectionChanged();
        }
    }
    public void addRenderer(ChartRendererBase renderer) {
        myChartModel.addRenderer(renderer);
    }
//    public void addTimeUnitVisitor(TimeUnitVisitor visitor) {
//        myChartModel.addTimeUnitVisitor(visitor);
//    }
    public void resetRenderers() {
        myChartModel.resetRenderers();
    }
    public TaskLength calculateLength(int x) {
        float units = myChartModel.calculateLengthNoWeekends(0, x);
        return myChartModel.getTaskManager().createLength(myChartModel.getBottomUnit(), units);
    }
    public ChartModel getModel() {
        return myChartModel;
    }
    @Override
    public ChartUIConfiguration getStyle() {
        return myChartModel.getChartUIConfiguration();
    }
    
}