package net.sourceforge.ganttproject.chart;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

import javax.swing.SwingUtilities;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.Mediator;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.scrolling.ScrollingListener;
import net.sourceforge.ganttproject.gui.zoom.ZoomEvent;
import net.sourceforge.ganttproject.gui.zoom.ZoomListener;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager;
import net.sourceforge.ganttproject.gui.zoom.ZoomManager.ZoomState;
import net.sourceforge.ganttproject.time.TimeFrame;
import net.sourceforge.ganttproject.time.TimeUnit;
import net.sourceforge.ganttproject.time.TimeUnitStack;

/**
 * Created by IntelliJ IDEA. User: bard
 */
public class ChartViewState implements ScrollingListener, ZoomListener {
    private Date myStartDate;

    private final TimeUnitStack myTimeUnitStack;

    private ZoomState myCurrentZoomState;
    private UIFacade myUIFacade;

    private final TimelineChart myChart;

    public ChartViewState(TimelineChart chart, UIFacade uiFacade) {
        final IGanttProject project = chart.getProject();
        myChart = chart;
        myUIFacade = uiFacade;
        myTimeUnitStack = project.getTimeUnitStack();
        myStartDate = Calendar.getInstance().getTime();
        uiFacade.getZoomManager().addZoomListener(this);
    }

    // private void setDefaultBottomUnitWidth() {
    // myBottomUnitWidth = 20;
    // }

    public void scrollRight() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                myChart.scrollRight();
            }
        });
    }

    public void scrollLeft() {
        myChart.scrollLeft();
    }

    public void scrollLeft(Date date) {
        myChart.setStartDate(date);
    }

    public void zoomChanged(ZoomEvent e) {
        myCurrentZoomState = e.getNewZoomState();
        Date date = null;
        if (myUIFacade.getViewIndex() == UIFacade.GANTT_INDEX) {
            Date d = Mediator.getTaskSelectionManager().getEarliestStart();
            // boolean zoomin = e.getZoomValue() < e.getOldValue();
            // if (zoomin && myZoomStateIndex>0) {
            // myZoomStateIndex--;
            // }
            // else if (!zoomin && myZoomStateIndex<myZoomStates.length-1) {
            // myZoomStateIndex++;
            // }

            // myCurrentTimeFrame = scrollTimeFrame(d==null ? getStartDate() :
            // d);
            date = d == null ? myChart.getStartDate() : d;
        } else
            date = myChart.getStartDate();

        myChart.setTopUnit(getTopTimeUnit());
        myChart.setBottomUnit(getBottomTimeUnit());
        myChart.setBottomUnitWidth(getBottomUnitWidth());
        myChart.setStartDate(date==null ? new Date() : date);
    }


    public int getBottomUnitWidth() {
        return getCurrentZoomState().getBottomUnitWidth();
    }

    /**
     * @return
     */
    public TimeUnit getTopTimeUnit() {
        return getCurrentZoomState().getTimeUnitPair().getTopTimeUnit();
    }

    /**
     * @return
     */
    public TimeUnit getBottomTimeUnit() {
        return getCurrentZoomState().getTimeUnitPair().getBottomTimeUnit();
    }

    public ZoomManager.ZoomState getCurrentZoomState() {
        return myCurrentZoomState;
    }
}