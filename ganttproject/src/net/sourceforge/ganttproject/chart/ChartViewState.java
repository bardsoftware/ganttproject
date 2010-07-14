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
import net.sourceforge.ganttproject.calendar.CalendarFactory;
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

    private TimeFrame myCurrentTimeFrame;

    private List myListeners = new ArrayList();

    // private int myBottomUnitWidth;
    private int myTimeUnitPair = 0;

    private ZoomManager.ZoomState[] myZoomStates;

    private int myZoomStateIndex = 2;

    private ZoomState myCurrentZoomState;

    private IGanttProject iProject;

    private UIFacade myUIFacade;

    public ChartViewState(IGanttProject project, UIFacade uiFacade) {
        iProject = project;
        myUIFacade = uiFacade;
        myTimeUnitStack = project.getTimeUnitStack();
        myStartDate = CalendarFactory.newCalendar().getTime();
        uiFacade.getZoomManager().addZoomListener(this);
        myCurrentTimeFrame = myTimeUnitStack.createTimeFrame(myStartDate,
                getTopTimeUnit(), getBottomTimeUnit());
        // setDefaultBottomUnitWidth();
    }

    // private void setDefaultBottomUnitWidth() {
    // myBottomUnitWidth = 20;
    // }

    public Date getStartDate() {
        return myStartDate;
    }

    public void scrollRight() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Date scrolledDate;
                if (myCurrentTimeFrame.getUnitCount(getBottomTimeUnit()) > 1) {
                    scrolledDate = myCurrentTimeFrame.getUnitStart(
                            getBottomTimeUnit(), 1);
                } else {
                    scrolledDate = myCurrentTimeFrame.getFinishDate();
                }
                setStartDate(scrolledDate);
            }
        });
    }

    public void scrollLeft() {
        Calendar c = CalendarFactory.newCalendar();
        c.setTime(myStartDate);
        c.add(Calendar.MILLISECOND, -1);
        Date scrolledDate = c.getTime();
        setStartDate(scrolledDate);
    }

    public void scrollLeft(Date date) {
        setStartDate(date);
    }

    public void setStartDate(Date startDate) {
        myCurrentTimeFrame = scrollTimeFrame(startDate);
        startDate = myCurrentTimeFrame.getStartDate();
        ViewStateEvent e = new ViewStateEvent(this, myStartDate, startDate);
        myStartDate = startDate;
        fireStartDateChanged(e);
    }

    private TimeFrame scrollTimeFrame(Date scrolledDate) {
        TimeFrame result = null;
        if (getTopTimeUnit().isConstructedFrom(getBottomTimeUnit())) {
            result = myTimeUnitStack.createTimeFrame(scrolledDate,
                    getTopTimeUnit(), getBottomTimeUnit());
        } else {
            result = myTimeUnitStack.createTimeFrame(scrolledDate,
                    getBottomTimeUnit(), getBottomTimeUnit());
        }
        return result;
    }

    private void fireStartDateChanged(ViewStateEvent e) {
        for (int i = 0; i < myListeners.size(); i++) {
            Listener nextListener = (Listener) myListeners.get(i);
            nextListener.startDateChanged(e);
        }
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
            date = d == null ? getStartDate() : d;
        } else
            date = getStartDate();
        setStartDate(date);
    }

    public void addStateListener(Listener listener) {
        myListeners.add(listener);
    }

    public void removeStateListener(Listener listener) {
        myListeners.remove(listener);
    }

    public static interface Listener extends EventListener {
        public void startDateChanged(ViewStateEvent e);

        public void zoomChanged(ZoomEvent e);
    }

    public static class ViewStateEvent extends EventObject {
        private final Object myOldValue;

        private final Object myNewValue;

        public ViewStateEvent(ChartViewState viewState, Object oldValue,
                Object newValue) {
            super(viewState);
            myOldValue = oldValue;
            myNewValue = newValue;
        }

        public Object getOldValue() {
            return myOldValue;
        }

        public Object getNewValue() {
            return myNewValue;
        }
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

    private ZoomManager.ZoomState getCurrentZoomState() {
        return myCurrentZoomState;
    }
}
